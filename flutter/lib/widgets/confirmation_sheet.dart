import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:device_apps/device_apps.dart';

const Color electricPurple = Color(0xFF8F00FF);

class ConfirmationSheet extends StatefulWidget {
  final Application app;
  const ConfirmationSheet({super.key, required this.app});

  @override
  State<ConfirmationSheet> createState() => _ConfirmationSheetState();
}

class _ConfirmationSheetState extends State<ConfirmationSheet> {
  bool _isLoading = false;
  final _log = StringBuffer();

  void _addLog(String line) {
    _log.writeln(line);
    debugPrint(line);
  }

  Future<void> _injectPolicy() async {
    setState(() {
      _isLoading = true;
      _log.clear();
    });

    _addLog('=== RootInjector Debug Log ===');
    _addLog('Package: ${widget.app.packageName}');
    _addLog('App: ${widget.app.appName}');

    try {
      // Step 1: get UID
      _addLog('\n[1] Getting UID via pm dump...');
      int? uid;

      final r1 = await Process.run('su', ['-c', 'pm dump ${widget.app.packageName} | grep -m1 "userId="']);
      _addLog('  exit: ${r1.exitCode}');
      _addLog('  stdout: ${r1.stdout.toString().trim()}');
      _addLog('  stderr: ${r1.stderr.toString().trim()}');

      final m1 = RegExp(r'userId=(\d+)').firstMatch(r1.stdout.toString());
      if (m1 != null) {
        uid = int.tryParse(m1.group(1)!);
        _addLog('  parsed uid: $uid');
      }

      if (uid == null) {
        _addLog('\n[1b] Fallback: stat /data/data/${widget.app.packageName}...');
        final r2 = await Process.run('su', ['-c', 'stat -c %u /data/data/${widget.app.packageName}']);
        _addLog('  exit: ${r2.exitCode}');
        _addLog('  stdout: ${r2.stdout.toString().trim()}');
        _addLog('  stderr: ${r2.stderr.toString().trim()}');
        uid = int.tryParse(r2.stdout.toString().trim());
        _addLog('  parsed uid: $uid');
      }

      if (uid == null) {
        throw Exception('Could not resolve UID for ${widget.app.packageName}');
      }

      // Step 2: inject policy
      _addLog('\n[2] Injecting Magisk policy for uid=$uid...');
      final cmd = 'magisk --sqlite "REPLACE INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1);"';
      _addLog('  cmd: $cmd');
      final r3 = await Process.run('su', ['-c', cmd]);
      _addLog('  exit: ${r3.exitCode}');
      _addLog('  stdout: ${r3.stdout.toString().trim()}');
      _addLog('  stderr: ${r3.stderr.toString().trim()}');

      if (r3.exitCode != 0) {
        // Fallback: try sqlite3 directly
        _addLog('\n[2b] Fallback: sqlite3 /data/adb/magisk.db...');
        final cmd2 = 'sqlite3 /data/adb/magisk.db "REPLACE INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1);"';
        final r4 = await Process.run('su', ['-c', cmd2]);
        _addLog('  exit: ${r4.exitCode}');
        _addLog('  stdout: ${r4.stdout.toString().trim()}');
        _addLog('  stderr: ${r4.stderr.toString().trim()}');
        if (r4.exitCode != 0) {
          throw Exception('Injection failed.\nmagisk stderr: ${r3.stderr}\nsqlite3 stderr: ${r4.stderr}');
        }
      }

      _addLog('\n[✓] Success!');
      HapticFeedback.lightImpact();
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('Policy injected for ${widget.app.appName}'),
        backgroundColor: electricPurple,
        behavior: SnackBarBehavior.floating,
      ));
    } catch (e) {
      _addLog('\n[✗] Exception: $e');
      if (!mounted) return;
      _showErrorDialog(_log.toString());
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showErrorDialog(String log) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A1A),
        title: const Text('Injection Failed', style: TextStyle(color: Colors.redAccent)),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                constraints: const BoxConstraints(maxHeight: 320),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.black,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.white12),
                ),
                child: SingleChildScrollView(
                  child: SelectableText(
                    log,
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 11,
                      color: Colors.white70,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.white70,
                  side: const BorderSide(color: Colors.white24),
                ),
                icon: const Icon(Icons.copy, size: 16),
                label: const Text('Copy Log'),
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: log));
                  ScaffoldMessenger.of(ctx).showSnackBar(const SnackBar(
                    content: Text('Log copied to clipboard'),
                    behavior: SnackBarBehavior.floating,
                    duration: Duration(seconds: 2),
                  ));
                },
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Close', style: TextStyle(color: Colors.white54)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('Confirm Action',
              style: Theme.of(context)
                  .textTheme
                  .headlineSmall
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          Text('Inject an "Allow" root policy for:\n${widget.app.appName}?',
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(color: Colors.white70),
              textAlign: TextAlign.center),
          const SizedBox(height: 24),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: electricPurple,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
              minimumSize: const Size(double.infinity, 50),
            ),
            onPressed: _isLoading ? null : _injectPolicy,
            child: _isLoading
                ? const SizedBox(
                    width: 24, height: 24,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Inject Policy'),
          ),
          const SizedBox(height: 8),
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel', style: TextStyle(color: Colors.white70)),
          ),
        ],
      ),
    );
  }
}
