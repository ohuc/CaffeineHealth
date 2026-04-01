package com.uc.caffeine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.ui.components.CaffeineChart
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.components.ConsumptionContributionChart
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.ui.viewmodel.HomeScreenUiEvent
import com.uc.caffeine.util.ConsumptionContributionDetail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: CaffeineViewModel = viewModel()) {
    val currentLevel by viewModel.currentCaffeineLevel.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val totalCaffeine by viewModel.todayTotalMg.collectAsStateWithLifecycle()
    val caffeineAtBedtime by viewModel.caffeineAtBedtime.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val todayEntries by viewModel.todayEntries.collectAsStateWithLifecycle()

    var logExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<ConsumptionEntry?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel, snackbarHostState, sheetState) {
        viewModel.homeScreenEvents.collectLatest { event ->
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            showEditDialog = false
            selectedEntry = null
            snackbarHostState.currentSnackbarData?.dismiss()

            when (event) {
                is HomeScreenUiEvent.LogActionCompleted -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    CaffeineScreenScaffold(
        title = "Home",
        snackbarHostState = snackbarHostState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CaffeineChart(
                        chartData = chartData,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Active Caffeine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.1f mg".format(currentLevel),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val bedtimeCaffeineLevel = caffeineAtBedtime.first

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = when {
                        bedtimeCaffeineLevel < userSettings.sleepThresholdMg ->
                            MaterialTheme.colorScheme.primaryContainer
                        bedtimeCaffeineLevel < userSettings.sleepThresholdMg * 1.5 ->
                            MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            bedtimeCaffeineLevel < userSettings.sleepThresholdMg -> Icons.Default.CheckCircle
                            bedtimeCaffeineLevel < userSettings.sleepThresholdMg * 1.5 -> Icons.Default.Warning
                            else -> Icons.Default.Cancel
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sleep Forecast",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = when {
                                bedtimeCaffeineLevel < userSettings.sleepThresholdMg ->
                                    "Safe to sleep at ${formatBedtime(userSettings)}"
                                bedtimeCaffeineLevel < userSettings.sleepThresholdMg * 1.5 ->
                                    "May affect sleep (${bedtimeCaffeineLevel.toInt()}mg at bedtime)"
                                else ->
                                    "Sleep disruption likely (${bedtimeCaffeineLevel.toInt()}mg)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total today:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$totalCaffeine mg",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { logExpanded = !logExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Log (${todayEntries.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        imageVector = if (logExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (logExpanded) "Collapse" else "Expand"
                    )
                }

                if (logExpanded) {
                    if (todayEntries.isEmpty()) {
                        Text(
                            text = "No drinks logged today",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            todayEntries.forEach { entry ->
                                LoggedDrinkRow(
                                    entry = entry,
                                    onClick = { selectedEntry = entry }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    selectedEntry?.let { entry ->
        val detail = remember(entry, chartData.currentTimeMillis, userSettings) {
            viewModel.getContributionDetail(
                entry = entry,
                currentTimeMillis = chartData.currentTimeMillis
            )
        }

        ModalBottomSheet(
            onDismissRequest = {
                showEditDialog = false
                selectedEntry = null
            },
            sheetState = sheetState,
            dragHandle = null
        ) {
            ConsumptionLogDetailSheet(
                detail = detail,
                onEdit = { showEditDialog = true },
                onDuplicate = { viewModel.duplicateLoggedEntry(entry) },
                onDelete = { viewModel.deleteLoggedEntry(entry) }
            )
        }

        if (showEditDialog) {
            EditConsumptionEntryDialog(
                detail = detail,
                onDismiss = { showEditDialog = false },
                onSave = { caffeineMg, timestamp ->
                    viewModel.updateLoggedEntry(
                        entry = entry,
                        caffeineMg = caffeineMg,
                        timestamp = timestamp
                    )
                }
            )
        }
    }
}

@Composable
private fun LoggedDrinkRow(
    entry: ConsumptionEntry,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = entry.emoji.ifBlank { "☕" },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(32.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.drinkName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatLoggedTime(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${entry.caffeineMg}mg",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ConsumptionLogDetailSheet(
    detail: ConsumptionContributionDetail,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = detail.emoji.ifBlank { "☕" },
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.drinkName,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Logged ${formatLoggedTime(detail.loggedAtMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Adds ${formatPreciseMg(detail.currentContributionMg)} now",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large
        ) {
            ConsumptionContributionChart(
                detail = detail,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Drink contribution to caffeine levels",
                style = MaterialTheme.typography.titleMedium
            )
            ContributionStatRow(
                label = "At peak (${formatLoggedTime(detail.peakTimestampMillis)})",
                value = formatPreciseMg(detail.peakContributionMg)
            )
            ContributionStatRow(
                label = "Now",
                value = formatPreciseMg(detail.currentContributionMg)
            )
            ContributionStatRow(
                label = "In total (over time)",
                value = formatPreciseMg(detail.totalContributionMg)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Edit,
                label = "Edit",
                onClick = onEdit
            )
            SheetActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ContentCopy,
                label = "Duplicate",
                onClick = onDuplicate
            )
            SheetActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Delete,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ContributionStatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SheetActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val contentTint = tint ?: MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentTint)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditConsumptionEntryDialog(
    detail: ConsumptionContributionDetail,
    onDismiss: () -> Unit,
    onSave: (Int, Long) -> Unit
) {
    val initialCalendar = remember(detail.entryId) {
        Calendar.getInstance().apply { timeInMillis = detail.loggedAtMillis }
    }
    var caffeineText by remember(detail.entryId) {
        mutableStateOf(detail.caffeineMg.toString())
    }
    var selectedHour by remember(detail.entryId) {
        mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by remember(detail.entryId) {
        mutableStateOf(initialCalendar.get(Calendar.MINUTE))
    }
    var showTimePicker by remember { mutableStateOf(false) }
    val parsedCaffeine = caffeineText.toIntOrNull()
    val isSaveEnabled = parsedCaffeine != null && parsedCaffeine > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit ${detail.drinkName}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = caffeineText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                            caffeineText = newValue
                        }
                    },
                    label = { Text("Caffeine (mg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text("Logged at ${formatTime(selectedHour, selectedMinute)}")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isSaveEnabled,
                onClick = {
                    onSave(
                        parsedCaffeine ?: detail.caffeineMg,
                        combineDateWithTime(detail.loggedAtMillis, selectedHour, selectedMinute)
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showTimePicker) {
        LogEntryTimePickerDialog(
            currentHour = selectedHour,
            currentMinute = selectedMinute,
            onTimeSelected = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogEntryTimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = currentHour,
        initialMinute = currentMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

private fun formatBedtime(settings: UserSettings): String {
    val hour = if (settings.sleepTimeHour > 12) settings.sleepTimeHour - 12 else settings.sleepTimeHour
    val amPm = if (settings.sleepTimeHour >= 12) "PM" else "AM"
    return "$hour:%02d $amPm".format(settings.sleepTimeMinute)
}

private fun formatLoggedTime(timestampMillis: Long): String {
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}

private fun formatPreciseMg(value: Double): String {
    return String.format(Locale.getDefault(), "%.1f mg", value)
}

private fun combineDateWithTime(
    baseTimestamp: Long,
    hour: Int,
    minute: Int
): Long {
    return Calendar.getInstance().apply {
        timeInMillis = baseTimestamp
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(displayHour, minute, amPm)
}

// ── Quick Add card — built from RecentDrink (log history), not DrinkPreset ──
@Composable
fun RecentDrinkCard(
    recent: RecentDrink,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    // RecentDrink doesn't have imageName — look it up by drinkName convention
    // e.g. "Dhak Blend" → we try "img_bluetokai_dhak_blend" won't work directly
    // So we just use emoji for Quick Add cards for now
    // When we wire up imageName into ConsumptionEntry later, images will appear here too

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = recent.emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recent.drinkName,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${recent.caffeineMg}mg",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Full drink catalog card — used on the Add screen ──────────────────────
@Composable
fun DrinkCard(drink: DrinkPreset, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (drink.imageName.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/items/${drink.imageName}.png")
                        .build(),
                    contentDescription = drink.name,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit,
                    error = null,
                    placeholder = null
                )
            } else {
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Text(text = drink.emoji, fontSize = 40.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = drink.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (drink.brand.isNotBlank()) {
                Text(
                    text = drink.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${drink.defaultCaffeineMg}mg",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = drink.defaultUnit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
