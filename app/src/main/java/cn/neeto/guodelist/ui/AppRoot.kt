package cn.neeto.guodelist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.ui.components.GuoBottomBar
import cn.neeto.guodelist.ui.components.GuoTopBar
import cn.neeto.guodelist.ui.components.ListsDrawer
import cn.neeto.guodelist.ui.components.Tab
import cn.neeto.guodelist.ui.components.TaskFeed
import cn.neeto.guodelist.ui.components.TodayFeed
import cn.neeto.guodelist.ui.motion.GuoMotion
import cn.neeto.guodelist.ui.motion.rememberClickFeedback
import cn.neeto.guodelist.ui.screens.CalendarScreen
import cn.neeto.guodelist.ui.screens.QuickAddSheet
import cn.neeto.guodelist.ui.screens.SearchScreen
import cn.neeto.guodelist.ui.screens.SettingsScreen
import cn.neeto.guodelist.ui.screens.TaskDetailSheet
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate

private sealed interface TaskPage {
    data object Today : TaskPage
    data object All : TaskPage
    data object Inbox : TaskPage
    data class List(val id: String) : TaskPage
}

private val TaskPage.key: String
    get() = when (this) {
        TaskPage.Today -> "@today"
        TaskPage.All -> "@all"
        TaskPage.Inbox -> "@inbox"
        is TaskPage.List -> id
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    vm: AppViewModel,
    ui: AppViewModel.UiState,
    initialTaskId: String? = null,
    initialTab: String? = null,
    initialQuickAdd: Boolean = false,
) {
    val colors = LocalGuoColors.current
    val clickFeedback = rememberClickFeedback()
    val update by vm.update.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now() }
    val listsById = remember(ui.lists) { ui.lists.associateBy { it.id } }

    val startTab = when (initialTab) {
        "calendar" -> Tab.CALENDAR
        "settings" -> Tab.SETTINGS
        else -> Tab.TASKS
    }
    var tabName by rememberSaveable { mutableStateOf(startTab.name) }
    val tab = Tab.valueOf(tabName)
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showQuickAdd by rememberSaveable { mutableStateOf(initialQuickAdd) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var consumedInitialTaskId by rememberSaveable { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var undoJson by remember { mutableStateOf<JsonObject?>(null) }
    var undoToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(undoToken) {
        if (undoJson != null) {
            delay(1_800)
            undoJson = null
        }
    }

    val taskPages = remember(ui.lists) {
        buildList {
            add(TaskPage.Today)
            add(TaskPage.All)
            add(TaskPage.Inbox)
            ui.lists.forEach { add(TaskPage.List(it.id)) }
        }
    }
    var taskKey by rememberSaveable { mutableStateOf("@today") }
    val taskPage = taskPages.firstOrNull { it.key == taskKey } ?: TaskPage.Today
    val tabPagerState = rememberPagerState(initialPage = startTab.ordinal) { Tab.values().size }
    LaunchedEffect(tabPagerState.currentPage) {
        val next = Tab.values().getOrNull(tabPagerState.currentPage) ?: return@LaunchedEffect
        if (tabName != next.name) tabName = next.name
    }
    LaunchedEffect(initialQuickAdd) {
        if (initialQuickAdd) showQuickAdd = true
    }
    LaunchedEffect(initialTab) {
        val target = when (initialTab) {
            "calendar" -> Tab.CALENDAR
            "settings" -> Tab.SETTINGS
            else -> null
        }
        if (target != null) {
            tabName = target.name
            tabPagerState.scrollToPage(target.ordinal)
        }
    }
    val selectedKey = when (taskPage) {
        TaskPage.Today -> "@today"
        TaskPage.Inbox -> "@inbox"
        is TaskPage.List -> taskPage.id
        else -> null
    }
    val currentList = (taskPage as? TaskPage.List)?.let { listsById[it.id] }

    fun showTaskPage(target: TaskPage) {
        taskKey = target.key
        tabName = Tab.TASKS.name
        scope.launch { tabPagerState.scrollToPage(Tab.TASKS.ordinal) }
    }

    val detailTask = ui.tasks.firstOrNull { it.id == detailId }
    LaunchedEffect(detailId, ui.tasks) {
        if (detailId != null && detailTask == null) detailId = null
    }
    LaunchedEffect(initialTaskId, ui.tasks) {
        if (initialTaskId != null &&
            initialTaskId != consumedInitialTaskId &&
            ui.tasks.any { it.id == initialTaskId }
        ) {
            detailId = initialTaskId
            consumedInitialTaskId = initialTaskId
        }
    }

    val title = when (tab) {
        Tab.TASKS -> when (taskPage) {
            TaskPage.Today -> if (locale.language == "zh") {
                stringResource(R.string.header_today) + " · " + Dates.monthDay(today, locale) + " " +
                    Dates.weekdayShort(today, locale)
            } else {
                stringResource(R.string.header_today) + " · " + Dates.weekdayShort(today, locale) + ", " +
                    Dates.monthDay(today, locale)
            }
            TaskPage.All -> stringResource(R.string.header_all)
            TaskPage.Inbox -> stringResource(R.string.header_inbox)
            is TaskPage.List -> listsById[taskPage.id]?.name ?: stringResource(R.string.header_all)
        }
        Tab.CALENDAR -> stringResource(R.string.header_calendar)
        Tab.SETTINGS -> stringResource(R.string.header_settings)
    }

    val deleteWithUndo: (TaskItem) -> Unit = { task ->
        undoJson = task.json
        vm.deleteTask(task.id)
        undoToken++
    }

    BackHandler(
        enabled = showSearch || showQuickAdd || detailTask != null || drawerState.isOpen,
    ) {
        when {
            showSearch -> showSearch = false
            detailTask != null -> detailId = null
            showQuickAdd -> showQuickAdd = false
            drawerState.isOpen -> scope.launch { drawerState.close() }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = tab == Tab.TASKS && !showSearch,
        drawerContent = {
            ListsDrawer(
                lists = ui.lists,
                tasks = ui.tasks,
                selectedKey = selectedKey,
                onClose = { scope.launch { drawerState.close() } },
                onToday = { showTaskPage(TaskPage.Today) },
                onAll = { showTaskPage(TaskPage.All) },
                onInbox = { showTaskPage(TaskPage.Inbox) },
                onOpenList = { id -> showTaskPage(TaskPage.List(id)) },
                onAddList = { name, emoji -> vm.addList(name, emoji) },
                onRenameList = vm::renameList,
                onDeleteList = { id ->
                    vm.deleteList(id)
                    if (currentList?.id == id) showTaskPage(TaskPage.All)
                },
            )
        },
    ) {
        Scaffold(
            containerColor = colors.canvas,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GuoTopBar(
                    title = title,
                    onMenu = if (tab == Tab.TASKS) {
                        { scope.launch { drawerState.open() } }
                    } else {
                        null
                    },
                    onSearch = if (tab == Tab.SETTINGS) {
                        null
                    } else {
                        { showSearch = true }
                    },
                    leadingIcon = when (tab) {
                        Tab.CALENDAR -> Icons.Outlined.CalendarMonth
                        Tab.SETTINGS -> Icons.Outlined.Settings
                        else -> null
                    },
                )
            },
            bottomBar = {
                GuoBottomBar(
                    selected = tab,
                    onSelect = { next ->
                        tabName = next.name
                        scope.launch { tabPagerState.scrollToPage(next.ordinal) }
                    },
                )
            },
            floatingActionButton = {
                if (tab != Tab.SETTINGS) {
                    FloatingActionButton(
                        onClick = { clickFeedback(); showQuickAdd = true },
                        containerColor = colors.brand,
                        contentColor = colors.onBrand,
                        shape = RoundedCornerShape(18.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_task))
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                HorizontalPager(
                    state = tabPagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 0,
                ) { index ->
                    when (Tab.values()[index]) {
                        Tab.TASKS -> Column(modifier = Modifier.fillMaxSize()) {
                            TaskListStrip(
                                pages = taskPages,
                                selectedKey = taskKey,
                                lists = listsById,
                                onSelect = { taskKey = it.key },
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                PullToRefreshBox(
                                    isRefreshing = ui.sync.status == cn.neeto.guodelist.core.net.SyncManager.Status.SYNCING,
                                    onRefresh = { vm.syncNow() },
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    TaskPageContent(
                                        page = taskPage,
                                        tasks = ui.tasks,
                                        lists = listsById,
                                        today = today,
                                        locale = locale,
                                        onOpen = { detailId = it.id },
                                        onToggle = { vm.toggleTask(it.id) },
                                        onDelete = deleteWithUndo,
                                    )
                                }
                            }
                        }

                        Tab.CALENDAR -> PullToRefreshBox(
                            isRefreshing = ui.sync.status == cn.neeto.guodelist.core.net.SyncManager.Status.SYNCING,
                            onRefresh = { vm.syncNow() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            CalendarScreen(
                                tasks = ui.tasks,
                                lists = listsById,
                                today = today,
                                locale = locale,
                                onOpen = { detailId = it.id },
                                onToggle = { vm.toggleTask(it.id) },
                                onDelete = deleteWithUndo,
                                onCreateAt = { date ->
                                    vm.addTask(
                                        cn.neeto.guodelist.core.data.TaskOps.NewTask(
                                            title = context.getString(R.string.fab_new),
                                            listId = currentList?.id ?: "inbox",
                                            dueDate = date,
                                        ),
                                    )
                                },
                            )
                        }

                        Tab.SETTINGS -> SettingsScreen(
                            ui = ui,
                            onTheme = vm::setTheme,
                            onPack = vm::setPack,
                            onIconStyle = vm::setIconStyle,
                            onLanguage = vm::setLanguage,
                            onServer = vm::setServerUrl,
                            onSync = vm::syncNow,
                            onLogin = vm::login,
                            onRegister = vm::register,
                            onLogout = vm::logout,
                            onLoadDemo = vm::loadDemo,
                            onClearLocal = vm::clearLocal,
                            onRemindersEnabled = vm::setRemindersEnabled,
                            onReminderLead = vm::setReminderLeadMinutes,
                            onWidgetLists = vm::setWidgetListIds,
                            update = update,
                            onCheckUpdate = vm::checkForUpdates,
                            onDownloadUpdate = vm::downloadUpdate,
                        )
                    }
                }
            }
        }
    }
    if (showQuickAdd) {
        QuickAddSheet(
            lists = ui.lists,
            defaultListId = currentList?.id ?: "inbox",
            onDismiss = { showQuickAdd = false },
            onAdd = { input ->
                vm.addTask(input)
                val future = input.dueDate?.isAfter(today) == true
                if (future && taskPage == TaskPage.Today) {
                    taskKey = when {
                        input.listId == "inbox" -> TaskPage.Inbox.key
                        listsById.containsKey(input.listId) -> input.listId
                        else -> TaskPage.All.key
                    }
                }
                showQuickAdd = false
            },
        )
    }

    detailTask?.let { task ->
        TaskDetailSheet(
            task = task,
            lists = ui.lists,
            today = today,
            locale = locale,
            onDismiss = { detailId = null },
            onToggle = { vm.toggleTask(task.id) },
            onTitle = { vm.updateTitle(task.id, it) },
            onNotes = { vm.updateNotes(task.id, it) },
            onPriority = { vm.setPriority(task.id, it) },
            onList = { vm.setList(task.id, it) },
            onDate = { vm.setDueDate(task.id, it) },
            onTime = { vm.setDueTime(task.id, it) },
            onRepeat = { vm.setRepeat(task.id, it) },
            onAddSubtask = { vm.addSubtask(task.id, it) },
            onToggleSubtask = { vm.toggleSubtask(task.id, it) },
            onRemoveSubtask = { vm.removeSubtask(task.id, it) },
            onDelete = {
                detailId = null
                deleteWithUndo(task)
            },
        )
    }

    AnimatedVisibility(
        visible = showSearch,
        enter = slideInVertically(animationSpec = tween(260, easing = GuoMotion.emphasized)) { it / 8 } + fadeIn(tween(160)),
        exit = fadeOut(tween(140)),
    ) {
        SearchScreen(
            tasks = ui.tasks,
            lists = listsById,
            today = today,
            locale = locale,
            onClose = { showSearch = false },
            onOpen = { task ->
                detailId = task.id
                showSearch = false
            },
            onToggle = { task -> vm.toggleTask(task.id) },
            onDelete = deleteWithUndo,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 76.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val pendingUndo = undoJson
        AnimatedVisibility(
            visible = pendingUndo != null,
            enter = slideInVertically(tween(180, easing = GuoMotion.emphasized)) { it } + fadeIn(tween(120)),
            exit = slideOutVertically(tween(140)) { it } + fadeOut(tween(100)),
        ) {
            Surface(
                color = colors.ink,
                contentColor = colors.panel,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.panel.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.task_deleted),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.panel,
                    )
                    Spacer(Modifier.width(10.dp))
                    TextButton(
                        onClick = {
                            val raw = undoJson
                            if (raw != null) vm.restoreTask(raw)
                            undoJson = null
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = null,
                            tint = colors.brand,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.common_undo),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.brand,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListStrip(
    pages: List<TaskPage>,
    selectedKey: String,
    lists: Map<String, TaskList>,
    onSelect: (TaskPage) -> Unit,
) {
    val colors = LocalGuoColors.current
    val clickFeedback = rememberClickFeedback()
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    listState.dispatchRawDelta(-dragAmount)
                }
            },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(pages, key = { it.key }) { page ->
            val selected = page.key == selectedKey
            val label = when (page) {
                TaskPage.Today -> stringResource(R.string.header_today)
                TaskPage.All -> stringResource(R.string.header_all)
                TaskPage.Inbox -> stringResource(R.string.header_inbox)
                is TaskPage.List -> lists[page.id]?.name ?: stringResource(R.string.header_all)
            }
            val icon: ImageVector? = when (page) {
                TaskPage.Today -> Icons.Outlined.Today
                TaskPage.All -> Icons.Outlined.Star
                TaskPage.Inbox -> Icons.Outlined.Inbox
                is TaskPage.List -> null
            }
            FilterChip(
                selected = selected,
                onClick = { clickFeedback(); onSelect(page) },
                modifier = Modifier.height(40.dp),
                shape = CircleShape,
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        fontSize = 12.sp,
                    )
                },
                leadingIcon = {
                    if (page is TaskPage.List) {
                        Text(lists[page.id]?.emoji ?: "•", fontSize = 13.sp)
                    } else if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.panel,
                    labelColor = colors.ink3,
                    iconColor = colors.ink3,
                    selectedContainerColor = colors.brandTint,
                    selectedLabelColor = colors.brandInk,
                    selectedLeadingIconColor = colors.brandInk,
                ),
            )        }
    }
}

@Composable
private fun TaskPageContent(
    page: TaskPage,
    tasks: List<TaskItem>,
    lists: Map<String, TaskList>,
    today: LocalDate,
    locale: java.util.Locale,
    onOpen: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
) {
    when (page) {
        TaskPage.Today -> TodayFeed(
            tasks = tasks,
            lists = lists,
            today = today,
            locale = locale,
            onOpen = onOpen,
            onToggle = onToggle,
            onDelete = onDelete,
        )
        TaskPage.All -> TaskFeed(
            tasks = tasks,
            lists = lists,
            today = today,
            locale = locale,
            emptyTitle = stringResource(R.string.empty_list_title),
            emptyBody = stringResource(R.string.empty_list_body),
            showList = true,
            onOpen = onOpen,
            onToggle = onToggle,
            onDelete = onDelete,
        )
        TaskPage.Inbox -> TaskFeed(
            tasks = tasks.filter { it.listId == "inbox" },
            lists = lists,
            today = today,
            locale = locale,
            emptyTitle = stringResource(R.string.empty_list_title),
            emptyBody = stringResource(R.string.empty_list_body),
            showList = false,
            onOpen = onOpen,
            onToggle = onToggle,
            onDelete = onDelete,
        )
        is TaskPage.List -> TaskFeed(
            tasks = tasks.filter { it.listId == page.id },
            lists = lists,
            today = today,
            locale = locale,
            emptyTitle = stringResource(R.string.empty_list_title),
            emptyBody = stringResource(R.string.empty_list_body),
            showList = false,
            onOpen = onOpen,
            onToggle = onToggle,
            onDelete = onDelete,
        )
    }
}
