package com.example.palette.features.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.model.Message
import com.example.palette.core.model.User
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.subtitulo
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.messages.presentation.ChatAction
import com.example.palette.features.messages.presentation.ChatState
import com.example.palette.features.messages.presentation.ChatViewModel

// el chat con una persona y el de un grupo son la misma pantalla y el mismo viewmodel.
// cada uno tiene su root porque cambia lo que se abre al tocar la cabecera.

@Composable
fun ChatScreenRoot(userId: String, onBack: () -> Unit, onOpenUser: (String) -> Unit, onOpenMedia: (Media) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ChatViewModel(userId = userId, groupId = null, container.messagesRepository) }
    ChatRoot(viewModel, onBack, onHeader = { onOpenUser(userId) }, onOpenMedia, modifier)
}

@Composable
fun GroupChatScreenRoot(groupId: String, onBack: () -> Unit, onOpenInfo: (String) -> Unit, onOpenMedia: (Media) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ChatViewModel(userId = null, groupId = groupId, container.messagesRepository) }
    ChatRoot(viewModel, onBack, onHeader = { onOpenInfo(groupId) }, onOpenMedia, modifier)
}

@Composable
private fun ChatRoot(viewModel: ChatViewModel, onBack: () -> Unit, onHeader: () -> Unit, onOpenMedia: (Media) -> Unit, modifier: Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // la persona o el grupo han dejado de existir (por ejemplo, he salido del grupo): se cierra el chat
    LaunchedEffect(state.existe) { if (!state.existe) onBack() }

    ChatScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ChatAction.OnBackClicked -> onBack()
                ChatAction.OnHeaderClicked -> onHeader()
                is ChatAction.OnMediaClicked -> onOpenMedia(action.media)
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ChatScreen(state: ChatState, onAction: (ChatAction) -> Unit, modifier: Modifier = Modifier) {
    val conversacion = state.conversacion
    val group = conversacion.group
    val user = conversacion.user

    Column(modifier.navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Back, "Volver", { onAction(ChatAction.OnBackClicked) })
            Row(Modifier.weight(1f).clickable { onAction(ChatAction.OnHeaderClicked) }, verticalAlignment = Alignment.CenterVertically) {
                if (group != null) {
                    GroupAvatar(group, 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${group.members.size} personas", color = Pigment.Graphite, fontSize = 12.sp)
                    }
                } else if (user != null) {
                    Avatar(user, 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("@${user.username}", color = Pigment.Graphite, fontSize = 12.sp)
                    }
                }
            }
        }
        // con reverselayout la lista arranca abajo y se queda pegada al ultimo mensaje
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(conversacion.mensajes.asReversed(), key = { it.id }) { message ->
                // en un grupo, cada mensaje ajeno lleva encima el nombre de quien lo escribio
                val autor = if (group != null && message.fromId != ME) conversacion.autores[message.fromId] else null
                Bubble(message, autor, conNombre = group != null) { onAction(ChatAction.OnMediaClicked(it)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            PlainField(state.borrador, { onAction(ChatAction.OnBorradorChanged(it)) }, "Escribe un mensaje", Modifier.weight(1f), singleLine = false)
            IconAction(
                Icons.Send, "Enviar",
                onClick = { onAction(ChatAction.OnSendClicked) },
                tint = if (state.borrador.isBlank()) Pigment.Graphite else Pigment.Accent,
            )
        }
    }
}

@Composable
private fun Bubble(message: Message, autor: User?, conNombre: Boolean, onMediaClick: (Media) -> Unit) {
    val mio = message.fromId == ME
    val adjunto = message.log?.media ?: message.media
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mio) Alignment.End else Alignment.Start) {
        if (conNombre && !mio) {
            Text(autor?.name ?: "Alguien", color = autor?.let { Color(it.favorite) } ?: Pigment.Graphite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp))
        }
        if (adjunto != null) {
            Row(
                Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(18.dp)).background(Pigment.of(adjunto.type).copy(alpha = 0.12f))
                    .clickable { onMediaClick(adjunto) }.padding(10.dp),
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
