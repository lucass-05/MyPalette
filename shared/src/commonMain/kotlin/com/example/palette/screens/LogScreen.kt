package com.example.palette.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class Media(
    val title: String,
    val type: String,
    val image: String
)

private val mediaList = listOf(
    Media(
        "Interstellar",
        "Película",
        "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"
    ),
    Media(
        "Breaking Bad",
        "Serie",
        "https://image.tmdb.org/t/p/w500/ggFHVNu6YYI5L9pCfOacjizRGt.jpg"
    ),
    Media(
        "The Dark Side of the Moon",
        "Álbum",
        "https://coverartarchive.org/release-group/f5093c06-23e3-404f-aeaa-40f72885ee3a/front-250"
    ),
    Media(
        "El nombre del viento",
        "Libro",
        "https://covers.openlibrary.org/b/isbn/9788401337208-M.jpg"
    ),
    Media(
        "The Legend of Zelda: Breath of the Wild",
        "Videojuego",
        "https://images.igdb.com/igdb/image/upload/t_cover_big/co3p2d.jpg"
    )
)

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    var searchText by remember { mutableStateOf("") }

    val filteredMedia = mediaList.filter {
        it.title.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = modifier.padding(20.dp)
    ) {
        Text(
            text = "Añadir media",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar películas, música, libros...") },
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredMedia) { media ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { searchText = media.title }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = media.image,
                        contentDescription = media.title,
                        modifier = Modifier
                            .size(width = 70.dp, height = 95.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = media.type,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (filteredMedia.isEmpty()) {
                item {
                    Text("No se han encontrado resultados.")
                }
            }
        }
    }
}