package com.example.palette.features.people.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.User
import com.example.palette.features.people.domain.model.Gente
import com.example.palette.features.people.domain.repository.PeopleRepository
import com.example.palette.features.people.domain.usecases.SearchPeopleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PeopleState(
    val consulta: String = "",
    val resultados: List<User> = emptyList(),
    val siguiendo: Set<String> = emptySet(),
)

sealed interface PeopleAction {
    data object OnBackClicked : PeopleAction
    data class OnConsultaChanged(val value: String) : PeopleAction
    data class OnUserClicked(val userId: String) : PeopleAction
    data class OnFollowClicked(val userId: String) : PeopleAction
}

class PeopleViewModel(
    private val peopleRepository: PeopleRepository,
    private val searchPeopleUseCase: SearchPeopleUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PeopleState())
    val state: StateFlow<PeopleState> = _state.asStateFlow()

    private var gente = Gente()

    init {
        viewModelScope.launch {
            peopleRepository.observar().collect {
                gente = it
                buscar()
            }
        }
    }

    fun onAction(action: PeopleAction) {
        when (action) {
            is PeopleAction.OnConsultaChanged -> {
                _state.update { it.copy(consulta = action.value) }
                buscar()
            }
            is PeopleAction.OnFollowClicked -> peopleRepository.alternarSeguir(action.userId)
            else -> {}
        }
    }

    // se repite la busqueda cuando cambia el texto y tambien cuando cambia la gente (alguien nuevo, un seguido mas)
    private fun buscar() {
        _state.update { it.copy(resultados = searchPeopleUseCase(gente, it.consulta), siguiendo = gente.siguiendo) }
    }
}
