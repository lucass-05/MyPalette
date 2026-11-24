package com.example.palette.core.util

import kotlin.random.Random
import kotlin.time.Clock

fun ahora(): Long = Clock.System.now().toEpochMilliseconds()

fun nuevoId(prefix: String): String = "$prefix${ahora().toString(36)}${Random.nextInt(0, 1 shl 20).toString(36)}"

const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * "12 nov 2026" a partir de milisegundos desde 1970 (hora utc).
 * kotlin comun no trae calendario, asi que se usa el calculo clasico de dias a fecha civil:
 * se cuentan los dias en bloques de 400 años (146097 dias) con el año empezando en marzo,
 * que deja el dia bisiesto al final y simplifica las cuentas.
 */
fun fechaTexto(at: Long): String {
    val (year, mes, dia) = fechaCivil(at)
    return "$dia ${meses[mes - 1]} $year"
}

/** "2026-11-24": la forma en que las apis piden una fecha. */
fun fechaIso(at: Long): String {
    val (year, mes, dia) = fechaCivil(at)
    return "$year-${mes.toString().padStart(2, '0')}-${dia.toString().padStart(2, '0')}"
}

// año, mes (1-12) y dia
private fun fechaCivil(at: Long): Triple<Long, Int, Int> {
    val z = at.floorDiv(DAY) + 719468
    val era = z.floorDiv(146097L)
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val dia = doy - (153 * mp + 2) / 5 + 1
    val mes = if (mp < 10) mp + 3 else mp - 9
    val year = yoe + era * 400 + if (mes <= 2) 1 else 0
    return Triple(year, mes.toInt(), dia.toInt())
}

fun haceCuanto(at: Long, from: Long = ahora()): String {
    val d = (from - at).coerceAtLeast(0)
    return when {
        d < MINUTE -> "ahora"
        d < HOUR -> "${d / MINUTE} min"
        d < DAY -> "${d / HOUR} h"
        d < 7 * DAY -> "${d / DAY} d"
        d < 365 * DAY -> "${d / (7 * DAY)} sem"
        else -> "${d / (365 * DAY)} a"
    }
}
