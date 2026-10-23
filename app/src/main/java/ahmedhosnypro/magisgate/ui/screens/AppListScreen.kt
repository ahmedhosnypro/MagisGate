package ahmedhosnypro.magisgate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ahmedhosnypro.magisgate.AppViewModel
import ahmedhosnypro.magisgate.R
import ahmedhosnypro.magisgate.model.AppInfo
import ahmedhosnypro.magisgate.model.FilterSortState
import ahmedhosnypro.magisgate.model.LogEntry
import ahmedhosnypro.magisgate.ui.ElectricPurple
import ahmedhosnypro.magisgate.ui.components.AppListItem
import ahmedhosnypro.magisgate.ui.components.ConfirmationSheet
import ahmedhosnypro.magisgate.ui.components.FilterSheet
import ahmedhosnypro.magisgate.ui.components.LogsTab
import ahmedhosnypro.magisgate.ui.components.SearchBarRow
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
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val scope = rememberCoroutineScope()

    when {
        (isRooted == null) || isLoadingApps -> LoadingScreen()
        isRooted == false -> NoRootScreen()
        else -> RootedScreen(
            noRootApps = noRootApps,
            rootApps = rootApps,
            logs = logs,
            searchQuery = searchQuery,
            filterSort = filterSort,
            isLoadingRootData = isLoadingRootData,
            selectedPackages = selectedPackages,
            onSearchChange = viewModel::setSearchQuery,
            onFilterUpdate = viewModel::updateFilterSort,
            onClearLogs = viewModel::clearLogs,
            onInject = { app -> viewModel.injectPolicy(app.packageName) },
            onRemove = { app -> viewModel.removePolicy(app.packageName) },
            onToggleSelection = viewModel::toggleSelection,
            onClearSelection = viewModel::clearSelection,
            onInjectBulk = { scope.launch { viewModel.injectBulk(selectedPackages) } },
            onRemoveBulk = { scope.launch { viewModel.removeBulk(selectedPackages) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScreen() {
    Scaffold(
        containerColor = Color.Black,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    tint = ElectricPurple,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = ElectricPurple, strokeWidth = 2.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoRootScreen(message: String = "Root access is required to use MagisGate.") {
    Scaffold(
        containerColor = Color.Black,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    null,
                    tint = ElectricPurple,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(32.dp))
                Text("Root Required", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(
                    message,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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
    selectedPackages: Set<String>,
    onSearchChange: (String) -> Unit,
    onFilterUpdate: (FilterSortState) -> Unit,
    onClearLogs: () -> Unit,
    onInject: suspend (AppInfo) -> Boolean,
    onRemove: suspend (AppInfo) -> Boolean,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onInjectBulk: () -> Unit,
    onRemoveBulk: () -> Unit,
) {
    val tabs = listOf("No Root", "Root Apps", "Logs")
    var selectedTab by remember { mutableIntStateOf(0) }
    val isLogsTab = selectedTab == 2
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val confirmationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val showFilterSheet = remember { mutableStateOf(value = false) }
    val appToConfirm = remember { mutableStateOf<AppInfo?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (showFilterSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet.value = false },
            sheetState = filterSheetState,
        ) {
            FilterSheet(state = filterSort, onUpdate = onFilterUpdate)
        }
    }

    if (appToConfirm.value != null) {
        ModalBottomSheet(
            onDismissRequest = { appToConfirm.value = null },
            sheetState = confirmationSheetState,
        ) {
            ConfirmationSheet(
                app = appToConfirm.value!!,
                onInject = { onInject(appToConfirm.value!!) },
                onDismiss = { appToConfirm.value = null }
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (selectedPackages.isNotEmpty() && selectedTab < 2) {
                            Text("${selectedPackages.size} Selected")
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("MagisGate")
                            }
                        }
                    },
                    navigationIcon = {
                        if (selectedPackages.isNotEmpty() && selectedTab < 2) {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, "Clear Selection")
                            }
                        }
                    },
                    actions = {
                        if (selectedPackages.isNotEmpty() && selectedTab < 2) {
                            val isNoRootTab = selectedTab == 0
                            IconButton(onClick = {
                                if (isNoRootTab) onInjectBulk() else onRemoveBulk()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isNoRootTab) "Policies injected" else "Policies removed"
                                    )
                                }
                            }) {
                                Icon(
                                    if (isNoRootTab) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                                    if (isNoRootTab) "Grant Root" else "Remove Root",
                                    tint = ElectricPurple
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                )
                if (!isLogsTab) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SearchBarRow(
                            query = searchQuery,
                            onQueryChange = onSearchChange,
                            onFilterClick = { showFilterSheet.value = true },
                        )
                    }
                }
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = ElectricPurple,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTabIndex = selectedTab),
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
                            icon = if (i == 2) ({
                                Icon(
                                    Icons.Default.Terminal,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }) else null,
                            selectedContentColor = ElectricPurple,
                            unselectedContentColor = Color.White.copy(alpha = 0.54f),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (selectedTab) {
                0 -> AppTab(
                    apps = noRootApps,
                    isLoading = isLoadingRootData,
                    onAppClick = { onToggleSelection(it.packageName) },
                    selectedPackages = selectedPackages,
                    onToggleSelection = onToggleSelection,
                    onAction = { app -> appToConfirm.value = app },
                    actionIcon = Icons.Default.AddCircle
                )

                1 -> AppTab(
                    apps = rootApps,
                    isLoading = isLoadingRootData,
                    onAppClick = { onToggleSelection(it.packageName) },
                    selectedPackages = selectedPackages,
                    onToggleSelection = onToggleSelection,
                    emptyForRootTab = true,
                    onAction = { app ->
                        scope.launch {
                            val success = onRemove(app)
                            snackbarHostState.showSnackbar(
                                if (success) "Root ungranted for ${app.appName}"
                                else "Failed to ungrant root"
                            )
                        }
                    },
                    actionIcon = Icons.Default.RemoveCircle
                )

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
    selectedPackages: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    emptyForRootTab: Boolean = false,
    onAction: (AppInfo) -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricPurple)
        }

        apps.isEmpty() && emptyForRootTab -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.ManageAccounts,
                    null,
                    tint = Color.White.copy(alpha = 0.24f),
                    modifier = Modifier.size(64.dp)
                )
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
                AppListItem(
                    app = app,
                    onClick = { onAppClick(app) },
                    onLongClick = { onToggleSelection(app.packageName) },
                    isSelected = selectedPackages.contains(app.packageName),
                    trailingIcon = {
                        IconButton(onClick = { onAction(app) }) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                tint = if (actionIcon == Icons.Default.AddCircle) ElectricPurple else Color.White.copy(alpha = 0.54f)
                            )
                        }
                    }
                )
            }
        }
    }
}
