<div align="center">

# Palette

### Tu diario de pelis, series, juegos, música y libros, convertido en una paleta de color.

Puntúa lo último que has visto, jugado, leído o escuchado, escribe una reseña si te apetece<br>
y mira cómo tu perfil se llena de manchas de pintura con tus gustos.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7f52ff)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.1-4285f4)
![Plataformas](https://img.shields.io/badge/Android_%7C_iOS_%7C_Escritorio_%7C_Web-una_sola_base_de_código-12a679)
![Licencia](https://img.shields.io/badge/licencia-AGPL--3.0-lightgrey)

</div>

<br>

## Qué puedes hacer

- **Registrar.** Un único buscador para películas, series, videojuegos, álbumes, canciones y libros. Eliges, pones de media a cinco estrellas y, si quieres, una reseña.
- **Tu paleta.** Cada cosa que registras es una mancha con su portada. Se ordena por favoritos o por fecha, como un diario. Puedes fijar lo que más te define y ocultar lo que no quieras enseñar.
- **Inicio.** Lo último que han registrado tus amigos, y sus historias arriba, rodeadas de su color favorito.
- **Descubrir.** Recomendaciones a partir de lo que has puntuado con cuatro estrellas o más, sin repetir lo que ya tienes.
- **Fichas con contexto.** Reparto y duración en una película, temporadas en una serie, plataformas en un juego, la lista de canciones en un álbum.
- **Mensajes y grupos.** Comparte una ficha o una reseña con un amigo o con un grupo y coméntala ahí mismo.
- **Pendientes.** Guarda lo que quieres ver, jugar, leer o escuchar más adelante. Tú decides si los demás la ven.
- **Todo a tu color.** Color favorito, fondo de tu perfil y los colores de toda la app, elegidos entre todos los posibles.

## Cómo está hecha

Una sola base de código en **Kotlin Multiplatform** con la interfaz en **Compose Multiplatform**. El mismo código
da la app de Android, la de iOS, la de escritorio y la web.

| Parte | Con qué |
|---|---|
| Interfaz | Compose Multiplatform 1.12, Material 3 |
| Arquitectura | Capas `domain` / `data` / `presentation` / `ui` por funcionalidad, con ViewModel y estado inmutable |
| Navegación | Navigation Compose, con rutas serializables |
| Red | Ktor |
| Imágenes | Coil 3 |
| Servidor | Supabase: cuentas, base de datos Postgres, almacenamiento y funciones |
| Películas y series | [TMDB](https://www.themoviedb.org) |
| Videojuegos | [IGDB](https://www.igdb.com) |
| Música | iTunes Search |
| Libros | [Open Library](https://openlibrary.org) |

En pantallas anchas (escritorio, web, tablet) la barra de navegación pasa al lateral y las listas se reparten en columnas.

## Para desarrolladores

### Requisitos

- JDK 17 o superior
- Android Studio o IntelliJ IDEA con el plugin de Kotlin Multiplatform
- Xcode, solo para la app de iOS

### Ejecutar

```bash
./gradlew :desktopApp:run                        # escritorio
./gradlew :androidApp:assembleDebug              # Android
./gradlew :webApp:wasmJsBrowserDevelopmentRun    # web (Wasm)
./gradlew :webApp:jsBrowserDevelopmentRun        # web (JS)
./gradlew :shared:jvmTest                        # tests
```

iOS: abre `iosApp` en Xcode y ejecuta desde ahí.

Si la compilación web en JS falla con *Lock file was changed*, ejecuta una vez `./gradlew kotlinUpgradeYarnLock`.

### Clave de TMDB

Las películas y las series necesitan una clave gratuita de TMDB (*Ajustes > API* en themoviedb.org).
Va en `local.properties`, en la raíz del proyecto, que no se sube al repositorio:

```properties
TMDB_API_KEY=la_tuya
```

Mientras falte, esos dos tipos no aparecen en el buscador. Música y libros no necesitan nada.

### Servidor

Para montar tu propio servidor en un proyecto de Supabase:

1. **Base de datos.** En *SQL Editor*, ejecuta en orden los ficheros de `supabase/` (`001` a `004`).
   El `002` y el `004` llevan dentro la URL del proyecto: cámbiala por la tuya.
2. **Registro.** En *Authentication > Sign In / Providers > Email*, activa *Confirm email*. Las plantillas de correo se dejan como vienen.
   En *Authentication > URL Configuration*, pon en *Site URL* la dirección de la web de la app: los enlaces de los correos llevan ahí.
   El correo de serie de Supabase solo escribe a los miembros del proyecto: para usuarios de verdad hay que configurar un SMTP propio.
3. **Videojuegos.** Crea una *Edge Function* llamada `igdb` con el contenido de `supabase/functions/igdb/index.ts`
   y añade los secretos `IGDB_CLIENT_ID` e `IGDB_CLIENT_SECRET` (de una app registrada en la consola de desarrolladores de Twitch).
4. **Conexión.** Pon la URL del proyecto y su clave publicable al principio de `data/remote/Supabase.kt`.

### Dónde va cada credencial

| Credencial | Dónde vive | ¿En el repositorio? |
|---|---|---|
| Clave de TMDB | `local.properties` de cada desarrollador | No |
| Client ID y Client Secret de IGDB | Secretos de la función en Supabase | No |
| *Secret key* de Supabase | Solo en el panel de Supabase | No |
| URL y clave publicable de Supabase | `data/remote/Supabase.kt` | Sí: es pública por diseño |

Lo que puede hacer cada usuario no depende de la clave publicable sino de las reglas de la base de datos
(*row level security*): cada uno solo modifica lo suyo, los registros ocultos y los pendientes privados no salen
del servidor, y un mensaje solo lo ven quienes participan en la conversación.

### Estructura

Todo el código de la app está en `shared/src/commonMain/kotlin/com/example/palette`:

| Carpeta | Contenido |
|---|---|
| `di/` | `AppContainer`: crea una sola vez los repositorios y los casos de uso y los reparte a las pantallas |
| `core/` | Lo que no es de una funcionalidad concreta: modelos, red (Ktor y Supabase), guardado local, componentes y tema |
| `features/<nombre>/domain` | Modelos, interfaces de repositorio y casos de uso |
| `features/<nombre>/data` | Implementación de los repositorios |
| `features/<nombre>/presentation` | `State`, `Action` y `ViewModel` de cada pantalla |
| `features/<nombre>/ui` | Las pantallas: `ScreenRoot` (conecta con el ViewModel y la navegación) y `Screen` (solo pinta) |
| `navigation/` | Rutas, `NavHost` y la barra de cristal |

Las funcionalidades son `auth`, `catalog` (buscar, ficha y descubrir), `library` (registros y pendientes), `home`
(inicio e historias), `people`, `messages` y `profile`. En [`docs/APRENDER.md`](docs/APRENDER.md) hay una guía para leer el código.

Lo específico de cada plataforma (guardado local y selector de fotos) está en `androidMain`, `iosMain`, `jvmMain`,
`jsMain` y `wasmJsMain`. Los colores no se escriben a mano en las pantallas: salen siempre de `Pigment` (`core/ui/Theme.kt`).

## Estado del proyecto

Palette es un proyecto sin ánimo de lucro. Antes de publicarla en una tienda o de ganar dinero con ella haría falta,
como mínimo: licencia comercial de TMDB e IGDB, otra fuente para las carátulas de música, política de privacidad y
términos de uso, y poder denunciar y bloquear usuarios. En iOS falta además el selector de fotos.

## Créditos

- Datos e imágenes de [TMDB](https://www.themoviedb.org), [IGDB](https://www.igdb.com), iTunes y [Open Library](https://openlibrary.org).
  Este producto usa la API de TMDB pero no está avalado ni certificado por TMDB.
- Iconos de [Lucide](https://lucide.dev) (licencia ISC).
- Tipografía [Young Serif](https://github.com/noirblancrouge/YoungSerif) (SIL Open Font License, en `licenses/`).

## Licencia

© 2026 Lucas Suárez y colaboradores. Código publicado bajo la licencia **[GNU AGPL-3.0](LICENSE)**.

Puedes usar y estudiar Palette libremente. Si copias o modificas el código y lo distribuyes o lo publicas como web o app,
debes mantener el aviso de autoría y publicar tu código completo bajo la misma licencia.
