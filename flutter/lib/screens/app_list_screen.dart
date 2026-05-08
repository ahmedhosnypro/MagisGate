import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/app_providers.dart';
import '../widgets/search_bar_row.dart';
import '../widgets/app_list_item.dart';
import '../widgets/logs_tab.dart';

const Color electricPurple = Color(0xFF8F00FF);
const Color trueBlack = Color(0xFF000000);

class AppListScreen extends ConsumerWidget {
  const AppListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rootState = ref.watch(rootCheckProvider);

    return Scaffold(
      body: rootState.when(
        loading: () => _shell(context,
            child: const Center(
              child: CircularProgressIndicator(
                  valueColor: AlwaysStoppedAnimation(electricPurple)),
            )),
        error: (_, __) => _shell(context,
            child: const _NoRootView(
                message: 'Root check failed. Please restart the app.')),
        data: (isRooted) => isRooted
            ? const _RootedView()
            : _shell(context, child: const _NoRootView()),
      ),
    );
  }

  Widget _shell(BuildContext context, {required Widget child}) {
    return CustomScrollView(slivers: [
      SliverAppBar(
        backgroundColor: trueBlack,
        pinned: true,
        centerTitle: true,
        title: const Text('RootInjector',
            style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      SliverFillRemaining(child: child),
    ]);
  }
}

class _NoRootView extends StatelessWidget {
  final String message;
  const _NoRootView(
      {this.message = 'Root access is required to use RootInjector.'});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.lock_outline, color: electricPurple, size: 72),
            const SizedBox(height: 24),
            Text('Root Required',
                style: Theme.of(context)
                    .textTheme
                    .headlineSmall
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            Text(message,
                textAlign: TextAlign.center,
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: Colors.white54)),
          ],
        ),
      ),
    );
  }
}

class _RootedView extends ConsumerStatefulWidget {
  const _RootedView();

  @override
  ConsumerState<_RootedView> createState() => _RootedViewState();
}

class _RootedViewState extends ConsumerState<_RootedView>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _tabController.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  bool get _isLogsTab => _tabController.index == 2;

  @override
  Widget build(BuildContext context) {
    final appsState = ref.watch(appsProvider);

    return NestedScrollView(
      headerSliverBuilder: (context, _) => [
        SliverAppBar(
          backgroundColor: trueBlack,
          floating: true,
          pinned: true,
          snap: false,
          centerTitle: true,
          title: const Text('RootInjector',
              style: TextStyle(fontWeight: FontWeight.bold)),
          bottom: PreferredSize(
            preferredSize: Size.fromHeight(_isLogsTab ? 48 : 112),
            child: Column(children: [
              if (!_isLogsTab)
                const Padding(
                  padding: EdgeInsets.fromLTRB(16, 0, 16, 8),
                  child: SearchBarRow(),
                ),
              TabBar(
                controller: _tabController,
                indicatorColor: electricPurple,
                labelColor: electricPurple,
                unselectedLabelColor: Colors.white54,
                dividerColor: Colors.white12,
                tabs: const [
                  Tab(text: 'No Root'),
                  Tab(text: 'Root Apps'),
                  Tab(icon: Icon(Icons.terminal, size: 18)),
                ],
              ),
            ]),
          ),
        ),
      ],
      body: TabBarView(
        controller: _tabController,
        children: [
          _AppTab(appsState: appsState, useRootApps: false),
          _AppTab(appsState: appsState, useRootApps: true),
          const LogsTab(),
        ],
      ),
    );
  }
}

class _AppTab extends ConsumerWidget {
  final AsyncValue appsState;
  final bool useRootApps;

  const _AppTab({required this.appsState, required this.useRootApps});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final uidMapState = ref.watch(packageUidMapProvider);
    final grantedState = ref.watch(grantedRootUidsProvider);
    final apps = useRootApps
        ? ref.watch(rootAppsProvider)
        : ref.watch(noRootAppsProvider);

    // Show loading if apps or (for root tab) uid data is still loading
    final isLoading = appsState.isLoading ||
        (useRootApps &&
            (uidMapState.isLoading || grantedState.isLoading));

    if (isLoading) {
      return const Center(
        child: CircularProgressIndicator(
            valueColor: AlwaysStoppedAnimation(electricPurple)),
      );
    }

    if (appsState.hasError) {
      return Center(child: Text('Error loading apps: ${appsState.error}'));
    }

    if (useRootApps && apps.isEmpty) {
      final uidMap = uidMapState.asData?.value ?? {};
      final grantedUids = grantedState.asData?.value ?? {};
      return _RootAppsEmptyView(
        uidMapSize: uidMap.length,
        grantedUidsSize: grantedUids.length,
        uidMapError: uidMapState.hasError ? '${uidMapState.error}' : null,
        grantedError: grantedState.hasError ? '${grantedState.error}' : null,
      );
    }

    return CustomScrollView(slivers: [
      SliverList(
        delegate: SliverChildBuilderDelegate(
          (context, index) => AppListItem(app: apps[index]),
          childCount: apps.length,
        ),
      ),
    ]);
  }
}

class _RootAppsEmptyView extends StatelessWidget {
  final int uidMapSize;
  final int grantedUidsSize;
  final String? uidMapError;
  final String? grantedError;

  const _RootAppsEmptyView({
    required this.uidMapSize,
    required this.grantedUidsSize,
    this.uidMapError,
    this.grantedError,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.manage_accounts_outlined,
                color: Colors.white24, size: 64),
            const SizedBox(height: 20),
            Text('No Root-Granted Apps',
                style: Theme.of(context)
                    .textTheme
                    .titleMedium
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            _DiagRow(label: 'UID map entries', value: '$uidMapSize'),
            _DiagRow(label: 'Granted UIDs found', value: '$grantedUidsSize'),
            if (uidMapError != null)
              _DiagRow(label: 'UID map error', value: uidMapError!, isError: true),
            if (grantedError != null)
              _DiagRow(label: 'Granted UIDs error', value: grantedError!, isError: true),
            const SizedBox(height: 16),
            Text(
              grantedUidsSize == 0
                  ? 'No apps have been granted root via Magisk yet.\nUse the "No Root" tab to inject a policy.'
                  : uidMapSize == 0
                      ? 'UID map is empty — the shell script may have failed.\nCheck the Logs tab for details.'
                      : 'UIDs were found but none matched installed apps.\nCheck the Logs tab for details.',
              textAlign: TextAlign.center,
              style: Theme.of(context)
                  .textTheme
                  .bodySmall
                  ?.copyWith(color: Colors.white38),
            ),
          ],
        ),
      ),
    );
  }
}

class _DiagRow extends StatelessWidget {
  final String label;
  final String value;
  final bool isError;

  const _DiagRow(
      {required this.label, required this.value, this.isError = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('$label: ',
              style: const TextStyle(color: Colors.white38, fontSize: 12)),
          Text(value,
              style: TextStyle(
                  color: isError ? Colors.redAccent : Colors.white60,
                  fontSize: 12,
                  fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }
}
