package com.example.palette.screens

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
import com.example.palette.data.LogEntry
import com.example.palette.data.Media
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Screen
import com.example.palette.ui.EmptyState
import com.example.palette.ui.Icons
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.ScreenTitle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * recomendaciones a partir de lo que he puntuado con 4 estrellas o mas:
 * por cada uno de esos logs se pide a su base de datos contenido parecido.
 */
@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val store = palette.store
    val cache = palette.discover

    val loved = store.data.logs.filter { it.stars >= 4f }.sortedWith(compareByDescending<LogEntry> { it.stars }.thenByDescending { it.createdAt }).take(6)
    val key = loved.joinToString("|") { it.media.id }
    var loading by remember { mutableStateOf(cache.key != key) }

    LaunchedEffect(key) {
        if (cache.key == key) return@LaunchedEffect
        loading = true
        val known = (store.data.logs.map { it.media.id } + store.data.watchlist.map { it.id }).toSet()
        val rows = coroutineScope {
            loved.map { log ->
                async { "Porque te gustó ${log.media.title}" to palette.catalog.similar(log.media).filter { it.id !in known && it.imageUrl != null } }
            }.awaitAll()
        }.filter { it.second.isNotEmpty() }.toMutableList()
        val trending = palette.catalog.trending().filter { it.id !in known }
        if (trending.isNotEmpty()) rows += "Lo más visto esta semana" to trending
        cache.rows = rows
        cache.key = key
        loading = false
    }

    // las filas se calculan una vez y se guardan, pero lo que ya esta en mi perfil o en pendientes
    // se quita cada vez que se pinta: si guardo algo desde aqui, desaparece al momento
    val mine = (store.data.logs.map { it.media.id } + store.data.watchlist.map { it.id }).toSet()
    val rows = cache.rows.map { (title, items) -> title to items.filter { it.id !in mine } }.filter { it.second.isNotEmpty() }

    LazyColumn(modifier, contentPadding = PaddingValues(bottom = BarSpace)) {
        item { ScreenTitle("Descubrir") }
        when {
            loading && rows.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Text("Mezclando colores…", color = Pigment.Graphite, fontSize = 14.sp)
                }
            }
            rows.isEmpty() -> item {
                EmptyState(
                    title = if (loved.isEmpty()) "Todavía no sé qué te gusta" else "No he encontrado nada parecido",
                    body = if (loved.isEmpty()) "Puntúa algo con 4 estrellas o más y aquí saldrán cosas en la misma línea."
                    else "Prueba a registrar algo más: cuantos más favoritos, mejores recomendaciones.",
                    color = Pigment.Violet,
                    seed = 21,
                ) {
                    PillButton("Registrar algo", { palette.nav.switchTo(Screen.LOG) }, icon = Icons.Plus)
                }
            }
            else -> items(rows) { (title, media) ->
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
