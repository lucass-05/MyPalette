package com.example.palette.core.ui

import com.example.palette.core.model.Media
import com.example.palette.core.model.User


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun Cover(media: Media, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(6.dp)) {
    val tint = Pigment.of(media.type)
    Box(modifier.clip(shape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(media.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        if (media.imageUrl != null) {
            AsyncImage(
                model = media.imageUrl,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** portada con la proporcion propia de su tipo a partir de una altura. */
@Composable
fun CoverByHeight(media: Media, height: Dp, modifier: Modifier = Modifier) {
    Cover(media, modifier.height(height).aspectRatio(media.type.proporcion))
}

/**
 * foto de perfil con forma de mancha. por defecto cada persona tiene siempre la misma forma;
 * en las listas donde importa el orden (las historias) se pasa la posicion en [variant].
 */
@Composable
fun Avatar(user: User, size: Dp, modifier: Modifier = Modifier, variante: Int = user.id.hashCode()) {
    val color = Color(user.favorite)
    Box(modifier.size(size).clip(formaAvatar(variante)).background(color), contentAlignment = Alignment.Center) {
        val inicial = (user.name.ifBlank { user.username }).take(1).uppercase()
        Text(inicial, style = titular((size.value * 0.46f).sp, color.legible()))
        if (user.photoUrl != null) {
            AsyncImage(user.photoUrl, contentDescription = user.name, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}

/**
 * cinco estrellas con medias. si recibe [onChange] se puede puntuar:
 * un toque pone la estrella entera y otro toque en la misma la deja en media.
 */
@Composable
fun Stars(value: Float, modifier: Modifier = Modifier, size: Dp = 14.dp, alCambiar: ((Float) -> Unit)? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (alCambiar != null) 6.dp else 1.dp)) {
        for (i in 1..5) {
            val fill = (value - (i - 1)).coerceIn(0f, 1f)
            val toque = if (alCambiar == null) Modifier else Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { alCambiar(if (value == i.toFloat()) i - 0.5f else i.toFloat()) }
            Box(toque) {
                Icon(Icons.StarFilled, contentDescription = null, tint = Pigment.Line, modifier = Modifier.size(size))
                if (fill > 0f) {
                    Icon(
                        Icons.StarFilled,
                        contentDescription = null,
                        tint = Pigment.Star,
                        modifier = Modifier.size(size).drawWithContent {
                            clipRect(right = this.size.width * if (fill >= 1f) 1f else 0.5f) { this@drawWithContent.drawContent() }
                        },
                    )
                }
            }
        }
    }
}

fun estrellasTexto(stars: Float): String = if (stars % 1f == 0f) stars.toInt().toString() else stars.toString().replace('.', ',')

/** "8,4" a partir de una nota sobre 10. */
fun notaTexto(score: Double): String {
    val decimas = (score * 10).roundToInt()
    return "${decimas / 10},${decimas % 10}"
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = titular(30.sp), modifier = Modifier.weight(1f))
        actions()
    }
}

/** cabecera de las pantallas que se abren encima de las pestañas. */
@Composable
fun BackBar(title: String, alVolver: () -> Unit, modifier: Modifier = Modifier, ink: Color = Pigment.Ink, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconAction(Icons.Back, "Volver", alVolver, tint = ink)
        Text(title, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
        actions()
    }
}

@Composable
fun IconAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Pigment.Ink) {
    Box(modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    color: Color = Pigment.Ink,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val content = if (filled) color.legible() else color
    val raiz = modifier.heightIn(min = 44.dp).clip(shape)
    val aspecto = when {
        !enabled -> raiz.background(Pigment.Mist)
        filled -> raiz.background(color)
        else -> raiz.border(1.dp, Pigment.Line, shape)
    }
    Row(
        aspecto.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) content else Pigment.Graphite
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(50)
    val aspecto = if (selected) Modifier.background(color, shape) else Modifier.border(1.dp, Pigment.Line, shape)
    Row(
        modifier.clip(shape).then(aspecto).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) color.legible() else LocalContentColor.current
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) color.legible() else color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(14.dp)).background(Pigment.Mist).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Search, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = Pigment.Graphite, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Pigment.Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(Pigment.Ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Close,
                contentDescription = "Borrar",
                tint = Pigment.Graphite,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onValueChange("") },
            )
        }
    }
}

/** campo de texto liso, para formularios y reseñas. */
@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp = 46.dp,
    secret: Boolean = false,
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = minHeight).clip(RoundedCornerShape(14.dp)).background(Pigment.Mist).padding(horizontal = 14.dp, vertical = 13.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
    ) {
        if (value.isEmpty()) Text(placeholder, color = Pigment.Graphite, fontSize = 15.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = Pigment.Ink, fontSize = 15.sp, lineHeight = 21.sp),
            cursorBrush = SolidColor(Pigment.Ink),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** pantalla vacia: una mancha, que pasa y que hacer. */
@Composable
fun EmptyState(title: String, cuerpo: String, color: Color, modifier: Modifier = Modifier, semilla: Int = 4, ink: Color = Pigment.Ink, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Splat(semilla, color, Modifier.size(84.dp))
        Spacer(Modifier.height(22.dp))
        Text(title, style = titular(22.sp, ink), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(cuerpo, color = if (ink == Pigment.Ink) Pigment.Graphite else ink.copy(alpha = 0.7f), fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** fila de menu dentro de una hoja inferior. */
@Composable
fun SheetRow(icon: ImageVector, text: String, onClick: () -> Unit, tint: Color = Pigment.Ink) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = tint, fontSize = 15.sp)
    }
}
