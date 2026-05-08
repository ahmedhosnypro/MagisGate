package com.example.rootinjector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.rootinjector.ui.RootInjectorTheme
import com.example.rootinjector.ui.screens.AppListScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RootInjectorTheme {
                AppListScreen(viewModel)
            }
        }
    }
}
