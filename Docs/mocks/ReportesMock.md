# ReportesMock · Reportes (14)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/ReportesMock.html`.
Estilo: tokens de [BandejaMock](BandejaMock.md); segmentos, tarjetas y vacíos del resto de las pantallas aprobadas. Para **Encargado y Admin** (la franja MAQUETA cambia el menú).

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/ReportesMock.html`.
- **Con Claude Code:** "abrí el ReportesMock" o `! start frontend/MockPaginas/aprobadas/ReportesMock.html`.
- Atajos: `?agrupar=proveedor&moneda=USD` · `?periodo=anio` (`mes`, `pasado`, `90`, `anio`).

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial | Filtro de **período** fijo arriba (Este mes · Mes pasado · Últimos 90 días · Este año + desde/hasta), con los días que abarca. Vale para los dos reportes |
| Desde posterior a hasta | Error en el filtro y los reportes piden corregir el período (mismo mensaje que el backend) |
| **Gasto**: 4 números | Comprado con envío (y cuánto hubo en la otra moneda, aparte), envíos y su %, órdenes, promedio por orden |
| Gráfico | Barras horizontales ordenadas de mayor a menor; cada barra = **compra (azul) + envío (verde agua)**, con el total al final y escala abajo |
| Pasar el mouse o tabular por una barra | Tooltip: compra, envío, total, órdenes y % del total |
| Sector / Categoría / Proveedor | Cambia el agrupamiento. Por proveedor hay más de 8: se muestran las 8 mayores y el resto en **"Otros (N)"** |
| ARS / USD | Cambia de moneda; **nunca se suman** |
| **Ver como tabla** | La misma información en tabla con totales; **Descargar CSV** (toast en la maqueta) |
| **Tiempo de resolución** | Número grande con el promedio general en horas (≈ días) y cuántos ítems; barras por urgencia en horas |
| MAQUETA → **Período sin compras** | "No hubo compras en ARS" con atajos a la otra moneda o a "Este año"; tiempos sin ítems resueltos |
| MAQUETA → Cargando / Error | Esqueletos; error con Reintentar |
| MAQUETA → Admin | Menú de Admin con Reportes marcado |
| Ancho < 860 px | Todo en una columna; nombres largos se recortan con "…" (completos en el tooltip) |

## Para implementarlo

**Ruta:** `/reportes?desde=&hasta=&agrupar=&moneda=` (roles `ENCARGADO` y `ADMIN`). Los filtros van en la URL para poder compartir el link.

| Qué | Endpoint |
|---|---|
| Gasto | `GET /api/v1/reportes/gasto?agrupar=sector|categoria|proveedor&desde=AAAA-MM-DD&hasta=AAAA-MM-DD` → `[{clave, nombre, moneda, total, envio, cantidadOrdenes}]` (una fila por clave **y moneda**) |
| Tiempos | `GET /api/v1/reportes/tiempos-resolucion?desde=&hasta=` → `{cantidadItems, promedioHoras, porUrgencia: {ALTA|MEDIA|BAJA: {cantidad, promedioHoras}}}` |

**Cómo se arma en el front:** separar las filas de gasto por moneda; ordenar por `total + envio`; top 8 + "Otros"; los números de arriba son sumas de esas filas. Un solo llamado por agrupamiento trae las dos monedas.

**Reglas que la pantalla refleja:** cuenta lo **comprado con orden** (no lo aprobado ni lo estimado); el día es el de **emisión de la orden en hora argentina**; si se corrige el precio o el envío de una compra, el reporte usa el valor corregido; las categorías unificadas suman en la que quedó; si un nombre ya no existe, aparece "Desconocido (id)"; tiempos = de creado a comprado o rechazado, promedio ponderado por cantidad, `null` si no hay ítems.

**Gráficos:** un solo eje, barras de 20 px con punta redondeada, escala con 0 a la izquierda, sin color por ranking. Colores validados con la guía de dataviz sobre el fondo `#24221e`: compra `#338ced`, envío `#00a888` (pasan contraste ≥ 3:1 y separación para daltonismo). Leyenda siempre visible; tabla como alternativa accesible. Los textos nunca van en el color de la serie.

**Componentes nuevos:** `FiltroPeriodo` (presets + rango), `BarrasHorizontales` (apiladas, top N + Otros, tooltip con teclado, alternar a tabla), `StatTile`, `NumeroHeroe`.

**Falta en backend / a decidir:**
- **Exportar CSV**: no hay endpoint; se puede armar en el front con la misma respuesta.
- Tiempos no separa **espera de aprobación** del trabajo de Compras, ni da mediana (un ítem trabado sube mucho el promedio).
- No hay **evolución en el tiempo** (gasto por mes) ni comparación con el período anterior. Si se suma, un gráfico de líneas por moneda.

**Solo de la maqueta:** la franja MAQUETA, los atajos por URL y los datos (escalados según la cantidad de días del período).
