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

    // crear la cuenta no da sesion: el servidor manda un codigo al correo y hay que confirmarlo
    suspend fun confirmarCorreo(email: String, codigo: String): String?
    suspend fun reenviarCodigo(email: String): String?

    // contraseña olvidada: se pide un codigo y con el se guarda una contraseña nueva
    suspend fun pedirRecuperacion(email: String): String?
    suspend fun cambiarContrasena(email: String, codigo: String, nueva: String): String?

    fun cerrarSesion()
    suspend fun borrarCuenta(): String?

    /** baja del servidor lo que haya cambiado: logs de amigos, mensajes, seguidores. */
    suspend fun sincronizar()
}
