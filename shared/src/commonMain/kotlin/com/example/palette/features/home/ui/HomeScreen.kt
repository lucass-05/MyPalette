package com.example.palette.features.home.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.Media
import com.example.palette.core.model.User
import com.example.palette.core.ui.Avatar
import com.example.palette.core.ui.CoverByHeight
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.IconAction
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.ScreenTitle
import com.example.palette.core.ui.Stars
import com.example.palette.core.ui.formaAvatar
import com.example.palette.core.ui.icon
import com.example.palette.core.ui.subtitulo
import com.example.palette.core.ui.titular
import com.example.palette.core.ui.verbo
import com.example.palette.core.util.haceCuanto
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.home.domain.model.Publicacion
import com.example.palette.features.home.presentation.HomeAction
import com.example.palette.features.home.presentation.HomeState
import com.example.palette.features.home.presentation.HomeViewModel
import com.example.palette.features.messages.domain.model.Outgoing
import com.example.palette.navigation.BarSpace
import org.jetbrains.compose.resources.painterResource
import palette.shared.generated.resources.Res
import palette.shared.generated.resources.logo

// LazyVerticalGrid solo compone las celdas que se ven en pantalla, por eso aguanta listas largas.
// la "key" de cada elemento le dice a compose cual es cual cuando la lista cambia de orden.

@Composable
fun HomeScreenRoot(
    onOpenPeople: () -> Unit,
    onOpenStory: (userId: String, cola: List<String>) -> Unit,
    onOpenMedia: (Media) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenReview: (String) -> Unit,
    onSend: (Outgoing) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { HomeViewModel(container.feedRepository, container.libraryRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                HomeAction.OnPeopleClicked -> onOpenPeople()
                // la historia se abre con la fila entera, para poder pasar de una persona a la siguiente
                is HomeAction.OnStoryClicked -> onOpenStory(action.userId, state.feed.historias.map { it.user.id })
                is HomeAction.OnMediaClicked -> onOpenMedia(action.media)
                is HomeAction.OnUserClicked -> onOpenUser(action.userId)
                is HomeAction.OnSendClicked -> onSend(Outgoing.Review(action.log))
                is HomeAction.OnResenaClicked -> onOpenReview(action.logId)
                // me gusta y pendientes cambian datos: van al viewmodel
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(state: HomeState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    val feed = state.feed

    // una columna en el movil; en pantallas anchas los logs se reparten en varias
    LazyVerticalGrid(GridCells.Adaptive(400.dp), modifier, contentPadding = PaddingValues(bottom = BarSpace)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                ScreenTitle("Palette", icono = { Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(34.dp)) }) {
                    IconAction(Icons.UserPlus, "Buscar gente", { onAction(HomeAction.OnPeopleClicked) })
                }
                if (feed.sinConexion) {
                    Text("Sin conexión. Estás viendo lo último que se guardó.", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
            }
        }
        if (feed.historias.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // la forma depende del puesto en la fila: la primera historia siempre lleva la misma mancha, la segunda la siguiente...
                        itemsIndexed(feed.historias, key = { _, historia -> historia.user.id }) { posicion, historia ->
                            StoryBubble(historia.user, posicion, historia.sinVer) { onAction(HomeAction.OnStoryClicked(historia.user.id)) }
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 6.dp), color = Pigment.Line)
                }
            }
        }
        if (feed.publicaciones.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = if (feed.sigoAAlguien) "Todo tranquilo por aquí" else "Aún no sigues a nadie",
                    cuerpo = if (feed.sigoAAlguien) "La gente a la que sigues todavía no ha registrado nada."
                    else "Cuando sigas a alguien, lo que vea, juegue, lea o escuche aparecerá aquí.",
                    color = Pigment.Cadmium,
                ) {
                    PillButton("Buscar gente", { onAction(HomeAction.OnPeopleClicked) }, icon = Icons.Search)
                }
            }
        }
        gridItems(feed.publicaciones, key = { it.log.id }) { publicacion ->
            Column {
                FeedPost(publicacion, onAction)
                HorizontalDivider(color = Pigment.Line)
            }
        }
    }
}

@Composable
private fun StoryBubble(user: User, posicion: Int, sinVer: Boolean, onClick: () -> Unit) {
    // sin ver: anillo del color favorito de esa persona. ya vista: una linea fina
    val anillo = if (sinVer) Color(user.favorite) else Pigment.Line
    Column(Modifier.width(72.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(70.dp).border(if (sinVer) 3.dp else 1.dp, anillo, formaAvatar(posicion)), contentAlignment = Alignment.Center) {
            Avatar(user, 58.dp, variante = posicion)
        }
        Spacer(Modifier.height(5.dp))
        Text(user.name.substringBefore(' '), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (sinVer) Pigment.Ink else Pigment.Graphite)
    }
}

@Composable
private fun FeedPost(publicacion: Publicacion, onAction: (HomeAction) -> Unit) {
    val autor = publicacion.autor
    val log = publicacion.log
    val media = log.media

    Column(Modifier.fillMaxWidth().clickable { onAction(HomeAction.OnMediaClicked(media)) }.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp)) {
        Row(Modifier.clickable { onAction(HomeAction.OnUserClicked(autor.id)) }, verticalAlignment = Alignment.CenterVertically) {
            Avatar(autor, 34.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(autor.name) }
                    withStyle(SpanStyle(color = Pigment.Graphite)) { append(" ${media.type.verbo}") }
                },
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(haceCuanto(log.createdAt), color = Pigment.Graphite, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            CoverByHeight(media, 148.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(media.title, style = titular(21.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(media.subtitulo(), color = Pigment.Graphite, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Stars(log.stars, size = 16.dp)
                if (log.review.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    // el texto de la reseña abre la reseña entera, con sus comentarios
                    Text(
                        log.review, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 5, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onAction(HomeAction.OnResenaClicked(log.id)) },
                    )
                }
            }
        }
        Row(Modifier.padding(top = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconAction(
                if (publicacion.meGusta) Icons.HeartFilled else Icons.Heart,
                if (publicacion.meGusta) "Quitar me gusta" else "Me gusta",
                { onAction(HomeAction.OnLikeClicked(log.id)) },
                tint = if (publicacion.meGusta) Pigment.Cadmium else Pigment.Ink,
            )
            if (publicacion.meGustas > 0) Text("${publicacion.meGustas}", fontSize = 13.sp, color = Pigment.Graphite, modifier = Modifier.padding(end = 6.dp))
            IconAction(Icons.Comment, "Comentarios", { onAction(HomeAction.OnResenaClicked(log.id)) })
            if (publicacion.comentarios > 0) Text("${publicacion.comentarios}", fontSize = 13.sp, color = Pigment.Graphite, modifier = Modifier.padding(end = 6.dp))
            IconAction(Icons.Send, "Enviar a un amigo", { onAction(HomeAction.OnSendClicked(log)) })
            IconAction(
                if (publicacion.enPendientes) Icons.BookmarkFilled else Icons.Bookmark,
                if (publicacion.enPendientes) "Quitar de pendientes" else "Guardar en pendientes",
                { onAction(HomeAction.OnWatchlistClicked(media)) },
            )
        }
    }
}
