package com.example.palette.features.auth.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.core.data.Stage
import com.example.palette.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(private val almacen: PaletteStore) : AuthRepository {

    override fun etapa(): Stage = almacen.etapa

    // snapshotFlow convierte estado de compose en un Flow: vuelve a emitir cada vez que
    // cambia algo de lo que se lee dentro del bloque
    override fun observarEtapa(): Flow<Stage> = snapshotFlow { almacen.etapa }

    override suspend fun iniciarSesion(email: String, password: String): String? = almacen.iniciarSesion(email, password)

    override suspend fun crearCuenta(email: String, password: String): String? = almacen.crearCuenta(email, password)

    override fun cerrarSesion() = almacen.cerrarSesion()

    override suspend fun borrarCuenta(): String? = almacen.borrarCuenta()

    override suspend fun sincronizar() = almacen.sincronizar()
}
