package com.example.palette.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.features.messages.domain.model.Outgoing
import kotlinx.serialization.Serializable

// cada pantalla tiene su ruta: un objeto o una clase marcada con @Serializable.
// las que necesitan saber que enseñar llevan solo el id (del contenido, de la persona, del grupo);
// con ese id la pantalla pide el resto a su repositorio.

// las cinco pestañas de la barra
@Serializable data object HomeRoute
@Serializable data object DiscoverRoute
@Serializable data object LogRoute
@Serializable data object ActivityRoute
@Serializable data object MyProfileRoute

// pantallas que se abren encima
@Serializable data class DetailRoute(val mediaId: String)
@Serializable data class ProfileRoute(val userId: String)
@Serializable data class ChatRoute(val userId: String)
@Serializable data class GroupChatRoute(val groupId: String)
// cola son los ids de las historias en el orden en que estaban en inicio, separados por comas,
// para pasar de una persona a la siguiente
@Serializable data class StoryRoute(val userId: String, val cola: String)
@Serializable data object PeopleRoute
@Serializable data object SettingsRoute
// una reseña con sus me gusta y comentarios
@Serializable data class ReviewRoute(val logId: String)
// una lista; abrirSelector abre el buscador nada mas entrar (al crearla, que esta vacia)
@Serializable data class ListRoute(val listId: String, val abrirSelector: Boolean = false)
@Serializable data object CustomizeRoute

enum class Screen(val route: Any) {
    HOME(HomeRoute),
    DISCOVER(DiscoverRoute),
    LOG(LogRoute),
    ACTIVITY(ActivityRoute),
    PROFILE(MyProfileRoute),
}

/**
 * las hojas inferiores no son pantallas con ruta: se abren encima de la que haya.
 * cada variable dice si su hoja esta abierta y con que.
 */
class Hojas {
    var enResena by mutableStateOf<Media?>(null)
    var enviando by mutableStateOf<Outgoing?>(null)
    var menuDeLog by mutableStateOf<LogEntry?>(null)
    var creandoGrupo by mutableStateOf(false)
    var infoDeGrupo by mutableStateOf<String?>(null)
    var creandoLista by mutableStateOf(false)
}

/** true cuando la ventana es ancha (escritorio, web, tablet o movil tumbado): la barra pasa al lateral. */
val LocalWide = compositionLocalOf { false }

/** hueco que hay que dejar al final de las listas para que la barra flotante no tape nada. */
val BarSpace: Dp
    @Composable get() = if (LocalWide.current) 28.dp else 104.dp

/** ancho que ocupa la barra cuando va en el lateral. */
val RailSpace = 108.dp
