# ReglasAprobacionMock · Reglas de aprobación (13)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/ReglasAprobacionMock.html`.
Estilo: tokens de [BandejaMock](BandejaMock.md); componente **Pasos de aprobación · variante editable** (la tercera variante prevista en [DetalleAprobacionMock](DetalleAprobacionMock.md)); interruptor, "Sin guardar" y confirmación de descarte del patrón de [SectoresMock](SectoresMock.md). Usuario de ejemplo: Pablo Ferreyra, Admin.

El formulario no entra en 400 px, así que el editor es **una vista propia** (lista ↔ editor), con el Probador siempre a la derecha.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/ReglasAprobacionMock.html`.
- **Con Claude Code:** "abrí el ReglasAprobacionMock" o `! start frontend/MockPaginas/aprobadas/ReglasAprobacionMock.html`.
- Atajos: `?editar=2` abre el editor · `?nueva=1` regla nueva · `?monto=800&moneda=USD` carga el Probador.

### Qué probar · Lista

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial | Reglas numeradas en orden de evaluación, cada una con su condición (`> 500.000 ARS`) y su cadena en chips (ROL violeta, SECTOR azul, `+` = en paralelo, `→` = después). La **por defecto** va última, punteada, con candado y "∞" |
| Probador con 600.000 ARS | La regla 2 queda **resaltada en naranja** ("LA ELIGE EL PROBADOR"); en el Probador: la cadena que se armaría y "Antes se probaron: 1. Más de 2.000.000 ARS · no: 600.000 no es más de 2.000.000" |
| Montos de ejemplo (30.000 ARS, 800 USD, 1.500 USD…) | Cambia la elegida en vivo. 30.000 → "Compras chicas" → **Aprobación automática**; 800 USD → cae en la por defecto ("es en ARS y el ítem en USD") |
| **Arrastrar** una regla (o ▲▼) | Línea naranja donde va a caer; al soltar: "Orden guardado · ahora se prueba en el lugar N" con **Deshacer** |
| Interruptor de una regla | Activa/desactiva al instante. "Mantenimiento grande" no se deja activar: el sector no tiene usuarios (`SIN_APROBADORES`) |
| "Más de 500.000 (copia)" | Aviso amarillo: **Nunca se aplica**, otra regla con las mismas condiciones va antes |
| MAQUETA → Cargando / Error | Esqueleto; error con Reintentar |

### Qué probar · Editor (clic en una regla o **+ Nueva regla**)

| Qué hacer | Qué tenés que ver |
|---|---|
| Bloques **1 Nombre · 2 Cuándo se aplica · 3 Quién aprueba** | Barra de guardar fija abajo; "Se prueba en el lugar N de M" |
| Condición de monto | Operador (más de / desde / menos de / hasta), monto y moneda; frase en castellano: "Se aplica a ítems de más de 500.000 ARS. Un ítem en USD no la cumple: no hay conversión." Mezclar ARS y USD avisa que nunca se cumple |
| Otras condiciones | Categoría, proveedor y centro de costo aparecen como **PRONTO** (el backend solo acepta monto) |
| Quitar todas las condiciones | Aviso: sin condiciones taparía a las de abajo |
| **Pasos**: "+ En paralelo…" / "+ Agregar paso…" | Agrega un aprobador al mismo paso o un paso nuevo; los selects muestran cuántos usuarios tiene cada rol o sector |
| **Arrastrar un aprobador** a otro paso, al hueco "DESPUÉS" entre pasos o a la zona punteada | Se mueve; si un paso queda vacío, se quita y los números se reacomodan (1, 2, 3 sin saltos) |
| Teclado: foco en un aprobador + **Alt ↑/↓** | Lo mueve al paso anterior/siguiente (o crea uno nuevo en los extremos); Supr lo quita |
| Arrastrar un paso o ▲▼ / × | Reordena o quita pasos |
| Agregar el mismo aprobador dos veces | Paso en rojo: `APROBADOR_REPETIDO` |
| Agregar "Mantenimiento" con la regla activa | Paso en rojo: `SIN_APROBADORES` (con la regla desactivada se permite) |
| Quitar todos los pasos | "Sin pasos: aprobación automática" |
| Cambiar algo | El Probador marca **CON TUS CAMBIOS** y evalúa con el borrador |
| Regla por defecto | Sin condiciones, siempre activa; no deja quedarse sin pasos |
| Guardar con errores | Toast "Revisá lo marcado en rojo" y scroll al primero |
| Volver con cambios | "¿Descartar los cambios?" |
| MAQUETA → **Otro Admin la editó (409)** y guardar | Aviso `CONFLICTO_VERSION` con **Recargar la regla** |
| Ancho < 860 px | Todo en una columna; ▲▼ abajo de cada paso; el Probador queda debajo |

## Para implementarlo

**Ruta:** `/admin/reglas`, `/admin/reglas/nueva`, `/admin/reglas/:id` (rol `ADMIN`).

| Qué | Endpoint |
|---|---|
| Lista | `GET /api/v1/admin/reglas` → `[ReglaResponse]` por prioridad (incluye `porDefecto`, `version` y `sinAprobadores` por paso) |
| Detalle | `GET /api/v1/admin/reglas/{id}` |
| Crear | `POST /api/v1/admin/reglas` `{nombre, prioridad, activa, condiciones[], pasos[]}` |
| Guardar | `PUT /api/v1/admin/reglas/{id}` (reemplaza todo, con `version`) |
| Activar / desactivar | `POST /api/v1/admin/reglas/{id}/activar` · `/desactivar` |
| Probador | `POST /api/v1/admin/reglas/simular` `{monto, moneda}` → `{regla, automatica, pasos}` |

**Del editor al request:** cada grupo visual es un `orden` (1, 2, 3…); cada aprobador del grupo es un `PasoRequest` con ese orden y `tipoAprobador` `ROL` + `rol` o `SECTOR` + `sectorId`.

**Errores del backend que la pantalla ya previene o muestra:** `NOMBRE_DUPLICADO` · `CONDICION_NO_SOPORTADA` · `CONDICION_INVALIDA` · `ORDEN_INVALIDO` · `PASO_INVALIDO` · `ROL_NO_APROBADOR` · `APROBADOR_REPETIDO` · `SIN_APROBADORES` · `REGLA_POR_DEFECTO_NO_DESACTIVABLE` / `_SIN_CONDICIONES` / `_SIN_PASOS` · `CONFLICTO_VERSION`.

**Reglas que la pantalla refleja:** gana la primera regla activa que se cumple por prioridad; todas las condiciones tienen que cumplirse; la moneda tiene que coincidir; regla sin pasos = aprobación automática; editar una regla **no cambia las cadenas en curso** (solo queda en el historial).

**Componentes nuevos:** `ListaOrdenable` (arrastrar + ▲▼ + Deshacer), `Probador` (con o sin borrador), `EditorCondiciones`, `PasosAprobacion` editable (arrastrar aprobadores entre pasos, Alt+flechas).

**Falta en backend / a decidir:**
- **Reordenar** hoy son N `PUT` (uno por regla con su `prioridad` y `version`); conviene un `PUT /admin/reglas/orden` `[ids]` atómico.
- El **Probador con borrador** lo calcula el front (mismo algoritmo); `simular` solo prueba lo guardado. Opción: aceptar una regla borrador en `simular`.
- El Probador no devuelve **por qué se descartaron** las reglas de arriba (lo calcula el front).
- **Admin como aprobador:** el backend lo permite (solo prohíbe Solicitante), pero en Usuarios (aprobada) dijimos que Admin no aprueba. El editor ofrece solo Supervisor y Encargado. Decidir.
- **Cantidad de usuarios por rol/sector** para los selects y el aviso: hoy solo viene `sinAprobadores` por paso guardado.
- El aviso **"Nunca se aplica"** (misma condición más arriba) es del front.

**Solo de la maqueta:** la franja MAQUETA, los atajos por URL, los conteos y la simulación del 409.
