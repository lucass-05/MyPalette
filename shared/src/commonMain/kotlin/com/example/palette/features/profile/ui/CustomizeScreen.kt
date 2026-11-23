package com.example.palette.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import com.example.palette.features.profile.presentation.Pagina
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
fun CustomizeScreenRoot(onClose: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { CustomizeViewModel(container.profileRepository, container.listsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elegirBanner = recordarSelectorDeFotos { viewModel.onAction(CustomizeAction.OnBannerElegido(it)) }

    // al guardar se vuelve al perfil, que ya enseña los cambios
    LaunchedEffect(state.guardado) { if (state.guardado) onClose() }

    CustomizeScreen(
        state,
        onAction = { action ->
            viewModel.onAction(action)
            // los ajustes de la cuenta son otra pantalla; el viewmodel guarda antes lo que haya
            if (action == CustomizeAction.OnAjustesClicked) onOpenSettings()
        },
        onElegirBanner = elegirBanner,
        modifier = modifier,
    )
}

/**
 * modo personalizacion: portada, fondo, que paginas se ven y como, y los bloques de la paleta.
 * desde aqui tambien se llega a los ajustes de la cuenta. se guarda todo junto al pulsar guardar (o la flecha).
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
            item {
                Row(
                    Modifier.fillMaxWidth().clickable { onAction(CustomizeAction.OnAjustesClicked) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Pigment.Mist), contentAlignment = Alignment.Center) {
                        Icon(Icons.Settings, contentDescription = null, tint = Pigment.Ink, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Perfil y cuenta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Nombre, foto, colores de la app, cerrar sesión", color = Pigment.Graphite, fontSize = 13.sp)
                    }
                    Icon(Icons.ChevronRight, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(18.dp))
                }
            }
            item { SeccionBanner(state, onAction, onElegirBanner) }
            item { SeccionFondo(state, onAction) }
            item { SeccionPaginas(state, onAction) }
            item {
                Titulo("Tu paleta", "Es la página que se abre al entrar en tu perfil. Pon los bloques que quieras, en el orden que quieras: todos son opcionales.")
            }
            items(state.widgets, key = { it.id }) { widget ->
                val posicion = state.widgets.indexOf(widget)
                TarjetaBloque(widget, state, primero = posicion == 0, ultimo = posicion == state.widgets.lastIndex, onAction = onAction)
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

    if (state.eligiendoBloque) HojaNuevoBloque(hayListas = state.listas.isNotEmpty(), onAction = onAction)
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

/** las paginas del perfil: si los demas las ven y si sus portadas van como manchas o como recuadros. */
@Composable
private fun SeccionPaginas(state: CustomizeState, onAction: (CustomizeAction) -> Unit) {
    Titulo("Páginas", "Salen arriba de tu perfil, después de tu paleta. Elige cuáles ven los demás y cómo se pintan.")
    val paginas = listOf<Pair<Pagina, String>>(Pagina.Favoritos to "Favoritos", Pagina.Diario to "Diario", Pagina.Pendientes to "Pendientes") +
        state.listas.map { Pagina.Lista(it.lista.id) to it.lista.name }
    paginas.forEach { (pagina, nombre) ->
        val visible = state.visible(pagina)
        val manchas = state.enManchas(pagina)
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Chip(
                    if (visible) "La ven todos" else "Solo tú", visible, Pigment.Ink,
                    { onAction(CustomizeAction.OnPaginaVisibleToggled(pagina)) },
                    icon = if (visible) Icons.Eye else Icons.Lock,
                )
            }
            Spacer(Modifier.height(8.dp))
            ElegirForma(manchas) { onAction(CustomizeAction.OnPaginaManchasChanged(pagina, it)) }
        }
    }
}

/** manchas de pintura o recuadros: dos chips, uno marcado. */
@Composable
private fun ElegirForma(manchas: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Manchas", manchas, Pigment.Accent, { alCambiar(true) }, icon = Icons.Droplet)
        Chip("Recuadros", !manchas, Pigment.Accent, { alCambiar(false) }, icon = Icons.Square)
    }
}

/** cuantos elementos como mucho: un - y un + a los lados del numero. */
@Composable
private fun ElegirCantidad(texto: String, cantidad: Int, alCambiar: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(texto, fontSize = 14.sp, modifier = Modifier.weight(1f))
        IconAction(Icons.Minus, "Menos", { alCambiar(cantidad - 1) }, tint = if (cantidad > 1) Pigment.Ink else Pigment.Line)
        Text("$cantidad", style = titular(20.sp), modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
        IconAction(Icons.Plus, "Más", { alCambiar(cantidad + 1) })
    }
}

