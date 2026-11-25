package com.example.palette.core.metricas

/**
 * estadisticas de uso: que se abre, que se busca, que se toca. sirven para saber como se usa la app
 * y, mas adelante, para enseñar numeros a anunciantes y programas de afiliados.
 *
 * son anonimas: cada dispositivo tiene un id inventado y nunca se manda quien es la cuenta.
 * los nombres de los eventos y de sus datos estan en [Evento], en un solo sitio.
 */
interface Metricas {
    /** si se mandan o no. se puede apagar desde perfil y cuenta. */
    var activadas: Boolean

    fun registrar(evento: String, datos: Map<String, String> = emptyMap())
}

/** para las pruebas y las capturas: no apunta nada. */
object SinMetricas : Metricas {
    override var activadas = false
    override fun registrar(evento: String, datos: Map<String, String>) = Unit
}

/** todos los eventos que se apuntan. un nombre corto y en minusculas, igual en todas las plataformas. */
object Evento {
    const val ABRIR_APP = "abrir_app"
    const val VER_PANTALLA = "ver_pantalla"
    const val CREAR_CUENTA = "crear_cuenta"
    const val INICIAR_SESION = "iniciar_sesion"
    const val BUSCAR = "buscar"
    const val ABRIR_FICHA = "abrir_ficha"
    const val REGISTRAR = "registrar_contenido"
    const val PENDIENTE = "anadir_pendiente"
    const val CLIC_RECOMENDACION = "clic_recomendacion"
    const val CLIC_PLATAFORMA = "clic_plataforma"
    const val CLIC_AFILIADO = "clic_afiliado"
    const val ANUNCIO_VISTO = "anuncio_visto"
    const val ANUNCIO_CLIC = "anuncio_clic"
    const val SEGUIR = "seguir"
    const val ME_GUSTA = "me_gusta"
    const val COMENTAR = "comentar"
    const val MENSAJE = "enviar_mensaje"
    const val VER_HISTORIA = "ver_historia"
    const val CREAR_LISTA = "crear_lista"
    const val PERSONALIZAR = "personalizar_perfil"
    const val TUTORIAL = "tutorial"
}
