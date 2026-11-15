package com.example.palette.features.auth.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.ui.ColorPicker
import com.example.palette.core.ui.Label
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.aGuardado
import com.example.palette.core.ui.formaAvatar
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.recordarSelectorDeFotos
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.auth.presentation.ProfileSetupAction
import com.example.palette.features.auth.presentation.ProfileSetupState
import com.example.palette.features.auth.presentation.ProfileSetupViewModel
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun ProfileSetupScreenRoot() {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ProfileSetupViewModel(container.profileRepository, container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // el selector de fotos es cosa de la plataforma y de compose: se prepara aqui y al viewmodel solo le llega la foto elegida
    val elegirFoto = recordarSelectorDeFotos { viewModel.onAction(ProfileSetupAction.OnFotoElegida(it)) }

    ProfileSetupScreen(state = state, onAction = viewModel::onAction, onElegirFoto = elegirFoto)
}

/** justo despues de crear la cuenta: nombre, usuario y color favorito. */
@Composable
fun ProfileSetupScreen(state: ProfileSetupState, onAction: (ProfileSetupAction) -> Unit, onElegirFoto: () -> Unit) {
    val favorito = Color(state.favorite)
    val resumen = remember(state.foto) { state.foto?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 28.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // la mancha grande va cogiendo el color que se esta eligiendo
        Splats(favorito)
        Spacer(Modifier.height(22.dp))
        Text("Tu perfil", style = titular(38.sp))
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(formaAvatar(0)).background(favorito), contentAlignment = Alignment.Center) {
                if (resumen != null) {
                    Image(resumen, contentDescription = "Tu foto", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                } else {
                    Text(state.name.take(1).uppercase(), style = titular(34.sp, favorito.legible()))
                }
            }
            Spacer(Modifier.width(16.dp))
            PillButton(if (state.foto == null) "Subir una foto" else "Cambiar foto", onElegirFoto, filled = false)
        }
        Label("Nombre")
        PlainField(state.name, { onAction(ProfileSetupAction.OnNameChanged(it)) }, "Cómo te llamas")
        Label("Usuario")
        PlainField(state.username, { onAction(ProfileSetupAction.OnUsernameChanged(it)) }, "minúsculas y números, así te buscarán")
        Label("Tu color favorito")
        Text("Será el color de tu avatar y del anillo de tus historias. Puedes cambiarlo cuando quieras.", color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 10.dp))
        ColorPicker(favorito, { onAction(ProfileSetupAction.OnFavoriteChanged(it.aGuardado())) })
        state.problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(24.dp))
        PillButton(
            text = if (state.ocupado) "Un momento…" else "Empezar",
            onClick = { onAction(ProfileSetupAction.OnEmpezarClicked) },
            enabled = !state.ocupado && state.valido,
            color = favorito,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Salir de esta cuenta",
            color = Pigment.Graphite, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).clickable { onAction(ProfileSetupAction.OnSalirClicked) }.padding(8.dp),
        )
    }
}
