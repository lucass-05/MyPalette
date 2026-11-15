package com.example.palette.features.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.MediaHeader
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.SheetRow
import com.example.palette.core.ui.Stars
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.library.presentation.LogMenuAction
import com.example.palette.features.library.presentation.LogMenuState
import com.example.palette.features.library.presentation.LogMenuViewModel
import com.example.palette.features.messages.domain.model.Outgoing

@Composable
fun LogMenuSheetRoot(
    log: LogEntry,
    onClose: () -> Unit,
    onReview: (Media) -> Unit,
    onSend: (Outgoing) -> Unit,
    onOpenMedia: (Media) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LogMenuViewModel(log, container.libraryRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LogMenuSheet(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            when (action) {
                // pedir confirmacion o echarse atras no cierra la hoja; todo lo demas, si
                LogMenuAction.OnBorrarClicked, LogMenuAction.OnBorrarCancelado -> {}
                LogMenuAction.OnEditarClicked -> {
                    onClose()
                    onReview(log.media)
                }
                LogMenuAction.OnEnviarClicked -> {
                    onClose()
                    onSend(Outgoing.Review(log))
                }
                LogMenuAction.OnVerFichaClicked -> {
                    onClose()
                    onOpenMedia(log.media)
                }
                else -> onClose()
            }
        },
    )
}

/** que se puede hacer con un log propio desde el perfil. */
@Composable
fun LogMenuSheet(state: LogMenuState, onAction: (LogMenuAction) -> Unit) {
    val log = state.log

    Sheet({ onAction(LogMenuAction.OnDismiss) }) {
        MediaHeader(log.media)
        Row(Modifier.padding(start = 22.dp, top = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Stars(log.stars, size = 16.dp)
            if (log.hidden) {
                Icon(Icons.EyeOff, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(14.dp))
                Text("Oculto en tu perfil", color = Pigment.Graphite, fontSize = 13.sp)
            }
        }
        if (log.review.isNotBlank()) {
            Text(log.review, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
        }
        Spacer(Modifier.height(6.dp))
        SheetRow(if (log.pinned) Icons.PinOff else Icons.Pin, if (log.pinned) "Dejar de fijar" else "Fijar arriba del perfil", { onAction(LogMenuAction.OnFijarClicked) })
        SheetRow(if (log.hidden) Icons.Eye else Icons.EyeOff, if (log.hidden) "Volver a mostrar en el perfil" else "Ocultar del perfil", { onAction(LogMenuAction.OnOcultarClicked) })
        SheetRow(Icons.Pencil, "Editar nota y reseña", { onAction(LogMenuAction.OnEditarClicked) })
        SheetRow(Icons.Send, "Enviar a un amigo", { onAction(LogMenuAction.OnEnviarClicked) })
        SheetRow(Icons.ChevronRight, "Ver la ficha", { onAction(LogMenuAction.OnVerFichaClicked) })
        if (state.confirmandoBorrado) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Se borra la nota y la reseña.", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                PillButton("Cancelar", { onAction(LogMenuAction.OnBorrarCancelado) }, filled = false)
                PillButton("Eliminar", { onAction(LogMenuAction.OnBorrarConfirmado) }, color = Pigment.Cadmium)
            }
        } else {
            SheetRow(Icons.Trash, "Eliminar", { onAction(LogMenuAction.OnBorrarClicked) }, tint = Pigment.Cadmium)
        }
    }
}
