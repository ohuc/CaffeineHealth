package com.uc.caffeine.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.AppDateFormat
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.formatTimeZoneName
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DateTimeSettingsScreen(
    userSettings: UserSettings,
    onUse24HourClockChange: (Boolean) -> Unit,
    onDateFormatChange: (AppDateFormat) -> Unit,
    onTimeZoneIdChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var showDateFormatSheet by rememberSaveable { mutableStateOf(false) }
    var showTimeZoneSheet by rememberSaveable { mutableStateOf(false) }

    SettingsPageScaffold(
        title = stringResource(R.string.settings_date_time_title),
        showBackButton = true,
        onBack = onBack,
    ) { bottomPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
        ) {

            item {
                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        onUse24HourClockChange(!userSettings.use24HourClock)
                    },
                    content = {
                        Text(text = stringResource(R.string.date_time_24h_label))
                    },
                    supportingContent = {
                        Text(text = stringResource(R.string.date_time_24h_summary))
                    },
                    trailingContent = {
                        Switch(
                            checked = userSettings.use24HourClock,
                            onCheckedChange = { enabled ->
                                haptics.toggle()
                                onUse24HourClockChange(enabled)
                            },
                        )
                    },
                    shapes = segmentedListItemShapes(0, 3),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }

            item {
                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        showDateFormatSheet = true
                    },
                    content = {
                        Text(text = stringResource(R.string.date_time_date_format_label))
                    },
                    supportingContent = {
                        Text(text = userSettings.dateFormat.displayLabel)
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    shapes = segmentedListItemShapes(1, 3),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }

            item {
                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        showTimeZoneSheet = true
                    },
                    content = {
                        Text(text = stringResource(R.string.date_time_timezone_label))
                    },
                    supportingContent = {
                        Text(text = formatTimeZoneName(userSettings.timeZoneId))
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    shapes = segmentedListItemShapes(2, 3),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }

        if (showDateFormatSheet) {
            AppDateFormatBottomSheet(
                selectedFormat = userSettings.dateFormat,
                onSelect = { dateFormat ->
                    haptics.confirm()
                    onDateFormatChange(dateFormat)
                    showDateFormatSheet = false
                },
                onDismiss = { showDateFormatSheet = false },
            )
        }

        if (showTimeZoneSheet) {
            TimeZoneBottomSheet(
                selectedTimeZoneId = userSettings.timeZoneId,
                onSelect = { timeZoneId ->
                    haptics.confirm()
                    onTimeZoneIdChange(timeZoneId)
                    showTimeZoneSheet = false
                },
                onDismiss = { showTimeZoneSheet = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppDateFormatBottomSheet(
    selectedFormat: AppDateFormat,
    onSelect: (AppDateFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    val dateFormats = AppDateFormat.entries

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.date_time_date_format_label),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.date_time_date_format_sheet_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            itemsIndexed(dateFormats) { index, dateFormat ->
                SegmentedListItem(
                    onClick = { onSelect(dateFormat) },
                    content = {
                        Text(text = dateFormat.displayLabel)
                    },
                    trailingContent = {
                        if (selectedFormat == dateFormat) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    shapes = segmentedListItemShapes(index, dateFormats.size),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeZoneBottomSheet(
    selectedTimeZoneId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val timeZoneIds = remember {
        ZoneId.getAvailableZoneIds().toList().sorted()
    }
    val filteredTimeZoneIds = remember(searchQuery, timeZoneIds) {
        val normalizedQuery = searchQuery.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            timeZoneIds
        } else {
            timeZoneIds.filter { timeZoneId ->
                val readableName = timeZoneId.replace('_', ' ').lowercase()
                readableName.contains(normalizedQuery) || timeZoneId.lowercase().contains(normalizedQuery)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.date_time_timezone_label),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.date_time_timezone_sheet_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    label = { Text(stringResource(R.string.date_time_timezone_search_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }

            itemsIndexed(filteredTimeZoneIds) { index, timeZoneId ->
                SegmentedListItem(
                    onClick = { onSelect(timeZoneId) },
                    content = {
                        Text(text = timeZoneId.replace('_', ' '))
                    },
                    supportingContent = {
                        Text(text = formatTimeZoneName(timeZoneId))
                    },
                    trailingContent = {
                        if (selectedTimeZoneId == timeZoneId) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    shapes = segmentedListItemShapes(index, filteredTimeZoneIds.size),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}
