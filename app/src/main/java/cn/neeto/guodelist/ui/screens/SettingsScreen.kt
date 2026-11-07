package cn.neeto.guodelist.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.neeto.guodelist.BuildConfig
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.net.ApiException
import cn.neeto.guodelist.core.net.SyncManager
import cn.neeto.guodelist.core.notifications.Notifications
import cn.neeto.guodelist.core.settings.AppSettings
import cn.neeto.guodelist.core.update.UpdateInstaller
import cn.neeto.guodelist.ui.AppViewModel
import cn.neeto.guodelist.ui.motion.rememberClickFeedback
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    ui: AppViewModel.UiState,
    onTheme: (String) -> Unit,
    onPack: (String) -> Unit,
    onIconStyle: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onServer: (String) -> Unit,
    onSync: () -> Unit,
    onLogin: (String, String, (Throwable?) -> Unit) -> Unit,
    onRegister: (String, String, String, (Throwable?) -> Unit) -> Unit,
    onLogout: () -> Unit,
    onLoadDemo: () -> Unit,
    onClearLocal: () -> Unit,
    onRemindersEnabled: (Boolean) -> Unit,
    onReminderLead: (Int) -> Unit,
    onWidgetLists: (Set<String>) -> Unit,
    update: AppViewModel.UpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onUpdateChannel: (String) -> Unit,
    onUpdateUrl: (String) -> Unit,
    onAiServerUrl: (String) -> Unit,
) {
    val colors = LocalGuoColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val settings = ui.settings
    val feedback = rememberClickFeedback()
    val sync = ui.sync

    var showAuth by remember { mutableStateOf(false) }
    var authRegister by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var serverDraft by remember(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var showAiServer by remember { mutableStateOf(false) }
    var aiDraft by remember(settings.aiServerUrl) { mutableStateOf(settings.aiServerUrl) }
    var installHint by remember { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(Notifications.canPost(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = Notifications.canPost(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsAllowed = granted && Notifications.canPost(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard(
                title = stringResource(R.string.settings_appearance),
                icon = Icons.Outlined.Palette,
            ) {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Segmented(
                    options = listOf(
                        SegOption("system", stringResource(R.string.theme_system), Icons.Outlined.SettingsBrightness),
                        SegOption("light", stringResource(R.string.theme_light), Icons.Outlined.LightMode),
                        SegOption("dark", stringResource(R.string.theme_dark), Icons.Outlined.DarkMode),
                    ),
                    selected = settings.theme,
                    onSelect = onTheme,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.settings_theme_pack),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Segmented(
                    options = listOf(
                        SegOption("classic", stringResource(R.string.pack_classic), Icons.Outlined.Palette),
                        SegOption("starry", stringResource(R.string.pack_starry), Icons.Outlined.Star),
                    ),
                    selected = settings.pack,
                    onSelect = onPack,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Segmented(
                    options = listOf(
                        SegOption("system", stringResource(R.string.language_system), Icons.Outlined.Translate),
                        SegOption("zh", stringResource(R.string.language_zh)),
                        SegOption("en", stringResource(R.string.language_en)),
                    ),
                    selected = settings.language,
                    onSelect = { language -> onLanguage(language) },
                )
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_icon),
                icon = Icons.Outlined.Apps,
            ) {
                Text(
                    text = stringResource(R.string.settings_icon_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink3,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val selectedIcon = cn.neeto.guodelist.core.icon.LauncherIcon.normalize(settings.iconStyle)
                    val options = listOf(
                        Triple(
                            cn.neeto.guodelist.core.icon.LauncherIcon.ADAPTIVE,
                            R.drawable.ic_launcher_preview_adaptive,
                            stringResource(R.string.icon_adaptive),
                        ),
                        Triple(
                            cn.neeto.guodelist.core.icon.LauncherIcon.ROUND,
                            R.drawable.ic_launcher_circle_icon,
                            stringResource(R.string.icon_round),
                        ),
                        Triple(
                            cn.neeto.guodelist.core.icon.LauncherIcon.SQUARE,
                            R.drawable.ic_launcher_square_icon,
                            stringResource(R.string.icon_square),
                        ),
                        Triple(
                            cn.neeto.guodelist.core.icon.LauncherIcon.IRREGULAR,
                            R.drawable.ic_launcher_irregular_icon,
                            stringResource(R.string.icon_irregular),
                        ),
                    )
                    options.forEach { (style, preview, label) ->
                        val active = style == selectedIcon
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = if (active) colors.brand else colors.line,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable {
                                    feedback()
                                    onIconStyle(style)
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(preview),
                                contentDescription = label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(44.dp),
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = if (active) colors.brandInk else colors.ink3,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_sync),
                icon = Icons.Outlined.CloudQueue,
                trailing = {
                    if (sync.status == SyncManager.Status.SYNCING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = colors.brand,
                        )
                    }
                },
            ) {
                if (!sync.signedIn) {
                    Text(
                        text = stringResource(R.string.sync_signed_out_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                feedback()
                                authRegister = false
                                showAuth = true
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brand,
                                contentColor = colors.onBrand,
                            ),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Login, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.sync_sign_in))
                        }
                        OutlinedButton(
                            onClick = {
                                feedback()
                                authRegister = true
                                showAuth = true
                            },
                            shape = CircleShape,
                        ) {
                            Icon(Icons.Outlined.PersonAdd, null, modifier = Modifier.size(15.dp), tint = colors.ink2)
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.sync_create_account), color = colors.ink2)
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.sync_signed_in_as, sync.account.email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = syncStatusText(sync, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sync.status == SyncManager.Status.ERROR) colors.danger else colors.ink3,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { feedback(); onSync() }, shape = CircleShape) {
                            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp), tint = colors.ink2)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.sync_now), color = colors.ink2)
                        }
                        TextButton(onClick = { feedback(); onLogout() }) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(15.dp), tint = colors.ink3)
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.sync_sign_out), color = colors.ink3)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        feedback()
                        serverDraft = settings.serverUrl
                        showServer = true
                    },
                ) {
                    Icon(Icons.Outlined.Dns, null, modifier = Modifier.size(14.dp), tint = colors.ink3)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.server_settings),
                        color = colors.ink3,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TextButton(
                    onClick = {
                        feedback()
                        aiDraft = settings.aiServerUrl
                        showAiServer = true
                    },
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(14.dp), tint = colors.ink3)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(
                            if (settings.aiServerUrl.isBlank()) R.string.ai_settings_off else R.string.ai_settings_on,
                        ),
                        color = colors.ink3,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_reminders),
                icon = Icons.Outlined.NotificationsActive,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = colors.ink3,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reminders_enable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink,
                        )
                    }
                    Switch(
                        checked = settings.remindersEnabled,
                        onCheckedChange = { enabled ->
                            onRemindersEnabled(enabled)
                            if (enabled && !notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }

                if (settings.remindersEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.reminders_lead),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.ink3,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Segmented(
                        options = listOf(
                            SegOption("5", stringResource(R.string.minutes_5)),
                            SegOption("10", stringResource(R.string.minutes_10)),
                            SegOption("30", stringResource(R.string.minutes_30)),
                            SegOption("60", stringResource(R.string.hours_1)),
                        ),
                        selected = settings.reminderLeadMinutes.toString(),
                        onSelect = { onReminderLead(it.toIntOrNull() ?: 10) },
                    )
                }

                if (!notificationsAllowed) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.reminders_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.danger,
                    )
                    TextButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                        },
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, null, modifier = Modifier.size(14.dp), tint = colors.brandInk)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.reminders_permission_button), color = colors.brandInk)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.reminders_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink3,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { feedback(); Notifications.postTest(context) }) {
                        Icon(Icons.Outlined.NotificationsActive, null, modifier = Modifier.size(14.dp), tint = colors.brandInk)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.reminders_test), color = colors.brandInk)
                    }
                    TextButton(
                        onClick = {
                            val intent = Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }.onFailure {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}"),
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.BatteryFull, null, modifier = Modifier.size(14.dp), tint = colors.ink3)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.reminders_battery_button), color = colors.ink3)
                    }
                }
                Text(
                    text = stringResource(R.string.reminders_battery_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                )
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_widget),
                icon = Icons.Outlined.Widgets,
            ) {
                Text(
                    text = stringResource(R.string.widget_lists),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = settings.widgetListIds.isEmpty(),
                        onClick = { feedback(); onWidgetLists(emptySet()) },
                        label = { Text(stringResource(R.string.widget_all_lists)) },
                        leadingIcon = if (settings.widgetListIds.isEmpty()) {
                            { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(14.dp)) }
                        } else {
                            null
                        },
                    )
                    ui.lists.forEach { list ->
                        val checked = list.id in settings.widgetListIds
                        FilterChip(
                            selected = checked,
                            onClick = {
                                feedback()
                                val next = if (checked) {
                                    settings.widgetListIds - list.id
                                } else {
                                    settings.widgetListIds + list.id
                                }
                                onWidgetLists(next)
                            },
                            label = { Text("${list.emoji} ${list.name}") },
                            leadingIcon = if (checked) {
                                { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(14.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_lists_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink3,
                )
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_updates),
                icon = Icons.Outlined.SystemUpdate,
            ) {
                Text(
                    text = stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink3,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.update_channel),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Segmented(
                    options = listOf(
                        SegOption("github", stringResource(R.string.update_channel_github)),
                        SegOption("custom", stringResource(R.string.update_channel_custom)),
                    ),
                    selected = settings.updateChannel,
                    onSelect = { channel ->
                        feedback()
                        onUpdateChannel(channel)
                    },
                )
                if (settings.updateChannel == "custom") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.updateUrl,
                        onValueChange = onUpdateUrl,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.update_custom_url)) },
                        placeholder = { Text(stringResource(R.string.update_custom_url_hint)) },
                    )
                }
                Spacer(Modifier.height(8.dp))

                when {
                    update.downloading -> {
                        LinearProgressIndicator(
                            progress = { update.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.update_downloading,
                                (update.progress * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.ink3,
                        )
                    }

                    update.apk != null -> {
                        Text(
                            text = stringResource(R.string.update_ready),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val installed = UpdateInstaller.install(context, update.apk)
                                installHint = !installed
                                feedback()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brand,
                                contentColor = colors.onBrand,
                            ),
                        ) {
                            Icon(Icons.Outlined.SystemUpdate, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.update_install))
                        }
                        if (installHint) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.update_install_permission),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.danger,
                            )
                        }
                    }

                    update.info != null -> {
                        Text(
                            text = stringResource(R.string.update_available, update.info.version),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                        )
                        if (update.info.notes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = update.info.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.ink3,
                                maxLines = 6,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { feedback(); onDownloadUpdate() },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.brand,
                                    contentColor = colors.onBrand,
                                ),
                            ) {
                                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(stringResource(R.string.update_download))
                            }
                        }
                    }

                    update.checking -> {
                        Text(
                            text = stringResource(R.string.update_checking),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.ink3,
                        )
                    }

                    update.error != null -> {
                        Text(
                            text = update.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger,
                        )
                    }

                    update.checked -> {
                        Text(
                            text = stringResource(R.string.update_latest),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.success,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = { feedback(); onCheckUpdate() },
                        enabled = !update.checking && !update.downloading,
                    ) {
                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(14.dp), tint = colors.brandInk)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.update_check), color = colors.brandInk)
                    }
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_data),
                icon = Icons.Outlined.Storage,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { feedback(); onLoadDemo() }, shape = CircleShape) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(15.dp), tint = colors.ink2)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.data_load_demo), color = colors.ink2)
                    }
                    TextButton(onClick = { feedback(); showReset = true }) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(15.dp), tint = colors.danger)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.data_reset), color = colors.danger)
                    }
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_about),
                icon = Icons.Outlined.Info,
            ) {
                Text(
                    text = stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                )
            }
        }
    }

    if (showServer) {
        AlertDialog(
            onDismissRequest = { showServer = false },
            title = { Text(stringResource(R.string.server_settings)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = serverDraft,
                        onValueChange = { serverDraft = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.sync_server_url)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.server_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.ink3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onServer(serverDraft)
                        showServer = false
                    },
                ) { Text(stringResource(R.string.common_save), color = colors.brandInk) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            serverDraft = AppSettings.DEFAULT_SERVER
                            onServer(AppSettings.DEFAULT_SERVER)
                            showServer = false
                        },
                    ) { Text(stringResource(R.string.server_default), color = colors.ink3) }
                    TextButton(onClick = { showServer = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            },
            containerColor = colors.panel,
        )
    }

    if (showAiServer) {
        AlertDialog(
            onDismissRequest = { showAiServer = false },
            title = { Text(stringResource(R.string.ai_server_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = aiDraft,
                        onValueChange = { aiDraft = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.ai_server_url)) },
                        placeholder = { Text("https://api.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ai_server_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.ink3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAiServerUrl(aiDraft)
                        showAiServer = false
                    },
                ) { Text(stringResource(R.string.common_save), color = colors.brandInk) }
            },
            dismissButton = {
                TextButton(onClick = { showAiServer = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            containerColor = colors.panel,
        )
    }

    if (showAuth) {
        AuthDialog(
            register = authRegister,
            onDismiss = { showAuth = false },
            onLogin = { email, password, done -> onLogin(email, password, done) },
            onRegister = { email, password, name, done -> onRegister(email, password, name, done) },
            onSuccess = { showAuth = false },
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.data_reset)) },
            text = { Text(stringResource(R.string.data_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearLocal()
                    showReset = false
                }) { Text(stringResource(R.string.common_delete), color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            containerColor = colors.panel,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalGuoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.panel)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink3,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

private data class SegOption(
    val value: String,
    val label: String,
    val icon: ImageVector? = null,
)

@Composable
private fun Segmented(
    options: List<SegOption>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val colors = LocalGuoColors.current
    val feedback = rememberClickFeedback()
    val buttonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = colors.brandTint,
        activeContentColor = colors.brandInk,
        activeBorderColor = colors.brand.copy(alpha = 0.3f),
        inactiveContainerColor = colors.panel,
        inactiveContentColor = colors.ink3,
        inactiveBorderColor = colors.lineStrong,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            val active = option.value == selected
            SegmentedButton(
                selected = active,
                onClick = { feedback(); onSelect(option.value) },
                modifier = Modifier.weight(1f),
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = buttonColors,
                icon = {
                    if (option.icon != null) {
                        Icon(option.icon, contentDescription = null, modifier = Modifier.size(15.dp))
                    } else {
                        SegmentedButtonDefaults.Icon(active)
                    }
                },
                label = {
                    Text(
                        text = option.label,
                        maxLines = 1,
                        fontSize = 11.sp,
                    )
                },
            )
        }
    }
}

@Composable
private fun syncStatusText(sync: SyncManager.SyncUi, locale: Locale): String {
    val base = when (sync.status) {
        SyncManager.Status.IDLE -> stringResource(R.string.sync_status_idle)
        SyncManager.Status.SYNCING -> stringResource(R.string.sync_status_syncing)
        SyncManager.Status.OK -> stringResource(R.string.sync_status_ok)
        SyncManager.Status.ERROR -> stringResource(R.string.sync_status_error)
    }
    val last = sync.lastSync?.let { iso ->
        runCatching {
            Instant.parse(iso).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm", locale))
        }.getOrNull()
    }
    return if (last != null && sync.status == SyncManager.Status.OK) {
        base + " · " + stringResource(R.string.sync_last, last)
    } else {
        base
    }
}

@Composable
private fun AuthDialog(
    register: Boolean,
    onDismiss: () -> Unit,
    onLogin: (String, String, (Throwable?) -> Unit) -> Unit,
    onRegister: (String, String, String, (Throwable?) -> Unit) -> Unit,
    onSuccess: () -> Unit,
) {
    val colors = LocalGuoColors.current
    val context = LocalContext.current
    var isRegister by remember(register) { mutableStateOf(register) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        if (loading) return
        error = null
        loading = true
        val done: (Throwable?) -> Unit = { failure ->
            loading = false
            if (failure == null) {
                onSuccess()
            } else {
                error = if (failure is ApiException && failure.status == 401) {
                    context.getString(R.string.sync_auth_error)
                } else {
                    failure.message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.sync_generic_error)
                }
            }
        }
        if (isRegister) onRegister(email, password, name, done) else onLogin(email, password, done)
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = {
            Text(stringResource(if (isRegister) R.string.sync_create_account else R.string.sync_sign_in))
        },
        text = {
            Column {
                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.sync_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.sync_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.sync_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, color = colors.danger, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        isRegister = !isRegister
                        error = null
                    },
                    enabled = !loading,
                ) {
                    Text(
                        text = stringResource(if (isRegister) R.string.sync_sign_in else R.string.sync_create_account),
                        color = colors.brandInk,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = !loading) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = colors.brand)
                } else {
                    Text(stringResource(if (isRegister) R.string.sync_create_account else R.string.sync_sign_in))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = colors.panel,
    )
}
