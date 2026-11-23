package com.example.palette.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.User
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.Chip
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.FollowButton
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.PaintBlob
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.proporcion
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.profile.presentation.ProfileAction
import com.example.palette.features.profile.presentation.ProfileState
import com.example.palette.features.profile.presentation.ProfileViewModel
import com.example.palette.features.profile.presentation.Seccion
import com.example.palette.navigation.BarSpace
import kotlin.random.Random

@Composable
fun ProfileScreenRoot(
    userId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenMedia: (Media) -> Unit,
    onOpenReview: (String) -> Unit,
    onRegistrar: () -> Unit,
    onOpenList: (String) -> Unit,
    onNewList: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        ProfileViewModel(userId, container.profileRepository, container.peopleRepository, container.listsRepository, container.sortProfileLogsUseCase)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ProfileAction.OnBackClicked -> onBack()
                ProfileAction.OnPersonalizarClicked -> onCustomize()
                ProfileAction.OnMessageClicked -> onOpenChat(userId)
                ProfileAction.OnRegistrarClicked -> onRegistrar()
                // una reseña abre su vista con me gusta y comentarios; un contenido suelto, su ficha
                is ProfileAction.OnResenaClicked -> onOpenReview(action.logId)
                is ProfileAction.OnMediaClicked -> onOpenMedia(action.media)
                ProfileAction.OnNuevaListaClicked -> onNewList()
                is ProfileAction.OnEditarListaClicked -> onOpenList(action.listId)
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

/**
 * perfil propio (pestaña) o de otra persona (abierto encima). es la misma pantalla.
 * arriba la cabecera (portada, foto, nombre), despues la fila de paginas (paleta, favoritos, diario,
 * pendientes y las listas) y debajo la pagina elegida. la paleta es la que se abre al entrar.
 */
@Composable
fun ProfileScreen(state: ProfileState, onAction: (ProfileAction) -> Unit, modifier: Modifier = Modifier) {
    val perfil = state.perfil ?: return
    val user = perfil.user
    val propio = perfil.propio
    val diseno = user.layout

    // cada perfil puede llevar su propio fondo; el texto se pone claro u oscuro segun cual se lea encima
    val fondo = user.background?.let { Color(it) }
    val ink = fondo?.legible() ?: Pigment.Ink
    val suave = if (fondo == null) Pigment.Graphite else ink.copy(alpha = 0.7f)
    // lo que ocupa la barra de estado del movil: la portada se mete debajo y los botones van por encima
    val arriba = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    CompositionLocalProvider(LocalContentColor provides ink) {
        Box(modifier.background(fondo ?: Pigment.Paper)) {
            BoxWithConstraints {
                // columnas del muro de manchas: dos en un movil, mas cuanto mas ancha es la ventana
                val columnas = (maxWidth / 250.dp).toInt().coerceIn(2, 7)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BarSpace)) {
                    item {
                        Cabecera(user, fondo ?: Pigment.Paper, ink, suave, arriba)
                        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (user.bio.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(user.bio, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                            }
                            if (!propio) {
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FollowButton(perfil.loSigo, { onAction(ProfileAction.OnFollowClicked) })
                                    PillButton("Mensaje", { onAction(ProfileAction.OnMessageClicked) }, filled = false, color = ink, icon = Icons.Send)
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        FilaDePaginas(state, propio, ink, onAction)
                    }

                    when (val seccion = state.seccion) {
                        Seccion.Paleta -> paleta(state, ink, suave, onAction)
                        Seccion.Favoritos -> {
                            val logs = state.favoritos
                            when {
                                logs.isEmpty() -> item { SinRegistros(propio, ink, onAction) }
                                diseno.favoritesBlob -> item { PaletteWall(logs, columnas, propio) { onAction(ProfileAction.OnResenaClicked(it.id)) } }
                                else -> item { Cuadricula(logs.map { it.pieza() }, blob = false, ink = ink, onClick = { p -> p.log?.let { onAction(ProfileAction.OnResenaClicked(it.id)) } }) }
                            }
                        }
                        Seccion.Diario -> item {
                            if (perfil.logs.isEmpty()) SinRegistros(propio, ink, onAction)
                            else Diario(perfil.logs, null, diseno.diaryBlob, ink, suave) { onAction(ProfileAction.OnResenaClicked(it)) }
                        }
                        Seccion.Pendientes -> item { Pendientes(state, ink, onAction) }
                        is Seccion.Lista -> listaEnPerfil(state, seccion.listId, ink, onAction)
                    }
                }
            }

            // los botones de arriba flotan sobre la portada, asi esta llega hasta el borde de la pantalla
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (!propio) BotonFlotante(Icons.Back, "Volver") { onAction(ProfileAction.OnBackClicked) }
                Spacer(Modifier.weight(1f))
                // un solo boton para todo lo de mi perfil: personalizarlo y, desde ahi, los ajustes de la cuenta
                if (propio) BotonFlotante(Icons.Brush, "Personalizar perfil y ajustes") { onAction(ProfileAction.OnPersonalizarClicked) }
            }
        }
    }
}

