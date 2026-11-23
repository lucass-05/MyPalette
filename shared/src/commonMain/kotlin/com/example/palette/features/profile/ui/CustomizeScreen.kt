package com.example.palette.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.ProfileWidget
import com.example.palette.core.model.WidgetKind
import com.example.palette.core.ui.Chip
import com.example.palette.core.ui.ColorPicker
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.Swatch
import com.example.palette.core.ui.aGuardado
import com.example.palette.core.ui.recordarSelectorDeFotos
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.catalog.ui.MediaPickerSheetRoot
import com.example.palette.features.profile.presentation.CustomizeAction
import com.example.palette.features.profile.presentation.CustomizeState
import com.example.palette.features.profile.presentation.CustomizeViewModel
import com.example.palette.navigation.BarSpace

@Composable
fun CustomizeScreenRoot(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { CustomizeViewModel(container.profileRepository, container.listsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elegirBanner = recordarSelectorDeFotos { viewModel.onAction(CustomizeAction.OnBannerElegido(it)) }

    // al guardar se vuelve al perfil, que ya enseña los cambios
    LaunchedEffect(state.guardado) { if (state.guardado) onClose() }

    CustomizeScreen(state, viewModel::onAction, onElegirBanner = elegirBanner, modifier = modifier)
}

/**
 * modo personalizacion: la cabecera, el fondo, que listas se ven y los bloques del perfil.
 * los cambios se guardan todos juntos al pulsar guardar (o al volver atras).
 */
@Composable
fun CustomizeScreen(state: CustomizeState, onAction: (CustomizeAction) -> Unit, onElegirBanner: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Back, "Guardar y volver", { onAction(CustomizeAction.OnGuardarClicked) })
            Text("Personalizar perfil", style = titular(22.sp), modifier = Modifier.weight(1f).padding(start = 4.dp))
            PillButton("Guardar", { onAction(CustomizeAction.OnGuardarClicked) }, color = Pigment.Accent)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = BarSpace)) {
            item { SeccionBanner(state, onAction, onElegirBanner) }
            item { SeccionFondo(state, onAction) }
            item { SeccionListas(state, onAction) }
            item {
                Titulo("Bloques", "Salen debajo de tu nombre, en este orden. Puedes esconderlos sin borrarlos.")
            }
            items(state.widgets, key = { it.id }) { widget ->
                val posicion = state.widgets.indexOf(widget)
                TarjetaBloque(widget, primero = posicion == 0, ultimo = posicion == state.widgets.lastIndex, onAction = onAction)
            }
            item {
                PillButton(
                    "Añadir bloque", { onAction(CustomizeAction.OnAnadirBloqueClicked) },
                    icon = Icons.Plus, color = Pigment.Accent,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                )
                state.problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
            }
        }
    }

    if (state.eligiendoBloque) HojaNuevoBloque(onAction)
    if (state.eligiendoFondo) HojaFondo(state.background, onAction)
    state.llenando?.let { id ->
        val bloque = state.widgets.firstOrNull { it.id == id }
        MediaPickerSheetRoot(
            titulo = bloque?.title?.ifBlank { null } ?: "Añadir portadas",
            elegidos = bloque?.items.orEmpty().map { it.media.id }.toSet(),
            alElegir = { onAction(CustomizeAction.OnMediaElegida(it)) },
            onClose = { onAction(CustomizeAction.OnSelectorCerrado) },
        )
    }
}

