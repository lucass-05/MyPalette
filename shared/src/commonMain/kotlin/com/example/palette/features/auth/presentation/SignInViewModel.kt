package com.example.palette.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.data.CORREO_SIN_CONFIRMAR
import com.example.palette.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// cifras del codigo que llega al correo. tiene que coincidir con el ajuste "email otp length"
// del proyecto de supabase, que no deja poner menos de 6
const val DIGITOS_CODIGO = 6

// la entrada es una sola pantalla que va cambiando de paso
enum class PasoEntrada {
    /** correo y contraseña, para crear la cuenta o para entrar. */
    DATOS,

    /** el codigo que confirma el correo de una cuenta recien creada. */
    CODIGO,

    /** contraseña olvidada: el codigo y la contraseña nueva. */
    RECUPERAR,
}

data class SignInState(
    // 0: crear cuenta, 1: ya tengo cuenta
    val modo: Int = 0,
    val paso: PasoEntrada = PasoEntrada.DATOS,
    val email: String = "",
    val password: String = "",
    val codigo: String = "",
    // la contraseña nueva al recuperar la cuenta
    val nueva: String = "",
    val ocupado: Boolean = false,
    val aviso: String? = null,
    // los avisos que no son fallos ("te hemos mandado otro codigo") no van en rojo
    val avisoEsFallo: Boolean = true,
) {
    val creando: Boolean get() = modo == 0
    val valido: Boolean
        get() = when (paso) {
            PasoEntrada.DATOS -> email.contains("@") && password.length >= 6
            PasoEntrada.CODIGO -> codigo.length == DIGITOS_CODIGO
            PasoEntrada.RECUPERAR -> codigo.length == DIGITOS_CODIGO && nueva.length >= 6
        }
}

sealed interface SignInAction {
    data class OnModoChanged(val modo: Int) : SignInAction
    data class OnEmailChanged(val email: String) : SignInAction
    data class OnPasswordChanged(val password: String) : SignInAction
    data class OnCodigoChanged(val codigo: String) : SignInAction
    data class OnNuevaChanged(val nueva: String) : SignInAction
    data object OnSubmitClicked : SignInAction
    data object OnOlvidadaClicked : SignInAction
    data object OnReenviarClicked : SignInAction
    data object OnVolverClicked : SignInAction
}

class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.OnModoChanged -> _state.update { it.copy(modo = action.modo, aviso = null) }
            is SignInAction.OnEmailChanged -> _state.update { it.copy(email = action.email.trim()) }
            is SignInAction.OnPasswordChanged -> _state.update { it.copy(password = action.password) }
            // del codigo solo se guardan las cifras, y no mas de las que tiene
            is SignInAction.OnCodigoChanged -> _state.update { it.copy(codigo = action.codigo.filter(Char::isDigit).take(DIGITOS_CODIGO)) }
            is SignInAction.OnNuevaChanged -> _state.update { it.copy(nueva = action.nueva) }
            SignInAction.OnSubmitClicked -> when (_state.value.paso) {
                PasoEntrada.DATOS -> entrar()
                PasoEntrada.CODIGO -> confirmar()
                PasoEntrada.RECUPERAR -> cambiarContrasena()
            }
            SignInAction.OnOlvidadaClicked -> pedirRecuperacion()
            SignInAction.OnReenviarClicked -> reenviar()
            SignInAction.OnVolverClicked -> _state.update { it.copy(paso = PasoEntrada.DATOS, codigo = "", nueva = "", aviso = null) }
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
            // cuenta recien creada, o cuenta que se quedo sin confirmar: toca meter el codigo del correo
            val faltaCodigo = (datos.creando && problema == null) || problema == CORREO_SIN_CONFIRMAR
            _state.update {
                if (faltaCodigo) it.copy(ocupado = false, paso = PasoEntrada.CODIGO, codigo = "")
                else it.copy(ocupado = false, aviso = problema, avisoEsFallo = true)
            }
        }
    }

    // si el codigo es bueno el servidor da la sesion y la etapa cambia sola, igual que al entrar
    private fun confirmar() {
        val datos = _state.value
        if (datos.ocupado || !datos.valido) return
        _state.update { it.copy(ocupado = true, aviso = null) }
        viewModelScope.launch {
            val problema = authRepository.confirmarCorreo(datos.email, datos.codigo)
            _state.update { it.copy(ocupado = false, aviso = problema, avisoEsFallo = true) }
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
                if (problema == null) it.copy(ocupado = false, paso = PasoEntrada.RECUPERAR, codigo = "", nueva = "")
                else it.copy(ocupado = false, aviso = problema, avisoEsFallo = true)
            }
        }
    }

    private fun cambiarContrasena() {
        val datos = _state.value
        if (datos.ocupado || !datos.valido) return
        _state.update { it.copy(ocupado = true, aviso = null) }
        viewModelScope.launch {
            val problema = authRepository.cambiarContrasena(datos.email, datos.codigo, datos.nueva)
            _state.update { it.copy(ocupado = false, aviso = problema, avisoEsFallo = true) }
        }
    }

    // vuelve a mandar el codigo del paso en el que se este
    private fun reenviar() {
        val datos = _state.value
        if (datos.ocupado) return
        _state.update { it.copy(ocupado = true, aviso = null) }
        viewModelScope.launch {
            val problema = if (datos.paso == PasoEntrada.RECUPERAR) authRepository.pedirRecuperacion(datos.email)
            else authRepository.reenviarCodigo(datos.email)
            _state.update {
                it.copy(ocupado = false, aviso = problema ?: "Te hemos mandado otro código.", avisoEsFallo = problema != null)
            }
        }
    }
}
