# CatalogoMock · Catálogo (10)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/CatalogoMock.html`.
Estilo: pestañas de [BandejaMock](BandejaMock.md); **Proveedores** con el patrón lista + formulario de [SectoresMock](SectoresMock.md) / [UsuariosMock](UsuariosMock.md); **Categorías** como cola de revisión. Usuario de ejemplo: Martín Ruiz, Encargado.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/CatalogoMock.html`.
- **Con Claude Code:** "abrí el CatalogoMock" o `! start frontend/MockPaginas/aprobadas/CatalogoMock.html`.
- Atajos: `?editar=4` (Distribuidora Norte) · `?tab=categorias` · `?tab=categorias&unificar=20&destino=1` (ventana de unificar abierta).

### Qué probar · Proveedores

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial | Lista alfabética (razón social, CUIT o "Sin CUIT", hasta 2 categorías + "+N"); formulario **Nuevo proveedor**. En el menú, Catálogo muestra en amarillo cuántas categorías hay por revisar |
| Buscar por nombre o por CUIT (con o sin guiones) / filtrar por categoría / "Mostrar dados de baja" | Filtra; Papelera Andina aparece punteada como dada de baja |
| Buscar algo que no existe | **+ Crear "…"** abre el formulario con la razón social cargada |
| Escribir "Tecno Insumos" en Nuevo | Aviso "¿Es Tecno Insumos SA? Ya está cargado" con **Abrirlo** (ignora SA/SRL/Inc) |
| CUIT: tipear `30711112225` | Se arma solo `30-71111222-5`; con un CUIT ya cargado, error con el nombre del otro proveedor |
| Categorías: escribir "inf", flechas y Enter | Lista agrupada por Producto / Servicio con cuántos proveedores tiene cada una; se agregan como chips; Backspace borra la última |
| Guardar sin categorías | "Elegí al menos una categoría: es lo que usamos para sugerirlo." |
| Editar Tecno Insumos y darlo de baja | "Al guardar": deja de aparecer en sugeridos y tiene 2 cotizaciones vigentes que pueden fallar al comprar (`PROVEEDOR_INACTIVO`) |
| Sacarle una categoría | "Deja de sugerirse para X" |
| MAQUETA → **CUIT ya cargado** → guardar con CUIT | Aviso `CUIT_DUPLICADO · 409` |

### Qué probar · Categorías

| Qué hacer | Qué tenés que ver |
|---|---|
| Pestaña **Categorías** | **Por revisar** (4 tarjetas con borde amarillo: quién la escribió, en qué solicitud, cuándo, cuántos ítems) · **Confirmadas** (tabla con tipo, proveedores e ítems) · **Unificadas** (historial plegado: "Computación → Informática") |
| "Insumos informaticos" / "Libreria" | Recuadro violeta **"Se parece a Informática / Librería"** con **Unificar en …** como acción principal |
| "Fumigación" (sin parecidas) | **Confirmar así** pasa a ser la acción principal |
| **Confirmar así** | La tarjeta sale, la categoría aparece en Confirmadas con destello y baja el contador |
| **Confirmar con otro nombre** → "Librería" en la tarjeta de "Libreria" | Error `DUPLICADO · 409` "Ya existe Librería" con link **Unificar en Librería** |
| **Unificar en otra…** o "Unificar en…" de una confirmada | Ventana: **DEJA DE USARSE** (tachada) → **QUEDA**, lista de destinos del mismo tipo con las parecidas primero, y "Qué pasa": proveedores que se mueven (y cuántos ya la tenían), los ítems viejos no cambian, deja de aparecer en el autocompletado, **no se puede deshacer** |
| Confirmar la unificación | La origen pasa a Unificadas; sus proveedores quedan con la destino |
| MAQUETA → **Un solicitante escribe una categoría** | Toast y suma "Muebles de oficina" a Por revisar (se parece a Mobiliario) |
| Ancho < 860 px | Tarjetas en una columna; en proveedores, formulario a pantalla completa |

## Para implementarlo

**Ruta:** `/catalogo/proveedores` y `/catalogo/categorias` (rol `ENCARGADO`).

| Qué | Endpoint |
|---|---|
| Proveedores | `GET /api/v1/proveedores?q=&categoriaId=&activo=&page=` · `GET /{id}` · `POST` `{razonSocial, cuit?, telefono?, email?, contacto?, categoriaIds}` · `PATCH /{id}` (mismos campos + `activo`, todos opcionales) |
| Errores de proveedor | 409 `CUIT_DUPLICADO` · 400 `CATEGORIA_INVALIDA` (sin categorías o una que no existe) |
| Categorías por estado | `GET /api/v1/categorias?estado=NUEVA|CONOCIDA|UNIFICADA&page=` |
| Autocompletado | `GET /api/v1/categorias?tipo=&q=` (mín. 2 letras, máx. 10, sin unificadas) |
| Confirmar | `PATCH /api/v1/categorias/{id}` `{estado: "CONOCIDA", nombre?}` · 409 `DUPLICADO` si el nombre choca → ofrecer unificar |
| Unificar | `POST /api/v1/categorias/{id}/unificar` `{destinoId}` · 422 `UNIFICACION_INVALIDA` (misma, otro tipo, destino unificada) / `TRANSICION_INVALIDA` (ya unificada) |

**Reglas que la pantalla refleja:** solo se unifican categorías del **mismo tipo**; la destino no puede estar unificada (el front solo ofrece confirmadas); los proveedores pasan a la destino sin duplicarse; los **ítems viejos conservan su categoría**; una NUEVA se puede seguir usando mientras no se revise.

**"Se parece a":** lo calcula el front (sin tildes ni mayúsculas, hasta 2 letras de diferencia, o una raíz de 6 letras en común salvo palabras genéricas como "insumos"). Es una sugerencia, no bloquea.

**Componentes nuevos:** `SelectorMultiple` (combobox con chips, accesible con teclado), `TarjetaRevision`, `ModalUnificar` (origen → destino + consecuencias), `CampoCuit` con máscara.

**Falta en backend / a decidir:**
- `CategoriaResponse` no trae **cantidad de proveedores ni de ítems**, ni **quién la creó / en qué solicitud** (se muestran en tarjetas, tabla y ventana).
- No hay **renombrar una categoría ya confirmada** ni **crear una** desde el catálogo (solo nacen al pedir). Decidir si hace falta.
- `ProveedorResponse` no trae **cotizaciones vigentes** (para avisar al dar de baja).
- Detección de **proveedor repetido por nombre**: hoy solo el CUIT es único.
- Contador del menú (categorías por revisar): sale de `GET /categorias?estado=NUEVA&size=1` → `totalElements`.

**Solo de la maqueta:** la franja MAQUETA, los atajos por URL, los conteos y las simulaciones.
