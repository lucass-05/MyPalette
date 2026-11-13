package com.example.palette.data.remote

import com.example.palette.data.Group
import com.example.palette.data.LogEntry
import com.example.palette.data.ME
import com.example.palette.data.Media
import com.example.palette.data.Message
import com.example.palette.data.User
import com.example.palette.data.ahora
import com.example.palette.data.leerGuardado
import com.example.palette.data.escribirGuardado
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// supabase se usa por su api rest, que son peticiones http normales:
//  - /auth/v1/...      cuentas: crear, iniciar sesion, renovar el token
//  - /rest/v1/<tabla>  leer (GET), crear (POST), cambiar (PATCH) y borrar (DELETE) filas
//  - /storage/v1/...   subir ficheros (las fotos)
//  - /functions/v1/... llamar a nuestras funciones de servidor
// cada peticion lleva dos cabeceras: "apikey" (identifica el proyecto) y "Authorization"
// con el token del usuario, que es lo que el servidor usa para saber quien pregunta.
// las clases "...Row" de abajo son el reflejo de las columnas de cada tabla.

// la clave publicable esta pensada para ir dentro de la app: quien puede leer o escribir cada fila
// lo deciden las reglas de la base de datos (supabase/schema.sql), no esta clave.
private const val SUPABASE_URL = "https://unlehgzkissbahwcjedp.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_hISx4izQ9ijDm-EvoN4CJg_6Md1Br4g"

/** una sola conexion para toda la app: la usan el estado (palettestore) y la busqueda de juegos (igdb). */
internal val supabase by lazy { Supabase() }

/** lo que hay en el servidor visto desde mi cuenta. */
internal class Remote(
    val me: User?,
    val people: List<User>,
    val logs: List<LogEntry>,
    val peopleLogs: List<LogEntry>,
    val watchlist: List<Media>,
    val peopleWatchlists: Map<String, List<Media>>,
    val following: Set<String>,
    /** quien me sigue y desde cuando. */
    val followers: List<Pair<String, Long>>,
    val messages: List<Message>,
    val liked: Set<String>,
    val groups: List<Group>,
)

/**
 * cliente de supabase hecho directamente sobre su api rest: cuentas (/auth/v1) y tablas (/rest/v1).
 * no hay nada en tiempo real: la app vuelve a pedir los datos cada pocos segundos.
 */
internal class Supabase {

    /** direccion publica de la foto de perfil de un usuario dentro del almacenamiento. */
    private fun urlAvatar(userId: String) = "$SUPABASE_URL/storage/v1/object/public/avatars/$userId/avatar.jpg"

    @Serializable
    private class Session(val accessToken: String, val refreshToken: String, val userId: String, val expiresAt: Long)

    private var sesion: Session? = try {
        leerGuardado(SESSION_KEY)?.let { json.decodeFromString(Session.serializer(), it) }
    } catch (e: Exception) {
        null
    }
    private val candadoRenovar = Mutex()

    // al escribir filas hay que mandar tambien los valores por defecto (pinned = false, review = ""...)
    private val filas = Json(json) { encodeDefaults = true }

    val conSesion get() = sesion != null
    private val miId get() = sesion?.userId ?: throw ApiException("No hay sesión iniciada")

    // ---- cuentas

    /** devuelve false si la cuenta se ha creado pero falta confirmar el correo para poder entrar. */
    suspend fun crearCuenta(email: String, password: String): Boolean {
        val text = enviar(HttpMethod.Post, "auth/v1/signup", cuerpo = filas.encodeToString(Credentials.serializer(), Credentials(email, password)), authorized = false)
        val concedido = json.decodeFromString(Granted.serializer(), text)
        if (concedido.accessToken == null) return false
        guardarSesion(concedido)
        return true
    }

    suspend fun iniciarSesion(email: String, password: String) {
        val text = enviar(
            HttpMethod.Post, "auth/v1/token", mapOf("grant_type" to "password"),
            cuerpo = filas.encodeToString(Credentials.serializer(), Credentials(email, password)), authorized = false,
        )
        guardarSesion(json.decodeFromString(Granted.serializer(), text))
    }

