# Cómo leer el código de MyPalette

Guía corta para orientarse y aprender del proyecto. Todo lo importante está en
`shared/src/commonMain/kotlin/com/example/palette`; los comentarios del código explican el detalle.

## Cómo está organizado

La estructura es la de la asignatura: `di`, `core` y `features`, y dentro de cada feature las capas
`domain`, `data`, `presentation` y `ui`.

| Carpeta | Qué hay |
|---|---|
| `di/` | `AppContainer`: crea una sola vez repositorios y casos de uso (`by lazy`) y los reparte |
| `core/` | Lo que no es de ninguna funcionalidad concreta: modelos compartidos (`model`), red (`network`), guardado local (`storage`), la fuente de datos (`data`), componentes y tema (`ui`) |
| `features/<nombre>/domain` | Modelos de la feature, interfaces de repositorio y casos de uso. No sabe de dónde salen los datos |
| `features/<nombre>/data` | Implementación de esos repositorios |
| `features/<nombre>/presentation` | `...State`, `...Action` y `...ViewModel`, los tres en el mismo fichero |
| `features/<nombre>/ui` | Las pantallas: `...ScreenRoot` (conecta con el ViewModel y la navegación) y `...Screen` (solo pinta) |
| `navigation/` | Las rutas (`Routes.kt`) y el `NavHost` (`AppNavigation.kt`) |

Las features:

| Feature | Pantallas | Repositorio |
|---|---|---|
| `auth` | Entrar, crear el perfil | `AuthRepository` |
| `catalog` | Registrar (buscador), ficha (con dónde verlo), Descubrir, buscador en hoja para listas y bloques | `CatalogRepository` (TMDB, IGDB, iTunes, Open Library) |
| `library` | Hoja de reseña, menú de un registro | `LibraryRepository` (mis registros y pendientes) |
| `home` | Inicio, historias | `FeedRepository` |
| `people` | Buscar gente | `PeopleRepository` |
| `messages` | Chats, grupos (editables), avisos, hoja de enviar | `MessagesRepository` |
| `profile` | Perfil, modo personalización (portada, fondo, bloques), ajustes | `ProfileRepository` |
| `reviews` | Una reseña con sus me gusta y comentarios | `ReviewsRepository` |
| `lists` | Listas de contenido o de reseñas, hoja para crear una | `ListsRepository` |

## El camino de un toque

Todas las pantallas funcionan igual. El ejemplo más corto es el buscador:

`LogScreen` → emite `LogAction.OnConsultaChanged` → `LogScreenRoot` se la pasa a `LogViewModel.onAction` →
`SearchMediaUseCase` → `CatalogRepository` (interfaz) → `CatalogRepositoryImpl` → TMDB, IGDB, iTunes, Open Library

y de vuelta: el ViewModel cambia su `LogState`, y `LogScreen`, que lo está observando, se repinta.

Tres reglas que se repiten en todas:

1. **La pantalla (`Screen`) no decide nada.** Recibe un `State` y avisa con un `Action`. No conoce ni el ViewModel
   ni la navegación, así que se puede previsualizar pasándole un estado a mano.
2. **El `ScreenRoot` reparte las acciones.** Las que cambian datos van al ViewModel; las que son navegar
   (abrir una ficha, volver atrás) las resuelve él llamando a las funciones que le pasa `AppNavigation`.
3. **El ViewModel no conoce Supabase ni TMDB.** Solo interfaces de repositorio y casos de uso, que le llegan por el constructor.

### Cuándo hay caso de uso

Solo donde hay una regla que no es ni de pantalla ni de datos: qué se recomienda en Descubrir
(`GetRecommendationsUseCase`), cómo se monta una ficha (`GetMediaDetailUseCase`), cómo se busca
(`SearchMediaUseCase`, `SearchPeopleUseCase`), cómo se ordena un perfil (`SortProfileLogsUseCase`).
Cuando el ViewModel solo tiene que guardar o leer algo, habla con el repositorio directamente.

### De dónde salen los datos

`core/data/PaletteStore` es la única fuente de datos propia: guarda una copia en el dispositivo (un JSON) y
habla con Supabase. Los repositorios de `auth`, `library`, `home`, `people`, `messages` y `profile` leen de ella
y entregan a cada feature solo lo que necesita, ya preparado (`Feed`, `Perfil`, `Hilo`...). Lo entregan como
`Flow`: el ViewModel lo colecciona y, cada vez que algo cambia, recibe el dato nuevo sin tener que pedirlo.

## Navegación

- `navigation/Routes.kt`: una ruta `@Serializable` por pantalla. Las que necesitan saber qué enseñar llevan solo un id.
- `navigation/AppNavigation.kt`: el `NavHost` asocia cada ruta con su `ScreenRoot`, y es el único sitio que usa el `NavController`.
- Cada pantalla abierta tiene su propio ViewModel, que se destruye al cerrarla.
- Las hojas inferiores (reseña, enviar, grupo nuevo...) no son rutas: se abren encima de la pantalla que haya (`Hojas`).

## Por dónde empezar

