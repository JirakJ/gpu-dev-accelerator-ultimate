package com.gpudev.accelerator.settings

import com.intellij.openapi.options.Configurable
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridLayout

class GPUDevSettingsConfigurable : Configurable {
    private var settingsPanel: SettingsPanel? = null
    
    override fun createComponent(): JComponent {
        settingsPanel = SettingsPanel()
        return settingsPanel!!
    }
    
    override fun isModified(): Boolean = settingsPanel?.isModified() ?: false
    
    override fun apply() {
        settingsPanel?.apply()
    }
    
    override fun getDisplayName(): String = "GPU Dev Accelerator"
}

class SettingsPanel : JPanel(BorderLayout()) {
    
    init {
        val mainPanel = JPanel(GridLayout(0, 1, 10, 10))
        
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.add(JLabel("GPU Dev Accelerator Ultimate"))
        infoPanel.add(JLabel(" "))
        infoPanel.add(JLabel("All features are enabled."))
        
        mainPanel.add(infoPanel)
        
        add(mainPanel, BorderLayout.NORTH)
    }
    
    fun isModified(): Boolean = false
    
    fun apply() {}
}
