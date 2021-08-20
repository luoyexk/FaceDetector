package com.facedetector.extension

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Fragment.showToast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
    if (!text.isNullOrEmpty()) {
        Toast.makeText(requireContext(), text, duration).show()
    }
}

fun Fragment.showToast(@StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), message, duration).show()
}

fun Fragment.activityResultLauncher(block: (ActivityResult?) -> Unit): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        block(it)
    }
}

fun Fragment.requestPermissionLauncher(block: (Boolean) -> Unit): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        block(it)
    }
}

fun Fragment.requestPermissionsLauncher(block: (Map<String?, Boolean?>) -> Unit): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        block(it)
    }
}

fun Fragment.finish() {
    activity?.finish()
}

fun Fragment.hasPermissions(permissions: Array<String>) =
    requireContext().hasPermissions(permissions)


fun Fragment.post(action: Runnable) {
    view?.post(action)
}