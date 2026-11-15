package com.example.palette.features.home.presentation

import androidx.lifecycle.ViewModel
import com.example.palette.core.model.LogEntry
import com.example.palette.features.home.domain.model.Historia
import com.example.palette.features.home.domain.repository.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StoryState(
    // null si esa persona no tiene historia (ya ha caducado, por ejemplo)
    val historia: Historia? = null,
    val index: Int = 0,
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
    data object OnResponderClicked : StoryAction
}

class StoryViewModel(
    private val userId: String,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    // la historia se lee una sola vez al abrir: marcar como visto no debe mover la lista
    private val _state = MutableStateFlow(StoryState(historia = feedRepository.historiaDe(userId)))
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
            else -> {}
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
