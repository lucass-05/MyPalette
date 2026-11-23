package com.example.palette.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.LogEntry
import com.example.palette.features.home.domain.model.Historia
import com.example.palette.features.home.domain.repository.FeedRepository
import com.example.palette.features.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryState(
    // null si esa persona no tiene historia (ya ha caducado, por ejemplo)
    val historia: Historia? = null,
    val index: Int = 0,
    // lo que se esta escribiendo para responder
    val respuesta: String = "",
    // true un momento despues de mandar una respuesta, para enseñar que ha salido
    val enviada: Boolean = false,
) {
    val log: LogEntry? get() = historia?.logs?.getOrNull(index)
    val enElUltimo: Boolean get() = historia == null || index >= historia.logs.lastIndex
}

sealed interface StoryAction {
    data object OnNextTapped : StoryAction
    data object OnPreviousTapped : StoryAction
    data object OnCloseClicked : StoryAction
    data object OnUserClicked : StoryAction
    data object OnVerFichaClicked : StoryAction
    data class OnRespuestaChanged(val texto: String) : StoryAction
    data object OnEnviarRespuestaClicked : StoryAction
}

class StoryViewModel(
    private val userId: String,
    private val feedRepository: FeedRepository,
    private val messagesRepository: MessagesRepository,
) : ViewModel() {

    // la historia se lee una sola vez al abrir: marcar como visto no debe mover la lista.
    // como en instagram, se abre por lo primero que no he visto
    private val _state = MutableStateFlow(
        feedRepository.historiaDe(userId).let { StoryState(historia = it, index = it?.primeroSinVer ?: 0) },
    )
    val state: StateFlow<StoryState> = _state.asStateFlow()

    init {
        marcarVisto()
    }

    fun onAction(action: StoryAction) {
        when (action) {
            // pasar a la historia de otra persona o cerrar lo decide StoryScreenRoot;
            // aqui solo se avanza o retrocede dentro de los logs de esta
            StoryAction.OnNextTapped -> if (!_state.value.enElUltimo) irA(_state.value.index + 1)
            StoryAction.OnPreviousTapped -> if (_state.value.index > 0) irA(_state.value.index - 1)
            is StoryAction.OnRespuestaChanged -> _state.update { it.copy(respuesta = action.texto.take(2000), enviada = false) }
            StoryAction.OnEnviarRespuestaClicked -> responder()
            else -> {}
        }
    }

    // la respuesta va al chat con esa persona, con la historia pegada para que sepa a que contesto
    private fun responder() {
        val datos = _state.value
        val log = datos.log ?: return
        if (datos.respuesta.isBlank()) return
        messagesRepository.responderHistoria(userId, log, datos.respuesta)
        _state.update { it.copy(respuesta = "", enviada = true) }
        viewModelScope.launch {
            delay(2_000)
            _state.update { it.copy(enviada = false) }
        }
    }

    private fun irA(index: Int) {
        _state.update { it.copy(index = index) }
        marcarVisto()
    }

    private fun marcarVisto() {
        _state.value.log?.let { feedRepository.marcarHistoriaVista(userId, it.createdAt) }
    }
}
