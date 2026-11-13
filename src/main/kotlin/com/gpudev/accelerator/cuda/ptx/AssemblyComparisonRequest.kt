package com.gpudev.accelerator.cuda.ptx

import com.gpudev.accelerator.cuda.CudaFileType
import com.intellij.diff.DiffContext
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.ComponentDiffRequest
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.SwingConstants

internal enum class AssemblyOutputKind(val title: String) { SASS("SASS"), PTX("PTX") }

internal data class AssemblyPaneData(
    val kind: AssemblyOutputKind,
    val text: String,
    val fileType: FileType,
    val references: List<SourceLineReference>,
    val title: String = kind.title
)

internal data class AssemblyComparisonData(
    val sourceName: String,
    val sourceText: String,
    val initialPane: AssemblyPaneData,
    val statusMessage: String? = null
)

internal sealed interface AssemblyOutputLoadResult {
    data class Success(val pane: AssemblyPaneData) : AssemblyOutputLoadResult
    data class Failure(val message: String) : AssemblyOutputLoadResult
}

internal fun interface AssemblyOutputLoader {
    fun load(kind: AssemblyOutputKind, onComplete: (AssemblyOutputLoadResult) -> Unit)
}

/** Opens a source/PTX/SASS comparison in the IDE's standard diff window. */
internal class AssemblyComparisonRequest(
    private val project: Project,
    private val data: AssemblyComparisonData,
    private val loader: AssemblyOutputLoader
) : ComponentDiffRequest() {
    private var panel: AssemblyComparisonPanel? = null

    override fun getTitle(): String = "${data.sourceName} ↔ Assembly"

    override fun getComponent(context: DiffContext): JPanel =
        panel?.component ?: AssemblyComparisonPanel(project, data, loader).also { panel = it }.component

    override fun onAssigned(isAssigned: Boolean) {
        super.onAssigned(isAssigned)
        if (!isAssigned) {
            panel?.let(Disposer::dispose)
            panel = null
        }
    }

    companion object {
        fun show(project: Project, data: AssemblyComparisonData, loader: AssemblyOutputLoader) {
            DiffManager.getInstance().showDiff(project, AssemblyComparisonRequest(project, data, loader))
        }
    }
}

