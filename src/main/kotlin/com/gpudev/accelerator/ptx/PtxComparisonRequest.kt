package com.gpudev.accelerator.ptx

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.ComponentDiffRequest
import com.intellij.openapi.Disposable
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
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingConstants

/** Opens a source/PTX correspondence view inside the IDE's standard diff window. */
class PtxComparisonRequest(
    private val project: Project,
    private val sourceName: String,
    private val sourceText: String,
    private val ptxText: String,
    private val references: List<PtxLineReference>
) : ComponentDiffRequest() {
    private var panel: PtxComparisonPanel? = null

    override fun getTitle(): String = "$sourceName ↔ PTX"

    override fun getComponent(context: DiffContext): JPanel =
        panel?.component ?: PtxComparisonPanel(project, sourceName, sourceText, ptxText, references).also {
            panel = it
        }.component

    override fun onAssigned(isAssigned: Boolean) {
        super.onAssigned(isAssigned)
        if (!isAssigned) {
            panel?.let(Disposer::dispose)
            panel = null
        }
    }

    companion object {
        fun show(
            project: Project,
            sourceName: String,
            sourceText: String,
            ptxText: String,
            references: List<PtxLineReference>
        ) {
            DiffManager.getInstance().showDiff(
                project,
                PtxComparisonRequest(project, sourceName, sourceText, ptxText, references)
            )
        }
    }
}

private class PtxComparisonPanel(
    project: Project,
    sourceName: String,
    sourceText: String,
    ptxText: String,
    references: List<PtxLineReference>
) : JPanel(BorderLayout()), Disposable {
    private val sourceEditor: Editor
    private val ptxEditor: Editor
    private val sourceBaseHighlighters = mutableListOf<RangeHighlighter>()
    private val ptxBaseHighlighters = mutableListOf<RangeHighlighter>()
    private val activeHighlighters = mutableListOf<Pair<Editor, RangeHighlighter>>()
    private val ptxLinesBySource = references.groupBy(PtxLineReference::sourceLine)
        .mapValues { (_, values) -> values.map(PtxLineReference::ptxLine).distinct() }
    private val sourceLineByPtx = references.associate { it.ptxLine to it.sourceLine }
    private val sourceMouseListener = object : EditorMouseListener {
        override fun mouseClicked(event: EditorMouseEvent) {
            if (event.isOverText) selectSourceLine(event.logicalPosition.line + 1)
        }
    }
    private val ptxMouseListener = object : EditorMouseListener {
        override fun mouseClicked(event: EditorMouseEvent) {
            if (event.isOverText) selectPtxLine(event.logicalPosition.line + 1)
        }
    }

    val component: JPanel get() = this

    init {
        val editorFactory = EditorFactory.getInstance()
        sourceEditor = editorFactory.createEditor(
            editorFactory.createDocument(sourceText), project, sourceFileType(), true
        )
        ptxEditor = editorFactory.createEditor(
            editorFactory.createDocument(ptxText), project, PtxFileType.INSTANCE, true
        )

        val sourcePane = editorPane(sourceName, sourceEditor.component)
        val ptxPane = editorPane("PTX", ptxEditor.component)
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourcePane, ptxPane).apply {
            resizeWeight = 0.5
            dividerSize = 6
            isContinuousLayout = true
        }
        add(JLabel(if (references.isEmpty()) {
            "PTX source line metadata is unavailable for this file; generated instructions are not mapped."
        } else {
            "Mapped PTX lines are color-coded by source line. Click a mapped line to highlight " +
                "and reveal its counterpart."
        }).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)
        }, BorderLayout.NORTH)
        add(split, BorderLayout.CENTER)
        preferredSize = java.awt.Dimension(1100, 700)

        addBaseMappingColors()
        sourceEditor.addEditorMouseListener(sourceMouseListener)
        ptxEditor.addEditorMouseListener(ptxMouseListener)
    }

    private fun sourceFileType(): FileType = com.gpudev.accelerator.cuda.CudaFileType.INSTANCE

    private fun editorPane(title: String, editor: javax.swing.JComponent): JPanel = JPanel(BorderLayout()).apply {
        add(JLabel(title, SwingConstants.LEADING).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)
            isOpaque = true
            background = EditorColorsManager.getInstance().globalScheme.defaultBackground
            foreground = EditorColorsManager.getInstance().globalScheme.defaultForeground
        }, BorderLayout.NORTH)
        add(editor, BorderLayout.CENTER)
    }

    private fun addBaseMappingColors() {
        val colors = listOf(
            JBColor(Color(0xE6F4EA), Color(0x263A2D)),
            JBColor(Color(0xE8F0FE), Color(0x28364A)),
            JBColor(Color(0xFFF4D6), Color(0x44391F)),
            JBColor(Color(0xF3E8FD), Color(0x3D2D4C)),
            JBColor(Color(0xFDE8E7), Color(0x492C2A)),
            JBColor(Color(0xE0F7FA), Color(0x203A3F))
        )
        ptxLinesBySource.entries.sortedBy { it.key }.forEachIndexed { index, (sourceLine, ptxLines) ->
            val attributes = lineAttributes(colors[index % colors.size])
            addLineHighlight(sourceEditor, sourceLine, attributes)?.let(sourceBaseHighlighters::add)
            ptxLines.forEach { line -> addLineHighlight(ptxEditor, line, attributes)?.let(ptxBaseHighlighters::add) }
        }
    }

    private fun selectSourceLine(sourceLine: Int) {
        val ptxLines = ptxLinesBySource[sourceLine].orEmpty()
        clearActiveHighlights()
        if (ptxLines.isEmpty()) return
        highlightActive(sourceEditor, sourceLine)
        ptxLines.forEach { highlightActive(ptxEditor, it) }
        scrollToLine(ptxEditor, ptxLines.first())
    }

    private fun selectPtxLine(ptxLine: Int) {
        val sourceLine = sourceLineByPtx[ptxLine]
        clearActiveHighlights()
        if (sourceLine == null) return
        highlightActive(ptxEditor, ptxLine)
        highlightActive(sourceEditor, sourceLine)
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

    override fun dispose() {
        sourceEditor.removeEditorMouseListener(sourceMouseListener)
        ptxEditor.removeEditorMouseListener(ptxMouseListener)
        sourceBaseHighlighters.forEach(sourceEditor.markupModel::removeHighlighter)
        ptxBaseHighlighters.forEach(ptxEditor.markupModel::removeHighlighter)
        clearActiveHighlights()
        EditorFactory.getInstance().releaseEditor(sourceEditor)
        EditorFactory.getInstance().releaseEditor(ptxEditor)
    }
}
