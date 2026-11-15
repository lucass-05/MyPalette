package com.example.palette.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.pintarSalpicadura
import com.example.palette.features.profile.domain.model.Perfil

val Screen.pigmento: Color
    get() = when (this) {
        Screen.HOME -> Pigment.Cadmium
        Screen.DISCOVER -> Pigment.Violet
        Screen.LOG -> Pigment.Ochre
        Screen.ACTIVITY -> Pigment.Cobalt
        Screen.PROFILE -> Pigment.Viridian
    }

private val Screen.label: String
    get() = when (this) {
        Screen.HOME -> "Inicio"
        Screen.DISCOVER -> "Descubrir"
        Screen.LOG -> "Registrar"
        Screen.ACTIVITY -> "Mensajes"
        Screen.PROFILE -> "Perfil"
    }

/**
 * barra inferior de cristal. [backdrop] es la capa donde se ha grabado el contenido de la pantalla:
 * aqui se vuelve a pintar el trozo que queda debajo de la barra, desenfocado, y encima un velo del color de fondo.
 * [backdropOrigin] es la posicion de esa capa en la ventana.
 */
@Composable
fun GlassBar(
    fondo: GraphicsLayer,
    origenFondo: Offset,
    current: Screen,
    unread: Int,
    alElegirPantalla: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    var posicion by remember { mutableStateOf(Offset.Zero) }
    // abajo y a lo ancho en el movil; a la izquierda y en columna en pantallas anchas
    val marco = if (vertical) {
        modifier.padding(start = 20.dp).width(70.dp).height(392.dp)
    } else {
        modifier.navigationBarsPadding().padding(start = 22.dp, end = 22.dp, bottom = 14.dp).fillMaxWidth().height(66.dp)
    }

    Box(
        marco
            .onGloballyPositioned { posicion = it.positionInRoot() - origenFondo }
            .clip(shape),
    ) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    renderEffect = BlurEffect(22.dp.toPx(), 22.dp.toPx(), TileMode.Clamp)
                    clip = true
                }
                .drawBehind { translate(-posicion.x, -posicion.y) { drawLayer(fondo) } }
        )
        Box(
            Modifier.matchParentSize()
                .background(Brush.verticalGradient(listOf(Pigment.Paper.copy(alpha = 0.78f), Pigment.Paper.copy(alpha = 0.6f))))
                .border(1.dp, Brush.verticalGradient(listOf(Pigment.Paper, Pigment.Line)), shape)
        )
        if (vertical) {
            Column(Modifier.matchParentSize().padding(vertical = 6.dp)) {
                Screen.entries.forEach { pantalla ->
                    BarItem(pantalla, pantalla == current, dot = pantalla == Screen.ACTIVITY && unread > 0, onClick = { alElegirPantalla(pantalla) }, modifier = Modifier.weight(1f).fillMaxWidth())
                }
            }
        } else {
            Row(Modifier.matchParentSize().padding(horizontal = 6.dp)) {
                Screen.entries.forEach { pantalla ->
                    BarItem(pantalla, pantalla == current, dot = pantalla == Screen.ACTIVITY && unread > 0, onClick = { alElegirPantalla(pantalla) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun BarItem(pantalla: Screen, selected: Boolean, dot: Boolean, onClick: () -> Unit, modifier: Modifier) {
    // la mancha de color crece al entrar en la pestaña
    val crecer by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    Box(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClickLabel = pantalla.label,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(50.dp)) {
            if (crecer > 0.01f) scale(crecer) { pintarSalpicadura(pantalla.ordinal + 11, pantalla.pigmento) }
        }
        Icon(
            getIconForScreen(pantalla),
            contentDescription = pantalla.label,
            tint = if (selected) pantalla.pigmento.legible() else Pigment.Ink,
            modifier = Modifier.size(23.dp),
        )
        if (dot && !selected) {
            Box(Modifier.offset(x = 11.dp, y = (-11).dp).size(9.dp).clip(CircleShape).background(Pigment.Cadmium).border(1.5.dp, Pigment.Paper, CircleShape))
        }
    }
}
