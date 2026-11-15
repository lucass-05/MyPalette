package com.example.palette.features.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.Media
import com.example.palette.features.library.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewState(
    val media: Media,
    val stars: Float = 0f,
    val review: String = "",
    // true si ya tenia un log de este contenido: se esta editando en vez de creando
    val editando: Boolean = false,
)

sealed interface ReviewAction {
    data object OnDismiss : ReviewAction
    data class OnStarsChanged(val stars: Float) : ReviewAction
    data class OnReviewChanged(val review: String) : ReviewAction
    data object OnGuardarClicked : ReviewAction
}

class ReviewViewModel(
    media: Media,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewState(media))
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // first() coge solo el valor actual del flujo, sin quedarse escuchando:
            // si ya habia puntuado esto, la hoja arranca con esa nota y esa reseña
            val existente = libraryRepository.observar().first().logs.firstOrNull { it.media.id == media.id }
            if (existente != null) _state.update { it.copy(stars = existente.stars, review = existente.review, editando = true) }
        }
    }

    fun onAction(action: ReviewAction) {
        when (action) {
            is ReviewAction.OnStarsChanged -> _state.update { it.copy(stars = action.stars) }
            is ReviewAction.OnReviewChanged -> _state.update { it.copy(review = action.review.take(2000)) }
            ReviewAction.OnGuardarClicked -> _state.value.let { libraryRepository.guardarLog(it.media, it.stars, it.review) }
            ReviewAction.OnDismiss -> {}
        }
    }
}
