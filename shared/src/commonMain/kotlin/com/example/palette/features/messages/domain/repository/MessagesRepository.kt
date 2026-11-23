package com.example.palette.features.messages.domain.repository

import com.example.palette.core.model.Group
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.features.messages.domain.model.Aviso
import com.example.palette.features.messages.domain.model.Conversacion
import com.example.palette.features.messages.domain.model.Hilo
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {

    fun observarHilos(): Flow<List<Hilo>>
    fun observarAvisos(): Flow<List<Aviso>>
    fun observarGrupos(): Flow<List<Group>>

    /** cuantas cosas hay sin leer entre mensajes y avisos: es el punto de la barra. */
    fun observarSinLeer(): Flow<Int>

    // emiten null si esa persona o ese grupo ya no existen
    fun observarChat(userId: String): Flow<Conversacion?>
    fun observarGrupo(groupId: String): Flow<Conversacion?>

    fun enviar(userId: String, text: String = "", media: Media? = null, log: LogEntry? = null)
    fun enviarAGrupo(groupId: String, text: String = "", media: Media? = null, log: LogEntry? = null)

    fun marcarLeido(userId: String)
    fun marcarGrupoVisto(groupId: String)
    fun marcarAvisosLeidos()

    /** baja solo los mensajes. lo usa el chat abierto para refrescar mas a menudo. */
    suspend fun refrescar()

    // devuelven null si ha ido bien o el motivo del fallo
    suspend fun crearGrupo(name: String, idsMiembros: Set<String>, foto: ByteArray?): String?
    suspend fun ponerFotoDeGrupo(groupId: String, foto: ByteArray): String?

    // cualquier miembro puede cambiar el nombre y meter gente; sacar a alguien solo quien lo creo
    fun renombrarGrupo(groupId: String, name: String)
    fun anadirAGrupo(groupId: String, ids: Set<String>)
    fun quitarDeGrupo(groupId: String, userId: String)

    /** salir de un grupo o, si lo cree yo, borrarlo para todos. */
    fun salirDeGrupo(groupId: String)
}
