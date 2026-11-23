package com.example.palette.capturas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.palette.core.model.Comment
import com.example.palette.core.model.Extras
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaList
import com.example.palette.core.model.MediaType
import com.example.palette.core.model.ModoDeVer
import com.example.palette.core.model.NoticeKind
import com.example.palette.core.model.Plataforma
import com.example.palette.core.model.ProfileLayout
import com.example.palette.core.model.ProfileWidget
import com.example.palette.core.model.User
import com.example.palette.core.model.WidgetKind
import com.example.palette.core.ui.PaletteTheme
import com.example.palette.core.ui.Pigment
import com.example.palette.core.util.DAY
import com.example.palette.features.catalog.presentation.DetailState
import com.example.palette.features.catalog.presentation.LogState
import com.example.palette.features.catalog.ui.DetailScreen
import com.example.palette.features.catalog.ui.LogScreen
import com.example.palette.features.home.domain.model.Feed
import com.example.palette.features.home.domain.model.Publicacion
import com.example.palette.features.home.presentation.HomeState
import com.example.palette.features.home.ui.HomeScreen
import com.example.palette.features.library.domain.model.LogDeAmigo
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.lists.presentation.ListState
import com.example.palette.features.lists.ui.ListScreen
import com.example.palette.features.messages.domain.model.Aviso
import com.example.palette.features.messages.presentation.ActivityState
import com.example.palette.features.messages.ui.ActivityScreen
import com.example.palette.features.profile.domain.model.Perfil
import com.example.palette.features.profile.domain.usecases.SortProfileLogsUseCase
import com.example.palette.features.profile.presentation.CustomizeState
import com.example.palette.features.profile.presentation.ProfileState
import com.example.palette.features.profile.presentation.Seccion
import com.example.palette.features.profile.ui.CustomizeScreen
import com.example.palette.features.profile.ui.ProfileScreen
import com.example.palette.features.reviews.domain.model.Comentario
import com.example.palette.features.reviews.domain.model.Resena
import com.example.palette.features.reviews.presentation.ReviewDetailState
import com.example.palette.features.reviews.ui.ReviewDetailScreen
import com.example.palette.features.tutorial.domain.usecases.PrepararEjemplosUseCase
import com.example.palette.features.tutorial.presentation.TutorialState
import com.example.palette.features.tutorial.ui.TutorialScreen
import com.example.palette.navigation.LocalWide
import com.example.palette.navigation.RailSpace
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

// capturas de las pantallas principales con datos de ejemplo, a ancho de movil y de ordenador.
// no comprueban nada: sirven para revisar a ojo como quedan los diseños sin tener que abrir la app.
// se generan con ./gradlew :shared:jvmTest y salen en shared/build/capturas.
class CapturasTest {

    private val salida = File("build/capturas").apply { mkdirs() }

    @Test
    fun pantallas() {
        val d = Datos
        capturar("perfil_paleta", ancho = null) {
            ProfileScreen(ProfileState(d.perfil, d.favoritos, d.listas, Seccion.Paleta), {})
        }
        capturar("perfil_favoritos", ancho = null) {
            ProfileScreen(ProfileState(d.perfil, d.favoritos, d.listas, Seccion.Favoritos), {})
        }
        capturar("perfil_diario", ancho = null) {
            ProfileScreen(ProfileState(d.perfil, d.favoritos, d.listas, Seccion.Diario), {})
        }
        capturar("perfil_lista", ancho = null) {
            ProfileScreen(ProfileState(d.perfil, d.favoritos, d.listas, Seccion.Lista("ls1")), {})
        }
        capturar("inicio", ancho = null) { HomeScreen(HomeState(d.feed), {}) }
        capturar("registrar", ancho = null) { LogScreen(LogState(consulta = "dune", resultados = d.contenidos), {}) }
        capturar("ficha", ancho = 820.dp) { DetailScreen(d.ficha, {}) }
        capturar("resena", ancho = 760.dp) { ReviewDetailScreen(ReviewDetailState(d.resena, cargada = true), {}) }
        capturar("actividad", ancho = 760.dp) { ActivityScreen(ActivityState(seccion = 1, avisos = d.avisos), {}) }
        capturar("lista", ancho = 820.dp) { ListScreen(ListState(vista = d.listas.first()), {}) }
        capturar("personalizar", ancho = 760.dp) {
            CustomizeScreen(CustomizeState(d.yo, d.yo.layout.copy(widgets = d.yo.layout.bloques), null, true, d.listas), {}, {})
        }
        // el tutorial de alguien recien llegado, sin nada registrado: todo sale de los ejemplos inventados
        val tutorial = TutorialState(PrepararEjemplosUseCase(SortProfileLogsUseCase())(d.yo, emptyList(), emptyList(), d.ahora))
        for (paso in listOf(0, 1, 6, 7, 11, 12, 15)) {
            capturar("tutorial_$paso", ancho = null, conBarra = false) { TutorialScreen(tutorial, {}, pasoInicial = paso) }
        }
    }

