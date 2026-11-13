package com.example.palette.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.palette.data.LogEntry
import com.example.palette.data.Media
import com.example.palette.data.PaletteStore
import com.example.palette.data.ThemeColors
import com.example.palette.data.remote.Catalog
import com.example.palette.ui.Pigment

/** pantallas que se abren por encima de las pestañas. */
sealed interface Dest {
    data class Detail(val media: Media) : Dest
    data class Profile(val userId: String) : Dest
    data class Chat(val userId: String) : Dest
    data class GroupChat(val groupId: String) : Dest
    /** [queue] son las historias en el orden en que estaban en inicio, para pasar de una persona a la siguiente. */
    data class Story(val userId: String, val queue: List<String> = listOf(userId)) : Dest
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
    var tab by mutableStateOf(Screen.HOME)
    val stack = mutableStateListOf<Dest>()

    // hojas inferiores
    var reviewing by mutableStateOf<Media?>(null)
    var sending by mutableStateOf<Outgoing?>(null)
    var logMenu by mutableStateOf<LogEntry?>(null)
    var creatingGroup by mutableStateOf(false)
    var groupInfo by mutableStateOf<String?>(null)

    fun open(dest: Dest) {
        if (stack.lastOrNull() != dest) stack.add(dest)
    }

    /** cambia la pantalla de encima por otra sin pasar por la de debajo. */
    fun replaceTop(dest: Dest) {
        if (stack.isEmpty()) stack.add(dest) else stack[stack.lastIndex] = dest
    }

    fun back() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun switchTo(screen: Screen) {
        stack.clear()
        tab = screen
    }
}

class Palette(val store: PaletteStore, val catalog: Catalog, val nav: Nav) {
    val discover = DiscoverCache()

    init {
        Pigment.colors = store.data.theme
    }

    /** cambia los colores de la app al momento y los deja guardados. */
    fun setTheme(theme: ThemeColors) {
        Pigment.colors = theme
        store.setTheme(theme)
    }
}

/** las recomendaciones tardan en calcularse: se guardan mientras no cambien los logs de los que salen. */
class DiscoverCache {
    var key by mutableStateOf<String?>(null)
    var rows by mutableStateOf<List<Pair<String, List<Media>>>>(emptyList())
}

val LocalPalette = staticCompositionLocalOf<Palette> { error("Palette no está disponible fuera de AppNavigation") }

/** true cuando la ventana es ancha (escritorio, web, tablet o movil tumbado): la barra pasa al lateral. */
val LocalWide = compositionLocalOf { false }

/** hueco que hay que dejar al final de las listas para que la barra flotante no tape nada. */
val BarSpace: Dp
    @Composable get() = if (LocalWide.current) 28.dp else 104.dp

/** ancho que ocupa la barra cuando va en el lateral. */
val RailSpace = 108.dp
