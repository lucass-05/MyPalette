package com.example.palette.data.remote

import com.example.palette.ApiKeys
import com.example.palette.data.Extras
import com.example.palette.data.Media
import com.example.palette.data.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** punto unico de entrada a las bases de datos externas. */
class Catalog {
    private val tmdb = Tmdb(ApiKeys.TMDB)
    private val igdb = Igdb()
    private val itunes = Itunes()
    private val openLibrary = OpenLibrary()

    /** tipos que ahora mismo no se pueden buscar porque falta su clave en local.properties. */
    val unavailable: Set<MediaType>
        get() = buildSet {
            if (!tmdb.ready) { add(MediaType.MOVIE); add(MediaType.SERIES) }
        }

    /** busca en todas las fuentes a la vez. con [types] vacio busca en todo. */
    suspend fun search(query: String, types: Set<MediaType> = emptySet()): List<Media> = coroutineScope {
        val wanted = (types.ifEmpty { MediaType.entries.toSet() }) - unavailable
        val movies = MediaType.MOVIE in wanted
        val series = MediaType.SERIES in wanted
        val jobs = buildList {
            if (movies || series) add(async { safe { tmdb.search(query, movies, series) } })
            if (MediaType.GAME in wanted) add(async { safe { igdb.search(query) } })
            if (MediaType.ALBUM in wanted) add(async { safe { itunes.search(query, MediaType.ALBUM) } })
            if (MediaType.SONG in wanted) add(async { safe { itunes.search(query, MediaType.SONG) } })
            if (MediaType.BOOK in wanted) add(async { safe { openLibrary.search(query) } })
        }
        interleave(jobs.awaitAll())
    }

    suspend fun details(media: Media): Media = safeOr(media) {
        val full = when {
            media.id.startsWith("tmdb:") && tmdb.ready -> tmdb.details(media)
            media.id.startsWith("igdb:") -> igdb.details(media)
            media.id.startsWith("itunes:") -> itunes.details(media)
            media.id.startsWith("ol:") -> openLibrary.details(media)
            else -> media
        }
        // lo que ya teniamos manda si la ficha completa viene sin ese dato
        full.copy(
            imageUrl = full.imageUrl ?: media.imageUrl,
            overview = full.overview ?: media.overview,
            creator = full.creator ?: media.creator,
        )
    }

    /** lo que solo tiene sentido para ese tipo de contenido: reparto, canciones del disco, plataformas... */
    suspend fun extras(media: Media): Extras = safeOr(Extras()) {
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

    suspend fun similar(media: Media): List<Media> = safe {
        when {
            media.id.startsWith("tmdb:") && tmdb.ready -> tmdb.similar(media)
            media.id.startsWith("igdb:") -> igdb.similar(media)
            media.id.startsWith("itunes:") -> itunes.similar(media)
            media.id.startsWith("ol:") -> openLibrary.similar(media)
            else -> emptyList()
        }
    }

    suspend fun trending(): List<Media> = safe { if (tmdb.ready) tmdb.trending() else emptyList() }

    // cada fuente ya viene ordenada por relevancia: se van cogiendo de una en una para que ninguna tape a las demas
    private fun interleave(lists: List<List<Media>>): List<Media> {
        val out = mutableListOf<Media>()
        val longest = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until longest) lists.forEach { l -> l.getOrNull(i)?.let(out::add) }
        return out.distinctBy { it.id }
    }

    private suspend fun safe(block: suspend () -> List<Media>): List<Media> = safeOr(emptyList()) { block().distinctBy { it.id } }

    private suspend fun <T> safeOr(fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Palette: ${e.message}")
            fallback
        }
}
