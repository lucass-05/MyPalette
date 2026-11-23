package com.example.palette.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.data.Stage
import com.example.palette.core.ui.Pigment
import com.example.palette.features.auth.domain.repository.AuthRepository
import com.example.palette.features.messages.domain.repository.MessagesRepository
import com.example.palette.features.profile.domain.repository.ProfileRepository
import com.example.palette.features.tutorial.domain.repository.TutorialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppState(
    val etapa: Stage,
    // mensajes y avisos sin leer: el punto de la barra
    val sinLeer: Int = 0,
    // true mientras haya que enseñar el tutorial (la primera vez, o al pedirlo otra vez desde ajustes)
    val tutorial: Boolean = false,
)

// el viewmodel de la app entera: en que etapa esta la sesion, los colores elegidos y el refresco periodico
class AppViewModel(
    private val authRepository: AuthRepository,
    private val messagesRepository: MessagesRepository,
    private val profileRepository: ProfileRepository,
    private val tutorialRepository: TutorialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState(authRepository.etapa()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // los colores guardados se ponen antes de pintar nada, para que la app no parpadee al abrir...
        Pigment.colors = profileRepository.tema()
        // ...y se vuelven a poner cada vez que se cambian en ajustes
        viewModelScope.launch {
            profileRepository.observarTema().collect { Pigment.colors = it }
        }
        viewModelScope.launch {
            authRepository.observarEtapa().collect { etapa -> _state.update { it.copy(etapa = etapa) } }
        }
        viewModelScope.launch {
            messagesRepository.observarSinLeer().collect { sinLeer -> _state.update { it.copy(sinLeer = sinLeer) } }
        }
        viewModelScope.launch {
            tutorialRepository.observarPendiente().collect { pendiente -> _state.update { it.copy(tutorial = pendiente) } }
        }
        // mientras hay sesion se pregunta al servidor cada 15 segundos.
        // collectLatest cancela el bucle anterior cuando la sesion se abre o se cierra
        viewModelScope.launch {
            authRepository.observarEtapa().map { it != Stage.SIGNED_OUT }.distinctUntilChanged().collectLatest { conSesion ->
                while (conSesion) {
                    authRepository.sincronizar()
                    delay(15_000)
                }
            }
        }
    }
}
