package cn.neeto.guodelist.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.neeto.guodelist.GuoApp

/**
 * WorkManager normally restores its queue after a reboot, but OEM task killers
 * can drop it, and a force-stop still clears everything. Rebuild the reminder
 * queue from the local task document on boot and after an app update.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            -> {
                val app = context.applicationContext as? GuoApp ?: return
                val container = app.container
                ReminderScheduler.reconcile(
                    context = context,
                    tasks = container.store.data.value.tasks,
                    settings = container.settings.state.value,
                )
            }
        }
    }
}
