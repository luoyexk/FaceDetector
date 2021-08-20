package com.facedetector.extension

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity


fun AppCompatActivity.requestPermissionsLauncher(block: (Map<String, Boolean>) -> Unit): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        block.invoke(it)
    }
}

fun Activity.commonOverridePendingTransition() {
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}