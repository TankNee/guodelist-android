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

/** HyperOS 4x2 card: what is coming up across the selected lists. */
class UpcomingWidgetProvider : AppWidgetProvider() {

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
        if (intent.action == TodayWidgetProvider.ACTION_REFRESH) refresh(context)
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, UpcomingWidgetProvider::class.java))
                .forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val snapshot = runCatching { WidgetData.upcoming(context, 3) }
                .getOrElse { WidgetSnapshot("", "", 0, 0, emptyList()) }
            val views = RemoteViews(context.packageName, R.layout.widget_upcoming)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_upcoming_title))
            val date = listOf(snapshot.dateLabel, snapshot.weekdayLabel)
                .filter { it.isNotBlank() }
                .joinToString("  ·  ")
            views.setTextViewText(R.id.widget_date, date)

            val shown = WidgetSupport.bindRows(
                context,
                views,
                intArrayOf(R.id.widget_task_1, R.id.widget_task_2, R.id.widget_task_3),
                snapshot,
            )
            views.setTextViewText(
                R.id.widget_empty,
                context.getString(R.string.widget_upcoming_empty),
            )
            views.setViewVisibility(
                R.id.widget_empty,
                if (shown == 0) View.VISIBLE else View.GONE,
            )
            if (snapshot.more > 0) {
                views.setTextViewText(R.id.widget_more, context.getString(R.string.widget_more, snapshot.more))
                views.setViewVisibility(R.id.widget_more, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_more, View.GONE)
            }

            views.setOnClickPendingIntent(R.id.widget_root, WidgetSupport.appIntent(context))
            views.setOnClickPendingIntent(
                R.id.widget_refresh,
                WidgetSupport.refreshIntent(context, UpcomingWidgetProvider::class.java),
            )
            manager.updateAppWidget(id, views)
        }
    }
}
