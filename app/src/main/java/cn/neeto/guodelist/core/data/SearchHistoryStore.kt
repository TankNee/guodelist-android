package cn.neeto.guodelist.core.data

import android.content.Context

/** Small, local-only list of recent search terms. */
object SearchHistoryStore {
    private const val PREFS = "guodelist_search"
    private const val KEY = "history"
    private const val MAX = 8

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return raw?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
    }

    fun record(context: Context, query: String) {
        val term = query.trim()
        if (term.isEmpty()) return
        val next = (listOf(term) + load(context).filterNot { it.equals(term, ignoreCase = true) })
            .take(MAX)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, next.joinToString("\n")).apply()
    }

    fun remove(context: Context, query: String) {
        val next = load(context).filterNot { it == query }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, next.joinToString("\n")).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
