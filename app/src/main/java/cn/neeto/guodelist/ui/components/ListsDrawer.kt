package cn.neeto.guodelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.ui.theme.LocalGuoColors

@Composable
fun ListsDrawer(
    lists: List<TaskList>,
    tasks: List<TaskItem>,
    selectedKey: String?,
    onClose: () -> Unit,
    onToday: () -> Unit,
    onAll: () -> Unit,
    onInbox: () -> Unit,
    onOpenList: (String) -> Unit,
    onAddList: (String, String) -> Unit,
    onRenameList: (String, String) -> Unit,
    onDeleteList: (String) -> Unit,
) {
    val colors = LocalGuoColors.current
    var showNewList by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<TaskList?>(null) }
    var deleteTarget by remember { mutableStateOf<TaskList?>(null) }

    ModalDrawerSheet(
        drawerContainerColor = colors.panel,
        drawerContentColor = colors.ink,
        modifier = Modifier.width(306.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(horizontal = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.brand),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = colors.onBrand, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                )
            }

            DrawerItem(
                label = stringResource(R.string.nav_today),
                leading = { Icon(Icons.Outlined.Today, null, tint = colors.ink2, modifier = Modifier.size(20.dp)) },
                selected = selectedKey == "@today",
                onClick = { onToday(); onClose() },
            )
            DrawerItem(
                label = stringResource(R.string.nav_inbox),
                leading = { Icon(Icons.Outlined.Inbox, null, tint = colors.ink2, modifier = Modifier.size(20.dp)) },
                selected = selectedKey == "@inbox",
                onClick = { onInbox(); onClose() },
            )
            DrawerItem(
                label = stringResource(R.string.nav_all),
                leading = { Icon(Icons.Outlined.Star, null, tint = colors.brandInk, modifier = Modifier.size(18.dp)) },
                selected = selectedKey == null,
                onClick = { onAll(); onClose() },
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.header_lists),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showNewList = true }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.list_new_title), tint = colors.ink3, modifier = Modifier.size(18.dp))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(lists, key = { it.id }) { list ->
                    val openCount = tasks.count { it.listId == list.id && !it.done }
                    DrawerItem(
                        label = list.name,
                        leading = { Text(list.emoji, fontSize = 17.sp) },
                        trailing = openCount.takeIf { it > 0 }?.toString(),
                        selected = selectedKey == list.id,
                        onClick = { onOpenList(list.id); onClose() },
                        onMore = { renameTarget = list },
                        onDeleteRequest = { deleteTarget = list },
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showNewList = true }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Icon(Icons.Outlined.Add, null, tint = colors.ink3, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.list_new_title), style = MaterialTheme.typography.bodyMedium, color = colors.ink3)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showNewList) {
        ListDialog(
            title = stringResource(R.string.list_new_title),
            initialName = "",
            initialEmoji = "📋",
            onDismiss = { showNewList = false },
            onConfirm = { name, emoji ->
                onAddList(name, emoji)
                showNewList = false
            },
        )
    }

    renameTarget?.let { list ->
        ListDialog(
            title = stringResource(R.string.list_rename),
            initialName = list.name,
            initialEmoji = "",
            showEmoji = false,
            onDismiss = { renameTarget = null },
            onConfirm = { name, _ ->
                onRenameList(list.id, name)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.list_delete)) },
            text = { Text(stringResource(R.string.list_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteList(list.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.common_delete), color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            },
            containerColor = colors.panel,
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    leading: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
    onMore: (() -> Unit)? = null,
    onDeleteRequest: () -> Unit = {},
) {
    val colors = LocalGuoColors.current
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.brandTint else colors.panel)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) { leading() }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.brandInk else colors.ink,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelSmall, color = colors.ink3)
        }
        if (onMore != null) {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.cd_more), tint = colors.ink3, modifier = Modifier.size(17.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.list_rename)) },
                        onClick = { menu = false; onMore() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.list_delete), color = colors.danger) },
                        onClick = { menu = false; onDeleteRequest() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ListDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    showEmoji: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val colors = LocalGuoColors.current
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (showEmoji) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { if (it.length <= 4) emoji = it },
                            label = { Text(stringResource(R.string.list_emoji)) },
                            singleLine = true,
                            modifier = Modifier.width(96.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.list_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.list_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, emoji) },
            ) { Text(stringResource(R.string.common_save), color = colors.brandInk) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
        containerColor = colors.panel,
    )
}
