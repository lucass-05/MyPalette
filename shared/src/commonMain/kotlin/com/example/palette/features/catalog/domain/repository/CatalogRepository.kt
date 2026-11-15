package com.example.palette.features.catalog.domain.repository

import com.example.palette.core.model.Extras
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType

// la interfaz vive en domain y la implementa data (CatalogRepositoryImpl).
// asi el resto de la app pide "busca esto" sin saber si detras hay tmdb, igdb u otra cosa:
// si un dia cambia una base de datos, solo se toca la implementacion.
interface CatalogRepository {

    // tipos que ahora mismo no se pueden buscar (falta su clave)
    val noDisponibles: Set<MediaType>

    suspend fun buscar(consulta: String, tipos: Set<MediaType> = emptySet()): List<Media>

    suspend fun detalles(media: Media): Media

    suspend fun extras(media: Media): Extras

    suspend fun similar(media: Media): List<Media>

    suspend fun tendencias(): List<Media>
}
