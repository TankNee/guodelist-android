package cn.neeto.guodelist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.data.SearchHistoryStore
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.ui.components.EmptyState
import cn.neeto.guodelist.ui.components.SwipeTaskRow
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import java.time.LocalDate

@Composable
fun SearchScreen(
    tasks: List<TaskItem>,
    lists: Map<String, TaskList>,
    today: LocalDate,
    locale: java.util.Locale,
    onClose: () -> Unit,
    onOpen: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
) {
    val colors = LocalGuoColors.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var query by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(SearchHistoryStore.load(context)) }
    val focus = remember { FocusRequester() }
    val results = remember(tasks, query) {
        val needle = query.trim()
        if (needle.isEmpty()) emptyList()
        else tasks.filter { task ->
            task.title.contains(needle, ignoreCase = true) || task.notes.contains(needle, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    fun rememberQuery(term: String) {
        SearchHistoryStore.record(context, term)
        history = SearchHistoryStore.load(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.nav_close), tint = colors.ink2)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.panel, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = colors.ink3, modifier = Modifier.width(18.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brand),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) rememberQuery(query) }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.ink3.copy(alpha = 0.75f),
                            )
                        }
                        inner()
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.width(28.dp)) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.cd_clear_search), tint = colors.ink3)
                    }
                }
            }
        }

        if (query.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.History, null, tint = colors.ink3, modifier = Modifier.width(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.search_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink3,
                    modifier = Modifier.weight(1f),
                )
                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            SearchHistoryStore.clear(context)
                            history = emptyList()
                        },
                    ) {
                        Text(stringResource(R.string.search_clear_history), color = colors.ink3)
                    }
                }
            }
            if (history.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.search_history_empty_title),
                    body = stringResource(R.string.search_history_empty_body),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
                ) {
                    items(history, key = { it }) { term ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    query = term
                                    rememberQuery(term)
                                }
                                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.History, null, tint = colors.ink3, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = term,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    SearchHistoryStore.remove(context, term)
                                    history = SearchHistoryStore.load(context)
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    stringResource(R.string.search_history_delete),
                                    tint = colors.ink3,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.search_result_count, results.size),
                style = MaterialTheme.typography.labelSmall,
                color = colors.ink3,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 4.dp),
            )

            if (results.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_search_title),
                    body = stringResource(R.string.empty_search_body),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(results, key = { it.id }) { task ->
                        SwipeTaskRow(
                            task = task,
                            list = lists[task.listId],
                            today = today,
                            locale = locale,
                            showList = true,
                            onOpen = {
                                rememberQuery(query)
                                onOpen(task)
                            },
                            onToggle = { onToggle(task) },
                            onDelete = { onDelete(task) },
                        )
                    }
                }
            }
        }
    }
}
