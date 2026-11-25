package com.example.palette.core.monetizacion

import com.example.palette.core.plataforma.parametroDelEnlace

/**
 * los interruptores de todo lo que da dinero. con todo apagado la app se ve como siempre.
 *
 * para encender algo basta con cambiar aqui false por true y volver a publicar:
 * - [afiliados]: en la ficha salen los botones de escuchar/comprar y las plataformas se pueden tocar.
 *   los ids de afiliado se ponen en [Afiliados].
 * - [anuncios]: en descubrir sale el cartel de arriba y la fila patrocinada con los anuncios de
 *   features/anuncios/data/AnunciosContratados.kt.
 *
 * [demostracion] enseña los huecos con anuncios y enlaces de ejemplo, marcados como "ejemplo", para
 * enseñarselo a una empresa. en la web no hace falta tocar codigo: basta con abrir el enlace con
 * ?demo al final (https://lucass-05.github.io/MyPalette/?demo).
 */
object Monetizacion {
    const val AFILIADOS_ACTIVOS = false
    const val ANUNCIOS_ACTIVOS = false
    const val DEMOSTRACION_SIEMPRE = false

    private val enlaceConDemo: Boolean by lazy { parametroDelEnlace("demo") != null }

    // solo para las capturas de las pruebas
    internal var forzarDemostracion = false

    /** true si se abrio la web con ?demo (o si DEMOSTRACION_SIEMPRE esta encendido). */
    val demostracion: Boolean get() = DEMOSTRACION_SIEMPRE || forzarDemostracion || enlaceConDemo

    val afiliados: Boolean get() = AFILIADOS_ACTIVOS || demostracion
    val anuncios: Boolean get() = ANUNCIOS_ACTIVOS || demostracion
}
