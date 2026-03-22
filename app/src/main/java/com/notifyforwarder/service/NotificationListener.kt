package com.notifyforwarder.service

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.notifyforwarder.App
import com.notifyforwarder.R
import com.notifyforwarder.manager.AppManager
import java.util.concurrent.ConcurrentHashMap

class NotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotifyForwarder"
        private const val FOREGROUND_ID = 1001
        private var isServiceRunning = false
        private val appNameCache = ConcurrentHashMap<String, String>()
        private var debugMode = true
        private const val MAX_CACHE_SIZE = 100
        private val handler = Handler(Looper.getMainLooper())
        
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val cn = ComponentName(context, NotificationListener::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }
        
        fun setDebugMode(enabled: Boolean) {
            debugMode = enabled
        }
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceRunning = true
        startForegroundNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, NotificationListener::class.java))
        }
        
        Log.d(TAG, "NotificationListener connected")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            processNotification(sbn)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification: ${e.message}")
        }
    }
    
    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val userId = getUserId(sbn)
        
        Log.d(TAG, "Notification received: $packageName, userId=$userId")
        
        if (packageName == "com.notifyforwarder") {
            return
        }
        
        val appKey = "${packageName}_$userId"
        
        if (userId != 0) {
            AppManager.registerDualApp(applicationContext, packageName, userId)
        }
        
        val isEnabled = AppManager.isEnabled(appKey)
        Log.d(TAG, "Checking appKey=$appKey, isEnabled=$isEnabled, allEnabled=${AppManager.getEnabledApps()}")
        
        if (!isEnabled) {
            Log.d(TAG, "App not enabled, skipping")
            return
        }
        
        val notification = sbn.notification
        val flags = notification.flags
        
        if ((flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            Log.d(TAG, "Skipping ongoing event")
            return
        }
        if ((flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) {
            Log.d(TAG, "Skipping foreground service")
            return
        }
        
        Log.d(TAG, "Forwarding notification from $packageName")
        forwardNotification(sbn)
    }
    
    private fun forwardNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
        val content = if (bigText.isNotEmpty()) bigText else text
        
        if (title.isEmpty() && content.isEmpty()) {
            Log.d(TAG, "Empty notification content, skipping")
            return
        }
        
        val appName = getAppName(sbn.packageName)
        
        val forwardNotification = NotificationCompat.Builder(this, App.CHANNEL_ID_FORWARD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("[$appName] $title")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = (sbn.packageName.hashCode() + sbn.id).let { if (it < 0) -it else it }
        
        manager.notify(id, forwardNotification)
        
        Log.d(TAG, "Notification forwarded: [$appName] $title")
        
        handler.post {
            Toast.makeText(applicationContext, "转发: $appName", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getAppName(packageName: String): String {
        return appNameCache.getOrPut(packageName) {
            if (appNameCache.size > MAX_CACHE_SIZE) {
                appNameCache.clear()
            }
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast(".")
            }
        }
    }
    
    private fun getUserId(sbn: StatusBarNotification): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val user = sbn.user
                val method = UserHandle::class.java.getDeclaredMethod("getIdentifier")
                method.invoke(user) as Int
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, App.CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("通知转发服务")
            .setContentText("服务运行中")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(FOREGROUND_ID, notification)
            }
        } catch (e: Exception) {
            try {
                startForeground(FOREGROUND_ID, notification)
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }
    
    override fun onListenerDisconnected() {
        isServiceRunning = false
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener disconnected")
    }
}
