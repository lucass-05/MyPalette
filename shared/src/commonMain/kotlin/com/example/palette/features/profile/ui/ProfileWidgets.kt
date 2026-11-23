package com.example.palette.features.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.ProfileWidget
import com.example.palette.core.model.ShelfItem
import com.example.palette.core.model.WidgetKind
import com.example.palette.core.ui.Cover
import com.example.palette.core.ui.PaintBlob
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.proporcion
import com.example.palette.core.ui.titular
import com.example.palette.core.util.DAY
import com.example.palette.core.util.fechaTexto
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.profile.presentation.ProfileAction
import com.example.palette.features.profile.presentation.ProfileState
import com.example.palette.features.profile.presentation.Seccion

// lo que se pinta dentro del perfil: los bloques de la paleta y las piezas que comparten con las
// paginas (la cuadricula de portadas y el diario). aqui solo se pinta; se edita en CustomizeScreen.

/**
 * una portada en una cuadricula del perfil. si lleva [log] es una reseña (sale con su nota y
 * abre la reseña); si no, es un contenido suelto y abre su ficha.
 */
data class Pieza(val media: Media, val log: LogEntry? = null)

fun LogEntry.pieza() = Pieza(media, this)

/**
 * portadas en filas. el ancho de cada una sale de cuantas caben, asi que con pocas el bloque
 * se queda corto en vez de estirarlas. [blob]: manchas de pintura o recuadros con las esquinas redondeadas.
 */
@Composable
fun Cuadricula(piezas: List<Pieza>, blob: Boolean, ink: Color, onClick: (Pieza) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        val hueco = 12.dp
        val columnas = (maxWidth / 104.dp).toInt().coerceIn(3, 8)
        val ancho = (maxWidth - hueco * (columnas - 1)) / columnas
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            piezas.chunked(columnas).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(hueco)) {
                    fila.forEach { pieza -> PiezaView(pieza, blob, ancho, ink) { onClick(pieza) } }
                }
            }
        }
    }
}

