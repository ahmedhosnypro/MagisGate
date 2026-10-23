package ahmedhosnypro.magisgate.model

enum class AppFilter { ALL, SYSTEM, USER, ENABLED, DISABLED }
enum class SortBy { NAME, PACKAGE_NAME, APP_SIZE, INSTALL_DATE, UPDATE_DATE }
enum class SortOrder { ASCENDING, DESCENDING }

data class FilterSortState(
    val filter: AppFilter = AppFilter.ALL,
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
)
