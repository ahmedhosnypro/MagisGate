package ahmedhosnypro.magisgate.ui.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ahmedhosnypro.magisgate.model.AppInfo
import ahmedhosnypro.magisgate.ui.ElectricPurple
import kotlinx.coroutines.launch

@Composable
fun ConfirmationSheet(
    app: AppInfo,
    onInject: suspend () -> Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isLoading = remember { mutableStateOf(value = false) }
    val errorLog = remember { mutableStateOf<String?>(value = null) }
    val clipboard = LocalClipboard.current

    errorLog.value?.let { log ->
        AlertDialog(
            onDismissRequest = { errorLog.value = null },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Injection Failed", color = Color.Red.copy(alpha = 0.8f)) },
            text = {
                Column {
                    Box(
                        Modifier
                            .heightIn(max = 320.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                log,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val clip = ClipData.newPlainText("log", log).toClipEntry()
                                clipboard.setClipEntry(clip)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy Log")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { errorLog.value = null }) { Text("Close", color = Color.White.copy(alpha = 0.54f)) }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            "Confirm Action",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Inject an \"Allow\" root policy for:\n${app.appName}?",
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.7f)),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    isLoading.value = true
                    val success = onInject()
                    isLoading.value = false
                    if (success) onDismiss()
                    // error log is populated via ViewModel logs; show dialog if needed
                }
            },
            enabled = !isLoading.value,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Inject Policy")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

