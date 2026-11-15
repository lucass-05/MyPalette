package com.example.palette.features.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.palette.core.model.Group
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.formaAvatar
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.titular

/** foto de un grupo, con la misma forma de mancha que los avatares. sin foto, su inicial sobre el color de detalle. */
@Composable
fun GroupAvatar(group: Group, size: Dp, modifier: Modifier = Modifier) {
    val color = Pigment.Accent
    Box(modifier.size(size).clip(formaAvatar(group.id.hashCode())).background(color), contentAlignment = Alignment.Center) {
        Text(group.name.take(1).uppercase(), style = titular((size.value * 0.46f).sp, color.legible()))
        if (group.photoUrl != null) {
            AsyncImage(group.photoUrl, contentDescription = group.name, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}
