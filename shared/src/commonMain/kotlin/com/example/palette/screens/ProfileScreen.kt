package com.example.palette.screens

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
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.LogEntry
import com.example.palette.data.ME
import com.example.palette.data.Media
import com.example.palette.data.dateLabel
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Screen
import com.example.palette.ui.Avatar
import com.example.palette.ui.BackBar
import com.example.palette.ui.Chip
import com.example.palette.ui.Cover
import com.example.palette.ui.EmptyState
import com.example.palette.ui.IconAction
import com.example.palette.ui.Icons
import com.example.palette.ui.PaintBlob
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.Stars
import com.example.palette.ui.coverRatio
import com.example.palette.ui.display
import com.example.palette.ui.readable
import kotlin.random.Random

/** perfil propio (pestaña) o de otra persona (abierto encima). es la misma pantalla. */
@Composable
fun ProfileScreen(userId: String = ME, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    val user = store.user(userId) ?: return
    val own = userId == ME
    var section by remember { mutableIntStateOf(0) }
    var recent by remember { mutableStateOf(false) }

    val logs = store.profileLogs(userId, includeHidden = own, recentFirst = recent)
    val watchlist = store.watchlistOf(userId)

    // cada perfil puede llevar su propio fondo; el texto se pone claro u oscuro segun cual se lea encima
    val backdrop = user.background?.let { Color(it) }
    val ink = backdrop?.readable() ?: Pigment.Ink
    val soft = if (backdrop == null) Pigment.Graphite else ink.copy(alpha = 0.7f)

    CompositionLocalProvider(LocalContentColor provides ink) {
    Column(if (backdrop == null) modifier else modifier.background(backdrop)) {
        if (own) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End) {
                IconAction(Icons.Settings, "Editar perfil y ajustes", { nav.open(Dest.Settings) }, tint = ink)
            }
        } else {
            BackBar("@${user.username}", { nav.back() }, ink = ink)
        }
      BoxWithConstraints {
        // columnas de la paleta: dos en un movil para que las portadas salgan grandes, mas cuanto mas ancha es la ventana
        val columns = (maxWidth / 250.dp).toInt().coerceIn(2, 7)
        val covers = (maxWidth / 132.dp).toInt().coerceIn(3, 12)
        LazyColumn(contentPadding = PaddingValues(bottom = BarSpace)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(user, 92.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(user.name, style = display(27.sp, ink), textAlign = TextAlign.Center)
                    Text("@${user.username}", color = soft, fontSize = 14.sp)
                    if (user.bio.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(user.bio, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                    }
                    if (!own) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FollowButton(userId)
                            PillButton("Mensaje", { nav.open(Dest.Chat(userId)) }, filled = false, color = ink, icon = Icons.Send)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Segments(
                    listOf("Paleta (${logs.count { !it.hidden }})", "Pendientes" + (watchlist?.let { " (${it.size})" } ?: "")),
                    section, { section = it }, if (backdrop == null) Pigment.Accent else ink,
                    Modifier.fillMaxWidth().padding(bottom = 6.dp), ink, soft,
                )
            }

            if (section == 0) {
                if (logs.isEmpty()) item {
                    EmptyState(
                        title = if (own) "Tu paleta está en blanco" else "Todavía no ha registrado nada",
                        body = if (own) "Cada cosa que puntúes será una mancha de color aquí, de tu favorita a la que menos." else "Cuando puntúe algo aparecerá aquí.",
                        color = Pigment.Viridian, seed = 40, ink = ink,
                    ) {
                        if (own) PillButton("Registrar lo primero", { nav.switchTo(Screen.LOG) }, color = ink, icon = Icons.Plus)
                    }
                }
                if (logs.isNotEmpty()) item {
                    // dos maneras de ordenar: por nota (con lo fijado arriba) o como diario, con su fecha
                    Row(Modifier.padding(start = 20.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Favoritos", !recent, ink, { recent = false })
                        Chip("Recientes", recent, ink, { recent = true })
                    }
                    PaletteWall(logs, columns, own, dated = recent, soft = soft) { log ->
                        if (own) nav.logMenu = log else nav.open(Dest.Detail(log.media))
                    }
                }
            } else {
                when {
                    watchlist == null -> item {
                        EmptyState("Pendientes privados", "${user.name.substringBefore(' ')} prefiere no enseñar su lista.", Pigment.Graphite, seed = 41, ink = ink)
                    }
                    watchlist.isEmpty() -> item {
                        EmptyState(
                            title = "Nada pendiente",
                            body = if (own) "Guarda aquí lo que quieras ver, jugar, leer o escuchar más adelante con el marcador de cada ficha." else "No tiene nada guardado ahora mismo.",
                            color = Pigment.Viridian, seed = 42, ink = ink,
                        )
                    }
                    else -> items(watchlist.chunked(covers), key = { row -> row.joinToString("|") { it.id } }) { row ->
                        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { media -> WatchItem(media, Modifier.weight(1f)) { nav.open(Dest.Detail(media)) } }
                            repeat(covers - row.size) { Spacer(Modifier.weight(1f)) }
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
private fun PaletteWall(logs: List<LogEntry>, columns: Int, own: Boolean, dated: Boolean, soft: Color, onClick: (LogEntry) -> Unit) {
    val piles = List(columns) { mutableListOf<LogEntry>() }
    val heights = FloatArray(columns)
    logs.forEach { log ->
        val shortest = heights.indices.minBy { heights[it] }
        piles[shortest] += log
        // alto que ocupa con ancho 1: la portada (1 / proporcion) mas el hueco del titulo
        heights[shortest] += 1f / log.media.type.coverRatio + 0.3f
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        piles.forEachIndexed { column, pile ->
            // las columnas impares empiezan mas abajo para que no quede una rejilla
            Column(Modifier.weight(1f).padding(top = if (column % 2 == 1) 36.dp else 0.dp)) {
                pile.forEach { log -> WallItem(log, own, dated, soft) { onClick(log) } }
            }
        }
    }
}

@Composable
private fun WallItem(log: LogEntry, own: Boolean, dated: Boolean, soft: Color, onClick: () -> Unit) {
    val seed = log.media.id.hashCode()
    // ancho (90 a 100 % de la columna), lado al que se arrima y giro (de -3 a 3 grados)
    val (width, side, tilt) = remember(seed) {
        val random = Random(seed)
        Triple(0.9f + random.nextFloat() * 0.1f, random.nextInt(3), random.nextFloat() * 6f - 3f)
    }
    val alignment = when (side) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(bottom = 18.dp).alpha(if (log.hidden) 0.4f else 1f),
        horizontalAlignment = alignment,
    ) {
        if (dated) Text(dateLabel(log.createdAt), color = soft, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box(Modifier.fillMaxWidth(width).graphicsLayer { rotationZ = tilt }) {
            PaintBlob(log.media, seed, Modifier.fillMaxWidth())
            if (own && (log.pinned || log.hidden)) {
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
        Cover(media, Modifier.fillMaxWidth().aspectRatio(media.type.coverRatio))
        Spacer(Modifier.height(6.dp))
        Text(media.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, lineHeight = 15.sp, overflow = TextOverflow.Ellipsis)
    }
}
