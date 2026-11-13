package com.example.palette.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.Media
import com.example.palette.data.MediaType
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.ui.Chip
import com.example.palette.ui.CoverByHeight
import com.example.palette.ui.EmptyState
import com.example.palette.ui.Icons
import com.example.palette.ui.Pigment
import com.example.palette.ui.ScreenTitle
import com.example.palette.ui.SearchField
import com.example.palette.ui.icon
import com.example.palette.ui.notaTexto
import kotlinx.coroutines.delay

// LaunchedEffect(clave) ejecuta su bloque cuando la clave cambia y cancela el anterior.
// aqui la clave es el texto del buscador: si se sigue escribiendo, la busqueda pendiente se cancela
// sola antes de salir (el delay hace de espera). es la manera habitual de buscar "mientras se escribe".

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val navegacion = app.navegacion
    var consulta by rememberSaveable { mutableStateOf("") }
    var tipos by remember { mutableStateOf(emptySet<MediaType>()) }
    var results by remember { mutableStateOf<List<Media>?>(null) }
    var buscando by remember { mutableStateOf(false) }

    // se busca mientras se escribe, esperando un momento para no lanzar una peticion por letra
    LaunchedEffect(consulta, tipos) {
        val text = consulta.trim()
        if (text.length < 2) {
            results = null
            buscando = false
            return@LaunchedEffect
        }
        buscando = true
        delay(350)
        results = app.catalogo.buscar(text, tipos)
        buscando = false
    }

    Column(modifier) {
        ScreenTitle("Registrar")
        SearchField(consulta, { consulta = it }, "Pelis, series, juegos, música, libros…", Modifier.padding(horizontal = 20.dp).widthIn(max = 720.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MediaType.entries.forEach { type ->
                Chip(
                    text = type.plural,
                    selected = type in tipos,
                    color = Pigment.of(type),
                    icon = type.icon,
                    onClick = { tipos = if (type in tipos) tipos - type else tipos + type },
                )
            }
        }

        val encontrados = results
        LazyVerticalGrid(GridCells.Adaptive(380.dp), contentPadding = PaddingValues(bottom = BarSpace)) {
            when {
                encontrados == null -> item(span = { GridItemSpan(maxLineSpan) }) { SearchHint(app.catalogo.noDisponibles) }
                encontrados.isEmpty() && !buscando -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Sin resultados",
                        cuerpo = "No hay nada que se llame así" + (if (tipos.isEmpty()) "." else " en lo que tienes marcado. Prueba a quitar filtros."),
                        color = Pigment.Ochre,
                        semilla = 13,
                    )
                }
                else -> items(encontrados, key = { it.id }) { media ->
                    ResultRow(media) { navegacion.abrir(Dest.Detail(media)) }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(media: Media, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // todas las miniaturas ocupan el mismo ancho para que los titulos queden alineados
        Row(Modifier.width(50.dp), horizontalArrangement = Arrangement.Center) { CoverByHeight(media, 62.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(media.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(media.type.icon, contentDescription = null, tint = Pigment.of(media.type), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(media.subtitulo(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        media.score?.let {
            Spacer(Modifier.width(10.dp))
            Icon(Icons.StarFilled, contentDescription = null, tint = Pigment.Star, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(notaTexto(it), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SearchHint(noDisponibles: Set<MediaType>) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Busca lo último que has visto, jugado, leído o escuchado para puntuarlo o guardarlo en pendientes.", color = Pigment.Graphite, fontSize = 14.sp, lineHeight = 20.sp)
        if (noDisponibles.isNotEmpty()) {
            // aviso para desarrollo: desaparece en cuanto las claves estan en local.properties
            Spacer(Modifier.height(14.dp))
            Text(
                "Falta la clave de TMDB: sin ella no salen " + noDisponibles.joinToString(" ni ") { it.plural.lowercase() } + ". Mira el README para añadirla.",
                color = Pigment.Cadmium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}
