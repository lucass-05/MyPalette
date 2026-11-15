package com.example.palette.features.auth.domain.repository

import com.example.palette.core.data.Stage
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** en que punto esta quien abre la app: sin sesion, cargando, sin perfil todavia o dentro. */
    fun etapa(): Stage
    fun observarEtapa(): Flow<Stage>

    // las funciones que hablan con el servidor devuelven null si ha ido bien
    // o el motivo del fallo, ya redactado para enseñarlo
    suspend fun iniciarSesion(email: String, password: String): String?
    suspend fun crearCuenta(email: String, password: String): String?
    fun cerrarSesion()
    suspend fun borrarCuenta(): String?

    /** baja del servidor lo que haya cambiado: logs de amigos, mensajes, seguidores. */
    suspend fun sincronizar()
}
