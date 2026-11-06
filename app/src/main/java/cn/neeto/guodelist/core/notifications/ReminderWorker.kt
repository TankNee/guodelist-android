package cn.neeto.guodelist.core.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.i18n.LocaleController
import cn.neeto.guodelist.core.util.Dates
import java.time.LocalTime

/**
 * Fires once per pending task. WorkManager owns the schedule, so the reminder
 * survives the app process being killed and the phone rebooting; the worker is
 * started by the system when the delay is up.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.success()
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { return Result.success() }
        val dueDate = inputData.getString(KEY_DUE_DATE)
        val dueTime = inputData.getString(KEY_DUE_TIME)

        val prefs = applicationContext.getSharedPreferences("guodelist_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminders_enabled", true)) return Result.success()
        if (!Notifications.canPost(applicationContext)) return Result.success()

        val localized = LocaleController.backgroundContext(applicationContext)
        val locale = localized.resources.configuration.locales[0]
        val date = Dates.parse(dueDate)
        val time = Dates.parseTime(dueTime) ?: LocalTime.of(9, 0)
        val whenText = if (date != null) {
            Dates.shortDate(date, locale) + " " + Dates.formatTime(time, locale)
        } else {
            ""
        }
        val body = localized.getString(R.string.reminder_due_at, whenText)
        Notifications.postReminder(applicationContext, taskId, title, body)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"
        const val KEY_DUE_DATE = "due_date"
        const val KEY_DUE_TIME = "due_time"
    }
}
