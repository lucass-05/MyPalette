package com.example.palette.features.catalog.domain.usecases

import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.features.catalog.domain.repository.CatalogRepository

// un caso de uso es una sola accion de negocio: aqui, "buscar contenido".
// tambien es el sitio de las reglas que no dependen de la pantalla ni de la red:
// no se busca con menos de dos letras.
class SearchMediaUseCase(private val catalogRepository: CatalogRepository) {

    // "operator fun invoke" permite llamar al objeto como si fuera una funcion: searchMediaUseCase(texto, tipos)
    suspend operator fun invoke(consulta: String, tipos: Set<MediaType>): List<Media>? {
        val texto = consulta.trim()
        if (texto.length < MIN_LETRAS) return null
        return catalogRepository.buscar(texto, tipos)
    }

    private companion object {
        const val MIN_LETRAS = 2
    }
}
