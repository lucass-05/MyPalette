package com.example.palette.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.palette.core.data.Stage
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.ui.ConViewModelsPropios
import com.example.palette.core.ui.Pigment
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.auth.ui.LoadingScreen
import com.example.palette.features.auth.ui.ProfileSetupScreenRoot
import com.example.palette.features.auth.ui.SignInScreenRoot
import com.example.palette.features.catalog.ui.DetailScreenRoot
import com.example.palette.features.catalog.ui.DiscoverScreenRoot
import com.example.palette.features.catalog.ui.LogScreenRoot
import com.example.palette.features.home.ui.HomeScreenRoot
import com.example.palette.features.home.ui.StoryScreenRoot
import com.example.palette.features.library.ui.LogMenuSheetRoot
import com.example.palette.features.library.ui.ReviewSheetRoot
import com.example.palette.features.messages.domain.model.Outgoing
import com.example.palette.features.messages.ui.ActivityScreenRoot
import com.example.palette.features.messages.ui.ChatScreenRoot
import com.example.palette.features.messages.ui.GroupChatScreenRoot
import com.example.palette.features.messages.ui.GroupInfoSheetRoot
import com.example.palette.features.messages.ui.NewGroupSheetRoot
import com.example.palette.features.messages.ui.SendSheetRoot
import com.example.palette.features.people.ui.PeopleScreenRoot
import com.example.palette.features.profile.ui.ProfileScreenRoot
import com.example.palette.features.profile.ui.SettingsScreenRoot
import com.example.palette.features.profile.ui.CustomizeScreenRoot
import com.example.palette.features.lists.ui.ListScreenRoot
import com.example.palette.features.lists.ui.NewListSheetRoot
import com.example.palette.features.reviews.ui.ReviewDetailScreenRoot

// la navegacion usa la libreria navigation de compose:
//  - cada pantalla tiene una ruta (Routes.kt)
//  - el NavController lleva la pila de pantallas abiertas: navigate() apila una y popBackStack() vuelve atras
//  - el NavHost dice que composable se pinta para cada ruta
// las pantallas no conocen el NavController. cada root recibe funciones (onBack, onOpenMedia...)
// y es aqui, en un solo sitio, donde se decide a que ruta lleva cada una.

