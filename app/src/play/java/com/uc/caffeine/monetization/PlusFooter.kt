package com.uc.caffeine.monetization

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R

internal enum class PlusInfoDialog(val title: Int, val body: Int) {
    TERMS(R.string.plus_terms, R.string.plus_terms_body),
    HELP(R.string.plus_help, R.string.plus_help_body),
    STORE_ERROR(R.string.plus_help, R.string.plus_store_error),
}

@Composable
internal fun PlusFooter(showRestore: Boolean, restoreEnabled: Boolean, onRestore: () -> Unit, onInfo: (PlusInfoDialog) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
            if (showRestore) TextButton(onClick = onRestore, enabled = restoreEnabled, shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.plus_restore)) }
            listOf(PlusInfoDialog.TERMS, PlusInfoDialog.HELP).forEach { dialog ->
                TextButton(onClick = { onInfo(dialog) }, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(dialog.title)) }
            }
        }
        Text(stringResource(R.string.plus_billing_disclosure), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Composable
internal fun PlusInfoDialog(dialog: PlusInfoDialog, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(dialog.title)) },
        text = { Text(stringResource(dialog.body)) },
        confirmButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.plus_close_dialog)) } })
}
