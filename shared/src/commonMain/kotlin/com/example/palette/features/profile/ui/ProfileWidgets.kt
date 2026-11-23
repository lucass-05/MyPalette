package com.example.palette.features.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.ProfileWidget
import com.example.palette.core.model.ShelfItem
import com.example.palette.core.model.WidgetKind
import com.example.palette.core.ui.Cover
import com.example.palette.core.ui.PaintBlob
import com.example.palette.core.ui.proporcion
import com.example.palette.core.ui.titular
import com.example.palette.core.util.DAY
import com.example.palette.core.util.fechaTexto

// los bloques que cada uno pone en su perfil: un texto, una fila de portadas o un diario.
// aqui solo se pintan; se editan en CustomizeScreen.

/** un bloque del perfil. [logs] son los del perfil, para el diario. */
@Composable
fun WidgetView(
    widget: ProfileWidget,
    logs: List<LogEntry>,
    ink: Color,
    suave: Color,
    onOpenMedia: (Media) -> Unit,
    onOpenReview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        if (widget.title.isNotBlank()) {
            Text(widget.title, style = titular(19.sp, ink), modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }
        when (widget.kind) {
            WidgetKind.TEXT -> if (widget.text.isNotBlank()) {
                Text(widget.text, color = ink, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }
            WidgetKind.SHELF -> LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(widget.items, key = { it.media.id }) { item ->
                    PortadaDeBloque(item, 150.dp, Modifier.clickable { onOpenMedia(item.media) })
                }
            }
            WidgetKind.DIARY -> Diario(logs, ink, suave, onOpenReview)
        }
    }
}

/** una portada de un bloque, como mancha de pintura o como portada con las esquinas redondeadas. */
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
 * los ultimos dias en los que registro algo: la fecha en grande y al lado lo de ese dia.
 * los dias sin nada no salen. tocar una portada abre la reseña.
 */
@Composable
private fun Diario(logs: List<LogEntry>, ink: Color, suave: Color, onOpenReview: (String) -> Unit) {
    val dias = logs.filter { !it.hidden }
        .groupBy { it.createdAt.floorDiv(DAY) }
        .toList()
        .sortedByDescending { it.first }
        .take(DIAS_EN_EL_DIARIO)
    if (dias.isEmpty()) {
        Text("Aún no hay días que contar.", color = suave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        dias.forEach { (dia, delDia) ->
            // fechaTexto da "12 nov 2026": el numero va en grande y el resto debajo
            val partes = fechaTexto(dia * DAY).split(" ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(84.dp).padding(start = 20.dp), horizontalAlignment = Alignment.Start) {
                    Text(partes[0], style = titular(36.sp, ink))
                    Text(partes.drop(1).joinToString(" "), color = suave, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                LazyRow(contentPadding = PaddingValues(start = 8.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(delDia.sortedBy { it.createdAt }, key = { it.id }) { log ->
                        Cover(
                            log.media,
                            Modifier.height(92.dp).aspectRatio(log.media.type.proporcion).clickable { onOpenReview(log.id) },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val DIAS_EN_EL_DIARIO = 7
