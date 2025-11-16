package com.gpudev.accelerator.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.license.LicenseManager
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
    private val licenseManager = LicenseManager.getInstance()
    private val licenseKeyField = JTextField(20)
    private val activateButton = JButton("Activate License")
    private val statusLabel = JLabel()
    
    init {
        val mainPanel = JPanel(GridLayout(0, 1, 10, 10))
        
        // License section
        val licensePanel = JPanel()
        licensePanel.add(JLabel("License Key:"))
        licensePanel.add(licenseKeyField)
        licensePanel.add(activateButton)
        
        mainPanel.add(licensePanel)
        mainPanel.add(statusLabel)
        
        updateStatus()
        
        activateButton.addActionListener {
            val key = licenseKeyField.text.trim()
            if (key.isEmpty()) {
                Messages.showErrorDialog("Please enter a license key", "Invalid Input")
                return@addActionListener
            }
            
            when (licenseManager.activateLicense(key)) {
                LicenseManager.ActivationResult.SUCCESS -> {
                    Messages.showInfoMessage(
                        "License activated successfully!\n\n${licenseManager.getLicenseInfo()}",
                        "Success"
                    )
                    updateStatus()
                }
                LicenseManager.ActivationResult.INVALID_KEY -> {
                    Messages.showErrorDialog("Invalid license key format", "Activation Failed")
                }
                else -> {
                    Messages.showErrorDialog("License activation failed", "Error")
                }
            }
        }
        
        // Info section
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.add(JLabel("GPU Dev Accelerator Ultimate"))
        infoPanel.add(JLabel(" "))
        infoPanel.add(JLabel("License Plans:"))
        infoPanel.add(JLabel("• Individual: $200-400/year"))
        infoPanel.add(JLabel("• Enterprise: $500-2000/year"))
        infoPanel.add(JLabel(" "))
        infoPanel.add(JLabel("Visit https://gpudev.com for licensing"))
        
        mainPanel.add(infoPanel)
        
        add(mainPanel, BorderLayout.NORTH)
    }
    
    private fun updateStatus() {
        val info = licenseManager.getLicenseInfo()
        statusLabel.text = "Current Status: $info"
    }
    
    fun isModified(): Boolean = false
    
    fun apply() {}
}
