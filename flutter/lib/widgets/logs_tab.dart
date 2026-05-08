import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/app_providers.dart';

class LogsTab extends ConsumerWidget {
  const LogsTab({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final logs = ref.watch(appLogProvider);

    return Column(
      children: [
        // Toolbar
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
          child: Row(
            children: [
              Text('${logs.length} entries',
                  style: Theme.of(context)
                      .textTheme
                      .bodySmall
                      ?.copyWith(color: Colors.white38)),
              const Spacer(),
              if (logs.isNotEmpty) ...[
                _ToolbarButton(
                  icon: Icons.copy_all,
                  label: 'Copy All',
                  onTap: () {
                    final text = logs.map((e) => e.formatted).join('\n');
                    Clipboard.setData(ClipboardData(text: text));
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                      content: Text('All logs copied'),
                      behavior: SnackBarBehavior.floating,
                      duration: Duration(seconds: 2),
                    ));
                  },
                ),
                const SizedBox(width: 8),
                _ToolbarButton(
                  icon: Icons.delete_sweep_outlined,
                  label: 'Clear',
                  onTap: () => ref.read(appLogProvider.notifier).clear(),
                ),
              ],
            ],
          ),
        ),
        const Divider(color: Colors.white12, height: 1),
        // List
        Expanded(
          child: logs.isEmpty
              ? const Center(
                  child: Text('No logs yet.',
                      style: TextStyle(color: Colors.white38)),
                )
              : ListView.builder(
                  reverse: true,
                  itemCount: logs.length,
                  itemBuilder: (context, index) {
                    // reverse: show newest at bottom
                    final entry = logs[logs.length - 1 - index];
                    return _LogItem(entry: entry);
                  },
                ),
        ),
      ],
    );
  }
}

class _LogItem extends StatelessWidget {
  final dynamic entry; // LogEntry

  const _LogItem({required this.entry});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onLongPress: () {
        Clipboard.setData(ClipboardData(text: entry.formatted));
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Log entry copied'),
          behavior: SnackBarBehavior.floating,
          duration: Duration(seconds: 2),
        ));
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Text(
                entry.formatted,
                style: const TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 11,
                  color: Colors.white70,
                ),
              ),
            ),
            GestureDetector(
              onTap: () {
                Clipboard.setData(ClipboardData(text: entry.formatted));
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                  content: Text('Copied'),
                  behavior: SnackBarBehavior.floating,
                  duration: Duration(seconds: 1),
                ));
              },
              child: const Padding(
                padding: EdgeInsets.only(left: 8),
                child: Icon(Icons.copy, size: 14, color: Colors.white24),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ToolbarButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _ToolbarButton(
      {required this.icon, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Row(children: [
          Icon(icon, size: 16, color: Colors.white54),
          const SizedBox(width: 4),
          Text(label,
              style: const TextStyle(fontSize: 12, color: Colors.white54)),
        ]),
      ),
    );
  }
}
