package com.example.palette.features.catalog.ui

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.MediaRow
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.ScreenTitle
import com.example.palette.core.ui.SectionTitle
import com.example.palette.core.ui.icon
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Screen


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * recomendaciones a partir de lo que he puntuado con 4 estrellas o mas:
 * por cada uno de esos logs se pide a su base de datos contenido parecido.
 */
@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    val app = LocalPalette.current
    val almacen = app.almacen
    val memoria = app.descubrir

    val favoritos = almacen.estado.logs.filter { it.stars >= 4f }.sortedWith(compareByDescending<LogEntry> { it.stars }.thenByDescending { it.createdAt }).take(6)
    val key = favoritos.joinToString("|") { it.media.id }
    var cargando by remember { mutableStateOf(memoria.key != key) }

    LaunchedEffect(key) {
        if (memoria.key == key) return@LaunchedEffect
        cargando = true
        val conocidos = (almacen.estado.logs.map { it.media.id } + almacen.estado.watchlist.map { it.id }).toSet()
        val filas = coroutineScope {
            favoritos.map { log ->
                async { "Porque te gustó ${log.media.title}" to app.catalogo.similar(log.media).filter { it.id !in conocidos && it.imageUrl != null } }
            }.awaitAll()
        }.filter { it.second.isNotEmpty() }.toMutableList()
        val tendencias = app.catalogo.tendencias().filter { it.id !in conocidos }
        if (tendencias.isNotEmpty()) filas += "Lo más visto esta semana" to tendencias
        memoria.filas = filas
        memoria.key = key
        cargando = false
    }

    // las filas se calculan una vez y se guardan, pero lo que ya esta en mi perfil o en pendientes
    // se quita cada vez que se pinta: si guardo algo desde aqui, desaparece al momento
    val mio = (almacen.estado.logs.map { it.media.id } + almacen.estado.watchlist.map { it.id }).toSet()
    val filas = memoria.filas.map { (title, items) -> title to items.filter { it.id !in mio } }.filter { it.second.isNotEmpty() }

    LazyColumn(modifier, contentPadding = PaddingValues(bottom = BarSpace)) {
        item { ScreenTitle("Descubrir") }
        when {
            cargando && filas.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Text("Mezclando colores…", color = Pigment.Graphite, fontSize = 14.sp)
                }
            }
            filas.isEmpty() -> item {
                EmptyState(
                    title = if (favoritos.isEmpty()) "Todavía no sé qué te gusta" else "No he encontrado nada parecido",
                    cuerpo = if (favoritos.isEmpty()) "Puntúa algo con 4 estrellas o más y aquí saldrán cosas en la misma línea."
                    else "Prueba a registrar algo más: cuantos más favoritos, mejores recomendaciones.",
                    color = Pigment.Violet,
                    semilla = 21,
                ) {
                    PillButton("Registrar algo", { app.navegacion.irA(Screen.LOG) }, icon = Icons.Plus)
                }
            }
            else -> items(filas) { (title, media) ->
                DiscoverRow(title, media)
            }
        }
    }
}

@Composable
private fun DiscoverRow(title: String, media: List<Media>) {
    Spacer(Modifier.height(18.dp))
    SectionTitle(title, dot = Pigment.of(media.first().type))
    Spacer(Modifier.height(12.dp))
    MediaRow(media)
}
