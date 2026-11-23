package com.example.palette

import com.example.palette.core.data.calcularAvisos
import com.example.palette.core.model.BLOQUES_POR_DEFECTO
import com.example.palette.core.model.Comment
import com.example.palette.core.model.Like
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.ME
import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import com.example.palette.core.model.Notice
import com.example.palette.core.model.NoticeKind
import com.example.palette.core.model.ProfileLayout
import com.example.palette.core.model.ProfileWidget
import com.example.palette.core.model.WidgetKind
import com.example.palette.features.profile.domain.usecases.SortProfileLogsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// pruebas de la logica que no depende de la pantalla ni del servidor
class LogicaTest {

    private fun media(id: String) = Media(id, MediaType.MOVIE, id)
    private fun log(id: String, userId: String, mediaId: String, stars: Float = 4f, at: Long = 0, pinned: Boolean = false, hidden: Boolean = false) =
        LogEntry(id, userId, media(mediaId), stars, createdAt = at, pinned = pinned, hidden = hidden)

    @Test
    fun favoritosOrdenaFijadosNotaYFecha() {
        val logs = listOf(
            log("a", ME, "m1", stars = 4f, at = 10),
            log("b", ME, "m2", stars = 5f, at = 5),
            log("c", ME, "m3", stars = 4f, at = 20),
            log("d", ME, "m4", stars = 2f, at = 1, pinned = true),
        )
        // fijado primero; despues por nota; con la misma nota, el mas reciente antes
        assertEquals(listOf("d", "b", "c", "a"), SortProfileLogsUseCase()(logs).map { it.id })
    }

    @Test
    fun paletaSinPersonalizarLlevaLosBloquesPorDefecto() {
        assertEquals(BLOQUES_POR_DEFECTO, ProfileLayout().bloques)
        // una vez personalizada, aunque se quede vacia, ya no salen los de por defecto
        assertTrue(ProfileLayout(custom = true).bloques.isEmpty())
        val mio = listOf(ProfileWidget("t", WidgetKind.TEXT))
        assertEquals(mio, ProfileLayout(widgets = mio).bloques)
    }

    @Test
    fun avisosDeLoQueTocaYSoloDeLoQueToca() {
        val mios = listOf(log("l1", ME, "dune"))
        val avisos = calcularAvisos(
            misLogs = mios,
            logsDeOtros = listOf(
                log("o1", "ana", "dune", at = 300),        // amiga que reseña lo mismo: aviso
                log("o2", "ana", "otra", at = 300),        // otra cosa: no
                log("o3", "pepe", "dune", at = 300),       // alguien a quien no sigo: no
                log("o4", "ana", "dune", at = 300, hidden = true), // oculto: no
            ),
            siguiendo = setOf("ana"),
            seguidores = listOf("marta" to 100L),
            meGustas = listOf(Like("ana", "l1", 200), Like(ME, "l1", 200), Like("ana", "l1x", 200), Like("pepe", "l1", 0)),
            comentarios = listOf(Comment("c1", "l1", "ana", "hola", 400), Comment("c2", "l1", ME, "gracias", 500)),
            anteriores = emptyList(),
            desde = 0,
        )
        assertEquals(listOf("c_c1", "s_o1", "l_ana_l1", "f_marta"), avisos.map { it.id })
        assertEquals(listOf(NoticeKind.COMMENT, NoticeKind.SAME_MEDIA, NoticeKind.LIKE, NoticeKind.FOLLOW), avisos.map { it.kind })
    }

    @Test
    fun avisosRecuerdanLoLeidoYNoAvisanDeLoAntiguo() {
        val mios = listOf(log("l1", ME, "dune"))
        val avisos = calcularAvisos(
            misLogs = mios, logsDeOtros = emptyList(), siguiendo = emptySet(),
            seguidores = listOf("marta" to 50L),
            meGustas = listOf(Like("ana", "l1", 50), Like("pepe", "l1", 500)),
            comentarios = emptyList(),
            anteriores = listOf(Notice("f_marta", NoticeKind.FOLLOW, "marta", at = 50, read = true)),
            desde = 100,
        ).associateBy { it.id }
        assertTrue(avisos.getValue("f_marta").read)      // ya estaba leido
        assertTrue(avisos.getValue("l_ana_l1").read)     // anterior a desde
        assertFalse(avisos.getValue("l_pepe_l1").read)   // nuevo
    }
}
