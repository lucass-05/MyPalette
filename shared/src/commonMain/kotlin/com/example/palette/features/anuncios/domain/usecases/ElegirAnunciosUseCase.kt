package com.example.palette.features.anuncios.domain.usecases

import com.example.palette.core.model.Media
import com.example.palette.core.monetizacion.Monetizacion
import com.example.palette.features.anuncios.domain.model.Anuncio
import com.example.palette.features.anuncios.domain.model.AnunciosDeDescubrir
import com.example.palette.features.anuncios.domain.model.Hueco
import com.example.palette.features.anuncios.domain.repository.AnunciosRepository

/**
 * que anuncios salen en descubrir.
 * - con los anuncios apagados, ninguno.
 * - si hay contratados, el primero de cada hueco.
 * - en modo demostracion y sin contratados, una fila de ejemplo hecha con contenidos que ya se ven en
 *   descubrir. el cartel no tiene ejemplo: el carrusel de estrenos ya enseña como se veria.
 */
class ElegirAnunciosUseCase(private val anunciosRepository: AnunciosRepository) {

    operator fun invoke(populares: List<Media>): AnunciosDeDescubrir {
        if (!Monetizacion.anuncios) return AnunciosDeDescubrir()
        val contratados = anunciosRepository.vigentes()
        val cartel = contratados.firstOrNull { it.hueco == Hueco.CARTEL }
        val fila = contratados.firstOrNull { it.hueco == Hueco.FILA }
        if (!Monetizacion.demostracion) return AnunciosDeDescubrir(cartel, fila)
        return AnunciosDeDescubrir(cartel, fila ?: filaDeEjemplo(populares))
    }

    private fun filaDeEjemplo(populares: List<Media>): Anuncio? {
        val contenidos = populares.drop(1).take(8)
        if (contenidos.isEmpty()) return null
        return Anuncio(
            id = "ejemplo-fila",
            hueco = Hueco.FILA,
            anunciante = "Tu plataforma",
            titulo = "Novedades de este mes",
            boton = "Ver catálogo",
            enlace = "https://www.themoviedb.org",
            contenidos = contenidos,
            ejemplo = true,
        )
    }
}
