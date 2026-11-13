package com.example.palette.data.remote

import com.example.palette.data.Group
import com.example.palette.data.LogEntry
import com.example.palette.data.ME
import com.example.palette.data.Media
import com.example.palette.data.Message
import com.example.palette.data.User
import com.example.palette.data.now
import com.example.palette.data.readStored
import com.example.palette.data.writeStored
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
    private fun avatarUrl(userId: String) = "$SUPABASE_URL/storage/v1/object/public/avatars/$userId/avatar.jpg"

    @Serializable
    private class Session(val accessToken: String, val refreshToken: String, val userId: String, val expiresAt: Long)

    private var session: Session? = try {
        readStored(SESSION_KEY)?.let { json.decodeFromString(Session.serializer(), it) }
    } catch (e: Exception) {
        null
    }
    private val refreshLock = Mutex()

    // al escribir filas hay que mandar tambien los valores por defecto (pinned = false, review = ""...)
    private val rows = Json(json) { encodeDefaults = true }

    val signedIn get() = session != null
    private val myId get() = session?.userId ?: throw ApiException("No hay sesión iniciada")

    // ---- cuentas

    /** devuelve false si la cuenta se ha creado pero falta confirmar el correo para poder entrar. */
    suspend fun signUp(email: String, password: String): Boolean {
        val text = send(HttpMethod.Post, "auth/v1/signup", body = rows.encodeToString(Credentials.serializer(), Credentials(email, password)), authorized = false)
        val granted = json.decodeFromString(Granted.serializer(), text)
        if (granted.accessToken == null) return false
        keep(granted)
        return true
    }

    suspend fun signIn(email: String, password: String) {
        val text = send(
            HttpMethod.Post, "auth/v1/token", mapOf("grant_type" to "password"),
            body = rows.encodeToString(Credentials.serializer(), Credentials(email, password)), authorized = false,
        )
        keep(json.decodeFromString(Granted.serializer(), text))
    }

    fun signOut() {
        session = null
        writeStored(SESSION_KEY, "")
    }

    private fun keep(granted: Granted) {
        val token = granted.accessToken ?: throw ApiException("El servidor no ha devuelto sesión")
        val fresh = Session(token, granted.refreshToken.orEmpty(), granted.user?.id ?: session?.userId.orEmpty(), now() + granted.expiresIn * 1000)
        session = fresh
        writeStored(SESSION_KEY, json.encodeToString(Session.serializer(), fresh))
    }

    /** token de acceso vigente; si esta a punto de caducar se renueva antes. */
    private suspend fun accessToken(): String = refreshLock.withLock {
        val current = session ?: throw ApiException("No hay sesión iniciada")
        if (now() < current.expiresAt - 60_000) return@withLock current.accessToken
        val text = send(
            HttpMethod.Post, "auth/v1/token", mapOf("grant_type" to "refresh_token"),
            body = rows.encodeToString(Refresh.serializer(), Refresh(current.refreshToken)), authorized = false,
        )
        keep(json.decodeFromString(Granted.serializer(), text))
        session!!.accessToken
    }

    // ---- peticiones

    private suspend fun send(
        method: HttpMethod,
        path: String,
        params: Map<String, String> = emptyMap(),
        body: String? = null,
        prefer: String? = null,
        authorized: Boolean = true,
        content: OutgoingContent? = null,
        upsert: Boolean = false,
    ): String {
        val bearer = if (authorized) accessToken() else null
        val response = http.request("$SUPABASE_URL/$path") {
            this.method = method
            params.forEach { (k, v) -> parameter(k, v) }
            header("apikey", SUPABASE_KEY)
            if (bearer != null) header("Authorization", "Bearer $bearer")
            if (prefer != null) header("Prefer", prefer)
            if (upsert) header("x-upsert", "true")
            if (body != null) setBody(TextContent(body, ContentType.Application.Json))
            if (content != null) setBody(content)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw ApiException(explain(text, response.status.value))
        return text
    }

    private fun explain(body: String, status: Int): String {
        val problem = try {
            json.decodeFromString(Problem.serializer(), body)
        } catch (e: Exception) {
            Problem()
        }
        return when (problem.errorCode ?: problem.code) {
            "invalid_credentials" -> "Correo o contraseña incorrectos."
            "user_already_exists", "email_exists" -> "Ya hay una cuenta con ese correo. Prueba a iniciar sesión."
            "weak_password" -> "La contraseña es demasiado corta: mínimo 6 caracteres."
            "email_not_confirmed" -> "Te hemos mandado un correo: confírmalo y vuelve a entrar."
            "email_address_invalid", "validation_failed" -> "Ese correo no parece válido."
            "over_email_send_rate_limit", "over_request_rate_limit" -> "Demasiados intentos seguidos. Espera un momento."
            "23505" -> "Ese nombre de usuario ya está cogido."
            "23514" -> "Algún dato no es válido. El usuario lleva de 2 a 30 letras sin acento, números, punto o guion bajo."
            else -> problem.msg ?: problem.message ?: problem.errorDescription ?: "Error $status del servidor."
        }
    }

    private suspend fun <T> list(table: String, row: KSerializer<T>, params: Map<String, String> = emptyMap()): List<T> =
        json.decodeFromString(ListSerializer(row), send(HttpMethod.Get, "rest/v1/$table", mapOf("select" to "*") + params))

    private suspend fun upsert(table: String, body: String, conflict: String) {
        send(HttpMethod.Post, "rest/v1/$table", mapOf("on_conflict" to conflict), body, prefer = "resolution=merge-duplicates,return=minimal")
    }

    private suspend fun delete(table: String, filters: Map<String, String>) {
        send(HttpMethod.Delete, "rest/v1/$table", filters)
    }

    // ---- lectura

    /** baja todo lo que mi cuenta puede ver. con pocos usuarios es mas simple y fiable que ir pidiendo trozos. */
    suspend fun fetchAll(): Remote = coroutineScope {
        val profiles = async { list("profiles", ProfileRow.serializer()) }
        val logs = async { list("logs", LogRow.serializer(), mapOf("order" to "created_at.desc", "limit" to "2000")) }
        val watch = async { list("watchlist", WatchRow.serializer(), mapOf("order" to "created_at.desc")) }
        val follows = async { list("follows", FollowRow.serializer()) }
        val messages = async { list("messages", MessageRow.serializer(), mapOf("order" to "sent_at.asc")) }
        val likes = async { list("likes", LikeRow.serializer(), mapOf("user_id" to "eq.$myId")) }
        val groups = async { list("groups", GroupRow.serializer()) }
        val members = async { list("group_members", MemberRow.serializer()) }

        val users = profiles.await().map { it.toUser() }
        val allLogs = logs.await().map { it.toLog() }
        val allWatch = watch.await()
        val allFollows = follows.await()
        Remote(
            me = users.firstOrNull { it.id == ME },
            people = users.filter { it.id != ME },
            logs = allLogs.filter { it.userId == ME },
            peopleLogs = allLogs.filter { it.userId != ME },
            watchlist = allWatch.filter { it.userId == myId }.map { it.media },
            peopleWatchlists = allWatch.filter { it.userId != myId }.groupBy({ it.userId }, { it.media }),
            following = allFollows.filter { it.followerId == myId }.map { it.followedId }.toSet(),
            followers = allFollows.filter { it.followedId == myId }.map { it.followerId to it.createdAt },
            messages = messages.await().map { it.toMessage() },
            liked = likes.await().map { it.logId }.toSet(),
            groups = members.await().groupBy({ it.groupId }, { local(it.userId) }).let { byGroup ->
                groups.await().map { Group(it.id, it.name, it.photoUrl, local(it.ownerId), byGroup[it.id].orEmpty(), it.createdAt) }
            },
        )
    }

    suspend fun fetchMessages(): List<Message> =
        list("messages", MessageRow.serializer(), mapOf("order" to "sent_at.asc")).map { it.toMessage() }

    // ---- escritura

    suspend fun saveProfile(user: User) {
        val row = ProfileRow(myId, user.username, user.name, user.bio, user.favorite, user.photoUrl, user.watchlistPublic, user.background)
        upsert("profiles", rows.encodeToString(ProfileRow.serializer(), row), "id")
    }

    suspend fun saveLog(log: LogEntry) {
        val row = LogRow(log.id, myId, log.media.id, log.media, log.stars, log.review, log.pinned, log.hidden, log.createdAt)
        upsert("logs", rows.encodeToString(LogRow.serializer(), row), "id")
    }

    suspend fun deleteLog(id: String) = delete("logs", mapOf("id" to "eq.$id"))

    suspend fun setWatchlist(media: Media, saved: Boolean) {
        if (saved) upsert("watchlist", rows.encodeToString(WatchRow.serializer(), WatchRow(myId, media.id, media, now())), "user_id,media_id")
        else delete("watchlist", mapOf("user_id" to "eq.$myId", "media_id" to "eq.${media.id}"))
    }

    suspend fun setFollowing(userId: String, following: Boolean) {
        if (following) upsert("follows", rows.encodeToString(FollowRow.serializer(), FollowRow(myId, userId, now())), "follower_id,followed_id")
        else delete("follows", mapOf("follower_id" to "eq.$myId", "followed_id" to "eq.$userId"))
    }

    suspend fun setLiked(logId: String, liked: Boolean) {
        if (liked) upsert("likes", rows.encodeToString(LikeRow.serializer(), LikeRow(myId, logId)), "user_id,log_id")
        else delete("likes", mapOf("user_id" to "eq.$myId", "log_id" to "eq.$logId"))
    }

    suspend fun sendMessage(message: Message) {
        // la reseña adjunta viaja con mi id real para que quien la reciba sepa de quien es
        val log = message.log?.let { if (it.userId == ME) it.copy(userId = myId) else it }
        val row = MessageRow(message.id, myId, message.toId.takeIf { message.groupId == null }, message.text, message.media, log, false, message.sentAt, message.groupId)
        send(HttpMethod.Post, "rest/v1/messages", body = rows.encodeToString(MessageRow.serializer(), row), prefer = "return=minimal")
    }

    suspend fun markRead(fromId: String) {
        send(
            HttpMethod.Patch, "rest/v1/messages",
            mapOf("from_id" to "eq.$fromId", "to_id" to "eq.$myId", "read" to "is.false"),
            body = "{\"read\":true}", prefer = "return=minimal",
        )
    }

    /** sube la foto de perfil (un jpeg ya recortado) y devuelve su direccion publica. */
    suspend fun uploadAvatar(jpeg: ByteArray): String {
        send(
            HttpMethod.Post, "storage/v1/object/avatars/$myId/avatar.jpg",
            content = ByteArrayContent(jpeg, ContentType.Image.JPEG), upsert = true,
        )
        // el fichero siempre se llama igual; el ?v= obliga a las apps a volver a bajarlo cuando cambia
        return avatarUrl(myId) + "?v=" + now()
    }

    // ---- grupos

    /** crea el grupo y mete a sus miembros (yo incluido). */
    suspend fun createGroup(group: Group) {
        val row = GroupRow(group.id, group.name, group.photoUrl, myId, group.createdAt)
        send(HttpMethod.Post, "rest/v1/groups", body = rows.encodeToString(GroupRow.serializer(), row), prefer = "return=minimal")
        val people = group.members.map { MemberRow(group.id, if (it == ME) myId else it) }
        send(HttpMethod.Post, "rest/v1/group_members", body = rows.encodeToString(ListSerializer(MemberRow.serializer()), people), prefer = "return=minimal")
    }

    /** sube la foto de un grupo (jpeg ya recortado), la deja puesta y devuelve su direccion. */
    suspend fun setGroupPhoto(groupId: String, jpeg: ByteArray): String {
        send(HttpMethod.Post, "storage/v1/object/avatars/$myId/g_$groupId.jpg", content = ByteArrayContent(jpeg, ContentType.Image.JPEG), upsert = true)
        val url = "$SUPABASE_URL/storage/v1/object/public/avatars/$myId/g_$groupId.jpg?v=" + now()
        send(HttpMethod.Patch, "rest/v1/groups", mapOf("id" to "eq.$groupId"), body = rows.encodeToString(PhotoPatch.serializer(), PhotoPatch(url)), prefer = "return=minimal")
        return url
    }

    suspend fun leaveGroup(groupId: String) = delete("group_members", mapOf("group_id" to "eq.$groupId", "user_id" to "eq.$myId"))

    /** solo quien lo creo. con el grupo se borran sus mensajes. */
    suspend fun deleteGroup(groupId: String) = delete("groups", mapOf("id" to "eq.$groupId"))

    /**
     * borra la cuenta entera: la foto, y con la cuenta caen en cascada perfil, logs, pendientes,
     * seguidos, mensajes y me gusta. no se puede deshacer.
     */
    suspend fun deleteAccount() {
        try {
            send(HttpMethod.Delete, "storage/v1/object/avatars/$myId/avatar.jpg")
        } catch (e: ApiException) {
            // no tenia foto
        }
        send(HttpMethod.Post, "rest/v1/rpc/delete_my_account", body = "{}")
        signOut()
    }

    /** llama a una funcion de servidor (supabase/functions) con la sesion del usuario y devuelve su respuesta. */
    suspend fun call(function: String, body: String): String =
        send(HttpMethod.Post, "functions/v1/$function", content = TextContent(body, ContentType.Text.Plain))

    // ---- filas de las tablas

    private fun local(id: String) = if (id == session?.userId) ME else id

    private fun ProfileRow.toUser() = User(local(id), username, name, bio, favorite, photoUrl, watchlistPublic, background)

    private fun LogRow.toLog() = LogEntry(id, local(userId), media, stars, review, createdAt, pinned, hidden)

    private fun MessageRow.toMessage() =
        Message(id, local(fromId), toId?.let(::local).orEmpty(), text, media, log?.let { it.copy(userId = local(it.userId)) }, sentAt, read, groupId)

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
