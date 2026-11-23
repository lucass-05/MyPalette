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

/** las paginas del perfil que se pueden configurar (la paleta esta siempre). */
sealed interface Pagina {
    data object Favoritos : Pagina
    data object Diario : Pagina
    data object Pendientes : Pagina
    data class Lista(val listId: String) : Pagina
}

data class CustomizeState(
    val yo: User,
    // lo que se esta editando: se guarda todo junto al pulsar guardar
    val layout: ProfileLayout,
    val background: Long?,
    val pendientesPublicos: Boolean,
    val listas: List<ListaVista> = emptyList(),
    val subiendoBanner: Boolean = false,
    val problema: String? = null,
    // hojas abiertas
    val eligiendoBloque: Boolean = false,
    val eligiendoFondo: Boolean = false,
    // el bloque de portadas elegidas al que se le estan añadiendo cosas con el buscador
    val llenando: String? = null,
    // true cuando ya se ha guardado: la pantalla se cierra
    val guardado: Boolean = false,
) {
    val widgets: List<ProfileWidget> get() = layout.widgets

    fun visible(pagina: Pagina): Boolean = when (pagina) {
        Pagina.Favoritos -> layout.favoritesVisible
        Pagina.Diario -> layout.diaryVisible
        Pagina.Pendientes -> pendientesPublicos
        is Pagina.Lista -> listas.firstOrNull { it.lista.id == pagina.listId }?.lista?.visible == true
    }

    fun enManchas(pagina: Pagina): Boolean = when (pagina) {
        Pagina.Favoritos -> layout.favoritesBlob
        Pagina.Diario -> layout.diaryBlob
        Pagina.Pendientes -> layout.watchlistBlob
        is Pagina.Lista -> pagina.listId in layout.blobLists
    }
}

sealed interface CustomizeAction {
    data object OnGuardarClicked : CustomizeAction
    data object OnAjustesClicked : CustomizeAction
    data class OnBannerElegido(val foto: ByteArray) : CustomizeAction
    data object OnQuitarBannerClicked : CustomizeAction
    data object OnFondoClicked : CustomizeAction
    data class OnFondoChanged(val background: Long?) : CustomizeAction
    data object OnFondoCerrado : CustomizeAction
    data class OnPaginaVisibleToggled(val pagina: Pagina) : CustomizeAction
    data class OnPaginaManchasChanged(val pagina: Pagina, val manchas: Boolean) : CustomizeAction
    data object OnAnadirBloqueClicked : CustomizeAction
    data object OnAnadirBloqueCerrado : CustomizeAction
    data class OnBloqueElegido(val kind: WidgetKind) : CustomizeAction
    data class OnTituloChanged(val widgetId: String, val title: String) : CustomizeAction
    data class OnTextoChanged(val widgetId: String, val text: String) : CustomizeAction
    data class OnCantidadChanged(val widgetId: String, val count: Int) : CustomizeAction
    data class OnManchasChanged(val widgetId: String, val manchas: Boolean) : CustomizeAction
    data class OnListaDelBloqueChanged(val widgetId: String, val listId: String) : CustomizeAction
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

    // arranca con lo que hay guardado. si nunca se ha personalizado, con los bloques de por defecto ya puestos
    private val _state = MutableStateFlow(
        profileRepository.yo().let { yo ->
            CustomizeState(yo, yo.layout.copy(widgets = yo.layout.bloques), yo.background, yo.watchlistPublic)
        },
    )
    val state: StateFlow<CustomizeState> = _state.asStateFlow()

    init {
        // la portada se sube al momento, asi que se escucha el perfil para enseñar la nueva
        viewModelScope.launch {
            profileRepository.observarYo().collect { yo -> _state.update { it.copy(yo = yo) } }
        }
        viewModelScope.launch {
            listsRepository.observarListas(ME).collect { listas -> _state.update { it.copy(listas = listas) } }
        }
    }

