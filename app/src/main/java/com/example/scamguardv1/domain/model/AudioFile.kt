package com.example.scamguardv1.domain.model

import android.net.Uri

data class AudioFile(
    val id: Long,
    val name: String,
    val uri: Uri,
    val duration: Int,
    val timestamp: Long
)