    /** pinta [contenido] como en el movil (390 x 844) y como en el ordenador (1280 x 860, con la barra a un lado). */
    private fun capturar(nombre: String, ancho: Dp?, conBarra: Boolean = true, contenido: @Composable () -> Unit) {
        for ((sufijo, w, h, wide) in listOf(Tamano("movil", 390, 844, false), Tamano("ordenador", 1280, 860, true))) {
            val escena = ImageComposeScene(w, h, Density(1f)) {
                CompositionLocalProvider(LocalWide provides wide) {
                    PaletteTheme {
                        // como en AppNavigation: en pantalla ancha la barra va a la izquierda y algunas pantallas se centran
                        Box(Modifier.fillMaxSize().background(Pigment.Paper).padding(start = if (wide && conBarra) RailSpace else 0.dp), contentAlignment = Alignment.TopCenter) {
                            Box(if (ancho != null) Modifier.widthIn(max = ancho).fillMaxSize() else Modifier.fillMaxSize()) { contenido() }
                        }
                    }
                }
            }
            // un par de fotogramas para que se asienten las listas perezosas y las fuentes
            escena.render(0)
            val imagen = escena.render(500_000_000)
            File(salida, "${nombre}_$sufijo.jpg").writeBytes(imagen.encodeToData(EncodedImageFormat.JPEG, 60)!!.bytes)
            escena.close()
        }
    }

    private data class Tamano(val nombre: String, val w: Int, val h: Int, val wide: Boolean)
}

/** datos de ejemplo: contenidos sin portada (sale el color de su tipo), gente, reseñas y listas. */
private object Datos {
    private const val AHORA = 1_795_000_000_000L
    val ahora = AHORA

    val contenidos = listOf(
        Media("tmdb:movie:1", MediaType.MOVIE, "Dune: parte dos", "Denis Villeneuve", 2024, score = 8.2, scoreSource = "TMDB"),
        Media("tmdb:tv:2", MediaType.SERIES, "Severance", "Dan Erickson", 2022, score = 8.4, scoreSource = "TMDB"),
        Media("igdb:3", MediaType.GAME, "Hollow Knight", "Team Cherry", 2017, score = 9.1, scoreSource = "IGDB"),
        Media("itunes:album:4", MediaType.ALBUM, "Blonde", "Frank Ocean", 2016),
        Media("itunes:song:5", MediaType.SONG, "Nights", "Frank Ocean", 2016),
        Media("ol:6", MediaType.BOOK, "Cien años de soledad", "Gabriel García Márquez", 1967),
        Media("tmdb:movie:7", MediaType.MOVIE, "Perfect Days", "Wim Wenders", 2023),
        Media("igdb:8", MediaType.GAME, "Outer Wilds", "Mobius Digital", 2019),
    )

    val yo = User(ME, "lucas", "Lucas Suárez", "Pelis lentas, juegos raros y discos enteros.", favorite = 0xFF2D5BF0)
    val ana = User("u1", "ana", "Ana García", favorite = 0xFFE8418F)
    val marta = User("u2", "marta", "Marta Ruiz", favorite = 0xFF12A679)

