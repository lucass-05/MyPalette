package com.example.palette.features.catalog.data.remote

import com.example.palette.core.model.Extras
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.core.model.ModoDeVer
import com.example.palette.core.model.Plataforma
import com.example.palette.core.network.anioDe
import com.example.palette.core.network.fechaCorta
import com.example.palette.core.network.json
import com.example.palette.core.network.pedirTexto
import com.example.palette.core.util.DAY
import com.example.palette.core.util.fechaIso
import com.example.palette.features.catalog.domain.model.Estreno
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
        // watch/providers dice donde se puede ver; viene en la misma peticion que el reparto
        val d = json.decodeFromString(Item.serializer(), llamarFuncion("$kind/$id", mapOf("append_to_response" to "credits,watch/providers")))
        val espana = d.providers?.results?.get("ES")
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
            ),
            plataformas = espana?.let { plataformas(it) }.orEmpty(),
            enlacePlataformas = espana?.link,
        )
    }

    // cada servicio sale una vez, con la forma mas barata de verlo (suscripcion antes que alquiler o compra)
    private fun plataformas(region: Region): List<Plataforma> {
        val todas = listOf(
            ModoDeVer.SUSCRIPCION to region.flatrate,
            ModoDeVer.GRATIS to region.free,
            ModoDeVer.ANUNCIOS to region.ads,
            ModoDeVer.ALQUILER to region.rent,
            ModoDeVer.COMPRA to region.buy,
        ).flatMap { (modo, lista) ->
            lista.sortedBy { it.priority }.map { Plataforma(it.name, it.logoPath?.let { logo -> "https://image.tmdb.org/t/p/w92$logo" }, modo) }
        }
        return todas.distinctBy { it.nombre }
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

    /**
     * series que estan sacando capitulos estas semanas, ordenadas por lo que se parecen a las mias.
     *
     * de mis series se sacan los generos que mas se repiten y, si casi todas son del mismo idioma
     * original (por ejemplo japones en el anime), tambien ese idioma. con eso se pregunta a discover
     * por lo que emite ahora; si sale poco se va soltando el filtro, primero el idioma y luego los generos.
     */
    suspend fun enEmision(mias: List<Media>, hoy: Long): List<Media> = coroutineScope {
        val fichas = mias.filter { it.id.startsWith("tmdb:tv:") }.take(MAX_SERIES_MIAS)
            .map { media -> async { runCatching { json.decodeFromString(Item.serializer(), llamarFuncion("tv/${split(media.id).second}")) }.getOrNull() } }
            .awaitAll().filterNotNull()
        val pesoDeGenero = fichas.flatMap { ficha -> ficha.genres.map { it.id } }.groupingBy { it }.eachCount()
        val generos = pesoDeGenero.entries.sortedByDescending { it.value }.take(3).map { it.key }
        // el idioma solo cuenta si es el de al menos la mitad de mis series
        val idioma = fichas.mapNotNull { it.idioma }.groupingBy { it }.eachCount().maxByOrNull { it.value }
            ?.takeIf { fichas.size >= 2 && it.value * 2 >= fichas.size }?.key

        val enAntena = mapOf(
            "air_date.gte" to fechaIso(hoy - 7 * DAY),
            "air_date.lte" to fechaIso(hoy + 7 * DAY),
            "sort_by" to "popularity.desc",
            "vote_count.gte" to "5",
        )
        suspend fun descubrir(filtro: Map<String, String>): List<Item> =
            runCatching { json.decodeFromString(Page.serializer(), llamarFuncion("discover/tv", enAntena + filtro)).results }.getOrDefault(emptyList())

        // con "|" tmdb busca series que tengan cualquiera de esos generos
        val conGeneros = if (generos.isEmpty()) emptyMap() else mapOf("with_genres" to generos.joinToString("|"))
        val candidatos = mutableListOf<Item>()
        if (idioma != null && generos.isNotEmpty()) candidatos += descubrir(conGeneros + ("with_original_language" to idioma))
        if (candidatos.size < SUFICIENTES && generos.isNotEmpty()) candidatos += descubrir(conGeneros)
        if (candidatos.size < SUFICIENTES) candidatos += descubrir(emptyMap())

        // primero lo que mas generos comparte con lo mio (y mi idioma); a igualdad, lo mas popular
        candidatos.distinctBy { it.id }
            .withIndex()
            .sortedByDescending { (posicion, item) ->
                item.genreIds.sumOf { pesoDeGenero[it] ?: 0 } + (if (idioma != null && item.idioma == idioma) fichas.size else 0) - posicion * 0.01
            }
            .mapNotNull { it.value.toMedia("tv") }
    }

    /**
     * los estrenos del ultimo mes que mas se estan viendo: pelis estrenadas en españa y series nuevas,
     * mezcladas por popularidad. solo los que tienen imagen ancha y sinopsis, que es lo que enseña el carrusel.
     */
    suspend fun estrenos(hoy: Long, cuantos: Int = 5): List<Estreno> = coroutineScope {
        val desde = fechaIso(hoy - 35 * DAY)
        val hasta = fechaIso(hoy)
        suspend fun lista(ruta: String, filtro: Map<String, String>) =
            runCatching { json.decodeFromString(Page.serializer(), llamarFuncion(ruta, filtro + ("sort_by" to "popularity.desc"))).results }.getOrDefault(emptyList())
        val pelis = async {
            lista("discover/movie", mapOf("primary_release_date.gte" to desde, "primary_release_date.lte" to hasta, "region" to "ES", "with_release_type" to "2|3"))
                .map { it to "movie" }
        }
        val series = async { lista("discover/tv", mapOf("first_air_date.gte" to desde, "first_air_date.lte" to hasta)).map { it to "tv" } }
        val elegidos = (pelis.await().take(8) + series.await().take(4))
            .filter { (item, _) -> item.backdropPath != null && !item.overview.isNullOrBlank() }
            .sortedByDescending { (item, _) -> item.popularidad ?: 0.0 }
            .take(cuantos)
        // el trailer de cada uno, a la vez: en español si lo hay, si no en ingles
        elegidos.map { (item, clase) ->
            async {
                val media = item.toMedia(clase) ?: return@async null
                Estreno(media.copy(backdropUrl = item.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }), trailer(clase, item.id))
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun trailer(clase: String, id: Long): String? = runCatching {
        val videos = json.decodeFromString(Videos.serializer(), llamarFuncion("$clase/$id/videos", mapOf("include_video_language" to "es,en"))).results
            .filter { it.site == "YouTube" && it.key.isNotBlank() }
        val mejor = videos.firstOrNull { it.type == "Trailer" && it.idioma == "es" }
            ?: videos.firstOrNull { it.type == "Trailer" }
            ?: videos.firstOrNull { it.type == "Teaser" }
        mejor?.let { "https://www.youtube.com/watch?v=${it.key}" }
    }.getOrNull()

    @Serializable
    private class Videos(val results: List<Video> = emptyList())

    @Serializable
    private class Video(val key: String = "", val site: String = "", val type: String = "", @SerialName("iso_639_1") val idioma: String? = null)

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
        @SerialName("popularity") val popularidad: Double? = null,
        // en las listas los generos vienen solo como numeros; en la ficha, como Named con id y nombre
        @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
        @SerialName("original_language") val idioma: String? = null,
        val runtime: Int? = null,
        @SerialName("number_of_seasons") val seasons: Int? = null,
        @SerialName("number_of_episodes") val episodes: Int? = null,
        @SerialName("original_title") val originalTitle: String? = null,
        @SerialName("original_name") val originalName: String? = null,
        val networks: List<Named> = emptyList(),
        val genres: List<Named> = emptyList(),
        @SerialName("created_by") val createdBy: List<Named> = emptyList(),
        val credits: Credits? = null,
        @SerialName("watch/providers") val providers: Providers? = null,
    )

    @Serializable
    private class Providers(val results: Map<String, Region> = emptyMap())

    @Serializable
    private class Region(
        val link: String? = null,
        val flatrate: List<Provider> = emptyList(),
        val free: List<Provider> = emptyList(),
        val ads: List<Provider> = emptyList(),
        val rent: List<Provider> = emptyList(),
        val buy: List<Provider> = emptyList(),
    )

    @Serializable
    private class Provider(
        @SerialName("provider_name") val name: String = "",
        @SerialName("logo_path") val logoPath: String? = null,
        @SerialName("display_priority") val priority: Int = 0,
    )

    @Serializable
    private class Named(val name: String = "", val id: Int = 0)

    @Serializable
    private class Credits(val crew: List<Crew> = emptyList(), val cast: List<Named> = emptyList())

    @Serializable
    private class Crew(val name: String = "", val job: String = "")
}

// cuantas series mias se miran para saber que me gusta, y cuantas en emision se buscan como minimo
private const val MAX_SERIES_MIAS = 8
private const val SUFICIENTES = 12
