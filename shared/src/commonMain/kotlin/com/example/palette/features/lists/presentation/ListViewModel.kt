package com.example.palette.features.lists.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.lists.domain.repository.ListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListState(
    val vista: ListaVista? = null,
    // pasa a false si la lista se borra: la pantalla se cierra
    val existe: Boolean = true,
    // el nombre mientras se esta cambiando; null cuando no se esta editando
    val renombrando: String? = null,
    // true con el buscador para añadir abierto
    val eligiendo: Boolean = false,
    // mis reseñas, para el buscador de las listas de reseñas
    val misResenas: List<LogEntry> = emptyList(),
    val filtroResenas: String = "",
    val confirmandoBorrado: Boolean = false,
) {
    /** mis reseñas que coinciden con lo escrito en el buscador. */
    val resenasFiltradas: List<LogEntry>
        get() = if (filtroResenas.isBlank()) misResenas
        else misResenas.filter { it.media.title.contains(filtroResenas.trim(), ignoreCase = true) || it.media.creator?.contains(filtroResenas.trim(), ignoreCase = true) == true }
}

sealed interface ListAction {
    data object OnBackClicked : ListAction
    data object OnRenombrarClicked : ListAction
    data class OnNombreChanged(val name: String) : ListAction
    data object OnNombreGuardado : ListAction
    data object OnVisibleToggled : ListAction
    data object OnAnadirClicked : ListAction
    data object OnSelectorCerrado : ListAction
    data class OnMediaToggled(val media: Media) : ListAction
    data class OnResenaToggled(val logId: String) : ListAction
    data class OnFiltroChanged(val texto: String) : ListAction
    data object OnBorrarClicked : ListAction
    data object OnBorrarCancelado : ListAction
    data object OnBorrarConfirmado : ListAction
    data class OnMediaClicked(val media: Media) : ListAction
    data class OnResenaClicked(val logId: String) : ListAction
}

class ListViewModel(
    private val listId: String,
    abrirSelector: Boolean,
    private val listsRepository: ListsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ListState(eligiendo = abrirSelector))
    val state: StateFlow<ListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            listsRepository.observarLista(listId).collect { vista ->
                _state.update { if (vista == null) it.copy(existe = false) else it.copy(vista = vista) }
            }
        }
        viewModelScope.launch {
            listsRepository.observarMisResenas().collect { resenas -> _state.update { it.copy(misResenas = resenas) } }
        }
    }

    fun onAction(action: ListAction) {
        val lista = _state.value.vista?.lista
        when (action) {
            ListAction.OnRenombrarClicked -> _state.update { it.copy(renombrando = lista?.name.orEmpty()) }
            is ListAction.OnNombreChanged -> _state.update { it.copy(renombrando = action.name.take(60)) }
            ListAction.OnNombreGuardado -> {
                val nombre = _state.value.renombrando
                if (lista != null && !nombre.isNullOrBlank()) listsRepository.guardar(lista.copy(name = nombre))
                _state.update { it.copy(renombrando = null) }
            }
            ListAction.OnVisibleToggled -> lista?.let { listsRepository.guardar(it.copy(visible = !it.visible)) }
            ListAction.OnAnadirClicked -> _state.update { it.copy(eligiendo = true, filtroResenas = "") }
            ListAction.OnSelectorCerrado -> _state.update { it.copy(eligiendo = false) }
            // tocar algo que ya esta en la lista lo quita; si no estaba, se añade al final
            is ListAction.OnMediaToggled -> lista?.let { l ->
                val dentro = l.media.any { it.id == action.media.id }
                listsRepository.guardar(l.copy(media = if (dentro) l.media.filter { it.id != action.media.id } else l.media + action.media))
            }
            is ListAction.OnResenaToggled -> lista?.let { l ->
                val dentro = action.logId in l.logIds
                listsRepository.guardar(l.copy(logIds = if (dentro) l.logIds - action.logId else l.logIds + action.logId))
            }
            is ListAction.OnFiltroChanged -> _state.update { it.copy(filtroResenas = action.texto) }
            ListAction.OnBorrarClicked -> _state.update { it.copy(confirmandoBorrado = true) }
            ListAction.OnBorrarCancelado -> _state.update { it.copy(confirmandoBorrado = false) }
            ListAction.OnBorrarConfirmado -> listsRepository.borrar(listId)
            // volver y abrir fichas o reseñas es navegacion: lo resuelve ListScreenRoot
            else -> {}
        }
    }
}
