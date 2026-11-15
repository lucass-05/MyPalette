package com.example.palette.features.people.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palette.core.ui.BackBar
import com.example.palette.core.ui.EmptyState
import com.example.palette.core.ui.FollowButton
import com.example.palette.core.ui.Pigment
import com.example.palette.core.ui.SearchField
import com.example.palette.core.ui.UserRow
import com.example.palette.di.LocalAppContainer
import com.example.palette.features.people.domain.model.Gente
import com.example.palette.features.people.presentation.PeopleAction
import com.example.palette.features.people.presentation.PeopleState
import com.example.palette.features.people.presentation.PeopleViewModel
import com.example.palette.navigation.BarSpace

@Composable
fun PeopleScreenRoot(onBack: () -> Unit, onOpenUser: (String) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { PeopleViewModel(container.peopleRepository, container.searchPeopleUseCase) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    PeopleScreen(
        state = state,
        onAction = { action ->
            when (action) {
                PeopleAction.OnBackClicked -> onBack()
                is PeopleAction.OnUserClicked -> onOpenUser(action.userId)
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}

/** buscador de personas para empezar a seguirlas. */
@Composable
fun PeopleScreen(state: PeopleState, onAction: (PeopleAction) -> Unit, modifier: Modifier = Modifier) {
    val sinTexto = state.consulta.isBlank()

    Column(modifier) {
        BackBar("Buscar gente", { onAction(PeopleAction.OnBackClicked) })
        SearchField(state.consulta, { onAction(PeopleAction.OnConsultaChanged(it)) }, "Nombre o @usuario", Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = BarSpace)) {
            if (sinTexto && state.resultados.isNotEmpty()) {
                item { Text("Gente que quizá conozcas", color = Pigment.Graphite, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            }
            items(state.resultados, key = { it.id }) { user ->
                UserRow(user, onClick = { onAction(PeopleAction.OnUserClicked(user.id)) }) {
                    FollowButton(user.id in state.siguiendo, { onAction(PeopleAction.OnFollowClicked(user.id)) })
                }
            }
            if (state.resultados.isEmpty()) {
                item {
                    EmptyState(
                        title = if (sinTexto) "Ya sigues a todo el mundo" else "Nadie con ese nombre",
                        cuerpo = if (sinTexto) "Cuando se registre más gente aparecerá aquí." else "Prueba con su nombre de usuario, sin la arroba.",
                        color = Pigment.Cobalt,
                        semilla = 9,
                    )
                }
            }
        }
    }
}
