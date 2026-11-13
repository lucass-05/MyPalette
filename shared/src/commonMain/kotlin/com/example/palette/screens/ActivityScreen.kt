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
import com.example.palette.data.timeAgo
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
import com.example.palette.ui.avatarShape
import com.example.palette.ui.display
import com.example.palette.ui.readable
import com.example.palette.ui.rememberPhotoPicker
import com.example.palette.ui.squareAvatar
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
    Box(modifier.size(size).clip(avatarShape(group.id.hashCode())).background(color), contentAlignment = Alignment.Center) {
        Text(group.name.take(1).uppercase(), style = display((size.value * 0.46f).sp, color.readable()))
        if (group.photoUrl != null) {
            AsyncImage(group.photoUrl, contentDescription = group.name, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}

/** mensajes con amigos y grupos, y avisos (quien ha empezado a seguirte). */
@Composable
fun ActivityScreen(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    var section by remember { mutableIntStateOf(0) }
    val threads = store.threads
    val notices = store.notices
    val pendingNotices = notices.count { !it.read }

    LaunchedEffect(section) { if (section == 1) store.markNoticesRead() }

    Column(modifier) {
        ScreenTitle("Mensajes") {
            IconAction(Icons.Users, "Grupo nuevo", { nav.creatingGroup = true })
            IconAction(Icons.Pencil, "Mensaje nuevo", { nav.sending = Outgoing.Nothing })
        }
        Segments(listOf("Chats", if (pendingNotices > 0) "Actividad ($pendingNotices)" else "Actividad"), section, { section = it }, Pigment.Accent)
        LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = BarSpace)) {
            if (section == 0) {
                if (threads.isEmpty()) item {
                    EmptyState("Ningún chat todavía", "Escribe a un amigo, crea un grupo o manda una reseña desde el icono del avión de papel.", Pigment.Cobalt, seed = 30) {
                        PillButton("Escribir a alguien", { nav.sending = Outgoing.Nothing }, icon = Icons.Pencil)
                    }
                }
                items(threads, key = { it.id }) { thread ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { nav.open(thread.group?.let { Dest.GroupChat(it.id) } ?: Dest.Chat(thread.user?.id.orEmpty())) }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (thread.group != null) GroupAvatar(thread.group, 50.dp) else thread.user?.let { Avatar(it, 50.dp) }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(thread.group?.name ?: thread.user?.name.orEmpty(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                thread.last?.let { preview(it, if (thread.group != null) store.user(it.fromId) else null) } ?: "Grupo recién creado",
                                color = if (thread.unread > 0) Pigment.Ink else Pigment.Graphite,
                                fontWeight = if (thread.unread > 0) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(timeAgo(thread.at), color = Pigment.Graphite, fontSize = 12.sp)
                            if (thread.unread > 0) {
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Pigment.Cadmium))
                            }
                        }
                    }
                }
            } else {
                if (notices.isEmpty()) item {
                    EmptyState("Sin novedades", "Aquí verás quién empieza a seguirte.", Pigment.Cobalt, seed = 31)
                }
                items(notices, key = { it.id }) { notice ->
                    val user = store.user(notice.userId) ?: return@items
                    UserRow(user, onClick = { nav.open(Dest.Profile(user.id)) }) { FollowButton(user.id) }
                    Text(
                        "Ha empezado a seguirte, hace ${timeAgo(notice.at)}".replace("hace ahora", "ahora mismo"),
                        color = Pigment.Graphite, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 79.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/** resumen de un mensaje para la lista. en los grupos se antepone quien lo escribio. */
private fun preview(m: Message, author: User?): String {
    val who = when {
        m.fromId == ME -> "Tú: "
        author != null -> author.name.substringBefore(' ') + ": "
        else -> ""
    }
    return who + when {
        m.text.isNotBlank() -> m.text
        m.log != null -> "Reseña de ${m.log.media.title}"
        m.media != null -> m.media.title
        else -> ""
    }
}

@Composable
fun ChatScreen(userId: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    val user = store.user(userId)
    if (user == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val messages = store.conversation(userId)
    LaunchedEffect(messages.size) { store.markRead(userId) }

    Conversation(
        messages = messages,
        modifier = modifier,
        onSend = { store.send(userId, it) },
        onHeaderClick = { nav.open(Dest.Profile(userId)) },
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
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    val group = store.group(groupId)
    if (group == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val messages = store.groupConversation(groupId)
    LaunchedEffect(messages.size) { store.markGroupSeen(groupId) }

    Conversation(
        messages = messages,
        modifier = modifier,
        onSend = { store.sendToGroup(groupId, it) },
        onHeaderClick = { nav.groupInfo = groupId },
        showAuthors = true,
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
    onSend: (String) -> Unit,
    onHeaderClick: () -> Unit,
    showAuthors: Boolean = false,
    header: @Composable () -> Unit,
) {
    val palette = LocalPalette.current
    val nav = palette.nav
    var draft by remember { mutableStateOf("") }

    // con el chat abierto se miran los mensajes nuevos mas a menudo que en el resto de la app
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            palette.store.syncMessages()
        }
    }

    Column(modifier.navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Back, "Volver", { nav.back() })
            Row(Modifier.weight(1f).clickable(onClick = onHeaderClick), verticalAlignment = Alignment.CenterVertically) { header() }
        }
        // con reverselayout la lista arranca abajo y se queda pegada al ultimo mensaje
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(messages.asReversed(), key = { it.id }) { message -> Bubble(message, showAuthors) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            PlainField(draft, { draft = it.take(2000) }, "Escribe un mensaje", Modifier.weight(1f), singleLine = false)
            IconAction(
                Icons.Send, "Enviar",
                onClick = {
                    onSend(draft)
                    draft = ""
                },
                tint = if (draft.isBlank()) Pigment.Graphite else Pigment.Accent,
            )
        }
    }
}

@Composable
private fun Bubble(message: Message, showAuthor: Boolean) {
    val palette = LocalPalette.current
    val nav = palette.nav
    val mine = message.fromId == ME
    val attached = message.log?.media ?: message.media
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        if (showAuthor && !mine) {
            val author = palette.store.user(message.fromId)
            Text(author?.name ?: "Alguien", color = author?.let { Color(it.favorite) } ?: Pigment.Graphite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp))
        }
        if (attached != null) {
            Row(
                Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(18.dp)).background(Pigment.of(attached.type).copy(alpha = 0.12f))
                    .clickable { nav.open(Dest.Detail(attached)) }.padding(10.dp),
            ) {
                CoverByHeight(attached, 86.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(attached.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(attached.byline(), color = Pigment.Graphite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                color = if (mine) Pigment.Paper else Pigment.Ink,
                fontSize = 15.sp, lineHeight = 21.sp,
                modifier = Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(20.dp)).background(if (mine) Pigment.Ink else Pigment.Mist).padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

/** crear un grupo: nombre, foto opcional y que amigos entran. */
@Composable
fun NewGroupSheet() {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(emptySet<String>()) }
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val close = { nav.creatingGroup = false }
    val pickPhoto = rememberPhotoPicker { picked ->
        scope.launch {
            photo = runCatching { withContext(Dispatchers.Default) { squareAvatar(picked) } }.getOrNull()
            problem = if (photo == null) "No se ha podido leer esa imagen." else null
        }
    }

    Sheet(close) {
        Text("Grupo nuevo", style = display(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(12.dp))
        PlainField(name, { name = it.take(60) }, "Nombre del grupo", Modifier.padding(horizontal = 22.dp))
        Row(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton(if (photo == null) "Añadir foto" else "Foto elegida. Cambiar", pickPhoto, filled = false)
        }
        if (store.friends.isEmpty()) {
            EmptyState("Aún no sigues a nadie", "Sigue a alguien para poder meterlo en un grupo.", Pigment.Cobalt, seed = 9)
        }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            store.friends.forEach { friend ->
                val inside = friend.id in chosen
                UserRow(friend, onClick = { chosen = if (inside) chosen - friend.id else chosen + friend.id }) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (inside) Pigment.Accent else Pigment.Mist),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (inside) Icon(Icons.Check, contentDescription = "Elegido", tint = Pigment.Accent.readable(), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        problem?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) }
        PillButton(
            if (busy) "Creando…" else "Crear grupo",
            {
                busy = true
                scope.launch {
                    problem = store.createGroup(name, chosen, photo)
                    busy = false
                    if (problem == null) close()
                }
            },
            enabled = !busy && name.isNotBlank() && chosen.isNotEmpty(),
            color = Pigment.Accent,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        )
    }
}

/** quien esta en el grupo, cambiarle la foto y salir (o borrarlo si es mio). */
@Composable
fun GroupInfoSheet(groupId: String) {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    val scope = rememberCoroutineScope()
    val group = store.group(groupId)
    val close = { nav.groupInfo = null }
    if (group == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf(false) }
    val mine = group.ownerId == ME
    val pickPhoto = rememberPhotoPicker { picked ->
        busy = true
        scope.launch {
            val square = runCatching { withContext(Dispatchers.Default) { squareAvatar(picked) } }.getOrNull()
            problem = if (square == null) "No se ha podido leer esa imagen." else store.setGroupPhoto(groupId, square)
            busy = false
        }
    }

    Sheet(close) {
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            GroupAvatar(group, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(group.name, style = display(22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${group.members.size} personas", color = Pigment.Graphite, fontSize = 13.sp)
            }
        }
        // la foto solo la puede cambiar quien creo el grupo: es quien tiene permiso en el servidor
        if (mine) {
            PillButton(if (busy) "Subiendo…" else "Cambiar foto del grupo", pickPhoto, filled = false, enabled = !busy, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        problem?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            group.members.mapNotNull { store.user(it) }.forEach { member ->
                UserRow(member, onClick = { if (member.id != ME) { close(); nav.open(Dest.Profile(member.id)) } }) {
                    if (member.id == group.ownerId) Text("Creó el grupo", color = Pigment.Graphite, fontSize = 12.sp)
                }
            }
        }
        if (confirming) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (mine) "Se borra para todos, con sus mensajes." else "Dejarás de ver sus mensajes.", fontSize = 14.sp, modifier = Modifier.weight(1f))
                PillButton("Cancelar", { confirming = false }, filled = false)
                PillButton(if (mine) "Borrar" else "Salir", { store.leaveGroup(groupId); close(); nav.back() }, color = Pigment.Cadmium)
            }
        } else {
            SheetRow(Icons.Trash, if (mine) "Borrar el grupo" else "Salir del grupo", { confirming = true }, tint = Pigment.Cadmium)
        }
    }
}
