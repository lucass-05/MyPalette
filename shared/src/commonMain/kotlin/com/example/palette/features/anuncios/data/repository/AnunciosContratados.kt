package com.example.palette.features.anuncios.data.repository

import com.example.palette.features.anuncios.domain.model.Anuncio

/**
 * los anuncios que se han vendido. para poner uno se añade aqui y se publica la app
 * (y Monetizacion.ANUNCIOS_ACTIVOS tiene que estar a true).
 *
 * cuando haya varios anunciantes compensa pasarlo a una tabla de supabase y leerlo de ahi, para
 * poder cambiarlos sin volver a publicar. el resto de la app no cambiaria: solo este repositorio.
 *
 * ejemplo:
 *
 *     Anuncio(
 *         id = "estreno-diciembre",
 *         hueco = Hueco.CARTEL,
 *         anunciante = "Nombre de la productora",
 *         titulo = "Título del estreno",
 *         texto = "En cines el 12 de diciembre",
 *         boton = "Ver tráiler",
 *         enlace = "https://...",
 *         imagenUrl = "https://.../cartel-ancho.jpg",
 *         desde = 1_796_000_000_000, // milisegundos
 *         hasta = 1_797_000_000_000,
 *     )
 */
internal val ANUNCIOS_CONTRATADOS: List<Anuncio> = emptyList()
