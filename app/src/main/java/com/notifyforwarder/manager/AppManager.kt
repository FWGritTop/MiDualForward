package com.notifyforwarder.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.notifyforwarder.model.AppInfo

object AppManager {
    
    private const val TAG = "NotifyForwarder"
    private const val PREFS_NAME = "forward_settings"
    private const val KEY_ENABLED_APPS = "enabled_apps"
    
    private val appList = mutableListOf<AppInfo>()
    private val enabledApps = mutableSetOf<String>()
    private var isLoaded = false
    private var debugMode = false
    
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
    }
    
    fun init(context: Context) {
        if (isLoaded) return
        if (debugMode) Log.d(TAG, "AppManager.init()")
        loadEnabledApps(context)
        loadApps(context)
        scanDualApps(context)
        isLoaded = true
    }
    
    private fun loadApps(context: Context) {
        appList.clear()
        
        val pm = context.packageManager
        
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
        
        val addedPackages = mutableSetOf<String>()
        
        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            
            if (packageName == "com.notifyforwarder") continue
            if (packageName in addedPackages) continue
            
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                
                if (appName.isBlank()) continue
                
                addedPackages.add(packageName)
                
                val appKey = "${packageName}_0"
                val isEnabled = enabledApps.contains(appKey)
                val app = AppInfo(
                    packageName = packageName,
                    userId = 0,
                    displayName = appName,
                    isDualApp = false,
                    isForwardEnabled = isEnabled
                )
                appList.add(app)
            } catch (e: Exception) {
                if (debugMode) Log.e(TAG, "Error loading $packageName: ${e.message}")
            }
        }
        
        appList.sortBy { it.displayName }
    }
    
    private fun scanDualApps(context: Context) {
        try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            if (userManager == null) return
            
            val userHandles = try {
                val method = UserManager::class.java.getDeclaredMethod("getUserProfiles")
                @Suppress("UNCHECKED_CAST")
                method.invoke(userManager) as? List<UserHandle> ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            
            for (userHandle in userHandles) {
                try {
                    val userId = getUserId(userHandle)
                    
                    if (userId == 0) continue
                    
                    val pm = context.packageManager
                    val mainIntent = Intent(Intent.ACTION_MAIN, null)
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    
                    val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.queryIntentActivities(mainIntent, 0)
                    }
                    
                    for (info in resolveInfos) {
                        val packageName = info.activityInfo.packageName
                        if (packageName == "com.notifyforwarder") continue
                        
                        val appKey = "${packageName}_$userId"
                        if (appList.any { it.appKey == appKey }) continue
                        
                        val appName = try {
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            pm.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            packageName.substringAfterLast(".")
                        }
                        
                        val isEnabled = enabledApps.contains(appKey)
                        val app = AppInfo(
                            packageName = packageName,
                            userId = userId,
                            displayName = "$appName (双开)",
                            isDualApp = true,
                            isForwardEnabled = isEnabled
                        )
                        appList.add(app)
                    }
                } catch (e: Exception) {
                    if (debugMode) Log.e(TAG, "Error scanning user profile: ${e.message}")
                }
            }
            
            appList.sortBy { it.displayName }
        } catch (e: Exception) {
            if (debugMode) Log.e(TAG, "Error scanning dual apps: ${e.message}")
        }
    }
    
    private fun getUserId(userHandle: UserHandle): Int {
        return try {
            val method = UserHandle::class.java.getDeclaredMethod("getIdentifier")
            method.invoke(userHandle) as Int
        } catch (e: Exception) {
            0
        }
    }
    
    private fun loadEnabledApps(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        enabledApps.clear()
        val saved = prefs.getStringSet(KEY_ENABLED_APPS, emptySet()) ?: emptySet()
        enabledApps.addAll(saved)
        if (debugMode) Log.d(TAG, "Loaded enabled apps: $enabledApps")
    }
    
    private fun saveEnabledApps(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ENABLED_APPS, enabledApps).apply()
    }
    
    fun getAppList(): List<AppInfo> = appList.toList()
    
    fun isEnabled(appKey: String): Boolean = enabledApps.contains(appKey)
    
    fun setEnabled(context: Context, appKey: String, enabled: Boolean) {
        if (enabled) {
            enabledApps.add(appKey)
        } else {
            enabledApps.remove(appKey)
        }
        saveEnabledApps(context)
        
        val index = appList.indexOfFirst { it.appKey == appKey }
        if (index >= 0) {
            appList[index].isForwardEnabled = enabled
        }
        
        if (debugMode) Log.d(TAG, "setEnabled: $appKey = $enabled, enabledApps=$enabledApps")
    }
    
    fun registerDualApp(context: Context, packageName: String, userId: Int): Boolean {
        val appKey = "${packageName}_$userId"
        
        if (appList.any { it.appKey == appKey }) return false
        
        val pm = context.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
        
        val isEnabled = enabledApps.contains(appKey)
        val app = AppInfo(
            packageName = packageName,
            userId = userId,
            displayName = "$appName (双开)",
            isDualApp = true,
            isForwardEnabled = isEnabled
        )
        appList.add(app)
        appList.sortBy { it.displayName }
        
        return true
    }
    
    fun clear() {
        appList.clear()
        isLoaded = false
    }
    
    fun reload(context: Context) {
        isLoaded = false
        init(context)
    }
    
    fun getEnabledCount(): Int = enabledApps.size
    
    fun getEnabledApps(): Set<String> = enabledApps.toSet()
}
