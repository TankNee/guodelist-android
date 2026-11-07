package cn.neeto.guodelist.core.data

import android.content.Context
import android.util.Log
import cn.neeto.guodelist.core.model.LocalData
import cn.neeto.guodelist.core.model.Removed
import cn.neeto.guodelist.core.model.StoredData
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.model.with
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.widget.TodayWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID

/**
 * Offline-first storage: one JSON document in the app's private directory,
 * atomically replaced. It mirrors the desktop `guodelist:v1` document closely
 * enough that both clients can sync the same records without translation.
 */
class GuoStore(private val context: Context) {

    private val file = File(context.filesDir, "guodelist.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveMutex = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<LocalData> = _data.asStateFlow()

    /**
     * Tasks created locally in the last two minutes. A sync response that was
     * built before the create can arrive late; merging it must not make a
     * freshly created task vanish from the phone.
     */
    private val pendingTasks = mutableMapOf<String, Pair<TaskItem, Long>>()

    /** Emissions are local edits only — remote merges never schedule a push. */
    private val _localChanges = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val localChanges: SharedFlow<Long> = _localChanges.asSharedFlow()

    private var saveJob: Job? = null
    private var widgetJob: Job? = null

    init {
        if (!file.exists()) {
            scope.launch { persist(_data.value) }
        }
    }

    private fun load(): LocalData {
        if (!file.exists()) return LocalData()
        return runCatching {
            val stored = json.decodeFromString<StoredData>(file.readText())
            val deadLists = stored.removed.lists - "inbox"
            val deadTasks = stored.removed.tasks
            val lists = stored.lists.mapNotNull { obj ->
                obj.takeIf { it["id"]?.let { id -> (id as? JsonPrimitive)?.content?.isNotBlank() } == true }
                    ?.let(::TaskList)
            }.filterNot { it.id in deadLists }
            val tasks = stored.tasks.mapNotNull { obj ->
                obj.takeIf { it["id"]?.let { id -> (id as? JsonPrimitive)?.content?.isNotBlank() } == true }
                    ?.let(::TaskItem)
            }.filterNot { it.id in deadTasks || it.listId in deadLists }
            LocalData(
                lists = lists,
                tasks = tasks,
                // Tombstones are final. Keep them on load; clearing a tombstone
                // just because a live copy exists is how deleted lists used to
                // reappear after a restart/sync.
                removed = Removed(lists = deadLists, tasks = deadTasks),
            )
        }.getOrElse { error ->
            Log.w("GuoStore", "Could not read local data, starting fresh", error)
            runCatching { file.renameTo(File(context.filesDir, "guodelist.corrupt.json")) }
            LocalData()
        }
    }

    private fun commit(next: LocalData) {
        _data.value = next
        _localChanges.tryEmit(next.revision)
        scheduleSave()
        scheduleWidgetRefresh()
    }

    private fun scheduleWidgetRefresh() {
        widgetJob?.cancel()
        widgetJob = scope.launch {
            delay(450)
            runCatching { TodayWidgetProvider.refresh(context) }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(220)
            persist(_data.value)
        }
    }

    private suspend fun persist(data: LocalData) = saveMutex.withLock {
        runCatching {
            val stored = StoredData(
                lists = data.lists.map { it.json },
                tasks = data.tasks.map { it.json },
                removed = data.removed,
            )
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(StoredData.serializer(), stored))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }.onFailure { Log.e("GuoStore", "Could not persist tasks", it) }
    }

    fun flushAsync() {
        scope.launch { persist(_data.value) }
    }

    /* ----------------------------- tasks ------------------------------ */

    fun addTask(input: TaskOps.NewTask): String {
        val id = newId("tsk")
        val next = TaskOps.addTask(_data.value, input, id, TaskOps.newInstant())
        commit(next)
        next.tasks.firstOrNull { it.id == id }?.let {
            pendingTasks[id] = it to System.currentTimeMillis()
        }
        return id
    }

    fun toggleTask(id: String) {
        commit(TaskOps.toggleTask(_data.value, id, TaskOps.newInstant(), newId("tsk")))
    }

    fun updateTask(id: String, vararg patch: Pair<String, kotlinx.serialization.json.JsonElement>) {
        commit(TaskOps.updateTask(_data.value, id, patch.toMap(), TaskOps.newInstant()))
    }

    fun setTitle(id: String, title: String) =
        updateTask(id, "title" to JsonPrimitive(title))

    fun setNotes(id: String, notes: String) =
        updateTask(id, "notes" to JsonPrimitive(notes))

    fun setPriority(id: String, priority: String) =
        updateTask(id, "priority" to JsonPrimitive(priority))

    fun setList(id: String, listId: String) =
        updateTask(id, "listId" to JsonPrimitive(listId))

    fun setDueDate(id: String, date: LocalDate?) =
        updateTask(id, "dueDate" to (date?.let { JsonPrimitive(Dates.key(it)) } ?: JsonNull))

    fun setDueTime(id: String, time: LocalTime?) =
        updateTask(id, "dueTime" to (time?.let { JsonPrimitive(it.toString()) } ?: JsonNull))