@Composable
private fun Titulo(texto: String, ayuda: String? = null) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)) {
        Text(texto, style = titular(19.sp))
        if (ayuda != null) Text(ayuda, color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SeccionBanner(state: CustomizeState, onAction: (CustomizeAction) -> Unit, onElegirBanner: () -> Unit) {
    val banner = state.yo.bannerUrl
    val fondo = state.background?.let { Color(it) } ?: Pigment.Paper
    Titulo("Portada", "Una foto ancha detrás de tu foto de perfil. Se funde con el fondo.")
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(120.dp).clip(RoundedCornerShape(16.dp)).background(Pigment.Mist)
            .clickable(enabled = !state.subiendoBanner, onClick = onElegirBanner),
        contentAlignment = Alignment.Center,
    ) {
        if (banner != null) {
            AsyncImage(banner, contentDescription = "Tu portada", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to fondo)))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Image, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(26.dp))
                Text("Sin portada", color = Pigment.Graphite, fontSize = 13.sp)
            }
        }
    }
    Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillButton(
            when {
                state.subiendoBanner -> "Subiendo…"
                banner == null -> "Subir portada"
                else -> "Cambiar portada"
            },
            onElegirBanner, filled = false, icon = Icons.Image, enabled = !state.subiendoBanner,
        )
        if (banner != null) PillButton("Quitar", { onAction(CustomizeAction.OnQuitarBannerClicked) }, filled = false, enabled = !state.subiendoBanner)
    }
}

@Composable
private fun SeccionFondo(state: CustomizeState, onAction: (CustomizeAction) -> Unit) {
    Titulo("Fondo", "Todo el que visite tu perfil lo verá con este color.")
    Row(
        Modifier.fillMaxWidth().clickable { onAction(CustomizeAction.OnFondoClicked) }.padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Swatch(state.background?.let { Color(it) } ?: Pigment.Paper) { onAction(CustomizeAction.OnFondoClicked) }
        Spacer(Modifier.width(14.dp))
        Text(if (state.background == null) "Sin color propio: cada uno lo ve con el fondo de su app" else "Color elegido", fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (state.background != null) PillButton("Quitar", { onAction(CustomizeAction.OnFondoChanged(null)) }, filled = false)
    }
}

@Composable
private fun SeccionListas(state: CustomizeState, onAction: (CustomizeAction) -> Unit) {
    Titulo("Listas", "Elige cuáles se ven en tu perfil. Las ocultas solo las ves tú.")
    if (state.listas.isEmpty()) {
        Text("Todavía no tienes listas. Créalas con el + de tu perfil, junto a Favoritos y Recientes.", color = Pigment.Graphite, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 20.dp))
    }
    state.listas.forEach { vista ->
        val lista = vista.lista
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (lista.kind == ListKind.REVIEWS) Icons.Star else Icons.ListBullets, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(lista.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Chip(
                if (lista.visible) "Visible" else "Oculta", lista.visible, Pigment.Ink,
                { onAction(CustomizeAction.OnListaVisibleToggled(lista.id)) },
                icon = if (lista.visible) Icons.Eye else Icons.EyeOff,
            )
        }
    }
}

