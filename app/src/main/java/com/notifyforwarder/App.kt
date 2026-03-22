package com.notifyforwarder

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

class App : Application() {
    
    companion object {
        const val CHANNEL_ID_FORWARD = "notify_forward"
        const val CHANNEL_ID_SERVICE = "notify_service"
        private const val TAG = "NotifyForwarder"
        private var debugMode = false
        
        fun setDebugMode(enabled: Boolean) {
            debugMode = enabled
        }
        
        fun isDebugMode(): Boolean = debugMode
    }
    
    override fun onCreate() {
        super.onCreate()
        if (debugMode) Log.d(TAG, "App.onCreate()")
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val forwardChannel = NotificationChannel(
                CHANNEL_ID_FORWARD,
                "通知转发",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "转发来自其他应用的通知到小米运动健康"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "服务状态",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "前台服务状态通知"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            
            manager.createNotificationChannel(forwardChannel)
            manager.createNotificationChannel(serviceChannel)
            
            if (debugMode) {
                Log.d(TAG, "Notification channels created")
                val channels = manager.notificationChannels
                for (ch in channels) {
                    Log.d(TAG, "  Channel: ${ch.id}, importance=${ch.importance}")
                }
            }
        }
    }
}
