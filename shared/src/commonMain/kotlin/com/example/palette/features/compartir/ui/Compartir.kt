package com.example.palette.features.compartir.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.core.metricas.Evento
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.User
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.Splat
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.codificarJpeg
import com.example.palette.core.ui.legible
import com.example.palette.core.ui.recordarCompartirImagen
import com.example.palette.core.ui.subtitulo
import com.example.palette.core.ui.titular
import com.example.palette.core.util.ahora
import com.example.palette.core.util.anioDe
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.profile.ui.Cuadricula
import com.example.palette.features.profile.ui.pieza
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import palette.shared.generated.resources.Res
import palette.shared.generated.resources.logo

/** la direccion que sale en las imagenes: quien la vea sabe donde esta palette. */
private const val DIRECCION = "lucass-05.github.io/MyPalette"

/**
 * hoja para compartir una tarjeta como imagen (tamaño de historia de instagram, 1080 x 1920).
 *
 * la tarjeta se pinta siempre a 360 x 640 con densidad 3, asi la imagen sale igual de nitida en
 * cualquier pantalla; en la hoja se ve encogida. que se pinta se "graba" en una capa y de ahi sale el jpeg.
 */
@Composable
fun CompartirSheet(que: String, nombreArchivo: String, onClose: () -> Unit, tarjeta: @Composable () -> Unit) {
    val metricas = LocalAppContainer.current.metricas
    val compartir = recordarCompartirImagen()
    val capa = rememberGraphicsLayer()
    val ambito = rememberCoroutineScope()
    // la imagen se prepara antes de tocar el boton: en el movil, el navegador solo deja compartir
    // si se hace justo al tocar, sin esperas por medio
    var jpeg by remember { mutableStateOf<ByteArray?>(null) }
    suspend fun preparar(): ByteArray = codificarJpeg(capa.toImageBitmap(), 92).also { jpeg = it }
    LaunchedEffect(Unit) {
        // se vuelve a hacer un poco despues por si las portadas tardan en llegar
        delay(900)
        preparar()
        delay(2500)
        preparar()
    }

    Sheet(onClose) {
        Text("Compartir como imagen", style = titular(22.sp), modifier = Modifier.padding(horizontal = 20.dp))
        Text(
            "Para tus historias de Instagram, TikTok o WhatsApp.",
            color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 14.dp),
        )
        BoxWithConstraints(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
            val alto = maxHeight
            val escala = with(LocalDensity.current) { alto.toPx() } / ALTO_PX
            Box(Modifier.size(alto * 9f / 16f, alto).clip(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalDensity provides Density(DENSIDAD, fontScale = 1f)) {
                    Box(
                        Modifier.requiredSize(ANCHO, ALTO)
                            .graphicsLayer { scaleX = escala; scaleY = escala }
                            .drawWithContent {
                                capa.record { this@drawWithContent.drawContent() }
                                drawLayer(capa)
                            },
                    ) { tarjeta() }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PillButton(
            "Compartir imagen",
            {
                metricas.registrar(Evento.COMPARTIR, mapOf("que" to que))
                val lista = jpeg
                if (lista != null) compartir(lista, nombreArchivo)
                else ambito.launch { compartir(preparar(), nombreArchivo) }
            },
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            color = Pigment.Accent,
        )
    }
}

private val ANCHO = 360.dp
private val ALTO = 640.dp
private const val DENSIDAD = 3f
private const val ALTO_PX = 640 * DENSIDAD

/** una reseña: la portada grande, las estrellas, el texto y quien la ha escrito. */
@Composable
fun TarjetaResena(log: LogEntry, autor: User) {
    val color = Pigment.of(log.media.type)
    Box(Modifier.fillMaxSize().background(Pigment.Paper)) {
        // manchas del color del tipo de contenido al fondo
        Splat(log.media.id.hashCode(), color.copy(alpha = 0.3f), Modifier.size(300.dp).offset(x = 170.dp, y = (-110).dp))
        Splat(autor.id.hashCode(), Color(autor.favorite).copy(alpha = 0.25f), Modifier.size(200.dp).offset(x = (-110).dp, y = 500.dp))
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Marca()
            Spacer(Modifier.height(26.dp))
            CoverByHeight(log.media, 250.dp, Modifier.clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.height(18.dp))
            Text(log.media.title, style = titular(26.sp), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(log.media.subtitulo(), color = Pigment.Graphite, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Stars(log.stars, size = 22.dp)
            if (log.review.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "“${log.review}”", fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                    maxLines = 5, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(autor, 30.dp)
                Spacer(Modifier.width(8.dp))
                Text("@${autor.username}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(DIRECCION, color = Pigment.Graphite, fontSize = 11.sp)
        }
    }
}

/**
 * un perfil: foto, nombre, cuanto lleva registrado este año y sus favoritos pintados como en su
 * paleta (manchas o recuadros). el fondo es el que eligio para su perfil.
 */
@Composable
fun TarjetaPerfil(user: User, logs: List<LogEntry>, favoritos: List<LogEntry>) {
    val fondo = user.background?.let { Color(it) }
    val ink = fondo?.legible() ?: Pigment.Ink
    val suave = if (fondo == null) Pigment.Graphite else ink.copy(alpha = 0.7f)
    val anio = anioDe(ahora())
    val deEsteAnio = logs.filter { anioDe(it.createdAt) == anio }
    val resumen = listOf(
        "${deEsteAnio.size} registros en $anio",
        "${logs.count { it.review.isNotBlank() }} reseñas",
    ).joinToString(" · ")

    Box(Modifier.fillMaxSize().background(fondo ?: Pigment.Paper)) {
        Splat(user.id.hashCode(), Color(user.favorite).copy(alpha = 0.3f), Modifier.size(280.dp).offset(x = 170.dp, y = (-120).dp))
        Column(Modifier.fillMaxSize().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Marca(ink)
            Spacer(Modifier.height(12.dp))
            Avatar(user, 72.dp)
            Spacer(Modifier.height(10.dp))
            Text(user.name, style = titular(28.sp), color = ink, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp))
            Text("@${user.username}", color = suave, fontSize = 13.sp)
            if (user.bio.isNotBlank()) {
                Text(
                    user.bio, color = ink, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 30.dp, vertical = 6.dp),
                )
            }
            Text(resumen, color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Cuadricula(favoritos.take(6).map { it.pieza() }, blob = user.layout.favoritesBlob, ink = ink, onClick = {}, modifier = Modifier.padding(horizontal = 34.dp))
            Spacer(Modifier.weight(1f))
            Text("Mi paleta en Palette · $DIRECCION", color = suave, fontSize = 11.sp)
        }
    }
}

// el logo y el nombre, arriba de las tarjetas
@Composable
private fun Marca(ink: Color = Pigment.Ink) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(7.dp))
        Text("Palette", style = titular(18.sp), color = ink)
    }
}
