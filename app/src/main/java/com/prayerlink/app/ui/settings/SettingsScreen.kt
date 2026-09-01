package com.prayerlink.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayerlink.app.R
import com.prayerlink.app.data.model.CalculationMethod
import com.prayerlink.app.data.model.CityData
import com.prayerlink.app.data.model.ThemeMode
import com.prayerlink.app.ui.settings.components.CitySelectionDialog
import com.prayerlink.app.ui.settings.components.PermissionsDialog
import com.prayerlink.app.ui.settings.components.SettingsGroup
import com.prayerlink.app.ui.settings.components.SettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locationStatus by viewModel.locationStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showCityDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    
    val isCheckingForUpdate by viewModel.isCheckingForUpdate.collectAsStateWithLifecycle()
    val updateResult by viewModel.updateResult.collectAsStateWithLifecycle()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.detectGpsLocation() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val audioPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateCustomAudioUri(uri.toString())
                    viewModel.updateSoundMode(com.prayerlink.app.data.model.NotificationSoundMode.CUSTOM)
                }
            }

            // ── Notifications Group ──────────────────────────────────────
            SettingsGroup(title = stringResource(R.string.settings_notifications)) {
                SettingsRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = stringResource(R.string.settings_enable_notifications),
                    subtitle = stringResource(R.string.settings_enable_notifications_desc),
                    onClick = { viewModel.updateNotificationsEnabled(!settings.notificationsEnabled) },
                    trailingContent = { Switch(checked = settings.notificationsEnabled, onCheckedChange = { viewModel.updateNotificationsEnabled(it) }) }
                )
                
                val soundModeName = when(settings.soundMode) {
                    com.prayerlink.app.data.model.NotificationSoundMode.ADHAN -> "Makkah Adhan (Default)"
                    com.prayerlink.app.data.model.NotificationSoundMode.SYSTEM -> "System Notification Sound"
                    com.prayerlink.app.data.model.NotificationSoundMode.CUSTOM -> "Custom Audio"
                }
                
                SettingsDropdownRow(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    title = stringResource(R.string.settings_notification_sound),
                    subtitle = soundModeName,
                    options = listOf("Makkah Adhan (Default)", "System Notification Sound", "Custom Audio"),
                    onSelected = { idx ->
                        when(idx) {
                            0 -> viewModel.updateSoundMode(com.prayerlink.app.data.model.NotificationSoundMode.ADHAN)
                            1 -> viewModel.updateSoundMode(com.prayerlink.app.data.model.NotificationSoundMode.SYSTEM)
                            2 -> audioPickerLauncher.launch("audio/*")
                        }
                    }
                )
                
                SettingsRow(
                    icon = Icons.Outlined.Vibration,
                    title = stringResource(R.string.settings_vibration),
                    subtitle = "Vibrate on notification",
                    showDivider = false,
                    onClick = { viewModel.updateVibration(!settings.vibrationEnabled) },
                    trailingContent = { Switch(checked = settings.vibrationEnabled, onCheckedChange = { viewModel.updateVibration(it) }) }
                )
            }

            // ── Location Group ───────────────────────────────────────────
            SettingsGroup(title = "Location") {
                val locSubtitle = if (settings.useGps) {
                    when (locationStatus) {
                        LocationStatus.DETECTING -> "Detecting location..."
                        LocationStatus.FAILED -> "Failed to detect. Using cached: ${settings.locationName}"
                        else -> settings.locationName
                    }
                } else {
                    "Manual Selection"
                }

                val onGpsToggled: (Boolean) -> Unit = { newGps ->
                    viewModel.updateUseGps(newGps)
                    if (newGps) locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }

                SettingsRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Automatic Location (GPS)",
                    subtitle = locSubtitle,
                    onClick = { onGpsToggled(!settings.useGps) },
                    trailingContent = { 
                        Switch(
                            checked = settings.useGps, 
                            onCheckedChange = { onGpsToggled(it) }
                        ) 
                    },
                    showDivider = !settings.useGps
                )
                if (!settings.useGps) {
                    SettingsRow(
                        icon = Icons.Outlined.Map,
                        title = "Manual City",
                        subtitle = settings.locationName,
                        showDivider = false,
                        onClick = { showCityDialog = true },
                        trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                    )
                }
            }

            // ── Calculation Method Group ─────────────────────────────────
            SettingsGroup(title = "Calculation Method") {
                SettingsDropdownRow(
                    icon = Icons.Outlined.Calculate,
                    title = "Calculation Method",
                    subtitle = stringResource(CalculationMethod.entries[settings.calculationMethodIndex].nameResId),
                    options = CalculationMethod.entries.map { stringResource(it.nameResId) },
                    onSelected = { viewModel.updateCalculationMethod(it) }
                )
                SettingsDropdownRow(
                    icon = Icons.Outlined.Calculate,
                    title = "Asr Juristic",
                    subtitle = if (settings.asrJuristic == 0) "Standard (Shafi, Hanbali, Maliki)" else "Hanafi",
                    options = listOf("Standard", "Hanafi"),
                    showDivider = false,
                    onSelected = { viewModel.updateAsrJuristic(it) }
                )
            }

            // ── Appearance Group ─────────────────────────────────────────
            SettingsGroup(title = "Appearance") {
                val themeSubtitle = when (settings.themeMode) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.SYSTEM -> "System Default"
                }
                SettingsRow(
                    icon = Icons.Outlined.ColorLens,
                    title = "Theme",
                    subtitle = themeSubtitle,
                    trailingContent = {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.5f)) {
                            ThemeMode.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.updateTheme(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size)
                                ) {
                                    Text(mode.name.first().toString()) // Simple L, D, S
                                }
                            }
                        }
                    }
                )
                SettingsDropdownRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = if (settings.language == "ar") "العربية" else "English",
                    options = listOf("English", "العربية"),
                    showDivider = false,
                    onSelected = { idx ->
                        val code = if (idx == 1) "ar" else "en"
                        viewModel.updateLanguage(code)
                        val locales = androidx.core.os.LocaleListCompat.forLanguageTags(code)
                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
                    }
                )
            }

            // ── Other Group ──────────────────────────────────────────────
            SettingsGroup(title = "Other") {
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Manage Permissions",
                    subtitle = "Review required app permissions",
                    onClick = { showPermissionsDialog = true },
                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                )
                
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isUnrestricted = pm.isIgnoringBatteryOptimizations(context.packageName)
                SettingsRow(
                    icon = Icons.Outlined.BatteryAlert,
                    title = "Battery Optimization",
                    subtitle = if (isUnrestricted) "Unrestricted (Recommended)" else "Ensure notifications work properly",
                    showDivider = false,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                )
            }

            // ── About Group ──────────────────────────────────────────────
            SettingsGroup(title = "About") {
                val updateSubtitle = if (isCheckingForUpdate) {
                    "Checking for updates..."
                } else if (settings.latestKnownVersion != null && settings.latestKnownVersion != viewModel.currentVersionName) {
                    "Update Available: ${settings.latestKnownVersion}"
                } else {
                    "PrayerLink Version ${viewModel.currentVersionName}"
                }
                
                SettingsRow(
                    icon = Icons.Outlined.Update,
                    title = "Check for Updates",
                    subtitle = updateSubtitle,
                    showDivider = false,
                    onClick = { viewModel.checkForUpdates() },
                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                )
            }
        }
    }

    if (updateResult != null) {
        UpdateDialog(
            currentVersion = viewModel.currentVersionName,
            result = updateResult!!,
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    if (showCityDialog) {
        CitySelectionDialog(
            language = settings.language,
            selectedIndex = settings.selectedCityIndex,
            onDismiss = { showCityDialog = false },
            onCitySelected = { viewModel.updateCity(it) }
        )
    }

    if (showPermissionsDialog) {
        PermissionsDialog(onDismiss = { showPermissionsDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    options: List<String>,
    showDivider: Boolean = true,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        SettingsRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            showDivider = showDivider,
            modifier = Modifier.menuAnchor(),
            trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { idx, text ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(idx)
                        expanded = false
                    }
                )
            }
        }
    }
}
