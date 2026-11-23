package com.example.palette.core.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.palette.core.model.Comment
import com.example.palette.core.model.Group
import com.example.palette.core.model.Like
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaList
import com.example.palette.core.model.Message
import com.example.palette.core.model.Notice
import com.example.palette.core.model.NoticeKind
import com.example.palette.core.model.ProfileLayout
import com.example.palette.core.model.Snapshot
import com.example.palette.core.model.ThemeColors
import com.example.palette.core.model.User
import com.example.palette.core.model.ligero
import com.example.palette.core.network.Supabase
import com.example.palette.core.network.json
import com.example.palette.core.network.supabase
import com.example.palette.core.storage.escribirGuardado
import com.example.palette.core.storage.leerGuardado
import com.example.palette.core.util.DAY
import com.example.palette.core.util.ahora
import com.example.palette.core.util.limpiarUsuario
import com.example.palette.core.util.nuevoId
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
 * fuente de datos de la app: mi perfil, mis logs, a quien sigo, mensajes...
 *
 * guarda una copia en el dispositivo y habla con supabase. cada cambio se aplica primero en
 * local, para que la pantalla responda al momento, y despues se manda al servidor. [sincronizar]
 * baja lo que haya cambiado alli (logs de amigos, mensajes nuevos, seguidores).
 *
 * las pantallas no la usan directamente: cada feature tiene su repositorio (carpeta data) que
 * lee de aqui y entrega los datos ya preparados para lo que esa feature necesita.
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
                } catch (e: Throwable) {
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

    // se captura Throwable y no Exception porque en web ktor avisa de un fallo de red con un Error
    private suspend fun intentar(block: suspend () -> String?): String? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            e.message ?: "No se ha podido conectar. Revisa la conexión."
        }

    /** baja del servidor todo lo que ve mi cuenta y lo deja como estado de la app. */
    suspend fun sincronizar() {
        if (!conSesion) return
        try {
            cola.withLock {
                val recibido = servidor.bajarTodo()
                actualizar { s ->
                    // la primera vez con esta version no se avisa de golpe de todo lo antiguo
                    val desde = if (s.avisosDesde == 0L) ahora() else s.avisosDesde
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
                        likes = recibido.likes,
                        comments = recibido.comments,
                        lists = recibido.lists,
                        groups = recibido.groups,
                        notices = calcularAvisos(
                            misLogs = recibido.logs, logsDeOtros = recibido.peopleLogs, siguiendo = recibido.following,
                            seguidores = recibido.followers, meGustas = recibido.likes, comentarios = recibido.comments,
                            anteriores = s.notices, desde = desde,
                        ).take(MAX_AVISOS),
                        avisosDesde = desde,
                    )
                }
            }
            sincronizado = true
            sinConexion = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
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
        } catch (e: Throwable) {
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

    fun ponerTema(theme: ThemeColors) = actualizar { it.copy(theme = theme) }

    // ---- logs

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
        actualizar { s ->
            s.copy(
                liked = if (like) s.liked + logId else s.liked - logId,
                likes = if (like) s.likes + Like(ME, logId, ahora()) else s.likes.filterNot { it.userId == ME && it.logId == logId },
            )
        }
        subir { ponerMeGusta(logId, like) }
    }

    // ---- reseñas y comentarios

    /** un log mio o de otra persona, por su id. */
    fun log(logId: String): LogEntry? =
        estado.logs.firstOrNull { it.id == logId } ?: estado.peopleLogs.firstOrNull { it.id == logId }

    fun meGustasDe(logId: String): Int = estado.likes.count { it.logId == logId }

    fun comentariosDe(logId: String): List<Comment> = estado.comments.filter { it.logId == logId }.sortedBy { it.createdAt }

    fun comentar(logId: String, text: String) {
        val limpio = text.trim().take(1000)
        if (limpio.isEmpty()) return
        val comment = Comment(nuevoId("c"), logId, ME, limpio, ahora())
        actualizar { s -> s.copy(comments = s.comments + comment) }
        subir { comentar(comment) }
    }

    fun borrarComentario(id: String) {
        actualizar { s -> s.copy(comments = s.comments.filter { it.id != id }) }
        subir { borrarComentario(id) }
    }

    // ---- listas

    fun lista(id: String): MediaList? = estado.lists.firstOrNull { it.id == id }

    /** las listas de esa persona. de los demas solo se ven las que tienen visibles. */
    fun listasDe(userId: String): List<MediaList> =
        estado.lists.filter { it.userId == userId && (userId == ME || it.visible) }.sortedBy { it.createdAt }

    fun crearLista(name: String, kind: ListKind): MediaList {
        val list = MediaList(nuevoId("ls"), ME, name.trim().take(60), kind, createdAt = ahora())
        actualizar { s -> s.copy(lists = s.lists + list) }
        subir { guardarLista(list) }
        return list
    }

    /** guarda los cambios de una lista mia (nombre, contenidos, si se ve en el perfil). */
    fun guardarLista(list: MediaList) {
        val limpia = list.copy(name = list.name.trim().take(60).ifEmpty { "Sin nombre" }, media = list.media.map { it.ligero() })
        actualizar { s -> s.copy(lists = s.lists.map { if (it.id == limpia.id) limpia else it }) }
        subir { guardarLista(limpia) }
    }

    fun borrarLista(id: String) {
        actualizar { s -> s.copy(lists = s.lists.filter { it.id != id }) }
        subir { borrarLista(id) }
    }

    // ---- perfil personalizado

    /** guarda como se ve el perfil: bloques, paginas, color de fondo y si los pendientes son publicos. */
    fun guardarDiseno(layout: ProfileLayout, background: Long?, pendientesPublicos: Boolean) {
        val limpio = layout.copy(widgets = layout.widgets.map { w -> w.copy(items = w.items.map { it.copy(media = it.media.ligero()) }) })
        actualizar { it.copy(me = it.me.copy(layout = limpio, background = background, watchlistPublic = pendientesPublicos)) }
        val profile = estado.me
        subir { guardarPerfil(profile) }
    }

    /** sube la foto de cabecera (jpeg ya recortado) y la deja puesta. con null se quita. */
    suspend fun ponerBanner(jpeg: ByteArray?): String? = intentar {
        cola.withLock {
            val url = jpeg?.let { servidor.subirBanner(it) }
            val profile = estado.me.copy(bannerUrl = url)
            servidor.guardarPerfil(profile)
            actualizar { it.copy(me = profile) }
        }
        null
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

    fun renombrarGrupo(groupId: String, name: String) {
        val limpio = name.trim().take(60)
        if (limpio.isEmpty()) return
        actualizar { s -> s.copy(groups = s.groups.map { if (it.id == groupId) it.copy(name = limpio) else it }) }
        subir { renombrarGrupo(groupId, limpio) }
    }

    fun anadirAGrupo(groupId: String, ids: Set<String>) {
        val group = group(groupId) ?: return
        val nuevos = ids.filter { it !in group.members }
        if (nuevos.isEmpty()) return
        actualizar { s -> s.copy(groups = s.groups.map { if (it.id == groupId) it.copy(members = it.members + nuevos) else it }) }
        subir { anadirAGrupo(groupId, nuevos) }
    }

    /** saca a alguien del grupo. solo puede hacerlo quien lo creo. */
    fun quitarDeGrupo(groupId: String, userId: String) {
        actualizar { s -> s.copy(groups = s.groups.map { if (it.id == groupId) it.copy(members = it.members - userId) else it }) }
        subir { quitarDeGrupo(groupId, userId) }
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
        private const val KEY = "palette.v1"
        private const val STORY_WINDOW = 4 * DAY
        private const val MAX_AVISOS = 150
    }
}
