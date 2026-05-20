package com.uc.caffeine.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uc.caffeine.LocalAppScaffoldPadding
import com.uc.caffeine.R
import com.uc.caffeine.data.ImportMode
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.ui.viewmodel.MyDataUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MyDataSettingsScreen(
    onBack: () -> Unit,
    viewModel: CaffeineViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val myDataState by viewModel.myDataState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showImportModeDialog by remember { mutableStateOf(false) }

    val unknownErrorText = stringResource(R.string.my_data_export_error_unknown)
    val exportFailedTemplate = stringResource(R.string.my_data_export_failed, "%1\$s")
    val couldNotReadFileText = stringResource(R.string.my_data_import_could_not_read)

    LaunchedEffect(myDataState) {
        when (val state = myDataState) {
            is MyDataUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearMyDataState()
            }
            is MyDataUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearMyDataState()
            }
            else -> Unit
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.createBackupJson(userSettings)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    viewModel.onExportSuccess()
                } catch (e: Exception) {
                    viewModel.onExportError(exportFailedTemplate.format(e.message ?: unknownErrorText))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    }
                    if (json != null) {
                        withContext(Dispatchers.Main) {
                            pendingImportJson = json
                            showImportModeDialog = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        viewModel.onExportError(couldNotReadFileText)
                    }
                }
            }
        }
    }

    if (showImportModeDialog) {
        ImportModeDialog(
            onMerge = {
                showImportModeDialog = false
                pendingImportJson?.let { viewModel.importBackup(it, ImportMode.MERGE) }
                pendingImportJson = null
            },
            onReplace = {
                showImportModeDialog = false
                pendingImportJson?.let { viewModel.importBackup(it, ImportMode.REPLACE) }
                pendingImportJson = null
            },
            onDismiss = {
                showImportModeDialog = false
                pendingImportJson = null
            },
        )
    }

    val appPadding = LocalAppScaffoldPadding.current
    Box(modifier = Modifier.fillMaxSize()) {
        MyDataSettingsContent(
            isWorking = myDataState is MyDataUiState.Working,
            onExportClick = {
                haptics.toggle()
                val date = LocalDate.now()
                exportLauncher.launch("caffeine_backup_$date.json")
            },
            onImportClick = {
                haptics.toggle()
                importLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onBack = onBack,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = appPadding.calculateBottomPadding() + 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MyDataSettingsContent(
    isWorking: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsPageScaffold(
        title = stringResource(R.string.settings_my_data_title),
        showBackButton = true,
        onBack = onBack,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding + 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SegmentedListItem(
                    onClick = onExportClick,
                    enabled = !isWorking,
                    leadingContent = {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                    },
                    content = {
                        Text(
                            text = stringResource(R.string.my_data_export_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.my_data_export_description))
                    },
                    shapes = segmentedListItemShapes(0, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
                SegmentedListItem(
                    onClick = onImportClick,
                    enabled = !isWorking,
                    leadingContent = {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                    },
                    content = {
                        Text(
                            text = stringResource(R.string.my_data_import_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.my_data_import_description))
                    },
                    shapes = segmentedListItemShapes(1, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ImportModeDialog(
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.my_data_import_title)) },
        text = {
            Text(stringResource(R.string.my_data_import_dialog_text))
        },
        confirmButton = {
            TextButton(onClick = onMerge) { Text(stringResource(R.string.my_data_import_merge)) }
        },
        dismissButton = {
            TextButton(onClick = onReplace) { Text(stringResource(R.string.my_data_import_replace)) }
        },
    )
}
