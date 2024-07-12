package com.phoenix.pillreminder.feature_alarms.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object PermissionManager {

    fun canDrawOverlays(context: Context): Boolean{
        return Settings.canDrawOverlays(context)
    }

    fun getOverlayPermissionIntent(context: Context) = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply{
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.parse("package:${context.packageName}")
    }
}