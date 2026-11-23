package com.example.palette.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.model.ProfileLayout
import com.example.palette.core.model.ProfileWidget
import com.example.palette.core.model.ShelfItem
import com.example.palette.core.model.User
import com.example.palette.core.model.WidgetKind
import com.example.palette.core.ui.prepararBanner
import com.example.palette.core.util.nuevoId
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.lists.domain.repository.ListsRepository
import com.example.palette.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomizeState(
    val yo: User,
    // lo que se esta editando. se guarda todo junto al salir
    val widgets: List<ProfileWidget>,
    val background: Long?,
    val listas: List<ListaVista> = emptyList(),
    val subiendoBanner: Boolean = false,
    val problema: String? = null,
    // hojas abiertas
    val eligiendoBloque: Boolean = false,
    val eligiendoFondo: Boolean = false,
    // el bloque de portadas al que se le estan añadiendo cosas con el buscador
    val llenando: String? = null,
    // true cuando ya se ha guardado: la pantalla se cierra
    val guardado: Boolean = false,
)

sealed interface CustomizeAction {
    data object OnGuardarClicked : CustomizeAction
    data class OnBannerElegido(val foto: ByteArray) : CustomizeAction
    data object OnQuitarBannerClicked : CustomizeAction
    data object OnFondoClicked : CustomizeAction
    data class OnFondoChanged(val background: Long?) : CustomizeAction
    data object OnFondoCerrado : CustomizeAction
    data class OnListaVisibleToggled(val listId: String) : CustomizeAction
    data object OnAnadirBloqueClicked : CustomizeAction
    data object OnAnadirBloqueCerrado : CustomizeAction
    data class OnBloqueElegido(val kind: WidgetKind) : CustomizeAction
    data class OnTituloChanged(val widgetId: String, val title: String) : CustomizeAction
    data class OnTextoChanged(val widgetId: String, val text: String) : CustomizeAction
    data class OnSubirClicked(val widgetId: String) : CustomizeAction
    data class OnBajarClicked(val widgetId: String) : CustomizeAction
    data class OnOcultarClicked(val widgetId: String) : CustomizeAction
    data class OnBorrarBloqueClicked(val widgetId: String) : CustomizeAction
    data class OnAnadirPortadasClicked(val widgetId: String) : CustomizeAction
    data class OnMediaElegida(val media: Media) : CustomizeAction
    data object OnSelectorCerrado : CustomizeAction
    data class OnPortadaTocada(val widgetId: String, val mediaId: String) : CustomizeAction
    data class OnQuitarPortadaClicked(val widgetId: String, val mediaId: String) : CustomizeAction
}