@Composable
private fun PiezaView(pieza: Pieza, blob: Boolean, ancho: Dp, ink: Color, onClick: () -> Unit) {
    val media = pieza.media
    // en mi perfil salen tambien mis reseñas ocultas, mas apagadas
    Column(Modifier.width(ancho).alpha(if (pieza.log?.hidden == true) 0.4f else 1f).clickable(onClick = onClick)) {
        Portada(media, blob, Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(media.title, color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        pieza.log?.let {
            Spacer(Modifier.height(2.dp))
            Stars(it.stars, size = 10.dp)
        }
    }
}

/** una portada como mancha de pintura o como recuadro. con el ancho que le den, el alto sale de su tipo. */
@Composable
fun Portada(media: Media, blob: Boolean, modifier: Modifier = Modifier) {
    if (blob) PaintBlob(media, media.id.hashCode(), modifier)
    else Cover(media, modifier.aspectRatio(media.type.proporcion), shape = RoundedCornerShape(12.dp))
}

/** una portada de un bloque de portadas elegidas: cada una se ve como mancha o recuadro por separado. */
@Composable
fun PortadaDeBloque(item: ShelfItem, alto: Dp, modifier: Modifier = Modifier) {
    val media = item.media
    if (item.blob) {
        PaintBlob(media, media.id.hashCode(), modifier.height(alto))
    } else {
        Cover(media, modifier.height(alto).aspectRatio(media.type.proporcion), shape = RoundedCornerShape(14.dp))
    }
}

/**
 * los dias en los que registro algo, del mas reciente al mas antiguo: la fecha en grande y al lado
 * lo de ese dia. los dias sin nada no salen. con [maxDias] solo los ultimos. tocar una portada abre la reseña.
 */
@Composable
fun Diario(logs: List<LogEntry>, maxDias: Int?, blob: Boolean, ink: Color, suave: Color, onOpenReview: (String) -> Unit) {
    val todos = logs.groupBy { it.createdAt.floorDiv(DAY) }.toList().sortedByDescending { it.first }
    val dias = if (maxDias != null) todos.take(maxDias) else todos
    if (dias.isEmpty()) {
        Text("Aún no hay días que contar.", color = suave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        dias.forEach { (dia, delDia) ->
            // fechaTexto da "12 nov 2026": el numero va en grande y el resto debajo
            val partes = fechaTexto(dia * DAY).split(" ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(84.dp).padding(start = 20.dp)) {
                    Text(partes[0], style = titular(36.sp, ink))
                    Text(partes.drop(1).joinToString(" "), color = suave, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                LazyRow(contentPadding = PaddingValues(start = 8.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(delDia.sortedBy { it.createdAt }, key = { it.id }) { log ->
                        Portada(
                            log.media, blob,
                            Modifier.height(92.dp).alpha(if (log.hidden) 0.4f else 1f).clickable { onOpenReview(log.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * un bloque de la paleta. los que resumen una pagina (favoritos, diario, pendientes, una lista)
 * llevan "ver todo" para abrirla, si quien mira puede verla.
 */
@Composable
fun BloqueView(widget: ProfileWidget, state: ProfileState, ink: Color, suave: Color, onAction: (ProfileAction) -> Unit) {
    val perfil = state.perfil ?: return
    val abrirPieza: (Pieza) -> Unit = { p -> onAction(p.log?.let { ProfileAction.OnResenaClicked(it.id) } ?: ProfileAction.OnMediaClicked(p.media)) }
    val lista: ListaVista? = if (widget.kind == WidgetKind.LIST) state.lista(widget.listId) else null

    // a donde lleva "ver todo"; null si no hay pagina que abrir o quien mira no puede verla
    val destino: Seccion? = when (widget.kind) {
        WidgetKind.FAVORITES -> Seccion.Favoritos.takeIf { state.verFavoritos }
        WidgetKind.DIARY -> Seccion.Diario.takeIf { state.verDiario }
        WidgetKind.WATCHLIST -> Seccion.Pendientes.takeIf { state.verPendientes }
        WidgetKind.LIST -> lista?.let { Seccion.Lista(it.lista.id) }
        else -> null
    }
    val titulo = widget.title.ifBlank {
        when (widget.kind) {
            WidgetKind.FAVORITES -> "Favoritos"
            WidgetKind.DIARY -> "Diario"
            WidgetKind.WATCHLIST -> "Pendientes"
            WidgetKind.LIST -> lista?.lista?.name.orEmpty()
            else -> ""
        }
    }

    // lo que va a enseñar; si no hay nada (unos pendientes privados, una lista que no se ve) el bloque no sale
    val piezas: List<Pieza>? = when (widget.kind) {
        WidgetKind.FAVORITES -> state.favoritos.filter { perfil.propio || !it.hidden }.take(widget.count).map { it.pieza() }
        WidgetKind.WATCHLIST -> perfil.pendientes?.take(widget.count)?.map { Pieza(it) }
        WidgetKind.LIST -> lista?.let { v ->
            if (v.lista.kind == ListKind.REVIEWS) v.resenas.take(widget.count).map { it.pieza() }
            else v.lista.media.take(widget.count).map { Pieza(it) }
        }
        else -> null
    }
    if (widget.kind in setOf(WidgetKind.WATCHLIST, WidgetKind.LIST) && piezas.isNullOrEmpty()) return

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        if (titulo.isNotBlank() || destino != null) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = titular(19.sp, ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (destino != null) {
                    Text(
                        "Ver todo", color = suave, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onAction(ProfileAction.OnSeccionChanged(destino)) }.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        when (widget.kind) {
            WidgetKind.TEXT -> if (widget.text.isNotBlank()) {
                Text(widget.text, color = ink, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }
            WidgetKind.DIARY -> Diario(
                perfil.logs.filter { perfil.propio || !it.hidden }, widget.count, widget.blob, ink, suave,
                onOpenReview = { onAction(ProfileAction.OnResenaClicked(it)) },
            )
            WidgetKind.SHELF -> LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(widget.items, key = { it.media.id }) { item ->
                    PortadaDeBloque(item, 150.dp, Modifier.clickable { onAction(ProfileAction.OnMediaClicked(item.media)) })
                }
            }
            else -> if (piezas.isNullOrEmpty()) {
                Text("Todavía no hay nada aquí.", color = suave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
            } else {
                Cuadricula(piezas, widget.blob, ink, abrirPieza)
            }
        }
    }
}
