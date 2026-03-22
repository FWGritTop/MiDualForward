package com.notifyforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotifyForwarder"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "=== BootReceiver.onReceive ===")
        Log.d(TAG, "  Action: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "  -> Boot completed, requesting notification listener restart")
        }
    }
}
