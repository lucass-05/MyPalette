package com.example.palette.features.tutorial.presentation

import androidx.lifecycle.ViewModel
import com.example.palette.core.metricas.Evento
import com.example.palette.core.metricas.Metricas
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
    // paso: en cual se salto. sirve para saber si el tutorial se hace largo
    data class OnSaltarClicked(val paso: Int = -1) : TutorialAction
    data object OnEmpezarClicked : TutorialAction
    data object OnPersonalizarClicked : TutorialAction
}

class TutorialViewModel(
    private val tutorialRepository: TutorialRepository,
    prepararEjemplos: PrepararEjemplosUseCase,
    private val metricas: Metricas,
) : ViewModel() {

    // los ejemplos se preparan una vez al abrirlo: mientras se ve el tutorial no hace falta que cambien
    private val _state = MutableStateFlow(
        TutorialState(prepararEjemplos(tutorialRepository.yo(), tutorialRepository.misLogs(), tutorialRepository.misPendientes(), ahora())),
    )
    val state: StateFlow<TutorialState> = _state.asStateFlow()

    // las tres formas de salir cuentan como visto. la de personalizar ademas abre el pincel (lo decide la navegacion)
    fun onAction(action: TutorialAction) {
        when (action) {
            is TutorialAction.OnSaltarClicked -> metricas.registrar(Evento.TUTORIAL, mapOf("accion" to "saltar", "paso" to action.paso.toString()))
            TutorialAction.OnEmpezarClicked -> metricas.registrar(Evento.TUTORIAL, mapOf("accion" to "terminar"))
            TutorialAction.OnPersonalizarClicked -> metricas.registrar(Evento.TUTORIAL, mapOf("accion" to "personalizar"))
        }
        tutorialRepository.marcarVisto()
    }
}
