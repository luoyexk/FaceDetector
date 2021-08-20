package com.facedetector.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


fun Context.getPackageInfo(): PackageInfo {
    return packageManager.getPackageInfo(packageName, 0)
}

fun Context.hasPermissions(permissions: Array<String>) = permissions.all {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}