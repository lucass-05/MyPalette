package com.example.palette.core.data

import com.example.palette.core.model.Comment
import com.example.palette.core.model.Like
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.ME
import com.example.palette.core.model.Notice
import com.example.palette.core.model.NoticeKind

/**
 * los avisos de la pestaña actividad. no se guardan en el servidor: salen de los datos que ya se bajan
 * (quien me sigue, los me gusta y comentarios de mis reseñas, lo que reseñan mis amigos).
 *
 * cada aviso tiene un id fijo, asi el que ya estaba leido sigue leido al volver a calcularlos.
 * los que no son de seguidores y son anteriores a [desde] cuentan como leidos: asi, al estrenar
 * esta funcion, no llega de golpe un aviso por cada cosa antigua.
 *
 * es una funcion pura (solo depende de lo que recibe), por eso se puede probar sin servidor.
 */
fun calcularAvisos(
    misLogs: List<LogEntry>,
    logsDeOtros: List<LogEntry>,
    siguiendo: Set<String>,
    seguidores: List<Pair<String, Long>>,
    meGustas: List<Like>,
    comentarios: List<Comment>,
    anteriores: List<Notice>,
    desde: Long,
): List<Notice> {
    val leidos = anteriores.associate { it.id to it.read }
    fun aviso(id: String, kind: NoticeKind, userId: String, logId: String?, at: Long) =
        Notice(id, kind, userId, logId, at, read = leidos[id] ?: (kind != NoticeKind.FOLLOW && at < desde))

    val idsDeMisLogs = misLogs.map { it.id }.toSet()
    val misContenidos = misLogs.map { it.media.id }.toSet()

    val nuevosSeguidores = seguidores.map { (userId, at) -> aviso("f_$userId", NoticeKind.FOLLOW, userId, null, at) }
    // los me gusta antiguos no tienen fecha (0): de esos no se avisa
    val meGustasRecibidos = meGustas.filter { it.logId in idsDeMisLogs && it.userId != ME && it.createdAt > 0 }
        .map { aviso("l_${it.userId}_${it.logId}", NoticeKind.LIKE, it.userId, it.logId, it.createdAt) }
    val comentariosRecibidos = comentarios.filter { it.logId in idsDeMisLogs && it.userId != ME }
        .map { aviso("c_${it.id}", NoticeKind.COMMENT, it.userId, it.logId, it.createdAt) }
    val coincidencias = logsDeOtros.filter { it.userId in siguiendo && !it.hidden && it.media.id in misContenidos }
        .map { aviso("s_${it.id}", NoticeKind.SAME_MEDIA, it.userId, it.id, it.createdAt) }

    return (nuevosSeguidores + meGustasRecibidos + comentariosRecibidos + coincidencias).sortedByDescending { it.at }
}
