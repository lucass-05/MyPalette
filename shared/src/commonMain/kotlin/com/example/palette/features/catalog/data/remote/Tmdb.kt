package com.example.palette.features.catalog.data.remote

import com.example.palette.core.model.Extras
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.core.network.anioDe
import com.example.palette.core.network.fechaCorta
import com.example.palette.core.network.json
import com.example.palette.core.network.pedirTexto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** peliculas y series: https://developer.themoviedb.org/reference */
internal class Tmdb(private val key: String) {

    val ready get() = key.isNotBlank()

    // tmdb da dos credenciales: la "api key" corta (v3) y el "read access token" largo (v4). vale cualquiera.
    private val esToken = key.length > 60

    private suspend fun llamarFuncion(ruta: String, parametros: Map<String, String?> = emptyMap()): String {
        val todos = parametros + mapOf("language" to "es-ES") + if (esToken) emptyMap() else mapOf("api_key" to key)
        return pedirTexto("https://api.themoviedb.org/3/$ruta", todos, credencial = key.takeIf { esToken })
    }

    suspend fun buscar(consulta: String, pelis: Boolean, series: Boolean): List<Media> {
        val ruta = when {
            pelis && series -> "search/multi"
            pelis -> "search/movie"
            else -> "search/tv"
        }
        val porDefecto = if (pelis && !series) "movie" else "tv"
        val pagina = json.decodeFromString(Page.serializer(), llamarFuncion(ruta, mapOf("query" to consulta, "include_adult" to "false")))
        return pagina.results.mapNotNull { it.toMedia(porDefecto) }
    }

    suspend fun detalles(media: Media): Media {
        val (kind, id) = split(media.id)
        val d = json.decodeFromString(Item.serializer(), llamarFuncion("$kind/$id", mapOf("append_to_response" to "credits")))
        val raiz = d.toMedia(kind) ?: return media
        val director = d.credits?.crew?.firstOrNull { it.job == "Director" }?.name
        val creator = director ?: d.createdBy.firstOrNull()?.name
        val extra = when {
            d.runtime != null && d.runtime > 0 -> "${d.runtime} min"
            d.seasons != null -> if (d.seasons == 1) "1 temporada" else "${d.seasons} temporadas"
            else -> null
        }
        return raiz.copy(creator = creator, genres = d.genres.map { it.name }, extra = extra)
    }

    suspend fun extras(media: Media): Extras {
        val (kind, id) = split(media.id)
        val d = json.decodeFromString(Item.serializer(), llamarFuncion("$kind/$id", mapOf("append_to_response" to "credits")))
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
                (d.releaseDate ?: d.firstAirDate)?.takeIf { it.isNotBlank() }?.let { "Estreno" to fechaCorta(it) },
                original?.let { "Título original" to it },
            )
        )
    }

    suspend fun similar(media: Media): List<Media> {
        val (kind, id) = split(media.id)
        val pagina = json.decodeFromString(Page.serializer(), llamarFuncion("$kind/$id/recommendations"))
        return pagina.results.mapNotNull { it.toMedia(kind) }
    }

    suspend fun tendencias(): List<Media> {
        val pagina = json.decodeFromString(Page.serializer(), llamarFuncion("trending/all/week"))
        return pagina.results.mapNotNull { it.toMedia("movie") }
    }

    private fun split(id: String): Pair<String, String> {
        val partes = id.split(":")
        return partes[1] to partes[2]
    }

    private fun Item.toMedia(clasePorDefecto: String): Media? {
        val kind = mediaType ?: clasePorDefecto
        if (kind != "movie" && kind != "tv") return null
        val name = title ?: name ?: return null
        return Media(
            id = "tmdb:$kind:$id",
            type = if (kind == "movie") MediaType.MOVIE else MediaType.SERIES,
            title = name,
            year = anioDe(releaseDate ?: firstAirDate),
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