@Composable
fun AppNavigation() {
    val container = LocalAppContainer.current
    val viewModel = viewModel { AppViewModel(container.authRepository, container.messagesRepository, container.profileRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BoxWithConstraints(Modifier.fillMaxSize().background(Pigment.Paper)) {
        // la app ocupa toda la ventana; a partir de este ancho cambia a la disposicion de pantalla grande
        CompositionLocalProvider(LocalWide provides (maxWidth >= 840.dp)) {
            // segun la etapa de la sesion se enseña la entrada, la creacion del perfil o la app.
            // no hace falta navegar entre ellas: al cambiar la etapa cambia sola la rama que se pinta
            when (state.etapa) {
                Stage.SIGNED_OUT -> ConViewModelsPropios { Centered(480.dp) { Box(it) { SignInScreenRoot() } } }
                Stage.LOADING -> LoadingScreen()
                Stage.NEEDS_PROFILE -> ConViewModelsPropios { Centered(480.dp) { Box(it) { ProfileSetupScreenRoot() } } }
                Stage.READY -> Shell(state.sinLeer)
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

/** apila una pantalla. dos toques seguidos en el mismo sitio no la abren dos veces. */
private inline fun <reified T : Any> NavController.abrir(ruta: T) {
    val encima = currentBackStackEntry
    if (encima?.destination?.hasRoute<T>() == true && encima.toRoute<T>() == ruta) return
    navigate(ruta)
}

/** la pestaña a la que corresponde un destino, o null si es una pantalla de las que se abren encima. */
private fun NavDestination.pestana(): Screen? = when {
    hasRoute<HomeRoute>() -> Screen.HOME
    hasRoute<DiscoverRoute>() -> Screen.DISCOVER
    hasRoute<LogRoute>() -> Screen.LOG
    hasRoute<ActivityRoute>() -> Screen.ACTIVITY
    hasRoute<MyProfileRoute>() -> Screen.PROFILE
    else -> null
}

@Composable
private fun Shell(sinLeer: Int) {
    val container = LocalAppContainer.current
    val navController = rememberNavController()
    val hojas = remember { Hojas() }
    val ancha = LocalWide.current
    val fondo = rememberGraphicsLayer()
    var origenFondo by remember { mutableStateOf(Offset.Zero) }

    // la pantalla que esta encima ahora mismo. es estado: cuando cambia, esto se recompone
    val entrada by navController.currentBackStackEntryAsState()
    val destino = entrada?.destination

    val perfilHastaArriba = destino?.let { it.hasRoute<MyProfileRoute>() || it.hasRoute<ProfileRoute>() } == true

    // pestaña marcada en la barra. si se vuelve atras hasta otra pestaña, se marca esa
    var apartado by rememberSaveable { mutableStateOf(Screen.HOME) }
    LaunchedEffect(destino) { destino?.pestana()?.let { apartado = it } }

    fun volver() {
        navController.popBackStack()
    }

    fun irA(pantalla: Screen) {
        if (pantalla == apartado && destino?.pestana() == null) {
            // tocar la pestaña en la que ya se esta cierra lo que tenga abierto encima
            navController.popBackStack(pantalla.route, inclusive = false)
        } else {
            navController.navigate(pantalla.route) {
                // en la pila solo queda inicio debajo de la pestaña elegida, para que no se acumulen;
                // lo que tenia abierto la pestaña que se deja se guarda y se recupera al volver a ella
                popUpTo<HomeRoute> { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        apartado = pantalla
    }

    // la ruta solo lleva el id: el contenido entero se lo queda apuntado el repositorio para que la ficha lo recupere
    fun abrirFicha(media: Media) {
        container.catalogRepository.recordar(media)
        navController.abrir(DetailRoute(media.id))
    }

    fun abrirPerfil(userId: String) = navController.abrir(ProfileRoute(userId))
    fun abrirResena(logId: String) = navController.abrir(ReviewRoute(logId))
    fun abrirLista(listId: String) = navController.abrir(ListRoute(listId))
    fun abrirChat(userId: String) = navController.abrir(ChatRoute(userId))

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { origenFondo = it.positionInRoot() }
                .drawWithContent {
                    // se graba lo que se pinta para que la barra de cristal pueda desenfocarlo
                    fondo.record { this@drawWithContent.drawContent() }
                    drawLayer(fondo)
                }
                // el perfil llega hasta arriba del todo (su portada va debajo de la barra de estado): ahi se deja solo el margen de los lados
                .windowInsetsPadding(WindowInsets.safeDrawing.only(if (perfilHastaArriba) WindowInsetsSides.Horizontal else WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = if (ancha) RailSpace else 0.dp),
            // las pantallas cambian al momento, sin fundido
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            // ---- las cinco pestañas

            composable<HomeRoute> {
                HomeScreenRoot(
                    onOpenPeople = { navController.abrir(PeopleRoute) },
                    onOpenStory = { userId, cola -> navController.abrir(StoryRoute(userId, cola.joinToString(","))) },
                    onOpenMedia = ::abrirFicha,
                    onOpenUser = ::abrirPerfil,
                    onOpenReview = ::abrirResena,
                    onSend = { hojas.enviando = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable<DiscoverRoute> {
                DiscoverScreenRoot(onOpenMedia = ::abrirFicha, onRegistrar = { irA(Screen.LOG) }, modifier = Modifier.fillMaxSize())
            }
            composable<LogRoute> {
                LogScreenRoot(onOpenMedia = ::abrirFicha, modifier = Modifier.fillMaxSize())
            }
            composable<ActivityRoute> {
                Centered(760.dp) {
                    ActivityScreenRoot(
                        onOpenChat = ::abrirChat,
                        onOpenGroup = { navController.abrir(GroupChatRoute(it)) },
                        onOpenUser = ::abrirPerfil,
                        onOpenReview = ::abrirResena,
                        onNewGroup = { hojas.creandoGrupo = true },
                        onNewMessage = { hojas.enviando = Outgoing.Nothing },
                        modifier = it,
                    )
                }
            }
            composable<MyProfileRoute> {
                ProfileScreenRoot(
                    userId = ME,
                    onBack = ::volver,
                    onOpenChat = ::abrirChat,
                    onOpenMedia = ::abrirFicha,
                    onOpenReview = ::abrirResena,
                    onRegistrar = { irA(Screen.LOG) },
                    onOpenList = ::abrirLista,
                    onNewList = { hojas.creandoLista = true },
                    onCustomize = { navController.abrir(CustomizeRoute) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // ---- pantallas que se abren encima

            composable<DetailRoute> { entry ->
                // toRoute devuelve la ruta con la que se llego aqui, y de ella se saca el id
                val ruta = entry.toRoute<DetailRoute>()
                Centered(820.dp) {
                    DetailScreenRoot(
                        mediaId = ruta.mediaId,
                        onBack = ::volver,
                        onOpenMedia = ::abrirFicha,
                        onOpenUser = ::abrirPerfil,
                        onOpenReview = ::abrirResena,
                        onReview = { media -> hojas.enResena = media },
                        onSend = { envio -> hojas.enviando = envio },
                        modifier = it,
                    )
                }
            }
            composable<ProfileRoute> { entry ->
                ProfileScreenRoot(
                    userId = entry.toRoute<ProfileRoute>().userId,
                    onBack = ::volver,
                    onOpenChat = ::abrirChat,
                    onOpenMedia = ::abrirFicha,
                    onOpenReview = ::abrirResena,
                    onRegistrar = { irA(Screen.LOG) },
                    onOpenList = ::abrirLista,
                    onNewList = { hojas.creandoLista = true },
                    onCustomize = { navController.abrir(CustomizeRoute) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable<ChatRoute> { entry ->
                Centered(760.dp) {
                    ChatScreenRoot(entry.toRoute<ChatRoute>().userId, onBack = ::volver, onOpenUser = ::abrirPerfil, onOpenMedia = ::abrirFicha, modifier = it)
                }
            }
            composable<GroupChatRoute> { entry ->
                Centered(760.dp) {
                    GroupChatScreenRoot(entry.toRoute<GroupChatRoute>().groupId, onBack = ::volver, onOpenInfo = { groupId -> hojas.infoDeGrupo = groupId }, onOpenMedia = ::abrirFicha, modifier = it)
                }
            }
            composable<StoryRoute> { entry ->
                val ruta = entry.toRoute<StoryRoute>()
                // desde una historia no se apila nada encima: lo que se abra la sustituye
                fun sustituir(nueva: Any) = navController.navigate(nueva) { popUpTo<StoryRoute> { inclusive = true } }
                StoryScreenRoot(
                    userId = ruta.userId,
                    cola = ruta.cola.split(","),
                    onClose = ::volver,
                    onOpenStory = { userId -> sustituir(StoryRoute(userId, ruta.cola)) },
                    onOpenUser = { userId -> sustituir(ProfileRoute(userId)) },
                    onOpenMedia = { media ->
                        container.catalogRepository.recordar(media)
                        sustituir(DetailRoute(media.id))
                    },
                    onReply = { userId -> sustituir(ChatRoute(userId)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable<PeopleRoute> {
                Centered(760.dp) { PeopleScreenRoot(onBack = ::volver, onOpenUser = ::abrirPerfil, modifier = it) }
            }
            composable<SettingsRoute> {
                Centered(640.dp) { SettingsScreenRoot(onBack = ::volver, modifier = it) }
            }
            composable<ReviewRoute> { entry ->
                Centered(760.dp) {
                    ReviewDetailScreenRoot(
                        logId = entry.toRoute<ReviewRoute>().logId,
                        onBack = ::volver,
                        onOpenUser = ::abrirPerfil,
                        onOpenMedia = ::abrirFicha,
                        onEdit = { media -> hojas.enResena = media },
                        onLogMenu = { log -> hojas.menuDeLog = log },
                        onSend = { log -> hojas.enviando = Outgoing.Review(log) },
                        modifier = it,
                    )
                }
            }
            composable<ListRoute> { entry ->
                val ruta = entry.toRoute<ListRoute>()
                Centered(820.dp) {
                    ListScreenRoot(ruta.listId, ruta.abrirSelector, onBack = ::volver, onOpenMedia = ::abrirFicha, onOpenReview = ::abrirResena, modifier = it)
                }
            }
            composable<CustomizeRoute> {
                Centered(760.dp) { CustomizeScreenRoot(onClose = ::volver, onOpenSettings = { navController.abrir(SettingsRoute) }, modifier = it) }
            }
        }

        // el chat y las historias van a pantalla completa, sin barra
        val pantallaCompleta = destino?.let {
            it.hasRoute<ChatRoute>() || it.hasRoute<GroupChatRoute>() || it.hasRoute<StoryRoute>() || it.hasRoute<ReviewRoute>()
        } == true
        if (!pantallaCompleta) {
            GlassBar(
                fondo = fondo,
                origenFondo = origenFondo,
                current = apartado,
                unread = sinLeer,
                alElegirPantalla = ::irA,
                modifier = Modifier.align(if (ancha) Alignment.CenterStart else Alignment.BottomCenter),
                vertical = ancha,
            )
        }
    }

    // las hojas inferiores se pintan encima de la pantalla que haya. cada una lleva sus propios
    // viewmodels (ConViewModelsPropios) para que arranque limpia cada vez que se abre
    hojas.enResena?.let { media ->
        ConViewModelsPropios { ReviewSheetRoot(media, onClose = { hojas.enResena = null }) }
    }
    hojas.enviando?.let { envio ->
        ConViewModelsPropios {
            SendSheetRoot(envio, onClose = { hojas.enviando = null }, onOpenChat = ::abrirChat, onOpenPeople = { navController.abrir(PeopleRoute) })
        }
    }
    hojas.menuDeLog?.let { log ->
        ConViewModelsPropios {
            LogMenuSheetRoot(
                log,
                onClose = { hojas.menuDeLog = null },
                onReview = { media -> hojas.enResena = media },
                onSend = { envio -> hojas.enviando = envio },
                onOpenMedia = ::abrirFicha,
                onOpenReview = ::abrirResena,
            )
        }
    }
    if (hojas.creandoLista) {
        ConViewModelsPropios {
            NewListSheetRoot(onClose = { hojas.creandoLista = false }, onCreated = { listId -> navController.abrir(ListRoute(listId, abrirSelector = true)) })
        }
    }
    if (hojas.creandoGrupo) {
        ConViewModelsPropios { NewGroupSheetRoot(onClose = { hojas.creandoGrupo = false }) }
    }
    hojas.infoDeGrupo?.let { groupId ->
        ConViewModelsPropios { GroupInfoSheetRoot(groupId, onClose = { hojas.infoDeGrupo = null }, onOpenUser = ::abrirPerfil) }
    }
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
