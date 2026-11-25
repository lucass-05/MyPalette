package com.example.palette.features.reviews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.features.reviews.domain.model.Resena
import com.example.palette.features.reviews.domain.repository.ReviewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewDetailState(
    // null mientras llega o si la reseña ya no existe
    val resena: Resena? = null,
    val cargada: Boolean = false,
    // el comentario que se esta escribiendo
    val borrador: String = "",
)

sealed interface ReviewDetailAction {
    data object OnBackClicked : ReviewDetailAction
    data class OnBorradorChanged(val texto: String) : ReviewDetailAction
    data object OnComentarClicked : ReviewDetailAction
    data class OnBorrarComentarioClicked(val commentId: String) : ReviewDetailAction
    data object OnLikeClicked : ReviewDetailAction
    data class OnUserClicked(val userId: String) : ReviewDetailAction
    data class OnMediaClicked(val media: Media) : ReviewDetailAction
    data class OnEditarClicked(val media: Media) : ReviewDetailAction
    data class OnSendClicked(val log: LogEntry) : ReviewDetailAction
    data class OnMenuClicked(val log: LogEntry) : ReviewDetailAction
    data object OnCompartirClicked : ReviewDetailAction
}

class ReviewDetailViewModel(
    private val logId: String,
    private val reviewsRepository: ReviewsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewDetailState())
    val state: StateFlow<ReviewDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            reviewsRepository.observar(logId).collect { resena -> _state.update { it.copy(resena = resena, cargada = true) } }
        }
    }

    fun onAction(action: ReviewDetailAction) {
        when (action) {
            is ReviewDetailAction.OnBorradorChanged -> _state.update { it.copy(borrador = action.texto.take(1000)) }
            ReviewDetailAction.OnComentarClicked -> {
                val texto = _state.value.borrador
                if (texto.isBlank()) return
                reviewsRepository.comentar(logId, texto)
                _state.update { it.copy(borrador = "") }
            }
            is ReviewDetailAction.OnBorrarComentarioClicked -> reviewsRepository.borrarComentario(action.commentId)
            ReviewDetailAction.OnLikeClicked -> reviewsRepository.alternarMeGusta(logId)
            // volver, abrir perfiles o fichas, editar y enviar son navegacion: lo resuelve ReviewDetailScreenRoot
            else -> {}
        }
    }
}
