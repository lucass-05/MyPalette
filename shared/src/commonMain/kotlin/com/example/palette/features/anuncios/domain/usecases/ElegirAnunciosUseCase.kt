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
 * - en modo demostracion (?demo, el enlace de la pagina de presentacion para marcas) y sin contratados:
 *   "vuestro posible anuncio" como primera diapositiva de los estrenos, y una fila patrocinada con un
 *   hueco para el producto de la marca entre contenidos reales.
 */
class ElegirAnunciosUseCase(private val anunciosRepository: AnunciosRepository) {

    operator fun invoke(populares: List<Media>): AnunciosDeDescubrir {
        if (!Monetizacion.anuncios) return AnunciosDeDescubrir()
        val contratados = anunciosRepository.vigentes()
        val cartel = contratados.firstOrNull { it.hueco == Hueco.CARTEL }
        val fila = contratados.firstOrNull { it.hueco == Hueco.FILA }
        if (!Monetizacion.demostracion) return AnunciosDeDescubrir(cartel, fila)
        return AnunciosDeDescubrir(cartel ?: CARTEL_DE_EJEMPLO, fila ?: filaDeEjemplo(populares))
    }

    private fun filaDeEjemplo(populares: List<Media>): Anuncio? {
        val contenidos = populares.drop(1).take(8)
        if (contenidos.isEmpty()) return null
        return Anuncio(
            id = "ejemplo-fila",
            hueco = Hueco.FILA,
            anunciante = "Vuestra marca",
            titulo = "Novedades de este mes",
            boton = "Ver catálogo",
            enlace = CONTACTO_POR_CORREO,
            contenidos = contenidos,
            ejemplo = true,
        )
    }
}

private const val CONTACTO_POR_CORREO = "mailto:${Monetizacion.CONTACTO}?subject=Anunciarse%20en%20Palette"

// sin imagen: el carrusel le pone un degradado de la marca
private val CARTEL_DE_EJEMPLO = Anuncio(
    id = "ejemplo-cartel",
    hueco = Hueco.CARTEL,
    anunciante = "Vuestra marca",
    titulo = "Vuestro posible anuncio",
    texto = "Lo primero que ve cada persona al abrir Descubrir: vuestro estreno, con imagen, texto y enlace.",
    boton = "Hablemos",
    enlace = CONTACTO_POR_CORREO,
    ejemplo = true,
)
