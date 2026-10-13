package com.example.scamguardv1.utils

import android.Manifest
import android.os.Build

object PermissionHelper {
    val readAudioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    const val recordAudioPermission = Manifest.permission.RECORD_AUDIO

    val phonePermissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ANSWER_PHONE_CALLS
    )

    val allRequiredPermissions: Array<String>
        get() = arrayOf(readAudioPermission, recordAudioPermission) + phonePermissions
}
