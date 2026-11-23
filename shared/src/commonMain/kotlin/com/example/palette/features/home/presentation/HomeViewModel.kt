package com.example.palette.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.features.home.domain.model.Feed
import com.example.palette.features.home.domain.repository.FeedRepository
import com.example.palette.features.library.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(val feed: Feed = Feed())

sealed interface HomeAction {
    data object OnPeopleClicked : HomeAction
    data class OnStoryClicked(val userId: String) : HomeAction
    data class OnMediaClicked(val media: Media) : HomeAction
    data class OnUserClicked(val userId: String) : HomeAction
    data class OnLikeClicked(val logId: String) : HomeAction
    data class OnSendClicked(val log: LogEntry) : HomeAction
    data class OnResenaClicked(val logId: String) : HomeAction
    data class OnWatchlistClicked(val media: Media) : HomeAction
}

class HomeViewModel(
    private val feedRepository: FeedRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        // cada vez que cambia algo (un log nuevo de un amigo, un me gusta...) llega un feed nuevo
        viewModelScope.launch {
            feedRepository.observar().collect { feed -> _state.update { it.copy(feed = feed) } }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnLikeClicked -> feedRepository.alternarMeGusta(action.logId)
            is HomeAction.OnWatchlistClicked -> libraryRepository.alternarPendiente(action.media)
            // el resto son navegacion: lo resuelve HomeScreenRoot
            else -> {}
        }
    }
}
