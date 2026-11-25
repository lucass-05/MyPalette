package com.example.palette.features.catalog.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.Media
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.MediaRow
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.ScreenTitle
import com.example.palette.core.ui.SectionTitle
import com.example.palette.core.ui.icon
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.anuncios.ui.FilaPatrocinada
import com.example.palette.features.catalog.domain.model.FilaRecomendada
import com.example.palette.features.catalog.presentation.DiscoverAction
import com.example.palette.features.catalog.presentation.DiscoverState
import com.example.palette.features.catalog.presentation.DiscoverViewModel
import com.example.palette.navigation.BarSpace

// mismo esquema que LogScreen: el root conecta con el viewmodel y la navegacion, la pantalla solo pinta.
// que se recomienda y por que esta en GetRecommendationsUseCase.

@Composable
fun DiscoverScreenRoot(onOpenMedia: (Media) -> Unit, onRegistrar: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        DiscoverViewModel(
            container.getRecommendationsUseCase, container.libraryRepository, container.elegirAnunciosUseCase, container.getEstrenosUseCase, container.metricas,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val abrirEnlace = LocalUriHandler.current

    DiscoverScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is DiscoverAction.OnMediaClicked -> onOpenMedia(action.media)
                DiscoverAction.OnRegistrarClicked -> onRegistrar()
                is DiscoverAction.OnAnuncioClicked -> abrirEnlace.openUri(action.anuncio.enlace)
                is DiscoverAction.OnTrailerClicked -> action.estreno.trailer?.let { abrirEnlace.openUri(it) }
                else -> {}
            }
            viewModel.onAction(action)
        },
        modifier = modifier,
    )
}

@Composable
fun DiscoverScreen(state: DiscoverState, onAction: (DiscoverAction) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = BarSpace)) {
        item { ScreenTitle("Descubrir") }
        // arriba, los estrenos del mes (y, si se contrata, una diapositiva patrocinada al principio)
        if (state.estrenos.isNotEmpty() || state.anuncios.cartel != null) {
            item(key = "estrenos") {
                Spacer(Modifier.height(6.dp))
                CarruselDeEstrenos(state.estrenos, state.anuncios.cartel, onAction)
            }
        }
        when {
            state.cargando && state.filas.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Text("Mezclando colores…", color = Pigment.Graphite, fontSize = 14.sp)
                }
            }
            state.filas.isEmpty() -> item {
                EmptyState(
                    title = if (state.hayFavoritos) "No he encontrado nada parecido" else "Todavía no sé qué te gusta",
                    cuerpo = if (state.hayFavoritos) "Prueba a registrar algo más: cuantos más favoritos, mejores recomendaciones."
                    else "Puntúa algo con más de 3 estrellas y aquí saldrán cosas en la misma línea.",
                    color = Pigment.Violet,
                    semilla = 21,
                ) {
                    PillButton("Registrar algo", { onAction(DiscoverAction.OnRegistrarClicked) }, icon = Icons.Plus)
                }
            }
            else -> {
                state.filas.forEachIndexed { indice, fila ->
                    item { DiscoverRow(fila, indice, onAction) }
                    // hueco 2 de publicidad: una fila patrocinada despues de la primera recomendacion
                    if (indice == 0) state.anuncios.fila?.let { patrocinada ->
                        item(key = "fila-patrocinada") {
                            Spacer(Modifier.height(18.dp))
                            FilaPatrocinada(
                                patrocinada,
                                onVisto = { onAction(DiscoverAction.OnAnuncioVisto(it)) },
                                onClick = { onAction(DiscoverAction.OnAnuncioClicked(it)) },
                                onMediaClick = { onAction(DiscoverAction.OnMediaClicked(it, fila = "patrocinada")) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverRow(fila: FilaRecomendada, indice: Int, onAction: (DiscoverAction) -> Unit) {
    Spacer(Modifier.height(18.dp))
    // el punto de color es el del tipo de contenido de la fila
    SectionTitle(fila.titulo, dot = Pigment.of(fila.contenidos.first().type))
    Spacer(Modifier.height(12.dp))
    MediaRow(fila.contenidos, onClick = { media ->
        onAction(DiscoverAction.OnMediaClicked(media, fila = "${fila.tipo.name.lowercase()}_$indice", posicion = fila.contenidos.indexOf(media)))
    })
}
