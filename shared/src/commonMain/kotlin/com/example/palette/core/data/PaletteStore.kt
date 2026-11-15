package com.example.palette.core.data

import com.example.palette.core.model.Group
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.model.Message
import com.example.palette.core.model.Notice
import com.example.palette.core.model.NoticeKind
import com.example.palette.core.model.Snapshot
import com.example.palette.core.model.ThemeColors
import com.example.palette.core.model.User
import com.example.palette.core.network.Supabase
import com.example.palette.core.network.json
import com.example.palette.core.network.supabase
import com.example.palette.core.storage.escribirGuardado
import com.example.palette.core.storage.leerGuardado
import com.example.palette.core.util.DAY
import com.example.palette.core.util.ahora
import com.example.palette.core.util.nuevoId
import com.example.palette.navigation.Palette


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// idea general de este fichero:
//  1. todo el estado de la app esta en una sola variable, "estado" (un Snapshot inmutable)
//  2. para cambiar algo no se modifica: se crea una copia con .copy(...) dentro de actualizar { }
//  3. como "estado" es un mutableStateOf, las pantallas que lo leen se repintan solas
//  4. el cambio se ve al instante en local y despues se sube al servidor con subir { }
//     (a esto se le llama actualizacion optimista: no se espera a que el servidor conteste)

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

    private val servidor = supabase
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // las subidas y las bajadas van de una en una y en orden, para que una bajada no pise un cambio a medio subir
    private val cola = Mutex()

    var estado by mutableStateOf(cargar())
        private set

    var conSesion by mutableStateOf(servidor.conSesion)
        private set

    private var sincronizado by mutableStateOf(false)

    /** true cuando el ultimo intento de hablar con el servidor ha fallado. */
    var sinConexion by mutableStateOf(false)
        private set

    val etapa: Stage
        get() = when {
            !conSesion -> Stage.SIGNED_OUT
            estado.me.username.isNotBlank() -> Stage.READY
            sincronizado -> Stage.NEEDS_PROFILE
            else -> Stage.LOADING
        }

    private fun cargar(): Snapshot =
        try {
            leerGuardado(KEY)?.let { json.decodeFromString(Snapshot.serializer(), it) } ?: Snapshot()
        } catch (e: Exception) {
            Snapshot()
        }

    private fun actualizar(cambio: (Snapshot) -> Snapshot) {
        estado = cambio(estado)
        try {
            escribirGuardado(KEY, json.encodeToString(Snapshot.serializer(), estado))
        } catch (e: Exception) {
            println("Palette: no se pudo guardar (${e.message})")
        }
    }

    /** manda un cambio al servidor sin hacer esperar a la pantalla. */
    private fun subir(cambio: suspend Supabase.() -> Unit) {
        ambito.launch {
            cola.withLock {
                try {
                    servidor.cambio()
                    sinConexion = false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sinConexion = true
                    println("Palette: no se pudo subir un cambio (${e.message})")
                }
            }
        }
    }

    // ---- cuenta

    /** devuelve null si ha ido bien o el motivo, ya redactado para enseñarlo. */
    suspend fun iniciarSesion(email: String, password: String): String? = intentar {
        servidor.iniciarSesion(email.trim(), password)
        conSesion = true
        sincronizar()
        null
    }

    suspend fun crearCuenta(email: String, password: String): String? = intentar {
        if (servidor.crearCuenta(email.trim(), password)) {
            conSesion = true
            sincronizar()
            null
        } else {
            "Cuenta creada. Te hemos mandado un correo: confírmalo y después inicia sesión."
        }
    }

    fun cerrarSesion() {
        servidor.cerrarSesion()
        conSesion = false
        sincronizado = false
        // los colores de la app son del dispositivo, no de la cuenta: se quedan
        actualizar { Snapshot(theme = it.theme) }
    }

    /** borra la cuenta y todo lo suyo del servidor, y deja la app como recien instalada (salvo los colores). */
    suspend fun borrarCuenta(): String? = intentar {
        cola.withLock { servidor.borrarCuenta() }
        conSesion = false
        sincronizado = false
        actualizar { Snapshot(theme = it.theme) }
        null
    }

    /** guarda nombre, usuario y demas. espera al servidor porque el nombre de usuario puede estar cogido. */
    suspend fun guardarPerfil(name: String, username: String, bio: String, favorite: Long, background: Long?): String? = intentar {
        val profile = estado.me.copy(
            name = name.trim(),
            username = limpiarUsuario(username),
            bio = bio.trim(),
            favorite = favorite,
            background = background,
        )
        cola.withLock { servidor.guardarPerfil(profile) }
        actualizar { it.copy(me = profile) }
        null
    }

    /** sube una foto de perfil ya recortada (jpeg) y la deja puesta. con null se quita la que hubiera. */
    suspend fun ponerFoto(jpeg: ByteArray?): String? = intentar {
        cola.withLock {
            val url = jpeg?.let { servidor.subirAvatar(it) }
            val profile = estado.me.copy(photoUrl = url)
            servidor.guardarPerfil(profile)
            actualizar { it.copy(me = profile) }
        }
        null
    }

    private suspend fun intentar(block: suspend () -> String?): String? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "No se ha podido conectar. Revisa la conexión."
        }

    /** baja del servidor todo lo que ve mi cuenta y lo deja como estado de la app. */
    suspend fun sincronizar() {
        if (!conSesion) return
        try {
            cola.withLock {
                val recibido = servidor.bajarTodo()
                actualizar { s ->
                    val conocidos = s.notices.associateBy { it.id }
                    s.copy(
                        me = recibido.me ?: s.me,
                        people = recibido.people,
                        logs = recibido.logs,
                        peopleLogs = recibido.peopleLogs,
                        watchlist = recibido.watchlist,
                        peopleWatchlists = recibido.peopleWatchlists,
                        following = recibido.following,
                        messages = recibido.messages,
                        liked = recibido.liked,
                        groups = recibido.groups,
                        // un aviso por seguidor; se conserva si ya lo habia leido
                        notices = recibido.followers.map { (userId, at) ->
                            val id = "f_$userId"
                            Notice(id, NoticeKind.FOLLOW, userId, at = at, read = conocidos[id]?.read ?: false)
                        },
                    )
                }
            }
            sincronizado = true
            sinConexion = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sincronizado = true
            sinConexion = true
            println("Palette: no se pudo sincronizar (${e.message})")
        }
    }

    /** solo los mensajes: lo usa el chat abierto para refrescar mas a menudo. */
    suspend fun sincronizarMensajes() {
        if (!conSesion) return
        try {
            cola.withLock {
                val recibido = servidor.bajarMensajes()
                if (recibido != estado.messages) actualizar { it.copy(messages = recibido) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Palette: no se pudieron bajar los mensajes (${e.message})")
        }
    }

    // ---- gente

    val me get() = estado.me

    fun user(id: String): User? = if (id == ME) estado.me else estado.people.firstOrNull { it.id == id }

    fun sigoA(id: String) = id in estado.following

    val amigos: List<User> get() = estado.people.filter { it.id in estado.following }

    fun alternarSeguir(id: String) {
        val follow = id !in estado.following
        actualizar { it.copy(following = if (follow) it.following + id else it.following - id) }
        subir { ponerSeguido(id, follow) }
    }

    fun buscarGente(consulta: String): List<User> {
        val q = consulta.trim().removePrefix("@")
        if (q.isEmpty()) return estado.people.filter { it.id !in estado.following }
        return estado.people.filter { it.username.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
    }

    fun ponerTema(theme: ThemeColors) = actualizar { it.copy(theme = theme) }

    fun ponerPendientesPublicos(public: Boolean) {
        actualizar { it.copy(me = it.me.copy(watchlistPublic = public)) }
        val profile = estado.me
        subir { guardarPerfil(profile) }
    }

    // ---- logs

    /** logs de un perfil tal y como se enseñan: fijados primero y luego de mas a menos estrellas. */
    fun logsDePerfil(userId: String, conOcultos: Boolean = false, recientesPrimero: Boolean = false): List<LogEntry> {
        val todos = if (userId == ME) estado.logs else estado.peopleLogs.filter { it.userId == userId }
        val shown = todos.filter { conOcultos || !it.hidden }
        // como diario: lo ultimo registrado arriba, sin tener en cuenta fijados ni nota
        return if (recientesPrimero) shown.sortedByDescending { it.createdAt }
        else shown.sortedWith(compareByDescending<LogEntry> { it.pinned }.thenByDescending { it.stars }.thenByDescending { it.createdAt })
    }

    fun miLogDe(mediaId: String): LogEntry? = estado.logs.firstOrNull { it.media.id == mediaId }

    fun guardarLog(media: Media, stars: Float, review: String) {
        val existente = miLogDe(media.id)
        val entrada = existente?.copy(stars = stars, review = review.trim(), media = media)
            ?: LogEntry(nuevoId("l"), ME, media, stars, review.trim(), ahora())
        val estabaPendiente = enPendientes(media.id)
        actualizar { s ->
            s.copy(
                logs = listOf(entrada) + s.logs.filter { it.id != entrada.id },
                // si estaba pendiente, ya no lo esta
                watchlist = s.watchlist.filter { it.id != media.id },
            )
        }
        subir {
            guardarLog(entrada)
            if (estabaPendiente) ponerPendiente(media, guardado = false)
        }
    }

    fun borrarLog(id: String) {
        actualizar { s -> s.copy(logs = s.logs.filter { it.id != id }) }
        subir { borrarLog(id) }
    }

    fun alternarFijado(id: String) = editarLog(id) { it.copy(pinned = !it.pinned, hidden = false) }

    fun alternarOculto(id: String) = editarLog(id) { it.copy(hidden = !it.hidden, pinned = false) }

    private fun editarLog(id: String, cambio: (LogEntry) -> LogEntry) {
        val editado = estado.logs.firstOrNull { it.id == id }?.let(cambio) ?: return
        actualizar { s -> s.copy(logs = s.logs.map { if (it.id == id) editado else it }) }
        subir { guardarLog(editado) }
    }

    // ---- pendientes

    fun enPendientes(mediaId: String) = estado.watchlist.any { it.id == mediaId }

    fun alternarPendiente(media: Media) {
        val save = !enPendientes(media.id)
        actualizar { s ->
            if (save) s.copy(watchlist = listOf(media) + s.watchlist)
            else s.copy(watchlist = s.watchlist.filter { it.id != media.id })
        }
        subir { ponerPendiente(media, save) }
    }

    /** null si esa persona tiene sus pendientes en privado. */
    fun pendientesDe(userId: String): List<Media>? = when {
        userId == ME -> estado.watchlist
        user(userId)?.watchlistPublic == true -> estado.peopleWatchlists[userId].orEmpty()
        else -> null
    }

    // ---- inicio

    /** lo ultimo que ha registrado la gente a la que sigo, lo mas nuevo arriba. */
    val novedades: List<LogEntry>
        get() = estado.peopleLogs.filter { it.userId in estado.following && !it.hidden }.sortedByDescending { it.createdAt }

    /** logs de un amigo de los ultimos dias, en orden, para verlos como historia. */
    fun historiaDe(userId: String): List<LogEntry> =
        estado.peopleLogs.filter { it.userId == userId && !it.hidden && ahora() - it.createdAt < STORY_WINDOW }.sortedBy { it.createdAt }

    fun tieneHistoriaSinVer(userId: String): Boolean {
        val last = historiaDe(userId).lastOrNull() ?: return false
        return last.createdAt > (estado.seenStories[userId] ?: 0)
    }

    /** amigos con historia: primero los que tienen algo sin ver. */
    val historias: List<User>
        get() = amigos.filter { historiaDe(it.id).isNotEmpty() }
            .sortedWith(compareByDescending<User> { tieneHistoriaSinVer(it.id) }.thenByDescending { historiaDe(it.id).last().createdAt })

    fun marcarHistoriaVista(userId: String, hasta: Long) = actualizar { s ->
        if ((s.seenStories[userId] ?: 0) >= hasta) s else s.copy(seenStories = s.seenStories + (userId to hasta))
    }

    fun meGusta(logId: String) = logId in estado.liked

    fun alternarMeGusta(logId: String) {
        val like = logId !in estado.liked
        actualizar { s -> s.copy(liked = if (like) s.liked + logId else s.liked - logId) }
        subir { ponerMeGusta(logId, like) }
    }

    // ---- mensajes

    /** una conversacion de la lista: con una persona o con un grupo. */
    class Thread(val user: User?, val group: Group?, val last: Message?, val unread: Int) {
        val id get() = group?.let { "g_${it.id}" } ?: "u_${user?.id}"
        val at get() = last?.sentAt ?: group?.createdAt ?: 0
    }

    val hilos: List<Thread>
        get() {
            val directos = estado.messages.filter { it.groupId == null }
                .groupBy { if (it.fromId == ME) it.toId else it.fromId }
                .mapNotNull { (idOtro, leerTabla) ->
                    val otro = user(idOtro) ?: return@mapNotNull null
                    Thread(otro, null, leerTabla.maxBy { it.sentAt }, leerTabla.count { it.fromId != ME && !it.read })
                }
            val deGrupo = estado.groups.map { group ->
                val leerTabla = estado.messages.filter { it.groupId == group.id }
                Thread(null, group, leerTabla.maxByOrNull { it.sentAt }, sinLeerEn(group.id))
            }
            return (directos + deGrupo).sortedByDescending { it.at }
        }

    fun mensajesCon(userId: String): List<Message> =
        estado.messages.filter { it.groupId == null && (it.fromId == userId || it.toId == userId) }.sortedBy { it.sentAt }

    fun enviar(toId: String, text: String = "", media: Media? = null, log: LogEntry? = null) {
        if (text.isBlank() && media == null && log == null) return
        val message = Message(nuevoId("m"), ME, toId, text.trim(), media, log, ahora(), read = false)
        actualizar { s -> s.copy(messages = s.messages + message) }
        subir { enviarMensaje(message) }
    }

    fun marcarLeido(userId: String) {
        if (estado.messages.none { it.groupId == null && it.fromId == userId && !it.read }) return
        actualizar { s -> s.copy(messages = s.messages.map { if (it.groupId == null && it.fromId == userId && !it.read) it.copy(read = true) else it }) }
        subir { marcarLeido(userId) }
    }

    // ---- grupos

    fun group(id: String): Group? = estado.groups.firstOrNull { it.id == id }

    fun mensajesDeGrupo(groupId: String): List<Message> = estado.messages.filter { it.groupId == groupId }.sortedBy { it.sentAt }

    private fun sinLeerEn(groupId: String): Int {
        val seen = estado.seenGroups[groupId] ?: 0
        return estado.messages.count { it.groupId == groupId && it.fromId != ME && it.sentAt > seen }
    }

    fun marcarGrupoVisto(groupId: String) {
        val ultimo = estado.messages.filter { it.groupId == groupId }.maxOfOrNull { it.sentAt } ?: return
        if ((estado.seenGroups[groupId] ?: 0) >= ultimo) return
        actualizar { it.copy(seenGroups = it.seenGroups + (groupId to ultimo)) }
    }

    fun enviarAGrupo(groupId: String, text: String = "", media: Media? = null, log: LogEntry? = null) {
        if (text.isBlank() && media == null && log == null) return
        val message = Message(nuevoId("m"), ME, "", text.trim(), media, log, ahora(), groupId = groupId)
        actualizar { s -> s.copy(messages = s.messages + message, seenGroups = s.seenGroups + (groupId to message.sentAt)) }
        subir { enviarMensaje(message) }
    }

    /** crea un grupo conmigo y con [memberIds]. la foto, si hay, es un jpeg ya recortado. devuelve el motivo si falla. */
    suspend fun crearGrupo(name: String, idsMiembros: Set<String>, foto: ByteArray?): String? = intentar {
        val group = Group(nuevoId("g"), name.trim().take(60), null, ME, listOf(ME) + idsMiembros, ahora())
        cola.withLock {
            servidor.crearGrupo(group)
            val url = foto?.let { servidor.ponerFotoDeGrupo(group.id, it) }
            actualizar { it.copy(groups = it.groups + group.copy(photoUrl = url)) }
        }
        null
    }

    suspend fun ponerFotoDeGrupo(groupId: String, foto: ByteArray): String? = intentar {
        cola.withLock {
            val url = servidor.ponerFotoDeGrupo(groupId, foto)
            actualizar { s -> s.copy(groups = s.groups.map { if (it.id == groupId) it.copy(photoUrl = url) else it }) }
        }
        null
    }

    /** salir de un grupo o, si lo cree yo, borrarlo para todos. */
    fun salirDeGrupo(groupId: String) {
        val mio = group(groupId)?.ownerId == ME
        actualizar { s -> s.copy(groups = s.groups.filter { it.id != groupId }, messages = s.messages.filter { it.groupId != groupId }) }
        subir { if (mio) borrarGrupo(groupId) else salirDeGrupo(groupId) }
    }

    val notices: List<Notice> get() = estado.notices.sortedByDescending { it.at }

    fun marcarAvisosLeidos() {
        if (estado.notices.none { !it.read }) return
        actualizar { s -> s.copy(notices = s.notices.map { it.copy(read = true) }) }
    }

    val sinLeer: Int
        get() = estado.messages.count { it.groupId == null && it.fromId != ME && !it.read } +
            estado.groups.sumOf { sinLeerEn(it.id) } +
            estado.notices.count { !it.read }

    companion object {
        /** deja un nombre de usuario como lo admite el servidor: minusculas sin acento, numeros, punto y guion bajo. */
        fun limpiarUsuario(text: String): String =
            text.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }.take(30)

        private const val KEY = "palette.v1"
        private const val STORY_WINDOW = 4 * DAY
    }
}
