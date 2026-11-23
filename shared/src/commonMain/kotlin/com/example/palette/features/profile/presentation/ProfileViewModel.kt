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

/**
 * las paginas de un perfil. las cuatro primeras estan siempre (aunque los demas solo ven las que
 * su dueño deja visibles); despues va una por cada lista.
 */
sealed interface Seccion {
    data object Paleta : Seccion
    data object Favoritos : Seccion
    data object Diario : Seccion
    data object Pendientes : Seccion
    data class Lista(val listId: String) : Seccion
}

data class ProfileState(
    // null hasta que llega el perfil, o si esa persona no existe
    val perfil: Perfil? = null,
    // sus logs en el orden de favoritos
    val favoritos: List<LogEntry> = emptyList(),
    // las listas que se ven en este perfil (en el mio, todas)
    val listas: List<ListaVista> = emptyList(),
    val seccion: Seccion = Seccion.Paleta,
) {
    // las paginas que puede abrir quien mira: en mi perfil todas, en el de otro solo las que deja ver
    val verFavoritos: Boolean get() = perfil?.let { it.propio || it.user.layout.favoritesVisible } == true
    val verDiario: Boolean get() = perfil?.let { it.propio || it.user.layout.diaryVisible } == true
    val verPendientes: Boolean get() = perfil?.let { it.propio || it.pendientes != null } == true

    fun lista(listId: String?): ListaVista? = listas.firstOrNull { it.lista.id == listId }
}

sealed interface ProfileAction {
    data object OnBackClicked : ProfileAction
    data object OnPersonalizarClicked : ProfileAction
    data object OnMessageClicked : ProfileAction
    data object OnFollowClicked : ProfileAction
    data object OnRegistrarClicked : ProfileAction
    data class OnSeccionChanged(val seccion: Seccion) : ProfileAction
    data class OnMediaClicked(val media: Media) : ProfileAction
    data class OnResenaClicked(val logId: String) : ProfileAction
    data object OnNuevaListaClicked : ProfileAction
    data class OnEditarListaClicked(val listId: String) : ProfileAction
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
                _state.update { it.copy(perfil = perfil, favoritos = sortProfileLogsUseCase(perfil?.logs.orEmpty())).conSeccionValida() }
            }
        }
        viewModelScope.launch {
            listsRepository.observarListas(userId).collect { listas ->
                _state.update { it.copy(listas = listas).conSeccionValida() }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnSeccionChanged -> _state.update { it.copy(seccion = action.seccion).conSeccionValida() }
            ProfileAction.OnFollowClicked -> peopleRepository.alternarSeguir(userId)
            // el resto es navegacion: lo resuelve ProfileScreenRoot
            else -> {}
        }
    }

    // si la pagina abierta deja de existir o de verse (una lista borrada, unos pendientes que pasan a privados)
    // se vuelve a la paleta
    private fun ProfileState.conSeccionValida(): ProfileState {
        val valida = when (val s = seccion) {
            Seccion.Paleta -> true
            Seccion.Favoritos -> verFavoritos
            Seccion.Diario -> verDiario
            Seccion.Pendientes -> verPendientes
            is Seccion.Lista -> lista(s.listId) != null
        }
        return if (valida || perfil == null) this else copy(seccion = Seccion.Paleta)
    }
}
