package com.cloudchef.assignment.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionUtil {
    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
        )

        fun getDeniedPermission(context: Context): ArrayList<String> {
            val deniedPermission = ArrayList<String>()
            for (permission in PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    deniedPermission.add(permission)
                }
            }
            return deniedPermission
        }
    }
}