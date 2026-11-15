package com.example.palette.features.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.User
import com.example.palette.core.ui.prepararAvatar
import com.example.palette.features.messages.domain.repository.MessagesRepository
import com.example.palette.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewGroupState(
    val name: String = "",
    val amigos: List<User> = emptyList(),
    val elegidos: Set<String> = emptySet(),
    val foto: ByteArray? = null,
    val ocupado: Boolean = false,
    val problema: String? = null,
    // true cuando el grupo ya esta creado: la hoja se cierra
    val creado: Boolean = false,
) {
    val valido: Boolean get() = name.isNotBlank() && elegidos.isNotEmpty()
}

sealed interface NewGroupAction {
    data object OnDismiss : NewGroupAction
    data class OnNameChanged(val name: String) : NewGroupAction
    data class OnMemberToggled(val userId: String) : NewGroupAction
    data class OnFotoElegida(val foto: ByteArray) : NewGroupAction
    data object OnCrearClicked : NewGroupAction
}

class NewGroupViewModel(
    private val messagesRepository: MessagesRepository,
    private val peopleRepository: PeopleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NewGroupState())
    val state: StateFlow<NewGroupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.observar().collect { gente -> _state.update { it.copy(amigos = gente.amigos) } }
        }
    }

    fun onAction(action: NewGroupAction) {
        when (action) {
            is NewGroupAction.OnNameChanged -> _state.update { it.copy(name = action.name.take(60)) }
            is NewGroupAction.OnMemberToggled -> _state.update {
                it.copy(elegidos = if (action.userId in it.elegidos) it.elegidos - action.userId else it.elegidos + action.userId)
            }
            is NewGroupAction.OnFotoElegida -> viewModelScope.launch {
                val cuadrada = prepararAvatar(action.foto)
                _state.update { it.copy(foto = cuadrada, problema = if (cuadrada == null) "No se ha podido leer esa imagen." else null) }
            }
            NewGroupAction.OnCrearClicked -> crear()
            NewGroupAction.OnDismiss -> {}
        }
    }

    private fun crear() {
        val datos = _state.value
        if (datos.ocupado || !datos.valido) return
        _state.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val problema = messagesRepository.crearGrupo(datos.name, datos.elegidos, datos.foto)
            _state.update { it.copy(ocupado = false, problema = problema, creado = problema == null) }
        }
    }
}
