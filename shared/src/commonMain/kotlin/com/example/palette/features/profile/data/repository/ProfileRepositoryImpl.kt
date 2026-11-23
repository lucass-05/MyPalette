package com.example.palette.features.profile.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.ME
import com.example.palette.core.model.ProfileLayout
import com.example.palette.core.model.ThemeColors
import com.example.palette.core.model.User
import com.example.palette.features.profile.domain.model.Perfil
import com.example.palette.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

class ProfileRepositoryImpl(private val almacen: PaletteStore) : ProfileRepository {

    override fun observarPerfil(userId: String): Flow<Perfil?> = snapshotFlow {
        almacen.user(userId)?.let { user ->
            val propio = userId == ME
            val logs = if (propio) almacen.estado.logs else almacen.estado.peopleLogs.filter { it.userId == userId && !it.hidden }
            Perfil(user, propio, logs, almacen.pendientesDe(userId), almacen.sigoA(userId))
        }
    }

    override fun yo(): User = almacen.me

    override fun observarYo(): Flow<User> = snapshotFlow { almacen.me }

    override suspend fun guardarPerfil(name: String, username: String, bio: String, favorite: Long, background: Long?): String? =
        almacen.guardarPerfil(name, username, bio, favorite, background)

    override suspend fun ponerFoto(jpeg: ByteArray?): String? = almacen.ponerFoto(jpeg)

    override fun ponerPendientesPublicos(publicos: Boolean) = almacen.ponerPendientesPublicos(publicos)

    override fun guardarDiseno(layout: ProfileLayout, background: Long?, pendientesPublicos: Boolean) =
        almacen.guardarDiseno(layout, background, pendientesPublicos)

    override suspend fun ponerBanner(jpeg: ByteArray?): String? = almacen.ponerBanner(jpeg)

    override fun tema(): ThemeColors = almacen.estado.theme

    override fun observarTema(): Flow<ThemeColors> = snapshotFlow { almacen.estado.theme }

    override fun ponerTema(theme: ThemeColors) = almacen.ponerTema(theme)
}
