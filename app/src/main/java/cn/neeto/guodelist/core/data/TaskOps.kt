package cn.neeto.guodelist.core.data

import cn.neeto.guodelist.core.model.LocalData
import cn.neeto.guodelist.core.model.Priority
import cn.neeto.guodelist.core.model.Removed
import cn.neeto.guodelist.core.model.SubtaskItem
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.model.bool
import cn.neeto.guodelist.core.model.child
import cn.neeto.guodelist.core.model.array
import cn.neeto.guodelist.core.model.string
import cn.neeto.guodelist.core.model.stringOrNull
import cn.neeto.guodelist.core.model.with
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.core.util.Recurrence
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pure task/list mutations, kept away from Android so they can be unit tested
 * and so the sync merge stays identical to the desktop implementation.
 */
object TaskOps {

    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    data class NewTask(
        val title: String,
        val notes: String = "",
        val listId: String = "inbox",
        val dueDate: LocalDate? = null,
        val dueTime: LocalTime? = null,
        val priority: Priority = Priority.NONE,
        val recurrence: JsonObject? = null,
    )

    fun newTask(id: String, input: NewTask, now: String, order: Double): JsonObject = JsonObject(
        linkedMapOf(
            "id" to JsonPrimitive(id),
            "title" to JsonPrimitive(input.title.trim().ifEmpty { "Untitled" }),
            "notes" to JsonPrimitive(input.notes),
            "listId" to JsonPrimitive(input.listId),
            "done" to JsonPrimitive(false),
            "completedAt" to JsonNull,
            "dueDate" to (input.dueDate?.let { JsonPrimitive(Dates.key(it)) } ?: JsonNull),
            "dueTime" to (input.dueTime?.let { JsonPrimitive(it.format(timeFormat)) } ?: JsonNull),
            "endTime" to JsonNull,
            "priority" to JsonPrimitive(input.priority.raw),
            "labels" to JsonArray(emptyList()),
            "subtasks" to JsonArray(emptyList()),
            "attachments" to JsonArray(emptyList()),
            "recurrence" to (input.recurrence ?: JsonNull),
            "createdAt" to JsonPrimitive(now),
            "updatedAt" to JsonPrimitive(now),
            "order" to JsonPrimitive(order),
        ),
    )

    fun nextOrder(tasks: List<TaskItem>): Double =
        (tasks.minOfOrNull { it.order } ?: 0.0) - 1.0

    fun addTask(data: LocalData, input: NewTask, id: String, now: String): LocalData {
        val task = newTask(id, input, now, nextOrder(data.tasks))
        return data.copy(tasks = listOf(TaskItem(task)) + data.tasks, revision = data.revision + 1)
    }

    fun toggleTask(data: LocalData, id: String, now: String, newId: String): LocalData {
        val current = data.tasks.firstOrNull { it.id == id } ?: return data
        if (!current.done && current.recurrence != null) {
            val next = Recurrence.nextDate(current)
            val spawnJson = JsonObject(
                LinkedHashMap(current.json).apply {
                    put("id", JsonPrimitive(newId))
                    put("done", JsonPrimitive(false))
                    put("completedAt", JsonNull)
                    put("dueDate", next?.let { JsonPrimitive(Dates.key(it)) } ?: JsonNull)
                    put("createdAt", JsonPrimitive(now))
                    put("updatedAt", JsonPrimitive(now))
                    put("subtasks", JsonArray(current.subtasks.map { SubtaskItem(it.json.with("done" to JsonPrimitive(false))).json }))
                },
            )
            val doneJson = current.json.with(
                "done" to JsonPrimitive(true),
                "completedAt" to JsonPrimitive(now),
                "updatedAt" to JsonPrimitive(now),
                "subtasks" to JsonArray(current.subtasks.map { SubtaskItem(it.json.with("done" to JsonPrimitive(true))).json }),
            )
            val tasks = data.tasks.map { if (it.id == id) TaskItem(doneJson) else it }
            return data.copy(tasks = listOf(TaskItem(spawnJson)) + tasks, revision = data.revision + 1)
        }

        val subtasks = current.subtasks.map { subtask ->
            if (current.done) subtask.json
            else subtask.json.with("done" to JsonPrimitive(true))
        }
        val updated = current.json.with(
            "done" to JsonPrimitive(!current.done),
            "completedAt" to (if (current.done) JsonNull else JsonPrimitive(now)),
            "updatedAt" to JsonPrimitive(now),
            "subtasks" to JsonArray(subtasks),
        )
        return data.copy(
            tasks = data.tasks.map { if (it.id == id) TaskItem(updated) else it },
            revision = data.revision + 1,
        )
    }

