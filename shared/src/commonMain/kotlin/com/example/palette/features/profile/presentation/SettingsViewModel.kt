package com.example.palette.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.metricas.Metricas
import com.example.palette.core.model.ThemeColors
import com.example.palette.core.model.User
import com.example.palette.core.ui.prepararAvatar
import com.example.palette.core.util.limpiarUsuario
import com.example.palette.features.auth.domain.repository.AuthRepository
import com.example.palette.features.profile.domain.repository.ProfileRepository
import com.example.palette.features.tutorial.domain.repository.TutorialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    // mi perfil tal y como esta guardado (foto, pendientes publicos...)
    val yo: User,
    // lo que se esta editando y todavia no se ha guardado
    val name: String,
    val username: String,
    val bio: String,
    val favorite: Long,
    val theme: ThemeColors,
    val guardando: Boolean = false,
    val subiendoFoto: Boolean = false,
    // true mientras se pregunta si de verdad quiere eliminar la cuenta
    val confirmandoBorrado: Boolean = false,
    val problema: String? = null,
    // true cuando el perfil se ha guardado bien: la pantalla se cierra
    val guardado: Boolean = false,
    // si se mandan las estadisticas de uso anonimas
    val metricas: Boolean = true,
) {
    val valido: Boolean get() = name.isNotBlank() && username.length >= 2
}

sealed interface SettingsAction {
    data object OnBackClicked : SettingsAction
    data class OnNameChanged(val name: String) : SettingsAction
    data class OnUsernameChanged(val username: String) : SettingsAction
    data class OnBioChanged(val bio: String) : SettingsAction
    data class OnFavoriteChanged(val favorite: Long) : SettingsAction
    data class OnFotoElegida(val foto: ByteArray) : SettingsAction
    data object OnQuitarFotoClicked : SettingsAction
    data class OnThemeChanged(val theme: ThemeColors) : SettingsAction
    data object OnGuardarClicked : SettingsAction
    data object OnVerTutorialClicked : SettingsAction
    data class OnMetricasChanged(val activadas: Boolean) : SettingsAction
    data object OnCerrarSesionClicked : SettingsAction
    data object OnBorrarCuentaClicked : SettingsAction
    data object OnBorrarCuentaCancelado : SettingsAction
    data object OnBorrarCuentaConfirmado : SettingsAction
}

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val tutorialRepository: TutorialRepository,
    private val metricas: Metricas,
) : ViewModel() {

    // el formulario arranca con lo que hay guardado
    private val _state = MutableStateFlow(
        profileRepository.yo().let {
            SettingsState(it, it.name, it.username, it.bio, it.favorite, profileRepository.tema(), metricas = metricas.activadas)
        },
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observarYo().collect { yo -> _state.update { it.copy(yo = yo) } }
        }
        viewModelScope.launch {
            profileRepository.observarTema().collect { theme -> _state.update { it.copy(theme = theme) } }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnNameChanged -> _state.update { it.copy(name = action.name) }
            is SettingsAction.OnUsernameChanged -> _state.update { it.copy(username = limpiarUsuario(action.username)) }
            is SettingsAction.OnBioChanged -> _state.update { it.copy(bio = action.bio.take(150)) }
            is SettingsAction.OnFavoriteChanged -> _state.update { it.copy(favorite = action.favorite) }
            is SettingsAction.OnFotoElegida -> cambiarFoto(action.foto)
            SettingsAction.OnQuitarFotoClicked -> viewModelScope.launch {
                val problema = profileRepository.ponerFoto(null)
                _state.update { it.copy(problema = problema) }
            }
            // los colores de la app se guardan al momento, sin esperar al boton de guardar
            is SettingsAction.OnThemeChanged -> profileRepository.ponerTema(action.theme)
            SettingsAction.OnGuardarClicked -> guardar()
            // el tutorial se abre solo encima de todo en cuanto vuelve a estar pendiente
            SettingsAction.OnVerTutorialClicked -> tutorialRepository.volverAVer()
            is SettingsAction.OnMetricasChanged -> {
                metricas.activadas = action.activadas
                _state.update { it.copy(metricas = action.activadas) }
            }
            SettingsAction.OnCerrarSesionClicked -> authRepository.cerrarSesion()
            SettingsAction.OnBorrarCuentaClicked -> _state.update { it.copy(confirmandoBorrado = true) }
            SettingsAction.OnBorrarCuentaCancelado -> _state.update { it.copy(confirmandoBorrado = false) }
            SettingsAction.OnBorrarCuentaConfirmado -> borrarCuenta()
            SettingsAction.OnBackClicked -> {}
        }
    }

    private fun cambiarFoto(foto: ByteArray) {
        _state.update { it.copy(subiendoFoto = true) }
        viewModelScope.launch {
            // la foto se recorta a cuadrado y se reduce antes de subirla
            val cuadrada = prepararAvatar(foto)
            val problema = if (cuadrada == null) "No se ha podido leer esa imagen. Prueba con un JPG o un PNG." else profileRepository.ponerFoto(cuadrada)
            _state.update { it.copy(subiendoFoto = false, problema = problema) }
        }
    }

    private fun guardar() {
        val datos = _state.value
        if (datos.guardando || !datos.valido) return
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val problema = profileRepository.guardarPerfil(datos.name, datos.username, datos.bio, datos.favorite, datos.yo.background)
            _state.update { it.copy(guardando = false, problema = problema, guardado = problema == null) }
        }
    }

    // si va bien la sesion se cierra sola y AppNavigation vuelve a la pantalla de entrada
    private fun borrarCuenta() {
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val problema = authRepository.borrarCuenta()
            _state.update { it.copy(guardando = false, problema = problema, confirmandoBorrado = false) }
        }
    }
}