/** un bloque en el editor: su tipo, los botones para moverlo, esconderlo o borrarlo, y sus campos. */
@Composable
private fun TarjetaBloque(widget: ProfileWidget, state: CustomizeState, primero: Boolean, ultimo: Boolean, onAction: (CustomizeAction) -> Unit) {
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
            PlainField(
                widget.title, { onAction(CustomizeAction.OnTituloChanged(widget.id, it)) },
                if (widget.kind == WidgetKind.LIST) "Título (si no, el de la lista)" else "Título (opcional)",
            )
            Spacer(Modifier.height(8.dp))
            when (widget.kind) {
                WidgetKind.TEXT -> {
                    PlainField(widget.text, { onAction(CustomizeAction.OnTextoChanged(widget.id, it)) }, "Escribe lo que quieras contar", singleLine = false, minHeight = 96.dp)
                }
                WidgetKind.SHELF -> EditorPortadas(widget, onAction)
                WidgetKind.FAVORITES, WidgetKind.WATCHLIST -> {
                    ElegirCantidad("Cuántos enseña, como mucho", widget.count) { onAction(CustomizeAction.OnCantidadChanged(widget.id, it)) }
                    ElegirForma(widget.blob) { onAction(CustomizeAction.OnManchasChanged(widget.id, it)) }
                }
                WidgetKind.DIARY -> {
                    ElegirCantidad("Cuántos días enseña, como mucho", widget.count) { onAction(CustomizeAction.OnCantidadChanged(widget.id, it)) }
                    ElegirForma(widget.blob) { onAction(CustomizeAction.OnManchasChanged(widget.id, it)) }
                }
                WidgetKind.LIST -> {
                    if (state.listas.isEmpty()) {
                        Text("No tienes listas. Créalas con el + de tu perfil.", color = Pigment.Graphite, fontSize = 13.sp)
                    } else {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.listas.forEach { vista ->
                                Chip(vista.lista.name, widget.listId == vista.lista.id, Pigment.Ink, { onAction(CustomizeAction.OnListaDelBloqueChanged(widget.id, vista.lista.id)) })
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    ElegirCantidad("Cuántos enseña, como mucho", widget.count) { onAction(CustomizeAction.OnCantidadChanged(widget.id, it)) }
                    ElegirForma(widget.blob) { onAction(CustomizeAction.OnManchasChanged(widget.id, it)) }
                }
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
private fun HojaNuevoBloque(hayListas: Boolean, onAction: (CustomizeAction) -> Unit) {
    Sheet({ onAction(CustomizeAction.OnAnadirBloqueCerrado) }) {
        Text("Añadir bloque", style = titular(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(8.dp))
        WidgetKind.entries.forEach { kind ->
            // un bloque de lista necesita alguna lista que enseñar
            val disponible = kind != WidgetKind.LIST || hayListas
            Row(
                Modifier.fillMaxWidth().alpha(if (disponible) 1f else 0.45f)
                    .clickable(enabled = disponible) { onAction(CustomizeAction.OnBloqueElegido(kind)) }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Pigment.Mist), contentAlignment = Alignment.Center) {
                    Icon(kind.icono, contentDescription = null, tint = Pigment.Accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(kind.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(if (disponible) kind.descripcion else "Crea antes una lista con el + de tu perfil.", color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp)
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
        WidgetKind.FAVORITES -> "Favoritos"
        WidgetKind.DIARY -> "Diario"
        WidgetKind.WATCHLIST -> "Pendientes"
        WidgetKind.LIST -> "Lista"
        WidgetKind.SHELF -> "Portadas elegidas"
    }

private val WidgetKind.descripcion: String
    get() = when (this) {
        WidgetKind.TEXT -> "Un título y lo que quieras contar."
        WidgetKind.FAVORITES -> "Tus reseñas con más nota, de la mejor a la peor."
        WidgetKind.DIARY -> "Los últimos días en los que registraste algo, con su fecha en grande."
        WidgetKind.WATCHLIST -> "Lo que tienes guardado para más adelante."
        WidgetKind.LIST -> "Lo que hay en una de tus listas."
        WidgetKind.SHELF -> "Una fila de portadas que eliges tú, una a una."
    }

private val WidgetKind.icono: ImageVector
    get() = when (this) {
        WidgetKind.TEXT -> Icons.Type
        WidgetKind.FAVORITES -> Icons.Star
        WidgetKind.DIARY -> Icons.Calendar
        WidgetKind.WATCHLIST -> Icons.Bookmark
        WidgetKind.LIST -> Icons.ListBullets
        WidgetKind.SHELF -> Icons.Image
    }
