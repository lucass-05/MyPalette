package com.example.palette.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.Group
import com.example.palette.data.ME
import com.example.palette.data.User
import com.example.palette.data.Message
import com.example.palette.data.haceCuanto
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Outgoing
import com.example.palette.ui.Avatar
import com.example.palette.ui.CoverByHeight
import com.example.palette.ui.EmptyState
import com.example.palette.ui.IconAction
import com.example.palette.ui.Icons
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.PlainField
import com.example.palette.ui.ScreenTitle
import com.example.palette.ui.SheetRow
import com.example.palette.ui.Stars
import com.example.palette.ui.formaAvatar
import com.example.palette.ui.titular
import com.example.palette.ui.legible
import com.example.palette.ui.recordarSelectorDeFotos
import com.example.palette.ui.avatarCuadrado
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/** foto de un grupo, con la misma forma de mancha que los avatares. sin foto, su inicial sobre el color de detalle. */
@Composable
fun GroupAvatar(group: Group, size: Dp, modifier: Modifier = Modifier) {
    val color = Pigment.Accent
    Box(modifier.size(size).clip(formaAvatar(group.id.hashCode())).background(color), contentAlignment = Alignment.Center) {
        Text(group.name.take(1).uppercase(), style = titular((size.value * 0.46f).sp, color.legible()))
        if (group.photoUrl != null) {
            AsyncImage(group.photoUrl, contentDescription = group.name, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}

/** mensajes con amigos y grupos, y avisos (quien ha empezado a seguirte). */
@Composable
fun ActivityScreen(modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    var seccion by remember { mutableIntStateOf(0) }
    val hilos = almacen.hilos
    val notices = almacen.notices
    val avisosPendientes = notices.count { !it.read }

    LaunchedEffect(seccion) { if (seccion == 1) almacen.marcarAvisosLeidos() }

    Column(modifier) {
        ScreenTitle("Mensajes") {
            IconAction(Icons.Users, "Grupo nuevo", { navegacion.creandoGrupo = true })
            IconAction(Icons.Pencil, "Mensaje nuevo", { navegacion.enviando = Outgoing.Nothing })
        }
        Segments(listOf("Chats", if (avisosPendientes > 0) "Actividad ($avisosPendientes)" else "Actividad"), seccion, { seccion = it }, Pigment.Accent)
        LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = BarSpace)) {
            if (seccion == 0) {
                if (hilos.isEmpty()) item {
                    EmptyState("Ningún chat todavía", "Escribe a un amigo, crea un grupo o manda una reseña desde el icono del avión de papel.", Pigment.Cobalt, semilla = 30) {
                        PillButton("Escribir a alguien", { navegacion.enviando = Outgoing.Nothing }, icon = Icons.Pencil)
                    }
                }
                items(hilos, key = { it.id }) { hilo ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { navegacion.abrir(hilo.group?.let { Dest.GroupChat(it.id) } ?: Dest.Chat(hilo.user?.id.orEmpty())) }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hilo.group != null) GroupAvatar(hilo.group, 50.dp) else hilo.user?.let { Avatar(it, 50.dp) }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(hilo.group?.name ?: hilo.user?.name.orEmpty(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                hilo.last?.let { resumen(it, if (hilo.group != null) almacen.user(it.fromId) else null) } ?: "Grupo recién creado",
                                color = if (hilo.unread > 0) Pigment.Ink else Pigment.Graphite,
                                fontWeight = if (hilo.unread > 0) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(haceCuanto(hilo.at), color = Pigment.Graphite, fontSize = 12.sp)
                            if (hilo.unread > 0) {
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Pigment.Cadmium))
                            }
                        }
                    }
                }
            } else {
                if (notices.isEmpty()) item {
                    EmptyState("Sin novedades", "Aquí verás quién empieza a seguirte.", Pigment.Cobalt, semilla = 31)
                }
                items(notices, key = { it.id }) { aviso ->
                    val user = almacen.user(aviso.userId) ?: return@items
                    UserRow(user, onClick = { navegacion.abrir(Dest.Profile(user.id)) }) { FollowButton(user.id) }
                    Text(
                        "Ha empezado a seguirte, hace ${haceCuanto(aviso.at)}".replace("hace ahora", "ahora mismo"),
                        color = Pigment.Graphite, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 79.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/** resumen de un mensaje para la lista. en los grupos se antepone quien lo escribio. */
private fun resumen(m: Message, autor: User?): String {
    val quien = when {
        m.fromId == ME -> "Tú: "
        autor != null -> autor.name.substringBefore(' ') + ": "
        else -> ""
    }
    return quien + when {
        m.text.isNotBlank() -> m.text
        m.log != null -> "Reseña de ${m.log.media.title}"
        m.media != null -> m.media.title
        else -> ""
    }
}

@Composable
fun ChatScreen(userId: String, modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val user = almacen.user(userId)
    if (user == null) {
        LaunchedEffect(Unit) { navegacion.volver() }
        return
    }
    val messages = almacen.mensajesCon(userId)
    LaunchedEffect(messages.size) { almacen.marcarLeido(userId) }

    Conversation(
        messages = messages,
        modifier = modifier,
        alEnviar = { almacen.enviar(userId, it) },
        alTocarCabecera = { navegacion.abrir(Dest.Profile(userId)) },
        header = {
            Avatar(user, 34.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("@${user.username}", color = Pigment.Graphite, fontSize = 12.sp)
            }
        },
    )
}

@Composable
fun GroupChatScreen(groupId: String, modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val group = almacen.group(groupId)
    if (group == null) {
        LaunchedEffect(Unit) { navegacion.volver() }
        return
    }
    val messages = almacen.mensajesDeGrupo(groupId)
    LaunchedEffect(messages.size) { almacen.marcarGrupoVisto(groupId) }

    Conversation(
        messages = messages,
        modifier = modifier,
        alEnviar = { almacen.enviarAGrupo(groupId, it) },
        alTocarCabecera = { navegacion.infoDeGrupo = groupId },
        mostrarAutores = true,
        header = {
            GroupAvatar(group, 34.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${group.members.size} personas", color = Pigment.Graphite, fontSize = 12.sp)
            }
        },
    )
}

/** la pantalla de chat en si; la comparten las conversaciones con una persona y las de grupo. */
@Composable
private fun Conversation(
    messages: List<Message>,
    modifier: Modifier,
    alEnviar: (String) -> Unit,
    alTocarCabecera: () -> Unit,
    mostrarAutores: Boolean = false,
    header: @Composable () -> Unit,
) {
    val app = LocalPalette.current
    val navegacion = app.navegacion
    var borrador by remember { mutableStateOf("") }

    // con el chat abierto se miran los mensajes nuevos mas a menudo que en el resto de la app
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            app.almacen.sincronizarMensajes()
        }
    }

    Column(modifier.navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Back, "Volver", { navegacion.volver() })
            Row(Modifier.weight(1f).clickable(onClick = alTocarCabecera), verticalAlignment = Alignment.CenterVertically) { header() }
        }
        // con reverselayout la lista arranca abajo y se queda pegada al ultimo mensaje
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(messages.asReversed(), key = { it.id }) { message -> Bubble(message, mostrarAutores) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            PlainField(borrador, { borrador = it.take(2000) }, "Escribe un mensaje", Modifier.weight(1f), singleLine = false)
            IconAction(
                Icons.Send, "Enviar",
                onClick = {
                    alEnviar(borrador)
                    borrador = ""
                },
                tint = if (borrador.isBlank()) Pigment.Graphite else Pigment.Accent,
            )
        }
    }
}

