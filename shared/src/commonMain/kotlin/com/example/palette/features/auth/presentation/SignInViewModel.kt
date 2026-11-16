package com.example.palette.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInState(
    // 0: crear cuenta, 1: ya tengo cuenta
    val modo: Int = 0,
    val email: String = "",
    val password: String = "",
    val ocupado: Boolean = false,
    val aviso: String? = null,
) {
    val creando: Boolean get() = modo == 0
    val valido: Boolean get() = email.contains("@") && password.length >= 6
}

sealed interface SignInAction {
    data class OnModoChanged(val modo: Int) : SignInAction
    data class OnEmailChanged(val email: String) : SignInAction
    data class OnPasswordChanged(val password: String) : SignInAction
    data object OnSubmitClicked : SignInAction
}

class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.OnModoChanged -> _state.update { it.copy(modo = action.modo, aviso = null) }
            is SignInAction.OnEmailChanged -> _state.update { it.copy(email = action.email.trim()) }
            is SignInAction.OnPasswordChanged -> _state.update { it.copy(password = action.password) }
            SignInAction.OnSubmitClicked -> entrar()
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
            _state.update { it.copy(ocupado = false, aviso = problema) }
        }
    }
}
