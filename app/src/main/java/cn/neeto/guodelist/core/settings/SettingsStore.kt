package cn.neeto.guodelist.core.settings

import android.content.Context
import cn.neeto.guodelist.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val theme: String = "system",
    val pack: String = "classic",
    val language: String = "system",
    val serverUrl: String = DEFAULT_SERVER,
    val remindersEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 10,
    val widgetListIds: Set<String> = emptySet(),
    val updateToken: String = "",
    val iconStyle: String = "adaptive",
    val updateChannel: String = BuildConfig.UPDATE_SOURCE,
    val updateUrl: String = "",
    val aiServerUrl: String = "",
) {
    companion object {
        val DEFAULT_SERVER = BuildConfig.DEFAULT_SERVER
    }
}

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("guodelist_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    init {
        if (!prefs.contains("server_url")) {
            prefs.edit().putString("server_url", AppSettings.DEFAULT_SERVER).apply()
        }
    }

    private fun read(): AppSettings = AppSettings(
        theme = prefs.getString("theme", "system") ?: "system",
        pack = prefs.getString("pack", "classic") ?: "classic",
        language = prefs.getString("language", "system") ?: "system",
        serverUrl = prefs.getString("server_url", AppSettings.DEFAULT_SERVER)
            ?.trim()?.trimEnd('/')?.ifBlank { AppSettings.DEFAULT_SERVER }
            ?: AppSettings.DEFAULT_SERVER,
        remindersEnabled = prefs.getBoolean("reminders_enabled", true),
        reminderLeadMinutes = prefs.getInt("reminder_lead_minutes", 10),
        widgetListIds = prefs.getStringSet("widget_list_ids", emptySet())?.toSet().orEmpty(),
        updateToken = prefs.getString("update_token", "").orEmpty(),
        iconStyle = prefs.getString("icon_style", "adaptive") ?: "adaptive",
        updateChannel = prefs.getString("update_channel", BuildConfig.UPDATE_SOURCE)
            ?: BuildConfig.UPDATE_SOURCE,
        updateUrl = prefs.getString("update_url", "").orEmpty(),
        aiServerUrl = prefs.getString("ai_server_url", "").orEmpty(),
    )

    fun setTheme(theme: String) = write { putString("theme", theme) }

    fun setPack(pack: String) = write { putString("pack", pack) }

    fun setLanguage(language: String) = write { putString("language", language) }

    fun setServerUrl(url: String) = write {
        putString("server_url", url.trim().trimEnd('/').ifBlank { AppSettings.DEFAULT_SERVER })
    }

    fun setRemindersEnabled(enabled: Boolean) = write { putBoolean("reminders_enabled", enabled) }

    fun setReminderLeadMinutes(minutes: Int) = write {
        putInt("reminder_lead_minutes", minutes.coerceIn(0, 24 * 60))
    }

    fun setWidgetListIds(ids: Set<String>) = write { putStringSet("widget_list_ids", ids) }

    fun setUpdateToken(token: String) = write { putString("update_token", token.trim()) }

    fun setUpdateChannel(channel: String) = write { putString("update_channel", channel) }

    fun setUpdateUrl(url: String) = write { putString("update_url", url.trim()) }

    fun setAiServerUrl(url: String) = write { putString("ai_server_url", url.trim()) }

    fun setIconStyle(style: String) = write { putString("icon_style", style) }

    private inline fun write(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _state.value = read()
    }

    val serverUrl: String get() = _state.value.serverUrl
}

data class Account(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "user",
)

class AuthStore(context: Context) {

    private val prefs = context.getSharedPreferences("guodelist_auth", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "").orEmpty()
        private set(value) = prefs.edit().putString("token", value).apply()

    var account: Account
        get() = Account(
            id = prefs.getString("id", "").orEmpty(),
            email = prefs.getString("email", "").orEmpty(),
            name = prefs.getString("name", "").orEmpty(),
            role = prefs.getString("role", "user").orEmpty(),
        )
        private set(value) = prefs.edit()
            .putString("id", value.id)
            .putString("email", value.email)
            .putString("name", value.name)
            .putString("role", value.role)
            .apply()

    var lastSync: String?
        get() = prefs.getString("last_sync", null)
        set(value) = prefs.edit().putString("last_sync", value).apply()

    fun save(token: String, account: Account) {
        this.token = token
        this.account = account
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
