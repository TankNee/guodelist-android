package cn.neeto.guodelist.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import cn.neeto.guodelist.R

/** HyperOS 2x2 quick-capture card. */
class QuickAddWidgetProvider : AppWidgetProvider() {

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
            manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))
                .forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val snapshot = runCatching { WidgetData.today(context, 1) }
                .getOrElse { WidgetSnapshot("", "", 0, 0, emptyList()) }
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
            views.setTextViewText(
                R.id.widget_quick_sub,
                context.getString(R.string.widget_quick_sub, snapshot.openCount),
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetSupport.appIntent(context, quickAdd = true),
            )
            manager.updateAppWidget(id, views)
        }
    }
}