    fun updateTask(data: LocalData, id: String, patch: Map<String, JsonElement>, now: String): LocalData {
        val current = data.tasks.firstOrNull { it.id == id } ?: return data
        val updated = JsonObject(LinkedHashMap(current.json).apply {
            putAll(patch)
            put("updatedAt", JsonPrimitive(now))
        })
        return data.copy(
            tasks = data.tasks.map { if (it.id == id) TaskItem(updated) else it },
            revision = data.revision + 1,
        )
    }

    fun deleteTask(data: LocalData, id: String, now: String): LocalData {
        val removed = data.removed.copy(tasks = data.removed.tasks + (id to now))
        return data.copy(
            tasks = data.tasks.filterNot { it.id == id },
            removed = removed,
            revision = data.revision + 1,
        )
    }

    fun restoreTask(data: LocalData, json: JsonObject, now: String): LocalData {
        val id = json.string("id")
        if (id.isBlank()) return data
        val restored = JsonObject(LinkedHashMap(json).apply { put("updatedAt", JsonPrimitive(now)) })
        return data.copy(
            tasks = listOf(TaskItem(restored)) + data.tasks.filterNot { it.id == id },
            removed = data.removed.copy(tasks = data.removed.tasks - id),
            revision = data.revision + 1,
        )
    }

    fun addList(data: LocalData, id: String, name: String, emoji: String, now: String): LocalData {
        val json = JsonObject(
            linkedMapOf(
                "id" to JsonPrimitive(id),
                "name" to JsonPrimitive(name.trim().ifEmpty { "List" }),
                "emoji" to JsonPrimitive(emoji.ifEmpty { "📋" }),
                "groupId" to JsonPrimitive(""),
                "createdAt" to JsonPrimitive(now),
                "updatedAt" to JsonPrimitive(now),
            ),
        )
        return data.copy(lists = data.lists + TaskList(json), revision = data.revision + 1)
    }

    fun renameList(data: LocalData, id: String, name: String, now: String): LocalData = data.copy(
        lists = data.lists.map {
            if (it.id == id) TaskList(it.json.with("name" to JsonPrimitive(name), "updatedAt" to JsonPrimitive(now)))
            else it
        },
        revision = data.revision + 1,
    )

    fun deleteList(data: LocalData, id: String, now: String): LocalData = data.copy(
        lists = data.lists.filterNot { it.id == id },
        tasks = data.tasks.filterNot { it.listId == id },
        removed = data.removed.copy(lists = data.removed.lists + (id to now)),
        revision = data.revision + 1,
    )

    fun addSubtask(data: LocalData, taskId: String, title: String, id: String, now: String): LocalData {
        val task = data.tasks.firstOrNull { it.id == taskId } ?: return data
        val subtasks = task.subtasks.map { it.json } + JsonObject(
            linkedMapOf(
                "id" to JsonPrimitive(id),
                "title" to JsonPrimitive(title.trim()),
                "done" to JsonPrimitive(false),
            ),
        )
        return updateTask(data, taskId, mapOf("subtasks" to JsonArray(subtasks)), now)
    }

    fun toggleSubtask(data: LocalData, taskId: String, subtaskId: String, now: String): LocalData {
        val task = data.tasks.firstOrNull { it.id == taskId } ?: return data
        val subtasks = task.subtasks.map {
            if (it.id == subtaskId) it.json.with("done" to JsonPrimitive(!it.done)) else it.json
        }
        return updateTask(data, taskId, mapOf("subtasks" to JsonArray(subtasks)), now)
    }

    fun removeSubtask(data: LocalData, taskId: String, subtaskId: String, now: String): LocalData {
        val task = data.tasks.firstOrNull { it.id == taskId } ?: return data
        val subtasks = task.subtasks.filterNot { it.id == subtaskId }.map { it.json }
        return updateTask(data, taskId, mapOf("subtasks" to JsonArray(subtasks)), now)
    }

