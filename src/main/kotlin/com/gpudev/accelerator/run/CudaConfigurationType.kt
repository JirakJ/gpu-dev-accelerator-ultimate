package com.gpudev.accelerator.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import javax.swing.Icon

class CudaConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "CUDA Application"
    
    override fun getConfigurationTypeDescription(): String = "CUDA application run configuration"
    
    override fun getIcon(): Icon? = null
    
    override fun getId(): String = "CUDA_RUN_CONFIGURATION"
    
    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(CudaConfigurationFactory(this))
    }
}

class CudaConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "CUDA_FACTORY"
    
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return CudaRunConfiguration(project, this, "CUDA")
    }
    
    override fun getName(): String = "CUDA Application"
}

class CudaRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : com.intellij.execution.configurations.RunConfigurationBase<Any>(project, factory, name) {
    
    override fun getConfigurationEditor() = CudaSettingsEditor()
    
    override fun getState(
        executor: com.intellij.execution.Executor,
        environment: com.intellij.execution.runners.ExecutionEnvironment
    ): com.intellij.execution.configurations.RunProfileState? {
        return null
    }
}

class CudaSettingsEditor : com.intellij.openapi.options.SettingsEditor<CudaRunConfiguration>() {
    private val panel = javax.swing.JPanel()
    
    override fun resetEditorFrom(s: CudaRunConfiguration) {}
    
    override fun applyEditorTo(s: CudaRunConfiguration) {}
    
    override fun createEditor(): javax.swing.JComponent = panel
}
