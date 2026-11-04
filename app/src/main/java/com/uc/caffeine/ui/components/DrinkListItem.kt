package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults

/**
 * Drink photos in drink lists: a 56dp expressive badge with a 44dp image, so the cut-out
 * photos are recognisable at a glance (they were 28dp inside 44dp and read as specks).
 */
val DrinkListBadgeSize = 56.dp
val DrinkListImageSize = 44.dp

/** The drink image inside its expressive badge, sized for [DrinkListItem] rows. */
@Composable
fun DrinkListBadge(
    imageName: String,
    emoji: String,
    contentDescription: String,
    index: Int,
) {
    ExpressiveIconBadge(
        index = index,
        size = DrinkListBadgeSize,
    ) {
        DrinkIcon(
            imageName = imageName,
            emoji = emoji,
            contentDescription = contentDescription,
            modifier = Modifier.size(DrinkListImageSize),
            emojiSize = MaterialTheme.typography.headlineMedium.fontSize,
        )
    }
}

/**
 * One drink row, shared by the Add Consumption catalogue and the Home history so both lists
 * read the same: large badge photo, emphasized name, one supporting line and a bold mg value.
 */
@Composable
fun DrinkListItem(
    name: String,
    imageName: String,
    emoji: String,
    caffeineMg: Int,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    supportingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        leadingContent = {
            DrinkListBadge(
                imageName = imageName,
                emoji = emoji,
                contentDescription = name,
                index = index,
            )
        },
        content = {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = supportingContent,
        trailingContent = {
            Text(
                text = stringResource(R.string.caffeine_mg_compact, caffeineMg),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        },
        verticalAlignment = Alignment.CenterVertically,
        shapes = segmentedListItemShapes(index, count),
        colors = ListItemDefaults.colors(
            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
        ),
    )
}

/** A single-line supporting text in the style [DrinkListItem] rows use. */
@Composable
fun DrinkListSupportingText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