    fun reorder(data: LocalData, activeId: String, overId: String): LocalData {
        if (activeId == overId) return data
        val active = data.tasks.firstOrNull { it.id == activeId } ?: return data
        val over = data.tasks.firstOrNull { it.id == overId } ?: return data
        if (active.dueDate != over.dueDate) return data

        val siblings = data.tasks.filter { it.dueDate == active.dueDate }.sortedBy { it.order }
        val from = siblings.indexOfFirst { it.id == activeId }
        val to = siblings.indexOfFirst { it.id == overId }
        if (from < 0 || to < 0) return data

        val next = siblings.toMutableList().apply { add(to, removeAt(from)) }
        val index = next.indexOfFirst { it.id == activeId }
        val before = next.getOrNull(index - 1)
        val after = next.getOrNull(index + 1)
        val order = when {
            before != null && after != null -> (before.order + after.order) / 2
            before != null -> before.order + 1
            after != null -> after.order - 1
            else -> active.order
        }
        return data.copy(
            tasks = data.tasks.map { if (it.id == activeId) TaskItem(it.json.with("order" to JsonPrimitive(order))) else it },
            revision = data.revision + 1,
        )
    }

    /**
     * Last-write-wins merge for live records, with permanent tombstones.
     *
     * A deletion is final for a known id. A device that still has an old copy
     * must not resurrect a list/task the user already deleted. Explicit restore
     * paths remove the tombstone before re-adding the record.
     */
    fun applyRemote(
        data: LocalData,
        remoteLists: List<TaskList>,
        remoteTasks: List<TaskItem>,
        deleted: Removed,
    ): LocalData {
        // The v1 server returns its global tombstone map. Foreign tombstones
        // (including another user's "inbox") must never delete this account's
        // records. Only accept tombstones that touch an id this device or the
        // incoming snapshot actually knows about, and never delete the virtual
        // Inbox list.
        val localTaskIds = data.tasks.mapTo(mutableSetOf()) { it.id }
        val remoteTaskIds = remoteTasks.mapTo(mutableSetOf()) { it.id }
        val localListIds = data.lists.mapTo(mutableSetOf()) { it.id }.apply {
            data.tasks.forEach { add(it.listId) }
        }
        val remoteListIds = remoteLists.mapTo(mutableSetOf()) { it.id }
        val localDeadLists = data.removed.lists - "inbox"
        val acceptedRemoteTasks = deleted.tasks.filterKeys {
            it in localTaskIds || it in remoteTaskIds || it in data.removed.tasks
        }
        val acceptedRemoteLists = deleted.lists.filterKeys {
            it != "inbox" && (it in localListIds || it in remoteListIds || it in localDeadLists)
        }
        val deadTasks = data.removed.tasks + acceptedRemoteTasks
        val deadLists = localDeadLists + acceptedRemoteLists
        val taskMap = LinkedHashMap<String, TaskItem>().apply { data.tasks.forEach { put(it.id, it) } }
        for (remote in remoteTasks) {
            if (deadTasks.containsKey(remote.id)) continue
            val local = taskMap[remote.id]
            if (local == null || remote.updatedAt >= local.updatedAt) taskMap[remote.id] = remote
        }
        val listMap = LinkedHashMap<String, TaskList>().apply { data.lists.forEach { put(it.id, it) } }
        for (remote in remoteLists) {
            if (deadLists.containsKey(remote.id)) continue
            val local = listMap[remote.id]
            if (local == null || remote.updatedAt >= local.updatedAt) listMap[remote.id] = remote
        }
        val survivingLists = listMap.values.filterNot { deadLists.containsKey(it.id) }
        val survivingTasks = taskMap.values.filterNot { task ->
            deadTasks.containsKey(task.id) || deadLists.containsKey(task.listId)
        }
        return data.copy(
            tasks = survivingTasks,
            lists = survivingLists,
            removed = Removed(lists = deadLists, tasks = deadTasks),
        )
    }

    fun clearLocal(data: LocalData): LocalData = data.copy(
        tasks = emptyList(),
        lists = emptyList(),
        removed = Removed(),
        revision = data.revision + 1,
    )

    fun replaceAll(data: LocalData, lists: List<TaskList>, tasks: List<TaskItem>, removed: Removed): LocalData =
        data.copy(lists = lists, tasks = tasks, removed = removed, revision = data.revision + 1)

    fun newInstant(): String = Instant.now().toString()

    fun subtaskCount(task: TaskItem): Pair<Int, Int> =
        task.subtasks.count { it.done } to task.subtasks.size

    fun isOverdue(task: TaskItem, today: LocalDate): Boolean {
        val due = Dates.parse(task.dueDate) ?: return false
        return !task.done && due.isBefore(today)
    }

    /** Kept for future migrations that need to inspect a raw field. */
    fun rawString(task: TaskItem, key: String): String? = task.json.stringOrNull(key)

    fun rawBool(task: TaskItem, key: String): Boolean = task.json.bool(key)

    fun rawChild(task: TaskItem, key: String): JsonObject? = task.json.child(key)
}
