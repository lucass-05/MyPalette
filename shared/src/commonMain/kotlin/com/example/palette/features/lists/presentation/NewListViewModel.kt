package com.example.palette.features.lists.presentation

import androidx.lifecycle.ViewModel
import com.example.palette.core.model.ListKind
import com.example.palette.features.lists.domain.repository.ListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NewListState(
    // null hasta que elige de que va a ser la lista
    val kind: ListKind? = null,
    val name: String = "",
    // el id de la lista ya creada: la hoja se cierra y se abre la lista
    val creada: String? = null,
) {
    val valido: Boolean get() = kind != null && name.isNotBlank()
}

sealed interface NewListAction {
    data object OnDismiss : NewListAction
    data class OnKindChosen(val kind: ListKind) : NewListAction
    data class OnNameChanged(val name: String) : NewListAction
    data object OnCrearClicked : NewListAction
}

class NewListViewModel(private val listsRepository: ListsRepository) : ViewModel() {

    private val _state = MutableStateFlow(NewListState())
    val state: StateFlow<NewListState> = _state.asStateFlow()

    fun onAction(action: NewListAction) {
        when (action) {
            is NewListAction.OnKindChosen -> _state.update { it.copy(kind = action.kind) }
            is NewListAction.OnNameChanged -> _state.update { it.copy(name = action.name.take(60)) }
            NewListAction.OnCrearClicked -> {
                val datos = _state.value
                val kind = datos.kind ?: return
                if (!datos.valido || datos.creada != null) return
                val id = listsRepository.crear(datos.name, kind)
                _state.update { it.copy(creada = id) }
            }
            NewListAction.OnDismiss -> {}
        }
    }
}
