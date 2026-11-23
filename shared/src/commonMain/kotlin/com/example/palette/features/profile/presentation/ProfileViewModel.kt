package com.example.palette.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.lists.domain.repository.ListsRepository
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
    // las listas que se ven en este perfil (en el mio, todas)
    val listas: List<ListaVista> = emptyList(),
    // la lista que se esta viendo en lugar de la paleta; null: la paleta
    val listaAbierta: String? = null,
) {
    val lista: ListaVista? get() = listas.firstOrNull { it.lista.id == listaAbierta }
}

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
    data class OnResenaClicked(val logId: String) : ProfileAction
    data class OnListaClicked(val listId: String) : ProfileAction
    data object OnNuevaListaClicked : ProfileAction
    data class OnEditarListaClicked(val listId: String) : ProfileAction
    data object OnPersonalizarClicked : ProfileAction
}

class ProfileViewModel(
    private val userId: String,
    private val profileRepository: ProfileRepository,
    private val peopleRepository: PeopleRepository,
    private val listsRepository: ListsRepository,
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
        viewModelScope.launch {
            listsRepository.observarListas(userId).collect { listas ->
                // si la lista abierta deja de existir se vuelve a la paleta
                _state.update { s -> s.copy(listas = listas, listaAbierta = s.listaAbierta?.takeIf { id -> listas.any { it.lista.id == id } }) }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnSeccionChanged -> _state.update { it.copy(seccion = action.seccion) }
            is ProfileAction.OnOrdenChanged -> _state.update {
                it.copy(recientesPrimero = action.recientesPrimero, logs = ordenar(it.perfil, action.recientesPrimero), listaAbierta = null)
            }
            is ProfileAction.OnListaClicked -> _state.update { it.copy(listaAbierta = action.listId) }
            ProfileAction.OnFollowClicked -> peopleRepository.alternarSeguir(userId)
            else -> {}
        }
    }

    private fun ordenar(perfil: Perfil?, recientesPrimero: Boolean): List<LogEntry> =
        sortProfileLogsUseCase(perfil?.logs.orEmpty(), recientesPrimero)
}
