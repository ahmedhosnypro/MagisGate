import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/app_providers.dart';
import 'filter_sheet.dart';

class SearchBarRow extends ConsumerWidget {
  const SearchBarRow({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Row(
      children: [
        Expanded(
          child: TextField(
            onChanged: (v) => ref.read(searchQueryProvider.notifier).setQuery(v),
            decoration: const InputDecoration(
              hintText: 'Search apps...',
              prefixIcon: Icon(Icons.search, color: Colors.white54),
            ),
          ),
        ),
        const SizedBox(width: 8),
        _FilterButton(),
      ],
    );
  }
}

class _FilterButton extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return IconButton(
      style: IconButton.styleFrom(
        backgroundColor: Colors.white10,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
        padding: const EdgeInsets.all(12),
      ),
      icon: const Icon(Icons.tune, color: Colors.white70),
      onPressed: () => showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        builder: (_) => const FilterSheet(),
      ),
    );
  }
}
