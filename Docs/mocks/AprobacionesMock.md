# AprobacionesMock · Aprobaciones (05)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/AprobacionesMock.html`.
Estilo: patrón de lista de [MisSolicitudesMock](MisSolicitudesMock.md) (pestañas + tabla + paginación, tarjetas en celular)
y badges / chip VOS de [DetalleAprobacionMock](DetalleAprobacionMock.md).

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/AprobacionesMock.html`.
- **Con Claude Code:** "abrí el AprobacionesMock" o `! start frontend/MockPaginas/aprobadas/AprobacionesMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Pestaña **Para decidir** | Ítems donde te toca a vos: chip VOS, "esperando hace…" (amarillo si son 2+ días) y botón "Decidir →" |
| Pestaña **En curso** | Solo lectura: a quién le toca ahora y desde cuándo |
| Pestaña **Decididas** | Tu decisión (Aprobaste / Rechazaste + fecha) y cómo terminó la cadena |
| MAQUETA → ver como **Admin** | Aparece la pestaña **Todas** (marcada ADMIN) y el menú de Admin |
| MAQUETA → **Llega uno nuevo en vivo** | Fila nueva arriba con badge "Nuevo", toast, y el contador de la pestaña y del menú suben |
| MAQUETA → Cargando / Vacío / Error | Esqueleto; vacío por pestaña ("Estás al día" en Para decidir); error con Reintentar |
| Clic en una fila | Va al Detalle de aprobación (toast en la maqueta); el #número va a la solicitud |
| Ancho < 860 px | Tarjetas: ítem + urgencia, solicitante, paso + montos, botón a lo ancho |

## Para implementarlo

**Ruta:** `/aprobaciones?vista=para-decidir|en-curso|decididas|todas` (el menú "Aprobaciones" aparece si `resumen.esAprobador` o si es Admin).

| Qué | Endpoint |
|---|---|
| Lista | `GET /api/v1/aprobaciones?vista=&page=&size=` → `Pagina<Fila>` (default 20 por página) |
| Contador (pestaña y menú) | `GET /api/v1/aprobaciones/resumen` → `{ esAprobador, paraDecidir }` |

- **Orden:** lo define el backend (urgencia y antigüedad) y no acepta `sort`, por eso no hay encabezados ordenables;
  se muestra el texto "Ordenado por urgencia y antigüedad".
- **Filtros:** el backend no tiene; la pantalla sale sin barra de filtros (cuando haya, se suma la del patrón de Mis solicitudes).
- `todas` devuelve 403 si no sos Admin: ocultar la pestaña.

### Columnas por pestaña (`Fila`)

| Pestaña | Columnas |
|---|---|
| Para decidir | Ítem (`detalle` × `cantidad`, `categoriaNombre`) · Solicitante (`solicitanteNombre`, `sector`, `#solicitudId`) · Urgencia · Estimado / Total · Regla (`reglaNombre`) · Paso (`aprobadorNombre` + VOS si `puedoDecidir`) · Decidir |
| En curso | igual, sin "Decidir"; "Le toca a" = `aprobadorNombre` |
| Decididas | Ítem · Solicitante · Urgencia · Total · Tu decisión · Cómo terminó (`estado` de la cadena) |
| Todas (Admin) | Ítem · Solicitante · Urgencia · Estimado / Total · Regla · Estado · Le toca a |

### Lo que `Fila` todavía no trae

| Necesidad | Propuesta |
|---|---|
| "esperando hace 3 h" (antigüedad del paso activo) | `activoDesde` (Instant) en `Fila` |
| "Tu decisión" en Decididas (aprobaste / rechazaste y cuándo) | `estadoPaso` y `decididoEn` del paso del usuario |

Sin esos campos, la primera versión puede mostrar solo el estado de la cadena.

### Tiempo real
Con la señal SSE: refrescar `resumen` (contador) y, si estás en Para decidir, la lista; marcar las filas nuevas con "Nuevo" y avisar con toast.

### Decisiones de diseño
- El botón "Decidir →" de cada fila es **secundario** (no naranja): con varias filas, el naranja queda para el contador y el chip VOS,
  respetando la regla de LayoutMock de un solo naranja de acción por pantalla.
- La decisión se toma siempre en el Detalle de aprobación (no hay aprobar desde la lista): así se ve el precio y la cadena antes de decidir.

**Solo de la maqueta:** la franja MAQUETA (selector de rol, estados, evento simulado) y los datos de ejemplo.
