package com.example.palette.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.palette.data.remote.Supabase
import com.example.palette.data.remote.supabase
import com.example.palette.data.remote.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** en que punto esta quien abre la app. */
enum class Stage { SIGNED_OUT, LOADING, NEEDS_PROFILE, READY }

/**
 * estado de la app: mi perfil, mis logs, a quien sigo, mensajes...
 *
 * las pantallas solo leen de aqui y llaman a estas funciones. cada cambio se aplica primero en
 * local, para que la pantalla responda al momento, y despues se manda a supabase. [sync] baja lo
 * que haya cambiado en el servidor (logs de amigos, mensajes nuevos, seguidores).
 */
class PaletteStore {

    private val remote = supabase
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // las subidas y las bajadas van de una en una y en orden, para que una bajada no pise un cambio a medio subir
    private val queue = Mutex()

    var data by mutableStateOf(load())
        private set

    var signedIn by mutableStateOf(remote.signedIn)
        private set

    private var synced by mutableStateOf(false)

    /** true cuando el ultimo intento de hablar con el servidor ha fallado. */
    var offline by mutableStateOf(false)
        private set

    val stage: Stage
        get() = when {
            !signedIn -> Stage.SIGNED_OUT
            data.me.username.isNotBlank() -> Stage.READY
            synced -> Stage.NEEDS_PROFILE
            else -> Stage.LOADING
        }

    private fun load(): Snapshot =
        try {
            readStored(KEY)?.let { json.decodeFromString(Snapshot.serializer(), it) } ?: Snapshot()
        } catch (e: Exception) {
            Snapshot()
        }

    private fun update(change: (Snapshot) -> Snapshot) {
        data = change(data)
        try {
            writeStored(KEY, json.encodeToString(Snapshot.serializer(), data))
        } catch (e: Exception) {
            println("Palette: no se pudo guardar (${e.message})")
        }
    }

    /** manda un cambio al servidor sin hacer esperar a la pantalla. */
    private fun push(change: suspend Supabase.() -> Unit) {
        scope.launch {
            queue.withLock {
                try {
                    remote.change()
                    offline = false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    offline = true
                    println("Palette: no se pudo subir un cambio (${e.message})")
                }
            }
        }
    }

    // ---- cuenta

    /** devuelve null si ha ido bien o el motivo, ya redactado para enseñarlo. */
    suspend fun signIn(email: String, password: String): String? = attempt {
        remote.signIn(email.trim(), password)
        signedIn = true
        sync()
        null
    }

    suspend fun signUp(email: String, password: String): String? = attempt {
        if (remote.signUp(email.trim(), password)) {
            signedIn = true
            sync()
            null
        } else {
            "Cuenta creada. Te hemos mandado un correo: confírmalo y después inicia sesión."
        }
    }

    fun signOut() {
        remote.signOut()
        signedIn = false
        synced = false
        // los colores de la app son del dispositivo, no de la cuenta: se quedan
        update { Snapshot(theme = it.theme) }
    }

    /** borra la cuenta y todo lo suyo del servidor, y deja la app como recien instalada (salvo los colores). */
    suspend fun deleteAccount(): String? = attempt {
        queue.withLock { remote.deleteAccount() }
        signedIn = false
        synced = false
        update { Snapshot(theme = it.theme) }
        null
    }

    /** guarda nombre, usuario y demas. espera al servidor porque el nombre de usuario puede estar cogido. */
    suspend fun saveProfile(name: String, username: String, bio: String, favorite: Long, background: Long?): String? = attempt {
        val profile = data.me.copy(
            name = name.trim(),
            username = cleanUsername(username),
            bio = bio.trim(),
            favorite = favorite,
            background = background,
        )
        queue.withLock { remote.saveProfile(profile) }
        update { it.copy(me = profile) }
        null
    }

    /** sube una foto de perfil ya recortada (jpeg) y la deja puesta. con null se quita la que hubiera. */
    suspend fun setPhoto(jpeg: ByteArray?): String? = attempt {
        queue.withLock {
            val url = jpeg?.let { remote.uploadAvatar(it) }
            val profile = data.me.copy(photoUrl = url)
            remote.saveProfile(profile)
            update { it.copy(me = profile) }
        }
        null
    }

