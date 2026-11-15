package com.example.palette

import com.example.palette.navigation.Palette


import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Palette",
        // arranca ocupando toda la pantalla
        state = rememberWindowState(placement = WindowPlacement.Maximized),
    ) {
        App()
    }
}
