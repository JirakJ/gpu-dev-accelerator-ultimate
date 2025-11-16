package com.gpudev.accelerator.premium

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.license.LicenseManager

@Service(Service.Level.PROJECT)
class PremiumFeatureGate(private val project: Project) {
    
    enum class Feature(val displayName: String) {
        ADVANCED_DEBUGGING("Advanced Debugging"),
        REAL_TIME_PROFILING("Real-time Profiling"),
        AI_OPTIMIZATION("AI-Powered Optimization"),
        MULTI_GPU_VISUALIZATION("Multi-GPU Visualization"),
        MEMORY_ANALYSIS("Advanced Memory Analysis"),
        PERFORMANCE_INSIGHTS("Performance Insights"),
        TEAM_COLLABORATION("Team Collaboration"),
        CUSTOM_KERNELS("Custom Kernel Library"),
        CLOUD_COMPILATION("Cloud Compilation"),
        PRIORITY_SUPPORT("Priority Support")
    }
    
    fun checkFeature(feature: Feature): Boolean {
        val licenseManager = service<LicenseManager>()
        return if (licenseManager.hasValidLicense()) {
            true
        } else {
            showUpgradeDialog(feature)
            false
        }
    }
    
    private fun showUpgradeDialog(feature: Feature) {
        val message = """
            <html>
            <h3>${feature.displayName} is a Premium Feature</h3>
            <p>Upgrade to <b>GPU Dev Accelerator Ultimate PRO</b> to unlock:</p>
            <ul>
                <li>Advanced debugging with cuda-gdb integration</li>
                <li>Real-time performance profiling</li>
                <li>AI-powered kernel optimization</li>
                <li>Multi-GPU topology visualization</li>
                <li>Advanced memory leak detection</li>
                <li>Priority support & updates</li>
            </ul>
            <br/>
            <p><b>Individual:</b> $$29.99/month or $$299/year</p>
            <p><b>Enterprise:</b> Starting at $$999/year per seat</p>
            </html>
        """.trimIndent()
        
        val result = Messages.showDialog(
            project,
            message,
            "Premium Feature Required",
            arrayOf("Upgrade Now", "Learn More", "Cancel"),
            0,
            Messages.getInformationIcon()
        )
        
        when (result) {
            0 -> openUpgradePage()
            1 -> openLearnMorePage()
        }
    }
    
    private fun openUpgradePage() {
        com.intellij.ide.BrowserUtil.browse("https://plugins.jetbrains.com/plugin/PLUGIN_ID/pricing")
    }
    
    private fun openLearnMorePage() {
        com.intellij.ide.BrowserUtil.browse("https://jakubjirak.com/gpu-dev-accelerator")
    }
    
    companion object {
        fun getInstance(project: Project): PremiumFeatureGate = project.service()
        
        fun isPremiumEnabled(project: Project): Boolean {
            return getInstance(project).checkFeature(Feature.ADVANCED_DEBUGGING)
        }
    }
}