@Composable
private fun BotonFlotante(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Pigment.Paper.copy(alpha = 0.82f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Pigment.Ink, modifier = Modifier.size(21.dp))
    }
}

/**
 * portada, foto, nombre y usuario. la portada empieza arriba del todo (debajo de la barra de estado)
 * y se funde con el fondo del perfil con un degradado, para que la foto quede encima sin un corte brusco.
 */
@Composable
private fun Cabecera(user: User, fondo: Color, ink: Color, suave: Color, arriba: androidx.compose.ui.unit.Dp) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val banner = user.bannerUrl
        if (banner != null) {
            Box(Modifier.fillMaxWidth().height(arriba + 214.dp)) {
                AsyncImage(banner, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(arriba + 178.dp))
                Box(Modifier.fillMaxWidth().height(arriba + 178.dp).background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to fondo)))
                Avatar(user, 92.dp, Modifier.align(Alignment.BottomCenter))
            }
        } else {
            // sin portada se deja el hueco de los botones de arriba
            Spacer(Modifier.height(arriba + 54.dp))
            Avatar(user, 92.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(user.name, style = titular(27.sp, ink), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp))
        Text("@${user.username}", color = suave, fontSize = 14.sp)
    }
}

/**
 * las paginas del perfil, en una fila: paleta, favoritos, diario, pendientes, las listas y el + para crear otra.
 * de otra persona solo salen las que deja ver; en el mio, las que no se ven llevan un candado.
 */
