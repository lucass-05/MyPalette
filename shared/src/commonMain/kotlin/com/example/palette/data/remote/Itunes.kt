package com.example.palette.data.remote

import com.example.palette.data.Extras
import com.example.palette.data.Media
import com.example.palette.data.MediaType
import kotlinx.serialization.Serializable

/** albumes y canciones. la busqueda de itunes es publica, no pide clave. */
internal class Itunes {

    private val base = "https://itunes.apple.com"

    suspend fun search(query: String, type: MediaType): List<Media> {
        val entity = if (type == MediaType.ALBUM) "album" else "song"
        val page = json.decodeFromString(
            Page.serializer(),
            getText("$base/search", mapOf("term" to query, "media" to "music", "entity" to entity, "limit" to "12", "country" to "ES")),
        )
        return page.results.mapNotNull { it.toMedia() }
    }

    suspend fun details(media: Media): Media {
        val page = json.decodeFromString(Page.serializer(), getText("$base/lookup", mapOf("id" to idOf(media), "country" to "ES")))
        return page.results.firstNotNullOfOrNull { it.toMedia() } ?: media
    }

    /** mas discos del mismo artista. */
    suspend fun similar(media: Media): List<Media> {
        val own = json.decodeFromString(Page.serializer(), getText("$base/lookup", mapOf("id" to idOf(media), "country" to "ES")))
        val artist = own.results.firstOrNull()?.artistId ?: return emptyList()
        val page = json.decodeFromString(
            Page.serializer(),
            getText("$base/lookup", mapOf("id" to artist.toString(), "entity" to "album", "limit" to "14", "country" to "ES")),
        )
        val current = own.results.firstOrNull()?.collectionId
        return page.results.filter { it.wrapperType == "collection" && it.collectionId != current }.mapNotNull { it.toMedia() }
    }

    suspend fun extras(media: Media): Extras {
        val page = json.decodeFromString(
            Page.serializer(),
            getText("$base/lookup", mapOf("id" to idOf(media), "entity" to "song", "limit" to "200", "country" to "ES")),
        )
        if (media.type == MediaType.ALBUM) {
            val tracks = page.results.filter { it.wrapperType == "track" }.sortedWith(compareBy({ it.discNumber ?: 1 }, { it.trackNumber ?: 0 }))
            val album = page.results.firstOrNull { it.wrapperType == "collection" }
            return Extras(
                facts = listOfNotNull(
                    album?.artistName?.let { "Artista" to it },
                    album?.releaseDate?.take(10)?.let { "Publicación" to spanishDate(it) },
                    album?.primaryGenreName?.let { "Género" to it },
                    tracks.sumOf { it.trackTimeMillis ?: 0L }.takeIf { it > 0 }?.let { "Duración" to "${it / 60_000} min" },
                ),
                // en la lista, el dato secundario de cada cancion es lo que dura
                tracks = tracks.mapNotNull { t -> t.toMedia()?.copy(extra = t.trackTimeMillis?.let(::clock)) },
            )
        }
        val song = page.results.firstOrNull { it.wrapperType == "track" } ?: return Extras()
        val album = song.collectionId?.let { id ->
            json.decodeFromString(Page.serializer(), getText("$base/lookup", mapOf("id" to id.toString(), "country" to "ES")))
                .results.firstNotNullOfOrNull { it.toMedia() }
        }
        return Extras(
            facts = listOfNotNull(
                song.artistName?.let { "Artista" to it },
                song.trackTimeMillis?.let { "Duración" to clock(it) },
                song.trackNumber?.let { n -> song.trackCount?.let { "Pista" to "$n de $it" } },
                song.primaryGenreName?.let { "Género" to it },
            ),
            album = album,
        )
    }

    private fun clock(millis: Long): String = "${millis / 60_000}:${(millis / 1000 % 60).toString().padStart(2, '0')}"

    private fun idOf(media: Media) = media.id.substringAfterLast(':')

    private fun Entry.toMedia(): Media? {
        val isTrack = wrapperType == "track"
        if (!isTrack && wrapperType != "collection") return null
        val title = (if (isTrack) trackName else collectionName) ?: return null
        val id = (if (isTrack) trackId else collectionId) ?: return null
        return Media(
            id = if (isTrack) "itunes:song:$id" else "itunes:album:$id",
            type = if (isTrack) MediaType.SONG else MediaType.ALBUM,
            title = title,
            creator = artistName,
            year = yearOf(releaseDate),
            // la api solo anuncia la miniatura de 100px, pero el mismo fichero existe a mas tamaño
            imageUrl = artworkUrl100?.replace("100x100bb", "600x600bb"),
            genres = listOfNotNull(primaryGenreName),
            extra = when {
                isTrack -> collectionName
                trackCount != null -> "$trackCount canciones"
                else -> null
            },
            link = if (isTrack) trackViewUrl else collectionViewUrl,
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
