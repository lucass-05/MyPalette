package com.example.palette.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.User
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.lists.ui.PortadaDeLista
import com.example.palette.features.lists.ui.ResenaDeLista
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.BackBar
import com.example.palette.core.ui.Chip
import com.example.palette.core.ui.Cover
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.FollowButton
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.PaintBlob
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.Segments
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.proporcion
import com.example.palette.core.ui.titular
import com.example.palette.core.util.ahora
import com.example.palette.core.util.fechaTexto
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.profile.presentation.ProfileAction
import com.example.palette.features.profile.presentation.ProfileState
import com.example.palette.features.profile.presentation.ProfileViewModel
import com.example.palette.navigation.BarSpace
import kotlin.random.Random

@Composable
fun ProfileScreenRoot(
    userId: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenMedia: (Media) -> Unit,
    onOpenReview: (String) -> Unit,
    onLogMenu: (LogEntry) -> Unit,
    onRegistrar: () -> Unit,
    onOpenList: (String) -> Unit,
    onNewList: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        ProfileViewModel(userId, container.profileRepository, container.peopleRepository, container.listsRepository, container.sortProfileLogsUseCase)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ProfileAction.OnBackClicked -> onBack()
                ProfileAction.OnSettingsClicked -> onOpenSettings()
                ProfileAction.OnMessageClicked -> onOpenChat(userId)
                ProfileAction.OnRegistrarClicked -> onRegistrar()
                // un log mio abre su menu (fijar, ocultar, editar...); el de otra persona, su reseña con los comentarios
                is ProfileAction.OnLogClicked -> if (state.perfil?.propio == true) onLogMenu(action.log) else onOpenReview(action.log.id)
                is ProfileAction.OnMediaClicked -> onOpenMedia(action.media)
                is ProfileAction.OnResenaClicked -> onOpenReview(action.logId)
                ProfileAction.OnNuevaListaClicked -> onNewList()
                is ProfileAction.OnEditarListaClicked -> onOpenList(action.listId)
                ProfileAction.OnPersonalizarClicked -> onCustomize()
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

/** perfil propio (pestaña) o de otra persona (abierto encima). es la misma pantalla. */
@Composable
fun ProfileScreen(state: ProfileState, onAction: (ProfileAction) -> Unit, modifier: Modifier = Modifier) {
    val perfil = state.perfil ?: return
    val user = perfil.user
    val propio = perfil.propio
    val logs = state.logs
    val watchlist = perfil.pendientes
    val bloques = user.layout.widgets.filter { !it.hidden }

    // cada perfil puede llevar su propio fondo; el texto se pone claro u oscuro segun cual se lea encima
    val fondo = user.background?.let { Color(it) }
    val ink = fondo?.legible() ?: Pigment.Ink
    val suave = if (fondo == null) Pigment.Graphite else ink.copy(alpha = 0.7f)

    CompositionLocalProvider(LocalContentColor provides ink) {
        Column(if (fondo == null) modifier else modifier.background(fondo)) {
            if (propio) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End) {
                    IconAction(Icons.Brush, "Personalizar perfil", { onAction(ProfileAction.OnPersonalizarClicked) }, tint = ink)
                    IconAction(Icons.Settings, "Editar perfil y ajustes", { onAction(ProfileAction.OnSettingsClicked) }, tint = ink)
                }
            } else {
                BackBar("@${user.username}", { onAction(ProfileAction.OnBackClicked) }, ink = ink)
            }
            BoxWithConstraints {
                // columnas de la paleta: dos en un movil para que las portadas salgan grandes, mas cuanto mas ancha es la ventana
                val columnas = (maxWidth / 250.dp).toInt().coerceIn(2, 7)
                val portadasPorFila = (maxWidth / 132.dp).toInt().coerceIn(3, 12)
                LazyColumn(contentPadding = PaddingValues(bottom = BarSpace)) {
                    item {
                        Cabecera(user, fondo ?: Pigment.Paper, ink, suave)
                        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (user.bio.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(user.bio, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                            }
                            if (!propio) {
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FollowButton(perfil.loSigo, { onAction(ProfileAction.OnFollowClicked) })
                                    PillButton("Mensaje", { onAction(ProfileAction.OnMessageClicked) }, filled = false, color = ink, icon = Icons.Send)
                                }
                            } else if (bloques.isEmpty() && user.bannerUrl == null) {
                                // mientras el perfil esta sin decorar se enseña el camino para hacerlo
                                Spacer(Modifier.height(14.dp))
                                PillButton("Personalizar perfil", { onAction(ProfileAction.OnPersonalizarClicked) }, filled = false, color = ink, icon = Icons.Brush)
                            }
                        }
                    }

                    // los bloques que ha elegido para su perfil, en su orden
                    items(bloques, key = { "w_${it.id}" }) { bloque ->
                        WidgetView(
                            bloque, perfil.logs, ink, suave,
                            onOpenMedia = { onAction(ProfileAction.OnMediaClicked(it)) },
                            onOpenReview = { onAction(ProfileAction.OnResenaClicked(it)) },
                        )
                    }

                    item {
                        Spacer(Modifier.height(18.dp))
                        Segments(
                            listOf("Paleta (${logs.count { !it.hidden }})", "Pendientes" + (watchlist?.let { " (${it.size})" } ?: "")),
                            state.seccion, { onAction(ProfileAction.OnSeccionChanged(it)) }, if (fondo == null) Pigment.Accent else ink,
                            Modifier.fillMaxWidth().padding(bottom = 6.dp), ink, suave,
                        )
                    }

                    if (state.seccion == 0) {
                        // orden de la paleta, listas y el + para crear una nueva, todo en la misma fila
                        if (propio || logs.isNotEmpty() || state.listas.isNotEmpty()) item {
                            FilaDeListas(state, propio, ink, onAction)
                        }
                        val abierta = state.lista
                        when {
                            abierta != null -> listaEnPerfil(abierta, propio, portadasPorFila, ink, onAction)
                            logs.isEmpty() -> item {
                                EmptyState(
                                    title = if (propio) "Tu paleta está en blanco" else "Todavía no ha registrado nada",
                                    cuerpo = if (propio) "Cada cosa que puntúes será una mancha de color aquí, de tu favorita a la que menos." else "Cuando puntúe algo aparecerá aquí.",
                                    color = Pigment.Viridian, semilla = 40, ink = ink,
                                ) {
                                    if (propio) PillButton("Registrar lo primero", { onAction(ProfileAction.OnRegistrarClicked) }, color = ink, icon = Icons.Plus)
                                }
                            }
                            else -> item {
                                PaletteWall(logs, columnas, propio, conFecha = state.recientesPrimero, suave = suave) { onAction(ProfileAction.OnLogClicked(it)) }
                            }
                        }
                    } else {
                        when {
                            watchlist == null -> item {
                                EmptyState("Pendientes privados", "${user.name.substringBefore(' ')} prefiere no enseñar su lista.", Pigment.Graphite, semilla = 41, ink = ink)
                            }
                            watchlist.isEmpty() -> item {
                                EmptyState(
                                    title = "Nada pendiente",
                                    cuerpo = if (propio) "Guarda aquí lo que quieras ver, jugar, leer o escuchar más adelante con el marcador de cada ficha." else "No tiene nada guardado ahora mismo.",
                                    color = Pigment.Viridian, semilla = 42, ink = ink,
                                )
                            }
                            else -> items(watchlist.chunked(portadasPorFila), key = { fila -> fila.joinToString("|") { it.id } }) { fila ->
                                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    fila.forEach { media -> WatchItem(media, Modifier.weight(1f)) { onAction(ProfileAction.OnMediaClicked(media)) } }
                                    repeat(portadasPorFila - fila.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * foto, nombre y usuario. si hay foto de cabecera va detras, y se funde con el fondo del perfil
 * con un degradado para que la foto de perfil quede encima sin un corte brusco.
 */
@Composable
private fun Cabecera(user: User, fondo: Color, ink: Color, suave: Color) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val banner = user.bannerUrl
        if (banner != null) {
            Box(Modifier.fillMaxWidth().height(206.dp)) {
                AsyncImage(banner, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(170.dp))
                Box(
                    Modifier.fillMaxWidth().height(170.dp).background(
                        Brush.verticalGradient(0.35f to Color.Transparent, 1f to fondo),
                    ),
                )
                Avatar(user, 92.dp, Modifier.align(Alignment.BottomCenter))
            }
        } else {
            Avatar(user, 92.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(user.name, style = titular(27.sp, ink), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp))
        Text("@${user.username}", color = suave, fontSize = 14.sp)
    }
}

@Composable
private fun FilaDeListas(state: ProfileState, propio: Boolean, ink: Color, onAction: (ProfileAction) -> Unit) {
    val abierta = state.listaAbierta
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip("Favoritos", abierta == null && !state.recientesPrimero, ink, { onAction(ProfileAction.OnOrdenChanged(false)) })
        Chip("Recientes", abierta == null && state.recientesPrimero, ink, { onAction(ProfileAction.OnOrdenChanged(true)) })
        state.listas.forEach { vista ->
            val lista = vista.lista
            Chip(
                lista.name, abierta == lista.id, ink, { onAction(ProfileAction.OnListaClicked(lista.id)) },
                // las listas que no se ven en mi perfil publico llevan un candado
                icon = if (propio && !lista.visible) Icons.Lock else if (lista.kind == ListKind.REVIEWS) Icons.Star else Icons.ListBullets,
            )
        }
        if (propio) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).border(1.dp, Pigment.Line, CircleShape).clickable { onAction(ProfileAction.OnNuevaListaClicked) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Plus, contentDescription = "Lista nueva", tint = ink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** una lista vista desde el perfil, en lugar de la paleta. */
private fun LazyListScope.listaEnPerfil(vista: ListaVista, propio: Boolean, porFila: Int, ink: Color, onAction: (ProfileAction) -> Unit) {
    val lista = vista.lista
    val deResenas = lista.kind == ListKind.REVIEWS
    if (propio) item {
        Row(Modifier.padding(start = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Editar lista", { onAction(ProfileAction.OnEditarListaClicked(lista.id)) }, filled = false, color = ink, icon = Icons.Pencil)
        }
    }
    val vacia = if (deResenas) vista.resenas.isEmpty() else lista.media.isEmpty()
    if (vacia) {
        item {
            EmptyState(
                "Lista vacía",
                if (propio) "Entra en la lista para añadirle cosas." else "Todavía no ha añadido nada.",
                Pigment.Ochre, semilla = 71, ink = ink,
            ) {
                if (propio) PillButton("Añadir", { onAction(ProfileAction.OnEditarListaClicked(lista.id)) }, color = ink, icon = Icons.Plus)
            }
        }
    } else if (deResenas) {
        items(vista.resenas, key = { "r_${it.id}" }) { log ->
            ResenaDeLista(log, propia = false, onClick = { onAction(ProfileAction.OnResenaClicked(log.id)) }, onQuitar = {})
        }
    } else {
        items(lista.media.chunked(porFila), key = { fila -> "m_" + fila.joinToString("|") { it.id } }) { fila ->
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                fila.forEach { media ->
                    PortadaDeLista(media, propia = false, modifier = Modifier.weight(1f), onClick = { onAction(ProfileAction.OnMediaClicked(media)) }, onQuitar = {})
                }
                repeat(porFila - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * la paleta: un muro de manchas a varias columnas. cada log va a la columna que lleve menos altura
 * (asi quedan parejas) y dentro de ella se coloca un poco a su aire: mas ancho o mas estrecho,
 * arrimado a un lado y algo girado. ese desorden sale del id del contenido, asi que no cambia al volver a abrir.
 */
@Composable
private fun PaletteWall(logs: List<LogEntry>, columnas: Int, propio: Boolean, conFecha: Boolean, suave: Color, onClick: (LogEntry) -> Unit) {
    val montones = List(columnas) { mutableListOf<LogEntry>() }
    val alturas = FloatArray(columnas)
    logs.forEach { log ->
        val masCorta = alturas.indices.minBy { alturas[it] }
        montones[masCorta] += log
        // alto que ocupa con ancho 1: la portada (1 / proporcion) mas el hueco del titulo
        alturas[masCorta] += 1f / log.media.type.proporcion + 0.3f
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        montones.forEachIndexed { columna, monton ->
            // las columnas impares empiezan mas abajo para que no quede una rejilla
            Column(Modifier.weight(1f).padding(top = if (columna % 2 == 1) 36.dp else 0.dp)) {
                monton.forEach { log -> WallItem(log, propio, conFecha, suave) { onClick(log) } }
            }
        }
    }
}

@Composable
private fun WallItem(log: LogEntry, propio: Boolean, conFecha: Boolean, suave: Color, onClick: () -> Unit) {
    val semilla = log.media.id.hashCode()
    // ancho (90 a 100 % de la columna), lado al que se arrima y giro (de -3 a 3 grados)
    val (width, lado, inclinacion) = remember(semilla) {
        val azar = Random(semilla)
        Triple(0.9f + azar.nextFloat() * 0.1f, azar.nextInt(3), azar.nextFloat() * 6f - 3f)
    }
    val alignment = when (lado) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(bottom = 18.dp).alpha(if (log.hidden) 0.4f else 1f),
        horizontalAlignment = alignment,
    ) {
        if (conFecha) Text(fechaTexto(log.createdAt), color = suave, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box(Modifier.fillMaxWidth(width).graphicsLayer { rotationZ = inclinacion }) {
            PaintBlob(log.media, semilla, Modifier.fillMaxWidth())
            if (propio && (log.pinned || log.hidden)) {
                Badge(if (log.pinned) Icons.Pin else Icons.EyeOff, Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(log.media.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Stars(log.stars, size = 12.dp)
    }
}

@Composable
private fun Badge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.size(26.dp).clip(CircleShape).background(Pigment.Paper), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Pigment.Ink, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun WatchItem(media: Media, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick)) {
        Cover(media, Modifier.fillMaxWidth().aspectRatio(media.type.proporcion))
        Spacer(Modifier.height(6.dp))
        Text(media.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, lineHeight = 15.sp, overflow = TextOverflow.Ellipsis)
    }
}
