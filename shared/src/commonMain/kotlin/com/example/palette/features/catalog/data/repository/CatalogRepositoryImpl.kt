package com.example.palette.features.catalog.data.repository

import com.example.palette.ApiKeys
import com.example.palette.core.model.Extras
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.features.catalog.data.remote.Igdb
import com.example.palette.features.catalog.data.remote.Itunes
import com.example.palette.features.catalog.data.remote.OpenLibrary
import com.example.palette.features.catalog.data.remote.Tmdb
import com.example.palette.features.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

// async { } lanza una tarea sin esperar a que acabe y awaitAll() espera a todas.
// asi las cuatro bases de datos se consultan a la vez y la busqueda tarda lo que tarde la mas lenta,
// no la suma de todas.

/** punto unico de entrada a las bases de datos externas. */
class CatalogRepositoryImpl : CatalogRepository {
    private val tmdb = Tmdb(ApiKeys.TMDB)
    private val igdb = Igdb()
    private val itunes = Itunes()
    private val openLibrary = OpenLibrary()

    // contenidos que se han abierto en esta sesion, por id
    private val vistos = mutableMapOf<String, Media>()

    override fun recordar(media: Media) {
        vistos[media.id] = media
    }

    // si no esta apuntado (la app se cerro y el sistema la ha restaurado) se rehace lo minimo a partir
    // del id, que lleva el origen y el tipo; el resto lo completa detalles()
    override fun recordado(mediaId: String): Media = vistos[mediaId] ?: Media(
        id = mediaId,
        type = when {
            mediaId.startsWith("tmdb:tv") -> MediaType.SERIES
            mediaId.startsWith("tmdb:") -> MediaType.MOVIE
            mediaId.startsWith("igdb:") -> MediaType.GAME
            mediaId.startsWith("itunes:album") -> MediaType.ALBUM
            mediaId.startsWith("itunes:") -> MediaType.SONG
            else -> MediaType.BOOK
        },
        title = "",
    )

    /** tipos que ahora mismo no se pueden buscar porque falta su clave en local.properties. */
    override val noDisponibles: Set<MediaType>
        get() = buildSet {
            if (!tmdb.ready) { add(MediaType.MOVIE); add(MediaType.SERIES) }
        }

    /** busca en todas las fuentes a la vez. con [types] vacio busca en todo. */
    override suspend fun buscar(consulta: String, tipos: Set<MediaType>): List<Media> = coroutineScope {
        val pedidos = (tipos.ifEmpty { MediaType.entries.toSet() }) - noDisponibles
        val pelis = MediaType.MOVIE in pedidos
        val series = MediaType.SERIES in pedidos
        val tareas = buildList {
            if (pelis || series) add(async { seguro { tmdb.buscar(consulta, pelis, series) } })
            if (MediaType.GAME in pedidos) add(async { seguro { igdb.buscar(consulta) } })
            if (MediaType.ALBUM in pedidos) add(async { seguro { itunes.buscar(consulta, MediaType.ALBUM) } })
            if (MediaType.SONG in pedidos) add(async { seguro { itunes.buscar(consulta, MediaType.SONG) } })
            if (MediaType.BOOK in pedidos) add(async { seguro { openLibrary.buscar(consulta) } })
        }
        intercalar(tareas.awaitAll())
    }

    override suspend fun detalles(media: Media): Media = seguroO(media) {
        val completo = when {
            media.id.startsWith("tmdb:") && tmdb.ready -> tmdb.detalles(media)
            media.id.startsWith("igdb:") -> igdb.detalles(media)
            media.id.startsWith("itunes:") -> itunes.detalles(media)
            media.id.startsWith("ol:") -> openLibrary.detalles(media)
            else -> media
        }
        // lo que ya teniamos manda si la ficha completa viene sin ese dato
        completo.copy(
            imageUrl = completo.imageUrl ?: media.imageUrl,
            overview = completo.overview ?: media.overview,
            creator = completo.creator ?: media.creator,
        )
    }

    /** lo que solo tiene sentido para ese tipo de contenido: reparto, canciones del disco, plataformas... */
    override suspend fun extras(media: Media): Extras = seguroO(Extras()) {
        when {
            media.id.startsWith("tmdb:") && tmdb.ready -> tmdb.extras(media)
            media.id.startsWith("igdb:") -> igdb.extras(media)
            media.id.startsWith("itunes:") -> itunes.extras(media)
            media.id.startsWith("ol:") -> Extras(
                listOfNotNull(
                    media.creator?.let { "Autor" to it },
                    media.year?.let { "Primera edición" to it.toString() },
                    media.extra?.let { "Extensión" to it },
                    media.genres.takeIf { it.isNotEmpty() }?.let { "Temas" to it.joinToString(", ") },
                )
            )
            else -> Extras()
        }
    }

    override suspend fun similar(media: Media): List<Media> = seguro {
        when {
            media.id.startsWith("tmdb:") && tmdb.ready -> tmdb.similar(media)
            media.id.startsWith("igdb:") -> igdb.similar(media)
            media.id.startsWith("itunes:") -> itunes.similar(media)
            media.id.startsWith("ol:") -> openLibrary.similar(media)
            else -> emptyList()
        }
    }

    override suspend fun tendencias(): List<Media> = seguro { if (tmdb.ready) tmdb.tendencias() else emptyList() }

    // cada fuente ya viene ordenada por relevancia: se van cogiendo de una en una para que ninguna tape a las demas
    private fun intercalar(listas: List<List<Media>>): List<Media> {
        val salida = mutableListOf<Media>()
        val masLarga = listas.maxOfOrNull { it.size } ?: 0
        for (i in 0 until masLarga) listas.forEach { l -> l.getOrNull(i)?.let(salida::add) }
        return salida.distinctBy { it.id }
    }

    private suspend fun seguro(block: suspend () -> List<Media>): List<Media> = seguroO(emptyList()) { block().distinctBy { it.id } }

    private suspend fun <T> seguroO(porDefecto: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Palette: ${e.message}")
            porDefecto
        }
}