@Composable
private fun Bubble(message: Message, mostrarAutor: Boolean) {
    val app = LocalPalette.current
    val navegacion = app.navegacion
    val mio = message.fromId == ME
    val adjunto = message.log?.media ?: message.media
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mio) Alignment.End else Alignment.Start) {
        if (mostrarAutor && !mio) {
            val autor = app.almacen.user(message.fromId)
            Text(autor?.name ?: "Alguien", color = autor?.let { Color(it.favorite) } ?: Pigment.Graphite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp))
        }
        if (adjunto != null) {
            Row(
                Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(18.dp)).background(Pigment.of(adjunto.type).copy(alpha = 0.12f))
                    .clickable { navegacion.abrir(Dest.Detail(adjunto)) }.padding(10.dp),
            ) {
                CoverByHeight(adjunto, 86.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(adjunto.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(adjunto.subtitulo(), color = Pigment.Graphite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    message.log?.let { log ->
                        Spacer(Modifier.height(6.dp))
                        Stars(log.stars, size = 13.dp)
                        if (log.review.isNotBlank()) {
                            Text(log.review, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            if (message.text.isNotBlank()) Spacer(Modifier.height(4.dp))
        }
        if (message.text.isNotBlank()) {
            Text(
                message.text,
                color = if (mio) Pigment.Paper else Pigment.Ink,
                fontSize = 15.sp, lineHeight = 21.sp,
                modifier = Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(20.dp)).background(if (mio) Pigment.Ink else Pigment.Mist).padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

/** crear un grupo: nombre, foto opcional y que amigos entran. */
@Composable
fun NewGroupSheet() {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val ambito = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var elegidos by remember { mutableStateOf(emptySet<String>()) }
    var foto by remember { mutableStateOf<ByteArray?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var problema by remember { mutableStateOf<String?>(null) }
    val cerrar = { navegacion.creandoGrupo = false }
    val elegirFoto = recordarSelectorDeFotos { elegida ->
        ambito.launch {
            foto = runCatching { withContext(Dispatchers.Default) { avatarCuadrado(elegida) } }.getOrNull()
            problema = if (foto == null) "No se ha podido leer esa imagen." else null
        }
    }

    Sheet(cerrar) {
        Text("Grupo nuevo", style = titular(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(12.dp))
        PlainField(name, { name = it.take(60) }, "Nombre del grupo", Modifier.padding(horizontal = 22.dp))
        Row(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton(if (foto == null) "Añadir foto" else "Foto elegida. Cambiar", elegirFoto, filled = false)
        }
        if (almacen.amigos.isEmpty()) {
            EmptyState("Aún no sigues a nadie", "Sigue a alguien para poder meterlo en un grupo.", Pigment.Cobalt, semilla = 9)
        }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            almacen.amigos.forEach { amigo ->
                val dentro = amigo.id in elegidos
                UserRow(amigo, onClick = { elegidos = if (dentro) elegidos - amigo.id else elegidos + amigo.id }) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (dentro) Pigment.Accent else Pigment.Mist),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dentro) Icon(Icons.Check, contentDescription = "Elegido", tint = Pigment.Accent.legible(), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) }
        PillButton(
            if (ocupado) "Creando…" else "Crear grupo",
            {
                ocupado = true
                ambito.launch {
                    problema = almacen.crearGrupo(name, elegidos, foto)
                    ocupado = false
                    if (problema == null) cerrar()
                }
            },
            enabled = !ocupado && name.isNotBlank() && elegidos.isNotEmpty(),
            color = Pigment.Accent,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        )
    }
}

/** quien esta en el grupo, cambiarle la foto y salir (o borrarlo si es mio). */
@Composable
fun GroupInfoSheet(groupId: String) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val ambito = rememberCoroutineScope()
    val group = almacen.group(groupId)
    val cerrar = { navegacion.infoDeGrupo = null }
    if (group == null) {
        LaunchedEffect(Unit) { cerrar() }
        return
    }
    var ocupado by remember { mutableStateOf(false) }
    var problema by remember { mutableStateOf<String?>(null) }
    var confirmando by remember { mutableStateOf(false) }
    val mio = group.ownerId == ME
    val elegirFoto = recordarSelectorDeFotos { elegida ->
        ocupado = true
        ambito.launch {
            val cuadrada = runCatching { withContext(Dispatchers.Default) { avatarCuadrado(elegida) } }.getOrNull()
            problema = if (cuadrada == null) "No se ha podido leer esa imagen." else almacen.ponerFotoDeGrupo(groupId, cuadrada)
            ocupado = false
        }
    }

    Sheet(cerrar) {
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            GroupAvatar(group, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(group.name, style = titular(22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${group.members.size} personas", color = Pigment.Graphite, fontSize = 13.sp)
            }
        }
        // la foto solo la puede cambiar quien creo el grupo: es quien tiene permiso en el servidor
        if (mio) {
            PillButton(if (ocupado) "Subiendo…" else "Cambiar foto del grupo", elegirFoto, filled = false, enabled = !ocupado, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            group.members.mapNotNull { almacen.user(it) }.forEach { miembro ->
                UserRow(miembro, onClick = { if (miembro.id != ME) { cerrar(); navegacion.abrir(Dest.Profile(miembro.id)) } }) {
                    if (miembro.id == group.ownerId) Text("Creó el grupo", color = Pigment.Graphite, fontSize = 12.sp)
                }
            }
        }
        if (confirmando) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (mio) "Se borra para todos, con sus mensajes." else "Dejarás de ver sus mensajes.", fontSize = 14.sp, modifier = Modifier.weight(1f))
                PillButton("Cancelar", { confirmando = false }, filled = false)
                PillButton(if (mio) "Borrar" else "Salir", { almacen.salirDeGrupo(groupId); cerrar(); navegacion.volver() }, color = Pigment.Cadmium)
            }
        } else {
            SheetRow(Icons.Trash, if (mio) "Borrar el grupo" else "Salir del grupo", { confirmando = true }, tint = Pigment.Cadmium)
        }
    }
}