    fun cerrarSesion() {
        sesion = null
        escribirGuardado(SESSION_KEY, "")
    }

    private fun guardarSesion(concedido: Granted) {
        val token = concedido.accessToken ?: throw ApiException("El servidor no ha devuelto sesión")
        val recibido = Session(token, concedido.refreshToken.orEmpty(), concedido.user?.id ?: sesion?.userId.orEmpty(), ahora() + concedido.expiresIn * 1000)
        sesion = recibido
        escribirGuardado(SESSION_KEY, json.encodeToString(Session.serializer(), recibido))
    }

    /** token de acceso vigente; si esta a punto de caducar se renueva antes. */
    private suspend fun accessToken(): String = candadoRenovar.withLock {
        val current = sesion ?: throw ApiException("No hay sesión iniciada")
        if (ahora() < current.expiresAt - 60_000) return@withLock current.accessToken
        val text = enviar(
            HttpMethod.Post, "auth/v1/token", mapOf("grant_type" to "refresh_token"),
            cuerpo = filas.encodeToString(Refresh.serializer(), Refresh(current.refreshToken)), authorized = false,
        )
        guardarSesion(json.decodeFromString(Granted.serializer(), text))
        sesion!!.accessToken
    }

    // ---- peticiones

    private suspend fun enviar(
        method: HttpMethod,
        ruta: String,
        parametros: Map<String, String> = emptyMap(),
        cuerpo: String? = null,
        prefer: String? = null,
        authorized: Boolean = true,
        content: OutgoingContent? = null,
        guardarFila: Boolean = false,
    ): String {
        val credencial = if (authorized) accessToken() else null
        val respuesta = http.request("$SUPABASE_URL/$ruta") {
            this.method = method
            parametros.forEach { (k, v) -> parameter(k, v) }
            header("apikey", SUPABASE_KEY)
            if (credencial != null) header("Authorization", "Bearer $credencial")
            if (prefer != null) header("Prefer", prefer)
            if (guardarFila) header("x-upsert", "true")
            if (cuerpo != null) setBody(TextContent(cuerpo, ContentType.Application.Json))
            if (content != null) setBody(content)
        }
        val text = respuesta.bodyAsText()
        if (!respuesta.status.isSuccess()) throw ApiException(explicar(text, respuesta.status.value))
        return text
    }

    private fun explicar(cuerpo: String, status: Int): String {
        val problema = try {
            json.decodeFromString(Problem.serializer(), cuerpo)
        } catch (e: Exception) {
            Problem()
        }
        return when (problema.errorCode ?: problema.code) {
            "invalid_credentials" -> "Correo o contraseña incorrectos."
            "user_already_exists", "email_exists" -> "Ya hay una cuenta con ese correo. Prueba a iniciar sesión."
            "weak_password" -> "La contraseña es demasiado corta: mínimo 6 caracteres."
            "email_not_confirmed" -> "Te hemos mandado un correo: confírmalo y vuelve a entrar."
            "email_address_invalid", "validation_failed" -> "Ese correo no parece válido."
            "over_email_send_rate_limit", "over_request_rate_limit" -> "Demasiados intentos seguidos. Espera un momento."
            "23505" -> "Ese nombre de usuario ya está cogido."
            "23514" -> "Algún dato no es válido. El usuario lleva de 2 a 30 letras sin acento, números, punto o guion bajo."
            else -> problema.msg ?: problema.message ?: problema.errorDescription ?: "Error $status del servidor."
        }
    }

    private suspend fun <T> leerTabla(tabla: String, fila: KSerializer<T>, parametros: Map<String, String> = emptyMap()): List<T> =
        json.decodeFromString(ListSerializer(fila), enviar(HttpMethod.Get, "rest/v1/$tabla", mapOf("select" to "*") + parametros))

