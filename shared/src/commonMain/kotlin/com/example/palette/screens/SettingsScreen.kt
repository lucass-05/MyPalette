package com.example.palette.screens

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
import com.example.palette.data.MediaType
import com.example.palette.data.ThemeColors
import com.example.palette.navigation.BarSpace
import com.example.palette.data.PaletteStore
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Screen
import com.example.palette.ui.Avatar
import com.example.palette.ui.BackBar
import com.example.palette.ui.ColorPicker
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.PlainField
import com.example.palette.ui.Swatch
import com.example.palette.ui.contrast
import com.example.palette.ui.display
import com.example.palette.ui.icon
import com.example.palette.ui.readable
import com.example.palette.ui.rememberPhotoPicker
import com.example.palette.ui.squareAvatar
import com.example.palette.ui.toHex
import com.example.palette.ui.toStored
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
private fun ColorRow(slot: ColorSlot, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        if (slot.icon != null) {
            Icon(slot.icon, contentDescription = null, tint = Pigment.Ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(slot.label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(slot.color.toHex(), color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp))
        Swatch(slot.color, onClick = onClick)
    }
}

@Composable
private fun ColorSheet(slot: ColorSlot, onDone: () -> Unit) {
    Sheet(onDone) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(slot.label, style = display(22.sp))
            Spacer(Modifier.height(14.dp))
            // la clave evita que el selector arrastre el tono del color anterior
            key(slot.label) { ColorPicker(slot.color, { slot.save(it.toStored()) }) }
            Spacer(Modifier.height(18.dp))
            PillButton("Listo", onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val store = palette.store
    val me = store.me
    var name by remember { mutableStateOf(me.name) }
    var username by remember { mutableStateOf(me.username) }
    var bio by remember { mutableStateOf(me.bio) }
    var favorite by remember { mutableLongStateOf(me.favorite) }
    var background by remember { mutableStateOf(me.background) }
    var deleting by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    // la foto se recorta a cuadrado y se reduce antes de subirla
    val pickPhoto = rememberPhotoPicker { photo ->
        uploading = true
        scope.launch {
            problem = try {
                store.setPhoto(withContext(Dispatchers.Default) { squareAvatar(photo) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "No se ha podido leer esa imagen. Prueba con un JPG o un PNG."
            }
            uploading = false
        }
    }
    val valid = name.isNotBlank() && username.length >= 2

    val theme = store.data.theme
    val favoriteSlot = ColorSlot("Tu color favorito", Color(favorite)) { favorite = it }
    val backgroundSlot = ColorSlot("Fondo de tu perfil", background?.let { Color(it) } ?: Pigment.Paper) { background = it }
    val appSlots = listOf(
        ColorSlot("Fondo", Pigment.Paper) { palette.setTheme(theme.copy(paper = it)) },
        ColorSlot("Texto", Pigment.Ink) { palette.setTheme(theme.copy(ink = it)) },
        ColorSlot("Botones y detalles", Pigment.Accent) { palette.setTheme(theme.copy(accent = it)) },
    )
    val typeSlots = listOf(
        ColorSlot(MediaType.MOVIE.plural, Pigment.Cadmium, MediaType.MOVIE.icon) { palette.setTheme(theme.copy(movie = it)) },
        ColorSlot(MediaType.SERIES.plural, Pigment.Cobalt, MediaType.SERIES.icon) { palette.setTheme(theme.copy(series = it)) },
        ColorSlot(MediaType.GAME.plural, Pigment.Viridian, MediaType.GAME.icon) { palette.setTheme(theme.copy(game = it)) },
        ColorSlot(MediaType.ALBUM.plural, Pigment.Ochre, MediaType.ALBUM.icon) { palette.setTheme(theme.copy(album = it)) },
        ColorSlot(MediaType.SONG.plural, Pigment.Violet, MediaType.SONG.icon) { palette.setTheme(theme.copy(song = it)) },
        ColorSlot(MediaType.BOOK.plural, Pigment.Magenta, MediaType.BOOK.icon) { palette.setTheme(theme.copy(book = it)) },
    )

    Column(modifier.imePadding()) {
        BackBar("Ajustes", { palette.nav.back() }) {
            PillButton(
                if (saving) "Guardando…" else "Guardar",
                {
                    saving = true
                    scope.launch {
                        problem = store.saveProfile(name, username, bio, favorite, background)
                        saving = false
                        if (problem == null) palette.nav.back()
                    }
                },
                enabled = valid && !saving, color = Pigment.Accent, modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = BarSpace)) {
            Text("Perfil", style = display(21.sp), modifier = Modifier.padding(top = 8.dp))
            problem?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp)) }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(me.copy(favorite = favorite), 76.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    PillButton(if (uploading) "Subiendo…" else "Cambiar foto", pickPhoto, filled = false, enabled = !uploading)
                    if (me.photoUrl != null && !uploading) {
                        Text(
                            "Quitar foto",
                            color = Pigment.Graphite, fontSize = 13.sp,
                            modifier = Modifier.clickable { scope.launch { problem = store.setPhoto(null) } }.padding(horizontal = 6.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Label("Nombre")
            PlainField(name, { name = it }, "Cómo te llamas")
            Label("Usuario")
            PlainField(username, { username = PaletteStore.cleanUsername(it) }, "minúsculas y números")
            Label("Sobre ti")
            PlainField(bio, { bio = it.take(150) }, "Una línea que te defina", singleLine = false, minHeight = 80.dp)
            Spacer(Modifier.height(8.dp))
            ColorRow(favoriteSlot) { editing = favoriteSlot.label }
            Text("Es el color de tu avatar y del anillo de tus historias.", color = Pigment.Graphite, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            ColorRow(backgroundSlot) { editing = backgroundSlot.label }
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
                    onCheckedChange = { store.setWatchlistPublic(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Pigment.Accent, checkedThumbColor = Pigment.Accent.readable(),
                        uncheckedTrackColor = Pigment.Mist, uncheckedBorderColor = Pigment.Line, uncheckedThumbColor = Pigment.Graphite,
                    ),
                )
            }

            Spacer(Modifier.height(30.dp))
            Text("Colores de la app", style = display(21.sp))
            Text("Se cambian al momento y se quedan guardados en este dispositivo.", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
            appSlots.forEach { slot -> ColorRow(slot) { editing = slot.label } }
            if (contrast(Pigment.Paper, Pigment.Ink) < 3f) {
                // este aviso va siempre en negro sobre blanco: si no, podria no leerse justo cuando hace falta
                Text(
                    "El fondo y el texto se parecen demasiado y la app cuesta de leer.",
                    color = Color.Black, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 6.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(10.dp),
                )
            }
            Label("Un color para cada tipo de contenido")
            typeSlots.forEach { slot -> ColorRow(slot) { editing = slot.label } }
            Spacer(Modifier.height(12.dp))
            // negro fijo para que el boton se vea aunque los colores elegidos no se distingan
            PillButton("Volver a los colores originales", { palette.setTheme(ThemeColors()) }, color = Color(0xFF17171A))

            Spacer(Modifier.height(30.dp))
            PillButton(
                "Cerrar sesión",
                {
                    palette.nav.switchTo(Screen.HOME)
                    store.signOut()
                },
                filled = false,
            )

            Spacer(Modifier.height(18.dp))
            if (!deleting) {
                Text(
                    "Eliminar mi cuenta",
                    color = Pigment.Cadmium, fontSize = 14.sp,
                    modifier = Modifier.clickable { deleting = true }.padding(vertical = 8.dp),
                )
            } else {
                Text(
                    "Se borran tu perfil, tus logs, tus pendientes, tus mensajes y tu foto. No se puede deshacer.",
                    fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Cancelar", { deleting = false }, filled = false, enabled = !saving)
                    PillButton(
                        if (saving) "Eliminando…" else "Eliminar para siempre",
                        {
                            saving = true
                            scope.launch {
                                problem = store.deleteAccount()
                                saving = false
                                if (problem == null) palette.nav.switchTo(Screen.HOME) else deleting = false
                            }
                        },
                        enabled = !saving, color = Pigment.Cadmium,
                    )
                }
                problem?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Los datos y las portadas vienen de TMDB (cine y series), IGDB (videojuegos), Open Library (libros) y iTunes (música). " +
                    "Este producto usa la API de TMDB pero no está avalado ni certificado por TMDB.",
                color = Pigment.Graphite, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }

    (listOf(favoriteSlot, backgroundSlot) + appSlots + typeSlots).firstOrNull { it.label == editing }?.let { slot ->
        ColorSheet(slot) { editing = null }
    }
}
