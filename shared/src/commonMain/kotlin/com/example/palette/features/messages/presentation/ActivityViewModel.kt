package com.example.palette.features.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.features.messages.domain.model.Aviso
import com.example.palette.features.messages.domain.model.Hilo
import com.example.palette.features.messages.domain.repository.MessagesRepository
import com.example.palette.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityState(
    // 0: chats, 1: actividad
    val seccion: Int = 0,
    val hilos: List<Hilo> = emptyList(),
    val avisos: List<Aviso> = emptyList(),
) {
    val avisosPendientes: Int get() = avisos.count { !it.leido }
}

sealed interface ActivityAction {
    data class OnSeccionChanged(val seccion: Int) : ActivityAction
    data class OnHiloClicked(val hilo: Hilo) : ActivityAction
    data class OnUserClicked(val userId: String) : ActivityAction
    data class OnFollowClicked(val userId: String) : ActivityAction
    data object OnNewGroupClicked : ActivityAction
    data object OnNewMessageClicked : ActivityAction
}

class ActivityViewModel(
    private val messagesRepository: MessagesRepository,
    private val peopleRepository: PeopleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    init {
        // dos flujos, cada uno en su corrutina: collect no termina nunca, asi que no pueden ir seguidos en la misma
        viewModelScope.launch {
            messagesRepository.observarHilos().collect { hilos -> _state.update { it.copy(hilos = hilos) } }
        }
        viewModelScope.launch {
            messagesRepository.observarAvisos().collect { avisos -> _state.update { it.copy(avisos = avisos) } }
        }
    }

    fun onAction(action: ActivityAction) {
        when (action) {
            is ActivityAction.OnSeccionChanged -> {
                _state.update { it.copy(seccion = action.seccion) }
                // al abrir la pestaña de actividad los avisos cuentan como leidos
                if (action.seccion == 1) messagesRepository.marcarAvisosLeidos()
            }
            is ActivityAction.OnFollowClicked -> peopleRepository.alternarSeguir(action.userId)
            else -> {}
        }
    }
}
