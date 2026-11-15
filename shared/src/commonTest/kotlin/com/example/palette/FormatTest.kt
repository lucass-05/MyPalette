package com.example.palette

import com.example.palette.core.ui.estrellasTexto
import com.example.palette.core.ui.notaTexto
import com.example.palette.core.util.DAY
import com.example.palette.core.util.HOUR
import com.example.palette.core.util.MINUTE
import com.example.palette.core.util.ahora
import com.example.palette.core.util.haceCuanto


import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {

    @Test
    fun timeAgoPicksTheLargestUnit() {
        val ahora = 100 * DAY
        assertEquals("ahora", haceCuanto(ahora - 20_000, ahora))
        assertEquals("5 min", haceCuanto(ahora - 5 * MINUTE, ahora))
        assertEquals("3 h", haceCuanto(ahora - 3 * HOUR, ahora))
        assertEquals("2 d", haceCuanto(ahora - 2 * DAY, ahora))
        assertEquals("3 sem", haceCuanto(ahora - 21 * DAY, ahora))
    }

    @Test
    fun scoresUseACommaAndOneDecimal() {
        assertEquals("8,4", notaTexto(8.43))
        assertEquals("10,0", notaTexto(9.96))
        assertEquals("7,0", notaTexto(7.0))
    }

    @Test
    fun halfStarsKeepTheirDecimal() {
        assertEquals("4", estrellasTexto(4f))
        assertEquals("3,5", estrellasTexto(3.5f))
    }
}
