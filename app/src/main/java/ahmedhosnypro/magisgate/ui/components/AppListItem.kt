package ahmedhosnypro.magisgate.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import ahmedhosnypro.magisgate.model.AppInfo
import ahmedhosnypro.magisgate.ui.ElectricPurple

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val backgroundColor = if (isSelected) {
        ElectricPurple.copy(alpha = 0.2f)
    } else {
        Color.White.copy(alpha = 0.1f)
    }
    val borderColor = if (isSelected) {
        ElectricPurple.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
            .let {
                if (onLongClick != null) {
                    it.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    it.clickable(onClick = onClick)
                }
            },
        color = backgroundColor,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            app.icon?.let {
                val bmp = it.toBitmap(40, 40).asImageBitmap()
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.54f)),
                    maxLines = 1,
                )
            }
            if (trailingIcon != null) {
                trailingIcon()
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ElectricPurple,
                )
            }
        }
    }
}
