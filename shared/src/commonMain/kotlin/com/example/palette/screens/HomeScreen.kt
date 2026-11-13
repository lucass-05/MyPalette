package com.example.palette.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.LogEntry
import com.example.palette.data.User
import com.example.palette.data.haceCuanto
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Outgoing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.palette.ui.Avatar
import com.example.palette.ui.BackBar
import com.example.palette.ui.SearchField
import com.example.palette.ui.CoverByHeight
import com.example.palette.ui.EmptyState
import com.example.palette.ui.IconAction
import com.example.palette.ui.Icons
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.ScreenTitle
import com.example.palette.ui.Stars
import com.example.palette.ui.formaAvatar
import com.example.palette.ui.titular

// LazyVerticalGrid solo compone las celdas que se ven en pantalla, por eso aguanta listas largas.
// la "key" de cada elemento le dice a compose cual es cual cuando la lista cambia de orden.

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val novedades = almacen.novedades
    val historias = almacen.historias

    // una columna en el movil; en pantallas anchas los logs se reparten en varias
    LazyVerticalGrid(GridCells.Adaptive(400.dp), modifier, contentPadding = PaddingValues(bottom = BarSpace)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Column {
            ScreenTitle("Palette") {
                IconAction(Icons.UserPlus, "Buscar gente", { navegacion.abrir(Dest.People) })
            }
            if (almacen.sinConexion) {
                Text("Sin conexión. Estás viendo lo último que se guardó.", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
          }
        }
        if (historias.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              Column {
                val cola = historias.map { it.id }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // la forma depende del puesto en la fila: la primera historia siempre lleva la misma mancha, la segunda la siguiente...
                    itemsIndexed(historias, key = { _, user -> user.id }) { posicion, user ->
                        StoryBubble(user, posicion, sinVer = almacen.tieneHistoriaSinVer(user.id)) { navegacion.abrir(Dest.Story(user.id, cola)) }
                    }
                }
                HorizontalDivider(Modifier.padding(top = 6.dp), color = Pigment.Line)
              }
            }
        }
        if (novedades.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = if (almacen.amigos.isEmpty()) "Aún no sigues a nadie" else "Todo tranquilo por aquí",
                    cuerpo = if (almacen.amigos.isEmpty()) "Cuando sigas a alguien, lo que vea, juegue, lea o escuche aparecerá aquí."
                    else "La gente a la que sigues todavía no ha registrado nada.",
                    color = Pigment.Cadmium,
                ) {
                    PillButton("Buscar gente", { navegacion.abrir(Dest.People) }, icon = Icons.Search)
                }
            }
        }
        gridItems(novedades, key = { it.id }) { log ->
            Column {
                FeedPost(log)
                HorizontalDivider(color = Pigment.Line)
            }
        }
    }
}

@Composable
private fun StoryBubble(user: User, posicion: Int, sinVer: Boolean, onClick: () -> Unit) {
    // sin ver: anillo del color favorito de esa persona. ya vista: una linea fina
    val anillo = if (sinVer) Color(user.favorite) else Pigment.Line
    Column(Modifier.width(72.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(70.dp).border(if (sinVer) 3.dp else 1.dp, anillo, formaAvatar(posicion)), contentAlignment = Alignment.Center) {
            Avatar(user, 58.dp, variante = posicion)
        }
        Spacer(Modifier.height(5.dp))
        Text(user.name.substringBefore(' '), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (sinVer) Pigment.Ink else Pigment.Graphite)
    }
}

@Composable
private fun FeedPost(log: LogEntry) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val autor = almacen.user(log.userId) ?: return
    val media = log.media

    Column(Modifier.fillMaxWidth().clickable { navegacion.abrir(Dest.Detail(media)) }.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp)) {
        Row(Modifier.clickable { navegacion.abrir(Dest.Profile(autor.id)) }, verticalAlignment = Alignment.CenterVertically) {
            Avatar(autor, 34.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(autor.name) }
                    withStyle(SpanStyle(color = Pigment.Graphite)) { append(" ${media.type.verbo}") }
                },
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(haceCuanto(log.createdAt), color = Pigment.Graphite, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            CoverByHeight(media, 148.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(media.title, style = titular(21.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(media.subtitulo(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Stars(log.stars, size = 16.dp)
                if (log.review.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Text(log.review, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(Modifier.padding(top = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            val liked = almacen.meGusta(log.id)
            IconAction(
                if (liked) Icons.HeartFilled else Icons.Heart,
                if (liked) "Quitar me gusta" else "Me gusta",
                { almacen.alternarMeGusta(log.id) },
                tint = if (liked) Pigment.Cadmium else Pigment.Ink,
            )
            IconAction(Icons.Send, "Enviar a un amigo", { navegacion.enviando = Outgoing.Review(log) })
            val guardado = almacen.enPendientes(media.id)
            IconAction(
                if (guardado) Icons.BookmarkFilled else Icons.Bookmark,
                if (guardado) "Quitar de pendientes" else "Guardar en pendientes",
                { almacen.alternarPendiente(media) },
            )
        }
    }
}

/** buscador de personas para empezar a seguirlas. */
@Composable
fun PeopleScreen(modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    var consulta by remember { mutableStateOf("") }
    val results = almacen.buscarGente(consulta)

    Column(modifier) {
        BackBar("Buscar gente", { navegacion.volver() })
        SearchField(consulta, { consulta = it }, "Nombre o @usuario", Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = BarSpace)) {
            if (consulta.isBlank() && results.isNotEmpty()) {
                item { Text("Gente que quizá conozcas", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            }
            items(results, key = { it.id }) { user ->
                UserRow(user, onClick = { navegacion.abrir(Dest.Profile(user.id)) }) { FollowButton(user.id) }
            }
            if (results.isEmpty()) {
                item {
                    EmptyState(
                        title = if (consulta.isBlank()) "Ya sigues a todo el mundo" else "Nadie con ese nombre",
                        cuerpo = if (consulta.isBlank()) "Cuando se registre más gente aparecerá aquí." else "Prueba con su nombre de usuario, sin la arroba.",
                        color = Pigment.Cobalt,
                        semilla = 9,
                    )
                }
            }
        }
    }
}