    val misLogs = contenidos.mapIndexed { i, m ->
        LogEntry("l$i", ME, m, stars = listOf(5f, 4.5f, 5f, 4f, 3.5f, 4.5f, 3f, 4f)[i], review = if (i % 2 == 0) "Me dejó pensando varios días. Volvería a verla mañana." else "", createdAt = AHORA - (i / 2) * DAY - i * 3_600_000L)
    }
    val favoritos = misLogs.sortedWith(compareByDescending<LogEntry> { it.stars }.thenByDescending { it.createdAt })

    val perfil = Perfil(yo, propio = true, logs = misLogs, pendientes = contenidos.take(4), loSigo = false)

    val listas = listOf(
        ListaVista(MediaList("ls1", ME, "Para el verano", ListKind.MEDIA, media = contenidos.drop(2).take(5)), propia = true),
        ListaVista(MediaList("ls2", ME, "Lo mejor de 2026", ListKind.REVIEWS, logIds = misLogs.take(3).map { it.id }), propia = true, resenas = misLogs.take(3)),
    )

    val feed = Feed(
        publicaciones = listOf(
            Publicacion(ana, misLogs[0].copy(id = "a0", userId = ana.id), meGusta = true, enPendientes = false, meGustas = 4, comentarios = 2),
            Publicacion(marta, misLogs[2].copy(id = "a2", userId = marta.id), meGusta = false, enPendientes = true, meGustas = 1, comentarios = 0),
            Publicacion(ana, misLogs[3].copy(id = "a3", userId = ana.id, review = ""), meGusta = false, enPendientes = false),
        ),
        sigoAAlguien = true,
    )

    val ficha = DetailState(
        media = contenidos[0].copy(overview = "Paul Atreides se une a los Fremen mientras busca venganza contra quienes destruyeron a su familia.", genres = listOf("Ciencia ficción", "Aventura")),
        extras = Extras(
            datos = listOf("Dirección" to "Denis Villeneuve", "Reparto" to "Timothée Chalamet, Zendaya", "Duración" to "2 h 46 min"),
            plataformas = listOf(Plataforma("Max", null, ModoDeVer.SUSCRIPCION), Plataforma("Apple TV", null, ModoDeVer.ALQUILER), Plataforma("Google Play", null, ModoDeVer.COMPRA)),
        ),
        miLog = misLogs[0],
        logsDeAmigos = listOf(LogDeAmigo(ana, misLogs[0].copy(id = "a0", userId = ana.id))),
    )

    val resena = Resena(
        log = misLogs[0], autor = yo, propia = true, meGustas = 3, meGusta = false,
        comentarios = listOf(
            Comentario("c1", ana, "Totalmente de acuerdo, la banda sonora es otra cosa.", AHORA - 3_600_000L, puedoBorrar = true),
            Comentario("c2", marta, "Me la apunto para el finde.", AHORA - 600_000L, puedoBorrar = true),
        ),
    )

    val avisos = listOf(
        Aviso("c_1", NoticeKind.COMMENT, ana, AHORA - 600_000L, leido = false, loSigo = true, log = misLogs[0], comentario = "Me la apunto para el finde."),
        Aviso("l_1", NoticeKind.LIKE, marta, AHORA - 3_600_000L, leido = false, loSigo = true, log = misLogs[0]),
        Aviso("s_1", NoticeKind.SAME_MEDIA, ana, AHORA - 2 * DAY, leido = true, loSigo = true, log = misLogs[2].copy(id = "a2", userId = ana.id)),
        Aviso("f_1", NoticeKind.FOLLOW, marta, AHORA - 3 * DAY, leido = true, loSigo = false),
    )

    @Suppress("unused")
    val comentarios = listOf(Comment("c1", "l0", "u1", "hola", AHORA))

    @Suppress("unused")
    val layoutDeEjemplo = ProfileLayout(listOf(ProfileWidget("t", WidgetKind.TEXT, "Sobre mí", "Hola")))
}
