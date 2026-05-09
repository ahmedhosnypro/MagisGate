package com.ahmedhosnypro.magisgate.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val installTime: Long,
    val updateTime: Long,
    val apkSize: Long,
)
