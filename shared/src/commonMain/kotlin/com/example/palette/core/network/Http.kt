package com.example.palette.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

// ktor es la libreria de red. HttpClient() elige solo el motor de cada plataforma
// (okhttp en android y escritorio, darwin en ios, fetch en web).
// las respuestas llegan como texto json y kotlinx.serialization las convierte en objetos:
// cada clase marcada con @Serializable declara los campos que nos interesan y el resto se ignora.

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// el motor (okhttp, darwin o fetch) lo pone cada plataforma en su sourceset
internal val http by lazy { HttpClient() }

class ApiException(message: String) : Exception(message)

internal suspend fun pedirTexto(
    url: String,
    parametros: Map<String, String?> = emptyMap(),
    credencial: String? = null,
): String {
    val respuesta = http.get(url) {
        parametros.forEach { (k, v) -> if (v != null) parameter(k, v) }
        if (credencial != null) header("Authorization", "Bearer $credencial")
        header("Accept", "application/json")
    }
    if (!respuesta.status.isSuccess()) throw ApiException("${respuesta.status.value} en $url")
    return respuesta.bodyAsText()
}

private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** "2014-11-07" → "7 nov 2014". si no tiene esa forma, se deja como venga. */
internal fun fechaCorta(iso: String): String {
    val partes = iso.take(10).split("-").mapNotNull { it.toIntOrNull() }
    if (partes.size != 3 || partes[1] !in 1..12) return iso
    return "${partes[2]} ${meses[partes[1] - 1]} ${partes[0]}"
}

internal fun anioDe(date: String?): Int? = date?.take(4)?.toIntOrNull()
