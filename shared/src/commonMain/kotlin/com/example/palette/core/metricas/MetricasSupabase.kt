package com.example.palette.core.metricas

import com.example.palette.core.network.Supabase
import com.example.palette.core.plataforma.VERSION_APP
import com.example.palette.core.plataforma.idiomaDelDispositivo
import com.example.palette.core.plataforma.nombreDePlataforma
import com.example.palette.core.plataforma.paginaDeOrigen
import com.example.palette.core.plataforma.parametroDelEnlace
import com.example.palette.core.storage.escribirGuardado
import com.example.palette.core.storage.leerGuardado
import com.example.palette.core.util.ahora
import com.example.palette.core.util.nuevoId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * guarda los eventos en memoria y los sube a la tabla events de supabase en grupos: cada 15 segundos
 * o cuando se juntan 20. si la subida falla se pierden, que para estadisticas no importa.
 */
internal class MetricasSupabase(private val servidor: Supabase) : Metricas {

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val candado = Mutex()
    private val pendientes = mutableListOf<FilaDeEvento>()

    // un id inventado por dispositivo (para contar usuarios y si vuelven) y otro por cada vez que se abre
    private val dispositivo: String = leerGuardado(CLAVE_DISPOSITIVO) ?: nuevoId("d").also { escribirGuardado(CLAVE_DISPOSITIVO, it) }
    private val visita = nuevoId("v")
    private val plataforma = nombreDePlataforma()

    override var activadas: Boolean = leerGuardado(CLAVE_ACTIVADAS) != "no"
        set(valor) {
            field = valor
            escribirGuardado(CLAVE_ACTIVADAS, if (valor) "si" else "no")
        }

    init {
        // de donde viene la visita: con esto se sabe si funciona un video de tiktok o salir en google
        registrar(
            Evento.ABRIR_APP,
            buildMap {
                paginaDeOrigen()?.let { put("origen", it.take(200)) }
                listOf("utm_source", "utm_medium", "utm_campaign").forEach { clave -> parametroDelEnlace(clave)?.let { put(clave, it.take(100)) } }
                idiomaDelDispositivo()?.let { put("idioma", it) }
            },
        )
        ambito.launch {
            // la apertura se sube enseguida: si alguien entra y se va en unos segundos tambien cuenta
            delay(3_000)
            vaciar()
            while (true) {
                delay(CADA)
                vaciar()
            }
        }
    }

    override fun registrar(evento: String, datos: Map<String, String>) {
        if (!activadas) return
        val fila = FilaDeEvento(evento, datos, dispositivo, visita, plataforma, VERSION_APP, servidor.conSesion, ahora())
        ambito.launch {
            val lleno = candado.withLock {
                pendientes += fila
                pendientes.size >= LOTE
            }
            if (lleno) vaciar()
        }
    }

    private suspend fun vaciar() {
        val lote = candado.withLock { pendientes.toList().also { pendientes.clear() } }
        if (lote.isEmpty()) return
        try {
            servidor.subirEventos(lote)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Palette: no se pudieron subir ${lote.size} eventos (${e.message})")
        }
    }

    private companion object {
        const val CLAVE_DISPOSITIVO = "palette.dispositivo"
        const val CLAVE_ACTIVADAS = "palette.metricas"
        const val CADA = 15_000L
        const val LOTE = 20
    }
}

/** una fila de la tabla events (supabase/007). */
@Serializable
internal class FilaDeEvento(
    val name: String,
    val props: Map<String, String>,
    val device: String,
    val visit: String,
    val platform: String,
    val version: String,
    val logged: Boolean,
    @SerialName("at") val momento: Long,
)