class CustomizeViewModel(
    private val profileRepository: ProfileRepository,
    private val listsRepository: ListsRepository,
) : ViewModel() {

    // arranca con lo que hay guardado en mi perfil
    private val _state = MutableStateFlow(
        profileRepository.yo().let { CustomizeState(it, it.layout.widgets, it.background) },
    )
    val state: StateFlow<CustomizeState> = _state.asStateFlow()

    init {
        // la foto de cabecera se sube al momento, asi que se escucha el perfil para enseñar la nueva
        viewModelScope.launch {
            profileRepository.observarYo().collect { yo -> _state.update { it.copy(yo = yo) } }
        }
        viewModelScope.launch {
            listsRepository.observarListas(ME).collect { listas -> _state.update { it.copy(listas = listas) } }
        }
    }

    fun onAction(action: CustomizeAction) {
        when (action) {
            CustomizeAction.OnGuardarClicked -> {
                val datos = _state.value
                profileRepository.guardarDiseno(ProfileLayout(datos.widgets), datos.background)
                _state.update { it.copy(guardado = true) }
            }
            is CustomizeAction.OnBannerElegido -> subirBanner(action.foto)
            CustomizeAction.OnQuitarBannerClicked -> viewModelScope.launch {
                val problema = profileRepository.ponerBanner(null)
                _state.update { it.copy(problema = problema) }
            }
            CustomizeAction.OnFondoClicked -> _state.update { it.copy(eligiendoFondo = true) }
            is CustomizeAction.OnFondoChanged -> _state.update { it.copy(background = action.background) }
            CustomizeAction.OnFondoCerrado -> _state.update { it.copy(eligiendoFondo = false) }
            is CustomizeAction.OnListaVisibleToggled -> _state.value.listas.firstOrNull { it.lista.id == action.listId }?.let {
                listsRepository.guardar(it.lista.copy(visible = !it.lista.visible))
            }
            CustomizeAction.OnAnadirBloqueClicked -> _state.update { it.copy(eligiendoBloque = true) }
            CustomizeAction.OnAnadirBloqueCerrado -> _state.update { it.copy(eligiendoBloque = false) }
            is CustomizeAction.OnBloqueElegido -> anadirBloque(action.kind)
            is CustomizeAction.OnTituloChanged -> editar(action.widgetId) { it.copy(title = action.title.take(60)) }
            is CustomizeAction.OnTextoChanged -> editar(action.widgetId) { it.copy(text = action.text.take(MAX_TEXTO)) }
            is CustomizeAction.OnSubirClicked -> mover(action.widgetId, -1)
            is CustomizeAction.OnBajarClicked -> mover(action.widgetId, +1)
            is CustomizeAction.OnOcultarClicked -> editar(action.widgetId) { it.copy(hidden = !it.hidden) }
            is CustomizeAction.OnBorrarBloqueClicked -> _state.update { s -> s.copy(widgets = s.widgets.filter { it.id != action.widgetId }) }
            is CustomizeAction.OnAnadirPortadasClicked -> _state.update { it.copy(llenando = action.widgetId) }
            CustomizeAction.OnSelectorCerrado -> _state.update { it.copy(llenando = null) }
            // en el buscador, tocar algo que ya esta en el bloque lo quita
            is CustomizeAction.OnMediaElegida -> _state.value.llenando?.let { id ->
                editar(id) { w ->
                    val dentro = w.items.any { it.media.id == action.media.id }
                    when {
                        dentro -> w.copy(items = w.items.filter { it.media.id != action.media.id })
                        w.items.size >= MAX_PORTADAS -> w
                        else -> w.copy(items = w.items + ShelfItem(action.media))
                    }
                }
            }
            // tocar una portada en el editor cambia entre mancha de pintura y portada rectangular
            is CustomizeAction.OnPortadaTocada -> editar(action.widgetId) { w ->
                w.copy(items = w.items.map { if (it.media.id == action.mediaId) it.copy(blob = !it.blob) else it })
            }
            is CustomizeAction.OnQuitarPortadaClicked -> editar(action.widgetId) { w ->
                w.copy(items = w.items.filter { it.media.id != action.mediaId })
            }
        }
    }

    private fun anadirBloque(kind: WidgetKind) {
        val titulo = when (kind) {
            WidgetKind.TEXT -> "Sobre mí"
            WidgetKind.SHELF -> "Mis favoritos"
            WidgetKind.DIARY -> "Diario"
        }
        val bloque = ProfileWidget(nuevoId("w"), kind, title = titulo)
        _state.update {
            it.copy(
                widgets = it.widgets + bloque,
                eligiendoBloque = false,
                // un bloque de portadas nace vacio: se abre directamente el buscador para llenarlo
                llenando = if (kind == WidgetKind.SHELF) bloque.id else null,
            )
        }
    }

    private fun editar(widgetId: String, cambio: (ProfileWidget) -> ProfileWidget) {
        _state.update { s -> s.copy(widgets = s.widgets.map { if (it.id == widgetId) cambio(it) else it }) }
    }

    private fun mover(widgetId: String, salto: Int) {
        _state.update { s ->
            val desde = s.widgets.indexOfFirst { it.id == widgetId }
            val hasta = desde + salto
            if (desde < 0 || hasta !in s.widgets.indices) return@update s
            val lista = s.widgets.toMutableList()
            lista.add(hasta, lista.removeAt(desde))
            s.copy(widgets = lista)
        }
    }

    private fun subirBanner(foto: ByteArray) {
        _state.update { it.copy(subiendoBanner = true, problema = null) }
        viewModelScope.launch {
            // se recorta a una franja apaisada y se reduce antes de subirla
            val banner = prepararBanner(foto)
            val problema = if (banner == null) "No se ha podido leer esa imagen. Prueba con un JPG o un PNG." else profileRepository.ponerBanner(banner)
            _state.update { it.copy(subiendoBanner = false, problema = problema) }
        }
    }

    private companion object {
        const val MAX_PORTADAS = 12
        const val MAX_TEXTO = 600
    }
}
