package com.example.palette.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.palette.data.PaletteStore
import com.example.palette.data.Stage
import com.example.palette.data.remote.Catalog
import com.example.palette.screens.ActivityScreen
import com.example.palette.screens.ChatScreen
import com.example.palette.screens.DetailScreen
import com.example.palette.screens.DiscoverScreen
import com.example.palette.screens.GroupChatScreen
import com.example.palette.screens.GroupInfoSheet
import com.example.palette.screens.HomeScreen
import com.example.palette.screens.NewGroupSheet
import com.example.palette.screens.LogMenuSheet
import com.example.palette.screens.LogScreen
import com.example.palette.screens.PeopleScreen
import com.example.palette.screens.ProfileScreen
import com.example.palette.screens.ReviewSheet
import com.example.palette.screens.SendSheet
import com.example.palette.screens.SettingsScreen
import com.example.palette.screens.StoryScreen
import com.example.palette.screens.LoadingScreen
import com.example.palette.screens.ProfileSetupScreen
import com.example.palette.screens.SignInScreen
import com.example.palette.ui.Pigment
import kotlinx.coroutines.delay

// la navegacion no usa ninguna libreria: es estado de compose.
//  - navegacion.apartado dice que pestaña se ve
//  - navegacion.pila es una lista de pantallas abiertas encima (ficha, chat, perfil de otro...)
// compose vuelve a pintar solo lo que lee esas variables cuando cambian, asi que "navegar"
// es simplemente cambiar su valor.

enum class Screen {
    HOME, DISCOVER, LOG, ACTIVITY, PROFILE
}

@Composable
fun AppNavigation() {
    val app = remember { Palette(PaletteStore(), Catalog(), Nav()) }

    // mientras hay sesion se vuelve a preguntar al servidor cada poco: logs de amigos, mensajes, seguidores
    LaunchedEffect(app.almacen.conSesion) {
        while (app.almacen.conSesion) {
            app.almacen.sincronizar()
            delay(15_000)
        }
    }

    CompositionLocalProvider(LocalPalette provides app) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Pigment.Paper)) {
            // la app ocupa toda la ventana; a partir de este ancho cambia a la disposicion de pantalla grande
            CompositionLocalProvider(LocalWide provides (maxWidth >= 840.dp)) {
                when (app.almacen.etapa) {
                    Stage.SIGNED_OUT -> Centered(480.dp) { Box(it) { SignInScreen() } }
                    Stage.LOADING -> LoadingScreen()
                    Stage.NEEDS_PROFILE -> Centered(480.dp) { Box(it) { ProfileSetupScreen() } }
                    Stage.READY -> Shell(app)
                }
            }
        }
    }
}

/** para formularios y lecturas: en pantallas anchas se quedan en una columna centrada en vez de estirarse. */
@Composable
private fun Centered(max: Dp, modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        content(Modifier.widthIn(max = max).fillMaxSize())
    }
}

@Composable
private fun Shell(app: Palette) {
    val navegacion = app.navegacion
    val ancha = LocalWide.current
    val fondo = rememberGraphicsLayer()
    var origenFondo by remember { mutableStateOf(Offset.Zero) }
    val top = navegacion.pila.lastOrNull()

    // boton o gesto de atras del sistema: cierra la pantalla de encima y, si no hay, vuelve a inicio
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = navegacion.pila.isNotEmpty() || navegacion.apartado != Screen.HOME,
        onBackCompleted = { if (navegacion.pila.isNotEmpty()) navegacion.volver() else navegacion.apartado = Screen.HOME },
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .onGloballyPositioned { origenFondo = it.positionInRoot() }
                .drawWithContent {
                    // se graba lo que se pinta para que la barra de cristal pueda desenfocarlo
                    fondo.record { this@drawWithContent.drawContent() }
                    drawLayer(fondo)
                }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = if (ancha) RailSpace else 0.dp),
        ) {
            val pagina = Modifier.fillMaxSize()
            when (navegacion.apartado) {
                Screen.HOME -> HomeScreen(pagina)
                Screen.DISCOVER -> DiscoverScreen(pagina)
                Screen.LOG -> LogScreen(pagina)
                Screen.ACTIVITY -> Centered(760.dp) { ActivityScreen(it) }
                Screen.PROFILE -> ProfileScreen(modifier = pagina)
            }
            navegacion.pila.forEachIndexed { index, destino ->
                key(index, destino) {
                    val encima = pagina.background(Pigment.Paper).pointerInput(Unit) { detectTapGestures { } }
                    when (destino) {
                        is Dest.Detail -> Centered(820.dp, encima) { DetailScreen(destino.media, it) }
                        is Dest.Profile -> ProfileScreen(destino.userId, encima)
                        is Dest.Chat -> Centered(760.dp, encima) { ChatScreen(destino.userId, it) }
                        is Dest.GroupChat -> Centered(760.dp, encima) { GroupChatScreen(destino.groupId, it) }
                        is Dest.Story -> StoryScreen(destino.userId, destino.cola, encima)
                        Dest.People -> Centered(760.dp, encima) { PeopleScreen(it) }
                        Dest.Settings -> Centered(640.dp, encima) { SettingsScreen(it) }
                    }
                }
            }
        }

        val pantallaCompleta = top is Dest.Chat || top is Dest.GroupChat || top is Dest.Story
        if (!pantallaCompleta) {
            GlassBar(
                fondo = fondo,
                origenFondo = origenFondo,
                current = navegacion.apartado,
                unread = app.almacen.sinLeer,
                alElegirPantalla = { navegacion.irA(it) },
                modifier = Modifier.align(if (ancha) Alignment.CenterStart else Alignment.BottomCenter),
                vertical = ancha,
            )
        }
    }

    navegacion.enResena?.let { ReviewSheet(it) }
    navegacion.enviando?.let { SendSheet(it) }
    navegacion.menuDeLog?.let { LogMenuSheet(it) }
    if (navegacion.creandoGrupo) NewGroupSheet()
    navegacion.infoDeGrupo?.let { GroupInfoSheet(it) }
}

fun getIconForScreen(pantalla: Screen): ImageVector {
    return when (pantalla) {
        Screen.HOME -> LucideHouse
        Screen.DISCOVER -> LucideBrush
        Screen.LOG -> LucideCirclePlus
        Screen.ACTIVITY -> LucideMessageCircle
        Screen.PROFILE -> LucideCircleUser
    }
}
