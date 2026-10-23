package ahmedhosnypro.magisgate.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search apps...", color = Color.White.copy(alpha = 0.54f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.54f)) },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onFilterClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.1f)),
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color.White.copy(alpha = 0.7f))
        }
    }
}
