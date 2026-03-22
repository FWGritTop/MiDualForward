package com.notifyforwarder.model

data class AppInfo(
    val packageName: String,
    val userId: Int,
    val displayName: String,
    val isDualApp: Boolean,
    var isForwardEnabled: Boolean = false,
    var notificationCount: Int = 0
) {
    val appKey: String
        get() = "${packageName}_$userId"
    
    val displayLabel: String
        get() = if (isDualApp) "$displayName (双开)" else displayName
}
