# Cómo leer el código de MyPalette

Guía corta para orientarse y aprender del proyecto. Todo lo importante está en
`shared/src/commonMain/kotlin/com/example/palette`; los comentarios del código explican el detalle.

## Cómo está organizado

La estructura es la de la asignatura (tema 5): `di`, `core` y `features`.

| Carpeta | Qué hay |
|---|---|
| `di/` | `AppContainer`: crea una sola vez repositorios y casos de uso y los reparte |
| `core/` | Lo que no es de ninguna funcionalidad concreta: red (`network`), guardado local (`storage`), modelos compartidos (`model`), componentes y tema (`ui`) |
| `features/<nombre>/domain` | Interfaces de repositorio y casos de uso. No depende de nadie |
| `features/<nombre>/data` | Implementación de esos repositorios: llamadas a las APIs |
| `features/<nombre>/presentation` | `...State`, `...Action` y `...ViewModel` |
| `features/<nombre>/ui` | Las pantallas: `...ScreenRoot` (conecta con el ViewModel) y `...Screen` (solo pinta) |
| `navigation/` | Qué pantalla se ve |

### Qué está migrado y qué no

La funcionalidad **catalog / Registrar** está hecha entera con este esquema y es el ejemplo a seguir:

`LogScreen` → `LogViewModel` → `SearchMediaUseCase` → `CatalogRepository` (interfaz) → `CatalogRepositoryImpl` → TMDB, IGDB, iTunes, Open Library

El resto de pantallas ya están en su carpeta de `features`, pero por dentro siguen como antes: leen y escriben
directamente en `core/data/PaletteStore` y no tienen ViewModel. Tampoco se usa todavía la librería de navegación
(tema 7): `navigation/Nav.kt` sigue siendo una pila hecha a mano. Son los siguientes pasos.

## Por dónde empezar

1. `features/catalog`, de `ui/LogScreen.kt` hacia abajo, siguiendo la cadena de arriba. Es el ejemplo completo.
2. `di/AppContainer.kt`: dónde se crea cada pieza.
3. `core/model/Models.kt`: qué es un contenido, un log, un usuario, un mensaje.
4. `core/data/PaletteStore.kt`: el estado del resto de la app, lo que queda por repartir en repositorios.

## Ideas que aparecen una y otra vez

| Idea | Qué es | Dónde verla |
|---|---|---|
| Estado de Compose | Un valor que, al cambiar, repinta lo que lo usa (`mutableStateOf`, `remember`) | `navigation/Nav.kt`, `features/home/ui/StoryScreen.kt` |
| Datos inmutables | Las clases de datos no se modifican: se copian con `.copy(...)` | `core/data/PaletteStore.kt`, función `actualizar` |
| Actualización optimista | El cambio se ve al momento y luego se sube al servidor | `core/data/PaletteStore.kt`, función `subir` |
| Corrutinas | Trabajo que espera (red) sin bloquear la pantalla: `suspend`, `launch`, `async` | `features/catalog/data/repository/CatalogRepositoryImpl.kt` |
| ViewModel, State y Action | El estado de una pantalla y lo que el usuario puede hacer en ella | `features/catalog/presentation/LogViewModel.kt` |
| `Job` y cancelación | Cancelar una búsqueda en marcha cuando se sigue escribiendo | `LogViewModel.kt`, función `buscar` |
| `LaunchedEffect` | Lanzar trabajo desde una pantalla cuando algo cambia | `features/catalog/ui/DetailScreen.kt` |
| `expect` / `actual` | Una función común con una versión por plataforma | `core/storage/Storage.kt`, `core/ui/Photo.kt` |
| JSON a objetos | `@Serializable` convierte la respuesta de una API en clases | `features/catalog/data/remote/Tmdb.kt` |
| API REST | Leer y escribir en el servidor con peticiones HTTP | `core/network/Supabase.kt` |
| Dibujo a mano | Formas hechas con `Path` y curvas de Bézier | `core/ui/Blob.kt`, `core/ui/ColorPicker.kt` |
| Seguridad en el servidor | Reglas que deciden qué fila puede ver o cambiar cada usuario | `supabase/001_tablas.sql` |

## Convenciones del proyecto

- **Nombres.** Variables, parámetros y funciones propias van en español. Se quedan en inglés los nombres de
  clases y de pantallas (`HomeScreen`, `Media`), los parámetros que siguen la costumbre de Compose (`modifier`,
  `onClick`, `text`) y los campos de las clases de `Models.kt` y de las respuestas de las APIs, porque son las
  claves con las que se guardan o llegan los datos.
- **Colores.** Nunca se escribe un color a mano en una pantalla: siempre sale de `Pigment` (`core/ui/Theme.kt`).
- **Comentarios.** En minúsculas y sin tildes, y solo donde el código no se explica solo.

## Para practicar

- Añade un tipo de contenido nuevo (por ejemplo podcasts): empieza por `MediaType` en `Models.kt` y deja que el
  compilador te diga qué `when` hay que completar.
- Cambia cada cuánto se refrescan los datos (`delay(15_000)` en `AppNavigation.kt`) y observa el efecto con dos cuentas.
- Añade un dato más a la ficha de las películas en `features/catalog/data/remote/Tmdb.kt`, función `extras`.
