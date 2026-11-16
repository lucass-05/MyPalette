package com.example.palette.features.auth.domain.repository

import com.example.palette.core.data.EnlaceDeCorreo
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

    // crear la cuenta no da sesion: el servidor manda un correo con un enlace y hay que pulsarlo.
    // ese enlace, y el de contraseña olvidada, abren la web de la app
    fun enlaceDeCorreo(): EnlaceDeCorreo
    suspend fun pedirRecuperacion(email: String): String?

    /** entra con el enlace del correo. si era el de contraseña olvidada, guarda ademas la nueva. */
    suspend fun entrarConEnlace(nueva: String?): String?

    fun cerrarSesion()
    suspend fun borrarCuenta(): String?

    /** baja del servidor lo que haya cambiado: logs de amigos, mensajes, seguidores. */
    suspend fun sincronizar()
}
