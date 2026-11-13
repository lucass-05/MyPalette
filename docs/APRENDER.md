# Cómo leer el código de MyPalette

Guía corta para orientarse y aprender del proyecto. Todo lo importante está en
`shared/src/commonMain/kotlin/com/example/palette`; los comentarios del código explican el detalle.

## Por dónde empezar

1. `App.kt` y `navigation/AppNavigation.kt`: cómo arranca la app y cómo se decide qué pantalla se ve.
2. `data/Models.kt`: qué es un contenido, un log, un usuario, un mensaje.
3. `data/PaletteStore.kt`: el estado de la app y todo lo que se puede hacer con él.
4. Una pantalla sencilla, por ejemplo `screens/LogScreen.kt`, y después `screens/DetailScreen.kt`.

## Ideas que aparecen una y otra vez

| Idea | Qué es | Dónde verla |
|---|---|---|
| Estado de Compose | Un valor que, al cambiar, repinta lo que lo usa (`mutableStateOf`, `remember`) | `navigation/Nav.kt`, `screens/LogScreen.kt` |
| Datos inmutables | Las clases de datos no se modifican: se copian con `.copy(...)` | `data/PaletteStore.kt`, función `actualizar` |
| Actualización optimista | El cambio se ve al momento y luego se sube al servidor | `data/PaletteStore.kt`, función `subir` |
| Corrutinas | Trabajo que espera (red) sin bloquear la pantalla: `suspend`, `launch`, `async` | `data/remote/Catalog.kt` |
| `LaunchedEffect` | Lanzar trabajo cuando algo cambia y cancelar el anterior | `screens/LogScreen.kt` (búsqueda mientras escribes) |
| `expect` / `actual` | Una función común con una versión por plataforma | `data/Storage.kt`, `ui/Photo.kt` |
| JSON a objetos | `@Serializable` convierte la respuesta de una API en clases | `data/remote/Tmdb.kt` |
| API REST | Leer y escribir en el servidor con peticiones HTTP | `data/remote/Supabase.kt` |
| Dibujo a mano | Formas hechas con `Path` y curvas de Bézier | `ui/Blob.kt`, `ui/ColorPicker.kt` |
| Seguridad en el servidor | Reglas que deciden qué fila puede ver o cambiar cada usuario | `supabase/001_tablas.sql` |

## Convenciones del proyecto

- **Nombres.** Variables, parámetros y funciones propias van en español. Se quedan en inglés los nombres de
  clases y de pantallas (`HomeScreen`, `Media`), los parámetros que siguen la costumbre de Compose (`modifier`,
  `onClick`, `text`) y los campos de las clases de `Models.kt` y de las respuestas de las APIs, porque son las
  claves con las que se guardan o llegan los datos.
- **Colores.** Nunca se escribe un color a mano en una pantalla: siempre sale de `Pigment` (`ui/Theme.kt`).
- **Comentarios.** En minúsculas y sin tildes, y solo donde el código no se explica solo.

## Para practicar

- Añade un tipo de contenido nuevo (por ejemplo podcasts): empieza por `MediaType` en `Models.kt` y deja que el
  compilador te diga qué `when` hay que completar.
- Cambia cada cuánto se refrescan los datos (`delay(15_000)` en `AppNavigation.kt`) y observa el efecto con dos cuentas.
- Añade un dato más a la ficha de las películas en `data/remote/Tmdb.kt`, función `extras`.
