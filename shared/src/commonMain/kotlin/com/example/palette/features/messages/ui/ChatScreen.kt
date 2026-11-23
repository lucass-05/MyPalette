package com.example.palette.features.messages.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.example.palette.features.messages.presentation.resumen
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
    val mensajes = conversacion.mensajes.asReversed()
    val lista = rememberLazyListState()
    val ambito = rememberCoroutineScope()

    // el nombre que sale encima de una cita o de la barra de responder
    fun nombreDe(fromId: String): String = when (fromId) {
        ME -> "ti"
        else -> (conversacion.autores[fromId] ?: user?.takeIf { it.id == fromId })?.name?.substringBefore(' ') ?: "alguien"
    }

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
            state = lista,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(mensajes, key = { it.id }) { message ->
                // en un grupo, cada mensaje ajeno lleva encima el nombre de quien lo escribio
                val autor = if (group != null && message.fromId != ME) conversacion.autores[message.fromId] else null
                DeslizarParaResponder({ onAction(ChatAction.OnResponderMensaje(message)) }) {
                    Bubble(
                        message, autor, conNombre = group != null,
                        nombreCitado = message.replyTo?.let { nombreDe(it.fromId) },
                        onMediaClick = { onAction(ChatAction.OnMediaClicked(it)) },
                        // tocar la cita lleva al mensaje original, si sigue en el chat
                        onCitaClick = { id ->
                            val posicion = mensajes.indexOfFirst { it.id == id }
                            if (posicion >= 0) ambito.launch { lista.animateScrollToItem(posicion) }
                        },
                    )
                }
            }
        }
        state.respondiendoA?.let { citado ->
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp).clip(RoundedCornerShape(14.dp)).background(Pigment.Mist).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(50)).background(Pigment.Accent))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text("Respondiendo a ${nombreDe(citado.fromId)}", color = Pigment.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(citado.resumen(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconAction(Icons.Close, "No responder", { onAction(ChatAction.OnCancelarRespuesta) }, tint = Pigment.Graphite)
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

/**
 * como en whatsapp: el mensaje se puede arrastrar a la derecha. a la izquierda aparece la flecha de
 * responder y, si se suelta pasado el umbral, se contesta a ese mensaje. al soltar vuelve a su sitio.
 */
@Composable
private fun DeslizarParaResponder(alResponder: () -> Unit, contenido: @Composable () -> Unit) {
    val desplazamiento = remember { Animatable(0f) }
    val ambito = rememberCoroutineScope()
    val umbral = with(LocalDensity.current) { 64.dp.toPx() }
    val responder by rememberUpdatedState(alResponder)

    Box(
        Modifier.fillMaxWidth().pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (desplazamiento.value >= umbral) responder()
                    ambito.launch { desplazamiento.animateTo(0f, spring()) }
                },
                onDragCancel = { ambito.launch { desplazamiento.animateTo(0f, spring()) } },
            ) { cambio, arrastre ->
                // solo hacia la derecha, y cada vez mas duro pasado el umbral
                val siguiente = (desplazamiento.value + arrastre * if (desplazamiento.value > umbral) 0.3f else 1f).coerceIn(0f, umbral * 1.5f)
                if (siguiente != desplazamiento.value) cambio.consume()
                ambito.launch { desplazamiento.snapTo(siguiente) }
            }
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        val avance = (desplazamiento.value / umbral).coerceIn(0f, 1f)
        Icon(
            Icons.Reply, contentDescription = null, tint = Pigment.Accent,
            modifier = Modifier.padding(start = 4.dp).size(22.dp).graphicsLayer { alpha = avance; scaleX = 0.6f + avance * 0.4f; scaleY = scaleX },
        )
        Box(Modifier.fillMaxWidth().offset { IntOffset(desplazamiento.value.roundToInt(), 0) }) { contenido() }
    }
}

@Composable
private fun Bubble(
    message: Message,
    autor: User?,
    conNombre: Boolean,
    nombreCitado: String?,
    onMediaClick: (Media) -> Unit,
    onCitaClick: (String) -> Unit,
) {
    val mio = message.fromId == ME
    val historia = message.log?.takeIf { message.story }
    val adjunto = if (historia != null) null else message.log?.media ?: message.media
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mio) Alignment.End else Alignment.Start) {
        if (conNombre && !mio) {
            Text(autor?.name ?: "Alguien", color = autor?.let { Color(it.favorite) } ?: Pigment.Graphite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp))
        }
        // respuesta a una historia: una portada pequeña con el aviso de a que se contesta
        if (historia != null) {
            Text(
                if (mio) "Has respondido a su historia" else "Ha respondido a tu historia",
                color = Pigment.Graphite, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Row(
                Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(12.dp)).clickable { onMediaClick(historia.media) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (mio) Box(Modifier.width(3.dp).height(70.dp).clip(RoundedCornerShape(50)).background(Pigment.Line))
                Spacer(Modifier.width(8.dp))
                CoverByHeight(historia.media, 70.dp, Modifier.clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(8.dp))
                if (!mio) Box(Modifier.width(3.dp).height(70.dp).clip(RoundedCornerShape(50)).background(Pigment.Line))
            }
        }
        // la cita del mensaje al que contesta, encima del texto
        message.replyTo?.let { cita ->
            Column(
                Modifier.widthIn(max = 260.dp).padding(bottom = 2.dp).clip(RoundedCornerShape(14.dp)).background(Pigment.Mist.copy(alpha = 0.6f))
                    .clickable { onCitaClick(cita.messageId) }.padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(if (nombreCitado == "ti") "Respondiendo a tu mensaje" else "Respondiendo a ${nombreCitado ?: "alguien"}", color = Pigment.Graphite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(cita.text, color = Pigment.Graphite, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
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
