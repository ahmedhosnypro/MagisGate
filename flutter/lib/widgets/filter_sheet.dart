import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/filter_sort.dart';
import '../providers/app_providers.dart';

class FilterSheet extends ConsumerWidget {
  const FilterSheet({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final fs = ref.watch(filterSortProvider);

    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 40, height: 4,
              decoration: BoxDecoration(
                color: Colors.white24,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),
          _Section(title: 'Filter', children: [
            _ChipRow<AppFilter>(
              values: AppFilter.values,
              selected: fs.filter,
              label: _filterLabel,
              onSelected: (v) => ref.read(filterSortProvider.notifier)
                  .update(fs.copyWith(filter: v)),
            ),
          ]),
          const SizedBox(height: 12),
          _Section(title: 'Sort By', children: [
            _ChipRow<SortBy>(
              values: SortBy.values,
              selected: fs.sortBy,
              label: _sortByLabel,
              onSelected: (v) => ref.read(filterSortProvider.notifier)
                  .update(fs.copyWith(sortBy: v)),
            ),
          ]),
          const SizedBox(height: 12),
          _Section(title: 'Order', children: [
            _ChipRow<SortOrder>(
              values: SortOrder.values,
              selected: fs.sortOrder,
              label: _sortOrderLabel,
              onSelected: (v) => ref.read(filterSortProvider.notifier)
                  .update(fs.copyWith(sortOrder: v)),
            ),
          ]),
        ],
      ),
    );
  }

  static String _filterLabel(AppFilter f) => switch (f) {
    AppFilter.all => 'All',
    AppFilter.system => 'System',
    AppFilter.user => 'User',
    AppFilter.enabled => 'Enabled',
    AppFilter.disabled => 'Disabled',
  };

  static String _sortByLabel(SortBy s) => switch (s) {
    SortBy.name => 'Name',
    SortBy.packageName => 'Package',
    SortBy.appSize => 'Size',
    SortBy.installDate => 'Installed',
    SortBy.updateDate => 'Updated',
  };

  static String _sortOrderLabel(SortOrder o) => switch (o) {
    SortOrder.ascending => '↑ Ascending',
    SortOrder.descending => '↓ Descending',
  };
}

class _Section extends StatelessWidget {
  final String title;
  final List<Widget> children;
  const _Section({required this.title, required this.children});

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(title,
          style: Theme.of(context)
              .textTheme
              .labelLarge
              ?.copyWith(color: Colors.white54)),
      const SizedBox(height: 8),
      ...children,
    ],
  );
}

class _ChipRow<T> extends StatelessWidget {
  final List<T> values;
  final T selected;
  final String Function(T) label;
  final void Function(T) onSelected;

  const _ChipRow({
    required this.values,
    required this.selected,
    required this.label,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) => Wrap(
    spacing: 8,
    runSpacing: 4,
    children: values.map((v) {
      final isSelected = v == selected;
      return ChoiceChip(
        label: Text(label(v)),
        selected: isSelected,
        onSelected: (_) => onSelected(v),
        selectedColor: const Color(0xFF8F00FF),
        backgroundColor: Colors.white10,
        labelStyle: TextStyle(
          color: isSelected ? Colors.white : Colors.white70,
          fontSize: 12,
        ),
        side: BorderSide.none,
        padding: const EdgeInsets.symmetric(horizontal: 4),
      );
    }).toList(),
  );
}