    private suspend fun guardarFila(tabla: String, cuerpo: String, conflicto: String) {
        enviar(HttpMethod.Post, "rest/v1/$tabla", mapOf("on_conflict" to conflicto), cuerpo, prefer = "resolution=merge-duplicates,return=minimal")
    }

    private suspend fun borrar(tabla: String, filtros: Map<String, String>) {
        enviar(HttpMethod.Delete, "rest/v1/$tabla", filtros)
    }

    // ---- lectura

    /** baja todo lo que mi cuenta puede ver. con pocos usuarios es mas simple y fiable que ir pidiendo trozos. */
    suspend fun bajarTodo(): Remote = coroutineScope {
        val perfiles = async { leerTabla("profiles", ProfileRow.serializer()) }
        val logs = async { leerTabla("logs", LogRow.serializer(), mapOf("order" to "created_at.desc", "limit" to "2000")) }
        val pendientes = async { leerTabla("watchlist", WatchRow.serializer(), mapOf("order" to "created_at.desc")) }
        val seguidos = async { leerTabla("follows", FollowRow.serializer()) }
        val messages = async { leerTabla("messages", MessageRow.serializer(), mapOf("order" to "sent_at.asc")) }
        val meGustas = async { leerTabla("likes", LikeRow.serializer(), mapOf("user_id" to "eq.$miId")) }
        val groups = async { leerTabla("groups", GroupRow.serializer()) }
        val members = async { leerTabla("group_members", MemberRow.serializer()) }

        val usuarios = perfiles.await().map { it.toUser() }
        val todosLosLogs = logs.await().map { it.toLog() }
        val todosPendientes = pendientes.await()
        val todosSeguidos = seguidos.await()
        Remote(
            me = usuarios.firstOrNull { it.id == ME },
            people = usuarios.filter { it.id != ME },
            logs = todosLosLogs.filter { it.userId == ME },
            peopleLogs = todosLosLogs.filter { it.userId != ME },
            watchlist = todosPendientes.filter { it.userId == miId }.map { it.media },
            peopleWatchlists = todosPendientes.filter { it.userId != miId }.groupBy({ it.userId }, { it.media }),
            following = todosSeguidos.filter { it.followerId == miId }.map { it.followedId }.toSet(),
            followers = todosSeguidos.filter { it.followedId == miId }.map { it.followerId to it.createdAt },
            messages = messages.await().map { it.toMessage() },
            liked = meGustas.await().map { it.logId }.toSet(),
            groups = members.await().groupBy({ it.groupId }, { idLocal(it.userId) }).let { porGrupo ->
                groups.await().map { Group(it.id, it.name, it.photoUrl, idLocal(it.ownerId), porGrupo[it.id].orEmpty(), it.createdAt) }
            },
        )
    }

    suspend fun bajarMensajes(): List<Message> =
        leerTabla("messages", MessageRow.serializer(), mapOf("order" to "sent_at.asc")).map { it.toMessage() }

    // ---- escritura

    suspend fun guardarPerfil(user: User) {
        val fila = ProfileRow(miId, user.username, user.name, user.bio, user.favorite, user.photoUrl, user.watchlistPublic, user.background)
        guardarFila("profiles", filas.encodeToString(ProfileRow.serializer(), fila), "id")
    }

    suspend fun guardarLog(log: LogEntry) {
        val fila = LogRow(log.id, miId, log.media.id, log.media, log.stars, log.review, log.pinned, log.hidden, log.createdAt)
        guardarFila("logs", filas.encodeToString(LogRow.serializer(), fila), "id")
    }

    suspend fun borrarLog(id: String) = borrar("logs", mapOf("id" to "eq.$id"))

    suspend fun ponerPendiente(media: Media, guardado: Boolean) {
        if (guardado) guardarFila("watchlist", filas.encodeToString(WatchRow.serializer(), WatchRow(miId, media.id, media, ahora())), "user_id,media_id")
        else borrar("watchlist", mapOf("user_id" to "eq.$miId", "media_id" to "eq.${media.id}"))
    }

