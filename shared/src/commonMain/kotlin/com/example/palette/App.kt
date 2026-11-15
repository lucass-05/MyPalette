package com.example.palette

import com.example.palette.core.ui.PaletteTheme
import com.example.palette.navigation.AppNavigation


import androidx.compose.runtime.Composable

// punto de entrada comun a todas las plataformas. android, ios, escritorio y web solo
// crean su ventana y llaman a App(); todo lo demas esta escrito una sola vez aqui, en commonMain.

@Composable
fun App() {
    PaletteTheme {
        AppNavigation()
    }
}