    private suspend fun attempt(block: suspend () -> String?): String? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "No se ha podido conectar. Revisa la conexión."
        }

    /** baja del servidor todo lo que ve mi cuenta y lo deja como estado de la app. */
    suspend fun sync() {
        if (!signedIn) return
        try {
            queue.withLock {
                val fresh = remote.fetchAll()
                update { s ->
                    val known = s.notices.associateBy { it.id }
                    s.copy(
                        me = fresh.me ?: s.me,
                        people = fresh.people,
                        logs = fresh.logs,
                        peopleLogs = fresh.peopleLogs,
                        watchlist = fresh.watchlist,
                        peopleWatchlists = fresh.peopleWatchlists,
                        following = fresh.following,
                        messages = fresh.messages,
                        liked = fresh.liked,
                        groups = fresh.groups,
                        // un aviso por seguidor; se conserva si ya lo habia leido
                        notices = fresh.followers.map { (userId, at) ->
                            val id = "f_$userId"
                            Notice(id, NoticeKind.FOLLOW, userId, at = at, read = known[id]?.read ?: false)
                        },
                    )
                }
            }
            synced = true
            offline = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            synced = true
            offline = true
            println("Palette: no se pudo sincronizar (${e.message})")
        }
    }

    /** solo los mensajes: lo usa el chat abierto para refrescar mas a menudo. */
    suspend fun syncMessages() {
        if (!signedIn) return
        try {
            queue.withLock {
                val fresh = remote.fetchMessages()
                if (fresh != data.messages) update { it.copy(messages = fresh) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Palette: no se pudieron bajar los mensajes (${e.message})")
        }
    }

    // ---- gente

    val me get() = data.me

    fun user(id: String): User? = if (id == ME) data.me else data.people.firstOrNull { it.id == id }

    fun isFollowing(id: String) = id in data.following

    val friends: List<User> get() = data.people.filter { it.id in data.following }

    fun toggleFollow(id: String) {
        val follow = id !in data.following
        update { it.copy(following = if (follow) it.following + id else it.following - id) }
        push { setFollowing(id, follow) }
    }

    fun searchPeople(query: String): List<User> {
        val q = query.trim().removePrefix("@")
        if (q.isEmpty()) return data.people.filter { it.id !in data.following }
        return data.people.filter { it.username.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
    }

    fun setTheme(theme: ThemeColors) = update { it.copy(theme = theme) }

    fun setWatchlistPublic(public: Boolean) {
        update { it.copy(me = it.me.copy(watchlistPublic = public)) }
        val profile = data.me
        push { saveProfile(profile) }
    }

    // ---- logs

    /** logs de un perfil tal y como se enseñan: fijados primero y luego de mas a menos estrellas. */
    fun profileLogs(userId: String, includeHidden: Boolean = false, recentFirst: Boolean = false): List<LogEntry> {
        val all = if (userId == ME) data.logs else data.peopleLogs.filter { it.userId == userId }
        val shown = all.filter { includeHidden || !it.hidden }
        // como diario: lo ultimo registrado arriba, sin tener en cuenta fijados ni nota
        return if (recentFirst) shown.sortedByDescending { it.createdAt }
        else shown.sortedWith(compareByDescending<LogEntry> { it.pinned }.thenByDescending { it.stars }.thenByDescending { it.createdAt })
    }

    fun myLogOf(mediaId: String): LogEntry? = data.logs.firstOrNull { it.media.id == mediaId }

    fun saveLog(media: Media, stars: Float, review: String) {
        val existing = myLogOf(media.id)
        val entry = existing?.copy(stars = stars, review = review.trim(), media = media)
            ?: LogEntry(newId("l"), ME, media, stars, review.trim(), now())
        val wasPending = inWatchlist(media.id)
        update { s ->
            s.copy(
                logs = listOf(entry) + s.logs.filter { it.id != entry.id },
                // si estaba pendiente, ya no lo esta
                watchlist = s.watchlist.filter { it.id != media.id },
            )
        }
        push {
            saveLog(entry)
            if (wasPending) setWatchlist(media, saved = false)
        }
    }

    fun deleteLog(id: String) {
        update { s -> s.copy(logs = s.logs.filter { it.id != id }) }
        push { deleteLog(id) }
    }

    fun togglePinned(id: String) = editLog(id) { it.copy(pinned = !it.pinned, hidden = false) }

    fun toggleHidden(id: String) = editLog(id) { it.copy(hidden = !it.hidden, pinned = false) }

    private fun editLog(id: String, change: (LogEntry) -> LogEntry) {
        val edited = data.logs.firstOrNull { it.id == id }?.let(change) ?: return
        update { s -> s.copy(logs = s.logs.map { if (it.id == id) edited else it }) }
        push { saveLog(edited) }
    }

    // ---- pendientes

    fun inWatchlist(mediaId: String) = data.watchlist.any { it.id == mediaId }

    fun toggleWatchlist(media: Media) {
        val save = !inWatchlist(media.id)
        update { s ->
            if (save) s.copy(watchlist = listOf(media) + s.watchlist)
            else s.copy(watchlist = s.watchlist.filter { it.id != media.id })
        }
        push { setWatchlist(media, save) }
    }

    /** null si esa persona tiene sus pendientes en privado. */
    fun watchlistOf(userId: String): List<Media>? = when {
        userId == ME -> data.watchlist
        user(userId)?.watchlistPublic == true -> data.peopleWatchlists[userId].orEmpty()
        else -> null
    }

    // ---- inicio

    /** lo ultimo que ha registrado la gente a la que sigo, lo mas nuevo arriba. */
    val feed: List<LogEntry>
        get() = data.peopleLogs.filter { it.userId in data.following && !it.hidden }.sortedByDescending { it.createdAt }

    /** logs de un amigo de los ultimos dias, en orden, para verlos como historia. */
    fun storyOf(userId: String): List<LogEntry> =
        data.peopleLogs.filter { it.userId == userId && !it.hidden && now() - it.createdAt < STORY_WINDOW }.sortedBy { it.createdAt }

    fun hasUnseenStory(userId: String): Boolean {
        val last = storyOf(userId).lastOrNull() ?: return false
        return last.createdAt > (data.seenStories[userId] ?: 0)
    }

    /** amigos con historia: primero los que tienen algo sin ver. */
    val stories: List<User>
        get() = friends.filter { storyOf(it.id).isNotEmpty() }
            .sortedWith(compareByDescending<User> { hasUnseenStory(it.id) }.thenByDescending { storyOf(it.id).last().createdAt })

    fun markStorySeen(userId: String, upTo: Long) = update { s ->
        if ((s.seenStories[userId] ?: 0) >= upTo) s else s.copy(seenStories = s.seenStories + (userId to upTo))
    }

    fun isLiked(logId: String) = logId in data.liked

    fun toggleLike(logId: String) {
        val like = logId !in data.liked
        update { s -> s.copy(liked = if (like) s.liked + logId else s.liked - logId) }
        push { setLiked(logId, like) }
    }

    // ---- mensajes

    /** una conversacion de la lista: con una persona o con un grupo. */
    class Thread(val user: User?, val group: Group?, val last: Message?, val unread: Int) {
        val id get() = group?.let { "g_${it.id}" } ?: "u_${user?.id}"
        val at get() = last?.sentAt ?: group?.createdAt ?: 0
    }

    val threads: List<Thread>
        get() {
            val direct = data.messages.filter { it.groupId == null }
                .groupBy { if (it.fromId == ME) it.toId else it.fromId }
                .mapNotNull { (otherId, list) ->
                    val other = user(otherId) ?: return@mapNotNull null
                    Thread(other, null, list.maxBy { it.sentAt }, list.count { it.fromId != ME && !it.read })
                }
            val grouped = data.groups.map { group ->
                val list = data.messages.filter { it.groupId == group.id }
                Thread(null, group, list.maxByOrNull { it.sentAt }, unreadIn(group.id))
            }
            return (direct + grouped).sortedByDescending { it.at }
        }

    fun conversation(userId: String): List<Message> =
        data.messages.filter { it.groupId == null && (it.fromId == userId || it.toId == userId) }.sortedBy { it.sentAt }

    fun send(toId: String, text: String = "", media: Media? = null, log: LogEntry? = null) {
        if (text.isBlank() && media == null && log == null) return
        val message = Message(newId("m"), ME, toId, text.trim(), media, log, now(), read = false)
        update { s -> s.copy(messages = s.messages + message) }
        push { sendMessage(message) }
    }

    fun markRead(userId: String) {
        if (data.messages.none { it.groupId == null && it.fromId == userId && !it.read }) return
        update { s -> s.copy(messages = s.messages.map { if (it.groupId == null && it.fromId == userId && !it.read) it.copy(read = true) else it }) }
        push { markRead(userId) }
    }

    // ---- grupos

    fun group(id: String): Group? = data.groups.firstOrNull { it.id == id }

    fun groupConversation(groupId: String): List<Message> = data.messages.filter { it.groupId == groupId }.sortedBy { it.sentAt }

    private fun unreadIn(groupId: String): Int {
        val seen = data.seenGroups[groupId] ?: 0
        return data.messages.count { it.groupId == groupId && it.fromId != ME && it.sentAt > seen }
    }

    fun markGroupSeen(groupId: String) {
        val latest = data.messages.filter { it.groupId == groupId }.maxOfOrNull { it.sentAt } ?: return
        if ((data.seenGroups[groupId] ?: 0) >= latest) return
        update { it.copy(seenGroups = it.seenGroups + (groupId to latest)) }
    }

    fun sendToGroup(groupId: String, text: String = "", media: Media? = null, log: LogEntry? = null) {
        if (text.isBlank() && media == null && log == null) return
        val message = Message(newId("m"), ME, "", text.trim(), media, log, now(), groupId = groupId)
        update { s -> s.copy(messages = s.messages + message, seenGroups = s.seenGroups + (groupId to message.sentAt)) }
        push { sendMessage(message) }
    }

    /** crea un grupo conmigo y con [memberIds]. la foto, si hay, es un jpeg ya recortado. devuelve el motivo si falla. */
    suspend fun createGroup(name: String, memberIds: Set<String>, photo: ByteArray?): String? = attempt {
        val group = Group(newId("g"), name.trim().take(60), null, ME, listOf(ME) + memberIds, now())
        queue.withLock {
            remote.createGroup(group)
            val url = photo?.let { remote.setGroupPhoto(group.id, it) }
            update { it.copy(groups = it.groups + group.copy(photoUrl = url)) }
        }
        null
    }

    suspend fun setGroupPhoto(groupId: String, photo: ByteArray): String? = attempt {
        queue.withLock {
            val url = remote.setGroupPhoto(groupId, photo)
            update { s -> s.copy(groups = s.groups.map { if (it.id == groupId) it.copy(photoUrl = url) else it }) }
        }
        null
    }

    /** salir de un grupo o, si lo cree yo, borrarlo para todos. */
    fun leaveGroup(groupId: String) {
        val mine = group(groupId)?.ownerId == ME
        update { s -> s.copy(groups = s.groups.filter { it.id != groupId }, messages = s.messages.filter { it.groupId != groupId }) }
        push { if (mine) deleteGroup(groupId) else leaveGroup(groupId) }
    }

    val notices: List<Notice> get() = data.notices.sortedByDescending { it.at }

    fun markNoticesRead() {
        if (data.notices.none { !it.read }) return
        update { s -> s.copy(notices = s.notices.map { it.copy(read = true) }) }
    }

    val unreadCount: Int
        get() = data.messages.count { it.groupId == null && it.fromId != ME && !it.read } +
            data.groups.sumOf { unreadIn(it.id) } +
            data.notices.count { !it.read }

    companion object {
        /** deja un nombre de usuario como lo admite el servidor: minusculas sin acento, numeros, punto y guion bajo. */
        fun cleanUsername(text: String): String =
            text.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }.take(30)

        private const val KEY = "palette.v1"
        private const val STORY_WINDOW = 4 * DAY
    }
}
