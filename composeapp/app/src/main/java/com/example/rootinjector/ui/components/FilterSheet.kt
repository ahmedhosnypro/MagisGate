package com.example.rootinjector.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.rootinjector.model.AppFilter
import com.example.rootinjector.model.FilterSortState
import com.example.rootinjector.model.SortBy
import com.example.rootinjector.model.SortOrder
import com.example.rootinjector.ui.ElectricPurple

@Composable
fun FilterSheet(state: FilterSortState, onUpdate: (FilterSortState) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
    ) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
                .let {
                    it // drawn via Surface below
                },
        )
        Surface(
            modifier = Modifier.width(40.dp).height(4.dp).align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(2.dp),
            color = Color.White.copy(alpha = 0.24f),
        ) {}
        Spacer(Modifier.height(16.dp))

        FilterSection(title = "Filter") {
            ChipRow(
                values = AppFilter.entries,
                selected = state.filter,
                label = { it.label() },
                onSelected = { onUpdate(state.copy(filter = it)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        FilterSection(title = "Sort By") {
            ChipRow(
                values = SortBy.entries,
                selected = state.sortBy,
                label = { it.label() },
                onSelected = { onUpdate(state.copy(sortBy = it)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        FilterSection(title = "Order") {
            ChipRow(
                values = SortOrder.entries,
                selected = state.sortOrder,
                label = { it.label() },
                onSelected = { onUpdate(state.copy(sortOrder = it)) },
            )
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(alpha = 0.54f)))
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            val isSelected = v == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(v) },
                label = { Text(label(v), style = MaterialTheme.typography.bodySmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricPurple,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.1f),
                    labelColor = Color.White.copy(alpha = 0.7f),
                ),
                border = null,
            )
        }
    }
}

private fun AppFilter.label() = when (this) {
    AppFilter.ALL -> "All"
    AppFilter.SYSTEM -> "System"
    AppFilter.USER -> "User"
    AppFilter.ENABLED -> "Enabled"
    AppFilter.DISABLED -> "Disabled"
}

private fun SortBy.label() = when (this) {
    SortBy.NAME -> "Name"
    SortBy.PACKAGE_NAME -> "Package"
    SortBy.APP_SIZE -> "Size"
    SortBy.INSTALL_DATE -> "Installed"
    SortBy.UPDATE_DATE -> "Updated"
}

private fun SortOrder.label() = when (this) {
    SortOrder.ASCENDING -> "↑ Ascending"
    SortOrder.DESCENDING -> "↓ Descending"
}
