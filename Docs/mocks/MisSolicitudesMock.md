# MisSolicitudesMock · Mis solicitudes (02)

Estado: **APROBADA** (2026-11-25). Mock: `frontend/MockPaginas/aprobadas/MisSolicitudesMock.html`.
Estilo: tabla paginada, filtros, badges y estados de [LayoutMock](LayoutMock.md). Es la primera tabla real:
el resto de las listas (Aprobaciones, Bandeja, Órdenes, Usuarios…) sigue este patrón.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/MisSolicitudesMock.html`.
- **Con Claude Code:** "abrí el MisSolicitudesMock" o `! start frontend/MockPaginas/aprobadas/MisSolicitudesMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Pestañas Todas / En revisión / Listas / Cerradas / Canceladas | Filtra por estado (también con ← → del teclado) |
| "¿Qué significa cada estado?" | Explicación de cada badge |
| Buscar "1047" o "notebook" | Busca por número o por el detalle del primer ítem |
| Urgencia, Ítems, Necesaria desde/hasta | Filtra al instante y aparecen chips (× para quitar cada uno, "Limpiar todo") |
| Clic en "N.º · Creada", "Urgencia" o "Necesaria" | Ordena; la flecha naranja indica el orden actual |
| Columna "Necesaria" en solicitudes En revisión | "en 2 días" (amarillo si faltan ≤ 3), "vencida hace 1 día" (rojo) |
| Clic o Enter en una fila | Toast "Abrir solicitud #…" (en la app real va al detalle) |
| Paginación y "Por página" | 8 / 20 / 50 filas |
| MAQUETA → **Recién creada** | Toast de éxito y la fila #1052 resaltada con badge "Nueva" (cuando volvés de Nueva solicitud) |
| MAQUETA → Cargando / Primera vez / Sin resultados / Error | Esqueleto, vacío con "+ Nueva solicitud", vacío con "Limpiar filtros", error con "Reintentar" |
| Ancho < 760 px | La tabla pasa a tarjetas (número + estado arriba, ítems, urgencia + fecha abajo) |

## Para implementarlo

**Ruta:** `/solicitudes` (se llega desde Inicio o el menú).

**Endpoint:** `GET /api/v1/solicitudes?mias=true&estado=&page=&size=&sort=`
→ `Pagina<SolicitudResumenResponse>` = `{ contenido, pagina, tamanio, totalElementos, totalPaginas }`.

| Control | Parámetro |
|---|---|
| Pestaña de estado | `estado=EN_REVISION | LISTA | CERRADA | CANCELADA` (vacío = todas) |
| Orden por columna | `sort=creadaEn,desc` (default) · `sort=fechaNecesaria,asc` · `sort=urgencia,desc` |
| Paginación | `page` (base 0) y `size` (8 / 20 / 50) |

Guardar filtros, pestaña, orden y página en la URL (`?estado=LISTA&page=2`) para que "volver" desde el detalle respete la vista.

### Estados de la solicitud (enum `EstadoSolicitud`)

| Backend | Badge | Significado |
|---|---|---|
| `EN_REVISION` | En revisión (azul, reloj) | Algún ítem sin resolver |
| `LISTA` | Lista (verde, check) | Todos los ítems resueltos; falta enviar alguna orden |
| `CERRADA` | Cerrada (gris claro, candado) | Todo resuelto y órdenes enviadas |
| `CANCELADA` | Cancelada (gris, tachado) | Cancelada por el solicitante |

> Nota: LayoutMock (aprobada) mostraba como estados de solicitud "Pendiente / En gestión / Lista". Los reales del
> backend son estos cuatro. Propuesta: actualizar la fila "Estado de la solicitud" de LayoutMock (pendiente de tu ok).

### Lo que el backend todavía no da (hace falta sumarlo)

| Necesidad del mock | Hoy | Propuesta |
|---|---|---|
| Detalle del primer ítem en la fila | `SolicitudResumenResponse` solo trae `id, creadaEn, urgencia, fechaNecesaria, estado, cantidadItems` | Agregar `primerItemDetalle` (o `resumenItems`) |
| Filtro por urgencia | no existe | `?urgencia=ALTA` |
| Filtro por rango de fecha necesaria | no existe | `?necesariaDesde=&necesariaHasta=` |
| Filtro por cantidad de ítems | no existe | `?itemsMin=&itemsMax=` |
| Buscar por número o detalle | no existe | `?q=` |

Mientras no estén, la primera versión puede salir solo con pestañas de estado, orden y paginación.

### Componentes
- Reutilizados de LayoutMock: `BarraSuperior`, `TablaPaginada`, `Badge`, `Urgencia`, `EstadoVacio` / `EstadoError`, chips de filtros, `Toast`.
- Nuevos: `PestaniasEstado` (tablist con subrayado naranja, mismo recurso que el menú), `FechaNecesaria` (fecha + "en N días" / "vencida"),
  `LeyendaEstados`. La tabla en celular se muestra como tarjetas (misma data, otro CSS).

### Accesibilidad
Pestañas con `role="tablist"`/`tab` y flechas; encabezados ordenables con `aria-sort`; filas enfocables con Enter;
estado con ícono + texto (no solo color); "Cargando" anunciado con `role="status"`.

**Solo de la maqueta:** la franja MAQUETA, los datos de ejemplo y el toast que reemplaza la navegación.
