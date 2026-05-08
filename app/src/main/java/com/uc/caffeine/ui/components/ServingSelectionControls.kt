package com.uc.caffeine.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uc.caffeine.data.model.DrinkUnit
import com.uc.caffeine.util.formatUnitLabel

@Composable
fun ServingQuantityStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onQuantitySet: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showInput by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = quantity > 1,
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease quantity",
                modifier = Modifier.size(26.dp),
            )
        }

        RollingNumberText(
            text = quantity.toString(),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showInput = true }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        )

        IconButton(onClick = onIncrement) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase quantity",
                modifier = Modifier.size(26.dp),
            )
        }
    }

    if (showInput) {
        QuantityInputDialog(
            currentQuantity = quantity,
            onConfirm = { value ->
                onQuantitySet(value)
                showInput = false
            },
            onDismiss = { showInput = false },
        )
    }
}

@Composable
private fun QuantityInputDialog(
    currentQuantity: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentQuantity.toString()) }
    val parsed = text.toIntOrNull()
    val isValid = parsed != null && parsed >= 1

    fun confirm() {
        if (isValid) onConfirm(parsed!!)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set quantity") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }.trimStart('0').ifEmpty { "" } },
                label = { Text("Quantity") },
                singleLine = true,
                isError = !isValid && text.isNotEmpty(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
            )
        },
        confirmButton = {
            TextButton(enabled = isValid, onClick = ::confirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun ServingUnitSelector(
    units: List<DrinkUnit>,
    selectedUnit: DrinkUnit?,
    onUnitSelected: (DrinkUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        units.forEachIndexed { index, unit ->
            ToggleButton(
                checked = unit.unitKey == selectedUnit?.unitKey,
                onCheckedChange = { if (it) onUnitSelected(unit) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    units.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(
                    text = formatUnitLabel(unit.unitKey),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}
