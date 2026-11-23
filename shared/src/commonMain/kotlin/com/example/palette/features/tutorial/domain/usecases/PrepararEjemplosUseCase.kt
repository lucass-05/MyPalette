package com.example.palette.features.tutorial.domain.usecases

import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaList
import com.example.palette.core.model.MediaType
import com.example.palette.core.model.NoticeKind
import com.example.palette.core.model.ProfileLayout
import com.example.palette.core.model.ProfileWidget
import com.example.palette.core.model.User
import com.example.palette.core.model.WidgetKind
import com.example.palette.core.util.DAY
import com.example.palette.features.catalog.domain.model.FilaRecomendada
import com.example.palette.features.home.domain.model.Feed
import com.example.palette.features.home.domain.model.Historia
import com.example.palette.features.home.domain.model.Publicacion
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.messages.domain.model.Aviso
import com.example.palette.features.profile.domain.usecases.SortProfileLogsUseCase
import com.example.palette.features.reviews.domain.model.Comentario
import com.example.palette.features.reviews.domain.model.Resena
import com.example.palette.features.tutorial.domain.model.Ejemplos

/**
 * prepara lo que enseña el tutorial a partir de mi perfil de verdad.
 *
 * si tengo pocos logs o pocos pendientes se rellenan con contenidos inventados, para que un perfil
 * recien creado tambien vea como queda todo. nada de esto se guarda: solo se pinta.
 */
class PrepararEjemplosUseCase(private val ordenarFavoritos: SortProfileLogsUseCase) {

    operator fun invoke(yo: User, misLogs: List<LogEntry>, misPendientes: List<Media>, ahora: Long): Ejemplos {
        // con menos de 6 logs el perfil se ve muy vacio: se completan hasta 8 con inventados
        val logs = if (misLogs.size >= MINIMO_LOGS) misLogs else misLogs + inventados(ahora).take(8 - misLogs.size)
        val favoritos = ordenarFavoritos(logs)
        val pendientes = (misPendientes + INVENTADOS.reversed()).distinctBy { it.id }.take(maxOf(misPendientes.size, 6))

        // la reseña de ejemplo es una mia que tenga texto; si no hay ninguna, una inventada
        val conTexto = favoritos.firstOrNull { it.review.isNotBlank() }
            ?: LogEntry("tut_r", ME, INVENTADOS[0], 4.5f, "Me dejó pensando varios días. Volvería a verla mañana.", ahora - DAY)
        val resena = Resena(
            log = conTexto, autor = yo, propia = true, meGustas = 3, meGusta = false,
            comentarios = listOf(
                Comentario("tut_c1", ANA, "Totalmente de acuerdo, me pasó lo mismo.", ahora - 3_600_000L, puedoBorrar = true),
                Comentario("tut_c2", MARTA, "Me la apunto para el finde.", ahora - 600_000L, puedoBorrar = true),
            ),
        )

        // lo que publican los amigos inventados
        fun deAmigo(user: User, i: Int, review: String = "") =
            LogEntry("tut_a$i", user.id, INVENTADOS[i % INVENTADOS.size], listOf(5f, 4f, 4.5f, 3.5f)[i % 4], review, ahora - i * 5_400_000L)
        val feed = Feed(
            publicaciones = listOf(
                Publicacion(ANA, deAmigo(ANA, 1, "La mejor serie que he visto este año."), meGusta = true, enPendientes = false, meGustas = 4, comentarios = 2),
                Publicacion(LEO, deAmigo(LEO, 2, "Cuarenta horas y quiero más."), meGusta = false, enPendientes = true, meGustas = 2, comentarios = 1),
                Publicacion(MARTA, deAmigo(MARTA, 3), meGusta = false, enPendientes = false),
            ),
            historias = listOf(
                Historia(ANA, listOf(deAmigo(ANA, 1)), sinVer = true),
                Historia(LEO, listOf(deAmigo(LEO, 2)), sinVer = true),
                Historia(MARTA, listOf(deAmigo(MARTA, 3)), sinVer = false),
            ),
            sigoAAlguien = true,
        )

        val avisos = listOf(
            Aviso("tut_v1", NoticeKind.COMMENT, MARTA, ahora - 600_000L, leido = false, loSigo = true, log = resena.log, comentario = "Me la apunto para el finde."),
            Aviso("tut_v2", NoticeKind.LIKE, ANA, ahora - 3_600_000L, leido = false, loSigo = true, log = resena.log),
            Aviso("tut_v3", NoticeKind.SAME_MEDIA, LEO, ahora - 2 * DAY, leido = true, loSigo = true, log = deAmigo(LEO, 2)),
            Aviso("tut_v4", NoticeKind.FOLLOW, MARTA, ahora - 3 * DAY, leido = true, loSigo = false),
        )

        val listas = listOf(
            ListaVista(MediaList(LISTA_DE_EJEMPLO, ME, "Para el verano", ListKind.MEDIA, media = INVENTADOS.drop(2).take(5)), propia = true),
            ListaVista(MediaList("tut_ls2", ME, "Lo mejor del año", ListKind.REVIEWS, logIds = favoritos.take(3).map { it.id }), propia = true, resenas = favoritos.take(3)),
        )

        val layout = ProfileLayout(
            custom = true,
            widgets = listOf(
                ProfileWidget("tut_b1", WidgetKind.TEXT, "Sobre mí", yo.bio.ifBlank { "Pelis lentas, juegos raros y discos enteros." }),
                ProfileWidget("tut_b2", WidgetKind.FAVORITES, count = 5, blob = true),
                ProfileWidget("tut_b3", WidgetKind.DIARY, count = 3),
                ProfileWidget("tut_b4", WidgetKind.LIST, count = 5, listId = LISTA_DE_EJEMPLO),
            ),
        )

        val origen = favoritos.first().media
        val filas = listOf(
            FilaRecomendada("Porque te gustó ${origen.title}", origen, INVENTADOS.shuffledBy(1)),
            FilaRecomendada("Lo que más se registra", null, INVENTADOS.shuffledBy(2)),
        )

        return Ejemplos(
            yo = yo,
            busqueda = INVENTADOS.take(6),
            misLogs = logs,
            favoritos = favoritos,
            pendientes = pendientes,
            listas = listas,
            layout = layout,
            feed = feed,
            resena = resena,
            avisos = avisos,
            filas = filas,
        )
    }

