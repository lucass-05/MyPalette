package com.example.palette.features.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.Group
import com.example.palette.core.model.User
import com.example.palette.features.messages.domain.model.Outgoing
import com.example.palette.features.messages.domain.repository.MessagesRepository
import com.example.palette.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SendState(
    val envio: Outgoing,
    val nota: String = "",
    val amigos: List<User> = emptyList(),
    val grupos: List<Group> = emptyList(),
    // ids de las personas a las que ya se ha mandado
    val enviadoA: Set<String> = emptySet(),
    val enviadoAGrupos: Set<String> = emptySet(),
)

sealed interface SendAction {
    data object OnDismiss : SendAction
    data class OnNotaChanged(val value: String) : SendAction
    data class OnUserClicked(val userId: String) : SendAction
    data class OnSendToUserClicked(val userId: String) : SendAction
    data class OnSendToGroupClicked(val groupId: String) : SendAction
    data object OnPeopleClicked : SendAction
}

class SendViewModel(
    envio: Outgoing,
    private val messagesRepository: MessagesRepository,
    private val peopleRepository: PeopleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SendState(envio))
    val state: StateFlow<SendState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.observar().collect { gente -> _state.update { it.copy(amigos = gente.amigos) } }
        }
        viewModelScope.launch {
            messagesRepository.observarGrupos().collect { grupos -> _state.update { it.copy(grupos = grupos) } }
        }
    }

    fun onAction(action: SendAction) {
        val actual = _state.value
        // lo que se adjunta al mensaje: un contenido o una reseña, segun desde donde se abrio la hoja
        val media = (actual.envio as? Outgoing.Content)?.media
        val log = (actual.envio as? Outgoing.Review)?.log
        when (action) {
            is SendAction.OnNotaChanged -> _state.update { it.copy(nota = action.value) }
            is SendAction.OnSendToUserClicked -> {
                messagesRepository.enviar(action.userId, actual.nota, media, log)
                _state.update { it.copy(enviadoA = it.enviadoA + action.userId) }
            }
            is SendAction.OnSendToGroupClicked -> {
                messagesRepository.enviarAGrupo(action.groupId, actual.nota, media, log)
                _state.update { it.copy(enviadoAGrupos = it.enviadoAGrupos + action.groupId) }
            }
            else -> {}
        }
    }
}
