package com.uc.caffeine.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.uc.caffeine.R
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTimePickerDialog(
    title: String,
    state: TimePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.action_save),
    extraContent: @Composable () -> Unit = {},
) {
    val haptics = rememberAppHaptics()
    val density = LocalDensity.current
    val fontScale = density.fontScale
    // Read the host window here, outside the dialog. The dialog's own window gets its insets a
    // frame or two after it opens, so sizing the layout from inside it made the picker jump (or
    // flip between clock and text input) right after opening. 72dp leaves room for system bars.
    val hostSize = with(density) { LocalWindowInfo.current.containerSize.let { DpSize(it.width.toDp(), it.height.toDp()) } }
    val compact = hostSize.height - 72.dp - 32.dp < 560.dp || hostSize.width - 32.dp < 360.dp || fontScale > 1.3f
    val sideBySide = compact && hostSize.width - 32.dp >= 480.dp
    // Keep input drafts if the window changes between layout modes.
    val input = remember(state) { movableContentOf { TimeInput(state = state) } }
    val actions: @Composable () -> Unit = {
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_cancel)) }
        TextButton(onClick = onConfirm, shapes = ButtonDefaults.shapes()) { Text(confirmLabel) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.hour to state.minute }.drop(1).collect { haptics.tick() }
    }
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
        // Fill the dialog window and centre the card in it, so late insets only trim the free
        // space around the card instead of resizing and re-centring the whole dialog.
        BoxWithConstraints(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = if (keyboardVisible) 0.dp else 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                modifier = Modifier.widthIn(max = 560.dp).semantics { paneTitle = title },
            ) {
                if (sideBySide) {
                    Row(
                        Modifier.heightIn(max = maxHeight).padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth())
                            extraContent()
                            Box(Modifier.horizontalScroll(rememberScrollState())) { input() }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
                    }
                } else {
                Column(Modifier.heightIn(max = maxHeight).padding(if (maxHeight < 360.dp) 12.dp else 24.dp)) {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth())
                        extraContent()
                        Box(Modifier.horizontalScroll(rememberScrollState())) {
                            if (compact) input()
                            else TimePicker(state = state, layoutType = TimePickerLayoutType.Vertical)
                        }
                    }
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        actions()
                    }
                }
                }
            }
        }
    }
}
