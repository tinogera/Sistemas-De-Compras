# GestionItemMock · Gestión de un ítem (08)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/GestionItemMock.html`.
Estilo: barra superior y badges de [BandejaMock](BandejaMock.md); adjuntos, datos y **Pasos de aprobación (variante compacta)** de [DetalleSolicitudMock](DetalleSolicitudMock.md). Usuario de ejemplo: Martín Ruiz, Encargado.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/GestionItemMock.html`.
- **Con Claude Code:** "abrí el GestionItemMock" o `! start frontend/MockPaginas/aprobadas/GestionItemMock.html`.
- Para abrir directo en un estado: `GestionItemMock.html?modo=aprobando` (`tomado`, `cotizando`, `aprobando`, `aprobado`, `comprado`, `rechazado`, `ajeno`).

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial (Cotizando) | Recorrido arriba **Cotizar → Fijar precio → Aprobación → Comprar** con el paso actual en naranja; 4 cotizaciones agrupadas por moneda y ordenadas por total (las vencidas al final) |
| Mirar cada cotización | Total grande + unitario × cantidad; barra comparativa con la marca blanca del **precio estimado**; "% sobre/bajo el estimado" (solo misma moneda); chips MÁS BARATA, ENTREGA MÁS RÁPIDA, VENCE EN N DÍAS, VENCIDA; ícono del presupuesto adjunto o clip punteado para adjuntarlo |
| Destildar "Mostrar vencidas" | Desaparece la de Grupo Datatec |
| **+ Cargar cotización** (o "Cargar" en un proveedor sugerido) | Modal: proveedor (los de la categoría arriba), moneda + precio unitario con **total en vivo** (× cantidad), válida hasta (no puede ser pasada), entrega, observaciones, presupuesto. Al guardar aparece la fila resaltada |
| "Dar de alta uno nuevo" dentro del modal | Alta rápida de proveedor (razón social, CUIT opcional con formato, email); queda elegido en el select |
| **Elegir** una cotización | Queda ELEGIDA (borde naranja), se fija el total y en el lateral **se arma la cadena** según el monto (ARS > 500.000 → Supervisor y luego Gerencia General + Finanzas en paralelo; si no, regla por defecto) |
| Elegir la de USD | La cadena cambia a la regla por defecto (1 paso) |
| **Fijar a mano** en el lateral | Precio + moneda sin cotización; al comprar pide el proveedor |
| Cambiar el precio con algún paso ya aprobado (estado "Precio fijado · en aprobación") | Confirmación: "La aprobación vuelve a empezar" |
| MAQUETA → **Aprueba el paso activo** (dos o tres veces) | Los pasos se van tildando; al terminar, el botón **Registrar la compra** se desbloquea y el recorrido pasa a Comprar |
| **Registrar la compra** | Modal con resumen (ítem, total, proveedor, validez, quién aprobó) → Comprado, las demás cotizaciones DESCARTADAS, orden OC-2291 |
| En Comprado: "Corregir precio o cargar envío" | Form con total final y envío; toast con valor anterior → nuevo |
| **Rechazar el ítem…** | Modal con motivos rápidos y motivo obligatorio → Rechazado con el motivo en el lateral |
| Estado **De otro Encargado** | Aviso azul con "Pasármelo"; todo en solo lectura |
| MAQUETA → **Alguien tocó el ítem (409)** | Aviso amarillo `CONFLICTO_VERSION` con "Recargar el ítem"; acciones bloqueadas |
| Ancho < 1100 px / < 860 px | El lateral baja debajo; las cotizaciones pasan a tarjetas y aparece una **barra fija abajo** con el total y la acción del momento |

## Para implementarlo

**Ruta:** `/bandeja/:itemId` (rol `ENCARGADO`). Se llega desde la Bandeja (fila o "Gestionar →").

| Qué | Endpoint |
|---|---|
| Ítem + cotizaciones | `GET /api/v1/bandeja/{itemId}` → `GestionItemDetalleResponse` |
| Proveedores sugeridos | `GET /api/v1/bandeja/{itemId}/proveedores-sugeridos` → `[{id, razonSocial}]` |
| Cargar cotización | `POST /api/v1/bandeja/{itemId}/cotizaciones` `{proveedorId, moneda, precioUnitario, fechaValidez, entregaEstimada?, observaciones?}` |
| Presupuesto de la cotización | `POST /api/v1/cotizaciones/{id}/adjuntos` (multipart `archivo`), se sube **después** de crearla; ver: `GET /api/v1/adjuntos-cotizacion/{id}` |
| Alta rápida de proveedor | `POST /api/v1/proveedores` `{razonSocial, cuit?, email?, categoriaIds: [categoriaDelItem]}` |
| Elegir cotización | `POST /api/v1/bandeja/{itemId}/cotizaciones/{cotizacionId}/elegir` `{version}` |
| Fijar a mano / quitar | `PUT /api/v1/bandeja/{itemId}/precio-total` `{precioTotal, moneda, version}` · `DELETE …/precio-total?version=` |
| Cadena de aprobación | `GET /api/v1/aprobaciones/items/{itemId}` (la misma del Detalle de aprobación, variante compacta) |
| Comprar | `POST /api/v1/bandeja/{itemId}/comprar` `{proveedorId?, version}` (proveedor solo si el precio se fijó a mano) |
| Corregir compra | `PATCH /api/v1/bandeja/{itemId}/compra` `{precioTotal?, montoEnvio?, version}` |
| Rechazar | `POST /api/v1/bandeja/{itemId}/rechazar` `{motivo, version}` |
| Pasármelo | `POST /api/v1/bandeja/{itemId}/reasignar` |

**Reglas que la pantalla refleja (vienen del backend):**
- Total de la cotización = unitario × cantidad, lo calcula el backend; el front solo lo muestra en vivo.
- Solo el Encargado asignado y con el ítem `EN_COTIZACION` puede cotizar, elegir, fijar precio o comprar (403 / `GESTION_ESTADO_INVALIDO`). Un ítem sin cotizar lo puede rechazar cualquier Encargado.
- No se elige ni se compra con cotización vencida (`COTIZACION_VENCIDA`). Se controla de nuevo al comprar.
- Comprar exige aprobación completa (`APROBACION_PENDIENTE`) y precio (`FALTA_PRECIO_TOTAL`). El botón queda bloqueado con el motivo escrito abajo.
- Cambiar o quitar el precio re-evalúa la cadena (`PRECIO_CAMBIO`): si había pasos aprobados, el front confirma antes.
- Todas las escrituras mandan `version`; ante 409 mostrar el aviso de conflicto y recargar.
- Comparación entre monedas: **no hay tipo de cambio en el backend**, por eso se agrupa por moneda y el "% vs estimado" solo aparece en la misma moneda.

**Componentes:** `Recorrido` (4 pasos, nuevo, reutilizable en Órdenes), `ComparadorCotizaciones` (fila con barra + marca de estimado + chips), `ModalCotizacion` con `AltaRapidaProveedor`, `PasosAprobacion` compacto, `PreciosLadoALado`, `BarraAccionMovil`.

**Falta en backend / a decidir:**
- `GestionItemDetalleResponse` no trae solicitante, sector, observaciones ni adjuntos del ítem: hoy habría que pedir también el detalle de la solicitud.
- `CotizacionResponse` no indica si tiene adjunto: el ícono del presupuesto necesita `cantidadAdjuntos` o un `GET` por cotización.
- El número de orden (`OC-…`) al comprar no vuelve en la respuesta de `comprar`.
- Motivos rápidos de rechazo: son texto del front (se guardan como motivo normal).

**Solo de la maqueta:** la franja MAQUETA, `?modo=`, el motor de reglas simulado y los nombres de aprobadores.
