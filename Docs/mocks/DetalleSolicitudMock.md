# DetalleSolicitudMock · Detalle de solicitud (04)

Estado: **APROBADA** (2026-11-25). Mock: `frontend/MockPaginas/aprobadas/DetalleSolicitudMock.html`.
Estilo: badges, pasos de aprobación, historial, adjuntos, modales y toasts de [LayoutMock](LayoutMock.md).
Acá aparecen por primera vez el **historial** y la **cadena de aprobación en modo lectura**.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/DetalleSolicitudMock.html`.
- **Con Claude Code:** "abrí el DetalleSolicitudMock" o `! start frontend/MockPaginas/aprobadas/DetalleSolicitudMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Abrir / cerrar cada ítem | Tarjeta desplegable: resultado, datos, adjuntos y cadena de aprobación |
| Ítem 2 (Monitor) | En gestión; cadena En curso: paso 1 aprobado, paso 2 **en paralelo** (Gerencia General "Le toca" + Finanzas aprobado) |
| Ítem 4 (Auriculares) | Rechazado con motivo; paso rechazado y paso siguiente **Omitido** |
| Ítem 3 (Licencias, Pendiente) | Sin cadena todavía ("se arma cuando Compras fije el precio"); botones **Editar** y **Quitar ítem** |
| Editar → Guardar | Modal con los campos del ítem; al guardar se actualiza y suma una línea al historial |
| Quitar ítem | Confirmación; el ítem queda Cancelado (tachado) |
| Cancelar solicitud | Confirmación que lista qué se cancela y qué no cambia |
| Precio total vs. estimado | En amarillo si es mayor ("más que lo estimado"), en verde si es menor |
| Resumen | Barra de progreso por estado de ítems y montos estimado / total por moneda |
| Historial → filtros Todo / Solicitud / Ítem N | Muestra solo los movimientos de lo elegido; los cambios de valores se ven tachado → nuevo |
| MAQUETA → **Cambio en vivo** | El precio total del ítem 2 cambia solo, la tarjeta se resalta en naranja y aparece un toast |
| MAQUETA → **Conflicto al editar** | Modal de edición con aviso "El ítem cambió mientras lo editabas" y botón Recargar |
| MAQUETA → Lista / Cancelada | Sin acciones; badges y cadenas finales |
| MAQUETA → Cargando / No encontrada / Sin permiso | Esqueleto, 404 y 403 |
| Ancho < 1100 px | Resumen e historial pasan abajo de los ítems; en celular las tarjetas se apilan |

## Para implementarlo

**Ruta:** `/solicitudes/:id`.

### Endpoints

| Qué | Endpoint |
|---|---|
| Detalle | `GET /api/v1/solicitudes/{id}` → `SolicitudDetalleResponse` (incluye `version` de la solicitud y de cada ítem) |
| Cadena de cada ítem | `GET /api/v1/aprobaciones/items/{itemId}` → `Detalle` con `reglaNombre`, `estado` y `pasos[]` (el solicitante puede verla) |
| Adjuntos de cada ítem | `GET /api/v1/items/{itemId}/adjuntos`; ver archivo: `GET /api/v1/adjuntos-item/{id}` |
| Historial | `GET /api/v1/historial?entidad=SOLICITUD&id={id}&incluirItems=true` → `HistorialResponse[]` |
| Editar ítem (solo Pendiente) | `PATCH /api/v1/solicitudes/{id}/items/{itemId}` con `EditarItemRequest` (+ `version`) |
| Quitar ítem | `DELETE /api/v1/solicitudes/{id}/items/{itemId}?version=` |
| Cancelar solicitud | `POST /api/v1/solicitudes/{id}/cancelar` con `{ version }` |

Pedir la cadena solo de los ítems que tienen `estadoAprobacion` (o al abrir el ítem).
Si un ítem no tiene cadena: "Todavía no hay cadena…" (sin precio total) o "Este monto no necesita aprobación" (regla sin pasos).

### Cómo se muestra cada cosa

| Dato | Componente |
|---|---|
| `EstadoSolicitud` | Badge: En revisión / Lista / Cerrada / Cancelada (igual que Mis solicitudes) |
| `EstadoItem` | Badge: Pendiente / En gestión (`EN_COTIZACION`) / Comprado / Rechazado / Cancelado |
| Cadena (`EstadoCadena`) | Badge: En curso / Aprobada / Rechazada / Cancelada |
| `EstadoPaso` + `activo` | `PENDIENTE` + activo → "Le toca" (naranja) · `PENDIENTE` no activo → "Aún no le toca" · Aprobado · Rechazado (+ motivo) · Omitido · Cancelado |
| Pasos con el mismo `orden` | Agrupados lado a lado con la etiqueta "EN PARALELO" |
| `proveedorComprado` / `motivoRechazo` + `rechazadoPor` | Caja de resultado arriba de los datos del ítem |
| `HistorialResponse` | Línea de historial; `estadoAnterior` → `estadoNuevo` tachado → nuevo; `comentario` como cita |

### Tiempo real
Cuando llegue la señal SSE (T24.1) con el id de la solicitud o de un ítem: volver a pedir el detalle, resaltar la tarjeta
que cambió (borde naranja que se desvanece) y mostrar un toast. El indicador "EN VIVO" del historial muestra que la conexión está activa.

### Errores de negocio
- `CONFLICTO_VERSION` (409) al editar / quitar / cancelar → aviso dentro del modal + "Recargar".
- Ítem ya tomado por Compras → ya no es editable; se oculta Editar/Quitar después de recargar.
- 404 / 403 → pantallas de estado con "Volver a Mis solicitudes".

### Componentes
- Reutilizados: `Badge`, `Urgencia`, `PasosAprobacion`, `Historial`, `Adjuntos` (solo lectura), `ModalConfirmacion`, `Aviso`, `Toast`.
- Nuevos: `ItemDetalle` (tarjeta desplegable), `ResultadoItem`, `ResumenSolicitud` (progreso + montos por moneda), `FiltroHistorial`, indicador `EnVivo`.

**Solo de la maqueta:** la franja MAQUETA, los botones de eventos simulados y los datos de ejemplo.
