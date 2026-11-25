package com.example.palette.features.anuncios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.core.model.Media
import com.example.palette.core.ui.MediaRow
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.titular
import com.example.palette.features.anuncios.domain.model.Anuncio

/** una fila de portadas de una marca, igual que las recomendaciones pero marcada como patrocinada. */
@Composable
fun FilaPatrocinada(
    anuncio: Anuncio,
    onVisto: (Anuncio) -> Unit,
    onClick: (Anuncio) -> Unit,
    onMediaClick: (Media) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(anuncio.id) { onVisto(anuncio) }
    Column(
        modifier.fillMaxWidth()
            .then(
                if (anuncio.ejemplo) Modifier.padding(horizontal = 8.dp).border(2.dp, Pigment.Accent, RoundedCornerShape(22.dp)).padding(vertical = 12.dp)
                else Modifier,
            ),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Etiqueta(anuncio)
                Spacer(Modifier.height(6.dp))
                Text(anuncio.titulo, style = titular(19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                anuncio.boton, color = Pigment.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onClick(anuncio) }.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        MediaRow(anuncio.contenidos, onClick = onMediaClick)
    }
}

// "patrocinado · marca". en los de ejemplo deja claro que es un hueco a la venta
@Composable
internal fun Etiqueta(anuncio: Anuncio, modifier: Modifier = Modifier) {
    val texto = if (anuncio.ejemplo) "Anuncio de ejemplo" else "Patrocinado · ${anuncio.anunciante}"
    Row(
        modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(50)).background(if (anuncio.ejemplo) Pigment.Accent else Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(texto, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
