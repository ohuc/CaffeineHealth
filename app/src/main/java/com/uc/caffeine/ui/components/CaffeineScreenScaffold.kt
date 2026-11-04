package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uc.caffeine.LocalAppScaffoldPadding

/**
 * Every top-level page header (Home, Analytics, Settings, Add) is at least this tall, so the
 * title sits at the same height whether or not the page has 48dp header actions such as the
 * Home view switcher. Without it, pages with actions placed their title lower.
 */
val ScreenHeaderMinHeight = 48.dp

@Composable
fun CaffeineScreenScaffold(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    headerBottomSpacing: Dp = 16.dp,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.(bottomPadding: Dp) -> Unit
) {
    val appPadding = LocalAppScaffoldPadding.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(LocalPanePlacement.current.headerInsetSides))
            .padding(
                start = contentPadding.calculateLeftPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateRightPadding(LocalLayoutDirection.current),
                top = contentPadding.calculateTopPadding()
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ScreenHeaderMinHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        if (headerBottomSpacing > 0.dp) {
            Spacer(modifier = Modifier.height(headerBottomSpacing))
        }

        content(appPadding.calculateBottomPadding())
    }
}