    fun setRecurrence(id: String, recurrence: JsonObject?) =
        updateTask(id, "recurrence" to (recurrence ?: JsonNull))

    fun deleteTask(id: String) {
        pendingTasks.remove(id)
        commit(TaskOps.deleteTask(_data.value, id, TaskOps.newInstant()))
    }

    fun restoreTask(json: JsonObject) {
        commit(TaskOps.restoreTask(_data.value, json, TaskOps.newInstant()))
    }

    fun reorderTask(activeId: String, overId: String) {
        commit(TaskOps.reorder(_data.value, activeId, overId))
    }

    /* ----------------------------- subtasks ---------------------------- */

    fun addSubtask(taskId: String, title: String) {
        if (title.isBlank()) return
        commit(TaskOps.addSubtask(_data.value, taskId, title, newId("sub"), TaskOps.newInstant()))
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        commit(TaskOps.toggleSubtask(_data.value, taskId, subtaskId, TaskOps.newInstant()))
    }

    fun removeSubtask(taskId: String, subtaskId: String) {
        commit(TaskOps.removeSubtask(_data.value, taskId, subtaskId, TaskOps.newInstant()))
    }

    /* ------------------------------ lists ------------------------------ */

    fun addList(name: String, emoji: String): String {
        val id = newId("lst")
        commit(TaskOps.addList(_data.value, id, name, emoji, TaskOps.newInstant()))
        return id
    }

    fun renameList(id: String, name: String) {
        commit(TaskOps.renameList(_data.value, id, name, TaskOps.newInstant()))
    }

    fun deleteList(id: String) {
        commit(TaskOps.deleteList(_data.value, id, TaskOps.newInstant()))
    }

    /* ------------------------------- sync ------------------------------ */

    fun applyRemote(lists: List<TaskList>, tasks: List<TaskItem>, deleted: Removed) {
        val merged = TaskOps.applyRemote(_data.value, lists, tasks, deleted)
        val now = System.currentTimeMillis()
        pendingTasks.entries.removeAll { (_, value) -> now - value.second > 120_000L }
        val missing = pendingTasks.values
            .map { it.first }
            .filter { pending -> merged.tasks.none { it.id == pending.id } }
        _data.value = if (missing.isEmpty()) merged else merged.copy(tasks = missing + merged.tasks)
        scheduleSave()
        scheduleWidgetRefresh()
    }

    fun clearLocal() {
        commit(TaskOps.clearLocal(_data.value))
    }

    fun loadDemo() {
        val locale = context.resources.configuration.locales[0]
        val zh = locale.language == Locale.CHINESE.language
        val now = TaskOps.newInstant()
        val today = LocalDate.now()

        var data = TaskOps.clearLocal(_data.value)
        val work = newId("lst")
        val life = newId("lst")
        data = TaskOps.addList(data, work, if (zh) "工作" else "Work", "🗂️", now)
        data = TaskOps.addList(data, life, if (zh) "生活" else "Life", "🌿", now)

        val daily = JsonObject(
            linkedMapOf(
                "freq" to JsonPrimitive("daily"),
                "interval" to JsonPrimitive(1),
                "weekdays" to JsonNull,
            ),
        )
        val demo = listOf(
            TaskOps.NewTask(
                title = if (zh) "过一遍同步设计" else "Review the sync design",
                notes = if (zh) "和桌面端保持一致：按 updatedAt 后写者胜出。" else "Same rules as desktop: last write wins by updatedAt.",
                listId = work,
                dueDate = today,
                priority = cn.neeto.guodelist.core.model.Priority.HIGH,
            ),
            TaskOps.NewTask(
                title = if (zh) "给房东打电话" else "Call the landlord",
                listId = work,
                dueDate = today,
                dueTime = LocalTime.of(18, 0),
            ),
            TaskOps.NewTask(
                title = if (zh) "拉伸 10 分钟" else "Stretch for 10 minutes",
                listId = life,
                dueDate = today,
                recurrence = daily,
            ),
            TaskOps.NewTask(
                title = if (zh) "买牛奶和面包" else "Buy milk and bread",
                listId = life,
                dueDate = today.plusDays(1),
            ),
            TaskOps.NewTask(
                title = if (zh) "周末看一场电影" else "See a film this weekend",
                listId = life,
                dueDate = today.plusDays(3),
                priority = cn.neeto.guodelist.core.model.Priority.LOW,
            ),
        )
        for (item in demo) {
            data = TaskOps.addTask(data, item, newId("tsk"), now)
        }
        val doneTask = TaskOps.newTask(
            newId("tsk"),
            TaskOps.NewTask(
                title = if (zh) "整理下载文件夹" else "Tidy the downloads folder",
                listId = work,
                dueDate = today,
            ),
            now,
            TaskOps.nextOrder(data.tasks),
        ).with(
            "done" to JsonPrimitive(true),
            "completedAt" to JsonPrimitive(now),
        )
        data = data.copy(tasks = listOf(TaskItem(doneTask)) + data.tasks)
        commit(data.copy(revision = data.revision + 1))
    }

    private fun newId(prefix: String): String =
        prefix + "_" + UUID.randomUUID().toString().replace("-", "").take(18)
}
