package com.example.palette.features.lists.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.ui.BackBar
import com.example.palette.core.ui.Chip
import com.example.palette.core.ui.Cover
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.SearchField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.SheetRow
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.proporcion
import com.example.palette.core.ui.subtitulo
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.catalog.ui.Marca
import com.example.palette.features.catalog.ui.MediaPickerSheetRoot
import com.example.palette.features.lists.presentation.ListAction
import com.example.palette.features.lists.presentation.ListState
import com.example.palette.features.lists.presentation.ListViewModel
import com.example.palette.navigation.BarSpace

@Composable
fun ListScreenRoot(
    listId: String,
    abrirSelector: Boolean,
    onBack: () -> Unit,
    onOpenMedia: (Media) -> Unit,
    onOpenReview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ListViewModel(listId, abrirSelector, container.listsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // si la lista se borra (desde aqui o desde otro dispositivo) no queda nada que enseñar
    LaunchedEffect(state.existe) { if (!state.existe) onBack() }

    ListScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ListAction.OnBackClicked -> onBack()
                is ListAction.OnMediaClicked -> onOpenMedia(action.media)
                is ListAction.OnResenaClicked -> onOpenReview(action.logId)
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

/** una lista: sus contenidos o reseñas. si es mia, desde aqui se añade, se quita, se renombra y se borra. */
@Composable
fun ListScreen(state: ListState, onAction: (ListAction) -> Unit, modifier: Modifier = Modifier) {
    val vista = state.vista ?: return
    val lista = vista.lista
    val propia = vista.propia
    val deResenas = lista.kind == ListKind.REVIEWS

    Column(modifier) {
        BackBar(lista.name, { onAction(ListAction.OnBackClicked) }) {
            if (propia) IconAction(Icons.Pencil, "Cambiar el nombre", { onAction(ListAction.OnRenombrarClicked) })
        }
        BoxWithConstraints {
            val porFila = (maxWidth / 132.dp).toInt().coerceIn(3, 12)
            LazyColumn(contentPadding = PaddingValues(bottom = BarSpace)) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        val renombrando = state.renombrando
                        if (renombrando != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlainField(renombrando, { onAction(ListAction.OnNombreChanged(it)) }, "Nombre de la lista", Modifier.weight(1f))
                                PillButton("Guardar", { onAction(ListAction.OnNombreGuardado) }, color = Pigment.Accent, enabled = renombrando.isNotBlank())
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(lista.name, style = titular(28.sp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            (if (deResenas) "Lista de reseñas" else "Lista de contenido") + " · " + cuantos(lista.size, deResenas),
                            color = Pigment.Graphite, fontSize = 14.sp,
                        )
                        if (propia) {
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                PillButton("Añadir", { onAction(ListAction.OnAnadirClicked) }, color = Pigment.Accent, icon = Icons.Plus)
                                Chip(
                                    if (lista.visible) "Se ve en tu perfil" else "Solo la ves tú",
                                    lista.visible, Pigment.Ink, { onAction(ListAction.OnVisibleToggled) },
                                    icon = if (lista.visible) Icons.Eye else Icons.Lock,
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }

                val vacia = if (deResenas) vista.resenas.isEmpty() else lista.media.isEmpty()
                if (vacia) item {
                    EmptyState(
                        title = "Lista vacía",
                        cuerpo = when {
                            !propia -> "Todavía no ha añadido nada."
                            deResenas -> "Añade reseñas tuyas buscando por el nombre del contenido."
                            else -> "Añade pelis, series, juegos, música o libros con el buscador."
                        },
                        color = Pigment.Ochre, semilla = 70,
                    ) {
                        if (propia) PillButton("Añadir", { onAction(ListAction.OnAnadirClicked) }, icon = Icons.Plus)
                    }
                }

                if (deResenas) {
                    items(vista.resenas, key = { it.id }) { log ->
                        ResenaDeLista(log, propia, { onAction(ListAction.OnResenaClicked(log.id)) }, { onAction(ListAction.OnResenaToggled(log.id)) })
                    }
                } else {
                    items(lista.media.chunked(porFila), key = { fila -> fila.joinToString("|") { it.id } }) { fila ->
                        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            fila.forEach { media ->
                                PortadaDeLista(media, propia, Modifier.weight(1f), { onAction(ListAction.OnMediaClicked(media)) }, { onAction(ListAction.OnMediaToggled(media)) })
                            }
                            repeat(porFila - fila.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                if (propia) item {
                    Spacer(Modifier.height(24.dp))
                    if (state.confirmandoBorrado) {
                        Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Se borra la lista, no lo que hay dentro.", fontSize = 14.sp, modifier = Modifier.weight(1f))
                            PillButton("Cancelar", { onAction(ListAction.OnBorrarCancelado) }, filled = false)
                            PillButton("Borrar", { onAction(ListAction.OnBorrarConfirmado) }, color = Pigment.Cadmium)
                        }
                    } else {
                        SheetRow(Icons.Trash, "Borrar la lista", { onAction(ListAction.OnBorrarClicked) }, tint = Pigment.Cadmium)
                    }
                }
            }
        }
    }

    if (state.eligiendo) {
        if (deResenas) {
            ResenasPickerSheet(state, onAction)
        } else {
            MediaPickerSheetRoot(
                titulo = "Añadir a la lista",
                elegidos = lista.media.map { it.id }.toSet(),
                alElegir = { onAction(ListAction.OnMediaToggled(it)) },
                onClose = { onAction(ListAction.OnSelectorCerrado) },
            )
        }
    }
}

private fun cuantos(n: Int, deResenas: Boolean): String = when {
    n == 0 -> "vacía"
    deResenas -> if (n == 1) "1 reseña" else "$n reseñas"
    else -> if (n == 1) "1 elemento" else "$n elementos"
}

@Composable
internal fun PortadaDeLista(media: Media, propia: Boolean, modifier: Modifier, onClick: () -> Unit, onQuitar: () -> Unit) {
    Column(modifier.clickable(onClick = onClick)) {
        Box {
            Cover(media, Modifier.fillMaxWidth().aspectRatio(media.type.proporcion))
            if (propia) BotonQuitar(Modifier.align(Alignment.TopEnd).padding(5.dp), onQuitar)
        }
        Spacer(Modifier.height(6.dp))
        Text(media.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, lineHeight = 15.sp, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ResenaDeLista(log: LogEntry, propia: Boolean, onClick: () -> Unit, onQuitar: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)) {
        CoverByHeight(log.media, 96.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(log.media.title, style = titular(18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(log.media.subtitulo(), color = Pigment.Graphite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Stars(log.stars, size = 14.dp)
            if (log.review.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(log.review, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (propia) IconAction(Icons.Close, "Quitar de la lista", onQuitar, tint = Pigment.Graphite)
    }
}

@Composable
private fun BotonQuitar(modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.size(26.dp).clip(CircleShape).background(Pigment.Paper).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Close, contentDescription = "Quitar de la lista", tint = Pigment.Ink, modifier = Modifier.size(14.dp))
    }
}

/** buscador de mis reseñas por el nombre del contenido; tocar una la mete o la saca de la lista. */
@Composable
private fun ResenasPickerSheet(state: ListState, onAction: (ListAction) -> Unit) {
    val dentro = state.vista?.lista?.logIds.orEmpty().toSet()
    Sheet({ onAction(ListAction.OnSelectorCerrado) }) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Añadir reseñas", style = titular(22.sp), modifier = Modifier.weight(1f))
            PillButton("Hecho", { onAction(ListAction.OnSelectorCerrado) }, color = Pigment.Accent)
        }
        Spacer(Modifier.height(12.dp))
        SearchField(state.filtroResenas, { onAction(ListAction.OnFiltroChanged(it)) }, "Busca entre tus reseñas", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        val resenas = state.resenasFiltradas
        LazyColumn(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 520.dp)) {
            if (resenas.isEmpty()) item {
                Text(
                    if (state.misResenas.isEmpty()) "Todavía no has reseñado nada." else "Ninguna reseña tuya coincide.",
                    color = Pigment.Graphite, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(resenas, key = { it.id }) { log ->
                Row(
                    Modifier.fillMaxWidth().clickable { onAction(ListAction.OnResenaToggled(log.id)) }.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.width(50.dp), horizontalArrangement = Arrangement.Center) { CoverByHeight(log.media, 62.dp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(log.media.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Stars(log.stars, size = 12.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Marca(log.id in dentro)
                }
            }
        }
    }
}
