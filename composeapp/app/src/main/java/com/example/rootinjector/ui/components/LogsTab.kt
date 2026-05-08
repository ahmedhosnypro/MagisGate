package com.example.rootinjector.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rootinjector.model.LogEntry

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogsTab(logs: List<LogEntry>, onClear: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${logs.size} entries", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.38f)))
            Spacer(Modifier.weight(1f))
            if (logs.isNotEmpty()) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(logs.joinToString("\n") { it.formatted }))
                }) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy All", fontSize = 12.sp)
                }
                TextButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No logs yet.", color = Color.White.copy(alpha = 0.38f))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                items(logs.reversed()) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { clipboard.setText(AnnotatedString(entry.formatted)) },
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            entry.formatted,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(entry.formatted)) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.24f))
                        }
                    }
                }
            }
        }
    }
}
