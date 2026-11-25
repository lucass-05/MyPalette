package com.example.palette.features.anuncios.data.repository

import com.example.palette.core.util.ahora
import com.example.palette.features.anuncios.domain.model.Anuncio
import com.example.palette.features.anuncios.domain.repository.AnunciosRepository

class AnunciosRepositoryImpl : AnunciosRepository {
    override fun vigentes(): List<Anuncio> {
        val hoy = ahora()
        return ANUNCIOS_CONTRATADOS.filter { (it.desde ?: 0) <= hoy && hoy <= (it.hasta ?: Long.MAX_VALUE) }
    }
}
