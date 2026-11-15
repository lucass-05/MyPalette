package com.example.palette.features.catalog.ui

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.Media
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.BackBar
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.MediaRow
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.SectionTitle
import com.example.palette.core.ui.Splat
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.notaTexto
import com.example.palette.core.ui.subtitulo
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.catalog.presentation.DetailAction
import com.example.palette.features.catalog.presentation.DetailState
import com.example.palette.features.catalog.presentation.DetailViewModel
import com.example.palette.features.messages.domain.model.Outgoing
import com.example.palette.navigation.BarSpace

// la ficha de un contenido es la misma pantalla para todo (peli, juego, album...).
// DetailScreenRoot crea el viewmodel y resuelve lo que no es estado: navegar, abrir hojas y el portapapeles.
// DetailScreen solo pinta el DetailState que le llega y avisa de lo que toca el usuario con un DetailAction.

@Composable
fun DetailScreenRoot(
    mediaId: String,
    onBack: () -> Unit,
    onOpenMedia: (Media) -> Unit,
    onOpenUser: (String) -> Unit,
    onReview: (Media) -> Unit,
    onSend: (Outgoing) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    // localclipboard, el sustituto, todavia no deja crear un clipentry de texto desde codigo comun
    @Suppress("DEPRECATION")
    val portapapeles = LocalClipboardManager.current

    // a la pantalla solo le llega el id; el contenido lo tiene apuntado el repositorio.
    // la ficha se pinta al momento con eso y el viewmodel pide el resto a la red
    val viewModel = viewModel {
        DetailViewModel(container.catalogRepository.recordado(mediaId), container.getMediaDetailUseCase, container.libraryRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    DetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                DetailAction.OnBackClicked -> onBack()
                DetailAction.OnReviewClicked -> onReview(state.media)
                DetailAction.OnSendClicked -> onSend(Outgoing.Content(state.media))
                DetailAction.OnShareClicked -> portapapeles.setText(AnnotatedString(state.media.link ?: state.media.title))
                is DetailAction.OnMediaClicked -> onOpenMedia(action.media)
                is DetailAction.OnUserClicked -> onOpenUser(action.userId)
                else -> {}
            }
            // el viewmodel se entera de todas: el decide cuales cambian el estado
            viewModel.onAction(action)
        },
        modifier = modifier,
    )
}

@Composable
fun DetailScreen(state: DetailState, onAction: (DetailAction) -> Unit, modifier: Modifier = Modifier) {
    val media = state.media
    val extras = state.extras
    val mio = state.miLog
    val guardado = state.enPendientes
    val tint = Pigment.of(media.type)

    Column(modifier) {
        BackBar(if (state.enlaceCopiado) "Enlace copiado" else "", { onAction(DetailAction.OnBackClicked) }) {
            IconAction(Icons.Send, "Enviar a un amigo", { onAction(DetailAction.OnSendClicked) })
            IconAction(Icons.Share, "Copiar enlace", { onAction(DetailAction.OnShareClicked) })
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
                        onClick = { onAction(DetailAction.OnReviewClicked) },
                        color = tint,
                        icon = if (mio == null) Icons.Star else Icons.Pencil,
                        modifier = Modifier.weight(1f),
                    )
                    PillButton(
                        text = if (guardado) "En pendientes" else "Pendiente",
                        onClick = { onAction(DetailAction.OnWatchlistClicked) },
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
                        Modifier.fillMaxWidth().clickable { onAction(DetailAction.OnMediaClicked(album)) }.padding(horizontal = 20.dp, vertical = 10.dp),
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
                    val miNota = state.misNotas[cancion.id]
                    Row(
                        Modifier.fillMaxWidth().clickable { onAction(DetailAction.OnMediaClicked(cancion)) }.padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${numero + 1}", color = Pigment.Graphite, fontSize = 14.sp, modifier = Modifier.width(30.dp))
                        Text(cancion.title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (miNota != null) {
                            Stars(miNota, size = 11.dp)
                            Spacer(Modifier.width(10.dp))
                        }
                        cancion.extra?.let { Text(it, color = Pigment.Graphite, fontSize = 13.sp) }
                    }
                }
            }

            if (state.logsDeAmigos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(26.dp))
                    SectionTitle("Entre tus amigos", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                }
                items(state.logsDeAmigos, key = { it.log.id }) { (autor, log) ->
                    Row(Modifier.fillMaxWidth().clickable { onAction(DetailAction.OnUserClicked(autor.id)) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
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

            if (state.parecidos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Pigment.Line)
                    Spacer(Modifier.height(20.dp))
                    SectionTitle("En la misma línea", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    MediaRow(state.parecidos, coverHeight = 150, onClick = { onAction(DetailAction.OnMediaClicked(it)) })
                }
            }
        }
    }
}
