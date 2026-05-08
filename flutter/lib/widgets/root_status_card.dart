import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/app_providers.dart';

class RootStatusCard extends ConsumerWidget {
  const RootStatusCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ref.watch(rootCheckProvider).when(
      data: (isRooted) => isRooted
          ? const SizedBox.shrink()
          : _card(context,
              icon: Icons.error_outline,
              color: Colors.orangeAccent,
              text: 'Root Not Detected. Functionality will be limited.'),
      error: (_, __) => _card(context,
          icon: Icons.error,
          color: Colors.redAccent,
          text: 'Root check failed. Please restart the app.'),
      loading: () => _card(context,
          iconWidget: const SizedBox(
            height: 16, width: 16,
            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
          ),
          color: Colors.grey,
          text: 'Verifying Root Access...'),
    );
  }

  Widget _card(BuildContext context,
      {IconData? icon, Widget? iconWidget, required Color color, required String text}) {
    return Container(
      margin: const EdgeInsets.all(12),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color),
      ),
      child: Row(children: [
        iconWidget ?? Icon(icon, color: color, size: 20),
        const SizedBox(width: 12),
        Expanded(
          child: Text(text,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Colors.white)),
        ),
      ]),
    );
  }
}
