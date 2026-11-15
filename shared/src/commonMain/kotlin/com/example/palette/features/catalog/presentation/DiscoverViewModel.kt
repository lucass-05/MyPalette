package com.example.palette.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.Media
import com.example.palette.features.catalog.domain.model.FilaRecomendada
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
)

sealed interface DiscoverAction {
    data class OnMediaClicked(val media: Media) : DiscoverAction
    data object OnRegistrarClicked : DiscoverAction
}

class DiscoverViewModel(
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = _state.asStateFlow()

    // lo ultimo que se pidio a la red y para que favoritos era, para no repetir peticiones
    private var calculadas: List<FilaRecomendada> = emptyList()
    private var clave: String? = null

    init {
        viewModelScope.launch {
            // collectLatest: si la biblioteca cambia mientras se estaban calculando recomendaciones,
            // se cancela ese calculo y se empieza con los datos nuevos
            libraryRepository.observar().collectLatest { biblioteca ->
                val favoritos = getRecommendationsUseCase.favoritos(biblioteca.logs)
                val nuevaClave = favoritos.joinToString("|") { it.media.id }
                // solo se vuelve a preguntar a las bases de datos si han cambiado mis favoritos
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
                        hayFavoritos = favoritos.isNotEmpty(),
                    )
                }
            }
        }
    }

    // las dos acciones son navegacion: las resuelve DiscoverScreenRoot
    fun onAction(action: DiscoverAction) {}
}
