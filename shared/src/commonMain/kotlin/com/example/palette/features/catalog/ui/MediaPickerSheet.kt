package com.example.palette.features.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.core.ui.Chip
import com.example.palette.core.ui.ConViewModelsPropios
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.SearchField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.catalog.presentation.LogAction
import com.example.palette.features.catalog.presentation.LogState
import com.example.palette.features.catalog.presentation.LogViewModel

// el mismo buscador de la pestaña registrar, metido en una hoja para elegir contenidos:
// lo usan las listas y los bloques de portadas del perfil. reutiliza LogViewModel tal cual.

/**
 * buscador para elegir contenidos. [elegidos] son los ids que ya estan dentro: salen marcados,
 * y tocarlos otra vez los quita. cada toque llega a [alElegir]; quien llama decide que hacer.
 */
@Composable
fun MediaPickerSheetRoot(titulo: String, elegidos: Set<String>, alElegir: (Media) -> Unit, onClose: () -> Unit) {
    // con su propio almacen de viewmodels: cada vez que se abre, el buscador empieza vacio
    ConViewModelsPropios {
        val container = LocalAppContainer.current
        val viewModel = viewModel { LogViewModel(container.searchMediaUseCase, container.catalogRepository.noDisponibles) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        MediaPickerSheet(
            titulo = titulo,
            state = state,
            elegidos = elegidos,
            onAction = { action ->
                when (action) {
                    is LogAction.OnResultadoClicked -> alElegir(action.media)
                    else -> viewModel.onAction(action)
                }
            },
            onClose = onClose,
        )
    }
}

@Composable
fun MediaPickerSheet(titulo: String, state: LogState, elegidos: Set<String>, onAction: (LogAction) -> Unit, onClose: () -> Unit) {
    Sheet(onClose) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = titular(22.sp), modifier = Modifier.weight(1f))
            PillButton("Hecho", onClose, color = Pigment.Accent)
        }
        Spacer(Modifier.height(12.dp))
        SearchField(state.consulta, { onAction(LogAction.OnConsultaChanged(it)) }, "Pelis, series, juegos, música, libros…", Modifier.padding(horizontal = 20.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MediaType.entries.forEach { type ->
                Chip(type.plural, type in state.tipos, Pigment.of(type), { onAction(LogAction.OnTipoToggled(type)) }, icon = type.icon)
            }
        }
        val encontrados = state.resultados
        LazyColumn(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 520.dp)) {
            when {
                encontrados == null -> item {
                    Text("Escribe para buscar y toca lo que quieras añadir.", color = Pigment.Graphite, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                encontrados.isEmpty() && !state.buscando -> item {
                    Text("Sin resultados.", color = Pigment.Graphite, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                else -> items(encontrados, key = { it.id }) { media ->
                    val dentro = media.id in elegidos
                    ResultRow(media, onClick = { onAction(LogAction.OnResultadoClicked(media)) }) { Marca(dentro) }
                }
            }
        }
    }
}

/** circulo de la derecha: vacio con un + si no esta elegido, de color con un check si lo esta. */
@Composable
internal fun Marca(dentro: Boolean) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(if (dentro) Pigment.Accent else Pigment.Mist),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (dentro) Icons.Check else Icons.Plus,
            contentDescription = if (dentro) "Quitar" else "Añadir",
            tint = if (dentro) Pigment.Accent.legible() else Pigment.Ink,
            modifier = Modifier.size(15.dp),
        )
    }
}
