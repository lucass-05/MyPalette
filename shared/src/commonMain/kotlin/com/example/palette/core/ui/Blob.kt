package com.example.palette.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.palette.core.model.Media
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// las manchas son un Path: unos cuantos puntos colocados alrededor de un centro, cada uno a una
// distancia algo distinta, unidos con curvas de bezier para que no haya picos.
// Random(semilla) da siempre los mismos numeros para la misma semilla, por eso una mancha no cambia.

/**
 * contorno cerrado e irregular, como una gota de pintura aplastada.
 * la misma [seed] da siempre la misma mancha, asi cada log conserva su forma.
 */
fun trazoMancha(semilla: Int, size: Size, ondulacion: Float = 0.2f, lobulos: Int = 7): Path {
    val azar = Random(semilla)
    val cx = size.width / 2
    val cy = size.height / 2
    val paso = 2 * PI / lobulos
    val giro = azar.nextDouble() * paso
    val puntos = List(lobulos) { i ->
        val angulo = giro + i * paso + (azar.nextDouble() - 0.5) * paso * 0.35
        val alcance = 1f - ondulacion * azar.nextFloat()
        Offset(cx + cx * alcance * cos(angulo).toFloat(), cy + cy * alcance * sin(angulo).toFloat())
    }
    // catmull-rom cerrada pasada a bezier: pasa por todos los puntos sin picos
    val ruta = Path()
    ruta.moveTo(puntos[0].x, puntos[0].y)
    for (i in puntos.indices) {
        val p0 = puntos[(i - 1 + lobulos) % lobulos]
        val p1 = puntos[i]
        val p2 = puntos[(i + 1) % lobulos]
        val p3 = puntos[(i + 2) % lobulos]
        ruta.cubicTo(
            p1.x + (p2.x - p0.x) / 5f, p1.y + (p2.y - p0.y) / 5f,
            p2.x - (p3.x - p1.x) / 5f, p2.y - (p3.y - p1.y) / 5f,
            p2.x, p2.y,
        )
    }
    ruta.close()
    return ruta
}

class BlobShape(private val semilla: Int, private val ondulacion: Float = 0.2f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(trazoMancha(semilla, size, ondulacion))
}

// las formas posibles de una foto de perfil. son pocas y fijas para que se reconozcan.
private val formasAvatar = listOf(
    BlobShape(semilla = 5, ondulacion = 0.17f),
    BlobShape(semilla = 23, ondulacion = 0.17f),
    BlobShape(semilla = 48, ondulacion = 0.17f),
    BlobShape(semilla = 71, ondulacion = 0.17f),
    BlobShape(semilla = 104, ondulacion = 0.17f),
)

/** forma de avatar numero [variant]; al pasarse del final vuelve a empezar. */
fun formaAvatar(variante: Int): Shape = formasAvatar[((variante % formasAvatar.size) + formasAvatar.size) % formasAvatar.size]

/** mancha de color con un par de salpicaduras alrededor. */
fun DrawScope.pintarSalpicadura(semilla: Int, color: Color) {
    drawPath(trazoMancha(semilla * 31 + 7, size, ondulacion = 0.14f, lobulos = 9), color)
    val azar = Random(semilla * 17 + 3)
    val r = size.minDimension / 2
    repeat(3) {
        val angulo = azar.nextDouble() * 2 * PI
        val distancia = r * (0.96f + azar.nextFloat() * 0.08f)
        val gota = r * (0.035f + azar.nextFloat() * 0.05f)
        drawCircle(color, gota, Offset(center.x + distancia * cos(angulo).toFloat(), center.y + distancia * sin(angulo).toFloat()))
    }
}

/** portada recortada con forma de mancha de pintura. la misma [seed] da siempre la misma forma. */
@Composable
fun PaintBlob(media: Media, semilla: Int, modifier: Modifier = Modifier) {
    // la mancha se estira a la proporcion de la portada (un poster sale alto, un disco cuadrado)
    // y con poco relieve en el borde, para recortar lo minimo de la imagen
    val shape = remember(semilla) { BlobShape(semilla, ondulacion = 0.11f) }
    Cover(media, modifier.aspectRatio(media.type.proporcion).clip(shape), shape = shape)
}

/** mancha lisa, sin imagen: fondos, avatares y estados vacios. */
@Composable
fun Splat(semilla: Int, color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.matchParentSize()) { pintarSalpicadura(semilla, color) }
        content()
    }
}
