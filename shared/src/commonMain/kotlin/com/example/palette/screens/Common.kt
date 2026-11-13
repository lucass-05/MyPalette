package com.example.palette.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.Media
import com.example.palette.data.MediaType
import com.example.palette.data.User
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.ui.Avatar
import com.example.palette.ui.CoverByHeight
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.proporcion
import com.example.palette.ui.titular

/** "pelicula, 2014, christopher nolan" */
fun Media.subtitulo(): String = listOfNotNull(type.label, year?.toString(), creator).joinToString(", ")

val MediaType.verbo: String
    get() = when (this) {
        MediaType.MOVIE, MediaType.SERIES -> "ha visto"
        MediaType.GAME -> "ha jugado a"
        MediaType.ALBUM, MediaType.SONG -> "ha escuchado"
        MediaType.BOOK -> "ha leído"
    }

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, dot: Color? = null) {
    Row(modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(9.dp))
        }
        Text(text, style = titular(19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** tira horizontal de portadas con su titulo debajo. */
@Composable
fun MediaRow(items: List<Media>, modifier: Modifier = Modifier, coverHeight: Int = 168) {
    val navegacion = LocalPalette.current.navegacion
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { media ->
            Column(Modifier.width((coverHeight * media.type.proporcion).dp).clickable { navegacion.abrir(Dest.Detail(media)) }) {
                CoverByHeight(media, coverHeight.dp)
                Spacer(Modifier.height(7.dp))
                Text(media.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, lineHeight = 16.sp, overflow = TextOverflow.Ellipsis)
                media.creator?.let { Text(it, fontSize = 12.sp, color = Pigment.Graphite, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
fun UserRow(user: User, modifier: Modifier = Modifier, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(modifier.clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(user, 46.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${user.username}", color = Pigment.Graphite, fontSize = 13.sp, maxLines = 1)
        }
        trailing()
    }
}

@Composable
fun FollowButton(userId: String, modifier: Modifier = Modifier) {
    val almacen = LocalPalette.current.almacen
    val following = almacen.sigoA(userId)
    PillButton(
        text = if (following) "Siguiendo" else "Seguir",
        onClick = { almacen.alternarSeguir(userId) },
        filled = !following,
        color = if (following) Pigment.Ink else Pigment.Accent,
        modifier = modifier,
    )
}

/** pestañas de texto: la activa lleva una pincelada de color debajo. */
@Composable
fun Segments(
    options: List<String>,
    selected: Int,
    alElegirPantalla: (Int) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    ink: Color = Pigment.Ink,
    suave: Color = Pigment.Graphite,
) {
    Row(modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        options.forEachIndexed { i, label ->
            Column(Modifier.clickable { alElegirPantalla(i) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (i == selected) ink else suave,
                )
                Spacer(Modifier.height(5.dp))
                Box(Modifier.size(width = 22.dp, height = 4.dp).clip(CircleShape).background(if (i == selected) color else Color.Transparent))
            }
        }
    }
}
