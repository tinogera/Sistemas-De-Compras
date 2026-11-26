# SectoresMock · Sectores (12)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/SectoresMock.html`.
Estilo: tokens y componentes de [BandejaMock](BandejaMock.md) / [LayoutMock](LayoutMock.md). Usuario de ejemplo: Pablo Ferreyra, Admin. Menú de Admin: Usuarios · Sectores · Reglas · Reportes.

**Define el patrón "lista + formulario"** que reusan Usuarios (11), Catálogo (10) y Reglas (13).

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/SectoresMock.html`.
- **Con Claude Code:** "abrí el SectoresMock" o `! start frontend/MockPaginas/aprobadas/SectoresMock.html`.
- Para abrir directo editando un sector: `SectoresMock.html?editar=9` (Ventas).

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial | Lista alfabética a la izquierda (inicial, nombre, cantidad de usuarios, chip "APRUEBA EN N REGLAS"); a la derecha el formulario **Nuevo sector** siempre visible |
| Escribir "Legales" y Enter | "Disponible" en verde → **Crear sector** → aparece en su lugar alfabético con badge "Nuevo" y destello verde; el formulario queda limpio para cargar otro |
| Escribir "ventas", "VENTAS" o "Ventas " | Error en vivo: "Ya existe un sector llamado Ventas" (no distingue mayúsculas, tildes ni espacios) |
| Dejarlo vacío y Enter | "Escribí el nombre del sector." |
| Escribir más de 85 caracteres | El contador `n/100` se pone amarillo; pasado 100, error |
| Clic en una fila (o flechas ↑↓ + Enter) | La fila queda marcada en naranja y el formulario pasa a **EDITANDO** con el nombre seleccionado; muestra cuántos usuarios y reglas lo usan |
| Cambiar el nombre | Aparece "antes → después" y "Sin guardar"; al guardar, toast con cuántos usuarios lo ven con el nombre nuevo |
| Con cambios sin guardar, tocar otra fila / Nuevo / Cancelar / Esc | Pregunta "¿Descartar los cambios?" |
| Buscar "rec" | Filtra y resalta la coincidencia; "3 de 10" |
| Buscar algo que no existe ("Legales") | Vacío con **+ Crear "Legales"**, que abre el formulario ya completo |
| MAQUETA → **Otro Admin crea "Legales" antes** → Crear | Aviso amarillo `SECTOR_DUPLICADO · 409`, el sector aparece en la lista y el error queda en el campo |
| MAQUETA → **Falla al guardar** → guardar | Aviso rojo; lo escrito no se pierde |
| MAQUETA → Cargando / Sin sectores / Error | Esqueleto; "Todavía no hay sectores" con **+ Crear el primero**; error con Reintentar |
| Ancho < 1000 px | Lista sola; **+ Nuevo sector** o tocar una fila abre el formulario a pantalla completa con ✕ arriba y botones abajo |

## El patrón "lista + formulario"

| Parte | Regla |
|---|---|
| Disposición | Escritorio: lista `1fr` + formulario fijo de 400 px (sticky). < 1000 px: lista sola y formulario a pantalla completa que sube desde abajo. |
| Modo | El formulario siempre está: **NUEVO** por defecto, **EDITANDO** (etiqueta naranja + borde naranja suave) al elegir una fila. La fila elegida lleva barra naranja a la izquierda. |
| Validación | En vivo mientras se escribe, mensaje debajo del campo, contador de caracteres. Los mismos chequeos que el backend (obligatorio, largo, duplicado sin distinguir mayúsculas) para no esperar al 409. |
| Guardar | Enter guarda. Botón primario blanco (el naranja queda para acciones de flujo como Comprar). Mientras guarda: "Guardando…" y campo deshabilitado. Después: destello verde en la fila, toast, y foco de vuelta al campo. |
| Cambios sin guardar | Etiqueta "Sin guardar" + confirmación al cambiar de fila, crear otro, cancelar o Esc. |
| Errores | Del campo: debajo del campo. Del servidor o de red: aviso arriba del formulario, sin perder lo escrito. |
| Borrar | Solo si el backend lo permite; si no, se explica por qué no hay botón. |
| Impacto | Al editar, mostrar qué se ve afectado (acá: usuarios y reglas). |

Usuarios (11) usa el mismo patrón con más campos; si el formulario no entra en 400 px se pasa al panel lateral de [OrdenesMock](OrdenesMock.md).

## Para implementarlo

**Ruta:** `/admin/sectores` y `/admin/sectores/:id` (rol `ADMIN`).

| Qué | Endpoint |
|---|---|
| Lista | `GET /api/v1/admin/sectores` → `[{id, nombre}]` ordenada por nombre |
| Crear | `POST /api/v1/admin/sectores` `{nombre}` → 201 · duplicado: 409 `SECTOR_DUPLICADO` |
| Renombrar | `PATCH /api/v1/admin/sectores/{id}` `{nombre}` · duplicado: 409 `SECTOR_DUPLICADO` |

**Reglas del backend:** nombre obligatorio, máximo 100, se guarda con `trim`; duplicado sin distinguir mayúsculas; renombrar a la misma palabra con otras mayúsculas está permitido. No hay borrado ni baja.

**Componentes:** `ListaAbm` (buscador + filas seleccionables con teclado), `FormularioAbm` (cabecera con modo, cuerpo, pie con estado), `ConfirmarDescartar`, `CampoTexto` con contador.

**Falta en backend / a decidir:**
- `SectorResponse` no trae **cantidad de usuarios ni de reglas** que lo usan (se muestran en la fila y en el formulario).
- El front normaliza tildes al detectar duplicados ("Deposito" = "Depósito"); el backend solo ignora mayúsculas. Decidir si se agrega al backend o se saca del front.
- "Ver usuarios de X" necesita que Usuarios filtre por sector (`/admin/usuarios?sectorId=`).

**Solo de la maqueta:** la franja MAQUETA, `?editar=`, los conteos y las simulaciones.
