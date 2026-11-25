package com.example.palette.features.anuncios.domain.repository

import com.example.palette.features.anuncios.domain.model.Anuncio

interface AnunciosRepository {
    /** los anuncios contratados que estan en fecha. */
    fun vigentes(): List<Anuncio>
}
