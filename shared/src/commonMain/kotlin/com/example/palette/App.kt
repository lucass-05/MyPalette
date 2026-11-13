package com.example.palette

import androidx.compose.runtime.Composable
import com.example.palette.navigation.AppNavigation
import com.example.palette.ui.PaletteTheme

// punto de entrada comun a todas las plataformas. android, ios, escritorio y web solo
// crean su ventana y llaman a App(); todo lo demas esta escrito una sola vez aqui, en commonMain.

@Composable
fun App() {
    PaletteTheme {
        AppNavigation()
    }
}
