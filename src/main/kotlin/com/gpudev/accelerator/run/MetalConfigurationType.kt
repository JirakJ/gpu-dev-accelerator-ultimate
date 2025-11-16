package com.gpudev.accelerator.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import javax.swing.Icon

class MetalConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Metal Application"
    
    override fun getConfigurationTypeDescription(): String = "Metal shader run configuration"
    
    override fun getIcon(): Icon? = null
    
    override fun getId(): String = "METAL_RUN_CONFIGURATION"
    
    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(MetalConfigurationFactory(this))
    }
}

class MetalConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "METAL_FACTORY"
    
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return MetalRunConfiguration(project, this, "Metal")
    }
    
    override fun getName(): String = "Metal Application"
}

class MetalRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : com.intellij.execution.configurations.RunConfigurationBase<Any>(project, factory, name) {
    
    override fun getConfigurationEditor() = MetalSettingsEditor()
    
    override fun getState(
        executor: com.intellij.execution.Executor,
        environment: com.intellij.execution.runners.ExecutionEnvironment
    ): com.intellij.execution.configurations.RunProfileState? {
        return null
    }
}

class MetalSettingsEditor : com.intellij.openapi.options.SettingsEditor<MetalRunConfiguration>() {
    private val panel = javax.swing.JPanel()
    
    override fun resetEditorFrom(s: MetalRunConfiguration) {}
    
    override fun applyEditorTo(s: MetalRunConfiguration) {}
    
    override fun createEditor(): javax.swing.JComponent = panel
}
