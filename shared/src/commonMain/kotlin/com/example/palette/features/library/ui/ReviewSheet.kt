package com.example.palette.features.library.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.Media
import com.example.palette.core.ui.MediaHeader
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.estrellasTexto
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.library.presentation.ReviewAction
import com.example.palette.features.library.presentation.ReviewState
import com.example.palette.features.library.presentation.ReviewViewModel

@Composable
fun ReviewSheetRoot(media: Media, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ReviewViewModel(media, container.libraryRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReviewSheet(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            // tanto al guardar como al descartar, la hoja se cierra
            if (action == ReviewAction.OnGuardarClicked || action == ReviewAction.OnDismiss) onClose()
        },
    )
}

/** poner estrellas y, si apetece, escribir una reseña. sirve igual para crear que para editar. */
@Composable
fun ReviewSheet(state: ReviewState, onAction: (ReviewAction) -> Unit) {
    Sheet({ onAction(ReviewAction.OnDismiss) }) {
        MediaHeader(state.media)
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Stars(state.stars, size = 38.dp, alCambiar = { onAction(ReviewAction.OnStarsChanged(it)) })
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.stars == 0f) "Toca una estrella. Otro toque en la misma la deja en media." else "${estrellasTexto(state.stars)} de 5",
                color = Pigment.Graphite, fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        PlainField(state.review, { onAction(ReviewAction.OnReviewChanged(it)) }, "Qué te ha parecido (opcional)", Modifier.padding(horizontal = 22.dp), singleLine = false, minHeight = 110.dp)
        Spacer(Modifier.height(16.dp))
        PillButton(
            text = if (state.editando) "Guardar cambios" else "Guardar en mi perfil",
            onClick = { onAction(ReviewAction.OnGuardarClicked) },
            enabled = state.stars > 0f,
            color = Pigment.of(state.media.type),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        )
    }
}
