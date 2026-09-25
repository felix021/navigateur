package cn.felix021.navigateur.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.felix021.navigateur.data.Bookmark
import cn.felix021.navigateur.ui.BrowserController
import cn.felix021.navigateur.ui.Screen
import cn.felix021.navigateur.ui.component.BookmarkDialog

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(controller: BrowserController) {
    val bookmarks by controller.container.bookmarks.bookmarks.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Bookmark?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书签") },
                navigationIcon = {
                    IconButton(onClick = { controller.screen.value = Screen.Browser }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editTarget = null
                        dialogOpen = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加")
                    }
                },
            )
        },
    ) { pad ->
        if (bookmarks.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(pad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text("暂无书签", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad)) {
                items(bookmarks, key = { it.id }) { b ->
                    ListItem(
                        headlineContent = {
                            Text(b.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(b.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    b.title.trim().take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        trailingContent = {
                            androidx.compose.foundation.layout.Row {
                                IconButton(onClick = {
                                    editTarget = b
                                    dialogOpen = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "编辑")
                                }
                                IconButton(onClick = { controller.container.bookmarks.remove(b.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                                }
                            }
                        },
                        modifier = Modifier.clickable { controller.openInCurrent(b.url) },
                    )
                }
            }
        }
    }

    if (dialogOpen) {
        BookmarkDialog(
            initial = editTarget,
            onDismiss = { dialogOpen = false },
            onOk = { title, url ->
                val target = editTarget
                if (target == null) {
                    controller.container.bookmarks.add(title, url)
                } else {
                    controller.container.bookmarks.update(target.id, title, url)
                }
                dialogOpen = false
            },
        )
    }
}
