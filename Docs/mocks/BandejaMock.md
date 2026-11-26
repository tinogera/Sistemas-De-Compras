# BandejaMock · Bandeja (07)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/BandejaMock.html`.
Estilo: patrón de lista de [MisSolicitudesMock](MisSolicitudesMock.md) (pestañas + filtros + tabla + paginación, tarjetas en celular)
y chip VOS / avisos de [DetalleAprobacionMock](DetalleAprobacionMock.md). Usuario de ejemplo: Martín Ruiz, Encargado de Compras.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/BandejaMock.html`.
- **Con Claude Code:** "abrí el BandejaMock" o `! start frontend/MockPaginas/aprobadas/BandejaMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial | Resumen con 4 números (Sin tomar · Míos · Listos para comprar · Para atender), pestaña **Sin resolver**, tabla ordenada por urgencia y fecha necesaria |
| Clic en un número del resumen | Atajo: aplica la pestaña / filtro que corresponde (ej. "Listos para comprar" → Para comprar + Solo los míos) |
| Pestaña **Para comprar** | Solo ítems con aprobación terminada (`EN_COTIZACION` + `APROBADA`); los tuyos dicen "Comprar →" |
| Filtro de urgencia / **Solo los míos** | La lista se filtra; si no queda nada, vacío con "Quitar filtros" |
| **Tomar** en un ítem sin asignar | "Tomando…" y después la fila pasa a tuya (VOS, "Gestionar →") con toast |
| MAQUETA → **Otro Encargado toma antes** y después **Tomar** | Aviso amarillo `YA_ASIGNADO`: "Lucía Pérez tomó … antes que vos", la fila se actualiza y se ofrece "Pasármelo" |
| **Pasármelo** (ítem de otro Encargado) | Modal de confirmación con el ítem; al confirmar pasa a vos |
| MAQUETA → **Llega un ítem nuevo** | Fila con badge "Nuevo" en su lugar según el orden, toast naranja, suben los contadores |
| MAQUETA → **Se aprueba uno mío** | La fila se resalta y pasa a "Aprobado · para comprar" / "Comprar →" |
| MAQUETA → Cargando / Bandeja vacía / Error | Esqueleto; "No hay nada sin resolver"; error con Reintentar |
| Clic en la fila o en "Gestionar →" | Va a Gestión de un ítem (toast en la maqueta); el #número va a la solicitud |
| Ancho < 860 px | Tarjetas: ítem, urgencia + fecha, montos + estado, encargado + botón |

## Para implementarlo

**Ruta:** `/bandeja?vista=sin-resolver|para-comprar&urgencia=&mios=` (solo rol `ENCARGADO`; se llega desde Inicio o el menú).

| Qué | Endpoint |
|---|---|
| Lista | `GET /api/v1/bandeja?asignadosAMi=&urgencia=&paraComprar=&page=&size=` → `Pagina<BandejaItemResponse>` |
| Tomar | `POST /api/v1/bandeja/{itemId}/tomar` → `GestionItemDetalleResponse` |
| Pasármelo (reasignar) | `POST /api/v1/bandeja/{itemId}/reasignar` → `GestionItemDetalleResponse` |

- **Orden:** fijo en el backend (`prioridad`, `fechaNecesaria`, `itemId`); ignora `sort`. Se muestra el texto "Ordenado por urgencia y fecha necesaria".
- **Pestañas → parámetros:** Sin resolver = sin `paraComprar`; Para comprar = `paraComprar=true`.
- **Filtros → parámetros:** urgencia = `urgencia`; "Solo los míos" = `asignadosAMi=true`.
- **Contadores de pestañas:** `totalElementos` de cada consulta (se puede pedir con `size=1`).

### Columnas (`BandejaItemResponse`)

| Columna | Campos |
|---|---|
| Ítem | `detalle` × `cantidad`, `tipo` (PROD/SERV), `categoriaNombre`, `#solicitudId` |
| Urgencia | `urgencia` (barritas) |
| Necesaria | `fechaNecesaria` + "vencida hace…/es hoy/en N días" |
| Estimado / Total | `precioEstimado` `monedaEstimada` · `precioTotal` `monedaTotal` (o "Total sin fijar") |
| Estado | `estado` (PENDIENTE = "Sin tomar", EN_COTIZACION = "En cotización") + `estadoAprobacion` (EN_CURSO / APROBADA / null = "Falta fijar el precio") + `todasVencidas` |
| Encargado | `encargadoNombre` (+ VOS si `encargadoId` = usuario actual) o "Sin asignar" |
| Acción | sin encargado → **Tomar** · tuyo → **Gestionar →** / **Comprar →** · de otro → **Pasármelo** |

### Errores de negocio

| Código | Qué muestra |
|---|---|
| `YA_ASIGNADO` (409) | Aviso amarillo arriba de la tabla, refrescar la fila y ofrecer "Pasármelo" |
| `GESTION_ESTADO_INVALIDO` | Toast con el mensaje del backend y refrescar la lista (el ítem cambió de estado) |

### Lo que el backend todavía no trae

| Necesidad | Propuesta |
|---|---|
| Número "Sin tomar" del resumen | filtro `sinAsignar=true` (hoy no se puede pedir solo los que no tienen Encargado) |
| Número "Para atender" (fecha vencida o cotizaciones vencidas) | un `GET /api/v1/bandeja/resumen` con los 4 números, o sacar esa tarjeta en la primera versión |
| Contador naranja de "Bandeja" en el menú | el mismo resumen (sin tomar) |
| Solicitante y sector en la fila (útil para priorizar) | `solicitanteNombre` y `sector` en `BandejaItemResponse` (no se muestran en el mock) |

Sin el resumen, la primera versión puede salir con los contadores de pestaña (que sí salen de `totalElementos`) y sin las 4 tarjetas.

### Tiempo real
Con la señal SSE de compras: volver a pedir la página actual y los contadores; marcar con "Nuevo" los ítems que no estaban y avisar con toast.
Si un ítem tuyo pasa a `APROBADA`, resaltar la fila y avisar "Ya lo podés comprar".

### Decisiones de diseño
- **Sin botón naranja en las filas:** Tomar / Gestionar / Comprar son secundarios y "Pasármelo" es fantasma (más discreto, porque le saca el ítem a otro).
  El naranja queda para el contador, la pestaña activa y el chip VOS (regla de LayoutMock: un solo naranja de acción por pantalla).
- **Tomar no navega:** el Encargado puede tomar varios seguidos; después entra con "Gestionar →".
- **Pasármelo pide confirmación**, porque cambia el trabajo de otra persona.
- Las filas tuyas llevan una marca gris a la izquierda para encontrarlas rápido sin filtrar.

**Solo de la maqueta:** la franja MAQUETA (estados y eventos simulados) y los datos de ejemplo.
