package com.example.palette.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.palette.App
import com.example.palette.core.data.PaletteStore
import com.example.palette.features.auth.data.repository.AuthRepositoryImpl
import com.example.palette.features.auth.domain.repository.AuthRepository
import com.example.palette.features.catalog.data.repository.CatalogRepositoryImpl
import com.example.palette.features.catalog.domain.repository.CatalogRepository
import com.example.palette.features.catalog.domain.usecases.GetMediaDetailUseCase
import com.example.palette.features.catalog.domain.usecases.GetRecommendationsUseCase
import com.example.palette.features.catalog.domain.usecases.SearchMediaUseCase
import com.example.palette.features.home.data.repository.FeedRepositoryImpl
import com.example.palette.features.home.domain.repository.FeedRepository
import com.example.palette.features.library.data.repository.LibraryRepositoryImpl
import com.example.palette.features.library.domain.repository.LibraryRepository
import com.example.palette.features.messages.data.repository.MessagesRepositoryImpl
import com.example.palette.features.messages.domain.repository.MessagesRepository
import com.example.palette.features.people.data.repository.PeopleRepositoryImpl
import com.example.palette.features.people.domain.repository.PeopleRepository
import com.example.palette.features.people.domain.usecases.SearchPeopleUseCase
import com.example.palette.features.profile.data.repository.ProfileRepositoryImpl
import com.example.palette.features.profile.domain.repository.ProfileRepository
import com.example.palette.features.profile.domain.usecases.SortProfileLogsUseCase

// inyeccion de dependencias hecha a mano: el contenedor sabe construir cada repositorio y cada
// caso de uso, y las pantallas se los piden a el en vez de crearlos ellas.
// las pantallas solo conocen las interfaces (AuthRepository, CatalogRepository...); que detras
// haya supabase, tmdb o datos de prueba se decide aqui y en ningun otro sitio.

interface AppContainer {
    // un repositorio por feature
    val authRepository: AuthRepository
    val catalogRepository: CatalogRepository
    val libraryRepository: LibraryRepository
    val feedRepository: FeedRepository
    val peopleRepository: PeopleRepository
    val messagesRepository: MessagesRepository
    val profileRepository: ProfileRepository

    // casos de uso: solo donde hay una regla que no es ni de pantalla ni de datos
    val searchMediaUseCase: SearchMediaUseCase
    val getMediaDetailUseCase: GetMediaDetailUseCase
    val getRecommendationsUseCase: GetRecommendationsUseCase
    val searchPeopleUseCase: SearchPeopleUseCase
    val sortProfileLogsUseCase: SortProfileLogsUseCase
}

class DefaultAppContainer : AppContainer {

    // la fuente de datos (copia local + supabase) es una sola y la comparten todos los repositorios.
    // es privada: fuera del contenedor nadie puede saltarse los repositorios
    private val almacen: PaletteStore by lazy { PaletteStore() }

    // by lazy: cada cosa se crea la primera vez que alguien la pide y despues se reutiliza
    override val authRepository: AuthRepository by lazy { AuthRepositoryImpl(almacen) }
    override val catalogRepository: CatalogRepository by lazy { CatalogRepositoryImpl() }
    override val libraryRepository: LibraryRepository by lazy { LibraryRepositoryImpl(almacen) }
    override val feedRepository: FeedRepository by lazy { FeedRepositoryImpl(almacen) }
    override val peopleRepository: PeopleRepository by lazy { PeopleRepositoryImpl(almacen) }
    override val messagesRepository: MessagesRepository by lazy { MessagesRepositoryImpl(almacen) }
    override val profileRepository: ProfileRepository by lazy { ProfileRepositoryImpl(almacen) }

    override val searchMediaUseCase: SearchMediaUseCase by lazy { SearchMediaUseCase(catalogRepository) }
    override val getMediaDetailUseCase: GetMediaDetailUseCase by lazy { GetMediaDetailUseCase(catalogRepository) }
    override val getRecommendationsUseCase: GetRecommendationsUseCase by lazy { GetRecommendationsUseCase(catalogRepository) }
    override val searchPeopleUseCase: SearchPeopleUseCase by lazy { SearchPeopleUseCase() }
    override val sortProfileLogsUseCase: SortProfileLogsUseCase by lazy { SortProfileLogsUseCase() }
}

/** el contenedor, al alcance de cualquier pantalla. lo pone App al arrancar. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("falta el AppContainer: lo pone App()") }
