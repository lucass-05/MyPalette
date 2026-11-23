package com.example.palette.features.tutorial.domain.model

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.ProfileLayout
import com.example.palette.core.model.User
import com.example.palette.features.catalog.domain.model.FilaRecomendada
import com.example.palette.features.home.domain.model.Feed
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.messages.domain.model.Aviso
import com.example.palette.features.reviews.domain.model.Resena

// lo que enseñan las pantallas del tutorial. mezcla lo mio de verdad (perfil, logs, pendientes)
// con cosas inventadas para que nada salga vacio. vive solo en memoria: no se guarda en ningun sitio
data class Ejemplos(
    val yo: User,
    // los resultados de una busqueda de ejemplo
    val busqueda: List<Media>,
    // mis logs (los de verdad y, si hay pocos, alguno inventado)
    val misLogs: List<LogEntry>,
    // los mismos en el orden de favoritos
    val favoritos: List<LogEntry>,
    val pendientes: List<Media>,
    val listas: List<ListaVista>,
    // un perfil con varios bloques puestos, para ver que se puede hacer
    val layout: ProfileLayout,
    val feed: Feed,
    val resena: Resena,
    val avisos: List<Aviso>,
    val filas: List<FilaRecomendada>,
)
