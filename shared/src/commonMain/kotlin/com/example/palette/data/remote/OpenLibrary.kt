package com.example.palette.data.remote

import com.example.palette.data.Media
import com.example.palette.data.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** libros: https://openlibrary.org/dev/docs/api/search. no pide clave. */
internal class OpenLibrary {

    private val raiz = "https://openlibrary.org"
    private val campos = "key,title,author_name,first_publish_year,cover_i,ratings_average,ratings_count,number_of_pages_median,subject"

    suspend fun buscar(consulta: String): List<Media> = find(mapOf("q" to consulta))

    suspend fun detalles(media: Media): Media {
        val obra = json.decodeFromString(Work.serializer(), pedirTexto("$raiz/works/${media.id.substringAfter(':')}.json"))
        // la descripcion llega a veces como texto y a veces como {"type": ..., "value": ...}
        val text = when (val d = obra.description) {
            is JsonPrimitive -> d.content
            is JsonObject -> (d["value"] as? JsonPrimitive)?.content
            else -> null
        }
        return media.copy(overview = text?.substringBefore("\n----")?.trim()?.takeIf { it.isNotEmpty() } ?: media.overview)
    }

    /** otros libros del mismo autor. */
    suspend fun similar(media: Media): List<Media> {
        val autor = media.creator ?: return emptyList()
        return find(mapOf("author" to autor, "sort" to "rating")).filter { it.id != media.id }
    }

    private suspend fun find(parametros: Map<String, String>): List<Media> {
        val pagina = json.decodeFromString(
            Page.serializer(),
            pedirTexto("$raiz/search.json", parametros + mapOf("fields" to campos, "limit" to "12", "lang" to "es")),
        )
        return pagina.docs.mapNotNull { it.toMedia() }
    }

    private fun Doc.toMedia(): Media? {
        val idObra = key?.substringAfterLast('/') ?: return null
        return Media(
            id = "ol:$idObra",
            type = MediaType.BOOK,
            title = title ?: return null,
            creator = authors.firstOrNull(),
            year = year,
            imageUrl = cover?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" },
            // open library puntua de 1 a 5
            score = rating?.takeIf { (ratingCount ?: 0) > 0 }?.let { it * 2 },
            scoreSource = "Open Library",
            votes = ratingCount,
            genres = subjects.take(3),
            extra = pages?.let { "$it páginas" },
            link = "$raiz/works/$idObra",
        )
    }

    @Serializable
    private class Page(val docs: List<Doc> = emptyList())

    @Serializable
    private class Doc(
        val key: String? = null,
        val title: String? = null,
        @SerialName("author_name") val authors: List<String> = emptyList(),
        @SerialName("first_publish_year") val year: Int? = null,
        @SerialName("cover_i") val cover: Long? = null,
        @SerialName("ratings_average") val rating: Double? = null,
        @SerialName("ratings_count") val ratingCount: Int? = null,
        @SerialName("number_of_pages_median") val pages: Int? = null,
        @SerialName("subject") val subjects: List<String> = emptyList(),
    )

    @Serializable
    private class Work(val description: JsonElement? = null)
}
