package com.example.palette.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.PaletteStore
import com.example.palette.navigation.LocalPalette
import com.example.palette.ui.ColorPicker
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.PlainField
import com.example.palette.ui.Splat
import com.example.palette.ui.avatarShape
import com.example.palette.ui.display
import com.example.palette.ui.readable
import com.example.palette.ui.rememberPhotoPicker
import com.example.palette.ui.squareAvatar
import com.example.palette.ui.toStored
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
private fun Splats(accent: Color) {
    Box(Modifier.fillMaxWidth().height(130.dp)) {
        Splat(3, Pigment.Cadmium, Modifier.size(100.dp).align(Alignment.CenterStart))
        Splat(8, Pigment.Ochre, Modifier.size(76.dp).align(Alignment.TopCenter))
        Splat(12, accent, Modifier.size(116.dp).align(Alignment.CenterEnd))
        Splat(5, Pigment.Viridian, Modifier.size(58.dp).align(Alignment.BottomCenter))
    }
}

/** crear cuenta o entrar con correo y contraseña. */
@Composable
fun SignInScreen() {
    val store = LocalPalette.current.store
    val scope = rememberCoroutineScope()
    var mode by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val creating = mode == 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 28.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Splats(Pigment.Cobalt)
        Spacer(Modifier.height(22.dp))
        Text("Palette", style = display(46.sp))
        Spacer(Modifier.height(6.dp))
        Text("Lo que ves, juegas, lees y escuchas, convertido en tu paleta.", color = Pigment.Graphite, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(22.dp))
        Segments(listOf("Crear cuenta", "Ya tengo cuenta"), mode, { mode = it; notice = null }, Pigment.Accent, Modifier.offset(x = (-20).dp))
        Label("Correo")
        PlainField(email, { email = it.trim() }, "tu@correo.com")
        Label("Contraseña")
        PlainField(password, { password = it }, if (creating) "Mínimo 6 caracteres" else "Tu contraseña", secret = true)
        notice?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(24.dp))
        PillButton(
            text = when {
                busy -> "Un momento…"
                creating -> "Crear cuenta"
                else -> "Entrar"
            },
            onClick = {
                busy = true
                notice = null
                scope.launch {
                    notice = if (creating) store.signUp(email, password) else store.signIn(email, password)
                    busy = false
                }
            },
            enabled = !busy && email.contains("@") && password.length >= 6,
            color = Pigment.Accent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** justo despues de crear la cuenta: nombre, usuario y color favorito. */
@Composable
fun ProfileSetupScreen() {
    val store = LocalPalette.current.store
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var favorite by remember { mutableLongStateOf(0xFF2D5BF0) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    // la foto se guarda ya recortada y solo se sube al pulsar empezar, cuando el perfil existe
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    val preview = remember(photo) { photo?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    val pickPhoto = rememberPhotoPicker { picked ->
        scope.launch {
            try {
                photo = withContext(Dispatchers.Default) { squareAvatar(picked) }
                problem = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                problem = "No se ha podido leer esa imagen. Prueba con un JPG o un PNG."
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 28.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // la mancha grande va cogiendo el color que se esta eligiendo
        Splats(Color(favorite))
        Spacer(Modifier.height(22.dp))
        Text("Tu perfil", style = display(38.sp))
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(avatarShape(0)).background(Color(favorite)), contentAlignment = Alignment.Center) {
                if (preview != null) {
                    Image(preview, contentDescription = "Tu foto", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                } else {
                    Text(name.take(1).uppercase(), style = display(34.sp, Color(favorite).readable()))
                }
            }
            Spacer(Modifier.width(16.dp))
            PillButton(if (photo == null) "Subir una foto" else "Cambiar foto", pickPhoto, filled = false)
        }
        Label("Nombre")
        PlainField(name, { name = it }, "Cómo te llamas")
        Label("Usuario")
        PlainField(username, { username = PaletteStore.cleanUsername(it) }, "minúsculas y números, así te buscarán")
        Label("Tu color favorito")
        Text("Será el color de tu avatar y del anillo de tus historias. Puedes cambiarlo cuando quieras.", color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 10.dp))
        ColorPicker(Color(favorite), { favorite = it.toStored() })
        problem?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(24.dp))
        PillButton(
            text = if (busy) "Un momento…" else "Empezar",
            onClick = {
                busy = true
                scope.launch {
                    val chosen = photo
                    problem = store.saveProfile(name, username, "", favorite, null)
                    // si la foto falla no se bloquea la entrada: se puede volver a subir desde ajustes
                    if (problem == null && chosen != null) store.setPhoto(chosen)
                    busy = false
                }
            },
            enabled = !busy && name.isNotBlank() && username.length >= 2,
            color = Color(favorite),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Salir de esta cuenta",
            color = Pigment.Graphite, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).clickable { store.signOut() }.padding(8.dp),
        )
    }
}

@Composable
fun LoadingScreen() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Splat(4, Pigment.Accent, Modifier.size(84.dp))
        Spacer(Modifier.height(18.dp))
        Text("Cargando tu paleta…", color = Pigment.Graphite, fontSize = 14.sp)
    }
}
