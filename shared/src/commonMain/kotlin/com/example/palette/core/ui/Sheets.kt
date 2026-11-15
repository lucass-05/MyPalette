package com.example.palette.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.core.model.Media

// lo que comparten todas las hojas inferiores de la app

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Pigment.Paper,
        contentColor = Pigment.Ink,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) { content() }
    }
}

/** cabecera de una hoja que habla de un contenido: su portada, su titulo y de que tipo es. */
@Composable
fun MediaHeader(media: Media) {
    Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverByHeight(media, 64.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(media.title, style = titular(20.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(media.subtitulo(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
