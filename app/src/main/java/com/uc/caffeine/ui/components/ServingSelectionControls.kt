package com.uc.caffeine.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import com.uc.caffeine.util.MIN_SERVING_QUANTITY
import com.uc.caffeine.util.formatQuantity
import com.uc.caffeine.util.formatUnitButtonLabel
import com.uc.caffeine.util.formatUnitLabel
import com.uc.caffeine.util.quantityStepFor
import com.uc.caffeine.util.quickPickQuantitiesFor

@Composable
fun ServingQuantityStepper(
    quantity: Double,
    unitKey: String,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    var showInput by remember { mutableStateOf(false) }
    val step = quantityStepFor(unitKey)
    val quickPicks = remember(unitKey) { quickPickQuantitiesFor(unitKey) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptics.tick()
                    onQuantityChange((quantity - step).coerceAtLeast(step))
                },
                enabled = quantity > step,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(
                    IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide),
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease quantity",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }

            // The whole number area is tappable to type an exact (and fractional) amount.
            // The small edit glyph makes that affordance discoverable.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showInput = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RollingNumberText(
                    text = formatQuantity(quantity),
                    style = MaterialTheme.typography.displaySmall,
                    horizontalArrangement = Arrangement.Center,
                )
                Spacer(Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Type exact quantity",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            FilledTonalIconButton(
                onClick = {
                    haptics.tick()
                    onQuantityChange(quantity + step)
                },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(
                    IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide),
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase quantity",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }
        }

        if (quickPicks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                quickPicks.forEachIndexed { index, value ->
                    ToggleButton(
                        checked = quantity == value,
                        onCheckedChange = {
                            haptics.toggle()
                            onQuantityChange(value)
                        },
                        modifier = Modifier.weight(1f),
                        shapes = connectedButtonShapes(index, quickPicks.size),
                        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                    ) {
                        Text(
                            text = "${formatQuantity(value)} ${formatUnitLabel(unitKey)}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    if (showInput) {
        QuantityInputDialog(
            currentQuantity = quantity,
            onConfirm = { value ->
                onQuantityChange(value)
                showInput = false
            },
            onDismiss = { showInput = false },
        )
    }
}

@Composable
private fun QuantityInputDialog(
    currentQuantity: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(formatQuantity(currentQuantity)) }
    val parsed = text.toDoubleOrNull()
    val isValid = parsed != null && parsed >= MIN_SERVING_QUANTITY

    fun confirm() {
        val value = text.toDoubleOrNull() ?: return
        if (value >= MIN_SERVING_QUANTITY) onConfirm(value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set quantity") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = sanitizeDecimalInput(it) },
                label = { Text("Quantity") },
                singleLine = true,
                isError = !isValid && text.isNotEmpty(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
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

// Keeps only digits and a single decimal point so the field always parses cleanly.
private fun sanitizeDecimalInput(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    if (firstDot < 0) return filtered
    val head = filtered.substring(0, firstDot + 1)
    val tail = filtered.substring(firstDot + 1).replace(".", "")
    return head + tail
}

@Composable
fun ServingUnitSelector(
    units: List<DrinkUnit>,
    selectedUnit: DrinkUnit?,
    onUnitSelected: (DrinkUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        units.forEachIndexed { index, unit ->
            ToggleButton(
                checked = unit.unitKey == selectedUnit?.unitKey,
                onCheckedChange = {
                    if (it) {
                        haptics.toggle()
                        onUnitSelected(unit)
                    }
                },
                modifier = Modifier.weight(1f),
                shapes = connectedButtonShapes(index, units.size),
            ) {
                Text(
                    text = formatUnitButtonLabel(unit.unitKey),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Shapes for item [index] of a connected [ToggleButton] row of [count] buttons. */
@Composable
fun connectedButtonShapes(index: Int, count: Int) = when {
    count == 1 -> ToggleButtonDefaults.shapes()
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}
