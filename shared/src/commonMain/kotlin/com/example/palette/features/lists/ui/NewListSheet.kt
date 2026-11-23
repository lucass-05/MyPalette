package com.example.palette.features.lists.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.model.ListKind
import com.example.palette.core.ui.Icons
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.PillButton
import com.example.palette.core.ui.PlainField
import com.example.palette.core.ui.Sheet
import com.example.palette.core.ui.titular
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.lists.presentation.NewListAction
import com.example.palette.features.lists.presentation.NewListState
import com.example.palette.features.lists.presentation.NewListViewModel

@Composable
fun NewListSheetRoot(onClose: () -> Unit, onCreated: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { NewListViewModel(container.listsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // en cuanto existe se cierra la hoja y se abre la lista para empezar a llenarla
    LaunchedEffect(state.creada) {
        state.creada?.let {
            onClose()
            onCreated(it)
        }
    }

    NewListSheet(state, { action -> if (action == NewListAction.OnDismiss) onClose() else viewModel.onAction(action) })
}

/** crear una lista: primero de que va a ser y despues su nombre. */
@Composable
fun NewListSheet(state: NewListState, onAction: (NewListAction) -> Unit) {
    Sheet({ onAction(NewListAction.OnDismiss) }) {
        Text("Lista nueva", style = titular(22.sp), modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(4.dp))
        Text("Saldrá en tu perfil junto a Favoritos y Recientes.", color = Pigment.Graphite, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Opcion(
                "De contenido", "Pelis, series, juegos, música o libros, como en el buscador.",
                Icons.Film, Pigment.Cadmium, state.kind == ListKind.MEDIA, Modifier.weight(1f),
            ) { onAction(NewListAction.OnKindChosen(ListKind.MEDIA)) }
            Opcion(
                "De reseñas", "Una selección de tus reseñas, con tu nota y lo que escribiste.",
                Icons.Star, Pigment.Ochre, state.kind == ListKind.REVIEWS, Modifier.weight(1f),
            ) { onAction(NewListAction.OnKindChosen(ListKind.REVIEWS)) }
        }
        if (state.kind != null) {
            Spacer(Modifier.height(16.dp))
            PlainField(state.name, { onAction(NewListAction.OnNameChanged(it)) }, "Nombre: Para el verano, Lo mejor de 2026…", Modifier.padding(horizontal = 22.dp))
        }
        PillButton(
            "Crear lista",
            { onAction(NewListAction.OnCrearClicked) },
            enabled = state.valido,
            color = Pigment.Accent,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun Opcion(titulo: String, texto: String, icon: ImageVector, color: Color, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(18.dp)
    val borde = if (elegida) Modifier.border(2.dp, color, forma) else Modifier.border(1.dp, Pigment.Line, forma)
    Column(
        modifier.clip(forma).then(borde).background(if (elegida) color.copy(alpha = 0.1f) else Pigment.Paper)
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(10.dp))
        Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(texto, color = Pigment.Graphite, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
