package com.example.palette.features.catalog.domain.usecases

import com.example.palette.core.model.Media
import com.example.palette.features.catalog.domain.model.FichaCompleta
import com.example.palette.features.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// completa la ficha de un contenido: sus detalles, los datos propios de su tipo y contenido parecido
class GetMediaDetailUseCase(private val catalogRepository: CatalogRepository) {

    suspend operator fun invoke(media: Media): FichaCompleta = coroutineScope {
        val completo = catalogRepository.detalles(media)
        // las dos peticiones que quedan no dependen una de otra: se lanzan a la vez
        val extras = async { catalogRepository.extras(completo) }
        val parecidos = async { catalogRepository.similar(completo).filter { it.imageUrl != null } }
        FichaCompleta(completo, extras.await(), parecidos.await())
    }
}
