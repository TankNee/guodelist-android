package cn.neeto.guodelist.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import cn.neeto.guodelist.R

/**
 * HyperOS 2x2 / 4x2 / 4x4 Today widget.
 *
 * Design follows Xiaomi's widget guidance: 24dp radius, >=16dp safe area,
 * MiSans through the system sans-serif family, one strong number, 40% opacity
 * secondary text, dark mode and no scrolling or text input.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refresh(context)
    }

    private data class Spec(
        val layout: Int,
        val rowIds: IntArray,
        val maxRows: Int,
        val hasDate: Boolean,
        val hasMore: Boolean,
    )

    companion object {
        const val ACTION_REFRESH = "cn.neeto.guodelist.REFRESH_WIDGET"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
                .forEach { update(context, manager, it) }
            UpcomingWidgetProvider.refresh(context)
            QuickAddWidgetProvider.refresh(context)
        }

        private fun specFor(manager: AppWidgetManager, id: Int): Spec {
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            return when {
                height >= 220 -> Spec(
                    R.layout.widget_today_large,
                    intArrayOf(
                        R.id.widget_task_1, R.id.widget_task_2, R.id.widget_task_3,
                        R.id.widget_task_4, R.id.widget_task_5, R.id.widget_task_6,
                    ),
                    6,
                    hasDate = true,
                    hasMore = true,
                )
                width in 1..219 -> Spec(
                    R.layout.widget_today_small,
                    intArrayOf(R.id.widget_next),
                    1,
                    hasDate = false,
                    hasMore = false,
                )
                else -> Spec(
                    R.layout.widget_today,
                    intArrayOf(R.id.widget_task_1, R.id.widget_task_2, R.id.widget_task_3),
                    3,
                    hasDate = true,
                    hasMore = true,
                )
            }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val spec = specFor(manager, id)
            val snapshot = runCatching { WidgetData.today(context, spec.maxRows) }
                .getOrElse { WidgetSnapshot("", "", 0, 0, emptyList()) }
            val views = RemoteViews(context.packageName, spec.layout)

            views.setTextViewText(R.id.widget_count, snapshot.openCount.toString())
            views.setTextViewText(R.id.widget_count_label, context.getString(R.string.widget_open_count))
            views.setProgressBar(R.id.widget_progress, 100, WidgetSupport.progress(snapshot), false)

            if (spec.hasDate) {
                val date = listOf(snapshot.dateLabel, snapshot.weekdayLabel)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  ")
                views.setTextViewText(R.id.widget_date, date)
            }

            val shown = WidgetSupport.bindRows(context, views, spec.rowIds, snapshot)
            views.setTextViewText(
                R.id.widget_empty,
                context.getString(R.string.widget_today_empty),
            )
            views.setViewVisibility(
                R.id.widget_empty,
                if (shown == 0) View.VISIBLE else View.GONE,
            )

            if (spec.hasMore) {
                if (snapshot.more > 0) {
                    views.setTextViewText(
                        R.id.widget_more,
                        context.getString(R.string.widget_more, snapshot.more),
                    )
                    views.setViewVisibility(R.id.widget_more, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_more, View.GONE)
                }
            }

            views.setOnClickPendingIntent(R.id.widget_root, WidgetSupport.appIntent(context))
            if (spec.hasDate || spec.layout != R.layout.widget_today_small) {
                views.setOnClickPendingIntent(
                    R.id.widget_refresh,
                    WidgetSupport.refreshIntent(context, TodayWidgetProvider::class.java),
                )
            }
            manager.updateAppWidget(id, views)
        }
    }
}
