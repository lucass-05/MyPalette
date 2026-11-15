package com.example.palette.features.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.Media
import com.example.palette.features.messages.domain.model.Conversacion
import com.example.palette.features.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatState(
    val conversacion: Conversacion = Conversacion(),
    val borrador: String = "",
    // pasa a false si la persona o el grupo dejan de existir: la pantalla se cierra
    val existe: Boolean = true,
)

sealed interface ChatAction {
    data object OnBackClicked : ChatAction
    data object OnHeaderClicked : ChatAction
    data class OnBorradorChanged(val value: String) : ChatAction
    data object OnSendClicked : ChatAction
    data class OnMediaClicked(val media: Media) : ChatAction
}

// el mismo viewmodel sirve para el chat con una persona y para el de un grupo:
// llega el id de una cosa o de la otra, nunca los dos
class ChatViewModel(
    private val userId: String?,
    private val groupId: String?,
    private val messagesRepository: MessagesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val mensajes = if (groupId != null) messagesRepository.observarGrupo(groupId) else messagesRepository.observarChat(userId.orEmpty())
            mensajes.collect { conversacion ->
                if (conversacion == null) {
                    _state.update { it.copy(existe = false) }
                } else {
                    _state.update { it.copy(conversacion = conversacion) }
                    // con el chat abierto, lo que llega se da por leido
                    if (groupId != null) messagesRepository.marcarGrupoVisto(groupId) else messagesRepository.marcarLeido(userId.orEmpty())
                }
            }
        }
        // con el chat abierto se miran los mensajes nuevos mas a menudo que en el resto de la app.
        // el bucle no hay que pararlo a mano: viewModelScope lo cancela cuando se cierra la pantalla
        viewModelScope.launch {
            while (true) {
                delay(4_000)
                messagesRepository.refrescar()
            }
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnBorradorChanged -> _state.update { it.copy(borrador = action.value.take(2000)) }
            ChatAction.OnSendClicked -> enviar()
            else -> {}
        }
    }

    private fun enviar() {
        val texto = _state.value.borrador
        if (texto.isBlank()) return
        if (groupId != null) messagesRepository.enviarAGrupo(groupId, texto) else messagesRepository.enviar(userId.orEmpty(), texto)
        _state.update { it.copy(borrador = "") }
    }
}
