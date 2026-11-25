package com.example.palette.features.catalog.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.palette.core.model.Media
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.titular
import com.example.palette.features.anuncios.domain.model.Anuncio
import com.example.palette.features.anuncios.ui.Etiqueta
import com.example.palette.features.catalog.domain.model.Estreno
import com.example.palette.features.catalog.presentation.DiscoverAction
import com.example.palette.navigation.LocalWide
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** cada cuanto pasa sola a la siguiente diapositiva. */
private const val CADA = 6_000L

/**
 * el carrusel de arriba de descubrir: los estrenos del mes, uno detras de otro, con su imagen,
 * titulo, sinopsis y el boton del trailer. pasa solo cada pocos segundos (se para mientras se arrastra)
 * y los puntos de abajo dicen por cual va y dejan saltar a cualquiera.
 *
 * si hay un anuncio de cartel contratado, va el primero y marcado como patrocinado.
 */
@Composable
fun CarruselDeEstrenos(estrenos: List<Estreno>, patrocinado: Anuncio?, onAction: (DiscoverAction) -> Unit, modifier: Modifier = Modifier) {
    val total = estrenos.size + if (patrocinado != null) 1 else 0
    if (total == 0) return
    val pager = rememberPagerState { total }
    val ambito = rememberCoroutineScope()
    val alto = if (LocalWide.current) 320.dp else 230.dp

    // cada vez que se para en una, cuenta de nuevo y pasa a la siguiente (de la ultima vuelve a la primera)
    LaunchedEffect(pager.settledPage, total) {
        delay(CADA)
        if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.settledPage + 1) % total, animationSpec = tween(700))
    }
    // el anuncio cuenta como visto cuando se enseña
    LaunchedEffect(pager.settledPage) {
        if (patrocinado != null && pager.settledPage == 0) onAction(DiscoverAction.OnAnuncioVisto(patrocinado))
    }

    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            pager,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().height(alto),
        ) { pagina ->
            // la que entra crece un poco y la que sale se apaga: la transicion se nota sin marear
            val distancia = ((pager.currentPage - pagina) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val capa = Modifier.graphicsLayer {
                val escala = 1f - 0.07f * distancia
                scaleX = escala
                scaleY = escala
                alpha = 1f - 0.45f * distancia
            }
            val indice = if (patrocinado != null) pagina - 1 else pagina
            if (patrocinado != null && pagina == 0) {
                Diapositiva(
                    imagen = patrocinado.imagenUrl ?: patrocinado.contenidos.firstOrNull()?.let { it.backdropUrl ?: it.imageUrl },
                    titulo = patrocinado.titulo,
                    texto = patrocinado.texto,
                    etiqueta = { Etiqueta(patrocinado) },
                    boton = patrocinado.boton,
                    icono = if (patrocinado.ejemplo) Icons.Send else Icons.ExternalLink,
                    onClick = { onAction(DiscoverAction.OnAnuncioClicked(patrocinado)) },
                    onBoton = { onAction(DiscoverAction.OnAnuncioClicked(patrocinado)) },
                    modifier = capa,
                )
            } else {
                val estreno = estrenos[indice]
                Diapositiva(
                    imagen = estreno.media.backdropUrl ?: estreno.media.imageUrl,
                    titulo = estreno.media.title,
                    texto = estreno.media.overview.orEmpty(),
                    etiqueta = { EtiquetaDeEstreno(estreno.media) },
                    boton = if (estreno.trailer != null) "Ver tráiler" else null,
                    icono = Icons.Play,
                    onClick = { onAction(DiscoverAction.OnMediaClicked(estreno.media, fila = "estrenos", posicion = indice)) },
                    onBoton = { onAction(DiscoverAction.OnTrailerClicked(estreno)) },
                    modifier = capa,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // los puntos: el de la diapositiva actual se estira
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { i ->
                val actual = pager.currentPage == i
                val ancho by animateDpAsState(if (actual) 20.dp else 7.dp, tween(300))
                Box(
                    Modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(50))
                        .clickable { ambito.launch { pager.animateScrollToPage(i, animationSpec = tween(600)) } }
                        .padding(vertical = 6.dp),
                ) {
                    Box(Modifier.width(ancho).height(7.dp).clip(RoundedCornerShape(50)).background(if (actual) Pigment.Ink else Pigment.Line))
                }
            }
        }
    }
}

@Composable
private fun Diapositiva(
    imagen: String?,
    titulo: String,
    texto: String,
    etiqueta: @Composable () -> Unit,
    boton: String?,
    icono: ImageVector,
    onClick: () -> Unit,
    onBoton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fondo = if (imagen == null) Brush.linearGradient(listOf(Pigment.Accent, Pigment.Violet, Pigment.Magenta)) else SolidColor(Pigment.Ink)
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).background(fondo).clickable(onClick = onClick)) {
        if (imagen != null) {
            AsyncImage(imagen, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        // degradado de abajo a arriba para que el texto se lea sobre cualquier imagen
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.05f), 0.45f to Color.Black.copy(alpha = 0.25f), 1f to Color.Black.copy(alpha = 0.85f))))
        Box(Modifier.align(Alignment.TopStart).padding(14.dp)) { etiqueta() }
        Column(Modifier.align(Alignment.BottomStart).widthIn(max = 620.dp).padding(18.dp)) {
            Text(titulo, style = titular(25.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (texto.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(texto, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (boton != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable(onClick = onBoton).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icono, contentDescription = null, tint = Pigment.Ink, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(boton, color = Pigment.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// "estreno · película" en la esquina, del color del tipo de contenido
@Composable
private fun EtiquetaDeEstreno(media: Media) {
    Text(
        "Estreno · ${media.type.label}",
        color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Pigment.of(media.type)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
