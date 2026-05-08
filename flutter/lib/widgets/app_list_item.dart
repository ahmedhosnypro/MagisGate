import 'package:flutter/material.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/app_providers.dart';
import 'confirmation_sheet.dart';

const Color electricPurple = Color(0xFF8F00FF);
const Color darkGlass = Colors.white10;

class AppListItem extends ConsumerWidget {
  final Application app;
  const AppListItem({super.key, required this.app});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return GestureDetector(
      onTap: () {
        final isRooted = ref.read(rootCheckProvider).asData?.value ?? false;
        if (isRooted) {
          showModalBottomSheet(
            context: context,
            isScrollControlled: true,
            builder: (ctx) => Padding(
              padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom),
              child: ConfirmationSheet(app: app),
            ),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Action requires root access.'),
            backgroundColor: Colors.orangeAccent,
            behavior: SnackBarBehavior.floating,
          ));
        }
      },
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: darkGlass,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: Colors.white12, width: 0.5),
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(children: [
              if (app is ApplicationWithIcon)
                Image.memory((app as ApplicationWithIcon).icon, width: 40, height: 40),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(app.appName,
                        style: Theme.of(context)
                            .textTheme
                            .titleMedium
                            ?.copyWith(fontWeight: FontWeight.bold),
                        overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 2),
                    Text(app.packageName,
                        style: Theme.of(context)
                            .textTheme
                            .bodySmall
                            ?.copyWith(color: Colors.white54),
                        overflow: TextOverflow.ellipsis),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: electricPurple),
            ]),
          ),
        ),
      ),
    );
  }
}
