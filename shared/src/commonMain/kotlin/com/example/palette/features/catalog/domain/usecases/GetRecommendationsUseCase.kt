package com.example.palette.features.catalog.domain.usecases

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.MediaType
import com.example.palette.features.catalog.domain.model.FilaRecomendada
import com.example.palette.features.catalog.domain.model.TipoDeFila
import com.example.palette.features.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * las filas de descubrir, en este orden:
 * 1. "porque te gustó...": una por cada una de mis ultimas 5 reseñas con mas de 3 estrellas
 * 2. lo mas visto de la semana
 * 3. series en emision parecidas a las que he visto
 */
class GetRecommendationsUseCase(private val catalogRepository: CatalogRepository) {

    /** mis ultimos logs con mas de 3 estrellas, del mas nuevo al mas viejo. */
    fun ultimasBuenas(logs: List<LogEntry>): List<LogEntry> =
        logs.filter { it.stars > NOTA_MINIMA }.sortedByDescending { it.createdAt }

    /** mis series, de la que mas me gusto a la que menos: con ellas se elige que hay en emision para mi. */
    fun misSeries(logs: List<LogEntry>): List<LogEntry> =
        logs.filter { it.media.type == MediaType.SERIES }
            .sortedWith(compareByDescending<LogEntry> { it.stars }.thenByDescending { it.createdAt })

    /** de que dependen las filas: si no cambia, no hace falta volver a preguntar a la red. */
    fun clave(logs: List<LogEntry>): String =
        (ultimasBuenas(logs).take(CANDIDATAS) + misSeries(logs).take(SERIES_PARA_EMISION)).joinToString("|") { it.media.id }

    suspend operator fun invoke(logs: List<LogEntry>): List<FilaRecomendada> = coroutineScope {
        // se piden mas de 5 por si alguna no tiene nada parecido: asi casi siempre salen 5 filas
        val porque = ultimasBuenas(logs).take(CANDIDATAS).map { log ->
            async { FilaRecomendada("Porque te gustó ${log.media.title}", log.media, catalogRepository.similar(log.media).filter { it.imageUrl != null }) }
        }
        val tendencias = async { catalogRepository.tendencias() }
        val series = misSeries(logs).take(SERIES_PARA_EMISION).map { it.media }
        val emision = async { catalogRepository.enEmision(series) }

        porque.awaitAll() +
            FilaRecomendada("Lo más visto esta semana", null, tendencias.await(), TipoDeFila.TENDENCIAS) +
            FilaRecomendada(if (series.isEmpty()) "Series en emisión" else "En emisión y recomendado para ti", null, emision.await(), TipoDeFila.EMISION)
    }

    // regla de negocio: nunca se recomienda algo que ya esta en mi perfil o en pendientes.
    // despues de quitarlo se quedan las 5 primeras filas de "porque te gustó" que no se hayan vaciado
    fun sinLoQueYaTengo(filas: List<FilaRecomendada>, idsQueTengo: Set<String>): List<FilaRecomendada> {
        val limpias = filas.map { it.copy(contenidos = it.contenidos.filter { c -> c.id !in idsQueTengo }) }.filter { it.contenidos.isNotEmpty() }
        return limpias.filter { it.origen != null }.take(FILAS_PORQUE) + limpias.filter { it.origen == null }
    }

    private companion object {
        // "mas de tres estrellas": 3,5 o mas
        const val NOTA_MINIMA = 3f
        const val FILAS_PORQUE = 5
        const val CANDIDATAS = 8
        const val SERIES_PARA_EMISION = 8
    }
}
