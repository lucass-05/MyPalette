package com.example.palette.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.Extras
import com.example.palette.data.Media
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Outgoing
import com.example.palette.ui.Avatar
import com.example.palette.ui.BackBar
import com.example.palette.ui.CoverByHeight
import com.example.palette.ui.IconAction
import com.example.palette.ui.Icons
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.Splat
import com.example.palette.ui.Stars
import com.example.palette.ui.titular
import com.example.palette.ui.notaTexto
import kotlinx.coroutines.delay

// remember { mutableStateOf(...) } guarda un valor mientras la pantalla siga abierta.
// la ficha se pinta primero con lo que ya se sabe del contenido y, cuando llegan los detalles
// de la red, se cambia el valor y compose repinta lo que haga falta.

/**
 * ficha de un contenido. es la misma pantalla para todo: se pinta al momento con lo que ya
 * sabemos del resultado de busqueda y se completa con los detalles de su base de datos.
 */
@Composable
fun DetailScreen(inicial: Media, modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    // localclipboard, el sustituto, todavia no deja crear un clipentry de texto desde codigo comun
    @Suppress("DEPRECATION")
    val portapapeles = LocalClipboardManager.current

    var media by remember { mutableStateOf(inicial) }
    var similar by remember { mutableStateOf(emptyList<Media>()) }
    var extras by remember { mutableStateOf(Extras()) }
    var copiado by remember { mutableStateOf(false) }

    LaunchedEffect(inicial.id) {
        media = app.catalogo.detalles(inicial)
        extras = app.catalogo.extras(media)
        similar = app.catalogo.similar(media).filter { it.imageUrl != null }
    }
    LaunchedEffect(copiado) {
        if (copiado) {
            delay(1800)
            copiado = false
        }
    }

    val mio = almacen.miLogDe(media.id)
    val guardado = almacen.enPendientes(media.id)
    val tint = Pigment.of(media.type)
    val logsDeAmigos = almacen.estado.peopleLogs.filter { it.media.id == media.id && almacen.sigoA(it.userId) && !it.hidden }

    Column(modifier) {
        BackBar(if (copiado) "Enlace copiado" else "", { navegacion.volver() }) {
            IconAction(Icons.Send, "Enviar a un amigo", { navegacion.enviando = Outgoing.Content(media) })
            IconAction(Icons.Share, "Copiar enlace", {
                portapapeles.setText(AnnotatedString(media.link ?: media.title))
                copiado = true
            })
        }
        LazyColumn(contentPadding = PaddingValues(bottom = BarSpace), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Box(Modifier.padding(top = 6.dp), contentAlignment = Alignment.Center) {
                    Splat(media.id.hashCode(), tint, Modifier.size(320.dp))
                    CoverByHeight(media, 250.dp, Modifier.clip(RoundedCornerShape(8.dp)))
                }
                Spacer(Modifier.height(14.dp))
                Text(media.title, style = titular(30.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(media.subtitulo(), media.extra).joinToString(", "),
                    color = Pigment.Graphite, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )

                Spacer(Modifier.height(18.dp))
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Top) {
                    media.score?.let { score ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(notaTexto(score), style = titular(26.sp))
                            Text("sobre 10 en ${media.scoreSource}", color = Pigment.Graphite, fontSize = 12.sp)
                        }
                    }
                    if (mio != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) { Stars(mio.stars, size = 18.dp) }
                            Text("tu nota", color = Pigment.Graphite, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = if (mio == null) "Puntuar y reseñar" else "Editar reseña",
                        onClick = { navegacion.enResena = media },
                        color = tint,
                        icon = if (mio == null) Icons.Star else Icons.Pencil,
                        modifier = Modifier.weight(1f),
                    )
                    PillButton(
                        text = if (guardado) "En pendientes" else "Pendiente",
                        onClick = { almacen.alternarPendiente(media) },
                        filled = false,
                        icon = if (guardado) Icons.BookmarkCheck else Icons.Bookmark,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (mio != null && mio.review.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(mio.review, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                }

                media.overview?.let { overview ->
                    var desplegado by remember { mutableStateOf(false) }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        overview,
                        fontSize = 15.sp, lineHeight = 22.sp, color = Pigment.Ink.copy(alpha = 0.82f),
                        maxLines = if (desplegado) Int.MAX_VALUE else 6, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clickable { desplegado = !desplegado }.padding(horizontal = 20.dp),
                    )
                }
                if (media.genres.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(media.genres.joinToString(", "), color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                }
            }

            // datos propios del tipo: reparto y duracion en una peli, plataformas en un juego...
            if (extras.datos.isNotEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    extras.datos.forEach { (label, value) ->
                        Row {
                            Text(label, color = Pigment.Graphite, fontSize = 14.sp, modifier = Modifier.width(118.dp))
                            Text(value, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // una cancion enlaza con su album
            extras.album?.let { album ->
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Del álbum", Modifier.fillMaxWidth())
                    Row(
                        Modifier.fillMaxWidth().clickable { navegacion.abrir(Dest.Detail(album)) }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverByHeight(album, 64.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(album.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(album.creator, album.year?.toString(), album.extra).joinToString(", "), color = Pigment.Graphite, fontSize = 13.sp)
                        }
                    }
                }
            }

            // y un album lista sus canciones; cada una abre su propia ficha
            if (extras.canciones.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Canciones", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                }
                itemsIndexed(extras.canciones, key = { _, cancion -> cancion.id }) { numero, cancion ->
                    val registrado = almacen.miLogDe(cancion.id)
                    Row(
                        Modifier.fillMaxWidth().clickable { navegacion.abrir(Dest.Detail(cancion)) }.padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${numero + 1}", color = Pigment.Graphite, fontSize = 14.sp, modifier = Modifier.width(30.dp))
                        Text(cancion.title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (registrado != null) {
                            Stars(registrado.stars, size = 11.dp)
                            Spacer(Modifier.width(10.dp))
                        }
                        cancion.extra?.let { Text(it, color = Pigment.Graphite, fontSize = 13.sp) }
                    }
                }
            }

            if (logsDeAmigos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(26.dp))
                    SectionTitle("Entre tus amigos", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                }
                items(logsDeAmigos, key = { it.id }) { log ->
                    val autor = almacen.user(log.userId) ?: return@items
                    Row(Modifier.fillMaxWidth().clickable { navegacion.abrir(Dest.Profile(autor.id)) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Avatar(autor, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(autor.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Stars(log.stars, size = 13.dp)
                            }
                            if (log.review.isNotBlank()) Text(log.review, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }

            if (similar.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Pigment.Line)
                    Spacer(Modifier.height(20.dp))
                    SectionTitle("En la misma línea", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    MediaRow(similar, coverHeight = 150)
                }
            }
        }
    }
}