@Suppress("TooManyFunctions")
private class AssemblyComparisonPanel(
    private val project: Project,
    private val data: AssemblyComparisonData,
    private val loader: AssemblyOutputLoader
) : JPanel(BorderLayout()), Disposable {
    private val sourceName = data.sourceName
    private val sourceText = data.sourceText
    private val initialPane = data.initialPane
    private val initialStatus = data.statusMessage
    private val sourceEditor: Editor
    private val sourcePane: JPanel
    private val sourceBaseHighlighters = mutableListOf<RangeHighlighter>()
    private val activeHighlighters = mutableListOf<Pair<Editor, RangeHighlighter>>()
    private val outputEditors = mutableMapOf<AssemblyOutputKind, OutputEditor>()
    private val enabledOutputs = mutableSetOf(initialPane.kind)
    private val loadingOutputs = mutableSetOf<AssemblyOutputKind>()
    private val content = JPanel(BorderLayout())
    private val status = JLabel()
    private var lastUsedTabs = false
    private var disposed = false
    private var updatingToggles = false

    private val sassToggle = JCheckBox("SASS", initialPane.kind == AssemblyOutputKind.SASS)
    private val ptxToggle = JCheckBox("PTX", initialPane.kind == AssemblyOutputKind.PTX)
    private val sourceMouseListener = object : EditorMouseListener {
        override fun mouseClicked(event: EditorMouseEvent) {
            if (event.isOverText) selectSourceLine(event.logicalPosition.line + 1)
        }
    }

    val component: JPanel get() = this

    init {
        val editorFactory = EditorFactory.getInstance()
        sourceEditor = editorFactory.createEditor(
            editorFactory.createDocument(sourceText), project, CudaFileType.INSTANCE, true
        )
        sourcePane = editorPane(sourceName, sourceEditor.component)
        outputEditors[initialPane.kind] = OutputEditor(initialPane)

        val toolbar = JPanel().apply {
            add(sassToggle)
            add(ptxToggle)
        }
        add(toolbar, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)
        add(status.apply { border = BorderFactory.createEmptyBorder(4, 8, 4, 8) }, BorderLayout.SOUTH)
        preferredSize = Dimension(1350, 700)

        sassToggle.addActionListener {
            if (!updatingToggles) setOutputEnabled(AssemblyOutputKind.SASS, sassToggle.isSelected)
        }
        ptxToggle.addActionListener {
            if (!updatingToggles) setOutputEnabled(AssemblyOutputKind.PTX, ptxToggle.isSelected)
        }
        sourceEditor.addEditorMouseListener(sourceMouseListener)
        refreshSourceBaseHighlights()
        rebuildContent()
        updateStatus(initialStatus)
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                val wantsTabs = width < TABS_WIDTH_THRESHOLD && enabledOutputs.size > 1
                if (wantsTabs != lastUsedTabs) rebuildContent()
            }
        })
    }

    private fun editorPane(title: String, editor: JComponent): JPanel = JPanel(BorderLayout()).apply {
        add(JLabel(title, SwingConstants.LEADING).apply {
            border = BorderFactory.createEmptyBorder(5, 8, 5, 8)
            isOpaque = true
            background = EditorColorsManager.getInstance().globalScheme.defaultBackground
            foreground = EditorColorsManager.getInstance().globalScheme.defaultForeground
        }, BorderLayout.NORTH)
        add(editor, BorderLayout.CENTER)
    }

    private fun setOutputEnabled(kind: AssemblyOutputKind, enabled: Boolean) {
        if (!enabled) {
            enabledOutputs.remove(kind)
            clearActiveHighlights()
            rebuildContent()
            updateStatus(null)
            return
        }
        enabledOutputs.add(kind)
        rebuildContent()
        if (outputEditors.containsKey(kind) || kind in loadingOutputs) {
            updateStatus(null)
            return
        }

        loadingOutputs.add(kind)
        updateStatus("Generating ${kind.title}…")
        rebuildContent()
        loader.load(kind) { result ->
            ApplicationManager.getApplication().invokeLater {
                if (disposed) return@invokeLater
                loadingOutputs.remove(kind)
                when (result) {
                    is AssemblyOutputLoadResult.Success -> {
                        outputEditors[kind] = OutputEditor(result.pane)
                        refreshSourceBaseHighlights()
                        updateStatus(null)
                    }
                    is AssemblyOutputLoadResult.Failure -> {
                        enabledOutputs.remove(kind)
                        setToggle(kind, false)
                        updateStatus(result.message)
                    }
                }
                rebuildContent()
            }
        }
    }

    private fun setToggle(kind: AssemblyOutputKind, selected: Boolean) {
        updatingToggles = true
        try {
            when (kind) {
                AssemblyOutputKind.SASS -> sassToggle.isSelected = selected
                AssemblyOutputKind.PTX -> ptxToggle.isSelected = selected
            }
        } finally {
            updatingToggles = false
        }
    }

    private fun rebuildContent() {
        val visibleOutputs = AssemblyOutputKind.values().filter { it in enabledOutputs }.mapNotNull(outputEditors::get)
        val tabs = width < TABS_WIDTH_THRESHOLD && enabledOutputs.size > 1
        lastUsedTabs = tabs
        detach(sourcePane)
        outputEditors.values.forEach { detach(it.panel) }
        content.removeAll()

        val outputArea = when {
            visibleOutputs.isEmpty() && loadingOutputs.isNotEmpty() -> JLabel(
                "Generating assembly…",
                SwingConstants.CENTER
            )
            visibleOutputs.isEmpty() -> JLabel("Enable SASS or PTX to view generated assembly.", SwingConstants.CENTER)
            visibleOutputs.size == 1 -> visibleOutputs.single().panel
            tabs -> JTabbedPane().apply {
                visibleOutputs.forEach { addTab(it.data.kind.title, it.panel) }
            }
            else -> JSplitPane(JSplitPane.HORIZONTAL_SPLIT, visibleOutputs[0].panel, visibleOutputs[1].panel).apply {
                resizeWeight = 0.5
                dividerSize = 6
                isContinuousLayout = true
            }
        }
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourcePane, outputArea).apply {
            resizeWeight = 0.4
            dividerSize = 6
            isContinuousLayout = true
        }
        content.add(split, BorderLayout.CENTER)
        content.revalidate()
        content.repaint()
    }

    private fun detach(component: Component) {
        (component.parent as? Container)?.remove(component)
    }

    private fun refreshSourceBaseHighlights() {
        sourceBaseHighlighters.forEach(sourceEditor.markupModel::removeHighlighter)
        sourceBaseHighlighters.clear()
        val sourceLines = outputEditors.values
            .flatMap { it.data.references.map(SourceLineReference::sourceLine) }
            .toSet()
        sourceLines.forEach { line ->
            addLineHighlight(sourceEditor, line, lineAttributes(mappingColor(line)))?.let(sourceBaseHighlighters::add)
        }
        outputEditors.values.forEach(OutputEditor::addBaseHighlights)
    }

    private fun selectSourceLine(sourceLine: Int) {
        clearActiveHighlights()
        val mappedOutputs = enabledOutputs.mapNotNull { kind ->
            outputEditors[kind]?.let { it to it.outputLinesBySource[sourceLine].orEmpty() }
        }.filter { (_, lines) -> lines.isNotEmpty() }
        if (mappedOutputs.isEmpty()) return
        highlightActive(sourceEditor, sourceLine)
        mappedOutputs.forEach { (output, lines) ->
            lines.forEach { highlightActive(output.editor, it) }
            scrollToLine(output.editor, lines.first())
        }
    }

    private fun selectOutputLine(kind: AssemblyOutputKind, outputLine: Int) {
        clearActiveHighlights()
        val selectedOutput = outputEditors[kind] ?: return
        val sourceLine = selectedOutput.sourceLineByOutput[outputLine] ?: return
        highlightActive(sourceEditor, sourceLine)
        enabledOutputs.mapNotNull(outputEditors::get).forEach { output ->
            output.outputLinesBySource[sourceLine].orEmpty().forEach { highlightActive(output.editor, it) }
        }
        scrollToLine(sourceEditor, sourceLine)
    }

    private fun highlightActive(editor: Editor, line: Int) {
        val color = JBColor(Color(0xFFD875), Color(0x725A22))
        addLineHighlight(editor, line, lineAttributes(color))?.let { activeHighlighters.add(editor to it) }
    }

    private fun addLineHighlight(editor: Editor, oneBasedLine: Int, attributes: TextAttributes): RangeHighlighter? {
        val line = oneBasedLine - 1
        if (line !in 0 until editor.document.lineCount) return null
        return editor.markupModel.addLineHighlighter(line, HighlighterLayer.SELECTION + 1, attributes)
    }

    private fun lineAttributes(background: Color) = TextAttributes(null, background, null, null, Font.PLAIN)

    private fun scrollToLine(editor: Editor, oneBasedLine: Int) {
        val line = (oneBasedLine - 1).coerceIn(0, editor.document.lineCount - 1)
        editor.caretModel.moveToLogicalPosition(LogicalPosition(line, 0))
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    private fun clearActiveHighlights() {
        activeHighlighters.forEach { (editor, highlighter) -> editor.markupModel.removeHighlighter(highlighter) }
        activeHighlighters.clear()
    }

    private fun updateStatus(message: String?) {
        val fallback = when {
            enabledOutputs.isEmpty() -> "Select an assembly output."
            enabledOutputs.any { outputEditors[it]?.data?.references.isNullOrEmpty() } ->
                "Some generated instructions have no source line information."
            else -> "Click a source or assembly line to reveal its mapped counterpart."
        }
        status.text = message ?: fallback
    }

    private fun mappingColor(sourceLine: Int): Color =
        MAPPING_COLORS[(sourceLine - 1).mod(MAPPING_COLORS.size)]

    override fun dispose() {
        disposed = true
        sourceEditor.removeEditorMouseListener(sourceMouseListener)
        sourceBaseHighlighters.forEach(sourceEditor.markupModel::removeHighlighter)
        clearActiveHighlights()
        outputEditors.values.forEach(OutputEditor::dispose)
        EditorFactory.getInstance().releaseEditor(sourceEditor)
    }

    private inner class OutputEditor(val data: AssemblyPaneData) {
        val editor: Editor = EditorFactory.getInstance().createEditor(
            EditorFactory.getInstance().createDocument(data.text), project, data.fileType, true
        )
        val panel: JPanel = editorPane(data.title, editor.component)
        val outputLinesBySource = data.references.groupBy(SourceLineReference::sourceLine)
            .mapValues { (_, refs) -> refs.map(SourceLineReference::outputLine).distinct() }
        val sourceLineByOutput = data.references.associate { it.outputLine to it.sourceLine }
        private val baseHighlighters = mutableListOf<RangeHighlighter>()
        private val mouseListener = object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                if (event.isOverText) selectOutputLine(data.kind, event.logicalPosition.line + 1)
            }
        }

        init {
            editor.addEditorMouseListener(mouseListener)
            addBaseHighlights()
        }

        fun addBaseHighlights() {
            baseHighlighters.forEach(editor.markupModel::removeHighlighter)
            baseHighlighters.clear()
            data.references.forEach { reference ->
                addLineHighlight(editor, reference.outputLine, lineAttributes(mappingColor(reference.sourceLine)))
                    ?.let(baseHighlighters::add)
            }
        }

        fun dispose() {
            editor.removeEditorMouseListener(mouseListener)
            baseHighlighters.forEach(editor.markupModel::removeHighlighter)
            baseHighlighters.clear()
            clearHighlightersFor(editor)
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    private fun clearHighlightersFor(editor: Editor) {
        activeHighlighters.filter { it.first === editor }.forEach { (_, highlighter) ->
            editor.markupModel.removeHighlighter(highlighter)
        }
        activeHighlighters.removeAll { it.first === editor }
    }

    private companion object {
        const val TABS_WIDTH_THRESHOLD = 1180
        val MAPPING_COLORS = listOf(
            JBColor(Color(0xE6F4EA), Color(0x263A2D)),
            JBColor(Color(0xE8F0FE), Color(0x28364A)),
            JBColor(Color(0xFFF4D6), Color(0x44391F)),
            JBColor(Color(0xF3E8FD), Color(0x3D2D4C)),
            JBColor(Color(0xFDE8E7), Color(0x492C2A)),
            JBColor(Color(0xE0F7FA), Color(0x203A3F))
        )
    }
}
