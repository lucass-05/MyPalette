package com.example.palette.features.tutorial.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.User
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.auth.ui.Splats
import com.example.palette.features.catalog.presentation.DiscoverState
import com.example.palette.features.catalog.presentation.LogState
import com.example.palette.features.catalog.ui.DiscoverScreen
import com.example.palette.features.catalog.ui.LogScreen
import com.example.palette.features.home.presentation.HomeState
import com.example.palette.features.home.ui.HomeScreen
import com.example.palette.features.messages.presentation.ActivityState
import com.example.palette.features.messages.ui.ActivityScreen
import com.example.palette.features.profile.domain.model.Perfil
import com.example.palette.features.profile.presentation.CustomizeState
import com.example.palette.features.profile.presentation.ProfileState
import com.example.palette.features.profile.presentation.Seccion
import com.example.palette.features.profile.ui.Cuadricula
import com.example.palette.features.profile.ui.CustomizeScreen
import com.example.palette.features.profile.ui.ProfileScreen
import com.example.palette.features.profile.ui.pieza
import com.example.palette.features.reviews.presentation.ReviewDetailState
import com.example.palette.features.reviews.ui.ReviewDetailScreen
import com.example.palette.features.tutorial.domain.model.Ejemplos
import com.example.palette.features.tutorial.presentation.TutorialAction
import com.example.palette.features.tutorial.presentation.TutorialState
import com.example.palette.features.tutorial.presentation.TutorialViewModel
import com.example.palette.navigation.LocalWide
import kotlinx.coroutines.launch

@Composable
fun TutorialScreenRoot(onPersonalizar: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { TutorialViewModel(container.tutorialRepository, container.prepararEjemplosUseCase) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    TutorialScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            if (action == TutorialAction.OnPersonalizarClicked) onPersonalizar()
        },
    )
}

/** un paso del tutorial: lo que se cuenta y lo que se enseña debajo. */
private class Paso(val titulo: String, val texto: String, val color: Color, val vista: @Composable () -> Unit)

/**
 * el tutorial: unos pasos que se pasan deslizando o con los botones. cada uno enseña la pantalla
 * de verdad en pequeño, pintada con los datos de [TutorialState.ejemplos], sin poder tocarla.
 */
@Composable
fun TutorialScreen(state: TutorialState, onAction: (TutorialAction) -> Unit, modifier: Modifier = Modifier, pasoInicial: Int = 0) {
    val pasos = pasos(state.ejemplos)
    // pasoInicial solo lo usan las capturas, para ver un paso concreto
    val pager = rememberPagerState(initialPage = pasoInicial) { pasos.size }
    val ambito = rememberCoroutineScope()
    val ultimo = pager.currentPage == pasos.lastIndex
    val ancha = LocalWide.current

    fun irA(paso: Int) {
        ambito.launch { pager.animateScrollToPage(paso) }
    }

    // tapa la app entera: lo que hay debajo no recibe toques mientras se ve el tutorial
    Box(
        modifier.fillMaxSize().background(Pigment.Paper).pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 1040.dp).fillMaxSize().safeDrawingPadding()) {
            // arriba: por donde se va y el boton de saltar
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Progreso(pasos.size, pager.currentPage, pasos[pager.currentPage].color, Modifier.weight(1f))
                Text(
                    if (ultimo) "" else "Saltar",
                    color = Pigment.Graphite, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(enabled = !ultimo) { onAction(TutorialAction.OnSaltarClicked) }
                        .padding(horizontal = 12.dp, vertical = 10.dp).widthIn(min = 44.dp),
                )
            }

            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { pagina ->
                PasoView(pasos[pagina], ancha)
            }

            // abajo: atras y siguiente; en el ultimo paso, las dos formas de terminar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ultimo) {
                    PillButton("Personalizar mi perfil", { onAction(TutorialAction.OnPersonalizarClicked) }, Modifier.weight(1f), filled = false, icon = Icons.Brush)
                    PillButton("Empezar", { onAction(TutorialAction.OnEmpezarClicked) }, Modifier.weight(1f), color = Pigment.Accent)
                } else {
                    if (pager.currentPage > 0) PillButton("Atrás", { irA(pager.currentPage - 1) }, filled = false)
                    Spacer(Modifier.weight(1f))
                    PillButton(if (pager.currentPage == 0) "Empezar el tutorial" else "Siguiente", { irA(pager.currentPage + 1) }, color = Pigment.Accent)
                }
            }
        }
    }
}

/** una rayita por paso; las ya vistas, del color del paso en el que se esta. */
@Composable
private fun Progreso(total: Int, actual: Int, color: Color, modifier: Modifier = Modifier) {
    val tinta by animateColorAsState(color)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(if (i <= actual) tinta else Pigment.Line))
        }
    }
}

