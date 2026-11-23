package com.example.palette.features.tutorial.presentation

import androidx.lifecycle.ViewModel
import com.example.palette.core.util.ahora
import com.example.palette.features.tutorial.domain.model.Ejemplos
import com.example.palette.features.tutorial.domain.repository.TutorialRepository
import com.example.palette.features.tutorial.domain.usecases.PrepararEjemplosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// el paso en el que se esta lo lleva el pager de la pantalla (es estado de la interfaz, no un dato)
data class TutorialState(val ejemplos: Ejemplos)

sealed interface TutorialAction {
    data object OnSaltarClicked : TutorialAction
    data object OnEmpezarClicked : TutorialAction
    data object OnPersonalizarClicked : TutorialAction
}

class TutorialViewModel(
    private val tutorialRepository: TutorialRepository,
    prepararEjemplos: PrepararEjemplosUseCase,
) : ViewModel() {

    // los ejemplos se preparan una vez al abrirlo: mientras se ve el tutorial no hace falta que cambien
    private val _state = MutableStateFlow(
        TutorialState(prepararEjemplos(tutorialRepository.yo(), tutorialRepository.misLogs(), tutorialRepository.misPendientes(), ahora())),
    )
    val state: StateFlow<TutorialState> = _state.asStateFlow()

    // las tres formas de salir cuentan como visto. la de personalizar ademas abre el pincel (lo decide la navegacion)
    fun onAction(action: TutorialAction) {
        when (action) {
            TutorialAction.OnSaltarClicked, TutorialAction.OnEmpezarClicked, TutorialAction.OnPersonalizarClicked -> tutorialRepository.marcarVisto()
        }
    }
}
