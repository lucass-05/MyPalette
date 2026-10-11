package com.example.palette.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.palette.screens.ActivityScreen
import com.example.palette.screens.DiscoverScreen
import com.example.palette.screens.HomeScreen
import com.example.palette.screens.LogScreen
import com.example.palette.screens.ProfileScreen

enum class Screen {
    HOME, DISCOVER, LOG, ACTIVITY, PROFILE
}

@Composable
fun AppNavigation() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) } // se inicia en Home

    // barra de abajo con iconos
    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Text(getIconForScreen(screen)) }, // emojis temporales TODO poner iconos
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            Screen.HOME -> HomeScreen(modifier = Modifier.padding(paddingValues).fillMaxSize())
            Screen.DISCOVER -> DiscoverScreen(modifier = Modifier.padding(paddingValues).fillMaxSize())
            Screen.LOG -> LogScreen(modifier = Modifier.padding(paddingValues).fillMaxSize())
            Screen.ACTIVITY -> ActivityScreen(modifier = Modifier.padding(paddingValues).fillMaxSize())
            Screen.PROFILE -> ProfileScreen(modifier = Modifier.padding(paddingValues).fillMaxSize())
        }
    }
}

fun getIconForScreen(screen: Screen): String {
    return when (screen) {
        Screen.HOME -> "⌂"
        Screen.DISCOVER -> "🖌️"
        Screen.LOG -> "➕"
        Screen.ACTIVITY -> "🔔"
        Screen.PROFILE -> "👤"
    }
}