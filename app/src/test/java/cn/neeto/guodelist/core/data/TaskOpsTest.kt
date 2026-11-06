package cn.neeto.guodelist.core.data

import cn.neeto.guodelist.core.model.Priority
import cn.neeto.guodelist.core.model.Removed
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.model.with
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskOpsTest {

    private fun task(
        id: String,
        updatedAt: String = "2026-01-01T00:00:00Z",
        done: Boolean = false,
        dueDate: String? = null,
        priority: String = "none",
        listId: String = "inbox",
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("title", "Task $id")
        put("notes", "")
        put("listId", listId)
        put("done", done)
        put("completedAt", null as String?)
        put("dueDate", dueDate)
        put("dueTime", null as String?)
        put("endTime", null as String?)
        put("priority", priority)
        put("createdAt", "2026-01-01T00:00:00Z")
        put("updatedAt", updatedAt)
        put("order", 0)
        put("labels", kotlinx.serialization.json.JsonArray(emptyList()))
        put("subtasks", kotlinx.serialization.json.JsonArray(emptyList()))
        put("attachments", kotlinx.serialization.json.JsonArray(emptyList()))
        put("recurrence", null as String?)
    }

    private fun list(id: String, updatedAt: String = "2026-01-01T00:00:00Z"): TaskList =
        TaskList(buildJsonObject {
            put("id", id)
            put("name", "List $id")
            put("emoji", "📋")
            put("groupId", "projects")
            put("updatedAt", updatedAt)
        })

    @Test
    fun addTaskPutsNewestFirst() {
        val base = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1"))),
        )
        val updated = TaskOps.addTask(
            data = base,
            input = TaskOps.NewTask(title = "Write the sync test", listId = "inbox"),
            id = "t2",
            now = "2026-02-01T10:00:00Z",
        )
        assertEquals(2, updated.tasks.size)
        assertEquals("t2", updated.tasks.first().id)
        assertEquals(1L, updated.revision)
        assertTrue(updated.tasks.first().order < updated.tasks[1].order)
    }

    @Test
    fun deleteRecordsTombstone() {
        val base = cn.neeto.guodelist.core.model.LocalData(tasks = listOf(TaskItem(task("t1"))))
        val updated = TaskOps.deleteTask(base, "t1", "2026-03-01T10:00:00Z")
        assertTrue(updated.tasks.isEmpty())
        assertEquals("2026-03-01T10:00:00Z", updated.removed.tasks["t1"])
    }

    @Test
    fun remoteNewerWinsAndOlderLoses() {        val base = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1", updatedAt = "2026-03-01T00:00:00Z"))),
        )
        val newer = TaskItem(task("t1", updatedAt = "2026-03-02T00:00:00Z", priority = "high"))
        val merged = TaskOps.applyRemote(base, emptyList(), listOf(newer), Removed())
        assertEquals("high", merged.tasks.single().priority.raw)

        val older = TaskItem(task("t1", updatedAt = "2026-02-01T00:00:00Z", priority = "low"))
        val mergedBack = TaskOps.applyRemote(merged, emptyList(), listOf(older), Removed())
        assertEquals("high", mergedBack.tasks.single().priority.raw)
    }

    @Test
    fun deletingAListCascadesToItsTasks() {
        val base = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1", listId = "lst_1"))),
        )
        val merged = TaskOps.applyRemote(
            data = base,
            remoteLists = emptyList(),
            remoteTasks = emptyList(),
            deleted = Removed(lists = mapOf("lst_1" to "2026-05-01T00:00:00Z")),
        )
        assertTrue(merged.tasks.isEmpty())
    }

    @Test
    fun remoteCopyWithNewerTimestampCannotResurrectDeletedTask() {
        val base = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1"))),
            removed = Removed(tasks = mapOf("t1" to "2026-04-01T00:00:00Z")),
        )
        val remote = TaskItem(task("t1", updatedAt = "2026-04-02T00:00:00Z"))
        val merged = TaskOps.applyRemote(base, emptyList(), listOf(remote), Removed())
        assertTrue(merged.tasks.isEmpty())
        assertTrue(merged.removed.tasks.containsKey("t1"))
    }

    @Test
    fun unknownFieldsSurviveAnEdit() {
        val raw = buildJsonObject {
            put("id", "t1")
            put("title", "Keep my attachments")
            put("notes", "")
            put("listId", "inbox")
            put("done", false)
            put("priority", "none")
            put("createdAt", "2026-01-01T00:00:00Z")
            put("updatedAt", "2026-01-01T00:00:00Z")
            put("order", 0)
            put("attachments", kotlinx.serialization.json.JsonArray(listOf(buildJsonObject { put("id", "a1") })))
        }
        val base = cn.neeto.guodelist.core.model.LocalData(tasks = listOf(TaskItem(raw)))
        val updated = TaskOps.updateTask(base, "t1", mapOf("title" to kotlinx.serialization.json.JsonPrimitive("Renamed")), "2026-02-01T00:00:00Z")
        val json = updated.tasks.single().json
        assertTrue(json.containsKey("attachments"))
        assertEquals("Renamed", updated.tasks.single().title)
    }

    @Test
    fun completingRecurringTaskSpawnsNextOccurrence() {
        val recurrence = kotlinx.serialization.json.buildJsonObject {
            put("freq", "daily")
            put("interval", 1)
        }
        val raw = task("t1", dueDate = "2026-05-01")
            .let { JsonObject(it + mapOf("recurrence" to recurrence)) }
        val base = cn.neeto.guodelist.core.model.LocalData(tasks = listOf(TaskItem(raw)))
        val updated = TaskOps.toggleTask(base, "t1", "2026-05-01T09:00:00Z", "t2")
        assertEquals(2, updated.tasks.size)
        val done = updated.tasks.first { it.id == "t1" }
        val spawn = updated.tasks.first { it.id == "t2" }
        assertTrue(done.done)
        assertFalse(spawn.done)
        assertEquals("2026-05-02", spawn.dueDate)
    }

    @Test
    fun overdueDetectionIgnoresCompletedTasks() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val open = TaskItem(task("t1", dueDate = yesterday))
        val done = TaskItem(task("t2", dueDate = yesterday, done = true))
        assertTrue(TaskOps.isOverdue(open, LocalDate.now()))
        assertFalse(TaskOps.isOverdue(done, LocalDate.now()))
        assertNull(open.dueTime)
    }

    @Test
    fun togglingPlainTaskSetsCompletedAt() {
        val base = cn.neeto.guodelist.core.model.LocalData(tasks = listOf(TaskItem(task("t1"))))
        val updated = TaskOps.toggleTask(base, "t1", "2026-06-01T12:00:00Z", "t2")
        assertTrue(updated.tasks.single().done)
        assertEquals("2026-06-01T12:00:00Z", updated.tasks.single().completedAt)
    }

    @Test
    fun newTaskKeepsServerCompatibleFieldNames() {
        val json = TaskOps.newTask(
            id = "tsk_1",
            input = TaskOps.NewTask(
                title = "  Ship it  ",
                listId = "lst_1",
                dueDate = LocalDate.of(2026, 7, 1),
                dueTime = LocalTime.of(9, 30),
                priority = Priority.HIGH,
            ),
            now = "2026-06-30T10:00:00Z",
            order = -1.0,
        )
        assertEquals("Ship it", json["title"].toString().trim('"'))
        assertEquals("2026-07-01", json["dueDate"].toString().trim('"'))
        assertEquals("09:30", json["dueTime"].toString().trim('"'))
        assertEquals("high", json["priority"].toString().trim('"'))
        assertTrue(json.containsKey("subtasks"))
        assertTrue(json.containsKey("recurrence"))
    }

    @Test
    fun foreignInboxTombstoneDoesNotDeleteLocalTasks() {
        val data = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1", updatedAt = "2026-11-01T10:00:00Z"))),
        )
        val merged = TaskOps.applyRemote(
            data = data,
            remoteLists = emptyList(),
            remoteTasks = emptyList(),
            deleted = Removed(
                lists = mapOf("inbox" to "2026-11-01T09:00:00Z"),
                tasks = mapOf("tsk_someone_else" to "2026-11-01T09:00:00Z"),
            ),
        )
        assertTrue(merged.tasks.any { it.id == "t1" })
        assertFalse(merged.removed.lists.containsKey("inbox"))
        assertFalse(merged.removed.tasks.containsKey("tsk_someone_else"))
    }

    @Test
    fun tombstoneIsNotClearedByALiveCopy() {
        val data = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1", updatedAt = "2026-11-01T10:00:00Z"))),
            removed = Removed(tasks = mapOf("t1" to "2026-11-01T09:00:00Z")),
        )
        val merged = TaskOps.applyRemote(
            data = data,
            remoteLists = emptyList(),
            remoteTasks = listOf(TaskItem(task("t1", updatedAt = "2026-11-01T10:00:00Z"))),
            deleted = Removed(),
        )
        assertTrue(merged.tasks.isEmpty())
        assertTrue(merged.removed.tasks.containsKey("t1"))
    }

    @Test
    fun newerLocalTaskCannotResurrectDeletedTask() {
        val data = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1", updatedAt = "2026-11-01T10:00:00Z"))),
        )
        val merged = TaskOps.applyRemote(
            data = data,
            remoteLists = emptyList(),
            remoteTasks = emptyList(),
            deleted = Removed(tasks = mapOf("t1" to "2026-11-01T09:00:00Z")),
        )
        assertTrue(merged.tasks.isEmpty())
        assertTrue(merged.removed.tasks.containsKey("t1"))
    }

    @Test
    fun deletedListCannotBeResurrectedByANewerCopy() {
        val data = cn.neeto.guodelist.core.model.LocalData(
            lists = listOf(list("lst_1")),
            tasks = listOf(TaskItem(task("t1", listId = "lst_1"))),
            removed = Removed(lists = mapOf("lst_1" to "2026-11-01T09:00:00Z")),
        )
        val merged = TaskOps.applyRemote(
            data = data,
            remoteLists = listOf(list("lst_1", "2026-11-01T10:00:00Z")),
            remoteTasks = listOf(TaskItem(task("t1", listId = "lst_1", updatedAt = "2026-11-01T10:00:00Z"))),
            deleted = Removed(),
        )
        assertTrue(merged.lists.isEmpty())
        assertTrue(merged.tasks.isEmpty())
        assertTrue(merged.removed.lists.containsKey("lst_1"))
    }

    @Test
    fun deletedListFromServerCascadesRemoteTasks() {
        val data = cn.neeto.guodelist.core.model.LocalData(lists = listOf(list("lst_1")))
        val merged = TaskOps.applyRemote(
            data = data,
            remoteLists = listOf(list("lst_1")),
            remoteTasks = listOf(TaskItem(task("t1", listId = "lst_1"))),
            deleted = Removed(lists = mapOf("lst_1" to "2026-11-01T09:00:00Z")),
        )
        assertTrue(merged.lists.isEmpty())
        assertTrue(merged.tasks.isEmpty())
    }

    @Test
    fun newerTombstoneRemovesTask() {
        val data = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(TaskItem(task("t1", updatedAt = "2026-11-01T09:00:00Z"))),
        )
        val merged = TaskOps.applyRemote(
            data = data,
            remoteLists = emptyList(),
            remoteTasks = emptyList(),
            deleted = Removed(tasks = mapOf("t1" to "2026-11-01T10:00:00Z")),
        )
        assertTrue(merged.tasks.isEmpty())
    }

    @Test
    fun listCrudTracksTombstoneAndCascade() {
        val base = cn.neeto.guodelist.core.model.LocalData()
        val added = TaskOps.addList(base, "lst_1", "  Work  ", "💼", "2026-11-01T10:00:00Z")
        assertEquals("Work", added.lists.single().name)
        assertEquals("💼", added.lists.single().emoji)

        val renamed = TaskOps.renameList(added, "lst_1", "Job", "2026-11-01T11:00:00Z")
        assertEquals("Job", renamed.lists.single().name)

        val withTask = TaskOps.addTask(
            renamed,
            TaskOps.NewTask(title = "Ship", listId = "lst_1"),
            "t1",
            "2026-11-01T12:00:00Z",
        )
        val deleted = TaskOps.deleteList(withTask, "lst_1", "2026-11-01T13:00:00Z")
        assertTrue(deleted.lists.isEmpty())
        assertTrue(deleted.tasks.isEmpty())
        assertEquals("2026-11-01T13:00:00Z", deleted.removed.lists["lst_1"])
    }

    @Test
    fun updateTaskKeepsUnknownFieldsAndCanRestoreDeletedTask() {
        val base = cn.neeto.guodelist.core.model.LocalData(tasks = listOf(TaskItem(task("t1"))))
        val updated = TaskOps.updateTask(
            base,
            "t1",
            mapOf("title" to kotlinx.serialization.json.JsonPrimitive("Renamed")),
            "2026-11-01T10:00:00Z",
        )
        assertEquals("Renamed", updated.tasks.single().title)
        assertEquals("2026-11-01T10:00:00Z", updated.tasks.single().updatedAt)

        val deleted = TaskOps.deleteTask(updated, "t1", "2026-11-01T11:00:00Z")
        val restored = TaskOps.restoreTask(deleted, updated.tasks.single().json, "2026-11-01T12:00:00Z")
        assertEquals("t1", restored.tasks.single().id)
        assertFalse(restored.removed.tasks.containsKey("t1"))
    }

    @Test
    fun subtasksRoundTripAndCount() {
        val base = cn.neeto.guodelist.core.model.LocalData(tasks = listOf(TaskItem(task("t1"))))
        val added = TaskOps.addSubtask(base, "t1", "step one", "s1", "2026-11-01T10:00:00Z")
        val second = TaskOps.addSubtask(added, "t1", "step two", "s2", "2026-11-01T10:01:00Z")
        assertEquals(2, second.tasks.single().subtasks.size)
        assertEquals(0 to 2, TaskOps.subtaskCount(second.tasks.single()))

        val toggled = TaskOps.toggleSubtask(second, "t1", "s1", "2026-11-01T10:02:00Z")
        assertEquals(1 to 2, TaskOps.subtaskCount(toggled.tasks.single()))

        val removed = TaskOps.removeSubtask(toggled, "t1", "s1", "2026-11-01T10:03:00Z")
        assertEquals(0 to 1, TaskOps.subtaskCount(removed.tasks.single()))
    }

    @Test
    fun reorderOnlyMovesTasksWithinTheSameDay() {
        val base = cn.neeto.guodelist.core.model.LocalData(
            tasks = listOf(
                TaskItem(task("a", dueDate = "2026-11-01").with("order" to kotlinx.serialization.json.JsonPrimitive(0))),
                TaskItem(task("b", dueDate = "2026-11-01").with("order" to kotlinx.serialization.json.JsonPrimitive(1))),
                TaskItem(task("c", dueDate = "2026-11-02").with("order" to kotlinx.serialization.json.JsonPrimitive(0))),
            ),
        )
        val reordered = TaskOps.reorder(base, "b", "a")
        assertTrue(reordered.tasks.first { it.id == "b" }.order < reordered.tasks.first { it.id == "a" }.order)
        assertEquals(0.0, TaskOps.reorder(base, "a", "c").tasks.first { it.id == "a" }.order, 0.0001)
    }

    @Test
    fun clearAndReplaceLocalData() {
        val base = cn.neeto.guodelist.core.model.LocalData(
            lists = listOf(list("lst_1")),
            tasks = listOf(TaskItem(task("t1", listId = "lst_1"))),
        )
        val cleared = TaskOps.clearLocal(base)
        assertTrue(cleared.tasks.isEmpty())
        assertTrue(cleared.lists.isEmpty())

        val replaced = TaskOps.replaceAll(
            cleared,
            listOf(list("lst_2")),
            listOf(TaskItem(task("t2", listId = "lst_2"))),
            Removed(),
        )
        assertEquals("lst_2", replaced.lists.single().id)
        assertEquals("t2", replaced.tasks.single().id)
    }

    @Test
    fun applyRemoteMergesListsAndTasksByTimestamp() {
        val base = cn.neeto.guodelist.core.model.LocalData(
            lists = listOf(list("lst_1", "2026-11-01T00:00:00Z")),
            tasks = listOf(TaskItem(task("t1", listId = "lst_1"))),
        )
        val merged = TaskOps.applyRemote(
            base,
            remoteLists = listOf(list("lst_1", "2026-11-02T00:00:00Z")),
            remoteTasks = listOf(TaskItem(task("t1", listId = "lst_1", updatedAt = "2026-11-02T00:00:00Z", priority = "high"))),
            deleted = Removed(),
        )
        assertEquals("2026-11-02T00:00:00Z", merged.lists.single().updatedAt)
        assertEquals("high", merged.tasks.single().priority.raw)
    }

    @Test
    fun rawAccessorsExposeJsonFields() {
        val item = TaskItem(task("t1", priority = "urgent", done = true))
        assertEquals("urgent", TaskOps.rawString(item, "priority"))
        assertTrue(TaskOps.rawBool(item, "done"))
    }
}
