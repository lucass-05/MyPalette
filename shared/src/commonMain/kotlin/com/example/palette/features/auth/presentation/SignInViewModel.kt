package com.example.palette.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.data.EnlaceDeCorreo
import com.example.palette.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// la entrada es una sola pantalla que va cambiando de paso
enum class PasoEntrada {
    /** correo y contraseña, para crear la cuenta o para entrar. */
    DATOS,

    /** se ha llegado desde el enlace de contraseña olvidada: toca elegir una nueva. */
    RECUPERAR,
}

data class SignInState(
    // 0: crear cuenta, 1: ya tengo cuenta
    val modo: Int = 0,
    val paso: PasoEntrada = PasoEntrada.DATOS,
    val email: String = "",
    val password: String = "",
    // la contraseña nueva al recuperar la cuenta
    val nueva: String = "",
    val ocupado: Boolean = false,
    val aviso: String? = null,
    // los avisos que no son fallos ("te hemos mandado un correo") no van en rojo
    val avisoEsFallo: Boolean = true,
) {
    val creando: Boolean get() = modo == 0
    val valido: Boolean
        get() = when (paso) {
            PasoEntrada.DATOS -> email.contains("@") && password.length >= 6
            PasoEntrada.RECUPERAR -> nueva.length >= 6
        }
}

sealed interface SignInAction {
    data class OnModoChanged(val modo: Int) : SignInAction
    data class OnEmailChanged(val email: String) : SignInAction
    data class OnPasswordChanged(val password: String) : SignInAction
    data class OnNuevaChanged(val nueva: String) : SignInAction
    data object OnSubmitClicked : SignInAction
    data object OnOlvidadaClicked : SignInAction
    data object OnVolverClicked : SignInAction
}

class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    init {
        // la web puede haberse abierto desde el enlace de un correo
        when (authRepository.enlaceDeCorreo()) {
            EnlaceDeCorreo.CONFIRMADO -> entrarConEnlace(nueva = null)
            EnlaceDeCorreo.RECUPERAR -> _state.update { it.copy(paso = PasoEntrada.RECUPERAR) }
            EnlaceDeCorreo.CADUCADO -> _state.update { it.copy(modo = 1, aviso = "Ese enlace ya no vale. Pide otro.") }
            EnlaceDeCorreo.NINGUNO -> Unit
        }
    }

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.OnModoChanged -> _state.update { it.copy(modo = action.modo, aviso = null) }
            is SignInAction.OnEmailChanged -> _state.update { it.copy(email = action.email.trim()) }
            is SignInAction.OnPasswordChanged -> _state.update { it.copy(password = action.password) }
            is SignInAction.OnNuevaChanged -> _state.update { it.copy(nueva = action.nueva) }
            SignInAction.OnSubmitClicked -> when (_state.value.paso) {
                PasoEntrada.DATOS -> entrar()
                PasoEntrada.RECUPERAR -> if (_state.value.valido) entrarConEnlace(_state.value.nueva)
            }
            SignInAction.OnOlvidadaClicked -> pedirRecuperacion()
            SignInAction.OnVolverClicked -> _state.update { it.copy(paso = PasoEntrada.DATOS, nueva = "", aviso = null) }
        }
    }

    // si va bien no hay que navegar a ningun sitio: cambia la etapa de la sesion
    // y AppNavigation pasa sola a la siguiente pantalla
    private fun entrar() {
        val datos = _state.value
        if (datos.ocupado || !datos.valido) return
        _state.update { it.copy(ocupado = true, aviso = null) }
        viewModelScope.launch {
            val problema = if (datos.creando) authRepository.crearCuenta(datos.email, datos.password)
            else authRepository.iniciarSesion(datos.email, datos.password)
            _state.update {
                // cuenta creada: falta pulsar el enlace del correo. se deja ya en "ya tengo cuenta" para entrar despues
                if (datos.creando && problema == null) {
                    it.copy(
                        ocupado = false, modo = 1, password = "", avisoEsFallo = false,
                        aviso = "Te hemos mandado un correo a ${datos.email}. Pulsa el enlace para confirmar la cuenta y después entra aquí.",
                    )
                } else {
                    it.copy(ocupado = false, aviso = problema, avisoEsFallo = true)
                }
            }
        }
    }

    private fun pedirRecuperacion() {
        val datos = _state.value
        if (datos.ocupado) return
        if (!datos.email.contains("@")) {
            _state.update { it.copy(aviso = "Escribe tu correo y vuelve a pulsar.", avisoEsFallo = true) }
            return
        }
        _state.update { it.copy(ocupado = true, aviso = null) }
        viewModelScope.launch {
            val problema = authRepository.pedirRecuperacion(datos.email)
            _state.update {
                it.copy(
                    ocupado = false, avisoEsFallo = problema != null,
                    aviso = problema ?: "Si ${datos.email} tiene cuenta, le hemos mandado un enlace para cambiar la contraseña.",
                )
            }
        }
    }

    // con nueva = null solo entra (enlace de confirmar la cuenta); con contraseña, ademas la cambia
    private fun entrarConEnlace(nueva: String?) {
        if (_state.value.ocupado) return
        _state.update { it.copy(ocupado = true, aviso = null) }
        viewModelScope.launch {
            val problema = authRepository.entrarConEnlace(nueva)
            _state.update { it.copy(ocupado = false, aviso = problema, avisoEsFallo = true) }
        }
    }
}
