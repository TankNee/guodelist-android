package cn.neeto.guodelist.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * The desktop app owns the sync schema. The phone therefore keeps every record
 * as a raw [JsonObject] and only reads the fields it renders: anything this
 * build does not know about (attachments, comments, future fields) survives a
 * pull → edit → push round trip untouched.
 */
fun JsonObject.string(key: String, fallback: String = ""): String =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() } ?: fallback

fun JsonObject.stringOrNull(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

fun JsonObject.bool(key: String): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: false

fun JsonObject.double(key: String): Double =
    (this[key] as? JsonPrimitive)?.doubleOrNull ?: 0.0

fun JsonObject.child(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

fun JsonElement?.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

fun jsonString(value: String?): JsonElement = if (value == null) JsonNull else JsonPrimitive(value)

fun JsonObject.with(vararg pairs: Pair<String, JsonElement>): JsonObject =
    JsonObject(LinkedHashMap(this).apply { pairs.forEach { (k, v) -> put(k, v) } })

data class SubtaskItem(val json: JsonObject) {
    val id: String get() = json.string("id")
    val title: String get() = json.string("title")
    val done: Boolean get() = json.bool("done")
}

enum class Priority(val raw: String, val weight: Int) {
    NONE("none", 0),
    LOW("low", 1),
    MEDIUM("medium", 2),
    HIGH("high", 3),
    URGENT("urgent", 4);

    companion object {
        fun of(raw: String?): Priority = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

enum class RepeatFreq(val raw: String) {
    DAILY("daily"),
    WEEKDAYS("weekdays"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    companion object {
        fun of(raw: String?): RepeatFreq? = entries.firstOrNull { it.raw == raw }
    }
}

@JvmInline
value class TaskItem(val json: JsonObject) {
    val id: String get() = json.string("id")
    val title: String get() = json.string("title")
    val notes: String get() = json.string("notes")
    val listId: String get() = json.string("listId", "inbox")
    val done: Boolean get() = json.bool("done")
    val completedAt: String? get() = json.stringOrNull("completedAt")
    val dueDate: String? get() = json.stringOrNull("dueDate")
    val dueTime: String? get() = json.stringOrNull("dueTime")
    val endTime: String? get() = json.stringOrNull("endTime")
    val priority: Priority get() = Priority.of(json.stringOrNull("priority"))
    val labels: List<String>
        get() = json.array("labels").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    val subtasks: List<SubtaskItem>
        get() = json.array("subtasks").mapNotNull { (it as? JsonObject)?.let(::SubtaskItem) }
    val recurrence: JsonObject? get() = json.child("recurrence")
    val repeatFreq: RepeatFreq? get() = RepeatFreq.of(recurrence?.stringOrNull("freq"))
    val createdAt: String get() = json.string("createdAt")
    val updatedAt: String get() = json.string("updatedAt")
    val order: Double get() = json.double("order")
}

@JvmInline
value class TaskList(val json: JsonObject) {
    val id: String get() = json.string("id")
    val name: String get() = json.string("name", "List")
    val emoji: String get() = json.string("emoji", "📋")
    val groupId: String get() = json.string("groupId")
    val updatedAt: String get() = json.string("updatedAt")
    val createdAt: String? get() = json.stringOrNull("createdAt")
}

@Serializable
data class Removed(
    val lists: Map<String, String> = emptyMap(),
    val tasks: Map<String, String> = emptyMap(),
) {
    fun without(ids: Set<String>): Removed = copy(
        tasks = tasks.filterKeys { it !in ids },
        lists = lists.filterKeys { it !in ids },
    )
}

data class LocalData(
    val lists: List<TaskList> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val removed: Removed = Removed(),
    /** Bumped only by local edits, never by a remote merge. */
    val revision: Long = 0,
)

@Serializable
data class StoredData(
    val version: Int = 1,
    val lists: List<JsonObject> = emptyList(),
    val tasks: List<JsonObject> = emptyList(),
    val removed: Removed = Removed(),
)
