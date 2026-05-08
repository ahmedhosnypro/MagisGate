package com.example.rootinjector.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.rootinjector.model.AppInfo
import com.example.rootinjector.ui.ElectricPurple

@Composable
fun AppListItem(app: AppInfo, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape)
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.1f),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            app.icon?.let {
                val bmp = it.toBitmap(40, 40).asImageBitmap()
                androidx.compose.foundation.Image(
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
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ElectricPurple,
            )
        }
    }
}
