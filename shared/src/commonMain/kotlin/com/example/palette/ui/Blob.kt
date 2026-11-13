package com.example.palette.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.palette.data.Media
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * contorno cerrado e irregular, como una gota de pintura aplastada.
 * la misma [seed] da siempre la misma mancha, asi cada log conserva su forma.
 */
fun blobPath(seed: Int, size: Size, wobble: Float = 0.2f, lobes: Int = 7): Path {
    val rnd = Random(seed)
    val cx = size.width / 2
    val cy = size.height / 2
    val step = 2 * PI / lobes
    val turn = rnd.nextDouble() * step
    val pts = List(lobes) { i ->
        val angle = turn + i * step + (rnd.nextDouble() - 0.5) * step * 0.35
        val reach = 1f - wobble * rnd.nextFloat()
        Offset(cx + cx * reach * cos(angle).toFloat(), cy + cy * reach * sin(angle).toFloat())
    }
    // catmull-rom cerrada pasada a bezier: pasa por todos los puntos sin picos
    val path = Path()
    path.moveTo(pts[0].x, pts[0].y)
    for (i in pts.indices) {
        val p0 = pts[(i - 1 + lobes) % lobes]
        val p1 = pts[i]
        val p2 = pts[(i + 1) % lobes]
        val p3 = pts[(i + 2) % lobes]
        path.cubicTo(
            p1.x + (p2.x - p0.x) / 5f, p1.y + (p2.y - p0.y) / 5f,
            p2.x - (p3.x - p1.x) / 5f, p2.y - (p3.y - p1.y) / 5f,
            p2.x, p2.y,
        )
    }
    path.close()
    return path
}

class BlobShape(private val seed: Int, private val wobble: Float = 0.2f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(blobPath(seed, size, wobble))
}

// las formas posibles de una foto de perfil. son pocas y fijas para que se reconozcan.
private val avatarShapes = listOf(
    BlobShape(seed = 5, wobble = 0.17f),
    BlobShape(seed = 23, wobble = 0.17f),
    BlobShape(seed = 48, wobble = 0.17f),
    BlobShape(seed = 71, wobble = 0.17f),
    BlobShape(seed = 104, wobble = 0.17f),
)

/** forma de avatar numero [variant]; al pasarse del final vuelve a empezar. */
fun avatarShape(variant: Int): Shape = avatarShapes[((variant % avatarShapes.size) + avatarShapes.size) % avatarShapes.size]

/** mancha de color con un par de salpicaduras alrededor. */
fun DrawScope.drawSplat(seed: Int, color: Color) {
    drawPath(blobPath(seed * 31 + 7, size, wobble = 0.14f, lobes = 9), color)
    val rnd = Random(seed * 17 + 3)
    val r = size.minDimension / 2
    repeat(3) {
        val angle = rnd.nextDouble() * 2 * PI
        val dist = r * (0.96f + rnd.nextFloat() * 0.08f)
        val drop = r * (0.035f + rnd.nextFloat() * 0.05f)
        drawCircle(color, drop, Offset(center.x + dist * cos(angle).toFloat(), center.y + dist * sin(angle).toFloat()))
    }
}

/** portada recortada con forma de mancha de pintura. la misma [seed] da siempre la misma forma. */
@Composable
fun PaintBlob(media: Media, seed: Int, modifier: Modifier = Modifier) {
    // la mancha se estira a la proporcion de la portada (un poster sale alto, un disco cuadrado)
    // y con poco relieve en el borde, para recortar lo minimo de la imagen
    val shape = remember(seed) { BlobShape(seed, wobble = 0.11f) }
    Cover(media, modifier.aspectRatio(media.type.coverRatio).clip(shape), shape = shape)
}

/** mancha lisa, sin imagen: fondos, avatares y estados vacios. */
@Composable
fun Splat(seed: Int, color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.matchParentSize()) { drawSplat(seed, color) }
        content()
    }
}
