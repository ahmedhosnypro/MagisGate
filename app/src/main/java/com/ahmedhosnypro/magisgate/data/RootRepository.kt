package com.ahmedhosnypro.magisgate.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.ahmedhosnypro.magisgate.model.AppInfo
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RootRepository {

    // ── Root Check ────────────────────────────────────────────────────────────

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        Shell.getShell().isRoot
    }

    // ── Apps ──────────────────────────────────────────────────────────────────

    @SuppressLint("QueryPermissionsNeeded")
    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
        packages.mapNotNull { pkg ->
            val ai = pkg.applicationInfo ?: return@mapNotNull null
            AppInfo(
                appName = pm.getApplicationLabel(ai).toString(),
                packageName = pkg.packageName,
                icon = runCatching { pm.getApplicationIcon(ai) }.getOrNull(),
                isSystemApp = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isEnabled = ai.enabled,
                installTime = pkg.firstInstallTime,
                updateTime = pkg.lastUpdateTime,
                apkSize = runCatching { File(ai.sourceDir).length() }.getOrDefault(0L),
            )
        }
    }

    // ── Granted UIDs ──────────────────────────────────────────────────────────

    suspend fun getGrantedRootUids(log: (String) -> Unit): Set<Int> = withContext(Dispatchers.IO) {
        log("[GrantedUIDs] Querying Magisk DB...")
        val cmds = listOf(
            """magisk --sqlite "SELECT uid FROM policies WHERE policy=2;"""",
            """sqlite3 /data/adb/magisk.db "SELECT uid FROM policies WHERE policy=2;"""",
        )
        for (cmd in cmds) {
            val result = Shell.cmd(cmd).exec()
            log("[GrantedUIDs] cmd: $cmd")
            log("[GrantedUIDs] exit=${result.code} out=\"${result.out.joinToString("\\n")}\"")
            if (result.isSuccess && result.out.isNotEmpty()) {
                val uids = mutableSetOf<Int>()
                for (line in result.out) {
                    val match = Regex("""(?:uid=)?(\d+)""").find(line.trim()) ?: continue
                    match.groupValues[1].toIntOrNull()?.let { uids.add(it) }
                }
                log("[GrantedUIDs] parsed ${uids.size} UIDs: $uids")
                return@withContext uids
            }
        }
        log("[GrantedUIDs] All commands failed — returning empty set")
        emptySet()
    }

    // ── Package → UID Map ─────────────────────────────────────────────────────

    suspend fun getPackageUidMap(apps: List<AppInfo>, log: (String) -> Unit): Map<String, Int> =
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<String, Int>()
            log("[UIDMap] Building UID map...")
            try {
                // Optimized: use pm list packages -U to get all UIDs at once
                val result = Shell.cmd("pm list packages -U").exec()
                if (result.isSuccess) {
                    for (line in result.out) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        // Expected format: package:com.example.app uid:10123
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            val pkgPart = parts[0].removePrefix("package:")
                            val uidPart = parts[1].removePrefix("uid:")
                            uidPart.toIntOrNull()?.let { uid ->
                                map[pkgPart] = uid
                            }
                        }
                    }
                    log("[UIDMap] Parsed ${map.size} UIDs from pm list")
                    if (map.isNotEmpty()) return@withContext map
                }

                // Fallback: build map via loop if pm list failed (rare)
                log("[UIDMap] Fallback to loop for ${apps.size} apps...")
                for (app in apps) {
                    val r = Shell.cmd("pm dump \"${app.packageName}\" | grep -m1 \"userId=\"").exec()
                    val m = Regex("""userId=(\d+)""").find(r.out.joinToString())
                    m?.groupValues?.get(1)?.toIntOrNull()?.let { uid ->
                        map[app.packageName] = uid
                    }
                }
                log("[UIDMap] Parsed ${map.size} entries total")
            } catch (e: Exception) {
                log("[UIDMap] ERROR: $e")
            }
            map
        }

    // ── Inject Policy ─────────────────────────────────────────────────────────

    suspend fun injectPolicy(packageName: String, log: (String) -> Unit): Int? =
        withContext(Dispatchers.IO) {
            log("=== MagisGate Debug Log ===")
            log("Package: $packageName")

            // Step 1: get UID
            log("\n[1] Getting UID...")
            var uid: Int? = null

            // Try pm list packages -U first (as per gist)
            val r0 = Shell.cmd("pm list packages -U | grep \"$packageName\"").exec()
            if (r0.isSuccess) {
                for (line in r0.out) {
                    val trimmed = line.trim()
                    // Expected: package:com.example.app uid:10123
                    if (trimmed.contains("package:$packageName ")) {
                        val match = Regex("""uid:(\d+)""").find(trimmed)
                        uid = match?.groupValues?.get(1)?.toIntOrNull()
                        if (uid != null) break
                    }
                }
            }

            if (uid != null) {
                log("  parsed uid from pm list: $uid")
            } else {
                log("  pm list failed or mismatch, trying pm dump...")
                val r1 = Shell.cmd("pm dump \"$packageName\" | grep -m1 \"userId=\"").exec()
                val m1 = Regex("""userId=(\d+)""").find(r1.out.joinToString())
                uid = m1?.groupValues?.get(1)?.toIntOrNull()
                log("  parsed uid from pm dump: $uid")
            }

            if (uid == null) {
                log("\n[1b] Fallback: stat /data/data/$packageName...")
                val r2 = Shell.cmd("stat -c %u /data/data/$packageName").exec()
                uid = r2.out.firstOrNull()?.trim()?.toIntOrNull()
                log("  parsed uid from stat: $uid")
            }

            if (uid == null) {
                log("\n[✗] Could not resolve UID for $packageName")
                return@withContext null
            }

            // Step 2: inject policy
            log("\n[2] Injecting Magisk policy for uid=$uid...")
            
            // Following gist: Delete existing then Insert
            val deleteCmd = """magisk --sqlite "DELETE FROM policies WHERE uid=$uid;""""
            val insertCmd = """magisk --sqlite "INSERT INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1);""""
            
            Shell.cmd(deleteCmd).exec()
            val r3 = Shell.cmd(insertCmd).exec()
            log("  insert exit: ${r3.code}")

            if (!r3.isSuccess) {
                log("\n[2b] Fallback: sqlite3 /data/adb/magisk.db...")
                val deleteCmd2 = """sqlite3 /data/adb/magisk.db "DELETE FROM policies WHERE uid=$uid;""""
                val insertCmd2 = """sqlite3 /data/adb/magisk.db "INSERT INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1);""""
                Shell.cmd(deleteCmd2).exec()
                val r4 = Shell.cmd(insertCmd2).exec()
                if (!r4.isSuccess) {
                    log("\n[✗] Injection failed.")
                    return@withContext null
                }
            }

            // Step 3: Force stop (Crucial from gist!)
            log("\n[3] Force-stopping app to apply changes...")
            val r5 = Shell.cmd("am force-stop $packageName").exec()
            log("  exit: ${r5.code}")

            log("\n[✓] Success!")
            uid
        }

    // ── Remove Policy ─────────────────────────────────────────────────────────

    suspend fun removePolicy(packageName: String, log: (String) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            log("=== MagisGate Remove Policy ===")
            log("Package: $packageName")

            // Step 1: get UID
            var uid: Int? = null
            val r0 = Shell.cmd("pm list packages -U | grep \"$packageName\"").exec()
            if (r0.isSuccess) {
                for (line in r0.out) {
                    val trimmed = line.trim()
                    if (trimmed.contains("package:$packageName ")) {
                        val match = Regex("""uid:(\d+)""").find(trimmed)
                        uid = match?.groupValues?.get(1)?.toIntOrNull()
                        if (uid != null) break
                    }
                }
            }

            if (uid == null) {
                val r1 = Shell.cmd("pm dump \"$packageName\" | grep -m1 \"userId=\"").exec()
                val m1 = Regex("""userId=(\d+)""").find(r1.out.joinToString())
                uid = m1?.groupValues?.get(1)?.toIntOrNull()
            }

            if (uid == null) {
                log("[✗] Could not resolve UID for $packageName to remove.")
                return@withContext false
            }

            log("[1] Removing Magisk policy for uid=$uid...")
            val deleteCmd = """magisk --sqlite "DELETE FROM policies WHERE uid=$uid;""""
            val result = Shell.cmd(deleteCmd).exec()
            log("  delete exit: ${result.code}")

            if (!result.isSuccess) {
                log("[1b] Fallback: sqlite3 /data/adb/magisk.db...")
                val deleteCmd2 = """sqlite3 /data/adb/magisk.db "DELETE FROM policies WHERE uid=$uid;""""
                val result2 = Shell.cmd(deleteCmd2).exec()
                if (!result2.isSuccess) {
                    log("[✗] Removal failed.")
                    return@withContext false
                }
            }

            log("[2] Force-stopping app to apply changes...")
            Shell.cmd("am force-stop $packageName").exec()

            log("[✓] Success!")
            true
        }

}
