package cn.neeto.guodelist.core.net

import cn.neeto.guodelist.core.model.Removed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class AccountDto(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "user",
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val user: AccountDto = AccountDto(),
)

@Serializable
private data class MeResponse(val user: AccountDto = AccountDto())

@Serializable
private data class ErrorResponse(val error: String = "", val message: String = "")

@Serializable
data class TaskSuggestion(
    val dueDate: String? = null,
    val dateLabel: String = "",
    val listId: String? = null,
    val listName: String = "",
    val emoji: String = "",
    val confidence: Double? = null,
    val dateConfidence: Double? = null,
    val listConfidence: Double? = null,
    val model: String = "",
    val source: String = "",
)

@Serializable
private data class SuggestionResponse(val suggestion: TaskSuggestion = TaskSuggestion())

@Serializable
data class SyncPayload(
    val lists: List<JsonObject>,
    val tasks: List<JsonObject>,
    val deleted: Removed,
)

@Serializable
data class SyncResponse(
    val serverTime: String = "",
    val lists: List<JsonObject> = emptyList(),
    val tasks: List<JsonObject> = emptyList(),
    val deleted: Removed = Removed(),
)

class ApiException(val status: Int, override val message: String) : IOException(message)

/** Thin REST client for the Guodelist sync API (server/index.mjs). */
class ApiClient {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun login(base: String, email: String, password: String): AuthResponse =
        call(
            base,
            "/api/v1/auth/login",
            """{"email":${quote(email)},"password":${quote(password)}}""",
            token = null,
        )

    suspend fun register(base: String, email: String, password: String, name: String): AuthResponse =
        call(
            base,
            "/api/v1/auth/register",
            """{"email":${quote(email)},"password":${quote(password)},"name":${quote(name)}}""",
            token = null,
        )

    suspend fun me(base: String, token: String): AccountDto =
        call<MeResponse>(base, "/api/v1/me", null, token).user

    suspend fun sync(base: String, token: String, payload: SyncPayload): SyncResponse =
        call(
            base,
            "/api/v1/sync",
            json.encodeToString(SyncPayload.serializer(), payload),
            token,
        )

    suspend fun suggestTask(base: String, token: String, title: String, today: String): TaskSuggestion =
        call<SuggestionResponse>(
            base,
            "/api/v1/ai/task-suggestion",
            """{"title":${quote(title)},"today":${quote(today)}}""",
            token,
        ).suggestion

    private suspend inline fun <reified T> call(
        base: String,
        path: String,
        body: String?,
        token: String?,
    ): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(base.trimEnd('/') + path)
            .apply {
                if (body != null) {
                    post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                } else {
                    get()
                }
                header("accept", "application/json")
                if (!token.isNullOrBlank()) header("authorization", "Bearer $token")
            }
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    json.decodeFromString(ErrorResponse.serializer(), raw)
                }.getOrNull()?.let { it.message.takeIf(String::isNotBlank) ?: it.error }
                    ?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
                throw ApiException(response.code, message)
            }
            if (raw.isBlank()) {
                throw ApiException(response.code, "Empty response")
            }
            json.decodeFromString<T>(raw)
        }
    }

    private fun quote(value: String): String = JsonPrimitive(value).toString()
}
