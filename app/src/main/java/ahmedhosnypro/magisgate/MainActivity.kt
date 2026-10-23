package ahmedhosnypro.magisgate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import ahmedhosnypro.magisgate.ui.MagisGateTheme
import ahmedhosnypro.magisgate.ui.screens.AppListScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagisGateTheme {
                AppListScreen(viewModel)
            }
        }
    }
}
