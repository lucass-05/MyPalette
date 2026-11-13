package com.example.palette.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// el motor (okhttp, darwin o fetch) lo pone cada plataforma en su sourceset
internal val http by lazy { HttpClient() }

class ApiException(message: String) : Exception(message)

internal suspend fun getText(
    url: String,
    params: Map<String, String?> = emptyMap(),
    bearer: String? = null,
): String {
    val response = http.get(url) {
        params.forEach { (k, v) -> if (v != null) parameter(k, v) }
        if (bearer != null) header("Authorization", "Bearer $bearer")
        header("Accept", "application/json")
    }
    if (!response.status.isSuccess()) throw ApiException("${response.status.value} en $url")
    return response.bodyAsText()
}

private val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** "2014-11-07" → "7 nov 2014". si no tiene esa forma, se deja como venga. */
internal fun spanishDate(iso: String): String {
    val parts = iso.take(10).split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3 || parts[1] !in 1..12) return iso
    return "${parts[2]} ${months[parts[1] - 1]} ${parts[0]}"
}

internal fun yearOf(date: String?): Int? = date?.take(4)?.toIntOrNull()
