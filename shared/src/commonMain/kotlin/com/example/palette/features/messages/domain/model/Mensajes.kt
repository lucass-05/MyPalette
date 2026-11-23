package com.example.palette.features.messages.domain.model

import com.example.palette.core.model.Group
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.Message
import com.example.palette.core.model.NoticeKind
import com.example.palette.core.model.User

// una conversacion de la lista: con una persona (user) o con un grupo (group)
data class Hilo(
    val id: String,
    val user: User?,
    val group: Group?,
    val ultimo: Message?,
    // en los grupos, quien escribio el ultimo mensaje
    val autorDelUltimo: User?,
    val sinLeer: Int,
    val at: Long,
)

// algo que ha pasado y me afecta: un seguidor nuevo, un me gusta o un comentario en una reseña mia,
// o un amigo que ha reseñado algo que yo tambien. log es la reseña de la que va (null en los seguidores)
data class Aviso(
    val id: String,
    val kind: NoticeKind,
    val user: User,
    val at: Long,
    val leido: Boolean,
    val loSigo: Boolean,
    val log: LogEntry? = null,
    // el texto del comentario, en los avisos de comentarios
    val comentario: String? = null,
)

// un chat abierto. autores lleva a los participantes por id, para poner nombre a cada mensaje
data class Conversacion(
    val user: User? = null,
    val group: Group? = null,
    val mensajes: List<Message> = emptyList(),
    val autores: Map<String, User> = emptyMap(),
)

/** lo que se esta mandando a un amigo. con [Nothing] solo se elige con quien abrir un chat. */
sealed interface Outgoing {
    data class Content(val media: Media) : Outgoing
    data class Review(val log: LogEntry) : Outgoing
    data object Nothing : Outgoing
}
