package cn.neeto.guodelist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.neeto.guodelist.AppContainer
import cn.neeto.guodelist.core.data.TaskOps
import cn.neeto.guodelist.core.model.RepeatFreq
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.net.SyncManager
import cn.neeto.guodelist.core.notifications.ReminderScheduler
import cn.neeto.guodelist.core.settings.AppSettings
import cn.neeto.guodelist.core.update.UpdateInfo
import cn.neeto.guodelist.core.update.UpdateManager
import cn.neeto.guodelist.core.util.Recurrence
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

@OptIn(FlowPreview::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val tasks: List<TaskItem> = emptyList(),
        val lists: List<TaskList> = emptyList(),
        val settings: AppSettings = AppSettings(),
        val sync: SyncManager.SyncUi = SyncManager.SyncUi(),
    )

    data class UpdateState(
        val checking: Boolean = false,
        val checked: Boolean = false,
        val info: UpdateInfo? = null,
        val downloading: Boolean = false,
        val progress: Float = 0f,
        val apk: File? = null,
        val error: String? = null,
    )

    private val _update = MutableStateFlow(UpdateState())
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    val ui: StateFlow<UiState> = combine(
        container.store.data,
        container.settings.state,
        container.sync.ui,
    ) { data, settings, sync ->
        UiState(tasks = data.tasks, lists = data.lists, settings = settings, sync = sync)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(
            tasks = container.store.data.value.tasks,
            lists = container.store.data.value.lists,
            settings = container.settings.state.value,
            sync = container.sync.ui.value,
        ),
    )

    init {
        viewModelScope.launch { container.sync.bootstrap() }
        viewModelScope.launch {
            combine(container.store.data, container.settings.state) { data, settings -> data to settings }
                .debounce(800)
                .collect { (data, settings) ->
                    ReminderScheduler.reconcile(container.context, data.tasks, settings)
                }
        }
        viewModelScope.launch {
            container.store.localChanges.debounce(1_800).collect {
                if (container.sync.signedIn) container.sync.syncNow()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                if (container.sync.signedIn) container.sync.syncNow()
            }
        }
    }

    fun addTask(input: TaskOps.NewTask) {
        container.store.addTask(input)
    }

    fun toggleTask(id: String) = container.store.toggleTask(id)

    fun updateTitle(id: String, title: String) = container.store.setTitle(id, title)

    fun updateNotes(id: String, notes: String) = container.store.setNotes(id, notes)

    fun setPriority(id: String, raw: String) = container.store.setPriority(id, raw)

    fun setList(id: String, listId: String) = container.store.setList(id, listId)

    fun setDueDate(id: String, date: LocalDate?) = container.store.setDueDate(id, date)

    fun setDueTime(id: String, time: LocalTime?) = container.store.setDueTime(id, time)

    fun setRepeat(id: String, freq: RepeatFreq?) {
        val recurrence: JsonObject? = freq?.let { Recurrence.json(it) }
        container.store.setRecurrence(id, recurrence)
    }

    fun addSubtask(taskId: String, title: String) = container.store.addSubtask(taskId, title)

    fun toggleSubtask(taskId: String, subtaskId: String) = container.store.toggleSubtask(taskId, subtaskId)

    fun removeSubtask(taskId: String, subtaskId: String) = container.store.removeSubtask(taskId, subtaskId)

    fun deleteTask(id: String) = container.store.deleteTask(id)

    fun restoreTask(json: JsonObject) = container.store.restoreTask(json)

    fun addList(name: String, emoji: String) = container.store.addList(name, emoji)

    fun renameList(id: String, name: String) = container.store.renameList(id, name)

    fun deleteList(id: String) = container.store.deleteList(id)

    fun syncNow() {
        viewModelScope.launch { container.sync.syncNow() }
    }

    fun bootstrapSync() {
        viewModelScope.launch { container.sync.bootstrap() }
    }

    fun login(email: String, password: String, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            val result = container.sync.login(email, password)
            onResult(result.exceptionOrNull())
        }
    }

    fun register(email: String, password: String, name: String, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            val result = container.sync.register(email, password, name)
            onResult(result.exceptionOrNull())
        }
    }

    fun logout() = container.sync.logout()

    fun setTheme(theme: String) = container.settings.setTheme(theme)

    fun setPack(pack: String) = container.settings.setPack(pack)

    fun setLanguage(language: String) = container.settings.setLanguage(language)

    fun setServerUrl(url: String) = container.settings.setServerUrl(url)

    fun setRemindersEnabled(enabled: Boolean) = container.settings.setRemindersEnabled(enabled)

    fun setReminderLeadMinutes(minutes: Int) = container.settings.setReminderLeadMinutes(minutes)

    fun setWidgetListIds(ids: Set<String>) {
        container.settings.setWidgetListIds(ids)
        cn.neeto.guodelist.widget.TodayWidgetProvider.refresh(container.context)
    }

    fun setUpdateToken(token: String) = container.settings.setUpdateToken(token)

    fun setUpdateChannel(channel: String) = container.settings.setUpdateChannel(channel)

    fun setUpdateUrl(url: String) = container.settings.setUpdateUrl(url)

    fun setIconStyle(style: String) {
        val normalized = cn.neeto.guodelist.core.icon.LauncherIcon.normalize(style)
        container.settings.setIconStyle(normalized)
        cn.neeto.guodelist.core.icon.LauncherIcon.apply(container.context, normalized)
    }

    fun checkForUpdates() {
        if (_update.value.checking || _update.value.downloading) return
        viewModelScope.launch {
            _update.value = _update.value.copy(
                checking = true,
                checked = false,
                info = null,
                apk = null,
                error = null,
            )
            val result = runCatching {
                val settings = container.settings.state.value
                UpdateManager.check(
                    channel = settings.updateChannel,
                    customUrl = settings.updateUrl,
                    serverUrl = settings.serverUrl,
                )
            }
            _update.value = result.fold(
                onSuccess = { info ->
                    UpdateState(checking = false, checked = true, info = info)
                },
                onFailure = { error ->
                    UpdateState(checking = false, checked = true, error = error.message ?: "Update check failed")
                },
            )
        }
    }

    fun downloadUpdate() {
        val current = _update.value
        val info = current.info ?: return
        if (current.downloading) return
        viewModelScope.launch {
            _update.value = current.copy(downloading = true, progress = 0f, error = null, apk = null)
            val result = runCatching {
                UpdateManager.download(
                    info = info,
                    destinationDir = File(container.context.cacheDir, "updates"),
                ) { progress ->
                    _update.update { it.copy(progress = progress) }
                }
            }
            _update.value = result.fold(
                onSuccess = { file ->
                    _update.value.copy(downloading = false, progress = 1f, apk = file)
                },
                onFailure = { error ->
                    _update.value.copy(
                        downloading = false,
                        error = error.message ?: "Update download failed",
                    )
                },
            )
        }
    }

    fun loadDemo() = container.store.loadDemo()

    fun clearLocal() = container.store.clearLocal()

    fun flush() = container.store.flushAsync()

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
    }
}
