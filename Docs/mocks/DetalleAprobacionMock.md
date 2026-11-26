# DetalleAprobacionMock · Detalle de aprobación (06)

Estado: **APROBADA** (2026-11-25). Mock: `frontend/MockPaginas/aprobadas/DetalleAprobacionMock.html`.
Estilo: [LayoutMock](LayoutMock.md). Caso de ejemplo: el mismo ítem que en [DetalleSolicitudMock](DetalleSolicitudMock.md)
(Monitor 27" 4K de la solicitud #1047), visto por Ana Torres de Gerencia General.

Esta pantalla define la **versión completa del componente Pasos de aprobación**, que después se usa en 04, 08 y 13.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/DetalleAprobacionMock.html`.
- **Con Claude Code:** "abrí el DetalleAprobacionMock" o `! start frontend/MockPaginas/aprobadas/DetalleAprobacionMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial (**Te toca decidir**) | Tarjeta naranja a la derecha con Aprobar / Rechazar y el precio estimado vs. total; en la cadena, tu caja marcada **VOS** |
| **Aprobar** → Sí, aprobar | Modal con resumen; al confirmar, tu paso queda Aprobado, Dirección pasa a "Le toca", el contador del menú baja a 4 y la tarjeta dice "Aprobaste el paso 2" |
| **Rechazar** sin motivo | Error "Escribí el motivo…"; los chips rellenan motivos frecuentes; contador 0/1000 |
| **Rechazar** con motivo | La cadena queda Rechazada, el paso 3 Omitido y el motivo aparece citado en tu caja |
| MAQUETA → **Precio cambia en vivo** | El total sube a ARS 640.000, se resalta y aparece un toast "Revisalo antes de decidir" |
| MAQUETA → **Otro decidió primero** | Al aprobar: aviso `CONFLICTO_VERSION`, botones deshabilitados y "Recargar" |
| MAQUETA → **Aún no te toca** | Vista de Dirección (paso 3): tarjeta gris "Aún no te toca", modo lectura |
| MAQUETA → **Cadena aprobada / rechazada** | Estados finales con el nodo de cierre ("Compras puede comprar" / "Rechazado") |
| MAQUETA → **Pedido propio** | "Es tu propio pedido" (`PEDIDO_PROPIO`), sin botones |
| Ancho < 1100 px | La tarjeta de decisión pasa arriba de la cadena |

## Componente: Pasos de aprobación

### Anatomía (versión completa)

1. **Cabecera:** regla aplicada (`reglaNombre`), condición en mono y badge del estado de la cadena (`EstadoCadena`).
2. **Progreso:** un segmento por `orden`. Verde = paso completo, naranja parcial = paso activo (porcentaje que ya aprobó),
   rojo = rechazado, rayado = omitido, gris = falta. Texto: "1 de 3 pasos completos".
3. **Pasos:** un nodo numerado por `orden` (✓ si se completó, ✕ si se rechazó, naranja con halo si está activo, punteado si omitido).
   - Si hay más de un aprobador con el mismo `orden`: etiqueta **EN PARALELO · TIENEN QUE APROBAR TODOS** y "N de M aprobaron".
   - Cada aprobador es una caja: ícono de rol (persona) o sector (edificio), nombre, tipo `ROL`/`SECTOR`, badge de estado,
     y una línea de detalle (quién decidió y cuándo, "Le toca ahora · desde hace 3 h" con punto que late, o por qué está en espera).
   - Caja con borde naranja + chip **VOS** cuando `puedoDecidir` es true.
   - Motivo de rechazo citado con borde rojo.
4. **Nodo final:** qué pasa cuando termina ("Compras puede comprar" o "Rechazado").

### Mapeo desde el backend (`PasoDetalle`)

| Campo | Se muestra como |
|---|---|
| `orden` | Agrupa las cajas; mismo orden = en paralelo |
| `aprobadorNombre` | Nombre de la caja (rol o sector) |
| `estado` = `PENDIENTE` + `activo` | Badge "Le toca" (naranja) |
| `estado` = `PENDIENTE` + `!activo` | Badge "Aún no le toca" (punteado) y caja punteada |
| `APROBADO` / `RECHAZADO` / `OMITIDO` / `CANCELADO` | Badges de LayoutMock |
| `decididoPorNombre`, `decididoEn` | "Aprobó **Laura Méndez** · 24/11 10:32 (hace 1 día)" |
| `motivo` | Cita con borde rojo |
| `puedoDecidir` | Chip VOS + habilita la tarjeta de decisión |
| `version` | Se manda al aprobar / rechazar |

> El backend no manda si el aprobador es ROL o SECTOR en `PasoDetalle`: hoy se puede deducir del nombre
> (Supervisor = rol). Conviene sumar un campo `tipoAprobador` para el ícono.

### Variantes

| Variante | Dónde | Diferencias |
|---|---|---|
| **Completa** | 06 Detalle de aprobación | Todo lo de arriba |
| **Compacta** | 04 Detalle de solicitud, 08 Gestión de un ítem | Sin progreso, sin íconos de aprobador, sin nodo final, nodos más chicos |
| **Editable** | 13 Reglas de aprobación | Sin estados ni fechas; pasos arrastrables, agregar aprobador al mismo orden (paralelo) o a uno nuevo |

> La 04 (aprobada) usa la versión compacta que se dibujó antes. Si querés que tome algo de esta versión
> (chip VOS, "N de M aprobaron", etiqueta "tienen que aprobar todos"), es un cambio a proponer sobre la 04.

## Para implementarlo

**Ruta:** `/aprobaciones/items/:itemId`.

| Qué | Endpoint |
|---|---|
| Detalle + pasos | `GET /api/v1/aprobaciones/items/{itemId}` → `Detalle` |
| Aprobar | `POST /api/v1/aprobaciones/pasos/{pasoId}/aprobar` `{ version }` |
| Rechazar | `POST /api/v1/aprobaciones/pasos/{pasoId}/rechazar` `{ motivo (≤ 1000), version }` |
| Contador del menú | `GET /api/v1/aprobaciones/resumen` → `{ esAprobador, paraDecidir }` (refrescar después de decidir) |
| "Siguiente para decidir" | primer ítem de `GET /api/v1/aprobaciones?vista=para-decidir` |

Cotizaciones y adjuntos: el `Detalle` trae `proveedor` y montos; la tabla de cotizaciones del mock necesitaría un endpoint
de lectura para aprobadores (hoy las cotizaciones se piden desde la bandeja del Encargado). **Propuesta:** sumar las cotizaciones
al `Detalle` o mostrar solo el proveedor elegido en la primera versión.

### Errores de negocio

| Código | Qué muestra |
|---|---|
| `CONFLICTO_VERSION` | Aviso amarillo "Otra persona decidió este paso" + Recargar; botones deshabilitados |
| `PASO_NO_ACTIVO` | Tarjeta gris "Aún no te toca" |
| `PEDIDO_PROPIO` | Tarjeta gris "Es tu propio pedido" |
| `APROBACION_CERRADA` | Recargar: la cadena ya terminó |
| `MOTIVO_REQUERIDO` | Error en el campo motivo (también se valida en el front) |

### Tiempo real
Con la señal SSE del ítem: volver a pedir el detalle. Si cambió el precio, resaltar el total y avisar con toast.
Si la cadena se recalculó (cambian `version`/pasos), mostrar el aviso de conflicto antes de dejar decidir.

**Solo de la maqueta:** la franja MAQUETA, los eventos simulados y los datos de ejemplo.
