package com.felix021.puff.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.felix021.puff.browser.TabState
import com.felix021.puff.ui.BrowserController
import com.felix021.puff.ui.Screen
import com.felix021.puff.util.UrlUtils
import androidx.compose.ui.res.stringResource
import com.felix021.puff.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(controller: BrowserController) {
    val tabs by controller.tabManager.tabs.collectAsState()
    val currentId by controller.tabManager.currentId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tabs_count, tabs.size)) },
                navigationIcon = {
                    IconButton(onClick = { controller.screen.value = Screen.Browser }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        controller.tabManager.newTab()
                        controller.screen.value = Screen.Browser
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tab_new))
                    }
                },
            )
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize().padding(pad).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabCard(
                    tab = tab,
                    isCurrent = tab.id == currentId,
                    onClick = {
                        controller.tabManager.switchTo(tab.id)
                        controller.screen.value = Screen.Browser
                    },
                    onClose = { controller.tabManager.closeTab(tab.id) },
                )
            }
        }
    }
}

@Composable
private fun TabCard(tab: TabState, isCurrent: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().height(116.dp).clickable(onClick = onClick),
        colors = if (isCurrent) {
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.elevatedCardColors()
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    tab.displayTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    UrlUtils.hostOf(tab.url).ifEmpty { tab.url },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), modifier = Modifier.size(16.dp))
            }
        }
    }
}
