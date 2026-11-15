package com.example.palette.core.model

import com.example.palette.core.network.json


import kotlinx.serialization.Serializable

// los nombres de los campos de estas clases no estan traducidos a proposito: son las claves
// con las que se guardan en json (en el dispositivo y en la base de datos). si se cambian,
// lo que ya esta guardado dejaria de leerse.

enum class MediaType(val label: String, val plural: String) {
    MOVIE("Película", "Pelis"),
    SERIES("Serie", "Series"),
    GAME("Videojuego", "Juegos"),
    ALBUM("Álbum", "Álbumes"),
    SONG("Canción", "Canciones"),
    BOOK("Libro", "Libros"),
}

/**
 * un contenido de cualquier base de datos externa ya normalizado.
 * el id lleva el origen delante ("tmdb:movie:157336", "igdb:1020", "itunes:album:123", "ol:ol45804w")
 * para poder volver a pedir sus detalles sin guardar nada mas.
 */
@Serializable
data class Media(
    val id: String,
    val type: MediaType,
    val title: String,
    val creator: String? = null,
    val year: Int? = null,
    val imageUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    // nota de la base de datos, siempre sobre 10
    val score: Double? = null,
    val scoreSource: String? = null,
    val votes: Int? = null,
    val genres: List<String> = emptyList(),
    val extra: String? = null,
    val link: String? = null,
)

@Serializable
data class LogEntry(
    val id: String,
    val userId: String,
    val media: Media,
    val stars: Float,
    val review: String = "",
    val createdAt: Long,
    val pinned: Boolean = false,
    val hidden: Boolean = false,
)

@Serializable
data class User(
    val id: String,
    val username: String,
    val name: String,
    val bio: String = "",
    // color favorito, en argb (0xffrrggbb). tiñe su avatar y el anillo de sus historias
    val favorite: Long = 0xFF2D5BF0,
    val photoUrl: String? = null,
    val watchlistPublic: Boolean = true,
    // color de fondo de su perfil, igual para quien lo visite. null: el fondo que tenga cada uno en su app
    val background: Long? = null,
)

@Serializable
data class Message(
    val id: String,
    val fromId: String,
    val toId: String,
    val text: String = "",
    val media: Media? = null,
    val log: LogEntry? = null,
    val sentAt: Long,
    val read: Boolean = false,
    // en un mensaje de grupo va el grupo y toid queda vacio
    val groupId: String? = null,
)

@Serializable
data class Group(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val ownerId: String,
    val members: List<String> = emptyList(),
    val createdAt: Long = 0,
)

/** datos de la ficha que dependen del tipo: reparto de una peli, canciones de un disco, plataformas de un juego... */
class Extras(
    val datos: List<Pair<String, String>> = emptyList(),
    /** canciones de un album, en orden. */
    val canciones: List<Media> = emptyList(),
    /** album al que pertenece una cancion. */
    val album: Media? = null,
)

enum class NoticeKind { FOLLOW }

@Serializable
data class Notice(
    val id: String,
    val kind: NoticeKind,
    val userId: String,
    val logId: String? = null,
    val at: Long,
    val read: Boolean = false,
)

/**
 * colores de la app elegidos por el usuario, en argb. los grises (lineas, textos secundarios,
 * fondos de campos) no se guardan: salen de mezclar fondo y texto.
 */
@Serializable
data class ThemeColors(
    val paper: Long = 0xFFFFFFFF,
    val ink: Long = 0xFF17171A,
    val accent: Long = 0xFF2D5BF0,
    val movie: Long = 0xFFF2473A,
    val series: Long = 0xFF2D5BF0,
    val game: Long = 0xFF12A679,
    val album: Long = 0xFFFFB81F,
    val song: Long = 0xFF8B4BDB,
    val book: Long = 0xFFE8418F,
)

/**
 * copia en el dispositivo de lo ultimo que se bajo del servidor, mas lo que solo es local
 * (colores de la app, historias ya vistas). asi la app abre al instante y sin conexion enseña lo que tenia.
 */
@Serializable
data class Snapshot(
    val me: User = User(id = ME, username = "", name = ""),
    val theme: ThemeColors = ThemeColors(),
    val logs: List<LogEntry> = emptyList(),
    val watchlist: List<Media> = emptyList(),
    val following: Set<String> = emptySet(),
    val messages: List<Message> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val liked: Set<String> = emptySet(),
    val seenStories: Map<String, Long> = emptyMap(),
    val groups: List<Group> = emptyList(),
    // hasta cuando he leido cada grupo (en los grupos no hay "leido" por mensaje)
    val seenGroups: Map<String, Long> = emptyMap(),
    val people: List<User> = emptyList(),
    val peopleLogs: List<LogEntry> = emptyList(),
    val peopleWatchlists: Map<String, List<Media>> = emptyMap(),
)

/** mi propio id dentro de la app. en el servidor es un uuid; se traduce al subir y al bajar datos. */
const val ME = "me"
