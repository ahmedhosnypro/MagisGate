package com.example.rootinjector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.rootinjector.AppViewModel
import com.example.rootinjector.model.AppInfo
import com.example.rootinjector.model.FilterSortState
import com.example.rootinjector.model.LogEntry
import com.example.rootinjector.ui.ElectricPurple
import com.example.rootinjector.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(viewModel: AppViewModel) {
    val isRooted by viewModel.isRooted.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val noRootApps by viewModel.noRootApps.collectAsState()
    val rootApps by viewModel.rootApps.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterSort by viewModel.filterSort.collectAsState()
    val isLoadingRootData by viewModel.isLoadingRootData.collectAsState()

    when {
        isRooted == null || isLoadingApps -> LoadingScreen()
        isRooted == false -> NoRootScreen("Root access is required to use RootInjector.")
        else -> RootedScreen(
            noRootApps = noRootApps,
            rootApps = rootApps,
            logs = logs,
            searchQuery = searchQuery,
            filterSort = filterSort,
            isLoadingRootData = isLoadingRootData,
            onSearchChange = viewModel::setSearchQuery,
            onFilterUpdate = viewModel::updateFilterSort,
            onClearLogs = viewModel::clearLogs,
            onInject = { app -> viewModel.injectPolicy(app.packageName) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("RootInjector") }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricPurple)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoRootScreen(message: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("RootInjector") }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Lock, null, tint = ElectricPurple, modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(24.dp))
                Text("Root Required", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text(message, color = Color.White.copy(alpha = 0.54f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootedScreen(
    noRootApps: List<AppInfo>,
    rootApps: List<AppInfo>,
    logs: List<LogEntry>,
    searchQuery: String,
    filterSort: FilterSortState,
    isLoadingRootData: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterUpdate: (FilterSortState) -> Unit,
    onClearLogs: () -> Unit,
    onInject: suspend (AppInfo) -> Boolean,
) {
    val tabs = listOf("No Root", "Root Apps", "Logs")
    var selectedTab by remember { mutableIntStateOf(0) }
    val isLogsTab = selectedTab == 2
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    @OptIn(ExperimentalMaterial3Api::class)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterSheetVisible by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (filterSheetVisible) {
        ModalBottomSheet(onDismissRequest = { filterSheetVisible = false }, sheetState = sheetState) {
            FilterSheet(state = filterSort, onUpdate = onFilterUpdate)
        }
    }

    selectedApp?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedApp = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ConfirmationSheet(
                app = app,
                onInject = {
                    val result = onInject(app)
                    if (result) {
                        scope.launch { snackbarHostState.showSnackbar("Policy injected for ${app.appName}") }
                    }
                    result
                },
                onDismiss = { selectedApp = null },
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("RootInjector") },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                )
                if (!isLogsTab) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SearchBarRow(
                            query = searchQuery,
                            onQueryChange = onSearchChange,
                            onFilterClick = { filterSheetVisible = true },
                        )
                    }
                }
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = ElectricPurple,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricPurple,
                        )
                    },
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.12f)) },
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = if (i < 2) ({ Text(title) }) else null,
                            icon = if (i == 2) ({ Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp)) }) else null,
                            selectedContentColor = ElectricPurple,
                            unselectedContentColor = Color.White.copy(alpha = 0.54f),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> AppTab(apps = noRootApps, isLoading = isLoadingRootData, onAppClick = { selectedApp = it })
                1 -> AppTab(apps = rootApps, isLoading = isLoadingRootData, onAppClick = { selectedApp = it }, emptyForRootTab = true)
                2 -> LogsTab(logs = logs, onClear = onClearLogs)
            }
        }
    }
}

@Composable
private fun AppTab(
    apps: List<AppInfo>,
    isLoading: Boolean,
    onAppClick: (AppInfo) -> Unit,
    emptyForRootTab: Boolean = false,
) {
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricPurple)
        }
        apps.isEmpty() && emptyForRootTab -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.ManageAccounts, null, tint = Color.White.copy(alpha = 0.24f), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(20.dp))
                Text("No Root-Granted Apps", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    "No apps have been granted root via Magisk yet.\nUse the \"No Root\" tab to inject a policy.",
                    color = Color.White.copy(alpha = 0.38f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(apps, key = { it.packageName }) { app ->
                AppListItem(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}
