package com.example.palette.di

import com.example.palette.core.data.PaletteStore
import com.example.palette.features.catalog.data.repository.CatalogRepositoryImpl
import com.example.palette.features.catalog.domain.repository.CatalogRepository
import com.example.palette.features.catalog.domain.usecases.SearchMediaUseCase

interface AppContainer {
    val catalogRepository: CatalogRepository
    val searchMediaUseCase: SearchMediaUseCase

    // PENDIENTE: el almacen sigue siendo una sola clase para todo lo que no es catalogo.
    // se ira partiendo en un repositorio por feature (auth, home, messages, profile).
    val almacen: PaletteStore
}

// crea las instancias una sola vez y las reparte: es el unico sitio donde se decide
// que implementacion hay detras de cada interfaz
class DefaultAppContainer : AppContainer {

    // by lazy: no se crea hasta la primera vez que alguien lo pide, y despues se reutiliza

    override val catalogRepository: CatalogRepository by lazy { CatalogRepositoryImpl() }

    override val searchMediaUseCase: SearchMediaUseCase by lazy { SearchMediaUseCase(catalogRepository) }

    override val almacen: PaletteStore by lazy { PaletteStore() }
}
