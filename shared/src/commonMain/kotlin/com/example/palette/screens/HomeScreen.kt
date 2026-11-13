package com.example.palette.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palette.data.LogEntry
import com.example.palette.data.User
import com.example.palette.data.timeAgo
import com.example.palette.navigation.BarSpace
import com.example.palette.navigation.Dest
import com.example.palette.navigation.LocalPalette
import com.example.palette.navigation.Outgoing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.palette.ui.Avatar
import com.example.palette.ui.BackBar
import com.example.palette.ui.SearchField
import com.example.palette.ui.CoverByHeight
import com.example.palette.ui.EmptyState
import com.example.palette.ui.IconAction
import com.example.palette.ui.Icons
import com.example.palette.ui.Pigment
import com.example.palette.ui.PillButton
import com.example.palette.ui.ScreenTitle
import com.example.palette.ui.Stars
import com.example.palette.ui.avatarShape
import com.example.palette.ui.display

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    val feed = store.feed
    val stories = store.stories

    // una columna en el movil; en pantallas anchas los logs se reparten en varias
    LazyVerticalGrid(GridCells.Adaptive(400.dp), modifier, contentPadding = PaddingValues(bottom = BarSpace)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Column {
            ScreenTitle("Palette") {
                IconAction(Icons.UserPlus, "Buscar gente", { nav.open(Dest.People) })
            }
            if (store.offline) {
                Text("Sin conexión. Estás viendo lo último que se guardó.", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
          }
        }
        if (stories.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              Column {
                val queue = stories.map { it.id }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // la forma depende del puesto en la fila: la primera historia siempre lleva la misma mancha, la segunda la siguiente...
                    itemsIndexed(stories, key = { _, user -> user.id }) { position, user ->
                        StoryBubble(user, position, unseen = store.hasUnseenStory(user.id)) { nav.open(Dest.Story(user.id, queue)) }
                    }
                }
                HorizontalDivider(Modifier.padding(top = 6.dp), color = Pigment.Line)
              }
            }
        }
        if (feed.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = if (store.friends.isEmpty()) "Aún no sigues a nadie" else "Todo tranquilo por aquí",
                    body = if (store.friends.isEmpty()) "Cuando sigas a alguien, lo que vea, juegue, lea o escuche aparecerá aquí."
                    else "La gente a la que sigues todavía no ha registrado nada.",
                    color = Pigment.Cadmium,
                ) {
                    PillButton("Buscar gente", { nav.open(Dest.People) }, icon = Icons.Search)
                }
            }
        }
        gridItems(feed, key = { it.id }) { log ->
            Column {
                FeedPost(log)
                HorizontalDivider(color = Pigment.Line)
            }
        }
    }
}

@Composable
private fun StoryBubble(user: User, position: Int, unseen: Boolean, onClick: () -> Unit) {
    // sin ver: anillo del color favorito de esa persona. ya vista: una linea fina
    val ring = if (unseen) Color(user.favorite) else Pigment.Line
    Column(Modifier.width(72.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(70.dp).border(if (unseen) 3.dp else 1.dp, ring, avatarShape(position)), contentAlignment = Alignment.Center) {
            Avatar(user, 58.dp, variant = position)
        }
        Spacer(Modifier.height(5.dp))
        Text(user.name.substringBefore(' '), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (unseen) Pigment.Ink else Pigment.Graphite)
    }
}

@Composable
private fun FeedPost(log: LogEntry) {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    val author = store.user(log.userId) ?: return
    val media = log.media

    Column(Modifier.fillMaxWidth().clickable { nav.open(Dest.Detail(media)) }.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp)) {
        Row(Modifier.clickable { nav.open(Dest.Profile(author.id)) }, verticalAlignment = Alignment.CenterVertically) {
            Avatar(author, 34.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(author.name) }
                    withStyle(SpanStyle(color = Pigment.Graphite)) { append(" ${media.type.verb}") }
                },
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(timeAgo(log.createdAt), color = Pigment.Graphite, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            CoverByHeight(media, 148.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(media.title, style = display(21.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(media.byline(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Stars(log.stars, size = 16.dp)
                if (log.review.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Text(log.review, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(Modifier.padding(top = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            val liked = store.isLiked(log.id)
            IconAction(
                if (liked) Icons.HeartFilled else Icons.Heart,
                if (liked) "Quitar me gusta" else "Me gusta",
                { store.toggleLike(log.id) },
                tint = if (liked) Pigment.Cadmium else Pigment.Ink,
            )
            IconAction(Icons.Send, "Enviar a un amigo", { nav.sending = Outgoing.Review(log) })
            val saved = store.inWatchlist(media.id)
            IconAction(
                if (saved) Icons.BookmarkFilled else Icons.Bookmark,
                if (saved) "Quitar de pendientes" else "Guardar en pendientes",
                { store.toggleWatchlist(media) },
            )
        }
    }
}

/** buscador de personas para empezar a seguirlas. */
@Composable
fun PeopleScreen(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val store = palette.store
    val nav = palette.nav
    var query by remember { mutableStateOf("") }
    val results = store.searchPeople(query)

    Column(modifier) {
        BackBar("Buscar gente", { nav.back() })
        SearchField(query, { query = it }, "Nombre o @usuario", Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = BarSpace)) {
            if (query.isBlank() && results.isNotEmpty()) {
                item { Text("Gente que quizá conozcas", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            }
            items(results, key = { it.id }) { user ->
                UserRow(user, onClick = { nav.open(Dest.Profile(user.id)) }) { FollowButton(user.id) }
            }
            if (results.isEmpty()) {
                item {
                    EmptyState(
                        title = if (query.isBlank()) "Ya sigues a todo el mundo" else "Nadie con ese nombre",
                        body = if (query.isBlank()) "Cuando se registre más gente aparecerá aquí." else "Prueba con su nombre de usuario, sin la arroba.",
                        color = Pigment.Cobalt,
                        seed = 9,
                    )
                }
            }
        }
    }
}