@Composable
private fun FilaDePaginas(state: ProfileState, propio: Boolean, ink: Color, onAction: (ProfileAction) -> Unit) {
    val actual = state.seccion
    val diseno = state.perfil?.user?.layout ?: return
    fun ir(s: Seccion) = onAction(ProfileAction.OnSeccionChanged(s))
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(if (propio) "Mi paleta" else "Paleta", actual == Seccion.Paleta, ink, { ir(Seccion.Paleta) })
        if (state.verFavoritos) {
            Chip("Favoritos", actual == Seccion.Favoritos, ink, { ir(Seccion.Favoritos) }, icon = Icons.Lock.takeIf { !diseno.favoritesVisible })
        }
        if (state.verDiario) {
            Chip("Diario", actual == Seccion.Diario, ink, { ir(Seccion.Diario) }, icon = Icons.Lock.takeIf { !diseno.diaryVisible })
        }
        if (state.verPendientes) {
            Chip("Pendientes", actual == Seccion.Pendientes, ink, { ir(Seccion.Pendientes) }, icon = Icons.Lock.takeIf { propio && state.perfil?.user?.watchlistPublic == false })
        }
        state.listas.forEach { vista ->
            val lista = vista.lista
            Chip(
                lista.name, actual == Seccion.Lista(lista.id), ink, { ir(Seccion.Lista(lista.id)) },
                icon = if (propio && !lista.visible) Icons.Lock else if (lista.kind == ListKind.REVIEWS) Icons.Star else Icons.ListBullets,
            )
        }
        if (propio) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).border(1.dp, Pigment.Line, CircleShape).clickable { onAction(ProfileAction.OnNuevaListaClicked) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Plus, contentDescription = "Lista nueva", tint = ink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** la paleta: los bloques que ha elegido, en su orden (o los de por defecto si nunca la ha personalizado). */
private fun LazyListScope.paleta(state: ProfileState, ink: Color, suave: Color, onAction: (ProfileAction) -> Unit) {
    val perfil = state.perfil ?: return
    val diseno = perfil.user.layout
    val bloques = diseno.bloques.filter { !it.hidden }
    when {
        // alguien nuevo, sin nada registrado ni nada personalizado: se enseña por donde empezar
        perfil.logs.isEmpty() && !diseno.custom && diseno.widgets.isEmpty() -> item { SinRegistros(perfil.propio, ink, onAction) }
        bloques.isEmpty() -> item {
            if (perfil.propio) {
                EmptyState("Tu paleta está vacía", "Añade bloques de favoritos, diario, listas o texto para que tu perfil hable de ti.", Pigment.Ochre, semilla = 43, ink = ink) {
                    PillButton("Personalizar", { onAction(ProfileAction.OnPersonalizarClicked) }, color = ink, icon = Icons.Brush)
                }
            } else {
                EmptyState("Paleta en blanco", "Todavía no ha puesto nada aquí.", Pigment.Ochre, semilla = 43, ink = ink)
            }
        }
        else -> items(bloques, key = { "w_${it.id}" }) { bloque -> BloqueView(bloque, state, ink, suave, onAction) }
    }
}

@Composable
private fun SinRegistros(propio: Boolean, ink: Color, onAction: (ProfileAction) -> Unit) {
    EmptyState(
        title = if (propio) "Tu paleta está en blanco" else "Todavía no ha registrado nada",
        cuerpo = if (propio) "Cada cosa que puntúes será una mancha de color aquí, de tu favorita a la que menos." else "Cuando puntúe algo aparecerá aquí.",
        color = Pigment.Viridian, semilla = 40, ink = ink,
    ) {
        if (propio) PillButton("Registrar lo primero", { onAction(ProfileAction.OnRegistrarClicked) }, color = ink, icon = Icons.Plus)
    }
}

@Composable
private fun Pendientes(state: ProfileState, ink: Color, onAction: (ProfileAction) -> Unit) {
    val perfil = state.perfil ?: return
    val pendientes = perfil.pendientes
    when {
        pendientes == null -> EmptyState("Pendientes privados", "${perfil.user.name.substringBefore(' ')} prefiere no enseñar su lista.", Pigment.Graphite, semilla = 41, ink = ink)
        pendientes.isEmpty() -> EmptyState(
            title = "Nada pendiente",
            cuerpo = if (perfil.propio) "Guarda aquí lo que quieras ver, jugar, leer o escuchar más adelante con el marcador de cada ficha." else "No tiene nada guardado ahora mismo.",
            color = Pigment.Viridian, semilla = 42, ink = ink,
        )
        else -> Cuadricula(pendientes.map { Pieza(it) }, perfil.user.layout.watchlistBlob, ink, { onAction(ProfileAction.OnMediaClicked(it.media)) })
    }
}

/** una lista como pagina del perfil. */
private fun LazyListScope.listaEnPerfil(state: ProfileState, listId: String, ink: Color, onAction: (ProfileAction) -> Unit) {
    val vista = state.lista(listId) ?: return
    val lista = vista.lista
    val propio = vista.propia
    val deResenas = lista.kind == ListKind.REVIEWS
    val blob = lista.id in (state.perfil?.user?.layout?.blobLists ?: emptyList())
    if (propio) item {
        Row(Modifier.padding(start = 20.dp, bottom = 12.dp)) {
            PillButton(if (lista.size == 0) "Añadir" else "Editar lista", { onAction(ProfileAction.OnEditarListaClicked(lista.id)) }, filled = false, color = ink, icon = if (lista.size == 0) Icons.Plus else Icons.Pencil)
        }
    }
    val piezas = if (deResenas) vista.resenas.map { it.pieza() } else lista.media.map { Pieza(it) }
    item {
        if (piezas.isEmpty()) {
            EmptyState("Lista vacía", if (propio) "Entra en la lista para añadirle cosas." else "Todavía no ha añadido nada.", Pigment.Ochre, semilla = 71, ink = ink)
        } else {
            Cuadricula(piezas, blob, ink, { p ->
                onAction(p.log?.let { ProfileAction.OnResenaClicked(it.id) } ?: ProfileAction.OnMediaClicked(p.media))
            })
        }
    }
}

/**
 * la paleta: un muro de manchas a varias columnas. cada log va a la columna que lleve menos altura
 * (asi quedan parejas) y dentro de ella se coloca un poco a su aire: mas ancho o mas estrecho,
 * arrimado a un lado y algo girado. ese desorden sale del id del contenido, asi que no cambia al volver a abrir.
 */
@Composable
private fun PaletteWall(logs: List<LogEntry>, columnas: Int, propio: Boolean, onClick: (LogEntry) -> Unit) {
    val montones = List(columnas) { mutableListOf<LogEntry>() }
    val alturas = FloatArray(columnas)
    logs.forEach { log ->
        val masCorta = alturas.indices.minBy { alturas[it] }
        montones[masCorta] += log
        // alto que ocupa con ancho 1: la portada (1 / proporcion) mas el hueco del titulo
        alturas[masCorta] += 1f / log.media.type.proporcion + 0.3f
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        montones.forEachIndexed { columna, monton ->
            // las columnas impares empiezan mas abajo para que no quede una rejilla
            Column(Modifier.weight(1f).padding(top = if (columna % 2 == 1) 36.dp else 0.dp)) {
                monton.forEach { log -> WallItem(log, propio) { onClick(log) } }
            }
        }
    }
}

@Composable
private fun WallItem(log: LogEntry, propio: Boolean, onClick: () -> Unit) {
    val semilla = log.media.id.hashCode()
    // ancho (90 a 100 % de la columna), lado al que se arrima y giro (de -3 a 3 grados)
    val (width, lado, inclinacion) = remember(semilla) {
        val azar = Random(semilla)
        Triple(0.9f + azar.nextFloat() * 0.1f, azar.nextInt(3), azar.nextFloat() * 6f - 3f)
    }
    val alignment = when (lado) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(bottom = 18.dp).alpha(if (log.hidden) 0.4f else 1f),
        horizontalAlignment = alignment,
    ) {
        Box(Modifier.fillMaxWidth(width).graphicsLayer { rotationZ = inclinacion }) {
            PaintBlob(log.media, semilla, Modifier.fillMaxWidth())
            if (propio && (log.pinned || log.hidden)) {
                Badge(if (log.pinned) Icons.Pin else Icons.EyeOff, Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(log.media.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Stars(log.stars, size = 12.dp)
    }
}

@Composable
private fun Badge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.size(26.dp).clip(CircleShape).background(Pigment.Paper), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Pigment.Ink, modifier = Modifier.size(14.dp))
    }
}