1. `features/catalog`, de `ui/LogScreen.kt` hacia abajo, siguiendo la cadena de arriba.
2. `di/AppContainer.kt`: dónde se crea cada pieza.
3. `navigation/AppNavigation.kt`: cómo se pasa de una pantalla a otra.
4. `core/model/Models.kt`: qué es un contenido, un registro, un usuario, un mensaje.
5. Una feature con datos propios, por ejemplo `features/home`: `FeedRepository` → `FeedRepositoryImpl` → `HomeViewModel` → `HomeScreen`.
6. `core/data/PaletteStore.kt`: copia local y sincronización con el servidor.

## Ideas que aparecen una y otra vez

| Idea | Qué es | Dónde verla |
|---|---|---|
| ViewModel, State y Action | El estado de una pantalla y lo que el usuario puede hacer en ella | `features/catalog/presentation/LogViewModel.kt` |
| Root y Screen | Separar la conexión (ViewModel, navegación) del dibujo | `features/catalog/ui/LogScreen.kt` |
| Repositorio | Una interfaz en `domain` y su implementación en `data` | `features/home/domain/repository`, `features/home/data/repository` |
| Inyección de dependencias | Nadie crea lo que necesita: se lo dan hecho | `di/AppContainer.kt` |
| `Flow` y `StateFlow` | Valores que cambian con el tiempo y avisan a quien los observa | `HomeViewModel.kt`, `FeedRepositoryImpl.kt` |
| `viewModelScope` | Corrutinas que se cancelan solas al cerrar la pantalla | `features/messages/presentation/ChatViewModel.kt` |
| `Job` y cancelación | Cancelar una búsqueda en marcha cuando se sigue escribiendo | `LogViewModel.kt`, función `buscar` |
| `async` y `coroutineScope` | Varias peticiones a la vez | `GetMediaDetailUseCase.kt`, `GetRecommendationsUseCase.kt` |
| `collectLatest` | Abandonar un trabajo cuando llega un dato más nuevo | `DiscoverViewModel.kt` |
| Rutas y `NavHost` | Navegar pasando solo un id | `navigation/Routes.kt`, `navigation/AppNavigation.kt` |
| Estado solo de interfaz | Lo que no cambia datos puede quedarse en la pantalla con `remember` | `SettingsScreen.kt` (`editando`), `StoryScreen.kt` (`progreso`) |
| Datos inmutables | Las clases de datos no se modifican: se copian con `.copy(...)` | cualquier `_state.update { it.copy(...) }` |
| Actualización optimista | El cambio se ve al momento y luego se sube al servidor | `core/data/PaletteStore.kt`, función `subir` |
| `expect` / `actual` | Una función común con una versión por plataforma | `core/storage/Storage.kt`, `core/ui/Photo.kt` |
| JSON a objetos | `@Serializable` convierte la respuesta de una API en clases | `features/catalog/data/remote/Tmdb.kt` |
| API REST | Leer y escribir en el servidor con peticiones HTTP | `core/network/Supabase.kt` |
| Dibujo a mano | Formas hechas con `Path` y curvas de Bézier | `core/ui/Blob.kt`, `core/ui/ColorPicker.kt` |
| Seguridad en el servidor | Reglas que deciden qué fila puede ver o cambiar cada usuario | `supabase/001_tablas.sql` |

## Librerías

| Para qué | Qué se usa |
|---|---|
| Inyección de dependencias | A mano, con `AppContainer`. No se usa Koin |
| Imágenes | Coil 3 (`AsyncImage`) |
| Red | Ktor |
| Navegación | Navigation Compose |
| Datos en el dispositivo | Un JSON guardado con `expect`/`actual` (`core/storage`). No hay base de datos SQL local: la base de datos es el Postgres de Supabase |

## Convenciones del proyecto

- **Nombres.** Variables, parámetros y funciones propias van en español. Se quedan en inglés los nombres de
  clases y de pantallas (`HomeScreen`, `Media`), lo que sigue la costumbre de la asignatura y de Compose
  (`state`, `onAction`, `OnBackClicked`, `modifier`, `onClick`) y los campos de las clases de `Models.kt` y de las
  respuestas de las APIs, porque son las claves con las que se guardan o llegan los datos.
- **Colores.** Nunca se escribe un color a mano en una pantalla: siempre sale de `Pigment` (`core/ui/Theme.kt`).
- **Comentarios.** En minúsculas y sin tildes, y solo donde el código no se explica solo.

## Para practicar

- Añade un tipo de contenido nuevo (por ejemplo podcasts): empieza por `MediaType` en `Models.kt` y deja que el
  compilador te diga qué `when` hay que completar.
- Añade una acción a una pantalla que ya existe, por ejemplo "limpiar búsqueda" en `LogAction`, y síguela
  hasta el ViewModel.
- Cambia cuántos favoritos por tipo usa Descubrir (`POR_TIPO` en `GetRecommendationsUseCase.kt`).
- Añade un dato más a la ficha de las películas en `features/catalog/data/remote/Tmdb.kt`, función `extras`.
