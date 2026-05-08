package com.example.rootinjector

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rootinjector.data.RootRepository
import com.example.rootinjector.model.AppFilter
import com.example.rootinjector.model.AppInfo
import com.example.rootinjector.model.FilterSortState
import com.example.rootinjector.model.LogEntry
import com.example.rootinjector.model.SortBy
import com.example.rootinjector.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    // ── Root Check ────────────────────────────────────────────────────────────
    val isRooted = MutableStateFlow<Boolean?>(null) // null = loading

    // ── Apps ──────────────────────────────────────────────────────────────────
    val allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val isLoadingApps = MutableStateFlow(true)

    // ── Granted UIDs + UID Map ────────────────────────────────────────────────
    val grantedUids = MutableStateFlow<Set<Int>>(emptySet())
    val packageUidMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val isLoadingRootData = MutableStateFlow(true)

    // ── Search + Filter ───────────────────────────────────────────────────────
    val searchQuery = MutableStateFlow("")
    val filterSort = MutableStateFlow(FilterSortState())

    // ── Logs ──────────────────────────────────────────────────────────────────
    val logs = MutableStateFlow<List<LogEntry>>(emptyList())

    // ── Derived lists ─────────────────────────────────────────────────────────
    val noRootApps: StateFlow<List<AppInfo>> = combine(
        allApps, grantedUids, packageUidMap, searchQuery, filterSort
    ) { apps, granted, uidMap, query, fs ->
        val filtered = apps.filter { app ->
            val uid = uidMap[app.packageName]
            uid == null || !granted.contains(uid)
        }
        applyFilterSort(filtered, query, fs)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rootApps: StateFlow<List<AppInfo>> = combine(
        allApps, grantedUids, packageUidMap, searchQuery, filterSort
    ) { apps, granted, uidMap, query, fs ->
        val filtered = apps.filter { app ->
            val uid = uidMap[app.packageName]
            uid != null && granted.contains(uid)
        }
        applyFilterSort(filtered, query, fs)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            isRooted.value = RootRepository.checkRoot()
            val apps = RootRepository.getInstalledApps(getApplication())
            allApps.value = apps
            isLoadingApps.value = false

            if (isRooted.value == true) {
                val uids = RootRepository.getGrantedRootUids(::addLog)
                grantedUids.value = uids
                val uidMap = RootRepository.getPackageUidMap(apps, ::addLog)
                packageUidMap.value = uidMap
            }
            isLoadingRootData.value = false
        }
    }

    fun addLog(message: String) {
        logs.value = logs.value + LogEntry(message)
    }

    fun clearLogs() {
        logs.value = emptyList()
    }

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun updateFilterSort(fs: FilterSortState) {
        filterSort.value = fs
    }

    suspend fun injectPolicy(packageName: String): Boolean {
        val logLines = mutableListOf<String>()
        val result = RootRepository.injectPolicy(packageName) { line ->
            logLines.add(line)
            addLog(line)
        }
        return result
    }

    private fun applyFilterSort(
        apps: List<AppInfo>,
        query: String,
        fs: FilterSortState,
    ): List<AppInfo> {
        val filtered = apps.filter { app ->
            when (fs.filter) {
                AppFilter.SYSTEM -> if (!app.isSystemApp) return@filter false
                AppFilter.USER -> if (app.isSystemApp) return@filter false
                AppFilter.ENABLED -> if (!app.isEnabled) return@filter false
                AppFilter.DISABLED -> if (app.isEnabled) return@filter false
                AppFilter.ALL -> Unit
            }
            if (query.isNotEmpty()) {
                val q = query.lowercase()
                if (!app.appName.lowercase().contains(q) && !app.packageName.lowercase().contains(q))
                    return@filter false
            }
            true
        }
        return filtered.sortedWith(Comparator { a, b ->
            val cmp = when (fs.sortBy) {
                SortBy.NAME -> a.appName.compareTo(b.appName)
                SortBy.PACKAGE_NAME -> a.packageName.compareTo(b.packageName)
                SortBy.APP_SIZE -> a.apkSize.compareTo(b.apkSize)
                SortBy.INSTALL_DATE -> a.installTime.compareTo(b.installTime)
                SortBy.UPDATE_DATE -> a.updateTime.compareTo(b.updateTime)
            }
            if (fs.sortOrder == SortOrder.ASCENDING) cmp else -cmp
        })
    }
}
