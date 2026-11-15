package com.example.palette.features.catalog.domain.usecases

import com.example.palette.core.model.LogEntry
import com.example.palette.features.catalog.domain.model.FilaRecomendada
import com.example.palette.features.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

// recomienda a partir de lo que mas me ha gustado: por cada favorito se pide contenido parecido
class GetRecommendationsUseCase(private val catalogRepository: CatalogRepository) {

    // los logs que se usan como punto de partida: los mejor puntuados de cada tipo,
    // para que salgan recomendaciones de todo lo que he subido y no solo de lo que mas abunda
    fun favoritos(logs: List<LogEntry>): List<LogEntry> =
        logs.filter { it.stars >= NOTA_MINIMA }
            .groupBy { it.media.type }
            .flatMap { (_, delTipo) -> delTipo.sortedWith(compareByDescending<LogEntry> { it.stars }.thenByDescending { it.createdAt }).take(POR_TIPO) }
            .sortedWith(compareByDescending<LogEntry> { it.stars }.thenByDescending { it.createdAt })

    suspend operator fun invoke(logs: List<LogEntry>): List<FilaRecomendada> = coroutineScope {
        val filas = favoritos(logs).map { log ->
            // una peticion por favorito, todas a la vez
            async { FilaRecomendada("Porque te gustó ${log.media.title}", log.media, catalogRepository.similar(log.media).filter { it.imageUrl != null }) }
        }.awaitAll()
        val tendencias = catalogRepository.tendencias()
        filas + FilaRecomendada("Lo más visto esta semana", null, tendencias)
    }

    // regla de negocio: nunca se recomienda algo que ya esta en mi perfil o en pendientes
    fun sinLoQueYaTengo(filas: List<FilaRecomendada>, idsQueTengo: Set<String>): List<FilaRecomendada> =
        filas.map { it.copy(contenidos = it.contenidos.filter { c -> c.id !in idsQueTengo }) }.filter { it.contenidos.isNotEmpty() }

    private companion object {
        const val NOTA_MINIMA = 4f
        const val POR_TIPO = 2
    }
}
