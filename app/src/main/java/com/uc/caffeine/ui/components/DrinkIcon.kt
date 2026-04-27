package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun DrinkIcon(
    imageName: String,
    emoji: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = TextUnit.Unspecified
) {
    val context = LocalContext.current

    if (imageName.isNotBlank()) {
        val imageData = when {
            imageName.startsWith("content://") || imageName.startsWith("file://") -> imageName
            imageName.startsWith("/") -> "file://$imageName"
            else -> "file:///android_asset/items/${imageName}.png"
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageData)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = emoji.ifBlank { "☕" },
                fontSize = emojiSize,
                textAlign = TextAlign.Center
            )
        }
    }
}
