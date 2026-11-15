package com.example.palette.navigation

import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.ThemeColors
import com.example.palette.core.ui.Pigment
import com.example.palette.di.AppContainer
import com.example.palette.features.catalog.domain.repository.CatalogRepository


import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// mutableStateOf y mutableStateListOf son la base de compose: guardan un valor y avisan
// a quien lo haya leido para que se repinte. "by" permite usarlos como variables normales.

/** pantallas que se abren por encima de las pestañas. */
sealed interface Dest {
    data class Detail(val media: Media) : Dest
    data class Profile(val userId: String) : Dest
    data class Chat(val userId: String) : Dest
    data class GroupChat(val groupId: String) : Dest
    /** [queue] son las historias en el orden en que estaban en inicio, para pasar de una persona a la siguiente. */
    data class Story(val userId: String, val cola: List<String> = listOf(userId)) : Dest
    data object People : Dest
    data object Settings : Dest
}

/** lo que se esta mandando a un amigo. con [Nothing] solo se elige con quien abrir un chat. */
sealed interface Outgoing {
    data class Content(val media: Media) : Outgoing
    data class Review(val log: LogEntry) : Outgoing
    data object Nothing : Outgoing
}

class Nav {
    var apartado by mutableStateOf(Screen.HOME)
    val pila = mutableStateListOf<Dest>()

    // hojas inferiores
    var enResena by mutableStateOf<Media?>(null)
    var enviando by mutableStateOf<Outgoing?>(null)
    var menuDeLog by mutableStateOf<LogEntry?>(null)
    var creandoGrupo by mutableStateOf(false)
    var infoDeGrupo by mutableStateOf<String?>(null)

    fun abrir(destino: Dest) {
        if (pila.lastOrNull() != destino) pila.add(destino)
    }

    /** cambia la pantalla de encima por otra sin pasar por la de debajo. */
    fun sustituirCima(destino: Dest) {
        if (pila.isEmpty()) pila.add(destino) else pila[pila.lastIndex] = destino
    }

    fun volver() {
        if (pila.isNotEmpty()) pila.removeAt(pila.lastIndex)
    }

    fun irA(pantalla: Screen) {
        pila.clear()
        apartado = pantalla
    }
}

class Palette(val container: AppContainer, val navegacion: Nav) {
    val almacen: PaletteStore get() = container.almacen
    val catalogo: CatalogRepository get() = container.catalogRepository

    val descubrir = DiscoverCache()

    init {
        Pigment.colors = almacen.estado.theme
    }

    /** cambia los colores de la app al momento y los deja guardados. */
    fun ponerTema(theme: ThemeColors) {
        Pigment.colors = theme
        almacen.ponerTema(theme)
    }
}

/** las recomendaciones tardan en calcularse: se guardan mientras no cambien los logs de los que salen. */
class DiscoverCache {
    var key by mutableStateOf<String?>(null)
    var filas by mutableStateOf<List<Pair<String, List<Media>>>>(emptyList())
}

val LocalPalette = staticCompositionLocalOf<Palette> { error("Palette no está disponible fuera de AppNavigation") }

/** true cuando la ventana es ancha (escritorio, web, tablet o movil tumbado): la barra pasa al lateral. */
val LocalWide = compositionLocalOf { false }

/** hueco que hay que dejar al final de las listas para que la barra flotante no tape nada. */
val BarSpace: Dp
    @Composable get() = if (LocalWide.current) 28.dp else 104.dp

/** ancho que ocupa la barra cuando va en el lateral. */
val RailSpace = 108.dp
