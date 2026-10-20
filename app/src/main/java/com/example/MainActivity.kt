package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ModelManagerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceChatScreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberBackground
                ) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.CHAT -> ChatScreen(viewModel = viewModel)
            AppScreen.VOICE_CHAT -> VoiceChatScreen(viewModel = viewModel)
            AppScreen.MODEL_MANAGER -> ModelManagerScreen(viewModel = viewModel)
            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
        }
    }
}
