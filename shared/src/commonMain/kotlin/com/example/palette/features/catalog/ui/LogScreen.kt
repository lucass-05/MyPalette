package com.example.palette.features.catalog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.core.ui.Chip
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.ScreenTitle
import com.example.palette.core.ui.SearchField
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.notaTexto
import com.example.palette.core.ui.subtitulo
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.catalog.presentation.LogAction
import com.example.palette.features.catalog.presentation.LogState
import com.example.palette.features.catalog.presentation.LogViewModel
import com.example.palette.navigation.BarSpace

// esta pantalla sigue el esquema de la asignatura:
//   LogState + LogAction + LogViewModel (carpeta presentation) y LogScreenRoot + LogScreen (aqui).
// la pantalla solo pinta el estado y emite acciones; quien decide que pasa es el viewmodel.

// "root": conecta la pantalla con su viewmodel y con la navegacion.
// la pantalla de verdad (LogScreen) no sabe nada de eso: recibe un estado y avisa de acciones,
// asi se puede previsualizar o probar pasandole un LogState a mano.
@Composable
fun LogScreenRoot(onOpenMedia: (Media) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    // viewModel { } crea el viewmodel la primera vez y devuelve el mismo en las siguientes recomposiciones
    val viewModel = viewModel { LogViewModel(container.searchMediaUseCase, container.catalogRepository.noDisponibles) }
    // el estado es un flujo: collectAsStateWithLifecycle lo convierte en estado de compose para que la ui se repinte
    val state by viewModel.state.collectAsStateWithLifecycle()

    LogScreen(
        state = state,
        onAction = { action ->
            when (action) {
                // abrir una ficha es navegar: no cambia el estado de esta pantalla, asi que no pasa por el viewmodel
                is LogAction.OnResultadoClicked -> onOpenMedia(action.media)
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun LogScreen(state: LogState, onAction: (LogAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        ScreenTitle("Registrar")
        SearchField(state.consulta, { onAction(LogAction.OnConsultaChanged(it)) }, "Pelis, series, juegos, música, libros…", Modifier.padding(horizontal = 20.dp).widthIn(max = 720.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MediaType.entries.forEach { type ->
                Chip(
                    text = type.plural,
                    selected = type in state.tipos,
                    color = Pigment.of(type),
                    icon = type.icon,
                    onClick = { onAction(LogAction.OnTipoToggled(type)) },
                )
            }
        }

        val encontrados = state.resultados
        LazyVerticalGrid(GridCells.Adaptive(380.dp), contentPadding = PaddingValues(bottom = BarSpace)) {
            when {
                encontrados == null -> item(span = { GridItemSpan(maxLineSpan) }) { SearchHint(state.noDisponibles) }
                encontrados.isEmpty() && !state.buscando -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Sin resultados",
                        cuerpo = "No hay nada que se llame así" + (if (state.tipos.isEmpty()) "." else " en lo que tienes marcado. Prueba a quitar filtros."),
                        color = Pigment.Ochre,
                        semilla = 13,
                    )
                }
                else -> items(encontrados, key = { it.id }) { media ->
                    ResultRow(media) { onAction(LogAction.OnResultadoClicked(media)) }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(media: Media, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // todas las miniaturas ocupan el mismo ancho para que los titulos queden alineados
        Row(Modifier.width(50.dp), horizontalArrangement = Arrangement.Center) { CoverByHeight(media, 62.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(media.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(media.type.icon, contentDescription = null, tint = Pigment.of(media.type), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(media.subtitulo(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        media.score?.let {
            Spacer(Modifier.width(10.dp))
            Icon(Icons.StarFilled, contentDescription = null, tint = Pigment.Star, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(notaTexto(it), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SearchHint(noDisponibles: Set<MediaType>) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Busca lo último que has visto, jugado, leído o escuchado para puntuarlo o guardarlo en pendientes.", color = Pigment.Graphite, fontSize = 14.sp, lineHeight = 20.sp)
        if (noDisponibles.isNotEmpty()) {
            // aviso para desarrollo: desaparece en cuanto las claves estan en local.properties
            Spacer(Modifier.height(14.dp))
            Text(
                "Falta la clave de TMDB: sin ella no salen " + noDisponibles.joinToString(" ni ") { it.plural.lowercase() } + ". Mira el README para añadirla.",
                color = Pigment.Cadmium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}
