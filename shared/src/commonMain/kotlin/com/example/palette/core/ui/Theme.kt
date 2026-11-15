package com.example.palette.core.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.palette.core.model.MediaType
import com.example.palette.core.model.ThemeColors
import org.jetbrains.compose.resources.Font
import palette.shared.generated.resources.Res
import palette.shared.generated.resources.youngserif_regular

// todos los colores salen de Pigment. como Pigment.colors es estado de compose,
// cambiar un color en ajustes repinta la app entera sin tener que avisar a nadie.

/**
 * los colores de la app. todos salen de [colors], que el usuario puede cambiar en ajustes:
 * como es estado de compose, cualquier pantalla que lea un color se repinta sola al cambiarlo.
 * por defecto: blanco de lienzo, tinta para el texto y seis tubos de pintura, uno por tipo de contenido.
 */
object Pigment {
    var colors by mutableStateOf(ThemeColors())

    val Paper get() = Color(colors.paper)
    val Ink get() = Color(colors.ink)
    val Accent get() = Color(colors.accent)

    // los grises son mezclas de fondo y texto, asi acompañan a cualquier combinacion
    val Mist get() = lerp(Paper, Ink, 0.055f)
    val Line get() = lerp(Paper, Ink, 0.11f)
    val Graphite get() = lerp(Paper, Ink, 0.55f)

    val Cadmium get() = Color(colors.movie)
    val Cobalt get() = Color(colors.series)
    val Viridian get() = Color(colors.game)
    val Ochre get() = Color(colors.album)
    val Violet get() = Color(colors.song)
    val Magenta get() = Color(colors.book)

    /** las estrellas son siempre doradas. */
    val Star = Color(0xFFFFB81F)

    fun of(type: MediaType): Color = when (type) {
        MediaType.MOVIE -> Cadmium
        MediaType.SERIES -> Cobalt
        MediaType.GAME -> Viridian
        MediaType.ALBUM -> Ochre
        MediaType.SONG -> Violet
        MediaType.BOOK -> Magenta
    }
}

/** blanco o casi negro, lo que se lea mejor encima de este color. */
fun Color.legible(): Color = if (luminance() > 0.45f) Color(0xFF17171A) else Color.White

/** el color como se guarda: argb en un long (0xffrrggbb). */
fun Color.aGuardado(): Long = toArgb().toLong() and 0xFFFFFFFF

/** contraste wcag entre dos colores, de 1 (iguales) a 21 (negro sobre blanco). */
fun contraste(a: Color, b: Color): Float {
    val hi = maxOf(a.luminance(), b.luminance())
    val lo = minOf(a.luminance(), b.luminance())
    return (hi + 0.05f) / (lo + 0.05f)
}

val MediaType.icon: ImageVector
    get() = when (this) {
        MediaType.MOVIE -> Icons.Film
        MediaType.SERIES -> Icons.Tv
        MediaType.GAME -> Icons.Gamepad
        MediaType.ALBUM -> Icons.Disc
        MediaType.SONG -> Icons.AudioLines
        MediaType.BOOK -> Icons.Book
    }

/** proporcion ancho/alto de la portada de cada tipo. */
val MediaType.proporcion: Float
    get() = when (this) {
        MediaType.MOVIE, MediaType.SERIES, MediaType.BOOK -> 2f / 3f
        MediaType.GAME -> 3f / 4f
        MediaType.ALBUM, MediaType.SONG -> 1f
    }

val LocalDisplayFont = staticCompositionLocalOf<FontFamily> { FontFamily.Serif }

/** young serif para titulos; el cuerpo usa la fuente del sistema. */
@Composable
fun titular(size: TextUnit, color: Color = Pigment.Ink): TextStyle =
    TextStyle(fontFamily = LocalDisplayFont.current, fontSize = size, lineHeight = size * 1.15f, color = color, letterSpacing = (-0.2).sp)

@Composable
fun PaletteTheme(content: @Composable () -> Unit) {
    val fuenteTitular = FontFamily(Font(Res.font.youngserif_regular))
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pigment.Ink,
            onPrimary = Pigment.Paper,
            secondary = Pigment.Accent,
            background = Pigment.Paper,
            onBackground = Pigment.Ink,
            surface = Pigment.Paper,
            onSurface = Pigment.Ink,
            surfaceVariant = Pigment.Mist,
            onSurfaceVariant = Pigment.Graphite,
            surfaceContainerLow = Pigment.Paper,
            outline = Pigment.Line,
            outlineVariant = Pigment.Line,
        ),
    ) {
        CompositionLocalProvider(
            LocalDisplayFont provides fuenteTitular,
            LocalContentColor provides Pigment.Ink,
            content = content,
        )
    }
}
