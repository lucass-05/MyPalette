package com.example.palette

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.palette.core.ui.PaletteTheme
import com.example.palette.di.AppContainer
import com.example.palette.di.DefaultAppContainer
import com.example.palette.di.LocalAppContainer
import com.example.palette.navigation.AppNavigation

// punto de entrada comun a todas las plataformas. android, ios, escritorio y web solo
// crean su ventana y llaman a App(); todo lo demas esta escrito una sola vez aqui, en commonMain.

// el contenedor de dependencias se crea una sola vez, la primera vez que se pide, y dura lo que dure la app.
// va fuera de App() para que siga siendo el mismo aunque android vuelva a crear la pantalla (al girar el movil, por ejemplo)
private val contenedor: AppContainer by lazy { DefaultAppContainer() }

@Composable
fun App() {
    PaletteTheme {
        CompositionLocalProvider(LocalAppContainer provides contenedor) {
            AppNavigation()
        }
    }
}
