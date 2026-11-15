package com.example.palette.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.ui.prepararAvatar
import com.example.palette.core.util.limpiarUsuario
import com.example.palette.features.auth.domain.repository.AuthRepository
import com.example.palette.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupState(
    val name: String = "",
    val username: String = "",
    val favorite: Long = 0xFF2D5BF0,
    // la foto ya recortada. solo se sube al pulsar empezar, cuando el perfil existe
    val foto: ByteArray? = null,
    val ocupado: Boolean = false,
    val problema: String? = null,
) {
    val valido: Boolean get() = name.isNotBlank() && username.length >= 2
}

sealed interface ProfileSetupAction {
    data class OnNameChanged(val name: String) : ProfileSetupAction
    data class OnUsernameChanged(val username: String) : ProfileSetupAction
    data class OnFavoriteChanged(val favorite: Long) : ProfileSetupAction
    data class OnFotoElegida(val foto: ByteArray) : ProfileSetupAction
    data object OnEmpezarClicked : ProfileSetupAction
    data object OnSalirClicked : ProfileSetupAction
}

class ProfileSetupViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state.asStateFlow()

    fun onAction(action: ProfileSetupAction) {
        when (action) {
            is ProfileSetupAction.OnNameChanged -> _state.update { it.copy(name = action.name) }
            is ProfileSetupAction.OnUsernameChanged -> _state.update { it.copy(username = limpiarUsuario(action.username)) }
            is ProfileSetupAction.OnFavoriteChanged -> _state.update { it.copy(favorite = action.favorite) }
            is ProfileSetupAction.OnFotoElegida -> recortar(action.foto)
            ProfileSetupAction.OnEmpezarClicked -> guardar()
            ProfileSetupAction.OnSalirClicked -> authRepository.cerrarSesion()
        }
    }

    private fun recortar(foto: ByteArray) {
        viewModelScope.launch {
            val cuadrada = prepararAvatar(foto)
            _state.update {
                if (cuadrada == null) it.copy(problema = "No se ha podido leer esa imagen. Prueba con un JPG o un PNG.")
                else it.copy(foto = cuadrada, problema = null)
            }
        }
    }

    private fun guardar() {
        val datos = _state.value
        if (datos.ocupado || !datos.valido) return
        _state.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val problema = profileRepository.guardarPerfil(datos.name, datos.username, "", datos.favorite, null)
            // si la foto falla no se bloquea la entrada: se puede volver a subir desde ajustes
            if (problema == null && datos.foto != null) profileRepository.ponerFoto(datos.foto)
            _state.update { it.copy(ocupado = false, problema = problema) }
        }
    }
}