    suspend fun ponerSeguido(userId: String, following: Boolean) {
        if (following) guardarFila("follows", filas.encodeToString(FollowRow.serializer(), FollowRow(miId, userId, ahora())), "follower_id,followed_id")
        else borrar("follows", mapOf("follower_id" to "eq.$miId", "followed_id" to "eq.$userId"))
    }

    suspend fun ponerMeGusta(logId: String, liked: Boolean) {
        if (liked) guardarFila("likes", filas.encodeToString(LikeRow.serializer(), LikeRow(miId, logId)), "user_id,log_id")
        else borrar("likes", mapOf("user_id" to "eq.$miId", "log_id" to "eq.$logId"))
    }

    suspend fun enviarMensaje(message: Message) {
        // la reseña adjunta viaja con mi id real para que quien la reciba sepa de quien es
        val log = message.log?.let { if (it.userId == ME) it.copy(userId = miId) else it }
        val fila = MessageRow(message.id, miId, message.toId.takeIf { message.groupId == null }, message.text, message.media, log, false, message.sentAt, message.groupId)
        enviar(HttpMethod.Post, "rest/v1/messages", cuerpo = filas.encodeToString(MessageRow.serializer(), fila), prefer = "return=minimal")
    }

    suspend fun marcarLeido(fromId: String) {
        enviar(
            HttpMethod.Patch, "rest/v1/messages",
            mapOf("from_id" to "eq.$fromId", "to_id" to "eq.$miId", "read" to "is.false"),
            cuerpo = "{\"read\":true}", prefer = "return=minimal",
        )
    }

    /** sube la foto de perfil (un jpeg ya recortado) y devuelve su direccion publica. */
    suspend fun subirAvatar(jpeg: ByteArray): String {
        enviar(
            HttpMethod.Post, "storage/v1/object/avatars/$miId/avatar.jpg",
            content = ByteArrayContent(jpeg, ContentType.Image.JPEG), guardarFila = true,
        )
        // el fichero siempre se llama igual; el ?v= obliga a las apps a volver a bajarlo cuando cambia
        return urlAvatar(miId) + "?v=" + ahora()
    }

    // ---- grupos

    /** crea el grupo y mete a sus miembros (yo incluido). */
    suspend fun crearGrupo(group: Group) {
        val fila = GroupRow(group.id, group.name, group.photoUrl, miId, group.createdAt)
        enviar(HttpMethod.Post, "rest/v1/groups", cuerpo = filas.encodeToString(GroupRow.serializer(), fila), prefer = "return=minimal")
        val people = group.members.map { MemberRow(group.id, if (it == ME) miId else it) }
        enviar(HttpMethod.Post, "rest/v1/group_members", cuerpo = filas.encodeToString(ListSerializer(MemberRow.serializer()), people), prefer = "return=minimal")
    }

    /** sube la foto de un grupo (jpeg ya recortado), la deja puesta y devuelve su direccion. */
    suspend fun ponerFotoDeGrupo(groupId: String, jpeg: ByteArray): String {
        enviar(HttpMethod.Post, "storage/v1/object/avatars/$miId/g_$groupId.jpg", content = ByteArrayContent(jpeg, ContentType.Image.JPEG), guardarFila = true)
        val url = "$SUPABASE_URL/storage/v1/object/public/avatars/$miId/g_$groupId.jpg?v=" + ahora()
        enviar(HttpMethod.Patch, "rest/v1/groups", mapOf("id" to "eq.$groupId"), cuerpo = filas.encodeToString(PhotoPatch.serializer(), PhotoPatch(url)), prefer = "return=minimal")
        return url
    }

    suspend fun salirDeGrupo(groupId: String) = borrar("group_members", mapOf("group_id" to "eq.$groupId", "user_id" to "eq.$miId"))

    /** solo quien lo creo. con el grupo se borran sus mensajes. */
    suspend fun borrarGrupo(groupId: String) = borrar("groups", mapOf("id" to "eq.$groupId"))

