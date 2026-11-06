package cn.neeto.guodelist.core.net

import cn.neeto.guodelist.core.data.GuoStore
import cn.neeto.guodelist.core.model.Removed
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.settings.Account
import cn.neeto.guodelist.core.settings.AuthStore
import cn.neeto.guodelist.core.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

/**
 * Push-after-edit, pull-on-timer sync against the same `/api/v1/sync` endpoint
 * the desktop client uses. The server merges last-write-wins and returns the
 * merged document, including tombstones.
 */
class SyncManager(
    private val api: ApiClient,
    private val store: GuoStore,
    private val auth: AuthStore,
    private val settings: SettingsStore,
) {

    enum class Status { IDLE, SYNCING, OK, ERROR }

    data class SyncUi(
        val signedIn: Boolean = false,
        val account: Account = Account(),
        val status: Status = Status.IDLE,
        val error: String? = null,
        val errorCode: Int? = null,
        val lastSync: String? = null,
    )

    private val _ui = MutableStateFlow(
        SyncUi(
            signedIn = auth.token.isNotBlank(),
            account = if (auth.token.isNotBlank()) auth.account else Account(),
            lastSync = auth.lastSync,
        ),
    )
    val ui: StateFlow<SyncUi> = _ui.asStateFlow()

    private val mutex = Mutex()

    val signedIn: Boolean get() = auth.token.isNotBlank()

    suspend fun bootstrap() {
        if (!signedIn) return
        _ui.update { it.copy(signedIn = true, account = auth.account) }
        try {
            val me = api.me(settings.serverUrl, auth.token)
            val account = Account(
                id = me.id,
                email = me.email,
                name = me.name,
                role = me.role,
            )
            auth.save(auth.token, account)
            _ui.update { it.copy(account = account) }
            syncNow()
        } catch (error: Exception) {
            if (error is ApiException && error.status == 401) {
                auth.clear()
                _ui.value = SyncUi()
            }
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(settings.serverUrl, email.trim(), password)
        val account = Account(
            id = response.user.id,
            email = response.user.email,
            name = response.user.name,
            role = response.user.role,
        )
        auth.save(response.token, account)
        _ui.update { it.copy(signedIn = true, account = account, error = null) }
        syncNow()
    }

    suspend fun register(email: String, password: String, name: String): Result<Unit> = runCatching {
        val response = api.register(settings.serverUrl, email.trim(), password, name.trim())
        val account = Account(
            id = response.user.id,
            email = response.user.email,
            name = response.user.name,
            role = response.user.role,
        )
        auth.save(response.token, account)
        _ui.update { it.copy(signedIn = true, account = account, error = null) }
        syncNow()
    }

    suspend fun syncNow(): Boolean {
        if (!signedIn) return false
        if (!mutex.tryLock()) return false
        try {
            _ui.update { it.copy(status = Status.SYNCING, error = null, errorCode = null) }
            val local = store.data.value
            val response = api.sync(
                base = settings.serverUrl,
                token = auth.token,
                payload = SyncPayload(
                    lists = local.lists.map { it.json },
                    tasks = local.tasks.map { it.json },
                    deleted = local.removed,
                ),
            )
            store.applyRemote(
                lists = response.lists.map(::TaskList),
                tasks = response.tasks.map(::TaskItem),
                deleted = response.deleted,
            )
            auth.lastSync = response.serverTime
            _ui.update {
                it.copy(
                    status = Status.OK,
                    error = null,
                    errorCode = null,
                    signedIn = true,
                    lastSync = response.serverTime,
                )
            }
            return true
        } catch (error: Exception) {
            val code = (error as? ApiException)?.status
            _ui.update {
                it.copy(
                    status = Status.ERROR,
                    error = error.message ?: error::class.java.simpleName,
                    errorCode = code,
                )
            }
            return false
        } finally {
            mutex.unlock()
        }
    }

    fun logout() {
        auth.clear()
        _ui.value = SyncUi()
    }

    fun payloadSize(): Int = store.data.value.lists.size + store.data.value.tasks.size

    fun tombstoneCount(): Int {
        val removed: Removed = store.data.value.removed
        return removed.lists.size + removed.tasks.size
    }
}
