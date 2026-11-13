package com.example.palette

import androidx.compose.runtime.Composable
import com.example.palette.navigation.AppNavigation
import com.example.palette.ui.PaletteTheme

@Composable
fun App() {
    PaletteTheme {
        AppNavigation()
    }
}
