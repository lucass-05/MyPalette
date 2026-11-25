package com.example.palette.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.metricas.Evento
import com.example.palette.core.metricas.Metricas
import com.example.palette.core.model.Extras
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.monetizacion.Afiliados
import com.example.palette.core.monetizacion.Oferta
import com.example.palette.features.catalog.domain.usecases.GetMediaDetailUseCase
import com.example.palette.features.library.domain.model.LogDeAmigo
import com.example.palette.features.library.domain.repository.LibraryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailState(
    val media: Media,
    val extras: Extras = Extras(),
    val parecidos: List<Media> = emptyList(),
    val miLog: LogEntry? = null,
    val enPendientes: Boolean = false,
    val logsDeAmigos: List<LogDeAmigo> = emptyList(),
    // nota que le he puesto a cada cancion del album, por id
    val misNotas: Map<String, Float> = emptyMap(),
    val enlaceCopiado: Boolean = false,
)

sealed interface DetailAction {
    data object OnBackClicked : DetailAction
    data object OnReviewClicked : DetailAction
    data object OnWatchlistClicked : DetailAction
    data object OnSendClicked : DetailAction
    data object OnShareClicked : DetailAction
    data class OnMediaClicked(val media: Media) : DetailAction
    data class OnUserClicked(val userId: String) : DetailAction
    data class OnResenaClicked(val logId: String) : DetailAction
    // salir de la app: a una plataforma de "dónde verlo" o a una tienda (enlaces de afiliado)
    data class OnPlataformaClicked(val plataforma: String, val enlace: String) : DetailAction
    data class OnOfertaClicked(val oferta: Oferta) : DetailAction
}

class DetailViewModel(
    inicial: Media,
    private val getMediaDetailUseCase: GetMediaDetailUseCase,
    private val libraryRepository: LibraryRepository,
    private val metricas: Metricas,
) : ViewModel() {

    // la ficha se pinta al momento con lo que ya se sabe del contenido...
    private val _state = MutableStateFlow(DetailState(media = inicial))
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        metricas.registrar(Evento.ABRIR_FICHA, mapOf("tipo" to inicial.type.name))
        // ...y se completa cuando llegan los detalles de la red
        viewModelScope.launch {
            val ficha = getMediaDetailUseCase(inicial)
            _state.update { it.copy(media = ficha.media, extras = ficha.extras, parecidos = ficha.parecidos) }
        }
        // en paralelo se escucha la biblioteca: si puntuo esto o lo guardo, la ficha se entera sola
        viewModelScope.launch {
            libraryRepository.observar().collect { biblioteca ->
                _state.update { actual ->
                    actual.copy(
                        miLog = biblioteca.logs.firstOrNull { it.media.id == actual.media.id },
                        enPendientes = biblioteca.pendientes.any { it.id == actual.media.id },
                        logsDeAmigos = biblioteca.logsDeAmigos.filter { it.log.media.id == actual.media.id },
                        misNotas = biblioteca.logs.associate { it.media.id to it.stars },
                    )
                }
            }
        }
    }

    fun onAction(action: DetailAction) {
        when (action) {
            is DetailAction.OnWatchlistClicked -> libraryRepository.alternarPendiente(_state.value.media)
            is DetailAction.OnShareClicked -> avisarDeCopiado()
            // abrir el enlace lo hace la pantalla; aqui se apunta, que es lo que se cobra
            is DetailAction.OnPlataformaClicked -> metricas.registrar(
                Evento.CLIC_PLATAFORMA,
                mapOf("plataforma" to action.plataforma, "tipo" to _state.value.media.type.name, "afiliado" to Afiliados.esDeAfiliado(action.enlace).toString()),
            )
            is DetailAction.OnOfertaClicked -> metricas.registrar(
                Evento.CLIC_AFILIADO,
                mapOf("tienda" to action.oferta.tienda, "tipo" to _state.value.media.type.name, "afiliado" to Afiliados.esDeAfiliado(action.oferta.enlace).toString()),
            )
            // el resto son navegacion o abrir hojas: lo resuelve DetailScreenRoot
            else -> {}
        }
    }

    // enseña "enlace copiado" un par de segundos
    private fun avisarDeCopiado() {
        viewModelScope.launch {
            _state.update { it.copy(enlaceCopiado = true) }
            delay(1800)
            _state.update { it.copy(enlaceCopiado = false) }
        }
    }
}
