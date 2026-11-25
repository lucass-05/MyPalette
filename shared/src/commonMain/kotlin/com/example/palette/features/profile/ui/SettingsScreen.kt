package com.example.palette.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.MediaType
import com.example.palette.core.model.ThemeColors
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.BackBar
import com.example.palette.core.ui.ColorPicker
import com.example.palette.core.ui.Label
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.Swatch
import com.example.palette.core.ui.aGuardado
import com.example.palette.core.ui.contraste
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.recordarSelectorDeFotos
import com.example.palette.core.ui.titular
import com.example.palette.core.ui.toHex
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.profile.domain.model.Perfil
import com.example.palette.features.profile.presentation.SettingsAction
import com.example.palette.features.profile.presentation.SettingsState
import com.example.palette.features.profile.presentation.SettingsViewModel
import com.example.palette.navigation.BarSpace

/** un color que se puede cambiar: su nombre, cual es ahora y como guardarlo. */
private class ColorSlot(val label: String, val color: Color, val icon: ImageVector? = null, val save: (Long) -> Unit)

@Composable
private fun ColorRow(hueco: ColorSlot, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        if (hueco.icon != null) {
            Icon(hueco.icon, contentDescription = null, tint = Pigment.Ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(hueco.label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(hueco.color.toHex(), color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp))
        Swatch(hueco.color, onClick = onClick)
    }
}

@Composable
private fun ColorSheet(hueco: ColorSlot, onDone: () -> Unit) {
    Sheet(onDone) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(hueco.label, style = titular(22.sp))
            Spacer(Modifier.height(14.dp))
            // la clave evita que el selector arrastre el tono del color anterior
            key(hueco.label) { ColorPicker(hueco.color, { hueco.save(it.aGuardado()) }) }
            Spacer(Modifier.height(18.dp))
            PillButton("Listo", onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SettingsScreenRoot(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { SettingsViewModel(container.profileRepository, container.authRepository, container.tutorialRepository, container.metricas) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elegirFoto = recordarSelectorDeFotos { viewModel.onAction(SettingsAction.OnFotoElegida(it)) }

    // el viewmodel avisa por el estado de que el perfil se ha guardado bien y la pantalla se cierra
    LaunchedEffect(state.guardado) { if (state.guardado) onBack() }

    SettingsScreen(
        state = state,
        onAction = { action -> if (action == SettingsAction.OnBackClicked) onBack() else viewModel.onAction(action) },
        onElegirFoto = elegirFoto,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(state: SettingsState, onAction: (SettingsAction) -> Unit, onElegirFoto: () -> Unit, modifier: Modifier = Modifier) {
    val yo = state.yo
    val theme = state.theme
    // que color se esta eligiendo en la hoja del selector. es estado solo de la interfaz
    // (no cambia ningun dato), por eso se queda aqui con remember y no sube al viewmodel
    var editando by remember { mutableStateOf<String?>(null) }

    val cambiarTema = { nuevo: ThemeColors -> onAction(SettingsAction.OnThemeChanged(nuevo)) }
    val huecoFavorito = ColorSlot("Tu color favorito", Color(state.favorite)) { onAction(SettingsAction.OnFavoriteChanged(it)) }
    val huecosApp = listOf(
        ColorSlot("Fondo", Pigment.Paper) { cambiarTema(theme.copy(paper = it)) },
        ColorSlot("Texto", Pigment.Ink) { cambiarTema(theme.copy(ink = it)) },
        ColorSlot("Botones y detalles", Pigment.Accent) { cambiarTema(theme.copy(accent = it)) },
    )
    val huecosTipo = listOf(
        ColorSlot(MediaType.MOVIE.plural, Pigment.Cadmium, MediaType.MOVIE.icon) { cambiarTema(theme.copy(movie = it)) },
        ColorSlot(MediaType.SERIES.plural, Pigment.Cobalt, MediaType.SERIES.icon) { cambiarTema(theme.copy(series = it)) },
        ColorSlot(MediaType.GAME.plural, Pigment.Viridian, MediaType.GAME.icon) { cambiarTema(theme.copy(game = it)) },
        ColorSlot(MediaType.ALBUM.plural, Pigment.Ochre, MediaType.ALBUM.icon) { cambiarTema(theme.copy(album = it)) },
        ColorSlot(MediaType.SONG.plural, Pigment.Violet, MediaType.SONG.icon) { cambiarTema(theme.copy(song = it)) },
        ColorSlot(MediaType.BOOK.plural, Pigment.Magenta, MediaType.BOOK.icon) { cambiarTema(theme.copy(book = it)) },
    )

    Column(modifier.imePadding()) {
        BackBar("Perfil y cuenta", { onAction(SettingsAction.OnBackClicked) }) {
            PillButton(
                if (state.guardando) "Guardando…" else "Guardar",
                { onAction(SettingsAction.OnGuardarClicked) },
                enabled = state.valido && !state.guardando, color = Pigment.Accent, modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = BarSpace)) {
            Text("Perfil", style = titular(21.sp), modifier = Modifier.padding(top = 8.dp))
            state.problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp)) }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(yo.copy(favorite = state.favorite), 76.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    PillButton(if (state.subiendoFoto) "Subiendo…" else "Cambiar foto", onElegirFoto, filled = false, enabled = !state.subiendoFoto)
                    if (yo.photoUrl != null && !state.subiendoFoto) {
                        Text(
                            "Quitar foto",
                            color = Pigment.Graphite, fontSize = 13.sp,
                            modifier = Modifier.clickable { onAction(SettingsAction.OnQuitarFotoClicked) }.padding(horizontal = 6.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Label("Nombre")
            PlainField(state.name, { onAction(SettingsAction.OnNameChanged(it)) }, "Cómo te llamas")
            Label("Usuario")
            PlainField(state.username, { onAction(SettingsAction.OnUsernameChanged(it)) }, "minúsculas y números")
            Label("Sobre ti")
            PlainField(state.bio, { onAction(SettingsAction.OnBioChanged(it)) }, "Una línea que te defina", singleLine = false, minHeight = 80.dp)
            Spacer(Modifier.height(8.dp))
            ColorRow(huecoFavorito) { editando = huecoFavorito.label }
            // el fondo del perfil y que paginas se ven estan en personalizar, junto al resto del aspecto del perfil
            Text("Es el color de tu avatar y del anillo de tus historias.", color = Pigment.Graphite, fontSize = 13.sp)

            Spacer(Modifier.height(30.dp))
            Text("Colores de la app", style = titular(21.sp))
            Text("Se cambian al momento y se quedan guardados en este dispositivo.", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
            huecosApp.forEach { hueco -> ColorRow(hueco) { editando = hueco.label } }
            if (contraste(Pigment.Paper, Pigment.Ink) < 3f) {
                // este aviso va siempre en negro sobre blanco: si no, podria no leerse justo cuando hace falta
                Text(
                    "El fondo y el texto se parecen demasiado y la app cuesta de leer.",
                    color = Color.Black, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 6.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(10.dp),
                )
            }
            Label("Un color para cada tipo de contenido")
            huecosTipo.forEach { hueco -> ColorRow(hueco) { editando = hueco.label } }
            Spacer(Modifier.height(12.dp))
            // negro fijo para que el boton se vea aunque los colores elegidos no se distingan
            PillButton("Volver a los colores originales", { onAction(SettingsAction.OnThemeChanged(ThemeColors())) }, color = Color(0xFF17171A))

            Spacer(Modifier.height(30.dp))
            Text("Ayuda", style = titular(21.sp))
            Text("Un repaso de todo lo que se puede hacer en la app y en tu perfil.", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            PillButton("Ver el tutorial otra vez", { onAction(SettingsAction.OnVerTutorialClicked) }, filled = false)

            Spacer(Modifier.height(30.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Estadísticas de uso", style = titular(21.sp))
                    Text(
                        "Datos anónimos de qué pantallas se usan y qué se busca, para mejorar Palette. Nunca incluyen tu cuenta ni lo que escribes en reseñas o mensajes.",
                        color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Switch(
                    checked = state.metricas,
                    onCheckedChange = { onAction(SettingsAction.OnMetricasChanged(it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pigment.Accent, checkedThumbColor = Color.White),
                )
            }

            Spacer(Modifier.height(30.dp))
            PillButton("Cerrar sesión", { onAction(SettingsAction.OnCerrarSesionClicked) }, filled = false)

            Spacer(Modifier.height(18.dp))
            if (!state.confirmandoBorrado) {
                Text(
                    "Eliminar mi cuenta",
                    color = Pigment.Cadmium, fontSize = 14.sp,
                    modifier = Modifier.clickable { onAction(SettingsAction.OnBorrarCuentaClicked) }.padding(vertical = 8.dp),
                )
            } else {
                Text(
                    "Se borran tu perfil, tus logs, tus pendientes, tus mensajes y tu foto. No se puede deshacer.",
                    fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Cancelar", { onAction(SettingsAction.OnBorrarCuentaCancelado) }, filled = false, enabled = !state.guardando)
                    PillButton(
                        if (state.guardando) "Eliminando…" else "Eliminar para siempre",
                        { onAction(SettingsAction.OnBorrarCuentaConfirmado) },
                        enabled = !state.guardando, color = Pigment.Cadmium,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Los datos y las portadas vienen de TMDB (cine y series), IGDB (videojuegos), Open Library (libros) y iTunes (música). " +
                    "Este producto usa la API de TMDB pero no está avalado ni certificado por TMDB.",
                color = Pigment.Graphite, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }

    (listOf(huecoFavorito) + huecosApp + huecosTipo).firstOrNull { it.label == editando }?.let { hueco ->
        ColorSheet(hueco) { editando = null }
    }
}
