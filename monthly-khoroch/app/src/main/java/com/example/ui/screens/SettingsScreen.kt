package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupValidationResult
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRed
import com.example.util.BanglaFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: String,
    lastBackupTimestamp: Long,
    currencySymbol: String = "৳",
    currentVersionName: String = "",
    isCheckingUpdate: Boolean = false,
    onThemeChange: (String) -> Unit,
    onCheckUpdate: () -> Unit = {},
    onExportBackup: (Uri, (Boolean, String?) -> Unit) -> Unit,
    onReadBackupUri: (Uri) -> BackupValidationResult,
    onRestoreBackup: (BackupValidationResult, replaceMode: Boolean) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportResult by remember { mutableStateOf<BackupValidationResult?>(null) }
    var showImportInvalidDialog by remember { mutableStateOf<String?>(null) }

    // Launcher for Export Backup
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportBackup(uri) { success, error ->
                if (success) {
                    Toast.makeText(context, "ব্যাকআপ সফলভাবে সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "ব্যাকআপ ব্যর্থ হয়েছে: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launcher for Import Backup
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = onReadBackupUri(uri)
            if (result.isValid) {
                pendingImportResult = result
            } else {
                showImportInvalidDialog = result.errorMessage ?: "অকার্যকর ফাইল।"
            }
        }
    }

    // Confirmation dialog before Restore
    if (pendingImportResult != null) {
        val result = pendingImportResult!!
        var replaceMode by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { pendingImportResult = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("ব্যাকআপ রিস্টোর নিশ্চিত করুন", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "ফাইলে পাওয়া গেছে: ${BanglaFormatter.toBanglaDigits(result.budgetCount.toString())} টি বাজেট এবং ${BanglaFormatter.toBanglaDigits(result.expenseCount.toString())} টি খরচ।",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "রিস্টোর পদ্ধতি বেছে নিন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceMode = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = replaceMode,
                            onClick = { replaceMode = true }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("প্রতিস্থাপন (Replace)", fontWeight = FontWeight.SemiBold)
                            Text("বর্তমান সব তথ্য মুছে ব্যাকআপ তথ্য বসবে", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceMode = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !replaceMode,
                            onClick = { replaceMode = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("যুক্ত করুন (Merge)", fontWeight = FontWeight.SemiBold)
                            Text("বর্তমান তথ্যের সাথে নতুন তথ্য যুক্ত হবে", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(result, replaceMode)
                        pendingImportResult = null
                        Toast.makeText(context, "তথ্য সফলভাবে রিস্টোর করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_restore_btn")
                ) {
                    Text("রিস্টোর করুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingImportResult = null },
                    modifier = Modifier.testTag("cancel_restore_btn")
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Invalid backup error dialog
    if (showImportInvalidDialog != null) {
        AlertDialog(
            onDismissRequest = { showImportInvalidDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusRed
                )
            },
            title = { Text("ত্রুটিপূর্ণ ফাইল", fontWeight = FontWeight.Bold) },
            text = { Text(showImportInvalidDialog ?: "") },
            confirmButton = {
                Button(onClick = { showImportInvalidDialog = null }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Clear All Confirmation
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = StatusRed
                )
            },
            title = { Text("সব তথ্য মুছে ফেলতে চান?", fontWeight = FontWeight.Bold) },
            text = {
                Text("আপনার সকল বাজেট এবং খরচের হিসাব সম্পূর্ণ মুছে যাবে। এই কাজটি পূর্বাবস্থায় ফেরানো যাবে না। মুছে ফেলার আগে একটি ব্যাকআপ ফাইল সংরক্ষণ করে রাখা ভালো।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "সকল ডেটা মুছে ফেলা হয়েছে।", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    modifier = Modifier.testTag("confirm_clear_all_btn")
                ) {
                    Text("সব মুছুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_clear_all_btn")
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "সেটিংস",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Appearance & Theme Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "থিম ও ডিসপ্লে",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentThemeMode == "system",
                            onClick = { onThemeChange("system") },
                            label = { Text("সিস্টেম ডিফল্ট") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_chip_system")
                        )
                        FilterChip(
                            selected = currentThemeMode == "light",
                            onClick = { onThemeChange("light") },
                            label = { Text("লাইট") },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("theme_chip_light")
                        )
                        FilterChip(
                            selected = currentThemeMode == "dark",
                            onClick = { onThemeChange("dark") },
                            label = { Text("ডার্ক") },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("theme_chip_dark")
                        )
                    }
                }
            }
        }

        // Backup & Data Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ব্যাকআপ ও ডেটা পুনরুদ্ধার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last Backup Status Indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "শেষ ব্যাকআপের অবস্থা:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (lastBackupTimestamp > 0L) {
                                Text(
                                    text = "শেষ Backup: ${BanglaFormatter.formatDateTimeBangla(lastBackupTimestamp)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "আপনার কোনো Backup তৈরি করা হয়নি।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = StatusAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch(BackupManager.generateBackupFileName())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_backup"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Backup নিন")
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_import_backup"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("রিস্টোর করুন")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "আপনার হিসাব সম্পূর্ণ ব্যক্তিগত এবং এই ফোনেই সুরক্ষিত থাকে। কোনো ক্লাউড সার্ভারে আপলোড হয় না।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Data Management (Clear Data)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ডেটা ম্যানেজমেন্ট",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "অ্যাপের সমস্ত হিসাব ও বাজেট রিসেট করতে চাইলে নিচের বাটনটি ব্যবহার করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_clear_all_data")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সকল ডেটা মুছে ফেলুন")
                    }
                }
            }
        }

        // App Update Card (GitHub self-update)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdateAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অ্যাপ আপডেট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "বর্তমান সংস্করণ: ${BanglaFormatter.toBanglaDigits(currentVersionName)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "নতুন সংস্করণ প্রকাশ হলে অ্যাপ নিজে থেকেই জানিয়ে দেবে। এখান থেকে ম্যানুয়ালি চেক করতেও পারেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { onCheckUpdate() },
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_check_update")
                    ) {
                        Text(if (isCheckingUpdate) "চেক করা হচ্ছে..." else "আপডেট চেক করুন")
                    }
                }
            }
        }

        // About Card with Official Branding
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon_fg),
                            contentDescription = "মাসিক খরচ লোগো",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly Khoroch (মাসিক খরচ)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "অফিসিয়াল সংস্করণ ${BanglaFormatter.toBanglaDigits(currentVersionName)} (অফলাইন ও নিরাপদ)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "সহজ, দ্রুত এবং নির্ভুল ব্যক্তিগত মাসিক বাজেট ও ব্যয়ের হিসাব রাখার অ্যাপ। কোনো ইন্টারনেট কানেকশন বা অ্যাকাউন্টের প্রয়োজন নেই।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val shareText = "📱 Monthly Khoroch (মাসিক খরচ) — সহজ, দ্রুত ও নিরাপদ মাসিক খরচ ও বাজেট হিসাবের অ্যাপ।\n\n" +
                                "ডাউনলোড করুন: https://github.com/nurulhudaturag-droid/Monthly-Khoroch-Android-App/releases/latest/download/MonthlyKhoroch.apk"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Monthly Khoroch (মাসিক খরচ)")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_share_app"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("এই অ্যাপটি শেয়ার করুন")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ডেভেলপার: Nurul Huda",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "প্রতিষ্ঠান: QiubZen",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:01609555593")))
                                }
                            ) {
                                Text(
                                    text = "যোগাযোগ: 01609555593",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "কল করুন",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
