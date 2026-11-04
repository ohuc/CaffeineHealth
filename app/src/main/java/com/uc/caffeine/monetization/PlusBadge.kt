package com.uc.caffeine.monetization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R

/**
 * The Caffeine Health+ mark: a rounded tile with the thick plus, drawn from the current
 * Material color scheme so it follows dynamic color and every palette.
 */
@Composable
fun PlusBadge(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus_glyph),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.62f),
        )
    }
}
