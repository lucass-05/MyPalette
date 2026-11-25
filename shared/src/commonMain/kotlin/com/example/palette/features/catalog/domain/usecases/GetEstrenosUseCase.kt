package com.example.palette.features.catalog.domain.usecases

import com.example.palette.features.catalog.domain.model.Estreno
import com.example.palette.features.catalog.domain.repository.CatalogRepository

/** los estrenos del mes para el carrusel de descubrir. no dependen de mis gustos: son los mismos para todos. */
class GetEstrenosUseCase(private val catalogRepository: CatalogRepository) {
    suspend operator fun invoke(): List<Estreno> = catalogRepository.estrenos()
}
