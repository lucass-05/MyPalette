package com.example.palette.features.catalog.data.remote

import com.example.palette.core.model.Extras
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.core.network.anioDe
import com.example.palette.core.network.fechaCorta
import com.example.palette.core.network.json
import com.example.palette.core.network.pedirTexto
import kotlinx.serialization.Serializable

/** albumes y canciones. la busqueda de itunes es publica, no pide clave. */
internal class Itunes {

    private val raiz = "https://itunes.apple.com"

    suspend fun buscar(consulta: String, type: MediaType): List<Media> {
        val entidad = if (type == MediaType.ALBUM) "album" else "song"
        val pagina = json.decodeFromString(
            Page.serializer(),
            pedirTexto("$raiz/search", mapOf("term" to consulta, "media" to "music", "entity" to entidad, "limit" to "12", "country" to "ES")),
        )
        return pagina.results.mapNotNull { it.toMedia() }
    }

    suspend fun detalles(media: Media): Media {
        val pagina = json.decodeFromString(Page.serializer(), pedirTexto("$raiz/lookup", mapOf("id" to idDe(media), "country" to "ES")))
        return pagina.results.firstNotNullOfOrNull { it.toMedia() } ?: media
    }

    /** mas discos del mismo artista. */
    suspend fun similar(media: Media): List<Media> {
        val propio = json.decodeFromString(Page.serializer(), pedirTexto("$raiz/lookup", mapOf("id" to idDe(media), "country" to "ES")))
        val artista = propio.results.firstOrNull()?.artistId ?: return emptyList()
        val pagina = json.decodeFromString(
            Page.serializer(),
            pedirTexto("$raiz/lookup", mapOf("id" to artista.toString(), "entity" to "album", "limit" to "14", "country" to "ES")),
        )
        val current = propio.results.firstOrNull()?.collectionId
        return pagina.results.filter { it.wrapperType == "collection" && it.collectionId != current }.mapNotNull { it.toMedia() }
    }

    suspend fun extras(media: Media): Extras {
        val pagina = json.decodeFromString(
            Page.serializer(),
            pedirTexto("$raiz/lookup", mapOf("id" to idDe(media), "entity" to "song", "limit" to "200", "country" to "ES")),
        )
        if (media.type == MediaType.ALBUM) {
            val canciones = pagina.results.filter { it.wrapperType == "track" }.sortedWith(compareBy({ it.discNumber ?: 1 }, { it.trackNumber ?: 0 }))
            val album = pagina.results.firstOrNull { it.wrapperType == "collection" }
            return Extras(
                datos = listOfNotNull(
                    album?.artistName?.let { "Artista" to it },
                    album?.releaseDate?.take(10)?.let { "Publicación" to fechaCorta(it) },
                    album?.primaryGenreName?.let { "Género" to it },
                    canciones.sumOf { it.trackTimeMillis ?: 0L }.takeIf { it > 0 }?.let { "Duración" to "${it / 60_000} min" },
                ),
                // en la lista, el dato secundario de cada cancion es lo que dura
                canciones = canciones.mapNotNull { t -> t.toMedia()?.copy(extra = t.trackTimeMillis?.let(::minutosYSegundos)) },
            )
        }
        val song = pagina.results.firstOrNull { it.wrapperType == "track" } ?: return Extras()
        val album = song.collectionId?.let { id ->
            json.decodeFromString(Page.serializer(), pedirTexto("$raiz/lookup", mapOf("id" to id.toString(), "country" to "ES")))
                .results.firstNotNullOfOrNull { it.toMedia() }
        }
        return Extras(
            datos = listOfNotNull(
                song.artistName?.let { "Artista" to it },
                song.trackTimeMillis?.let { "Duración" to minutosYSegundos(it) },
                song.trackNumber?.let { n -> song.trackCount?.let { "Pista" to "$n de $it" } },
                song.primaryGenreName?.let { "Género" to it },
            ),
            album = album,
        )
    }

    private fun minutosYSegundos(millis: Long): String = "${millis / 60_000}:${(millis / 1000 % 60).toString().padStart(2, '0')}"

    private fun idDe(media: Media) = media.id.substringAfterLast(':')

    private fun Entry.toMedia(): Media? {
        val esCancion = wrapperType == "track"
        if (!esCancion && wrapperType != "collection") return null
        val title = (if (esCancion) trackName else collectionName) ?: return null
        val id = (if (esCancion) trackId else collectionId) ?: return null
        return Media(
            id = if (esCancion) "itunes:song:$id" else "itunes:album:$id",
            type = if (esCancion) MediaType.SONG else MediaType.ALBUM,
            title = title,
            creator = artistName,
            year = anioDe(releaseDate),
            // la api solo anuncia la miniatura de 100px, pero el mismo fichero existe a mas tamaño
            imageUrl = artworkUrl100?.replace("100x100bb", "600x600bb"),
            genres = listOfNotNull(primaryGenreName),
            extra = when {
                esCancion -> collectionName
                trackCount != null -> "$trackCount canciones"
                else -> null
            },
            link = if (esCancion) trackViewUrl else collectionViewUrl,
        )
    }

    @Serializable
    private class Page(val results: List<Entry> = emptyList())

    @Serializable
    private class Entry(
        val wrapperType: String? = null,
        val artistId: Long? = null,
        val collectionId: Long? = null,
        val trackId: Long? = null,
        val artistName: String? = null,
        val collectionName: String? = null,
        val trackName: String? = null,
        val artworkUrl100: String? = null,
        val releaseDate: String? = null,
        val trackCount: Int? = null,
        val trackNumber: Int? = null,
        val discNumber: Int? = null,
        val trackTimeMillis: Long? = null,
        val primaryGenreName: String? = null,
        val collectionViewUrl: String? = null,
        val trackViewUrl: String? = null,
    )
}