    // logs inventados, repartidos en varios dias para que el diario tenga fechas distintas
    private fun inventados(ahora: Long) = INVENTADOS.mapIndexed { i, m ->
        LogEntry(
            "tut_l$i", ME, m,
            stars = listOf(5f, 4.5f, 4f, 5f, 3.5f, 4.5f, 3f, 4f)[i],
            review = if (i % 3 == 0) "De lo mejor que he visto en mucho tiempo." else "",
            createdAt = ahora - (i / 2) * DAY - i * 3_600_000L,
        )
    }

    // el mismo orden siempre (nada de aleatorio): asi el tutorial se ve igual cada vez
    private fun List<Media>.shuffledBy(salto: Int) = indices.map { this[(it * 3 + salto) % size] }

    private companion object {
        const val MINIMO_LOGS = 6
        const val LISTA_DE_EJEMPLO = "tut_ls1"

        val ANA = User("tut_u1", "ana", "Ana García", favorite = 0xFFE8418F)
        val MARTA = User("tut_u2", "marta", "Marta Ruiz", favorite = 0xFF12A679)
        val LEO = User("tut_u3", "leo", "Leo Martín", favorite = 0xFFF2A33A)

        // sin portada: sale el color de su tipo, que ya queda bien
        val INVENTADOS = listOf(
            Media("tut:1", MediaType.MOVIE, "Dune: parte dos", "Denis Villeneuve", 2024),
            Media("tut:2", MediaType.SERIES, "Severance", "Dan Erickson", 2022),
            Media("tut:3", MediaType.GAME, "Hollow Knight", "Team Cherry", 2017),
            Media("tut:4", MediaType.ALBUM, "Blonde", "Frank Ocean", 2016),
            Media("tut:5", MediaType.BOOK, "Cien años de soledad", "Gabriel García Márquez", 1967),
            Media("tut:6", MediaType.SONG, "Nights", "Frank Ocean", 2016),
            Media("tut:7", MediaType.MOVIE, "Perfect Days", "Wim Wenders", 2023),
            Media("tut:8", MediaType.GAME, "Outer Wilds", "Mobius Digital", 2019),
        )
    }
}
