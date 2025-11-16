package com.gpudev.accelerator.license

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@State(
    name = "GPUDevLicenseManager",
    storages = [Storage("gpudev-license.xml")]
)
class LicenseManager : PersistentStateComponent<LicenseManager.State> {
    
    data class State(
        var licenseKey: String = "",
        var licenseType: LicenseType = LicenseType.FREE,
        var activationDate: String = "",
        var expirationDate: String = "",
        var companyName: String = "",
        var userName: String = ""
    )
    
    enum class LicenseType {
        FREE,           // Basic features only
        INDIVIDUAL,     // $200-400/year - Advanced debugging, profiling
        ENTERPRISE      // $500-2000/year - Full features + team collaboration
    }
    
    enum class Feature {
        SYNTAX_HIGHLIGHTING,
        BASIC_COMPLETION,
        BASIC_COMPILATION,
        CUDA_DEBUGGING,
        METAL_DEBUGGING,
        ADVANCED_PROFILING,
        AI_OPTIMIZATION,
        MULTI_GPU_VISUALIZATION,
        MEMORY_ANALYSIS,
        TEAM_COLLABORATION,
        KERNEL_OPTIMIZATION,
        NSIGHT_INTEGRATION
    }
    
    private var state = State()
    
    override fun getState(): State = state
    
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }
    
    companion object {
        fun getInstance(): LicenseManager {
            return ApplicationManager.getApplication().getService(LicenseManager::class.java)
        }
    }
    
    fun isFeatureEnabled(feature: Feature): Boolean {
        return when (state.licenseType) {
            LicenseType.FREE -> feature in listOf(
                Feature.SYNTAX_HIGHLIGHTING,
                Feature.BASIC_COMPLETION,
                Feature.BASIC_COMPILATION
            )
            LicenseType.INDIVIDUAL -> feature != Feature.TEAM_COLLABORATION
            LicenseType.ENTERPRISE -> true
        }
    }
    
    fun activateLicense(key: String): ActivationResult {
        // License key format: GPUDEV-TYPE-XXXXXX-XXXXXX
        val parts = key.split("-")
        if (parts.size != 4 || parts[0] != "GPUDEV") {
            return ActivationResult.INVALID_KEY
        }
        
        val type = when (parts[1]) {
            "IND" -> LicenseType.INDIVIDUAL
            "ENT" -> LicenseType.ENTERPRISE
            else -> return ActivationResult.INVALID_KEY
        }
        
        state.licenseKey = key
        state.licenseType = type
        state.activationDate = LocalDate.now().toString()
        state.expirationDate = LocalDate.now().plusYears(1).toString()
        
        return ActivationResult.SUCCESS
    }
    
    fun isLicenseValid(): Boolean {
        if (state.licenseType == LicenseType.FREE) return true
        if (state.expirationDate.isEmpty()) return false
        
        val expirationDate = LocalDate.parse(state.expirationDate)
        return LocalDate.now().isBefore(expirationDate)
    }
    
    fun getDaysUntilExpiration(): Long {
        if (state.licenseType == LicenseType.FREE) return Long.MAX_VALUE
        if (state.expirationDate.isEmpty()) return 0
        
        val expirationDate = LocalDate.parse(state.expirationDate)
        return ChronoUnit.DAYS.between(LocalDate.now(), expirationDate)
    }
    
    fun getLicenseInfo(): String {
        return when (state.licenseType) {
            LicenseType.FREE -> "Free License - Limited Features"
            LicenseType.INDIVIDUAL -> {
                val days = getDaysUntilExpiration()
                "Individual License - $days days remaining"
            }
            LicenseType.ENTERPRISE -> {
                val days = getDaysUntilExpiration()
                "Enterprise License - $days days remaining"
            }
        }
    }
    
    enum class ActivationResult {
        SUCCESS,
        INVALID_KEY,
        EXPIRED,
        NETWORK_ERROR
    }
}
