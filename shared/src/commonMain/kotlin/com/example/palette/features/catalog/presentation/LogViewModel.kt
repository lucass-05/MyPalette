package com.example.palette.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.metricas.Evento
import com.example.palette.core.metricas.Metricas
import com.example.palette.core.metricas.SinMetricas
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.features.catalog.domain.usecases.SearchMediaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// clase state: todo lo que la pantalla de registrar necesita para pintarse
data class LogState(
    val consulta: String = "",
    val tipos: Set<MediaType> = emptySet(),
    // null: todavia no se ha buscado nada. lista vacia: se ha buscado y no hay resultados
    val resultados: List<Media>? = null,
    val buscando: Boolean = false,
    val noDisponibles: Set<MediaType> = emptySet(),
)

// interfaz action: lo que puede hacer el usuario en esta pantalla
sealed interface LogAction {
    data class OnConsultaChanged(val value: String) : LogAction
    data class OnTipoToggled(val tipo: MediaType) : LogAction
    data class OnResultadoClicked(val media: Media) : LogAction
}

class LogViewModel(
    private val searchMediaUseCase: SearchMediaUseCase,
    noDisponibles: Set<MediaType>,
    private val metricas: Metricas = SinMetricas,
) : ViewModel() {

    // estado privado modificable y estado publico de solo lectura
    private val _state = MutableStateFlow(LogState(noDisponibles = noDisponibles))
    val state: StateFlow<LogState> = _state.asStateFlow()

    // la busqueda en marcha: se guarda su Job para poder cancelarla si el usuario sigue escribiendo
    private var busqueda: Job? = null

    fun onAction(action: LogAction) {
        when (action) {
            is LogAction.OnConsultaChanged -> {
                _state.update { it.copy(consulta = action.value) }
                buscar()
            }
            is LogAction.OnTipoToggled -> {
                _state.update {
                    it.copy(tipos = if (action.tipo in it.tipos) it.tipos - action.tipo else it.tipos + action.tipo)
                }
                buscar()
            }
            // abrir la ficha es navegar: eso lo resuelve LogScreenRoot, no el viewmodel
            is LogAction.OnResultadoClicked -> {}
        }
    }

    private fun buscar() {
        // lo que estuviera buscandose ya no interesa
        busqueda?.cancel()
        val actual = _state.value

        // viewModelScope: la corrutina se cancela sola si la pantalla se cierra de verdad
        busqueda = viewModelScope.launch {
            _state.update { it.copy(buscando = true) }
            // pequeña espera: si llega otra letra antes, esta corrutina se cancela y no se gasta una peticion
            delay(ESPERA)
            val resultados = searchMediaUseCase(actual.consulta, actual.tipos)
            _state.update { it.copy(resultados = resultados, buscando = false) }
            // lo que mas se busca dice que contenidos interesan (y a que marcas les interesa anunciarse)
            metricas.registrar(
                Evento.BUSCAR,
                mapOf("consulta" to actual.consulta.trim().lowercase().take(60), "tipos" to actual.tipos.joinToString(",") { it.name }, "resultados" to (resultados?.size ?: 0).toString()),
            )
        }
    }

    private companion object {
        const val ESPERA = 350L
    }
}
