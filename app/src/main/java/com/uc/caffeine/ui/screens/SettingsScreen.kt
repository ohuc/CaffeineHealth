package com.uc.caffeine.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CaffeineViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    
    // Convert minutes to hours for display
    val halfLifeHours = userSettings.halfLifeMinutes / 60

    CaffeineScreenScaffold(
        title = "Settings",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Caffeine Half-Life Stepper
            CaffeineHalfLifeStepper(
                currentValue = halfLifeHours,
                onValueChange = { hours -> viewModel.updateHalfLife(hours) }
            )
            
            // 2. Bedtime Picker
            BedtimePicker(
                currentHour = userSettings.sleepTimeHour,
                currentMinute = userSettings.sleepTimeMinute,
                onTimeChange = { hour, minute -> viewModel.updateSleepTime(hour, minute) }
            )
            
            // 3. Safe Threshold Stepper
            SafeThresholdStepper(
                currentValue = userSettings.sleepThresholdMg,
                onValueChange = { mg -> viewModel.updateSleepThreshold(mg) }
            )
        }
    }
}

@Composable
private fun CaffeineHalfLifeStepper(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column {
        Text("Caffeine Half-Life", style = MaterialTheme.typography.titleMedium)
        Text(
            "This is the time in hours that it takes for your body to eliminate half of the caffeine you have consumed. Average is around 5 hours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        var textValue by remember(currentValue) { mutableStateOf(currentValue.toString()) }
        
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                    textValue = newValue
                    val intValue = newValue.toIntOrNull() ?: currentValue
                    if (intValue in 2..12) {
                        onValueChange(intValue)
                    }
                }
            },
            label = { Text("Hours") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        val newValue = (currentValue - 1).coerceIn(2, 12)
                        textValue = newValue.toString()
                        onValueChange(newValue)
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    IconButton(onClick = {
                        val newValue = (currentValue + 1).coerceIn(2, 12)
                        textValue = newValue.toString()
                        onValueChange(newValue)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            },
            modifier = Modifier.width(160.dp)
        )
    }
}

@Composable
private fun BedtimePicker(
    currentHour: Int,
    currentMinute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    Column {
        Text("Bedtime", style = MaterialTheme.typography.titleMedium)
        Text(
            "Set your usual bedtime to help track when caffeine might affect your sleep",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Material 3 TimePicker implementation
        var showTimePicker by remember { mutableStateOf(false) }
        
        OutlinedButton(onClick = { showTimePicker = true }) {
            Text(formatTime(currentHour, currentMinute))
        }
        
        if (showTimePicker) {
            TimePickerDialog(
                currentHour = currentHour,
                currentMinute = currentMinute,
                onTimeSelected = { hour, minute ->
                    onTimeChange(hour, minute)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }
    }
}

@Composable
private fun SafeThresholdStepper(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column {
        Text("Safe Caffeine Threshold", style = MaterialTheme.typography.titleMedium)
        Text(
            "This is the amount of caffeine you can have in your body without significantly disrupting your sleep function. Average is around 100mg.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        var textValue by remember(currentValue) { mutableStateOf(currentValue.toString()) }
        
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                    textValue = newValue
                    val intValue = newValue.toIntOrNull() ?: currentValue
                    if (intValue in 20..200) {
                        onValueChange(intValue)
                    }
                }
            },
            label = { Text("Milligrams") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        val newValue = (currentValue - 5).coerceIn(20, 200)
                        textValue = newValue.toString()
                        onValueChange(newValue)
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    IconButton(onClick = {
                        val newValue = (currentValue + 5).coerceIn(20, 200)
                        textValue = newValue.toString()
                        onValueChange(newValue)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            },
            modifier = Modifier.width(160.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
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

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(displayHour, minute, amPm)
}