    /**
     * borra la cuenta entera: la foto, y con la cuenta caen en cascada perfil, logs, pendientes,
     * seguidos, mensajes y me gusta. no se puede deshacer.
     */
    suspend fun borrarCuenta() {
        try {
            enviar(HttpMethod.Delete, "storage/v1/object/avatars/$miId/avatar.jpg")
        } catch (e: ApiException) {
            // no tenia foto
        }
        enviar(HttpMethod.Post, "rest/v1/rpc/delete_my_account", cuerpo = "{}")
        cerrarSesion()
    }

    /** llama a una funcion de servidor (supabase/functions) con la sesion del usuario y devuelve su respuesta. */
    suspend fun llamarFuncion(function: String, cuerpo: String): String =
        enviar(HttpMethod.Post, "functions/v1/$function", content = TextContent(cuerpo, ContentType.Text.Plain))

    // ---- filas de las tablas

    private fun idLocal(id: String) = if (id == sesion?.userId) ME else id

    private fun ProfileRow.toUser() = User(idLocal(id), username, name, bio, favorite, photoUrl, watchlistPublic, background)

    private fun LogRow.toLog() = LogEntry(id, idLocal(userId), media, stars, review, createdAt, pinned, hidden)

    private fun MessageRow.toMessage() =
        Message(id, idLocal(fromId), toId?.let(::idLocal).orEmpty(), text, media, log?.let { it.copy(userId = idLocal(it.userId)) }, sentAt, read, groupId)

    @Serializable
    private class Credentials(val email: String, val password: String)

    @Serializable
    private class Refresh(@SerialName("refresh_token") val refreshToken: String)

    @Serializable
    private class Granted(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long = 3600,
        val user: Account? = null,
    )

    @Serializable
    private class Account(val id: String = "")

    @Serializable
    private class Problem(
        val code: String? = null,
        @SerialName("error_code") val errorCode: String? = null,
        val msg: String? = null,
        val message: String? = null,
        @SerialName("error_description") val errorDescription: String? = null,
    )

    @Serializable
    private class ProfileRow(
        val id: String,
        val username: String,
        val name: String,
        val bio: String = "",
        val favorite: Long = 0xFF2D5BF0,
        @SerialName("photo_url") val photoUrl: String? = null,
        @SerialName("watchlist_public") val watchlistPublic: Boolean = true,
        val background: Long? = null,
    )

    @Serializable
    private class LogRow(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("media_id") val mediaId: String,
        val media: Media,
        val stars: Float,
        val review: String = "",
        val pinned: Boolean = false,
        val hidden: Boolean = false,
        @SerialName("created_at") val createdAt: Long = 0,
    )

    @Serializable
    private class WatchRow(
        @SerialName("user_id") val userId: String,
        @SerialName("media_id") val mediaId: String,
        val media: Media,
        @SerialName("created_at") val createdAt: Long = 0,
    )

    @Serializable
    private class FollowRow(
        @SerialName("follower_id") val followerId: String,
        @SerialName("followed_id") val followedId: String,
        @SerialName("created_at") val createdAt: Long = 0,
    )

    @Serializable
    private class MessageRow(
        val id: String,
        @SerialName("from_id") val fromId: String,
        @SerialName("to_id") val toId: String? = null,
        val text: String = "",
        val media: Media? = null,
        val log: LogEntry? = null,
        val read: Boolean = false,
        @SerialName("sent_at") val sentAt: Long = 0,
        @SerialName("group_id") val groupId: String? = null,
    )

    @Serializable
    private class GroupRow(
        val id: String,
        val name: String,
        @SerialName("photo_url") val photoUrl: String? = null,
        @SerialName("owner_id") val ownerId: String,
        @SerialName("created_at") val createdAt: Long = 0,
    )

    @Serializable
    private class MemberRow(@SerialName("group_id") val groupId: String, @SerialName("user_id") val userId: String)

    @Serializable
    private class PhotoPatch(@SerialName("photo_url") val photoUrl: String)

    @Serializable
    private class LikeRow(@SerialName("user_id") val userId: String, @SerialName("log_id") val logId: String)

    private companion object {
        const val SESSION_KEY = "palette.session"
    }
}
