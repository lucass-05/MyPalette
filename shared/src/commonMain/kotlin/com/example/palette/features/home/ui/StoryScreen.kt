package com.example.palette.features.home.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.Media
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.titular
import com.example.palette.core.ui.verbo
import com.example.palette.core.util.ahora
import com.example.palette.core.util.haceCuanto
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.home.presentation.StoryAction
import com.example.palette.features.home.presentation.StoryState
import com.example.palette.features.home.presentation.StoryViewModel

@Composable
fun StoryScreenRoot(
    userId: String,
    cola: List<String>,
    onClose: () -> Unit,
    onOpenStory: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenMedia: (Media) -> Unit,
    onReply: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { StoryViewModel(userId, container.feedRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // la historia de esta persona ya no existe: no hay nada que enseñar
    if (state.historia == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val puesto = cola.indexOf(userId)
    StoryScreen(
        state = state,
        onAction = { action ->
            when (action) {
                // como en instagram: al acabar los logs de una persona se pasa a la siguiente historia de la fila
                StoryAction.OnNextTapped -> if (state.enElUltimo) cola.getOrNull(puesto + 1)?.let(onOpenStory) ?: onClose()
                StoryAction.OnPreviousTapped -> if (state.index == 0) cola.getOrNull(puesto - 1)?.let(onOpenStory)
                StoryAction.OnCloseClicked -> onClose()
                StoryAction.OnUserClicked -> onOpenUser(userId)
                StoryAction.OnVerFichaClicked -> state.log?.let { onOpenMedia(it.media) }
                StoryAction.OnResponderClicked -> onReply(userId)
            }
            // moverse dentro de los logs de esta persona lo lleva el viewmodel
            viewModel.onAction(action)
        },
        modifier = modifier,
    )
}

/** los ultimos logs de un amigo a pantalla completa, uno detras de otro. */
@Composable
fun StoryScreen(state: StoryState, onAction: (StoryAction) -> Unit, modifier: Modifier = Modifier) {
    val historia = state.historia ?: return
    val log = state.log ?: return
    val user = historia.user
    val media = log.media
    val tint = Pigment.of(media.type)
    val ink = tint.legible()
    // los gestos y el temporizador se quedan con la funcion del momento en que se crearon;
    // con rememberUpdatedState llaman siempre a la ultima
    val accion by rememberUpdatedState(onAction)

    // la barra de progreso es una animacion, asi que vive en la pantalla: cuando se llena pide el siguiente
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(state.index) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(6000, easing = LinearEasing))
        accion(StoryAction.OnNextTapped)
    }

    Box(
        modifier.background(tint).pointerInput(Unit) {
            // tocar la parte derecha avanza; el tercio izquierdo vuelve atras
            detectTapGestures { offset -> accion(if (offset.x > size.width / 3) StoryAction.OnNextTapped else StoryAction.OnPreviousTapped) }
        },
    ) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                historia.logs.forEachIndexed { i, _ ->
                    val filled = when {
                        i < state.index -> 1f
                        i == state.index -> progreso.value
                        else -> 0f
                    }
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(ink.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(filled).background(ink))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clickable { onAction(StoryAction.OnUserClicked) }, verticalAlignment = Alignment.CenterVertically) {
                    Avatar(user, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(user.name, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${media.type.verbo} hace ${haceCuanto(log.createdAt)}".replace("hace ahora", "ahora mismo"), color = ink.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                IconAction(Icons.Close, "Cerrar", { onAction(StoryAction.OnCloseClicked) }, tint = ink)
            }

            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CoverByHeight(media, 290.dp, Modifier.clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.height(22.dp))
                Text(media.title, style = titular(28.sp, ink), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Pigment.Paper).padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Stars(log.stars, size = 18.dp)
                }
                if (log.review.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(log.review, color = ink, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center, maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                PillButton("Ver ficha", { onAction(StoryAction.OnVerFichaClicked) }, color = ink)
                PillButton("Responder", { onAction(StoryAction.OnResponderClicked) }, color = ink, icon = Icons.Send)
            }
        }
    }
}
