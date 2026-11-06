package cn.neeto.guodelist.core.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import cn.neeto.guodelist.BuildConfig

/**
 * Launcher icon switching uses activity-alias entries. Only one alias is
 * enabled at a time; enabling the new one before disabling the others keeps
 * the launcher from briefly losing the app.
 */
object LauncherIcon {

    const val ADAPTIVE = "adaptive"
    const val ROUND = "round"
    const val SQUARE = "square"
    const val IRREGULAR = "irregular"

    val styles = listOf(ADAPTIVE, ROUND, SQUARE, IRREGULAR)

    private val aliases = mapOf(
        ADAPTIVE to "${BuildConfig.APPLICATION_ID}.IconAdaptive",
        ROUND to "${BuildConfig.APPLICATION_ID}.IconRound",
        SQUARE to "${BuildConfig.APPLICATION_ID}.IconSquare",
        IRREGULAR to "${BuildConfig.APPLICATION_ID}.IconIrregular",
    )

    fun normalize(style: String?): String = if (style in styles) style!! else ADAPTIVE

    fun apply(context: Context, style: String) {
        val selected = normalize(style)
        val packageManager = context.packageManager
        val chosen = ComponentName(context.packageName, aliases.getValue(selected))

        packageManager.setComponentEnabledSetting(
            chosen,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        aliases.forEach { (key, name) ->
            if (key == selected) return@forEach
            packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, name),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
