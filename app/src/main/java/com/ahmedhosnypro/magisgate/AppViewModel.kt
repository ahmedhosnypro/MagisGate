package com.ahmedhosnypro.magisgate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedhosnypro.magisgate.data.RootRepository
import com.ahmedhosnypro.magisgate.model.AppFilter
import com.ahmedhosnypro.magisgate.model.AppInfo
import com.ahmedhosnypro.magisgate.model.FilterSortState
import com.ahmedhosnypro.magisgate.model.LogEntry
import com.ahmedhosnypro.magisgate.model.SortBy
import com.ahmedhosnypro.magisgate.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.plus

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val tag = "MagisGate"

    // ── Root Check ────────────────────────────────────────────────────────────
    val isRooted = MutableStateFlow<Boolean?>(null) // null = loading

    // ── Apps ──────────────────────────────────────────────────────────────────
    val allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val isLoadingApps = MutableStateFlow(value = true)

    // ── Granted UIDs + UID Map ────────────────────────────────────────────────
    val grantedUids = MutableStateFlow<Set<Int>>(emptySet())
    val packageUidMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val isLoadingRootData = MutableStateFlow(value = true)

    // ── Selection ─────────────────────────────────────────────────────────────
    val selectedPackages = MutableStateFlow<Set<String>>(emptySet())

    fun toggleSelection(packageName: String) {
        val current = selectedPackages.value
        selectedPackages.value = if (current.contains(packageName)) {
            current - packageName
        } else {
            current + packageName
        }
    }

    fun clearSelection() {
        selectedPackages.value = emptySet()
    }

    suspend fun injectBulk(packageNames: Set<String>) {
        packageNames.forEach { pkg ->
            injectPolicy(pkg)
        }
        clearSelection()
    }

    suspend fun removeBulk(packageNames: Set<String>) {
        packageNames.forEach { pkg ->
            removePolicy(pkg)
        }
        clearSelection()
    }

    // ── Search + Filter ───────────────────────────────────────────────────────
    val searchQuery = MutableStateFlow("")
    val filterSort = MutableStateFlow(FilterSortState())

    // ── Logs ──────────────────────────────────────────────────────────────────
    val logs = MutableStateFlow<List<LogEntry>>(emptyList())

    // ── Derived lists ─────────────────────────────────────────────────────────
    val noRootApps: StateFlow<List<AppInfo>> = combine(
        allApps,
        grantedUids,
        packageUidMap,
        searchQuery,
        filterSort,
    ) { apps, granted, uidMap, query, fs ->
        val filtered = apps.filter { app ->
            val uid = uidMap[app.packageName]
            (uid == null) || !granted.contains(uid)
        }
        applyFilterSort(filtered, query, fs)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rootApps: StateFlow<List<AppInfo>> = combine(
        allApps,
        grantedUids,
        packageUidMap,
        searchQuery,
        filterSort,
    ) { apps, granted, uidMap, query, fs ->
        val filtered = apps.filter { app ->
            val uid = uidMap[app.packageName]
            (uid != null) && granted.contains(uid)
        }
        applyFilterSort(filtered, query, fs)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            Log.i(tag, "loadAll: starting (root check + app enumeration)")
            isRooted.value = RootRepository.checkRoot()
            val apps = RootRepository.getInstalledApps(getApplication())
            allApps.value = apps
            isLoadingApps.value = false
            Log.i(tag, "loadAll: rooted=${isRooted.value}, apps=${apps.size}")

            if (isRooted.value == true) {
                val uids = RootRepository.getGrantedRootUids(::addLog)
                grantedUids.value = uids
                val uidMap = RootRepository.getPackageUidMap(apps, ::addLog)
                uidMap.forEach { (pkg, uid) ->
                    if (!packageUidMap.value.containsKey(pkg)) {
                        packageUidMap.value += (pkg to uid)
                    }
                }
            }
            isLoadingRootData.value = false
        }
    }

    fun addLog(message: String) {
        logs.value += LogEntry(message)
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
        val injectedUid = RootRepository.injectPolicy(packageName) { line ->
            addLog(line)
        }

        if (injectedUid != null) {
            // Update UID map if needed
            if (!packageUidMap.value.containsKey(packageName)) {
                packageUidMap.value += (packageName to injectedUid)
            }

            addLog("\n[?] Verifying grant...")
            val uids = RootRepository.getGrantedRootUids(::addLog)
            grantedUids.value = uids

            if (!uids.contains(injectedUid)) {
                addLog("\n[✗] Verification failed: App UID ($injectedUid) not found in granted list after injection.")
                return false
            }
            addLog("\n[✓] Verification successful: App is now in granted list.")
            return true
        }

        return false
    }

    suspend fun removePolicy(packageName: String): Boolean {
        val success = RootRepository.removePolicy(packageName) { line ->
            addLog(line)
        }
        if (success) {
            val uids = RootRepository.getGrantedRootUids(::addLog)
            grantedUids.value = uids
        }
        return success
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
        return filtered.sortedWith(
            Comparator { a, b ->
                val cmp = when (fs.sortBy) {
                    SortBy.NAME -> a.appName.compareTo(b.appName)
                    SortBy.PACKAGE_NAME -> a.packageName.compareTo(b.packageName)
                    SortBy.APP_SIZE -> a.apkSize.compareTo(b.apkSize)
                    SortBy.INSTALL_DATE -> a.installTime.compareTo(b.installTime)
                    SortBy.UPDATE_DATE -> a.updateTime.compareTo(b.updateTime)
                }
                if (fs.sortOrder == SortOrder.ASCENDING) cmp else -cmp
            }
        )
    }
}
