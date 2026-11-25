package com.example.palette.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.metricas.Evento
import com.example.palette.core.metricas.Metricas
import com.example.palette.core.model.Media
import com.example.palette.features.anuncios.domain.model.Anuncio
import com.example.palette.features.anuncios.domain.model.AnunciosDeDescubrir
import com.example.palette.features.anuncios.domain.usecases.ElegirAnunciosUseCase
import com.example.palette.features.catalog.domain.model.Estreno
import com.example.palette.features.catalog.domain.model.FilaRecomendada
import com.example.palette.features.catalog.domain.model.TipoDeFila
import com.example.palette.features.catalog.domain.usecases.GetEstrenosUseCase
import com.example.palette.features.catalog.domain.usecases.GetRecommendationsUseCase
import com.example.palette.features.library.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverState(
    val filas: List<FilaRecomendada> = emptyList(),
    val cargando: Boolean = true,
    // false si todavia no he puntuado nada con buena nota
    val hayFavoritos: Boolean = false,
    // los huecos de publicidad (vacios mientras los anuncios esten apagados)
    val anuncios: AnunciosDeDescubrir = AnunciosDeDescubrir(),
    // el carrusel de arriba: lo mas visto de lo estrenado este mes
    val estrenos: List<Estreno> = emptyList(),
)

sealed interface DiscoverAction {
    // fila y posicion son para las metricas: en que fila y en que puesto se toco
    data class OnMediaClicked(val media: Media, val fila: String = "", val posicion: Int = -1) : DiscoverAction
    data object OnRegistrarClicked : DiscoverAction
    data class OnAnuncioVisto(val anuncio: Anuncio) : DiscoverAction
    data class OnAnuncioClicked(val anuncio: Anuncio) : DiscoverAction
    data class OnTrailerClicked(val estreno: Estreno) : DiscoverAction
}

class DiscoverViewModel(
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val libraryRepository: LibraryRepository,
    private val elegirAnunciosUseCase: ElegirAnunciosUseCase,
    private val getEstrenosUseCase: GetEstrenosUseCase,
    private val metricas: Metricas,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = _state.asStateFlow()

    // lo ultimo que se pidio a la red y con que reseñas, para no repetir peticiones
    private var calculadas: List<FilaRecomendada> = emptyList()
    private var clave: String? = null

    init {
        // los estrenos son iguales para todos: se piden una vez, a la vez que las recomendaciones
        viewModelScope.launch {
            val estrenos = getEstrenosUseCase()
            _state.update { it.copy(estrenos = estrenos) }
        }
        viewModelScope.launch {
            // collectLatest: si la biblioteca cambia mientras se estaban calculando recomendaciones,
            // se cancela ese calculo y se empieza con los datos nuevos
            libraryRepository.observar().collectLatest { biblioteca ->
                val nuevaClave = getRecommendationsUseCase.clave(biblioteca.logs)
                // solo se vuelve a preguntar a las bases de datos si han cambiado mis ultimas reseñas o mis series
                if (nuevaClave != clave) {
                    _state.update { it.copy(cargando = true) }
                    calculadas = getRecommendationsUseCase(biblioteca.logs)
                    clave = nuevaClave
                }
                // quitar lo que ya tengo no cuesta nada: se hace cada vez que cambia la biblioteca
                _state.update {
                    it.copy(
                        filas = getRecommendationsUseCase.sinLoQueYaTengo(calculadas, biblioteca.idsQueTengo),
                        cargando = false,
                        hayFavoritos = getRecommendationsUseCase.ultimasBuenas(biblioteca.logs).isNotEmpty(),
                        anuncios = elegirAnunciosUseCase(calculadas.firstOrNull { f -> f.tipo == TipoDeFila.TENDENCIAS }?.contenidos.orEmpty()),
                    )
                }
            }
        }
    }

    // abrir fichas y enlaces lo resuelve DiscoverScreenRoot; aqui solo se apunta en las metricas
    fun onAction(action: DiscoverAction) {
        when (action) {
            is DiscoverAction.OnMediaClicked -> metricas.registrar(
                Evento.CLIC_RECOMENDACION,
                mapOf("fila" to action.fila, "posicion" to action.posicion.toString(), "tipo" to action.media.type.name),
            )
            is DiscoverAction.OnAnuncioVisto -> metricas.registrar(Evento.ANUNCIO_VISTO, datosDe(action.anuncio))
            is DiscoverAction.OnAnuncioClicked -> metricas.registrar(Evento.ANUNCIO_CLIC, datosDe(action.anuncio))
            is DiscoverAction.OnTrailerClicked -> metricas.registrar(Evento.TRAILER, mapOf("tipo" to action.estreno.media.type.name))
            DiscoverAction.OnRegistrarClicked -> {}
        }
    }

    private fun datosDe(anuncio: Anuncio) = mapOf("anuncio" to anuncio.id, "hueco" to anuncio.hueco.name, "ejemplo" to anuncio.ejemplo.toString())
}
