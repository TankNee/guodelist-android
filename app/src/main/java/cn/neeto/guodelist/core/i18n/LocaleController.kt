package cn.neeto.guodelist.core.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** App-level language override; "system" keeps the device locale. */
object LocaleController {

    fun wrap(base: Context, language: String): Context {
        if (language != "zh" && language != "en") return base
        val locale = Locale.forLanguageTag(if (language == "zh") "zh-CN" else "en")
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    fun tag(language: String): String = when (language) {
        "zh" -> "zh-CN"
        "en" -> "en"
        else -> Locale.getDefault().toLanguageTag()
    }

    /** Context for Compose so language changes apply without recreating the Activity. */
    fun languageContext(base: Context, language: String): Context {
        val locale = when (language) {
            "zh" -> Locale.forLanguageTag("zh-CN")
            "en" -> Locale.forLanguageTag("en")
            else -> Resources.getSystem().configuration.locales[0]
        }
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    /**
     * Context for workers, the widget and notification channels. The activity
     * gets its locale through attachBaseContext; these components only have the
     * application context, so they have to apply the stored choice themselves.
     */
    fun backgroundContext(base: Context): Context {
        val language = base
            .getSharedPreferences("guodelist_settings", Context.MODE_PRIVATE)
            .getString("language", "system") ?: "system"
        if (language != "zh" && language != "en") return base
        val locale = Locale.forLanguageTag(if (language == "zh") "zh-CN" else "en")
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    fun displayName(language: String): String = when (language) {
        "zh" -> "简体中文"
        "en" -> "English"
        else -> "System"
    }
}
