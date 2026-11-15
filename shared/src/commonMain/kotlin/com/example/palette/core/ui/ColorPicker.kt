package com.example.palette.core.ui



import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** tono (0-360), saturacion y brillo (0-1) de un color. */
private fun Color.toHsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val tono = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta).mod(6f))
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return Triple(tono, if (max == 0f) 0f else delta / max, max)
}

fun Color.toHex(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

private fun leerHex(text: String): Color? {
    val digitos = text.trim().removePrefix("#")
    if (digitos.length != 6) return null
    return digitos.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

private fun DrawScope.tirador(at: Offset, fill: Color) {
    drawCircle(Color.White, 11.dp.toPx(), at)
    drawCircle(Color.Black.copy(alpha = 0.25f), 11.dp.toPx(), at, style = Stroke(1.dp.toPx()))
    drawCircle(fill, 8.dp.toPx(), at)
}

/**
 * selector de cualquier color: un cuadro para saturacion y brillo, una barra para el tono
 * y el codigo hexadecimal por si se quiere pegar uno exacto.
 */
@Composable
fun ColorPicker(color: Color, alCambiar: (Color) -> Unit, modifier: Modifier = Modifier) {
    // el tono se guarda aparte: un gris o un negro no tienen tono y al arrastrar se perderia
    val start = remember { color.toHsv() }
    var tono by remember { mutableFloatStateOf(start.first) }
    var saturacion by remember { mutableFloatStateOf(start.second) }
    var value by remember { mutableFloatStateOf(start.third) }
    var hex by remember { mutableStateOf(color.toHex()) }
    // los gestos se registran una sola vez, asi que tienen que llamar siempre al onchange mas reciente
    val avisar by rememberUpdatedState(alCambiar)

    fun emitir() {
        val elegida = Color.hsv(tono, saturacion, value)
        hex = elegida.toHex()
        avisar(elegida)
    }

    Column(modifier) {
        Canvas(
            Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp)).pointerInput(Unit) {
                awaitEachGesture {
                    fun pick(at: Offset) {
                        saturacion = (at.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (at.y / size.height).coerceIn(0f, 1f)
                        emitir()
                    }
                    val down = awaitFirstDown()
                    down.consume()
                    pick(down.position)
                    drag(down.id) { cambio ->
                        cambio.consume()
                        pick(cambio.position)
                    }
                }
            },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(tono, 1f, 1f))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            tirador(Offset(saturacion * size.width, (1f - value) * size.height), Color.hsv(tono, saturacion, value))
        }
        Spacer(Modifier.height(14.dp))
        Canvas(
            Modifier.fillMaxWidth().height(26.dp).pointerInput(Unit) {
                awaitEachGesture {
                    fun pick(at: Offset) {
                        tono = (at.x / size.width).coerceIn(0f, 1f) * 360f
                        emitir()
                    }
                    val down = awaitFirstDown()
                    down.consume()
                    pick(down.position)
                    drag(down.id) { cambio ->
                        cambio.consume()
                        pick(cambio.position)
                    }
                }
            },
        ) {
            val bar = size.height * 0.55f
            drawRoundRect(
                Brush.horizontalGradient(List(13) { Color.hsv(it * 30f % 360f, 1f, 1f) }),
                topLeft = Offset(0f, (size.height - bar) / 2),
                size = Size(size.width, bar),
                cornerRadius = CornerRadius(bar / 2),
            )
            tirador(Offset((tono / 360f) * size.width, size.height / 2), Color.hsv(tono, 1f, 1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color.hsv(tono, saturacion, value)).border(1.dp, Pigment.Line, CircleShape))
            Box(Modifier.width(120.dp).clip(RoundedCornerShape(12.dp)).background(Pigment.Mist).height(38.dp), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = hex,
                    onValueChange = { typed ->
                        hex = typed.take(7)
                        leerHex(typed)?.let { parsed ->
                            val (h, s, v) = parsed.toHsv()
                            if (s > 0f && v > 0f) tono = h
                            saturacion = s
                            value = v
                            alCambiar(parsed)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = Pigment.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(Pigment.Ink),
                )
            }
            Text("Código del color", color = Pigment.Graphite, fontSize = 13.sp)
        }
    }
}

/** redondel con el color actual; se usa como boton para abrir el selector. */
@Composable
fun Swatch(color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(34.dp).clip(CircleShape).background(color).border(1.dp, Pigment.Line, CircleShape).clickable(onClick = onClick))
}
