package com.example.palette.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.features.people.domain.repository.PeopleRepository
import com.example.palette.features.profile.domain.model.Perfil
import com.example.palette.features.profile.domain.repository.ProfileRepository
import com.example.palette.features.profile.domain.usecases.SortProfileLogsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    // null hasta que llega el perfil, o si esa persona no existe
    val perfil: Perfil? = null,
    // los logs del perfil ya en el orden elegido
    val logs: List<LogEntry> = emptyList(),
    // 0: paleta, 1: pendientes
    val seccion: Int = 0,
    val recientesPrimero: Boolean = false,
)

sealed interface ProfileAction {
    data object OnBackClicked : ProfileAction
    data object OnSettingsClicked : ProfileAction
    data object OnMessageClicked : ProfileAction
    data object OnFollowClicked : ProfileAction
    data object OnRegistrarClicked : ProfileAction
    data class OnSeccionChanged(val seccion: Int) : ProfileAction
    data class OnOrdenChanged(val recientesPrimero: Boolean) : ProfileAction
    data class OnLogClicked(val log: LogEntry) : ProfileAction
    data class OnMediaClicked(val media: Media) : ProfileAction
}

class ProfileViewModel(
    private val userId: String,
    private val profileRepository: ProfileRepository,
    private val peopleRepository: PeopleRepository,
    private val sortProfileLogsUseCase: SortProfileLogsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observarPerfil(userId).collect { perfil ->
                _state.update { it.copy(perfil = perfil, logs = ordenar(perfil, it.recientesPrimero)) }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnSeccionChanged -> _state.update { it.copy(seccion = action.seccion) }
            is ProfileAction.OnOrdenChanged -> _state.update {
                it.copy(recientesPrimero = action.recientesPrimero, logs = ordenar(it.perfil, action.recientesPrimero))
            }
            ProfileAction.OnFollowClicked -> peopleRepository.alternarSeguir(userId)
            else -> {}
        }
    }

    private fun ordenar(perfil: Perfil?, recientesPrimero: Boolean): List<LogEntry> =
        sortProfileLogsUseCase(perfil?.logs.orEmpty(), recientesPrimero)
}
