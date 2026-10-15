package com.uc.caffeine.ui.components

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb

/**
 * A single-image request for the system photo picker, which needs no storage permission and brings
 * its own search and albums. The picker takes the app's accent where it supports one (Android 15+)
 * and falls back to its own colours elsewhere.
 */
@Composable
fun rememberImagePickerRequest(): PickVisualMediaRequest {
    val accent = MaterialTheme.colorScheme.primary
    return remember(accent) {
        PickVisualMediaRequest(
            accentColor = android.graphics.Color.pack(accent.toArgb()),
            mediaType = PickVisualMedia.ImageOnly,
        )
    }
}
