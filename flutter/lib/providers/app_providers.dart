import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/filter_sort.dart';
import '../models/log_entry.dart';

// ── Live Log ──────────────────────────────────────────────────────────────────

class AppLogNotifier extends Notifier<List<LogEntry>> {
  @override
  List<LogEntry> build() => [];

  void add(String message) {
    state = [...state, LogEntry(message)];
  }

  void clear() => state = [];
}

final appLogProvider =
    NotifierProvider<AppLogNotifier, List<LogEntry>>(AppLogNotifier.new);

// ── Root Check ────────────────────────────────────────────────────────────────

final rootCheckProvider = FutureProvider<bool>((ref) async {
  try {
    final result = await Process.run('su', ['-c', 'echo root_granted']);
    return result.stdout.toString().trim() == 'root_granted';
  } catch (_) {
    return false;
  }
});

// ── Apps ──────────────────────────────────────────────────────────────────────

final appsProvider = FutureProvider<List<Application>>((ref) async {
  ref.watch(rootCheckProvider);
  return DeviceApps.getInstalledApplications(
    includeAppIcons: true,
    includeSystemApps: true,
    onlyAppsWithLaunchIntent: false,
  );
});

// ── Granted UIDs ──────────────────────────────────────────────────────────────

final grantedRootUidsProvider = FutureProvider<Set<int>>((ref) async {
  final log = ref.read(appLogProvider.notifier);
  log.add('[GrantedUIDs] Querying Magisk DB...');

  final cmds = [
    r'magisk --sqlite "SELECT uid FROM policies WHERE policy=2;"',
    r'sqlite3 /data/adb/magisk.db "SELECT uid FROM policies WHERE policy=2;"',
  ];

  for (final cmd in cmds) {
    final result = await Process.run('su', ['-c', cmd]);
    log.add('[GrantedUIDs] cmd: $cmd');
    log.add('[GrantedUIDs] exit=${result.exitCode} stdout="${result.stdout.toString().trim()}" stderr="${result.stderr.toString().trim()}"');

    if (result.exitCode == 0 && result.stdout.toString().trim().isNotEmpty) {
      final uids = <int>{};
      for (final line in result.stdout.toString().split('\n')) {
        final trimmed = line.trim();
        if (trimmed.isEmpty) continue;
        final match = RegExp(r'(?:uid=)?(\d+)').firstMatch(trimmed);
        if (match != null) {
          final uid = int.tryParse(match.group(1)!);
          if (uid != null) uids.add(uid);
        }
      }
      log.add('[GrantedUIDs] parsed ${uids.length} UIDs: $uids');
      return uids;
    }
  }

  log.add('[GrantedUIDs] All commands failed — returning empty set');
  return {};
});

// ── Package → UID Map ─────────────────────────────────────────────────────────

final packageUidMapProvider = FutureProvider<Map<String, int>>((ref) async {
  final log = ref.read(appLogProvider.notifier);
  final apps = await ref.watch(appsProvider.future);
  final map = <String, int>{};

  log.add('[UIDMap] Building UID map for ${apps.length} apps...');

  try {
    // Write package list to temp file (avoids arg length limits)
    final packages = apps.map((a) => a.packageName).join('\n');
    final writeResult = await Process.run('su', [
      '-c',
      'printf "%s" ${_shellEscape(packages)} > /data/local/tmp/_ri_pkgs.txt && echo ok',
    ]);
    log.add('[UIDMap] write pkgs file: exit=${writeResult.exitCode} out="${writeResult.stdout.toString().trim()}"');

    if (writeResult.exitCode != 0) {
      throw Exception('Failed to write package list: ${writeResult.stderr}');
    }

    // Run loop — note double-quotes around variables so they expand in sh
    const script = r'''
while IFS= read -r p; do
  uid=$(pm dump "$p" 2>/dev/null | grep -m1 "userId=" | grep -oE "userId=[0-9]+" | cut -d= -f2)
  if [ -n "$uid" ]; then
    echo "$p=$uid"
  fi
done < /data/local/tmp/_ri_pkgs.txt
rm -f /data/local/tmp/_ri_pkgs.txt
''';

    final result = await Process.run('su', ['-c', script]);
    log.add('[UIDMap] loop exit=${result.exitCode} stderr="${result.stderr.toString().trim()}"');

    int parsed = 0;
    for (final line in result.stdout.toString().split('\n')) {
      final trimmed = line.trim();
      if (trimmed.isEmpty) continue;
      final eqIdx = trimmed.indexOf('=');
      if (eqIdx < 1) continue;
      final pkg = trimmed.substring(0, eqIdx);
      final uid = int.tryParse(trimmed.substring(eqIdx + 1));
      if (uid != null) {
        map[pkg] = uid;
        parsed++;
      }
    }
    log.add('[UIDMap] parsed $parsed entries. Sample: ${map.entries.take(3).map((e) => "${e.key}=${e.value}").join(", ")}');
  } catch (e) {
    log.add('[UIDMap] ERROR: $e');
  }

  return map;
});

