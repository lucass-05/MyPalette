package com.example.palette.features.messages.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.MediaHeader
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.UserRow
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.messages.domain.model.Outgoing
import com.example.palette.features.messages.presentation.SendAction
import com.example.palette.features.messages.presentation.SendState
import com.example.palette.features.messages.presentation.SendViewModel

@Composable
fun SendSheetRoot(envio: Outgoing, onClose: () -> Unit, onOpenChat: (String) -> Unit, onOpenPeople: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { SendViewModel(envio, container.messagesRepository, container.peopleRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    SendSheet(
        state = state,
        onAction = { action ->
            when (action) {
                SendAction.OnDismiss -> onClose()
                // sin nada que mandar, tocar a un amigo abre su chat
                is SendAction.OnUserClicked -> if (envio == Outgoing.Nothing) {
                    onClose()
                    onOpenChat(action.userId)
                }
                SendAction.OnPeopleClicked -> {
                    onClose()
                    onOpenPeople()
                }
                else -> viewModel.onAction(action)
            }
        },
    )
}

/** elegir a que amigo o a que grupo se le manda un contenido o una reseña. */
@Composable
fun SendSheet(state: SendState, onAction: (SendAction) -> Unit) {
    val envio = state.envio
    val hayAlgoQueMandar = envio != Outgoing.Nothing

    Sheet({ onAction(SendAction.OnDismiss) }) {
        when (envio) {
            is Outgoing.Content -> MediaHeader(envio.media)
            is Outgoing.Review -> MediaHeader(envio.log.media)
            Outgoing.Nothing -> Text("Mensaje nuevo", style = titular(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        }
        if (hayAlgoQueMandar) {
            Spacer(Modifier.height(14.dp))
            PlainField(state.nota, { onAction(SendAction.OnNotaChanged(it)) }, "Añade un mensaje (opcional)", Modifier.padding(horizontal = 22.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (state.amigos.isEmpty()) {
            EmptyState("Aún no sigues a nadie", "Sigue a alguien para poder escribirle.", Pigment.Cobalt, semilla = 9) {
                PillButton("Buscar gente", { onAction(SendAction.OnPeopleClicked) })
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // primero los grupos, luego los amigos uno a uno
            if (hayAlgoQueMandar) state.grupos.forEach { group ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GroupAvatar(group, 46.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Grupo de ${group.members.size}", color = Pigment.Graphite, fontSize = 13.sp)
                    }
                    SendButton(enviado = group.id in state.enviadoAGrupos) { onAction(SendAction.OnSendToGroupClicked(group.id)) }
                }
            }
            state.amigos.forEach { amigo ->
                UserRow(amigo, onClick = { onAction(SendAction.OnUserClicked(amigo.id)) }) {
                    if (hayAlgoQueMandar) SendButton(enviado = amigo.id in state.enviadoA) { onAction(SendAction.OnSendToUserClicked(amigo.id)) }
                }
            }
        }
    }
}

@Composable
private fun SendButton(enviado: Boolean, onClick: () -> Unit) {
    PillButton(
        text = if (enviado) "Enviado" else "Enviar",
        onClick = onClick,
        enabled = !enviado,
        color = Pigment.Accent,
        icon = if (enviado) Icons.Check else null,
    )
}