/** en el movil el texto va arriba y la vista debajo; en pantallas anchas, uno al lado del otro. */
@Composable
private fun PasoView(paso: Paso, ancha: Boolean) {
    if (ancha) {
        Row(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Textos(paso, Modifier.weight(1f).padding(end = 40.dp))
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) { paso.vista() }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Textos(paso, Modifier.padding(top = 18.dp, bottom = 16.dp))
            Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) { paso.vista() }
        }
    }
}

@Composable
private fun Textos(paso: Paso, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(paso.titulo, style = titular(26.sp))
        Spacer(Modifier.height(8.dp))
        Text(paso.texto, color = Pigment.Graphite, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

/**
 * una pantalla de la app en pequeño, como si fuera un movil. se pinta a tamaño de movil (390 x 844)
 * y se encoge entera para que quepa. no se puede tocar: la capa de encima se queda con los toques
 * (pero deja pasar el deslizar del tutorial, que lo recoge el pager de fuera).
 */
@Composable
private fun MiniMovil(contenido: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val escala = minOf(maxWidth / ANCHO_MOVIL, maxHeight / ALTO_MOVIL, 0.8f)
        val forma = RoundedCornerShape(26.dp * escala / 0.6f)
        Box(
            Modifier.size(ANCHO_MOVIL * escala, ALTO_MOVIL * escala).clip(forma).border(1.dp, Pigment.Line, forma),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.requiredSize(ANCHO_MOVIL, ALTO_MOVIL).graphicsLayer { scaleX = escala; scaleY = escala }.background(Pigment.Paper),
            ) {
                // dentro siempre se ve la version de movil, aunque el tutorial se abra en el ordenador
                CompositionLocalProvider(LocalWide provides false) { contenido() }
            }
            Box(Modifier.matchParentSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
        }
    }
}

/** para las vistas que no son una pantalla entera: una tarjeta con el contenido centrado. */
@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    val forma = RoundedCornerShape(22.dp)
    Box(
        Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(forma).border(1.dp, Pigment.Line, forma).padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        contenido()
        Box(Modifier.matchParentSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
    }
}

private val ANCHO_MOVIL = 390.dp
private val ALTO_MOVIL = 844.dp

// ---- los pasos

@Composable
private fun pasos(e: Ejemplos): List<Paso> {
    val conBloques = e.yo.copy(layout = e.layout)

    // mi perfil con los ejemplos, abierto en la pagina que toque
    @Composable
    fun perfil(seccion: Seccion, user: User = conBloques) {
        MiniMovil {
            ProfileScreen(ProfileState(Perfil(user, propio = true, logs = e.misLogs, pendientes = e.pendientes, loSigo = false), e.favoritos, e.listas, seccion), {})
        }
    }

    return listOf(
        Paso(
            "Te damos la bienvenida a Palette",
            "En unos pasos te enseñamos todo lo que puedes hacer y cómo dejar tu perfil a tu gusto. Lo que sale en los ejemplos no se guarda. Puedes saltarlo cuando quieras.",
            Pigment.Accent,
        ) { Bienvenida() },
        Paso(
            "Registra lo que te gusta",
            "En Registrar buscas pelis, series, juegos, discos, canciones y libros. Filtra por tipo y toca uno para darle estrellas y escribir tu reseña.",
            Pigment.Cadmium,
        ) { MiniMovil { LogScreen(LogState(consulta = "Frank", resultados = e.busqueda), {}) } },
        Paso(
            "Cada reseña tiene su página",
            "Ahí están los me gusta y los comentarios. Con el avión de papel se la mandas a un amigo y, si es tuya, desde los tres puntos la fijas, la ocultas o la editas.",
            Pigment.Magenta,
        ) { MiniMovil { ReviewDetailScreen(ReviewDetailState(e.resena, cargada = true), {}) } },
        Paso(
            "Inicio: lo de tus amigos",
            "Arriba, sus historias de los últimos días. Debajo, sus reseñas: dale a me gusta, comenta, guárdalas en pendientes o ábrelas enteras.",
            Pigment.Cobalt,
        ) { MiniMovil { HomeScreen(HomeState(e.feed), {}) } },
        Paso(
            "Descubrir aprende de ti",
            "Las recomendaciones salen de lo que mejor puntúas. Toca cualquier portada para ver su ficha y en qué plataformas está.",
            Pigment.Viridian,
        ) { MiniMovil { DiscoverScreen(DiscoverState(e.filas, cargando = false, hayFavoritos = true), {}) } },
        Paso(
            "Mensajes y actividad",
            "Escribe a tus amigos o crea grupos. En Actividad te avisamos de seguidores nuevos, me gusta, comentarios y amigos que han registrado lo mismo que tú.",
            Pigment.Ochre,
        ) { MiniMovil { ActivityScreen(ActivityState(seccion = 1, avisos = e.avisos), {}) } },
        Paso(
            "Tu perfil es tu paleta",
            "Arriba van tu portada, tu foto y tu bio. Debajo, las páginas: Paleta, Favoritos, Diario, Pendientes y las listas que crees.",
            Pigment.Accent,
        ) { perfil(Seccion.Paleta) },
        Paso(
            "La Paleta se monta con bloques",
            "Un texto, tus favoritos, tu diario o una lista. Tú eliges cuáles poner, en qué orden y cuántas cosas enseña cada uno. Todos son opcionales.",
            Pigment.Violet,
        ) { Tarjeta { Bloques(e) } },
        Paso(
            "Favoritos",
            "Todo lo que registras, ordenado por nota. Lo que fijes sale siempre lo primero.",
            Pigment.Star,
        ) { perfil(Seccion.Favoritos) },
        Paso(
            "Diario",
            "Lo que has ido registrando, día a día. Toca una portada para abrir su reseña.",
            Pigment.Cobalt,
        ) { perfil(Seccion.Diario) },
        Paso(
            "Pendientes",
            "Lo que quieres ver, jugar, leer o escuchar más adelante. Se añade con el marcador de cualquier ficha o reseña.",
            Pigment.Viridian,
        ) { perfil(Seccion.Pendientes) },
        Paso(
            "Listas con el +",
            "Junto a las páginas hay un +: crea listas de contenidos o de reseñas, como «Para el verano». Cada una es una página más de tu perfil.",
            Pigment.Cadmium,
        ) { perfil(Seccion.Lista(e.listas.first().lista.id)) },
        Paso(
            "Manchas o recuadros",
            "Cada página y cada bloque se pinta como prefieras: con manchas de pintura o con recuadros de esquinas redondeadas.",
            Pigment.Magenta,
        ) { Tarjeta { ManchasORecuadros(e) } },
        Paso(
            "Todo se cambia desde el pincel",
            "Está arriba en tu perfil. Ahí cambias la portada, el fondo, las páginas y los bloques. En Perfil y cuenta, tu foto, tu nombre y los colores de la app.",
            Pigment.Ochre,
        ) {
            MiniMovil {
                CustomizeScreen(CustomizeState(conBloques, e.layout, conBloques.background, conBloques.watchlistPublic, e.listas), {}, {})
            }
        },
        Paso(
            "Tú decides qué se ve",
            "Las páginas con candado solo las ves tú. Quien entre en tu perfil ve el resto tal y como lo dejes.",
            Pigment.Graphite,
        ) { perfil(Seccion.Paleta, conBloques.copy(layout = e.layout.copy(diaryVisible = false), watchlistPublic = false)) },
        Paso(
            "Ya lo tienes",
            "Los ejemplos se quedan aquí: tu perfil sigue como estaba. Si quieres repasarlo, está en el pincel › Perfil y cuenta › Ver el tutorial otra vez.",
            Pigment.Accent,
        ) { Final() },
    )
}

// ---- vistas que no son pantallas de la app

@Composable
private fun Bienvenida() {
    Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Splats(Pigment.Accent)
        Spacer(Modifier.height(18.dp))
        Text("Palette", style = titular(46.sp))
        Spacer(Modifier.height(6.dp))
        Text("Lo que ves, juegas, lees y escuchas, convertido en tu paleta.", color = Pigment.Graphite, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Final() {
    Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Splats(Pigment.Viridian)
    }
}

/** el editor de bloques en pequeño: cada bloque con su icono, cuantas cosas enseña y las flechas para moverlo. */
@Composable
private fun Bloques(e: Ejemplos) {
    val lista = e.listas.first().lista.name
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FilaDeBloque(Icons.Type, "Texto", "Sobre mí", Pigment.Violet)
        FilaDeBloque(Icons.Star, "Favoritos", "5 · con manchas", Pigment.Star)
        FilaDeBloque(Icons.Calendar, "Diario", "Últimos 3 días", Pigment.Cobalt)
        FilaDeBloque(Icons.ListBullets, "Lista", "$lista · 5", Pigment.Cadmium)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Pigment.Line, RoundedCornerShape(14.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Plus, contentDescription = null, tint = Pigment.Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Añadir bloque", color = Pigment.Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FilaDeBloque(icono: ImageVector, nombre: String, detalle: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Pigment.Line).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(detalle, color = Pigment.Graphite, fontSize = 13.sp)
        }
        Icon(Icons.ArrowUp, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.ArrowDown, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Eye, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(18.dp))
    }
}

/** los mismos favoritos pintados de las dos formas, uno encima del otro. */
@Composable
private fun ManchasORecuadros(e: Ejemplos) {
    val piezas = e.favoritos.take(3).map { it.pieza() }
    Column(Modifier.fillMaxWidth()) {
        Text("Manchas", style = titular(18.sp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Cuadricula(piezas, blob = true, ink = Pigment.Ink, onClick = {})
        Spacer(Modifier.height(18.dp))
        Text("Recuadros", style = titular(18.sp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Cuadricula(piezas, blob = false, ink = Pigment.Ink, onClick = {})
    }
}