    fun onAction(action: CustomizeAction) {
        when (action) {
            CustomizeAction.OnGuardarClicked -> guardar()
            // antes de ir a los ajustes se guarda lo que haya: asi no se pierde nada al cambiar de pantalla
            CustomizeAction.OnAjustesClicked -> guardar(cerrar = false)
            is CustomizeAction.OnBannerElegido -> subirBanner(action.foto)
            CustomizeAction.OnQuitarBannerClicked -> viewModelScope.launch {
                val problema = profileRepository.ponerBanner(null)
                _state.update { it.copy(problema = problema) }
            }
            CustomizeAction.OnFondoClicked -> _state.update { it.copy(eligiendoFondo = true) }
            is CustomizeAction.OnFondoChanged -> _state.update { it.copy(background = action.background) }
            CustomizeAction.OnFondoCerrado -> _state.update { it.copy(eligiendoFondo = false) }
            is CustomizeAction.OnPaginaVisibleToggled -> cambiarVisible(action.pagina)
            is CustomizeAction.OnPaginaManchasChanged -> _state.update { s ->
                val l = s.layout
                s.copy(
                    layout = when (val p = action.pagina) {
                        Pagina.Favoritos -> l.copy(favoritesBlob = action.manchas)
                        Pagina.Diario -> l.copy(diaryBlob = action.manchas)
                        Pagina.Pendientes -> l.copy(watchlistBlob = action.manchas)
                        is Pagina.Lista -> l.copy(blobLists = if (action.manchas) l.blobLists + p.listId else l.blobLists - p.listId)
                    },
                )
            }
            CustomizeAction.OnAnadirBloqueClicked -> _state.update { it.copy(eligiendoBloque = true) }
            CustomizeAction.OnAnadirBloqueCerrado -> _state.update { it.copy(eligiendoBloque = false) }
            is CustomizeAction.OnBloqueElegido -> anadirBloque(action.kind)
            is CustomizeAction.OnTituloChanged -> editar(action.widgetId) { it.copy(title = action.title.take(60)) }
            is CustomizeAction.OnTextoChanged -> editar(action.widgetId) { it.copy(text = action.text.take(MAX_TEXTO)) }
            is CustomizeAction.OnCantidadChanged -> editar(action.widgetId) { it.copy(count = action.count.coerceIn(1, MAX_CANTIDAD)) }
            is CustomizeAction.OnManchasChanged -> editar(action.widgetId) { it.copy(blob = action.manchas) }
            is CustomizeAction.OnListaDelBloqueChanged -> editar(action.widgetId) { it.copy(listId = action.listId) }
            is CustomizeAction.OnSubirClicked -> mover(action.widgetId, -1)
            is CustomizeAction.OnBajarClicked -> mover(action.widgetId, +1)
            is CustomizeAction.OnOcultarClicked -> editar(action.widgetId) { it.copy(hidden = !it.hidden) }
            is CustomizeAction.OnBorrarBloqueClicked -> cambiarBloques { lista -> lista.filter { it.id != action.widgetId } }
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
            // tocar una portada elegida cambia entre mancha de pintura y recuadro
            is CustomizeAction.OnPortadaTocada -> editar(action.widgetId) { w ->
                w.copy(items = w.items.map { if (it.media.id == action.mediaId) it.copy(blob = !it.blob) else it })
            }
            is CustomizeAction.OnQuitarPortadaClicked -> editar(action.widgetId) { w ->
                w.copy(items = w.items.filter { it.media.id != action.mediaId })
            }
        }
    }

    private fun guardar(cerrar: Boolean = true) {
        val datos = _state.value
        // custom = true: a partir de ahora la paleta es la que se ha elegido, aunque se quede vacia
        profileRepository.guardarDiseno(datos.layout.copy(custom = true), datos.background, datos.pendientesPublicos)
        if (cerrar) _state.update { it.copy(guardado = true) }
    }

    private fun cambiarVisible(pagina: Pagina) {
        when (pagina) {
            Pagina.Favoritos -> _state.update { it.copy(layout = it.layout.copy(favoritesVisible = !it.layout.favoritesVisible)) }
            Pagina.Diario -> _state.update { it.copy(layout = it.layout.copy(diaryVisible = !it.layout.diaryVisible)) }
            Pagina.Pendientes -> _state.update { it.copy(pendientesPublicos = !it.pendientesPublicos) }
            // las listas guardan su visibilidad ellas mismas, al momento
            is Pagina.Lista -> _state.value.listas.firstOrNull { it.lista.id == pagina.listId }?.let {
                listsRepository.guardar(it.lista.copy(visible = !it.lista.visible))
            }
        }
    }

    private fun anadirBloque(kind: WidgetKind) {
        val primeraLista = _state.value.listas.firstOrNull()?.lista
        val bloque = ProfileWidget(
            id = nuevoId("w"),
            kind = kind,
            title = when (kind) {
                WidgetKind.TEXT -> "Sobre mí"
                WidgetKind.FAVORITES -> "Favoritos"
                WidgetKind.DIARY -> "Diario"
                WidgetKind.WATCHLIST -> "Pendientes"
                // el de una lista toma el nombre de la lista (se deja vacio)
                WidgetKind.LIST -> ""
                WidgetKind.SHELF -> "Mis elegidos"
            },
            blob = kind == WidgetKind.FAVORITES,
            listId = if (kind == WidgetKind.LIST) primeraLista?.id else null,
        )
        _state.update {
            it.copy(
                layout = it.layout.copy(widgets = it.widgets + bloque),
                eligiendoBloque = false,
                // un bloque de portadas elegidas nace vacio: se abre directamente el buscador para llenarlo
                llenando = if (kind == WidgetKind.SHELF) bloque.id else null,
            )
        }
    }

    private fun cambiarBloques(cambio: (List<ProfileWidget>) -> List<ProfileWidget>) {
        _state.update { it.copy(layout = it.layout.copy(widgets = cambio(it.widgets))) }
    }

    private fun editar(widgetId: String, cambio: (ProfileWidget) -> ProfileWidget) {
        cambiarBloques { lista -> lista.map { if (it.id == widgetId) cambio(it) else it } }
    }

    private fun mover(widgetId: String, salto: Int) {
        cambiarBloques { bloques ->
            val desde = bloques.indexOfFirst { it.id == widgetId }
            val hasta = desde + salto
            if (desde < 0 || hasta !in bloques.indices) {
                bloques
            } else {
                bloques.toMutableList().apply { add(hasta, removeAt(desde)) }
            }
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
        const val MAX_CANTIDAD = 30
    }
}
