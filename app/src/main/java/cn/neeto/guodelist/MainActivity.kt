package cn.neeto.guodelist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.neeto.guodelist.core.i18n.LocaleController
import cn.neeto.guodelist.core.notifications.Notifications
import cn.neeto.guodelist.ui.AppRoot
import cn.neeto.guodelist.ui.AppViewModel
import cn.neeto.guodelist.ui.theme.GuoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory((application as GuoApp).container)
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private var pendingTaskId by mutableStateOf<String?>(null)
    private var pendingQuickAdd by mutableStateOf(false)
    private var pendingTab by mutableStateOf<String?>(null)

    companion object {
        const val EXTRA_QUICK_ADD = "cn.neeto.guodelist.QUICK_ADD"
        const val EXTRA_TAB = "cn.neeto.guodelist.TAB"
    }

    override fun attachBaseContext(newBase: Context) {
        val language = newBase
            .getSharedPreferences("guodelist_settings", MODE_PRIVATE)
            .getString("language", "system") ?: "system"
        super.attachBaseContext(LocaleController.wrap(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingTaskId = intent?.getStringExtra(Notifications.EXTRA_TASK_ID)
        pendingQuickAdd = intent?.getBooleanExtra(EXTRA_QUICK_ADD, false) == true
        pendingTab = intent?.getStringExtra(EXTRA_TAB)
        enableEdgeToEdge()
        setContent {
            val ui by viewModel.ui.collectAsStateWithLifecycle()
            val base = LocalContext.current
            val language = ui.settings.language
            val localized = remember(language) { LocaleController.languageContext(base, language) }
            val registryOwner = base as? ActivityResultRegistryOwner ?: this@MainActivity
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                GuoTheme(ui.settings) {
                    AppRoot(
                        vm = viewModel,
                        ui = ui,
                        initialTaskId = pendingTaskId,
                        initialTab = pendingTab,
                        initialQuickAdd = pendingQuickAdd,
                    )
                }
            }
        }
        askForNotificationsOnce()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTaskId = intent.getStringExtra(Notifications.EXTRA_TASK_ID)
        pendingQuickAdd = intent.getBooleanExtra(EXTRA_QUICK_ADD, false)
        pendingTab = intent.getStringExtra(EXTRA_TAB)
    }

    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        val prefs = getSharedPreferences("guodelist_settings", MODE_PRIVATE)
        if (prefs.getBoolean("asked_notifications", false)) return
        prefs.edit().putBoolean("asked_notifications", true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStop() {
        super.onStop()
        viewModel.flush()
    }
}
