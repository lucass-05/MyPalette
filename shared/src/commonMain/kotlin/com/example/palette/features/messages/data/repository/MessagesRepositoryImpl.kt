package com.example.palette.features.messages.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.Group
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.NoticeKind
import com.example.palette.features.messages.domain.model.Aviso
import com.example.palette.features.messages.domain.model.Conversacion
import com.example.palette.features.messages.domain.model.Hilo
import com.example.palette.features.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.flow.Flow

class MessagesRepositoryImpl(private val almacen: PaletteStore) : MessagesRepository {

    override fun observarHilos(): Flow<List<Hilo>> = snapshotFlow {
        almacen.hilos.map { hilo ->
            // en la lista, el ultimo mensaje de un grupo lleva delante quien lo escribio
            val autor = if (hilo.group != null) hilo.last?.let { almacen.user(it.fromId) } else null
            Hilo(hilo.id, hilo.user, hilo.group, hilo.last, autor, hilo.unread, hilo.at)
        }
    }

    override fun observarAvisos(): Flow<List<Aviso>> = snapshotFlow {
        almacen.notices.mapNotNull { aviso ->
            val user = almacen.user(aviso.userId) ?: return@mapNotNull null
            val log = aviso.logId?.let { almacen.log(it) }
            // si la reseña de la que hablaba ya no existe, el aviso tampoco se enseña
            if (aviso.kind != NoticeKind.FOLLOW && log == null) return@mapNotNull null
            val comentario = if (aviso.kind == NoticeKind.COMMENT) almacen.estado.comments.firstOrNull { "c_${it.id}" == aviso.id }?.text else null
            Aviso(aviso.id, aviso.kind, user, aviso.at, aviso.read, almacen.sigoA(user.id), log, comentario)
        }
    }

    override fun observarGrupos(): Flow<List<Group>> = snapshotFlow { almacen.estado.groups }

    override fun observarSinLeer(): Flow<Int> = snapshotFlow { almacen.sinLeer }

    override fun observarChat(userId: String): Flow<Conversacion?> = snapshotFlow {
        almacen.user(userId)?.let { Conversacion(user = it, mensajes = almacen.mensajesCon(userId)) }
    }

    override fun observarGrupo(groupId: String): Flow<Conversacion?> = snapshotFlow {
        almacen.group(groupId)?.let { group ->
            Conversacion(
                group = group,
                mensajes = almacen.mensajesDeGrupo(groupId),
                autores = group.members.mapNotNull { almacen.user(it) }.associateBy { it.id },
            )
        }
    }

    override fun enviar(userId: String, text: String, media: Media?, log: LogEntry?) = almacen.enviar(userId, text, media, log)

    override fun enviarAGrupo(groupId: String, text: String, media: Media?, log: LogEntry?) = almacen.enviarAGrupo(groupId, text, media, log)

    override fun marcarLeido(userId: String) = almacen.marcarLeido(userId)

    override fun marcarGrupoVisto(groupId: String) = almacen.marcarGrupoVisto(groupId)

    override fun marcarAvisosLeidos() = almacen.marcarAvisosLeidos()

    override suspend fun refrescar() = almacen.sincronizarMensajes()

    override suspend fun crearGrupo(name: String, idsMiembros: Set<String>, foto: ByteArray?): String? = almacen.crearGrupo(name, idsMiembros, foto)

    override suspend fun ponerFotoDeGrupo(groupId: String, foto: ByteArray): String? = almacen.ponerFotoDeGrupo(groupId, foto)

    override fun renombrarGrupo(groupId: String, name: String) = almacen.renombrarGrupo(groupId, name)

    override fun anadirAGrupo(groupId: String, ids: Set<String>) = almacen.anadirAGrupo(groupId, ids)

    override fun quitarDeGrupo(groupId: String, userId: String) = almacen.quitarDeGrupo(groupId, userId)

    override fun salirDeGrupo(groupId: String) = almacen.salirDeGrupo(groupId)
}
