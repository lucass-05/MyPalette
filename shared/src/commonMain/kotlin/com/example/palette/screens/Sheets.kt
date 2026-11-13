package com.example.palette.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.LogEntry
import com.example.palette.data.Media
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Outgoing
import com.example.palette.ui.CoverByHeight
import com.example.palette.ui.EmptyState
import com.example.palette.ui.Icons
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.PlainField
import com.example.palette.ui.SheetRow
import com.example.palette.ui.Stars
import com.example.palette.ui.titular
import com.example.palette.ui.estrellasTexto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Pigment.Paper,
        contentColor = Pigment.Ink,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) { content() }
    }
}

@Composable
private fun MediaHeader(media: Media) {
    Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverByHeight(media, 64.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(media.title, style = titular(20.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(media.subtitulo(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** poner estrellas y, si apetece, escribir una reseña. sirve igual para crear que para editar. */
@Composable
fun ReviewSheet(media: Media) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val existente = remember(media.id) { almacen.miLogDe(media.id) }
    var stars by remember { mutableStateOf(existente?.stars ?: 0f) }
    var review by remember { mutableStateOf(existente?.review ?: "") }
    val cerrar = { app.navegacion.enResena = null }

    Sheet(cerrar) {
        MediaHeader(media)
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Stars(stars, size = 38.dp, alCambiar = { stars = it })
            Spacer(Modifier.height(8.dp))
            Text(
                if (stars == 0f) "Toca una estrella. Otro toque en la misma la deja en media." else "${estrellasTexto(stars)} de 5",
                color = Pigment.Graphite, fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        PlainField(review, { review = it.take(2000) }, "Qué te ha parecido (opcional)", Modifier.padding(horizontal = 22.dp), singleLine = false, minHeight = 110.dp)
        Spacer(Modifier.height(16.dp))
        PillButton(
            text = if (existente == null) "Guardar en mi perfil" else "Guardar cambios",
            onClick = {
                almacen.guardarLog(media, stars, review)
                cerrar()
            },
            enabled = stars > 0f,
            color = Pigment.of(media.type),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        )
    }
}

/** elegir a que amigo se le manda un contenido o una reseña. */
@Composable
fun SendSheet(envio: Outgoing) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    var nota by remember { mutableStateOf("") }
    var enviadoA by remember { mutableStateOf(emptySet<String>()) }
    val cerrar = { navegacion.enviando = null }

    Sheet(cerrar) {
        when (envio) {
            is Outgoing.Content -> MediaHeader(envio.media)
            is Outgoing.Review -> MediaHeader(envio.log.media)
            Outgoing.Nothing -> Text("Mensaje nuevo", style = titular(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        }
        if (envio != Outgoing.Nothing) {
            Spacer(Modifier.height(14.dp))
            PlainField(nota, { nota = it }, "Añade un mensaje (opcional)", Modifier.padding(horizontal = 22.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (almacen.amigos.isEmpty()) {
            EmptyState("Aún no sigues a nadie", "Sigue a alguien para poder escribirle.", Pigment.Cobalt, semilla = 9) {
                PillButton("Buscar gente", { cerrar(); navegacion.abrir(Dest.People) })
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // primero los grupos, luego los amigos uno a uno
            if (envio != Outgoing.Nothing) almacen.estado.groups.forEach { group ->
                val enviado = "g_${group.id}" in enviadoA
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GroupAvatar(group, 46.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Grupo de ${group.members.size}", color = Pigment.Graphite, fontSize = 13.sp)
                    }
                    PillButton(
                        text = if (enviado) "Enviado" else "Enviar",
                        onClick = {
                            almacen.enviarAGrupo(group.id, nota, (envio as? Outgoing.Content)?.media, (envio as? Outgoing.Review)?.log)
                            enviadoA = enviadoA + "g_${group.id}"
                        },
                        enabled = !enviado,
                        color = Pigment.Accent,
                        icon = if (enviado) Icons.Check else null,
                    )
                }
            }
            almacen.amigos.forEach { amigo ->
                val enviado = amigo.id in enviadoA
                UserRow(
                    amigo,
                    onClick = {
                        if (envio == Outgoing.Nothing) {
                            cerrar()
                            navegacion.abrir(Dest.Chat(amigo.id))
                        }
                    },
                ) {
                    if (envio != Outgoing.Nothing) {
                        PillButton(
                            text = if (enviado) "Enviado" else "Enviar",
                            onClick = {
                                almacen.enviar(
                                    toId = amigo.id,
                                    text = nota,
                                    media = (envio as? Outgoing.Content)?.media,
                                    log = (envio as? Outgoing.Review)?.log,
                                )
                                enviadoA = enviadoA + amigo.id
                            },
                            enabled = !enviado,
                            color = Pigment.Accent,
                            icon = if (enviado) Icons.Check else null,
                        )
                    }
                }
            }
        }
    }
}

/** que se puede hacer con un log propio desde el perfil. */
@Composable
fun LogMenuSheet(log: LogEntry) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val navegacion = app.navegacion
    val cerrar = { navegacion.menuDeLog = null }
    var confirmarBorrado by remember { mutableStateOf(false) }

    Sheet(cerrar) {
        MediaHeader(log.media)
        Row(Modifier.padding(start = 22.dp, top = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Stars(log.stars, size = 16.dp)
            if (log.hidden) {
                Icon(Icons.EyeOff, contentDescription = null, tint = Pigment.Graphite, modifier = Modifier.size(14.dp))
                Text("Oculto en tu perfil", color = Pigment.Graphite, fontSize = 13.sp)
            }
        }
        if (log.review.isNotBlank()) {
            Text(log.review, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
        }
        Spacer(Modifier.height(6.dp))
        SheetRow(if (log.pinned) Icons.PinOff else Icons.Pin, if (log.pinned) "Dejar de fijar" else "Fijar arriba del perfil", { almacen.alternarFijado(log.id); cerrar() })
        SheetRow(if (log.hidden) Icons.Eye else Icons.EyeOff, if (log.hidden) "Volver a mostrar en el perfil" else "Ocultar del perfil", { almacen.alternarOculto(log.id); cerrar() })
        SheetRow(Icons.Pencil, "Editar nota y reseña", { cerrar(); navegacion.enResena = log.media })
        SheetRow(Icons.Send, "Enviar a un amigo", { cerrar(); navegacion.enviando = Outgoing.Review(log) })
        SheetRow(Icons.ChevronRight, "Ver la ficha", { cerrar(); navegacion.abrir(Dest.Detail(log.media)) })
        if (confirmarBorrado) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Se borra la nota y la reseña.", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                PillButton("Cancelar", { confirmarBorrado = false }, filled = false)
                PillButton("Eliminar", { almacen.borrarLog(log.id); cerrar() }, color = Pigment.Cadmium)
            }
        } else {
            SheetRow(Icons.Trash, "Eliminar", { confirmarBorrado = true }, tint = Pigment.Cadmium)
        }
    }
}
