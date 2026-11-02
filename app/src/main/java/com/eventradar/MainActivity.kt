@file:OptIn(ExperimentalMaterial3Api::class)

package com.eventradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.data.EventEntity
import com.eventradar.ui.CalendarScreen
import com.eventradar.ui.EventDetailSheet
import com.eventradar.ui.EventRadarTheme
import com.eventradar.ui.Flare
import com.eventradar.ui.InterestsScreen
import com.eventradar.ui.MainViewModel
import com.eventradar.ui.Muted
import com.eventradar.ui.RefreshDialog
import com.eventradar.ui.Panel
import com.eventradar.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EventRadarTheme { App(vm) } }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Calendar("Calendar", Icons.Filled.DateRange),
    Interests("Interests", Icons.Filled.Favorite),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
private fun App(vm: MainViewModel) {
    val events by vm.events.collectAsStateWithLifecycle()
    val interests by vm.interests.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    var askRefresh by remember { mutableStateOf(false) }

    var tab by rememberSaveable { mutableStateOf(Tab.Calendar) }
    var selected by remember { mutableStateOf<EventEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == Tab.Calendar) "EventRadar" else tab.label) },
                actions = {
                    if (refreshing) {
                        Box(Modifier.padding(14.dp)) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Flare)
                        }
                    } else {
                        IconButton(onClick = { askRefresh = true }) { Icon(Icons.Filled.Refresh, "Find events") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Flare,
                            selectedTextColor = Flare,
                            indicatorColor = Flare.copy(alpha = 0.15f),
                        ),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
        progress?.let { (done, total) ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    color = Flare,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Searching… $done of $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.Calendar -> CalendarScreen(
                    events = events,
                    interests = interests,
                    hasSources = settings.hasApiKey || interests.any { it.feedUrl != null },
                    onEventClick = { selected = it },
                    onGoToInterests = { tab = Tab.Interests },
                    onGoToSettings = { tab = Tab.Settings },
                )
                Tab.Interests -> InterestsScreen(
                    interests = interests,
                    events = events,
                    onAdd = vm::addInterest,
                    onUpdate = vm::updateInterest,
                    onDelete = { vm.deleteInterest(it) },
                )
                Tab.Settings -> SettingsScreen(
                    settings = settings,
                    onChange = vm::updateSettings,
                    onRefresh = { askRefresh = true },
                    onClear = { vm.clearEvents() },
                )
            }
        }
        }
    }

    if (askRefresh) {
        RefreshDialog(
            interestCount = interests.size,
            defaultMonths = settings.monthsAhead,
            onSearch = { months -> askRefresh = false; vm.refreshAll(months) },
            onDismiss = { askRefresh = false },
        )
    }

    selected?.let { EventDetailSheet(event = it, onDismiss = { selected = null }) }
}
