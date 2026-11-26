# OrdenesMock · Órdenes (09)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/OrdenesMock.html`.
Estilo: patrón de lista de [MisSolicitudesMock](MisSolicitudesMock.md) / [BandejaMock](BandejaMock.md) (resumen con números clicables + pestañas + filtros + tabla + paginación, tarjetas en celular). Usuario de ejemplo: Martín Ruiz, Encargado.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/OrdenesMock.html`.
- **Con Claude Code:** "abrí el OrdenesMock" o `! start frontend/MockPaginas/aprobadas/OrdenesMock.html`.
- Para abrir directo el detalle de una orden: `OrdenesMock.html?detalle=2289`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial | Resumen (Sin enviar · Enviadas 7 días · Comprado este mes en ARS y USD con envío), pestaña **Sin enviar** con 4 órdenes, más nuevas primero |
| Columna Emitida | Las que llevan más de 2 días sin enviar dicen "hace N días · sin enviar" en amarillo |
| Clic en "Sin enviar" / "Enviadas" del resumen | Cambia a esa pestaña |
| Pestaña **Enviadas** | Badge verde + quién la marcó y cuándo; sin botón "Marcar enviada" |
| Buscar `2287`, `OC-2287`, `#1047` o "silla" | Filtra por N°, solicitud o detalle; los contadores de las pestañas se actualizan |
| Proveedor / Moneda | Filtran; si no queda nada, vacío con "Quitar filtros" |
| Clic en una fila | Panel lateral: aviso si está demorada, **vista previa del PDF** (proveedor, ítem, cantidad, unitario, total, envío, total con envío), recorrido Compra → Orden → Envío, links al ítem y a la solicitud |
| OC-2288 en el panel | Nota de que el total se corrigió después de comprar (1.350 → 1.290 USD) |
| Ícono de PDF / "Descargar PDF" | Toast de descarga (`orden-2289.pdf`) |
| **Marcar enviada** | Modal que aclara que el sistema no le manda nada al proveedor, qué pasa después y que no se puede deshacer → fila pasa a Enviada con destello verde y toast |
| MAQUETA → **Otro Encargado la marca antes** y después marcar una | En el modal: aviso `ORDEN_YA_ENVIADA` "Lucía Pérez la marcó hace un momento" |
| MAQUETA → **Se registra una compra** | Fila nueva arriba con badge "Nueva" y toast naranja |
| MAQUETA → Cargando / Sin órdenes / Error | Esqueleto; "Todavía no hay órdenes" con link a la Bandeja; error con Reintentar |
| Marcar todas las sin enviar | Pestaña vacía "Todo enviado" |
| Ancho < 860 px | Tarjetas: N° + estado, ítem, proveedor, total + acciones; resumen en 2 columnas |

## Para implementarlo

**Ruta:** `/ordenes` y `/ordenes?detalle={id}` (rol `ENCARGADO`). El panel lateral se abre sobre la lista; en celular ocupa toda la pantalla.

| Qué | Endpoint |
|---|---|
| Lista | `GET /api/v1/ordenes?estado=GENERADA|ENVIADA&page=&size=` → `Pagina<OrdenResponse>` (ordenada por más nuevas) |
| Detalle | `GET /api/v1/ordenes/{id}` → `OrdenDetalleResponse` (agrega `enviadaPorNombre`, `proveedorId`, `cotizacionId`) |
| PDF | `GET /api/v1/ordenes/{id}/pdf` (descarga `orden-{numero}.pdf`) |
| Marcar enviada | `POST /api/v1/ordenes/{id}/marcar-enviada` → `OrdenResponse`; si ya estaba: 422 `ORDEN_YA_ENVIADA` (mostrar aviso, recargar la fila) |

**Textos:** `GENERADA` se muestra como **"Sin enviar"** (es lo que le importa al Encargado); `ENVIADA` como "Enviada". El número se muestra como `OC-{numero}`; en el PDF dice "N° {numero}".

**Reglas que la pantalla refleja:**
- Una orden por ítem; se crea sola con `CompraRegistrada` (no hay botón "Nueva orden").
- Si el Encargado corrige el precio o el envío del ítem comprado, la orden se actualiza aunque ya esté enviada.
- Marcar enviada no se deshace; con todas las órdenes de una solicitud enviadas, la solicitud se cierra.
- "Demorada" = `GENERADA` hace más de 48 h (criterio del front, a confirmar).

**Componentes:** `PanelLateral` (cajón derecho, nuevo, reutilizable en Catálogo y Usuarios), `VistaPreviaOrden`, `LineaDeTiempo`, y los de lista ya aprobados.

**Falta en backend / a decidir:**
- Filtros por **proveedor, moneda y búsqueda** (N°, detalle, solicitud): hoy `GET /ordenes` solo filtra por `estado`.
- **Resumen** (sin enviar, demoradas, enviadas 7 días, comprado del mes por moneda): no hay endpoint; o se suma uno o se saca del reporte de gasto.
- `OrdenResponse` no trae **sector ni solicitante** (se muestran en la fila y en el panel), ni si se compró **con cotización o a mano** (sí está `cotizacionId` en el detalle).
- Historial de correcciones de monto de la orden: hoy solo queda en el historial del ítem.

**Nota para 08 (aprobada):** el texto del resultado de compra dice "Se sumó a la orden OC-2291 (Creada)"; con este criterio debería decir "(Sin enviar)". Cambio propuesto, pendiente de tu ok.

**Solo de la maqueta:** la franja MAQUETA, `?detalle=`, los datos y la simulación de carrera.
