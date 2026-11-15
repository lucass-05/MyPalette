package com.example.palette.features.profile.ui

import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.MediaType
import com.example.palette.core.model.ThemeColors
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.BackBar
import com.example.palette.core.ui.ColorPicker
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.Swatch
import com.example.palette.core.ui.aGuardado
import com.example.palette.core.ui.avatarCuadrado
import com.example.palette.core.ui.contraste
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.recordarSelectorDeFotos
import com.example.palette.core.ui.titular
import com.example.palette.core.ui.toHex
import com.example.palette.core.util.ahora
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Screen


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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun Label(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

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
fun SettingsScreen(modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val me = almacen.me
    var name by remember { mutableStateOf(me.name) }
    var username by remember { mutableStateOf(me.username) }
    var bio by remember { mutableStateOf(me.bio) }
    var favorite by remember { mutableLongStateOf(me.favorite) }
    var background by remember { mutableStateOf(me.background) }
    var borrando by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var problema by remember { mutableStateOf<String?>(null) }
    val ambito = rememberCoroutineScope()
    var subiendo by remember { mutableStateOf(false) }
    // la foto se recorta a cuadrado y se reduce antes de subirla
    val elegirFoto = recordarSelectorDeFotos { foto ->
        subiendo = true
        ambito.launch {
            problema = try {
                almacen.ponerFoto(withContext(Dispatchers.Default) { avatarCuadrado(foto) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "No se ha podido leer esa imagen. Prueba con un JPG o un PNG."
            }
            subiendo = false
        }
    }
    val valido = name.isNotBlank() && username.length >= 2

    val theme = almacen.estado.theme
    val huecoFavorito = ColorSlot("Tu color favorito", Color(favorite)) { favorite = it }
    val huecoFondo = ColorSlot("Fondo de tu perfil", background?.let { Color(it) } ?: Pigment.Paper) { background = it }
    val huecosApp = listOf(
        ColorSlot("Fondo", Pigment.Paper) { app.ponerTema(theme.copy(paper = it)) },
        ColorSlot("Texto", Pigment.Ink) { app.ponerTema(theme.copy(ink = it)) },
        ColorSlot("Botones y detalles", Pigment.Accent) { app.ponerTema(theme.copy(accent = it)) },
    )
    val huecosTipo = listOf(
        ColorSlot(MediaType.MOVIE.plural, Pigment.Cadmium, MediaType.MOVIE.icon) { app.ponerTema(theme.copy(movie = it)) },
        ColorSlot(MediaType.SERIES.plural, Pigment.Cobalt, MediaType.SERIES.icon) { app.ponerTema(theme.copy(series = it)) },
        ColorSlot(MediaType.GAME.plural, Pigment.Viridian, MediaType.GAME.icon) { app.ponerTema(theme.copy(game = it)) },
        ColorSlot(MediaType.ALBUM.plural, Pigment.Ochre, MediaType.ALBUM.icon) { app.ponerTema(theme.copy(album = it)) },
        ColorSlot(MediaType.SONG.plural, Pigment.Violet, MediaType.SONG.icon) { app.ponerTema(theme.copy(song = it)) },
        ColorSlot(MediaType.BOOK.plural, Pigment.Magenta, MediaType.BOOK.icon) { app.ponerTema(theme.copy(book = it)) },
    )

    Column(modifier.imePadding()) {
        BackBar("Ajustes", { app.navegacion.volver() }) {
            PillButton(
                if (guardando) "Guardando…" else "Guardar",
                {
                    guardando = true
                    ambito.launch {
                        problema = almacen.guardarPerfil(name, username, bio, favorite, background)
                        guardando = false
                        if (problema == null) app.navegacion.volver()
                    }
                },
                enabled = valido && !guardando, color = Pigment.Accent, modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = BarSpace)) {
            Text("Perfil", style = titular(21.sp), modifier = Modifier.padding(top = 8.dp))
            problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp)) }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(me.copy(favorite = favorite), 76.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    PillButton(if (subiendo) "Subiendo…" else "Cambiar foto", elegirFoto, filled = false, enabled = !subiendo)
                    if (me.photoUrl != null && !subiendo) {
                        Text(
                            "Quitar foto",
                            color = Pigment.Graphite, fontSize = 13.sp,
                            modifier = Modifier.clickable { ambito.launch { problema = almacen.ponerFoto(null) } }.padding(horizontal = 6.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Label("Nombre")
            PlainField(name, { name = it }, "Cómo te llamas")
            Label("Usuario")
            PlainField(username, { username = PaletteStore.limpiarUsuario(it) }, "minúsculas y números")
            Label("Sobre ti")
            PlainField(bio, { bio = it.take(150) }, "Una línea que te defina", singleLine = false, minHeight = 80.dp)
            Spacer(Modifier.height(8.dp))
            ColorRow(huecoFavorito) { editando = huecoFavorito.label }
            Text("Es el color de tu avatar y del anillo de tus historias.", color = Pigment.Graphite, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            ColorRow(huecoFondo) { editando = huecoFondo.label }
            Text(
                if (background == null) "Ahora tu perfil se ve con el fondo que tenga cada uno en su app. Si eliges un color, lo verá igual todo el que te visite."
                else "Todo el que visite tu perfil lo verá con este fondo. Toca aquí para quitarlo.",
                color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp,
                modifier = Modifier.clickable(enabled = background != null) { background = null },
            )

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Pendientes a la vista", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Quien visite tu perfil podrá ver tu lista de pendientes.", color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp)
                }
                Switch(
                    checked = me.watchlistPublic,
                    onCheckedChange = { almacen.ponerPendientesPublicos(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Pigment.Accent, checkedThumbColor = Pigment.Accent.legible(),
                        uncheckedTrackColor = Pigment.Mist, uncheckedBorderColor = Pigment.Line, uncheckedThumbColor = Pigment.Graphite,
                    ),
                )
            }

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
            PillButton("Volver a los colores originales", { app.ponerTema(ThemeColors()) }, color = Color(0xFF17171A))

            Spacer(Modifier.height(30.dp))
            PillButton(
                "Cerrar sesión",
                {
                    app.navegacion.irA(Screen.HOME)
                    almacen.cerrarSesion()
                },
                filled = false,
            )

            Spacer(Modifier.height(18.dp))
            if (!borrando) {
                Text(
                    "Eliminar mi cuenta",
                    color = Pigment.Cadmium, fontSize = 14.sp,
                    modifier = Modifier.clickable { borrando = true }.padding(vertical = 8.dp),
                )
            } else {
                Text(
                    "Se borran tu perfil, tus logs, tus pendientes, tus mensajes y tu foto. No se puede deshacer.",
                    fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Cancelar", { borrando = false }, filled = false, enabled = !guardando)
                    PillButton(
                        if (guardando) "Eliminando…" else "Eliminar para siempre",
                        {
                            guardando = true
                            ambito.launch {
                                problema = almacen.borrarCuenta()
                                guardando = false
                                if (problema == null) app.navegacion.irA(Screen.HOME) else borrando = false
                            }
                        },
                        enabled = !guardando, color = Pigment.Cadmium,
                    )
                }
                problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Los datos y las portadas vienen de TMDB (cine y series), IGDB (videojuegos), Open Library (libros) y iTunes (música). " +
                    "Este producto usa la API de TMDB pero no está avalado ni certificado por TMDB.",
                color = Pigment.Graphite, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }

    (listOf(huecoFavorito, huecoFondo) + huecosApp + huecosTipo).firstOrNull { it.label == editando }?.let { hueco ->
        ColorSheet(hueco) { editando = null }
    }
}
