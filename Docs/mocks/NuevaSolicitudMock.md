# NuevaSolicitudMock · Nueva solicitud (03)

Estado: **APROBADA** (2026-11-25). Mock: `frontend/MockPaginas/aprobadas/NuevaSolicitudMock.html`.
Estilo: tokens y componentes de [LayoutMock](LayoutMock.md). Es el formulario más importante: define cómo se ven
los inputs, el selector de moneda y los adjuntos en el resto de la app.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/NuevaSolicitudMock.html`.
- **Con Claude Code:** "abrí el NuevaSolicitudMock" o `! start frontend/MockPaginas/aprobadas/NuevaSolicitudMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Franja MAQUETA → **Con 3 ítems** | Formulario completo: 2 productos y 1 servicio, ARS y USD, una categoría nueva, adjuntos y uno inválido (`.docx`) |
| Elegir urgencia y fecha | El resumen de la derecha se actualiza al instante |
| Escribir precios en ARS y en USD | Totales separados por moneda en el resumen (nunca se suman) |
| **+ Agregar otro ítem** | Aparece un ítem nuevo, con foco en el detalle |
| **Quitar** | Se quita el ítem (deshabilitado si es el único) |
| Producto / Servicio | Cambia el ejemplo del detalle |
| Categoría: escribir "infor" | Lista con coincidencias resaltadas; flechas ↑↓ + Enter para elegir, Esc para cerrar |
| Categoría: escribir algo que no existe (≥ 3 letras) | Opción "+ Crear categoría «…»" y badge violeta **Nueva** |
| Arrastrar o elegir archivos | PDF/JPG/PNG hasta 10 MB se agregan; otro formato o más pesado → error en rojo con "Descartar" |
| **Errores de validación** (o Enviar con datos mal) | Aviso arriba con la lista de errores (cada uno es un link al campo), campos en rojo con su mensaje |
| **Enviando** | Modal con pasos: creando la solicitud → subiendo adjuntos (n de m) → listo |
| **Error del servidor** | Aviso rojo arriba, los datos siguen cargados, botón Reintentar |
| **Enviada** / **Enviada con adjunto fallido** | Pantalla de éxito con número, resumen y estado Pendiente; si falló un adjunto, aviso para reintentar |
| **Cancelar** con datos cargados | Modal "¿Descartar la solicitud?" |
| Ancho < 960 px | El resumen pasa a una barra fija abajo con totales y "Enviar" |

## Para implementarlo

**Ruta:** `/solicitudes/nueva` (cualquier usuario logueado).

### Endpoints

| Paso | Endpoint | Body |
|---|---|---|
| Categorías para el autocompletado | `GET /api/v1/categorias` | — |
| 1. Crear la solicitud | `POST /api/v1/solicitudes` | `{ urgencia, fechaNecesaria, observaciones, items: [...] }` |
| 2. Subir adjuntos (por ítem, después de crear) | `POST /api/v1/items/{itemId}/adjuntos` | `multipart/form-data` |

Cada ítem (`ItemRequest`): `{ tipo: PRODUCTO|SERVICIO, categoriaId | categoriaNueva, cantidad, detalle, precioEstimado, monedaEstimada: ARS|USD }`.

> Los adjuntos no viajan con la solicitud: primero se crea, la respuesta trae los `itemId`, y recién ahí se sube cada archivo.
> Si un adjunto falla, la solicitud **ya existe**: mostrar éxito + aviso de adjunto fallido (no "error").

### Validaciones (las mismas que el backend, para avisar antes de enviar)

| Campo | Regla | Mensaje |
|---|---|---|
| Urgencia | obligatoria (ALTA/MEDIA/BAJA) | La urgencia es obligatoria. |
| Fecha necesaria | obligatoria, hoy o después | La fecha necesaria no puede ser anterior a hoy. |
| Ítems | al menos 1 | Tenés que cargar al menos un ítem. |
| Cantidad | > 0, hasta 10 enteros y 2 decimales | La cantidad debe ser mayor a 0. |
| Categoría | una existente **o** una nueva (no las dos), nueva ≤ 120 caracteres | Indicá una categoría. |
| Precio estimado | > 0, hasta 14 enteros y 2 decimales | El precio estimado debe ser mayor a 0. |
| Moneda | obligatoria | — |
| Adjuntos | PDF/JPG/PNG, ≤ 10 MB | Formato no permitido / Pesa X MB. |

**Decisión a confirmar:** el backend acepta `detalle` vacío, pero el mock lo pide como obligatorio (un ítem sin detalle
no le sirve a Compras). Si se mantiene, conviene sumar `@NotBlank` en `ItemRequest`.

**Precio estimado = total del ítem** (no unitario): es el monto con el que se elige la regla de aprobación.

**Números:** se escriben en formato argentino (`4.200.000,50`); convertir a `4200000.50` antes de enviar.

### Componentes

- Reutilizados de LayoutMock: `BarraSuperior`, `CampoMonto`, `Adjuntos`, `Aviso`, `ModalConfirmacion`, `Toast`, botón `acento`.
- Nuevos: `SelectorUrgencia` (tarjetas con barritas), `ItemFormulario`, `AutocompletarCategoria` (combobox con "Crear"),
  `ResumenSolicitud` (lateral en escritorio, barra fija en celular), `ProgresoEnvio`.

### Accesibilidad
Urgencia como `fieldset` de radios; autocompletado con `role="combobox"` + `listbox` y navegación con flechas;
errores asociados con `aria-describedby`; resumen de errores con links a cada campo y foco al aparecer.

**Solo de la maqueta:** la franja MAQUETA, los datos de ejemplo y los toasts que reemplazan la navegación.
