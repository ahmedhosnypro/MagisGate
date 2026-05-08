package com.example.rootinjector.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.rootinjector.model.AppInfo
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootRepository {

    // ── Root Check ────────────────────────────────────────────────────────────

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        Shell.getShell().isRoot
    }

    // ── Apps ──────────────────────────────────────────────────────────────────

    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledPackages(PackageManager.GET_META_DATA).mapNotNull { pkg ->
            val ai = pkg.applicationInfo ?: return@mapNotNull null
            AppInfo(
                appName = pm.getApplicationLabel(ai).toString(),
                packageName = pkg.packageName,
                icon = runCatching { pm.getApplicationIcon(ai) }.getOrNull(),
                isSystemApp = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isEnabled = ai.enabled,
                installTime = pkg.firstInstallTime,
                updateTime = pkg.lastUpdateTime,
                apkSize = runCatching { java.io.File(ai.sourceDir).length() }.getOrDefault(0L),
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
            log("[UIDMap] Building UID map for ${apps.size} apps...")
            try {
                val packages = apps.joinToString("\n") { it.packageName }
                val writeResult = Shell.cmd(
                    "printf '%s' ${shellEscape(packages)} > /data/local/tmp/_ri_pkgs.txt && echo ok"
                ).exec()
                log("[UIDMap] write pkgs file: exit=${writeResult.code} out=\"${writeResult.out.joinToString()}\"")
                if (!writeResult.isSuccess) throw Exception("Failed to write package list")

                val script = """
while IFS= read -r p; do
  uid=$(pm dump "${'$'}p" 2>/dev/null | grep -m1 "userId=" | grep -oE "userId=[0-9]+" | cut -d= -f2)
  if [ -n "${'$'}uid" ]; then
    echo "${'$'}p=${'$'}uid"
  fi
done < /data/local/tmp/_ri_pkgs.txt
rm -f /data/local/tmp/_ri_pkgs.txt
""".trimIndent()

                val result = Shell.cmd(script).exec()
                log("[UIDMap] loop exit=${result.code}")
                var parsed = 0
                for (line in result.out) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val eqIdx = trimmed.indexOf('=')
                    if (eqIdx < 1) continue
                    val pkg = trimmed.substring(0, eqIdx)
                    val uid = trimmed.substring(eqIdx + 1).toIntOrNull() ?: continue
                    map[pkg] = uid
                    parsed++
                }
                log("[UIDMap] parsed $parsed entries")
            } catch (e: Exception) {
                log("[UIDMap] ERROR: $e")
            }
            map
        }

    // ── Inject Policy ─────────────────────────────────────────────────────────

    suspend fun injectPolicy(packageName: String, log: (String) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            log("=== RootInjector Debug Log ===")
            log("Package: $packageName")

            // Step 1: get UID
            log("\n[1] Getting UID via pm dump...")
            var uid: Int? = null

            val r1 = Shell.cmd("pm dump \"$packageName\" | grep -m1 \"userId=\"").exec()
            log("  exit: ${r1.code}")
            log("  stdout: ${r1.out.joinToString()}")
            val m1 = Regex("""userId=(\d+)""").find(r1.out.joinToString())
            if (m1 != null) {
                uid = m1.groupValues[1].toIntOrNull()
                log("  parsed uid: $uid")
            }

            if (uid == null) {
                log("\n[1b] Fallback: stat /data/data/$packageName...")
                val r2 = Shell.cmd("stat -c %u /data/data/$packageName").exec()
                log("  exit: ${r2.code}")
                log("  stdout: ${r2.out.joinToString()}")
                uid = r2.out.firstOrNull()?.trim()?.toIntOrNull()
                log("  parsed uid: $uid")
            }

            if (uid == null) {
                log("\n[✗] Could not resolve UID for $packageName")
                return@withContext false
            }

            // Step 2: inject policy
            log("\n[2] Injecting Magisk policy for uid=$uid...")
            val cmd = """magisk --sqlite "REPLACE INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1);""""
            log("  cmd: $cmd")
            val r3 = Shell.cmd(cmd).exec()
            log("  exit: ${r3.code}")
            log("  stdout: ${r3.out.joinToString()}")

            if (!r3.isSuccess) {
                log("\n[2b] Fallback: sqlite3 /data/adb/magisk.db...")
                val cmd2 = """sqlite3 /data/adb/magisk.db "REPLACE INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1);""""
                val r4 = Shell.cmd(cmd2).exec()
                log("  exit: ${r4.code}")
                log("  stdout: ${r4.out.joinToString()}")
                if (!r4.isSuccess) {
                    log("\n[✗] Injection failed.")
                    return@withContext false
                }
            }

            log("\n[✓] Success!")
            true
        }

    private fun shellEscape(s: String) = "'${s.replace("'", "'\\''")}'"
}
