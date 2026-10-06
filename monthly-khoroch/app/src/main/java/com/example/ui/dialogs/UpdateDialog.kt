package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.update.UpdateState
import com.example.ui.theme.StatusRed
import com.example.util.BanglaFormatter

@Composable
fun UpdateDialog(
    state: UpdateState,
    currentVersionName: String,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manifest = when (state) {
        is UpdateState.Available -> state.manifest
        is UpdateState.Downloading -> state.manifest
        is UpdateState.Failed -> state.manifest
        else -> return
    }

    AlertDialog(
        onDismissRequest = {
            if (state !is UpdateState.Downloading) onDismiss()
        },
        modifier = modifier.testTag("update_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdateAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("নতুন আপডেট পাওয়া গেছে", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "সংস্করণ ${BanglaFormatter.toBanglaDigits(manifest.versionName)} (আপনার: ${BanglaFormatter.toBanglaDigits(currentVersionName)})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (manifest.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = manifest.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state is UpdateState.Downloading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_progress")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ডাউনলোড হচ্ছে... ${BanglaFormatter.toBanglaDigits((state.progress * 100).toInt().coerceIn(0, 100).toString())}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state is UpdateState.Failed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRed
                    )
                }
            }
        },
        confirmButton = {
            if (state !is UpdateState.Downloading) {
                Button(
                    onClick = onInstallClick,
                    modifier = Modifier.testTag("btn_update_now")
                ) {
                    Text(if (state is UpdateState.Failed) "আবার চেষ্টা করুন" else "আপডেট করুন")
                }
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_update_later")
                ) {
                    Text("পরে")
                }
            }
        }
    )
}
