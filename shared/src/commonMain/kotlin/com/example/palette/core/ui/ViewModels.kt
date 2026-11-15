package com.example.palette.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * un viewmodel vive mientras viva su "dueño". las pantallas con ruta tienen uno cada una (se lo da
 * la navegacion), pero una hoja inferior no es una pantalla: sin esto su viewmodel seria de la
 * pantalla de debajo y al volver a abrir la hoja apareceria con lo que se dejo escrito la vez anterior.
 * lo que va dentro de [content] tiene sus propios viewmodels, que se destruyen al cerrarse.
 */
@Composable
fun ConViewModelsPropios(content: @Composable () -> Unit) {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
