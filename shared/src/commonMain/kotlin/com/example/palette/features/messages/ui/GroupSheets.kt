package com.example.palette.features.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.ME
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.SheetRow
import com.example.palette.core.ui.UserRow
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.recordarSelectorDeFotos
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.messages.presentation.GroupInfoAction
import com.example.palette.features.messages.presentation.GroupInfoState
import com.example.palette.features.messages.presentation.GroupInfoViewModel
import com.example.palette.features.messages.presentation.NewGroupAction
import com.example.palette.features.messages.presentation.NewGroupState
import com.example.palette.features.messages.presentation.NewGroupViewModel

@Composable
fun NewGroupSheetRoot(onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { NewGroupViewModel(container.messagesRepository, container.peopleRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elegirFoto = recordarSelectorDeFotos { viewModel.onAction(NewGroupAction.OnFotoElegida(it)) }

    // el viewmodel avisa por el estado de que el grupo ya existe y la hoja se cierra
    LaunchedEffect(state.creado) { if (state.creado) onClose() }

    NewGroupSheet(
        state = state,
        onAction = { action -> if (action == NewGroupAction.OnDismiss) onClose() else viewModel.onAction(action) },
        onElegirFoto = elegirFoto,
    )
}

/** crear un grupo: nombre, foto opcional y que amigos entran. */
@Composable
fun NewGroupSheet(state: NewGroupState, onAction: (NewGroupAction) -> Unit, onElegirFoto: () -> Unit) {
    Sheet({ onAction(NewGroupAction.OnDismiss) }) {
        Text("Grupo nuevo", style = titular(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(12.dp))
        PlainField(state.name, { onAction(NewGroupAction.OnNameChanged(it)) }, "Nombre del grupo", Modifier.padding(horizontal = 22.dp))
        Row(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton(if (state.foto == null) "Añadir foto" else "Foto elegida. Cambiar", onElegirFoto, filled = false)
        }
        if (state.amigos.isEmpty()) {
            EmptyState("Aún no sigues a nadie", "Sigue a alguien para poder meterlo en un grupo.", Pigment.Cobalt, semilla = 9)
        }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            state.amigos.forEach { amigo ->
                val dentro = amigo.id in state.elegidos
                UserRow(amigo, onClick = { onAction(NewGroupAction.OnMemberToggled(amigo.id)) }) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (dentro) Pigment.Accent else Pigment.Mist),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dentro) Icon(Icons.Check, contentDescription = "Elegido", tint = Pigment.Accent.legible(), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        state.problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) }
        PillButton(
            if (state.ocupado) "Creando…" else "Crear grupo",
            { onAction(NewGroupAction.OnCrearClicked) },
            enabled = !state.ocupado && state.valido,
            color = Pigment.Accent,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun GroupInfoSheetRoot(groupId: String, onClose: () -> Unit, onOpenUser: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { GroupInfoViewModel(groupId, container.messagesRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elegirFoto = recordarSelectorDeFotos { viewModel.onAction(GroupInfoAction.OnFotoElegida(it)) }

    // al salir del grupo deja de existir para mi: se cierra la hoja (y el chat de debajo se cierra por su cuenta)
    LaunchedEffect(state.existe) { if (!state.existe) onClose() }

    GroupInfoSheet(
        state = state,
        onAction = { action ->
            when (action) {
                GroupInfoAction.OnDismiss -> onClose()
                is GroupInfoAction.OnMemberClicked -> if (action.userId != ME) {
                    onClose()
                    onOpenUser(action.userId)
                }
                else -> viewModel.onAction(action)
            }
        },
        onElegirFoto = elegirFoto,
    )
}

/** quien esta en el grupo, cambiarle la foto y salir (o borrarlo si es mio). */
@Composable
fun GroupInfoSheet(state: GroupInfoState, onAction: (GroupInfoAction) -> Unit, onElegirFoto: () -> Unit) {
    val group = state.group ?: return

    Sheet({ onAction(GroupInfoAction.OnDismiss) }) {
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            GroupAvatar(group, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(group.name, style = titular(22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${group.members.size} personas", color = Pigment.Graphite, fontSize = 13.sp)
            }
        }
        // la foto solo la puede cambiar quien creo el grupo: es quien tiene permiso en el servidor
        if (state.mio) {
            PillButton(if (state.ocupado) "Subiendo…" else "Cambiar foto del grupo", onElegirFoto, filled = false, enabled = !state.ocupado, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        state.problema?.let { Text(it, color = Pigment.Cadmium, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            state.miembros.forEach { miembro ->
                UserRow(miembro, onClick = { onAction(GroupInfoAction.OnMemberClicked(miembro.id)) }) {
                    if (miembro.id == group.ownerId) Text("Creó el grupo", color = Pigment.Graphite, fontSize = 12.sp)
                }
            }
        }
        if (state.confirmando) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.mio) "Se borra para todos, con sus mensajes." else "Dejarás de ver sus mensajes.", fontSize = 14.sp, modifier = Modifier.weight(1f))
                PillButton("Cancelar", { onAction(GroupInfoAction.OnSalirCancelado) }, filled = false)
                PillButton(if (state.mio) "Borrar" else "Salir", { onAction(GroupInfoAction.OnSalirConfirmado) }, color = Pigment.Cadmium)
            }
        } else {
            SheetRow(Icons.Trash, if (state.mio) "Borrar el grupo" else "Salir del grupo", { onAction(GroupInfoAction.OnSalirClicked) }, tint = Pigment.Cadmium)
        }
    }
}