/** un bloque en el editor: su tipo, los botones para moverlo, esconderlo o borrarlo, y sus campos. */
@Composable
private fun TarjetaBloque(widget: ProfileWidget, primero: Boolean, ultimo: Boolean, onAction: (CustomizeAction) -> Unit) {
    val forma = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(forma).border(1.dp, Pigment.Line, forma).padding(vertical = 6.dp)) {
        Row(Modifier.padding(start = 14.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(widget.kind.icono, contentDescription = null, tint = Pigment.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(widget.kind.nombre + if (widget.hidden) " · oculto" else "", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Pigment.Graphite, modifier = Modifier.weight(1f))
            if (!primero) IconAction(Icons.ArrowUp, "Subir", { onAction(CustomizeAction.OnSubirClicked(widget.id)) })
            if (!ultimo) IconAction(Icons.ArrowDown, "Bajar", { onAction(CustomizeAction.OnBajarClicked(widget.id)) })
            IconAction(
                if (widget.hidden) Icons.EyeOff else Icons.Eye,
                if (widget.hidden) "Mostrar en el perfil" else "Esconder del perfil",
                { onAction(CustomizeAction.OnOcultarClicked(widget.id)) },
                tint = if (widget.hidden) Pigment.Graphite else Pigment.Ink,
            )
            IconAction(Icons.Trash, "Borrar bloque", { onAction(CustomizeAction.OnBorrarBloqueClicked(widget.id)) }, tint = Pigment.Cadmium)
        }
        Column(Modifier.padding(horizontal = 14.dp).alpha(if (widget.hidden) 0.5f else 1f)) {
            PlainField(widget.title, { onAction(CustomizeAction.OnTituloChanged(widget.id, it)) }, "Título (opcional)")
            when (widget.kind) {
                WidgetKind.TEXT -> {
                    Spacer(Modifier.height(8.dp))
                    PlainField(widget.text, { onAction(CustomizeAction.OnTextoChanged(widget.id, it)) }, "Escribe lo que quieras contar", singleLine = false, minHeight = 96.dp)
                }
                WidgetKind.SHELF -> EditorPortadas(widget, onAction)
                WidgetKind.DIARY -> Text(
                    "Enseña los últimos días en los que registraste algo, con lo que registraste cada día.",
                    color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EditorPortadas(widget: ProfileWidget, onAction: (CustomizeAction) -> Unit) {
    Spacer(Modifier.height(10.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(widget.items, key = { it.media.id }) { item ->
            Box {
                // tocar la portada cambia como se ve: mancha de pintura o portada con esquinas redondeadas
                PortadaDeBloque(item, 116.dp, Modifier.clickable { onAction(CustomizeAction.OnPortadaTocada(widget.id, item.media.id)) })
                Insignia(if (item.blob) Icons.Droplet else Icons.Square, Modifier.align(Alignment.BottomStart).padding(4.dp))
                Insignia(Icons.Close, Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    onAction(CustomizeAction.OnQuitarPortadaClicked(widget.id, item.media.id))
                }
            }
        }
        item {
            Box(
                Modifier.size(width = 78.dp, height = 116.dp).clip(RoundedCornerShape(14.dp)).background(Pigment.Mist)
                    .clickable { onAction(CustomizeAction.OnAnadirPortadasClicked(widget.id)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Plus, contentDescription = "Añadir portadas", tint = Pigment.Ink, modifier = Modifier.size(22.dp))
            }
        }
    }
    Text(
        if (widget.items.isEmpty()) "Añade portadas con el +." else "Toca una portada para cambiar entre mancha de pintura y portada.",
        color = Pigment.Graphite, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Insignia(icon: ImageVector, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val base = modifier.size(24.dp).clip(CircleShape).background(Pigment.Paper)
    Box(if (onClick != null) base.clickable(onClick = onClick) else base, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Pigment.Ink, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun HojaNuevoBloque(onAction: (CustomizeAction) -> Unit) {
    Sheet({ onAction(CustomizeAction.OnAnadirBloqueCerrado) }) {
        Text("Añadir bloque", style = titular(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(8.dp))
        WidgetKind.entries.forEach { kind ->
            Row(
                Modifier.fillMaxWidth().clickable { onAction(CustomizeAction.OnBloqueElegido(kind)) }.padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Pigment.Mist), contentAlignment = Alignment.Center) {
                    Icon(kind.icono, contentDescription = null, tint = Pigment.Accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(kind.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(kind.descripcion, color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun HojaFondo(background: Long?, onAction: (CustomizeAction) -> Unit) {
    Sheet({ onAction(CustomizeAction.OnFondoCerrado) }) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text("Fondo de tu perfil", style = titular(22.sp))
            Spacer(Modifier.height(14.dp))
            ColorPicker(background?.let { Color(it) } ?: Pigment.Paper, { onAction(CustomizeAction.OnFondoChanged(it.aGuardado())) })
            Spacer(Modifier.height(18.dp))
            PillButton("Listo", { onAction(CustomizeAction.OnFondoCerrado) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private val WidgetKind.nombre: String
    get() = when (this) {
        WidgetKind.TEXT -> "Texto"
        WidgetKind.SHELF -> "Portadas"
        WidgetKind.DIARY -> "Diario"
    }

private val WidgetKind.descripcion: String
    get() = when (this) {
        WidgetKind.TEXT -> "Un título y lo que quieras contar."
        WidgetKind.SHELF -> "Una fila de portadas elegidas por ti, como manchas o como portadas."
        WidgetKind.DIARY -> "Los últimos días en los que registraste algo, con su fecha en grande."
    }

private val WidgetKind.icono: ImageVector
    get() = when (this) {
        WidgetKind.TEXT -> Icons.Type
        WidgetKind.SHELF -> Icons.Image
        WidgetKind.DIARY -> Icons.Calendar
    }
