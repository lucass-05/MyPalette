package com.example.palette.features.profile.ui

import androidx.compose.foundation.background
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
    onLogMenu: (LogEntry) -> Unit,
    onRegistrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        ProfileViewModel(userId, container.profileRepository, container.peopleRepository, container.sortProfileLogsUseCase)
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
                // un log mio abre su menu (fijar, ocultar, editar...); el de otra persona, la ficha del contenido
                is ProfileAction.OnLogClicked -> if (state.perfil?.propio == true) onLogMenu(action.log) else onOpenMedia(action.log.media)
                is ProfileAction.OnMediaClicked -> onOpenMedia(action.media)
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

    // cada perfil puede llevar su propio fondo; el texto se pone claro u oscuro segun cual se lea encima
    val fondo = user.background?.let { Color(it) }
    val ink = fondo?.legible() ?: Pigment.Ink
    val suave = if (fondo == null) Pigment.Graphite else ink.copy(alpha = 0.7f)

    CompositionLocalProvider(LocalContentColor provides ink) {
        Column(if (fondo == null) modifier else modifier.background(fondo)) {
            if (propio) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End) {
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
                        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Avatar(user, 92.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(user.name, style = titular(27.sp, ink), textAlign = TextAlign.Center)
                            Text("@${user.username}", color = suave, fontSize = 14.sp)
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
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Segments(
                            listOf("Paleta (${logs.count { !it.hidden }})", "Pendientes" + (watchlist?.let { " (${it.size})" } ?: "")),
                            state.seccion, { onAction(ProfileAction.OnSeccionChanged(it)) }, if (fondo == null) Pigment.Accent else ink,
                            Modifier.fillMaxWidth().padding(bottom = 6.dp), ink, suave,
                        )
                    }

                    if (state.seccion == 0) {
                        if (logs.isEmpty()) item {
                            EmptyState(
                                title = if (propio) "Tu paleta está en blanco" else "Todavía no ha registrado nada",
                                cuerpo = if (propio) "Cada cosa que puntúes será una mancha de color aquí, de tu favorita a la que menos." else "Cuando puntúe algo aparecerá aquí.",
                                color = Pigment.Viridian, semilla = 40, ink = ink,
                            ) {
                                if (propio) PillButton("Registrar lo primero", { onAction(ProfileAction.OnRegistrarClicked) }, color = ink, icon = Icons.Plus)
                            }
                        }
                        if (logs.isNotEmpty()) item {
                            // dos maneras de ordenar: por nota (con lo fijado arriba) o como diario, con su fecha
                            Row(Modifier.padding(start = 20.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip("Favoritos", !state.recientesPrimero, ink, { onAction(ProfileAction.OnOrdenChanged(false)) })
                                Chip("Recientes", state.recientesPrimero, ink, { onAction(ProfileAction.OnOrdenChanged(true)) })
                            }
                            PaletteWall(logs, columnas, propio, conFecha = state.recientesPrimero, suave = suave) { onAction(ProfileAction.OnLogClicked(it)) }
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
