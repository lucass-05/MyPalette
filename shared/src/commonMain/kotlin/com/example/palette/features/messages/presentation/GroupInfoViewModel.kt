package com.example.palette.features.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.Group
import com.example.palette.core.model.ME
import com.example.palette.core.model.User
import com.example.palette.core.ui.prepararAvatar
import com.example.palette.features.messages.domain.repository.MessagesRepository
import com.example.palette.features.people.domain.repository.PeopleRepository
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
    // el nombre mientras se esta cambiando; null cuando no se esta editando
    val renombrando: String? = null,
    // true con la lista de amigos para meter en el grupo abierta
    val anadiendo: Boolean = false,
    // amigos que aun no estan en el grupo, y los que se han marcado para entrar
    val amigosFuera: List<User> = emptyList(),
    val elegidos: Set<String> = emptySet(),
) {
    // el grupo es mio si lo cree yo: puedo sacar a gente y borrarlo
    val mio: Boolean get() = group?.ownerId == ME
}

sealed interface GroupInfoAction {
    data object OnDismiss : GroupInfoAction
    data class OnFotoElegida(val foto: ByteArray) : GroupInfoAction
    data class OnMemberClicked(val userId: String) : GroupInfoAction
    data object OnRenombrarClicked : GroupInfoAction
    data class OnNombreChanged(val name: String) : GroupInfoAction
    data object OnNombreGuardado : GroupInfoAction
    data object OnAnadirClicked : GroupInfoAction
    data class OnAmigoToggled(val userId: String) : GroupInfoAction
    data object OnAnadirConfirmado : GroupInfoAction
    data object OnAnadirCancelado : GroupInfoAction
    data class OnQuitarMiembroClicked(val userId: String) : GroupInfoAction
    data object OnSalirClicked : GroupInfoAction
    data object OnSalirCancelado : GroupInfoAction
    data object OnSalirConfirmado : GroupInfoAction
}

class GroupInfoViewModel(
    private val groupId: String,
    private val messagesRepository: MessagesRepository,
    private val peopleRepository: PeopleRepository,
) : ViewModel() {

    // la gente a la que sigo: de ahi salen los que se pueden meter en el grupo
    private var amigos: List<User> = emptyList()

    private val _state = MutableStateFlow(GroupInfoState())
    val state: StateFlow<GroupInfoState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            messagesRepository.observarGrupo(groupId).collect { conversacion ->
                val group = conversacion?.group
                _state.update {
                    if (group == null) it.copy(existe = false)
                    // los miembros salen en el orden en que entraron al grupo
                    else it.copy(
                        group = group,
                        miembros = group.members.mapNotNull { id -> conversacion.autores[id] },
                        amigosFuera = amigos.filter { a -> a.id !in group.members },
                    )
                }
            }
        }
        viewModelScope.launch {
            peopleRepository.observar().collect { gente ->
                amigos = gente.amigos
                _state.update { s -> s.copy(amigosFuera = amigos.filter { a -> a.id !in s.group?.members.orEmpty() }) }
            }
        }
    }

    fun onAction(action: GroupInfoAction) {
        when (action) {
            is GroupInfoAction.OnFotoElegida -> cambiarFoto(action.foto)
            GroupInfoAction.OnSalirClicked -> _state.update { it.copy(confirmando = true) }
            GroupInfoAction.OnSalirCancelado -> _state.update { it.copy(confirmando = false) }
            GroupInfoAction.OnSalirConfirmado -> messagesRepository.salirDeGrupo(groupId)
            GroupInfoAction.OnRenombrarClicked -> _state.update { it.copy(renombrando = it.group?.name.orEmpty()) }
            is GroupInfoAction.OnNombreChanged -> _state.update { it.copy(renombrando = action.name.take(60)) }
            GroupInfoAction.OnNombreGuardado -> {
                _state.value.renombrando?.takeIf { it.isNotBlank() }?.let { messagesRepository.renombrarGrupo(groupId, it) }
                _state.update { it.copy(renombrando = null) }
            }
            GroupInfoAction.OnAnadirClicked -> _state.update { it.copy(anadiendo = true, elegidos = emptySet()) }
            is GroupInfoAction.OnAmigoToggled -> _state.update {
                it.copy(elegidos = if (action.userId in it.elegidos) it.elegidos - action.userId else it.elegidos + action.userId)
            }
            GroupInfoAction.OnAnadirConfirmado -> {
                messagesRepository.anadirAGrupo(groupId, _state.value.elegidos)
                _state.update { it.copy(anadiendo = false, elegidos = emptySet()) }
            }
            GroupInfoAction.OnAnadirCancelado -> _state.update { it.copy(anadiendo = false, elegidos = emptySet()) }
            is GroupInfoAction.OnQuitarMiembroClicked -> messagesRepository.quitarDeGrupo(groupId, action.userId)
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