String _shellEscape(String s) => "'${s.replaceAll("'", "'\\''")}'";

// ── UID helper (used by ConfirmationSheet) ────────────────────────────────────

Future<int?> getPackageUid(String packageName) async {
  final r1 = await Process.run('su', [
    '-c',
    'pm dump "$packageName" | grep -m1 "userId=" | grep -oE "userId=[0-9]+" | cut -d= -f2',
  ]);
  if (r1.exitCode == 0 && r1.stdout.toString().trim().isNotEmpty) {
    return int.tryParse(r1.stdout.toString().trim());
  }
  final r2 = await Process.run('su', ['-c', 'stat -c %u /data/data/$packageName']);
  if (r2.exitCode == 0) {
    return int.tryParse(r2.stdout.toString().trim());
  }
  return null;
}

// ── Search ────────────────────────────────────────────────────────────────────

class SearchQueryNotifier extends Notifier<String> {
  @override
  String build() => '';
  void setQuery(String q) => state = q;
}

final searchQueryProvider =
    NotifierProvider<SearchQueryNotifier, String>(SearchQueryNotifier.new);

// ── Filter + Sort ─────────────────────────────────────────────────────────────

class FilterSortNotifier extends Notifier<FilterSortState> {
  @override
  FilterSortState build() => const FilterSortState();
  void update(FilterSortState s) => state = s;
}

final filterSortProvider =
    NotifierProvider<FilterSortNotifier, FilterSortState>(FilterSortNotifier.new);

List<Application> _applyFilterSort(
  List<Application> apps,
  String query,
  FilterSortState fs,
) {
  var list = apps.where((app) {
    switch (fs.filter) {
      case AppFilter.system:
        if (!app.systemApp) return false;
      case AppFilter.user:
        if (app.systemApp) return false;
      case AppFilter.enabled:
        if (!app.enabled) return false;
      case AppFilter.disabled:
        if (app.enabled) return false;
      case AppFilter.all:
        break;
    }
    if (query.isNotEmpty) {
      final q = query.toLowerCase();
      if (!app.appName.toLowerCase().contains(q) &&
          !app.packageName.toLowerCase().contains(q)) {
        return false;
      }
    }
    return true;
  }).toList();

  list.sort((a, b) {
    int cmp;
    switch (fs.sortBy) {
      case SortBy.name:
        cmp = a.appName.compareTo(b.appName);
      case SortBy.packageName:
        cmp = a.packageName.compareTo(b.packageName);
      case SortBy.appSize:
        cmp = a.apkFilePath.length.compareTo(b.apkFilePath.length);
      case SortBy.installDate:
        cmp = a.installTimeMillis.compareTo(b.installTimeMillis);
      case SortBy.updateDate:
        cmp = a.updateTimeMillis.compareTo(b.updateTimeMillis);
    }
    return fs.sortOrder == SortOrder.ascending ? cmp : -cmp;
  });

  return list;
}

// ── Derived App Lists ─────────────────────────────────────────────────────────

final noRootAppsProvider = Provider<List<Application>>((ref) {
  final apps = ref.watch(appsProvider).asData?.value ?? [];
  final grantedUids = ref.watch(grantedRootUidsProvider).asData?.value ?? {};
  final uidMap = ref.watch(packageUidMapProvider).asData?.value ?? {};
  final query = ref.watch(searchQueryProvider);
  final fs = ref.watch(filterSortProvider);

  final filtered = apps.where((app) {
    final uid = uidMap[app.packageName];
    return uid == null || !grantedUids.contains(uid);
  }).toList();

  return _applyFilterSort(filtered, query, fs);
});

final rootAppsProvider = Provider<List<Application>>((ref) {
  final apps = ref.watch(appsProvider).asData?.value ?? [];
  final grantedUids = ref.watch(grantedRootUidsProvider).asData?.value ?? {};
  final uidMap = ref.watch(packageUidMapProvider).asData?.value ?? {};
  final query = ref.watch(searchQueryProvider);
  final fs = ref.watch(filterSortProvider);

  final filtered = apps.where((app) {
    final uid = uidMap[app.packageName];
    return uid != null && grantedUids.contains(uid);
  }).toList();

  return _applyFilterSort(filtered, query, fs);
});
