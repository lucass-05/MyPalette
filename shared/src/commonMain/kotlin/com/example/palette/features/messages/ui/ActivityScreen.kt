package com.example.palette.features.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.ME
import com.example.palette.core.model.Message
import com.example.palette.core.model.NoticeKind
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.Stars
import com.example.palette.features.messages.domain.model.Aviso
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.palette.core.model.User
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.FollowButton
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.ScreenTitle
import com.example.palette.core.ui.Segments
import com.example.palette.core.ui.icon
import com.example.palette.core.util.ahora
import com.example.palette.core.util.haceCuanto
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.messages.domain.model.Hilo
import com.example.palette.features.messages.presentation.ActivityAction
import com.example.palette.features.messages.presentation.ActivityState
import com.example.palette.features.messages.presentation.ActivityViewModel
import com.example.palette.navigation.BarSpace

@Composable
fun ActivityScreenRoot(
    onOpenChat: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenReview: (String) -> Unit,
    onNewGroup: () -> Unit,
    onNewMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ActivityViewModel(container.messagesRepository, container.peopleRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ActivityScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is ActivityAction.OnHiloClicked -> {
                    val group = action.hilo.group
                    if (group != null) onOpenGroup(group.id) else action.hilo.user?.let { onOpenChat(it.id) }
                }
                is ActivityAction.OnUserClicked -> onOpenUser(action.userId)
                is ActivityAction.OnResenaClicked -> onOpenReview(action.logId)
                ActivityAction.OnNewGroupClicked -> onNewGroup()
                ActivityAction.OnNewMessageClicked -> onNewMessage()
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

/** mensajes con amigos y grupos, y avisos: seguidores, me gusta, comentarios y reseñas de amigos. */
@Composable
fun ActivityScreen(state: ActivityState, onAction: (ActivityAction) -> Unit, modifier: Modifier = Modifier) {
    val pendientes = state.avisosPendientes

    Column(modifier) {
        ScreenTitle("Mensajes") {
            IconAction(Icons.Users, "Grupo nuevo", { onAction(ActivityAction.OnNewGroupClicked) })
            IconAction(Icons.Pencil, "Mensaje nuevo", { onAction(ActivityAction.OnNewMessageClicked) })
        }
        Segments(listOf("Chats", if (pendientes > 0) "Actividad ($pendientes)" else "Actividad"), state.seccion, { onAction(ActivityAction.OnSeccionChanged(it)) }, Pigment.Accent)
        LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = BarSpace)) {
            if (state.seccion == 0) {
                if (state.hilos.isEmpty()) item {
                    EmptyState("Ningún chat todavía", "Escribe a un amigo, crea un grupo o manda una reseña desde el icono del avión de papel.", Pigment.Cobalt, semilla = 30) {
                        PillButton("Escribir a alguien", { onAction(ActivityAction.OnNewMessageClicked) }, icon = Icons.Pencil)
                    }
                }
                items(state.hilos, key = { it.id }) { hilo -> ThreadRow(hilo) { onAction(ActivityAction.OnHiloClicked(hilo)) } }
            } else {
                if (state.avisos.isEmpty()) item {
                    EmptyState(
                        "Sin novedades",
                        "Aquí verás quién empieza a seguirte, los me gusta y comentarios de tus reseñas y lo que reseñan tus amigos de lo que tú ya has reseñado.",
                        Pigment.Cobalt, semilla = 31,
                    )
                }
                items(state.avisos, key = { it.id }) { aviso -> AvisoRow(aviso, onAction) }
            }
        }
    }
}

/**
 * un aviso. tocarlo lleva a lo que tiene que ver: el perfil de quien te sigue
 * o la reseña de la que va (la tuya que han comentado, la de tu amigo...).
 */
@Composable
private fun AvisoRow(aviso: Aviso, onAction: (ActivityAction) -> Unit) {
    val log = aviso.log
    val titulo = log?.media?.title.orEmpty()
    val accion = when (aviso.kind) {
        NoticeKind.FOLLOW -> " ha empezado a seguirte"
        NoticeKind.LIKE -> " ha dado me gusta a tu reseña de $titulo"
        NoticeKind.COMMENT -> " ha comentado tu reseña de $titulo"
        NoticeKind.SAME_MEDIA -> " también ha reseñado $titulo"
    }
    val alTocar = { if (log == null) onAction(ActivityAction.OnUserClicked(aviso.user.id)) else onAction(ActivityAction.OnResenaClicked(log.id)) }

    Row(
        Modifier.fillMaxWidth().clickable(onClick = alTocar).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(aviso.user, 46.dp, Modifier.clickable { onAction(ActivityAction.OnUserClicked(aviso.user.id)) })
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(aviso.user.name) }
                    append(accion)
                },
                fontSize = 14.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            aviso.comentario?.let {
                Text("“$it”", color = Pigment.Ink.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            if (aviso.kind == NoticeKind.SAME_MEDIA && log != null) {
                Stars(log.stars, size = 12.dp, modifier = Modifier.padding(top = 3.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                if (!aviso.leido) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Pigment.Accent))
                    Spacer(Modifier.width(6.dp))
                }
                Text(haceCuanto(aviso.at).let { if (it == "ahora") "ahora mismo" else "hace $it" }, color = Pigment.Graphite, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        if (log == null) {
            FollowButton(aviso.loSigo, { onAction(ActivityAction.OnFollowClicked(aviso.user.id)) })
        } else {
            CoverByHeight(log.media, 56.dp)
        }
    }
}

@Composable
private fun ThreadRow(hilo: Hilo, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hilo.group != null) GroupAvatar(hilo.group, 50.dp) else hilo.user?.let { Avatar(it, 50.dp) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(hilo.group?.name ?: hilo.user?.name.orEmpty(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                hilo.ultimo?.let { resumen(it, hilo.autorDelUltimo) } ?: "Grupo recién creado",
                color = if (hilo.sinLeer > 0) Pigment.Ink else Pigment.Graphite,
                fontWeight = if (hilo.sinLeer > 0) FontWeight.Medium else FontWeight.Normal,
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(haceCuanto(hilo.at), color = Pigment.Graphite, fontSize = 12.sp)
            if (hilo.sinLeer > 0) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(10.dp).clip(CircleShape).background(Pigment.Cadmium))
            }
        }
    }
}

/** resumen de un mensaje para la lista. en los grupos se antepone quien lo escribio. */
private fun resumen(m: Message, autor: User?): String {
    if (m.story) return if (m.fromId == ME) "Has respondido a su historia" else "Ha respondido a tu historia"
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
