package com.example.palette.features.reviews.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.BackBar
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.subtitulo
import com.example.palette.core.ui.titular
import com.example.palette.core.ui.verbo
import com.example.palette.core.util.fechaTexto
import com.example.palette.core.util.haceCuanto
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.reviews.domain.model.Comentario
import com.example.palette.features.reviews.domain.model.Resena
import com.example.palette.features.reviews.presentation.ReviewDetailAction
import com.example.palette.features.reviews.presentation.ReviewDetailState
import com.example.palette.features.reviews.presentation.ReviewDetailViewModel

@Composable
fun ReviewDetailScreenRoot(
    logId: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenMedia: (Media) -> Unit,
    onEdit: (Media) -> Unit,
    onSend: (LogEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ReviewDetailViewModel(logId, container.reviewsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReviewDetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ReviewDetailAction.OnBackClicked -> onBack()
                is ReviewDetailAction.OnUserClicked -> onOpenUser(action.userId)
                is ReviewDetailAction.OnMediaClicked -> onOpenMedia(action.media)
                is ReviewDetailAction.OnEditarClicked -> onEdit(action.media)
                is ReviewDetailAction.OnSendClicked -> onSend(action.log)
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

/** una reseña entera: la nota, el texto, los me gusta y los comentarios de la gente. */
@Composable
fun ReviewDetailScreen(state: ReviewDetailState, onAction: (ReviewDetailAction) -> Unit, modifier: Modifier = Modifier) {
    val resena = state.resena

    Column(modifier.navigationBarsPadding().imePadding()) {
        BackBar("Reseña", { onAction(ReviewDetailAction.OnBackClicked) }) {
            if (resena != null) IconAction(Icons.Send, "Enviar a un amigo", { onAction(ReviewDetailAction.OnSendClicked(resena.log)) })
        }
        if (resena == null) {
            // mientras llega no se enseña nada; si no llega es que se ha borrado o se ha ocultado
            if (state.cargada) EmptyState("Esta reseña ya no está", "Puede que su autor la haya borrado u ocultado.", Pigment.Graphite, semilla = 60)
            return@Column
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 12.dp)) {
            item { Cabecera(resena, onAction) }
            item {
                HorizontalDivider(Modifier.padding(top = 4.dp), color = Pigment.Line)
                Text(
                    if (resena.comentarios.isEmpty()) "Comentarios" else "Comentarios (${resena.comentarios.size})",
                    style = titular(19.sp),
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
                )
                if (resena.comentarios.isEmpty()) {
                    Text(
                        if (resena.propia) "Cuando alguien comente tu reseña aparecerá aquí." else "Sé el primero en decir algo.",
                        color = Pigment.Graphite, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            items(resena.comentarios, key = { it.id }) { comentario ->
                ComentarioRow(comentario, onAction)
            }
        }

        // la caja para comentar va fija abajo, como en un chat
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            PlainField(state.borrador, { onAction(ReviewDetailAction.OnBorradorChanged(it)) }, "Añade un comentario", Modifier.weight(1f), singleLine = false)
            IconAction(
                Icons.Send, "Publicar comentario",
                onClick = { onAction(ReviewDetailAction.OnComentarClicked) },
                tint = if (state.borrador.isBlank()) Pigment.Graphite else Pigment.Accent,
            )
        }
    }
}

@Composable
private fun Cabecera(resena: Resena, onAction: (ReviewDetailAction) -> Unit) {
    val log = resena.log
    val media = log.media
    val autor = resena.autor

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.clickable { onAction(ReviewDetailAction.OnUserClicked(autor.id)) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(autor, 38.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(if (resena.propia) "Tú" else autor.name) }
                    withStyle(SpanStyle(color = Pigment.Graphite)) { append(if (resena.propia) " ${media.type.verbo.replace("ha ", "has ")}" else " ${media.type.verbo}") }
                },
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.clickable { onAction(ReviewDetailAction.OnMediaClicked(media)) }) {
            CoverByHeight(media, 170.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(media.title, style = titular(24.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(media.subtitulo(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                Stars(log.stars, size = 19.dp)
                Spacer(Modifier.height(8.dp))
                Text(fechaTexto(log.createdAt), color = Pigment.Graphite, fontSize = 12.sp)
            }
        }
        if (log.review.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(log.review, fontSize = 16.sp, lineHeight = 24.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(
                if (resena.meGusta) Icons.HeartFilled else Icons.Heart,
                if (resena.meGusta) "Quitar me gusta" else "Me gusta",
                { onAction(ReviewDetailAction.OnLikeClicked) },
                tint = if (resena.meGusta) Pigment.Cadmium else Pigment.Ink,
            )
            Text(
                when (resena.meGustas) {
                    0 -> "Sin me gusta"
                    1 -> "1 me gusta"
                    else -> "${resena.meGustas} me gusta"
                },
                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (resena.propia) {
                PillButton("Editar", { onAction(ReviewDetailAction.OnEditarClicked(media)) }, filled = false, icon = Icons.Pencil)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ComentarioRow(comentario: Comentario, onAction: (ReviewDetailAction) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)) {
        Avatar(comentario.autor, 34.dp, Modifier.clickable { onAction(ReviewDetailAction.OnUserClicked(comentario.autor.id)) })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(comentario.autor.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(haceCuanto(comentario.at), color = Pigment.Graphite, fontSize = 12.sp)
            }
            Text(comentario.texto, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (comentario.puedoBorrar) {
            IconAction(Icons.Trash, "Borrar comentario", { onAction(ReviewDetailAction.OnBorrarComentarioClicked(comentario.id)) }, tint = Pigment.Graphite)
        }
    }
}
