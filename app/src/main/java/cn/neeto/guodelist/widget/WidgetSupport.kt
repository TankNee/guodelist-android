package cn.neeto.guodelist.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import cn.neeto.guodelist.MainActivity
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.notifications.Notifications

/** Small helpers shared by the three HyperOS-style home screen widgets. */
object WidgetSupport {

    fun taskIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(Notifications.EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun appIntent(context: Context, quickAdd: Boolean = false, tab: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (quickAdd) putExtra(MainActivity.EXTRA_QUICK_ADD, true)
            if (tab != null) putExtra(MainActivity.EXTRA_TAB, tab)
        }
        return PendingIntent.getActivity(
            context,
            if (quickAdd) 11 else 10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun refreshIntent(context: Context, provider: Class<*>): PendingIntent {
        val intent = Intent(context, provider).setAction(TodayWidgetProvider.ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context,
            provider.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun bindRows(
        context: Context,
        views: RemoteViews,
        rowIds: IntArray,
        snapshot: WidgetSnapshot,
    ): Int {
        rowIds.forEachIndexed { index, rowId ->
            val row = snapshot.rows.getOrNull(index)
            if (row == null) {
                views.setViewVisibility(rowId, View.GONE)
            } else {
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(rowId, formatRow(row))
                views.setOnClickPendingIntent(rowId, taskIntent(context, row.id))
            }
        }
        return snapshot.rows.size.coerceAtMost(rowIds.size)
    }

    fun progress(snapshot: WidgetSnapshot): Int {
        val total = snapshot.openCount + snapshot.doneCount
        if (total == 0) return 0
        return (snapshot.doneCount * 100 / total).coerceIn(0, 100)
    }

    fun formatRow(row: WidgetRow): String =
        if (row.time.isNullOrBlank()) row.title else "${row.time}  ${row.title}"
}
