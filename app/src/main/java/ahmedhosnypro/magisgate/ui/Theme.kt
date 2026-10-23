package ahmedhosnypro.magisgate.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ElectricPurple = Color(0xFF8F00FF)
val TrueBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF1A1A1A)

private val DarkColors = darkColorScheme(
    primary = ElectricPurple,
    background = TrueBlack,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun MagisGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
