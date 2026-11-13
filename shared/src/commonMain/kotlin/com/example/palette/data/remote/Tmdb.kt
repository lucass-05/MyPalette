package com.example.palette.data.remote

import com.example.palette.data.Extras
import com.example.palette.data.Media
import com.example.palette.data.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** peliculas y series: https://developer.themoviedb.org/reference */
internal class Tmdb(private val key: String) {

    val ready get() = key.isNotBlank()

    // tmdb da dos credenciales: la "api key" corta (v3) y el "read access token" largo (v4). vale cualquiera.
    private val isToken = key.length > 60

    private suspend fun call(path: String, params: Map<String, String?> = emptyMap()): String {
        val all = params + mapOf("language" to "es-ES") + if (isToken) emptyMap() else mapOf("api_key" to key)
        return getText("https://api.themoviedb.org/3/$path", all, bearer = key.takeIf { isToken })
    }

    suspend fun search(query: String, movies: Boolean, series: Boolean): List<Media> {
        val path = when {
            movies && series -> "search/multi"
            movies -> "search/movie"
            else -> "search/tv"
        }
        val fallback = if (movies && !series) "movie" else "tv"
        val page = json.decodeFromString(Page.serializer(), call(path, mapOf("query" to query, "include_adult" to "false")))
        return page.results.mapNotNull { it.toMedia(fallback) }
    }

    suspend fun details(media: Media): Media {
        val (kind, id) = split(media.id)
        val d = json.decodeFromString(Item.serializer(), call("$kind/$id", mapOf("append_to_response" to "credits")))
        val base = d.toMedia(kind) ?: return media
        val director = d.credits?.crew?.firstOrNull { it.job == "Director" }?.name
        val creator = director ?: d.createdBy.firstOrNull()?.name
        val extra = when {
            d.runtime != null && d.runtime > 0 -> "${d.runtime} min"
            d.seasons != null -> if (d.seasons == 1) "1 temporada" else "${d.seasons} temporadas"
            else -> null
        }
        return base.copy(creator = creator, genres = d.genres.map { it.name }, extra = extra)
    }

    suspend fun extras(media: Media): Extras {
        val (kind, id) = split(media.id)
        val d = json.decodeFromString(Item.serializer(), call("$kind/$id", mapOf("append_to_response" to "credits")))
        val original = (d.originalTitle ?: d.originalName)?.takeIf { it != (d.title ?: d.name) }
        return Extras(
            listOfNotNull(
                d.credits?.crew?.filter { it.job == "Director" }?.takeIf { it.isNotEmpty() }?.let { "Dirección" to it.joinToString(", ") { c -> c.name } },
                d.createdBy.takeIf { it.isNotEmpty() }?.let { "Creada por" to it.joinToString(", ") { c -> c.name } },
                d.credits?.cast?.take(6)?.takeIf { it.isNotEmpty() }?.let { "Reparto" to it.joinToString(", ") { c -> c.name } },
                d.runtime?.takeIf { it > 0 }?.let { "Duración" to "${it / 60} h ${it % 60} min" },
                d.seasons?.let { "Temporadas" to it.toString() },
                d.episodes?.let { "Episodios" to it.toString() },
                d.networks.firstOrNull()?.let { "Cadena" to it.name },
                (d.releaseDate ?: d.firstAirDate)?.takeIf { it.isNotBlank() }?.let { "Estreno" to spanishDate(it) },
                original?.let { "Título original" to it },
            )
        )
    }

    suspend fun similar(media: Media): List<Media> {
        val (kind, id) = split(media.id)
        val page = json.decodeFromString(Page.serializer(), call("$kind/$id/recommendations"))
        return page.results.mapNotNull { it.toMedia(kind) }
    }

    suspend fun trending(): List<Media> {
        val page = json.decodeFromString(Page.serializer(), call("trending/all/week"))
        return page.results.mapNotNull { it.toMedia("movie") }
    }

    private fun split(id: String): Pair<String, String> {
        val parts = id.split(":")
        return parts[1] to parts[2]
    }

    private fun Item.toMedia(fallbackKind: String): Media? {
        val kind = mediaType ?: fallbackKind
        if (kind != "movie" && kind != "tv") return null
        val name = title ?: name ?: return null
        return Media(
            id = "tmdb:$kind:$id",
            type = if (kind == "movie") MediaType.MOVIE else MediaType.SERIES,
            title = name,
            year = yearOf(releaseDate ?: firstAirDate),
            imageUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
            overview = overview?.takeIf { it.isNotBlank() },
            score = voteAverage?.takeIf { (voteCount ?: 0) > 0 },
            scoreSource = "TMDB",
            votes = voteCount,
            link = "https://www.themoviedb.org/$kind/$id",
        )
    }

    @Serializable
    private class Page(val results: List<Item> = emptyList())

    @Serializable
    private class Item(
        val id: Long = 0,
        @SerialName("media_type") val mediaType: String? = null,
        val title: String? = null,
        val name: String? = null,
        val overview: String? = null,
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("backdrop_path") val backdropPath: String? = null,
        @SerialName("release_date") val releaseDate: String? = null,
        @SerialName("first_air_date") val firstAirDate: String? = null,
        @SerialName("vote_average") val voteAverage: Double? = null,
        @SerialName("vote_count") val voteCount: Int? = null,
        val runtime: Int? = null,
        @SerialName("number_of_seasons") val seasons: Int? = null,
        @SerialName("number_of_episodes") val episodes: Int? = null,
        @SerialName("original_title") val originalTitle: String? = null,
        @SerialName("original_name") val originalName: String? = null,
        val networks: List<Named> = emptyList(),
        val genres: List<Named> = emptyList(),
        @SerialName("created_by") val createdBy: List<Named> = emptyList(),
        val credits: Credits? = null,
    )

    @Serializable
    private class Named(val name: String = "")

    @Serializable
    private class Credits(val crew: List<Crew> = emptyList(), val cast: List<Named> = emptyList())

    @Serializable
    private class Crew(val name: String = "", val job: String = "")
}
