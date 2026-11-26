# LayoutMock · Componentes comunes + layout (00)

Estado: **APROBADA** (2026-11-25). Mock: `frontend/MockPaginas/aprobadas/LayoutMock.html`.
Es la referencia visual de todas las demás pantallas: el resto copia sus tokens y componentes.

## Cómo verlo

Ver la guía general [README.md](README.md) (sin y con Claude Code). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/LayoutMock.html`.
- **Con Claude Code:** pedile "abrí el LayoutMock" o escribí `! start frontend/MockPaginas/aprobadas/LayoutMock.html`.

Necesita internet solo para las fuentes de Google Fonts. Sin conexión se ve con la fuente del sistema.

### Qué probar

| Dónde | Qué hacer | Qué tenés que ver |
|---|---|---|
| Franja de arriba "MAQUETA · ver como" | Cambiar el rol | El menú de la barra cambia según el rol; Supervisor y Gerencia General muestran el contador naranja en Aprobaciones; a la derecha dice que después del login todos van a **Inicio** (pantalla 16) |
| Ventana < 1080 px de ancho (o F12 → Ctrl+Shift+M) | Achicar | El menú pasa al botón ≡ |
| 04 · Tabla | Escribir en Buscar, elegir Estado/Urgencia/Fecha | Filtra en vivo, aparecen chips de filtros activos (se quitan con ×), paginación de 6 filas |
| 04 · Tabla, botones arriba a la derecha | Cargando / Vacío / Sin resultados / Error | Esqueleto animado, estado vacío con "Nueva solicitud", sin resultados con "Limpiar filtros", error con "Reintentar" |
| 04 · Tabla | Clic en una fila | Toast "Abrir detalle" (en la app real navega al detalle) |
| 06 · Toasts | Éxito / Error / Info | Toast abajo a la derecha; éxito e info se van a los 5 s, error queda hasta cerrarlo |
| 06 · Modales | Confirmación | Modal de cancelar solicitud; "Sí, cancelar" muestra toast de éxito |
| 06 · Modales | Rechazo con motivo → Rechazar sin escribir | Error "Escribí el motivo…"; con motivo cierra y muestra toast |
| 02 · Precio estimado | Clic en ARS / USD | Cambia la moneda seleccionada |
| Teclado | Tab por la página | Foco visible celeste en todo lo clickeable; Esc cierra modales |

## Para implementarlo (React + Vite)

### Tokens → CSS global
Copiar el bloque `:root { … }` del `<style>` a `src/styles/tokens.css`. Son los únicos colores permitidos:

- Base: `--fondo #1b1916`, `--superficie #24221e`, `--borde #3a362f`, `--texto #fbfaf8`, `--texto-2 #c9c3b8`, `--apagado #a39d92`.
- Acento: `--naranja #e08a3c` (una sola acción principal por pantalla, ítem activo del menú, paso que "le toca").
- Semánticos: `--info` En gestión · `--curso` En curso · `--exito` Comprado/Aprobado/Lista · `--aviso` Media · `--peligro` Rechazado/Alta/errores · `--inactivo` Cancelado/Omitido.
- Fuentes: Public Sans (texto) y JetBrains Mono (montos, códigos, fechas, etiquetas en mayúscula).

### Componentes sugeridos

| Componente | Sección del mock | Props principales |
|---|---|---|
| `Layout` + `BarraSuperior` | barra superior | `usuario`, `rol`, `aprobacionesPendientes` |
| `Badge` | 03 | `tipo`: pendiente · gestion · comprado · rechazado · cancelado · lista · curso · aprobada · rechazada · cancelada · aprobado · omitido · activo · espera |
| `Urgencia` | 03 | `nivel`: alta · media · baja (barritas + texto) |
| `Precio` | 04 | `monto`, `moneda` (ARS/USD, formato es-AR con 2 decimales, nunca sumar monedas distintas) |
| `CampoMonto` | 02 | `valor`, `moneda`, `onChange` |
| `TablaPaginada` | 04 | `columnas`, `filas`, `pagina`, `total`, `cargando`, `error`, `vacio` |
| `EstadoVacio` / `EstadoError` / `SinPermiso` | 04, 05 | `titulo`, `texto`, `accion` |
| `Aviso` | 05 | `tipo` (aviso/error/info), `titulo`, `codigo`, `accion` |
| `Toast` + `useToast()` | 06 | `tipo`, `titulo`, `texto` |
| `ModalConfirmacion` / `ModalRechazo` | 06 | `titulo`, `onConfirmar`; el de rechazo exige `motivo` |
| `PasosAprobacion` | 07 | `pasos[]` agrupados por `orden` (mismo orden = paralelo) |
| `Historial` | 07 | `eventos[]` con `antes`/`despues` opcionales |
| `Adjuntos` | 08 | PDF/JPG/PNG, máx. 10 MB, estados subido / subiendo / error |

### Menú por rol (del brief)

Todos los roles empiezan el menú con **Inicio** (ícono de casa + separador) y el logo también lleva a Inicio. Después del login, todos van a **Inicio** ([InicioMock](InicioMock.md)).

| Rol | Menú (después de Inicio) |
|---|---|
| Solicitante | Mis solicitudes · Nueva solicitud |
| Encargado | Bandeja · Órdenes · Catálogo · Reportes · Mis solicitudes · Nueva solicitud |
| Supervisor | Aprobaciones · Mis solicitudes · Nueva solicitud |
| Admin | Usuarios · Sectores · Reglas · Reportes |
| Aprobador por sector | Aprobaciones (solo si `esAprobador`) · Mis solicitudes · Nueva solicitud |

El contador de Aprobaciones sale de `GET /api/v1/aprobaciones/resumen` → `{ esAprobador, paraDecidir }`.

### Errores de negocio
El backend responde `{ codigo, mensaje }`. Mostrar `mensaje` en un `Aviso` o `Toast` y reaccionar al `codigo`
(ej: conflicto de versión → botón Recargar; ítem ya tomado → sacarlo de la bandeja).

### Lo que es solo de la maqueta
La franja "MAQUETA · ver como", los botones de estado de la tabla (Cargando/Vacío/…) y las secciones 01–03 (muestras de colores, tipografía, botones) no van a la app.
