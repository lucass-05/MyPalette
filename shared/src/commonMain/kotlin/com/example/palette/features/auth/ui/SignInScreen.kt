package com.example.palette.features.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.ui.Label
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Segments
import com.example.palette.core.ui.Splat
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.auth.presentation.DIGITOS_CODIGO
import com.example.palette.features.auth.presentation.PasoEntrada
import com.example.palette.features.auth.presentation.SignInAction
import com.example.palette.features.auth.presentation.SignInState
import com.example.palette.features.auth.presentation.SignInViewModel

// las manchas de colores de arriba; las comparten la pantalla de entrada y la de crear el perfil
@Composable
internal fun Splats(accent: Color) {
    Box(Modifier.fillMaxWidth().height(130.dp)) {
        Splat(3, Pigment.Cadmium, Modifier.size(100.dp).align(Alignment.CenterStart))
        Splat(8, Pigment.Ochre, Modifier.size(76.dp).align(Alignment.TopCenter))
        Splat(12, accent, Modifier.size(116.dp).align(Alignment.CenterEnd))
        Splat(5, Pigment.Viridian, Modifier.size(58.dp).align(Alignment.BottomCenter))
    }
}

@Composable
fun SignInScreenRoot() {
    val container = LocalAppContainer.current
    val viewModel = viewModel { SignInViewModel(container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // aqui no hay navegacion que resolver: todas las acciones van al viewmodel
    SignInScreen(state = state, onAction = viewModel::onAction)
}

/** crear cuenta o entrar con correo y contraseña, confirmar el correo y recuperar la contraseña. */
@Composable
fun SignInScreen(state: SignInState, onAction: (SignInAction) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 28.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Splats(Pigment.Cobalt)
        Spacer(Modifier.height(22.dp))
        Text("Palette", style = titular(46.sp))
        Spacer(Modifier.height(6.dp))
        Text("Lo que ves, juegas, lees y escuchas, convertido en tu paleta.", color = Pigment.Graphite, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(22.dp))
        // cada paso pinta sus campos; el aviso y el boton principal de abajo son comunes
        when (state.paso) {
            PasoEntrada.DATOS -> {
                Segments(listOf("Crear cuenta", "Ya tengo cuenta"), state.modo, { onAction(SignInAction.OnModoChanged(it)) }, Pigment.Accent, Modifier.offset(x = (-20).dp))
                Label("Correo")
                PlainField(state.email, { onAction(SignInAction.OnEmailChanged(it)) }, "tu@correo.com")
                Label("Contraseña")
                PlainField(state.password, { onAction(SignInAction.OnPasswordChanged(it)) }, if (state.creando) "Mínimo 6 caracteres" else "Tu contraseña", secret = true)
                if (!state.creando) Enlace("He olvidado mi contraseña") { onAction(SignInAction.OnOlvidadaClicked) }
            }
            PasoEntrada.CODIGO -> {
                Text("Confirma tu correo", style = titular(24.sp))
                Explicacion("Te hemos mandado un código de $DIGITOS_CODIGO cifras a ${state.email}. Escríbelo aquí para terminar de crear la cuenta.")
                Label("Código")
                PlainField(state.codigo, { onAction(SignInAction.OnCodigoChanged(it)) }, "0".repeat(DIGITOS_CODIGO))
                Enlace("Mandar otro código") { onAction(SignInAction.OnReenviarClicked) }
            }
            PasoEntrada.RECUPERAR -> {
                Text("Nueva contraseña", style = titular(24.sp))
                Explicacion("Si ${state.email} tiene cuenta, le hemos mandado un código de $DIGITOS_CODIGO cifras. Escríbelo y elige una contraseña nueva.")
                Label("Código")
                PlainField(state.codigo, { onAction(SignInAction.OnCodigoChanged(it)) }, "0".repeat(DIGITOS_CODIGO))
                Label("Contraseña nueva")
                PlainField(state.nueva, { onAction(SignInAction.OnNuevaChanged(it)) }, "Mínimo 6 caracteres", secret = true)
                Enlace("Mandar otro código") { onAction(SignInAction.OnReenviarClicked) }
            }
        }
        state.aviso?.let {
            Text(it, color = if (state.avisoEsFallo) Pigment.Cadmium else Pigment.Graphite, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(24.dp))
        PillButton(
            text = when {
                state.ocupado -> "Un momento…"
                state.paso == PasoEntrada.CODIGO -> "Confirmar"
                state.paso == PasoEntrada.RECUPERAR -> "Cambiar contraseña"
                state.creando -> "Crear cuenta"
                else -> "Entrar"
            },
            onClick = { onAction(SignInAction.OnSubmitClicked) },
            enabled = !state.ocupado && state.valido,
            color = Pigment.Accent,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.paso != PasoEntrada.DATOS) {
            Spacer(Modifier.height(10.dp))
            PillButton("Volver", { onAction(SignInAction.OnVolverClicked) }, Modifier.fillMaxWidth(), filled = false)
        }
    }
}

@Composable
private fun Explicacion(text: String) {
    Text(text, color = Pigment.Graphite, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
}

// accion secundaria en forma de texto, debajo de los campos
@Composable
private fun Enlace(text: String, onClick: () -> Unit) {
    Text(text, color = Pigment.Accent, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp).clickable(onClick = onClick))
}

@Composable
fun LoadingScreen() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Splat(4, Pigment.Accent, Modifier.size(84.dp))
        Spacer(Modifier.height(18.dp))
        Text("Cargando tu paleta…", color = Pigment.Graphite, fontSize = 14.sp)
    }
}
