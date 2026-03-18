enum AppFilter { all, system, user, enabled, disabled }

enum SortBy { name, packageName, appSize, installDate, updateDate }

enum SortOrder { ascending, descending }

class FilterSortState {
  final AppFilter filter;
  final SortBy sortBy;
  final SortOrder sortOrder;

  const FilterSortState({
    this.filter = AppFilter.all,
    this.sortBy = SortBy.name,
    this.sortOrder = SortOrder.ascending,
  });

  FilterSortState copyWith({
    AppFilter? filter,
    SortBy? sortBy,
    SortOrder? sortOrder,
  }) => FilterSortState(
    filter: filter ?? this.filter,
    sortBy: sortBy ?? this.sortBy,
    sortOrder: sortOrder ?? this.sortOrder,
  );
}
