package com.example.palette.data.remote

import com.example.palette.data.Extras
import com.example.palette.data.Media
import com.example.palette.data.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/**
 * videojuegos: https://api-docs.igdb.com
 *
 * igdb es de twitch y pide un client secret que no puede ir dentro de una app, ademas de no aceptar
 * llamadas desde navegador. por eso no se le pregunta directamente: la consulta se manda a nuestra
 * funcion de servidor (supabase/functions/igdb), que es la que guarda las credenciales y habla con igdb.
 * las consultas son texto en su lenguaje, apicalypse.
 */
internal class Igdb {

    private suspend fun juegos(consulta: String): List<Game> =
        json.decodeFromString(ListSerializer(Game.serializer()), supabase.llamarFuncion("igdb", consulta))

    suspend fun buscar(consulta: String): List<Media> {
        val text = consulta.replace("\\", " ").replace("\"", " ")
        // version_parent = null quita las ediciones "deluxe", "goty"... del mismo juego
        return juegos("search \"$text\"; fields $FIELDS; where version_parent = null; limit 12;").map { it.toMedia() }
    }

    suspend fun detalles(media: Media): Media {
        val game = juegos("fields $FIELDS,involved_companies.developer,involved_companies.company.name; where id = ${idDe(media)};")
            .firstOrNull() ?: return media
        return game.toMedia().copy(creator = game.companies.firstOrNull { it.developer }?.company?.name)
    }

    suspend fun extras(media: Media): Extras {
        val game = juegos(
            "fields first_release_date,platforms.name,game_modes.name,involved_companies.developer,involved_companies.publisher,involved_companies.company.name; where id = ${idDe(media)};"
        ).firstOrNull() ?: return Extras()
        return Extras(
            listOfNotNull(
                game.companies.filter { it.developer }.takeIf { it.isNotEmpty() }?.let { "Desarrolla" to it.joinToString(", ") { c -> c.company?.name.orEmpty() } },
                game.companies.filter { it.publisher && !it.developer }.takeIf { it.isNotEmpty() }?.let { "Publica" to it.joinToString(", ") { c -> c.company?.name.orEmpty() } },
                game.platforms.takeIf { it.isNotEmpty() }?.let { "Plataformas" to it.joinToString(", ") { p -> p.name } },
                game.modes.takeIf { it.isNotEmpty() }?.let { "Modos" to it.joinToString(", ") { m -> m.name } },
                game.released?.let { "Lanzamiento" to (1970 + it / 31_556_952).toString() },
            )
        )
    }

    suspend fun similar(media: Media): List<Media> {
        val anidados = FIELDS.split(",").joinToString(",") { "similar_games.$it" }
        val game = juegos("fields $anidados; where id = ${idDe(media)};").firstOrNull() ?: return emptyList()
        return game.similar.map { it.toMedia() }
    }

    private fun idDe(media: Media) = media.id.substringAfter(':').toLongOrNull() ?: 0

    private fun Game.toMedia() = Media(
        id = "igdb:$id",
        type = MediaType.GAME,
        title = name,
        // la fecha llega en segundos desde 1970
        year = released?.let { (1970 + it / 31_556_952).toInt() },
        imageUrl = cover?.imageId?.let { "https://images.igdb.com/igdb/image/upload/t_cover_big/$it.jpg" },
        overview = summary?.takeIf { it.isNotBlank() },
        // igdb puntua sobre 100
        score = rating?.takeIf { (ratingCount ?: 0) > 0 }?.let { it / 10 },
        scoreSource = "IGDB",
        votes = ratingCount,
        genres = genres.map { it.name },
        link = slug?.let { "https://www.igdb.com/games/$it" },
    )

    @Serializable
    private class Game(
        val id: Long = 0,
        val name: String = "",
        val slug: String? = null,
        val summary: String? = null,
        @SerialName("first_release_date") val released: Long? = null,
        @SerialName("total_rating") val rating: Double? = null,
        @SerialName("total_rating_count") val ratingCount: Int? = null,
        val cover: Image? = null,
        val genres: List<Named> = emptyList(),
        @SerialName("involved_companies") val companies: List<Involved> = emptyList(),
        val platforms: List<Named> = emptyList(),
        @SerialName("game_modes") val modes: List<Named> = emptyList(),
        @SerialName("similar_games") val similar: List<Game> = emptyList(),
    )

    @Serializable
    private class Image(@SerialName("image_id") val imageId: String? = null)

    @Serializable
    private class Named(val name: String = "")

    @Serializable
    private class Involved(val developer: Boolean = false, val publisher: Boolean = false, val company: Named? = null)

    private companion object {
        const val FIELDS = "name,slug,summary,first_release_date,total_rating,total_rating_count,cover.image_id,genres.name"
    }
}
