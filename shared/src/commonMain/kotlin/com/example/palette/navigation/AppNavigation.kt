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

enum class Screen {
    HOME, DISCOVER, LOG, ACTIVITY, PROFILE
}

@Composable
fun AppNavigation() {
    val palette = remember { Palette(PaletteStore(), Catalog(), Nav()) }

    // mientras hay sesion se vuelve a preguntar al servidor cada poco: logs de amigos, mensajes, seguidores
    LaunchedEffect(palette.store.signedIn) {
        while (palette.store.signedIn) {
            palette.store.sync()
            delay(15_000)
        }
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Pigment.Paper)) {
            // la app ocupa toda la ventana; a partir de este ancho cambia a la disposicion de pantalla grande
            CompositionLocalProvider(LocalWide provides (maxWidth >= 840.dp)) {
                when (palette.store.stage) {
                    Stage.SIGNED_OUT -> Centered(480.dp) { Box(it) { SignInScreen() } }
                    Stage.LOADING -> LoadingScreen()
                    Stage.NEEDS_PROFILE -> Centered(480.dp) { Box(it) { ProfileSetupScreen() } }
                    Stage.READY -> Shell(palette)
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
private fun Shell(palette: Palette) {
    val nav = palette.nav
    val wide = LocalWide.current
    val backdrop = rememberGraphicsLayer()
    var backdropOrigin by remember { mutableStateOf(Offset.Zero) }
    val top = nav.stack.lastOrNull()

    // boton o gesto de atras del sistema: cierra la pantalla de encima y, si no hay, vuelve a inicio
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = nav.stack.isNotEmpty() || nav.tab != Screen.HOME,
        onBackCompleted = { if (nav.stack.isNotEmpty()) nav.back() else nav.tab = Screen.HOME },
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .onGloballyPositioned { backdropOrigin = it.positionInRoot() }
                .drawWithContent {
                    // se graba lo que se pinta para que la barra de cristal pueda desenfocarlo
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = if (wide) RailSpace else 0.dp),
        ) {
            val page = Modifier.fillMaxSize()
            when (nav.tab) {
                Screen.HOME -> HomeScreen(page)
                Screen.DISCOVER -> DiscoverScreen(page)
                Screen.LOG -> LogScreen(page)
                Screen.ACTIVITY -> Centered(760.dp) { ActivityScreen(it) }
                Screen.PROFILE -> ProfileScreen(modifier = page)
            }
            nav.stack.forEachIndexed { index, dest ->
                key(index, dest) {
                    val over = page.background(Pigment.Paper).pointerInput(Unit) { detectTapGestures { } }
                    when (dest) {
                        is Dest.Detail -> Centered(820.dp, over) { DetailScreen(dest.media, it) }
                        is Dest.Profile -> ProfileScreen(dest.userId, over)
                        is Dest.Chat -> Centered(760.dp, over) { ChatScreen(dest.userId, it) }
                        is Dest.GroupChat -> Centered(760.dp, over) { GroupChatScreen(dest.groupId, it) }
                        is Dest.Story -> StoryScreen(dest.userId, dest.queue, over)
                        Dest.People -> Centered(760.dp, over) { PeopleScreen(it) }
                        Dest.Settings -> Centered(640.dp, over) { SettingsScreen(it) }
                    }
                }
            }
        }

        val fullScreen = top is Dest.Chat || top is Dest.GroupChat || top is Dest.Story
        if (!fullScreen) {
            GlassBar(
                backdrop = backdrop,
                backdropOrigin = backdropOrigin,
                current = nav.tab,
                unread = palette.store.unreadCount,
                onSelect = { nav.switchTo(it) },
                modifier = Modifier.align(if (wide) Alignment.CenterStart else Alignment.BottomCenter),
                vertical = wide,
            )
        }
    }

    nav.reviewing?.let { ReviewSheet(it) }
    nav.sending?.let { SendSheet(it) }
    nav.logMenu?.let { LogMenuSheet(it) }
    if (nav.creatingGroup) NewGroupSheet()
    nav.groupInfo?.let { GroupInfoSheet(it) }
}

fun getIconForScreen(screen: Screen): ImageVector {
    return when (screen) {
        Screen.HOME -> LucideHouse
        Screen.DISCOVER -> LucideBrush
        Screen.LOG -> LucideCirclePlus
        Screen.ACTIVITY -> LucideMessageCircle
        Screen.PROFILE -> LucideCircleUser
    }
}
