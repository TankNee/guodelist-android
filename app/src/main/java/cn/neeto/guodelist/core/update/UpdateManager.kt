package cn.neeto.guodelist.core.update

import cn.neeto.guodelist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkName: String,
    val size: Long,
    val sha256: String,
    val publishedAt: String,
    val mandatory: Boolean,
)

object UpdateManager {

    /**
     * The same origin the desktop app and phone use for sync. Releases are
     * uploaded to the admin console and served from /downloads/android/...
     */
    /** Configured at build time via GUODELIST_SERVER; see docs/RELEASING.md. */
    val BASE_URL: String = BuildConfig.DEFAULT_SERVER

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Default source is the GitHub release; use GUODELIST_UPDATE_SOURCE=api to self-host. */
    suspend fun check(baseUrl: String = BASE_URL): UpdateInfo? =
        if (BuildConfig.UPDATE_SOURCE.equals("api", ignoreCase = true)) {
            checkServer(baseUrl)
        } else {
            checkGithub()
        }

    private suspend fun checkGithub(): UpdateInfo? = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"
            .toHttpUrl()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (response.code == 403) throw IOException("GitHub Releases is unavailable right now.")
            if (!response.isSuccessful) throw IOException("GitHub Releases returned HTTP ${response.code}")

            val release = runCatching {
                json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            }.getOrElse { throw IOException("GitHub Releases returned invalid JSON") }

            val version = release["tag_name"]?.jsonPrimitive?.contentOrNull
                .orEmpty()
                .removePrefix("v")
            if (version.isBlank() || !isNewerVersion(version, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }

            val assets = release["assets"]?.jsonArray?.mapNotNull { it.jsonObject }.orEmpty()
            val apk = assets.firstOrNull {
                (it["name"]?.jsonPrimitive?.contentOrNull.orEmpty()).endsWith(".apk", ignoreCase = true)
            } ?: return@withContext null
            val apkName = apk["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val apkUrl = apk["browser_download_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (apkName.isBlank() || apkUrl.isBlank()) return@withContext null

            val directDigest = apk["digest"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val directSha = Regex("([a-fA-F0-9]{64})").find(directDigest)?.groupValues?.get(1).orEmpty()
            val sidecar = assets.firstOrNull {
                val name = it["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                name.endsWith(".sha256", ignoreCase = true) &&
                    name.startsWith(apkName.removeSuffix(".apk"), ignoreCase = true)
            }
            val sha = directSha.ifBlank {
                sidecar?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
                    ?.let { checksumText(it) }
                    .orEmpty()
            }

            UpdateInfo(
                version = version,
                versionCode = 0,
                notes = release["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                pageUrl = release["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                apkUrl = apkUrl,
                apkName = apkName,
                size = apk["size"]?.jsonPrimitive?.longOrNull ?: 0L,
                sha256 = sha,
                publishedAt = release["published_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                mandatory = Regex("\\[mandatory\\]", RegexOption.IGNORE_CASE)
                    .containsMatchIn(release["body"]?.jsonPrimitive?.contentOrNull.orEmpty()),
            )
        }
    }

    private fun checksumText(url: String): String = runCatching {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            Regex("\\b[a-fA-F0-9]{64}\\b").find(response.body?.string().orEmpty())
                ?.value
                .orEmpty()
        }
    }.getOrDefault("")

    private fun isNewerVersion(remote: String, current: String): Boolean {
        fun parts(value: String): List<Int> = value
            .trim()
            .removePrefix("v")
            .split('.', '-', '+')
            .map { it.toIntOrNull() ?: 0 }
        val left = parts(remote)
        val right = parts(current)
        val length = maxOf(left.size, right.size)
        for (index in 0 until length) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private suspend fun checkServer(baseUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/v1/app/latest".toHttpUrl().newBuilder()
            .addQueryParameter("platform", "android")
            .addQueryParameter("versionCode", BuildConfig.VERSION_CODE.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) {
                throw IOException(
                    when (response.code) {
                        429 -> "Too many update checks. Try again later."
                        in 500..599 -> "The update server is unavailable (HTTP ${response.code})."
                        else -> "Update check failed: HTTP ${response.code}"
                    },
                )
            }
            val body = response.body?.string().orEmpty()
            val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrElse {
                throw IOException("The update server returned invalid data")
            }
            val available = obj["available"]?.jsonPrimitive?.booleanOrNull ?: false
            if (!available) return@withContext null

            val version = obj["versionName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val versionCode = obj["versionCode"]?.jsonPrimitive?.intOrNull ?: 0
            val apkUrl = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (version.isBlank() || apkUrl.isBlank()) return@withContext null

            UpdateInfo(
                version = version,
                versionCode = versionCode,
                notes = obj["notes"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                pageUrl = baseUrl,
                apkUrl = apkUrl,
                apkName = obj["fileName"]?.jsonPrimitive?.contentOrNull ?: "guodelist-update.apk",
                size = obj["size"]?.jsonPrimitive?.longOrNull ?: 0L,
                sha256 = obj["sha256"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                publishedAt = obj["publishedAt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                mandatory = obj["mandatory"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }
    }

    suspend fun download(
        info: UpdateInfo,
        destinationDir: File,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (!destinationDir.exists()) destinationDir.mkdirs()
        val target = File(destinationDir, "guodelist-update.apk")
        val temp = File(destinationDir, "guodelist-update.apk.part")
        if (temp.exists()) temp.delete()

        val request = Request.Builder().url(info.apkUrl).build()
        val digest = MessageDigest.getInstance("SHA-256")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("APK download failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("APK download failed: empty response")
            val total = body.contentLength().takeIf { it > 0 } ?: info.size
            var written = 0L
            FileOutputStream(temp).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            if (written <= 0) {
                temp.delete()
                throw IOException("APK download failed: empty file")
            }
            if (info.size > 0 && written != info.size) {
                temp.delete()
                throw IOException("Incomplete download: expected ${info.size} bytes, got ${written}")
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (info.sha256.isNotBlank() && !actual.equals(info.sha256, ignoreCase = true)) {
                temp.delete()
                throw IOException("Package checksum failed. Try again.")
            }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            target
        }
    }
}
