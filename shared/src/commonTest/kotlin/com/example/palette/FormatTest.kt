package com.example.palette

import com.example.palette.data.DAY
import com.example.palette.data.HOUR
import com.example.palette.data.MINUTE
import com.example.palette.data.timeAgo
import com.example.palette.ui.scoreLabel
import com.example.palette.ui.starsLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {

    @Test
    fun timeAgoPicksTheLargestUnit() {
        val now = 100 * DAY
        assertEquals("ahora", timeAgo(now - 20_000, now))
        assertEquals("5 min", timeAgo(now - 5 * MINUTE, now))
        assertEquals("3 h", timeAgo(now - 3 * HOUR, now))
        assertEquals("2 d", timeAgo(now - 2 * DAY, now))
        assertEquals("3 sem", timeAgo(now - 21 * DAY, now))
    }

    @Test
    fun scoresUseACommaAndOneDecimal() {
        assertEquals("8,4", scoreLabel(8.43))
        assertEquals("10,0", scoreLabel(9.96))
        assertEquals("7,0", scoreLabel(7.0))
    }

    @Test
    fun halfStarsKeepTheirDecimal() {
        assertEquals("4", starsLabel(4f))
        assertEquals("3,5", starsLabel(3.5f))
    }
}
