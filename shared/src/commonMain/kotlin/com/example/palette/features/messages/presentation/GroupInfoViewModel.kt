package com.example.palette.features.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.Group
import com.example.palette.core.model.ME
import com.example.palette.core.model.User
import com.example.palette.core.ui.prepararAvatar
import com.example.palette.features.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupInfoState(
    val group: Group? = null,
    val miembros: List<User> = emptyList(),
    val ocupado: Boolean = false,
    val problema: String? = null,
    // true mientras se pregunta si de verdad quiere salir
    val confirmando: Boolean = false,
    // pasa a false cuando el grupo ya no existe (he salido o lo han borrado)
    val existe: Boolean = true,
) {
    // el grupo es mio si lo cree yo: puedo cambiarle la foto y borrarlo
    val mio: Boolean get() = group?.ownerId == ME
}

sealed interface GroupInfoAction {
    data object OnDismiss : GroupInfoAction
    data class OnFotoElegida(val foto: ByteArray) : GroupInfoAction
    data class OnMemberClicked(val userId: String) : GroupInfoAction
    data object OnSalirClicked : GroupInfoAction
    data object OnSalirCancelado : GroupInfoAction
    data object OnSalirConfirmado : GroupInfoAction
}

class GroupInfoViewModel(
    private val groupId: String,
    private val messagesRepository: MessagesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupInfoState())
    val state: StateFlow<GroupInfoState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            messagesRepository.observarGrupo(groupId).collect { conversacion ->
                val group = conversacion?.group
                _state.update {
                    if (group == null) it.copy(existe = false)
                    // los miembros salen en el orden en que entraron al grupo
                    else it.copy(group = group, miembros = group.members.mapNotNull { id -> conversacion.autores[id] })
                }
            }
        }
    }

    fun onAction(action: GroupInfoAction) {
        when (action) {
            is GroupInfoAction.OnFotoElegida -> cambiarFoto(action.foto)
            GroupInfoAction.OnSalirClicked -> _state.update { it.copy(confirmando = true) }
            GroupInfoAction.OnSalirCancelado -> _state.update { it.copy(confirmando = false) }
            GroupInfoAction.OnSalirConfirmado -> messagesRepository.salirDeGrupo(groupId)
            else -> {}
        }
    }

    private fun cambiarFoto(foto: ByteArray) {
        _state.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val cuadrada = prepararAvatar(foto)
            val problema = if (cuadrada == null) "No se ha podido leer esa imagen." else messagesRepository.ponerFotoDeGrupo(groupId, cuadrada)
            _state.update { it.copy(ocupado = false, problema = problema) }
        }
    }
}
