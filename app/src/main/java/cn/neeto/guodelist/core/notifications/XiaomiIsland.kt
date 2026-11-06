package cn.neeto.guodelist.core.notifications

import android.app.Notification
import android.os.Build
import org.json.JSONObject

/**
 * Best-effort Xiaomi HyperOS "Super Island" payload.
 *
 * Xiaomi requires the app to be registered and whitelisted on the HyperOS developer
 * platform before the system renders an island. On unsupported devices, or before
 * approval, the extra is simply ignored and the normal notification remains.
 */
object XiaomiIsland {

    fun isXiaomi(): Boolean {
        val maker = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        return maker.contains("xiaomi", ignoreCase = true) || brand.contains("xiaomi", ignoreCase = true)
    }

    fun attach(notification: Notification, title: String, detail: String) {
        if (!isXiaomi()) return
        runCatching {
            notification.extras.putString("miui.focus.param", payload(title, detail))
        }
    }

    private fun payload(title: String, detail: String): String {
        val text = JSONObject()
            .put("frontTitle", "Guodelist")
            .put("title", title)
            .put("content", detail)
            .put("useHighLight", false)
        val textArea = JSONObject()
            .put("type", 1)
            .put("textInfo", text)
        val paramIsland = JSONObject()
            .put("islandProperty", 1)
            .put("bigIslandArea", JSONObject().put("imageTextInfoLeft", textArea))
            .put("smallIslandArea", JSONObject().put("imageTextInfoLeft", textArea))
        val param = JSONObject()
            .put("protocol", 1)
            .put("business", "todo")
            .put("enableFloat", true)
            .put("updatable", false)
            .put("ticker", title)
            .put("aodTitle", title)
            .put("param_island", paramIsland)
            .put("baseInfo", JSONObject()
                .put("title", title)
                .put("content", detail)
                .put("colorTitle", "#E1543A")
                .put("type", 2))
            .put("hintInfo", JSONObject().put("type", 1).put("title", detail))
        return JSONObject().put("param_v2", param).toString()
    }
}
