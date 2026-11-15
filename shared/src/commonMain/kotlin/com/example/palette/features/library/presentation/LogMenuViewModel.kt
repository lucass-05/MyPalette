package com.example.palette.features.library.presentation

import androidx.lifecycle.ViewModel
import com.example.palette.core.model.LogEntry
import com.example.palette.features.library.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LogMenuState(
    val log: LogEntry,
    // true mientras se pregunta si de verdad quiere eliminarlo
    val confirmandoBorrado: Boolean = false,
)

sealed interface LogMenuAction {
    data object OnDismiss : LogMenuAction
    data object OnFijarClicked : LogMenuAction
    data object OnOcultarClicked : LogMenuAction
    data object OnEditarClicked : LogMenuAction
    data object OnEnviarClicked : LogMenuAction
    data object OnVerFichaClicked : LogMenuAction
    data object OnBorrarClicked : LogMenuAction
    data object OnBorrarCancelado : LogMenuAction
    data object OnBorrarConfirmado : LogMenuAction
}

class LogMenuViewModel(
    log: LogEntry,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LogMenuState(log))
    val state: StateFlow<LogMenuState> = _state.asStateFlow()

    fun onAction(action: LogMenuAction) {
        val logId = _state.value.log.id
        when (action) {
            LogMenuAction.OnFijarClicked -> libraryRepository.alternarFijado(logId)
            LogMenuAction.OnOcultarClicked -> libraryRepository.alternarOculto(logId)
            LogMenuAction.OnBorrarClicked -> _state.update { it.copy(confirmandoBorrado = true) }
            LogMenuAction.OnBorrarCancelado -> _state.update { it.copy(confirmandoBorrado = false) }
            LogMenuAction.OnBorrarConfirmado -> libraryRepository.borrarLog(logId)
            // editar, enviar y ver la ficha abren otra pantalla u otra hoja: lo resuelve LogMenuSheetRoot
            else -> {}
        }
    }
}
