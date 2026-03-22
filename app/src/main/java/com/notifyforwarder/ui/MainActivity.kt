package com.notifyforwarder.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.notifyforwarder.databinding.ActivityMainBinding
import com.notifyforwarder.manager.AppManager
import com.notifyforwarder.service.NotificationListener

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_NOTIFICATION_PERMISSION = 1001
    
    companion object {
        private const val PREFS_NAME = "notify_forwarder_prefs"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        binding.btnGrantPermission.setOnClickListener {
            openNotificationSettings()
        }
        
        binding.btnAppManager.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }
        
        binding.btnOpenMiHealth.setOnClickListener {
            openMiHealth()
        }
        
        requestNotificationPermission()
        checkAndRequestOptimizations()
    }
    
    override fun onResume() {
        super.onResume()
        refreshStatus()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!manager.areNotificationsEnabled()) {
                Toast.makeText(this, "请开启应用通知权限", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
        }
    }
    
    private fun refreshStatus() {
        val cn = ComponentName(this, NotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val hasPermission = flat?.contains(cn.flattenToString()) == true
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            val notificationsEnabled = manager.areNotificationsEnabled()
            
            if (!notificationsEnabled) {
                binding.permissionCard.visibility = View.VISIBLE
                binding.contentLayout.visibility = View.GONE
                Toast.makeText(this, "请先开启应用通知权限", Toast.LENGTH_LONG).show()
                return
            }
        }
        
        if (hasPermission) {
            binding.permissionCard.visibility = View.GONE
            binding.contentLayout.visibility = View.VISIBLE
            
            AppManager.init(this)
            val enabledCount = AppManager.getAppList().count { it.isForwardEnabled }
            binding.tvEnabledCount.text = enabledCount.toString()
            
            binding.tvServiceStatus.text = "服务运行中"
            binding.tvServiceStatus.setTextColor(getColor(com.notifyforwarder.R.color.success))
        } else {
            binding.permissionCard.visibility = View.VISIBLE
            binding.contentLayout.visibility = View.GONE
        }
    }
    
    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "请找到「通知转发助手」并开启权限", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
    
    private fun openMiHealth() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.mi.health")
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "未找到小米运动健康应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkAndRequestOptimizations() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        
        if (setupCompleted) {
            return
        }
        
        var needBatteryOpt = false
        var needAutoStart = false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                needBatteryOpt = true
            }
        }
        
        if (!needBatteryOpt && !needAutoStart) {
            prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
            return
        }
        
        if (needBatteryOpt) {
            requestBatteryOptimization()
        }
    }
    
    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                    Toast.makeText(this, "请允许忽略电池优化以保持服务运行", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    } catch (e2: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }
    
    fun openAutoStartSettings(view: android.view.View) {
        try {
            val intent = Intent()
            intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            startActivity(intent)
            Toast.makeText(this, "请开启通知转发助手的自启动权限", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                val intent = Intent("miui.intent.action.OP_AUTO_START")
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "请在系统设置中开启自启动权限", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
                refreshStatus()
            }
        }
    }
}
