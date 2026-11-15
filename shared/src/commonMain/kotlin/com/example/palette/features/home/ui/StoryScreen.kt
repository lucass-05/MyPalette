package com.example.palette.features.home.ui

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
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette


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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** los ultimos logs de un amigo a pantalla completa, uno detras de otro. */
@Composable
fun StoryScreen(userId: String, cola: List<String>, modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val user = almacen.user(userId)
    // se congela la lista al abrir: marcar como visto no debe moverla
    val logs = remember(userId) { almacen.historiaDe(userId) }
    if (user == null || logs.isEmpty()) {
        LaunchedEffect(Unit) { navegacion.volver() }
        return
    }

    var index by remember { mutableIntStateOf(0) }
    val puesto = cola.indexOf(userId)

    // como en instagram: al acabar los logs de una persona se pasa a la siguiente historia de la fila
    fun siguiente() {
        val following = cola.getOrNull(puesto + 1)
        when {
            index < logs.lastIndex -> index++
            following != null -> navegacion.sustituirCima(Dest.Story(following, cola))
            else -> navegacion.volver()
        }
    }

    fun anterior() {
        val before = cola.getOrNull(puesto - 1)
        when {
            index > 0 -> index--
            before != null -> navegacion.sustituirCima(Dest.Story(before, cola))
        }
    }

    val progreso = remember { Animatable(0f) }
    val log = logs[index]
    val media = log.media
    val tint = Pigment.of(media.type)
    val ink = tint.legible()

    LaunchedEffect(index) {
        almacen.marcarHistoriaVista(userId, log.createdAt)
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(6000, easing = LinearEasing))
        siguiente()
    }

    Box(
        modifier.background(tint).pointerInput(logs.size) {
            // tocar la parte derecha avanza; el tercio izquierdo vuelve atras
            detectTapGestures { offset -> if (offset.x > size.width / 3) siguiente() else anterior() }
        },
    ) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                logs.forEachIndexed { i, _ ->
                    val filled = when {
                        i < index -> 1f
                        i == index -> progreso.value
                        else -> 0f
                    }
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(ink.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(filled).background(ink))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clickable { navegacion.volver(); navegacion.abrir(Dest.Profile(userId)) }, verticalAlignment = Alignment.CenterVertically) {
                    Avatar(user, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(user.name, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${media.type.verbo} hace ${haceCuanto(log.createdAt)}".replace("hace ahora", "ahora mismo"), color = ink.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                IconAction(Icons.Close, "Cerrar", { navegacion.volver() }, tint = ink)
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
                PillButton("Ver ficha", { navegacion.volver(); navegacion.abrir(Dest.Detail(media)) }, color = ink)
                PillButton("Responder", { navegacion.volver(); navegacion.abrir(Dest.Chat(userId)) }, color = ink, icon = Icons.Send)
            }
        }
    }
}
