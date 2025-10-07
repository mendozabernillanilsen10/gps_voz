package com.example.demoappchat.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Manager for handling device-specific compatibility and optimizations
 * Supports multiple manufacturers with aggressive battery optimization: Honor, Huawei, Xiaomi, Oppo, Vivo, Samsung
 */
object DeviceCompatibilityManager {
    
    private const val TAG = "DeviceCompat"
    
    enum class DeviceManufacturer {
        HONOR,
        HUAWEI,
        XIAOMI,
        OPPO,
        VIVO,
        SAMSUNG,
        ONEPLUS,
        REALME,
        GENERIC
    }
    
    data class DeviceConfig(
        val manufacturer: DeviceManufacturer,
        val requiresAggressiveOptimization: Boolean,
        val checkInterval: Int,
        val recognitionTimeout: Int,
        val confidenceThreshold: Float,
        val requiresSpecialPermissions: Boolean
    )
    
    /**
     * Detect current device manufacturer
     */
    fun getDeviceManufacturer(): DeviceManufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return when {
            manufacturer.contains("honor") || brand.contains("honor") -> DeviceManufacturer.HONOR
            manufacturer.contains("huawei") || brand.contains("huawei") -> DeviceManufacturer.HUAWEI
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || 
            brand.contains("redmi") || brand.contains("poco") -> DeviceManufacturer.XIAOMI
            manufacturer.contains("oppo") || brand.contains("oppo") -> DeviceManufacturer.OPPO
            manufacturer.contains("vivo") || brand.contains("vivo") -> DeviceManufacturer.VIVO
            manufacturer.contains("samsung") -> DeviceManufacturer.SAMSUNG
            manufacturer.contains("oneplus") -> DeviceManufacturer.ONEPLUS
            manufacturer.contains("realme") -> DeviceManufacturer.REALME
            else -> DeviceManufacturer.GENERIC
        }
    }
    
    /**
     * Get device-specific configuration
     */
    fun getDeviceConfig(): DeviceConfig {
        val manufacturer = getDeviceManufacturer()
        
        return when (manufacturer) {
            DeviceManufacturer.HONOR, DeviceManufacturer.HUAWEI -> DeviceConfig(
                manufacturer = manufacturer,
                requiresAggressiveOptimization = true,
                checkInterval = 3000,
                recognitionTimeout = 10000,
                confidenceThreshold = 0.6f,
                requiresSpecialPermissions = true
            )
            
            DeviceManufacturer.XIAOMI -> DeviceConfig(
                manufacturer = manufacturer,
                requiresAggressiveOptimization = true,
                checkInterval = 2500,
                recognitionTimeout = 8000,
                confidenceThreshold = 0.65f,
                requiresSpecialPermissions = true
            )
            
            DeviceManufacturer.OPPO, DeviceManufacturer.VIVO, 
            DeviceManufacturer.REALME -> DeviceConfig(
                manufacturer = manufacturer,
                requiresAggressiveOptimization = true,
                checkInterval = 2000,
                recognitionTimeout = 7000,
                confidenceThreshold = 0.65f,
                requiresSpecialPermissions = true
            )
            
            DeviceManufacturer.SAMSUNG -> DeviceConfig(
                manufacturer = manufacturer,
                requiresAggressiveOptimization = false,
                checkInterval = 1500,
                recognitionTimeout = 5000,
                confidenceThreshold = 0.7f,
                requiresSpecialPermissions = false
            )
            
            DeviceManufacturer.ONEPLUS -> DeviceConfig(
                manufacturer = manufacturer,
                requiresAggressiveOptimization = false,
                checkInterval = 1500,
                recognitionTimeout = 5000,
                confidenceThreshold = 0.7f,
                requiresSpecialPermissions = false
            )
            
            else -> DeviceConfig(
                manufacturer = manufacturer,
                requiresAggressiveOptimization = false,
                checkInterval = 1000,
                recognitionTimeout = 5000,
                confidenceThreshold = 0.7f,
                requiresSpecialPermissions = false
            )
        }
    }
    
    /**
     * Open device-specific settings for battery optimization
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val manufacturer = getDeviceManufacturer()
        
        val intents = when (manufacturer) {
            DeviceManufacturer.HONOR, DeviceManufacturer.HUAWEI -> listOf(
                createIntent("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
                createIntent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                createIntent("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity")
            )
            
            DeviceManufacturer.XIAOMI -> listOf(
                createIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                createIntent("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"),
                createIntent("com.xiaomi.smarthome", "com.xiaomi.smarthome.settings.SettingsActivity")
            )
            
            DeviceManufacturer.OPPO -> listOf(
                createIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                createIntent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                createIntent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            )
            
            DeviceManufacturer.VIVO -> listOf(
                createIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
                createIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                createIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
            )
            
            DeviceManufacturer.SAMSUNG -> listOf(
                createIntent("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            )
            
            else -> emptyList()
        }
        
        for (intent in intents) {
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    Log.d(TAG, "✅ Opened $manufacturer battery optimization settings")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open intent: ${intent.component?.className}", e)
                }
            }
        }
        
        Log.w(TAG, "Could not open $manufacturer specific settings")
        return false
    }
    
    /**
     * Create intent for specific activity
     */
    private fun createIntent(packageName: String, className: String): Intent {
        return Intent().apply {
            component = android.content.ComponentName(packageName, className)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    /**
     * Get device info string
     */
    fun getDeviceInfo(): String {
        return "${Build.BRAND} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    }
    
    /**
     * Log device information
     */
    fun logDeviceInfo() {
        val config = getDeviceConfig()
        Log.i(TAG, """
            |========================================
            |Device Information:
            |  Manufacturer: ${config.manufacturer}
            |  Model: ${Build.MODEL}
            |  Brand: ${Build.BRAND}
            |  Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            |  Requires Aggressive Optimization: ${config.requiresAggressiveOptimization}
            |  Check Interval: ${config.checkInterval}ms
            |  Recognition Timeout: ${config.recognitionTimeout}ms
            |  Confidence Threshold: ${config.confidenceThreshold}
            |  Requires Special Permissions: ${config.requiresSpecialPermissions}
            |========================================
        """.trimMargin())
    }
    
    /**
     * Apply device-specific optimizations to SharedPreferences
     */
    fun applyOptimizations(context: Context) {
        val config = getDeviceConfig()
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        
        prefs.edit().apply {
            putString("device_manufacturer", config.manufacturer.name)
            putString("device_model", Build.MODEL)
            putBoolean("requires_aggressive_optimization", config.requiresAggressiveOptimization)
            putInt("check_interval", config.checkInterval)
            putInt("recognition_timeout", config.recognitionTimeout)
            putFloat("confidence_threshold", config.confidenceThreshold)
            putBoolean("requires_special_permissions", config.requiresSpecialPermissions)
            putLong("last_config_update", System.currentTimeMillis())
        }.apply()
        
        Log.d(TAG, "✅ Applied optimizations for ${config.manufacturer}")
    }
}

