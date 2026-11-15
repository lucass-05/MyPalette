package com.example.palette.features.profile.domain.repository

import com.example.palette.core.model.ThemeColors
import com.example.palette.core.model.User
import com.example.palette.features.profile.domain.model.Perfil
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {

    /** el perfil de esa persona (o el mio). emite null si no existe. */
    fun observarPerfil(userId: String): Flow<Perfil?>

    fun yo(): User
    fun observarYo(): Flow<User>

    // devuelven null si ha ido bien o el motivo del fallo
    suspend fun guardarPerfil(name: String, username: String, bio: String, favorite: Long, background: Long?): String?

    /** sube una foto de perfil ya recortada (jpeg). con null se quita la que hubiera. */
    suspend fun ponerFoto(jpeg: ByteArray?): String?

    fun ponerPendientesPublicos(publicos: Boolean)

    // los colores de la app son de este dispositivo, no de la cuenta
    fun tema(): ThemeColors
    fun observarTema(): Flow<ThemeColors>
    fun ponerTema(theme: ThemeColors)
}
