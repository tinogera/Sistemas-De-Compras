# Brief de diseño: Sistema de Compras (frontend)

Pegá este documento en Claude Design y pedile, por ejemplo: *"Diseñá un sistema de diseño (colores, tipografía, componentes) y las pantallas de la lista de abajo, empezando por el layout y las pantallas de cada rol."*

## 1. Qué es

Aplicación web interna para que los empleados de una empresa pidan compras, el equipo de Compras las gestione (cotizar, comprar) y los aprobadores autoricen los montos grandes. Idioma de la interfaz: **español rioplatense** ("Salir", "Aprobar", "Rechazar"). Monedas: **ARS y USD** (nunca se suman entre sí: tablas y totales separados por moneda).

Stack del front: React 19 + Vite, React Router, Recharts para gráficos, sin librería de UI definida (el diseño la define). Uso principal en escritorio; que se vea bien en tablet.

## 2. Roles y menú

| Rol | Qué hace | Menú |
|---|---|---|
| **Solicitante** (cualquier empleado) | Crea solicitudes y ve su estado | Mis solicitudes · Nueva solicitud |
| **Encargado** (Compras) | Toma ítems, cotiza, fija precio, registra la compra, gestiona órdenes, proveedores y categorías | Bandeja · Órdenes · Catálogo · Reportes (+ lo del Solicitante) |
| **Supervisor** | Aprueba pasos de la cadena (siempre ve "Aprobaciones") | Aprobaciones (+ Solicitante) |
| **Admin** | Usuarios, sectores, reglas de aprobación, reportes | Usuarios · Sectores · Reglas · Reportes |
| Aprobador por sector (ej. Gerencia General) | El menú "Aprobaciones" aparece solo si el usuario es aprobador de alguna regla | Aprobaciones |

Barra superior: nombre del usuario, rol, botón **Salir**; indicador con la cantidad de aprobaciones pendientes. Tras el login: Encargado → Bandeja, Admin → Usuarios, el resto → Mis solicitudes.

## 3. Conceptos y estados que la interfaz debe mostrar

- **Solicitud**: tiene uno o más **ítems** (producto o servicio), urgencia (**Alta / Media / Baja**) y fecha necesaria.
- **Estado del ítem**: Pendiente → En gestión → Comprado | Rechazado | Cancelado. Una solicitud queda "Lista" cuando todos sus ítems se resolvieron.
- **Precio**: cada ítem tiene un *precio estimado* (lo carga el solicitante) y un *precio total* (lo fija el Encargado eligiendo una cotización o a mano). Se muestran lado a lado.
- **Cadena de aprobación** (por ítem): lista ordenada de pasos. Cada paso lo cumple un rol (Supervisor) o un sector (Gerencia General). Estado de cada paso: Pendiente (activo / aún no le toca) · Aprobado · Rechazado · Omitido · Cancelado. Pasos con el mismo orden corren en paralelo. Si cambia el precio o se edita el ítem, la cadena se recalcula y el ítem puede volver a "en curso".
- **Estado de aprobación del ítem**: En curso · Aprobada · Rechazada · Cancelada. No se puede registrar la compra hasta que esté Aprobada.
- **Tiempo real**: el precio total y las aprobaciones se actualizan en pantalla sin recargar (el front recibe una señal y vuelve a pedir el detalle).
- **Errores de negocio frecuentes** (mostrar con mensaje claro): otro Encargado tomó el ítem antes; conflicto de versión (alguien decidió primero, "recargá"); paso que todavía no le toca; no podés decidir sobre tu propio pedido; ítem ya resuelto.

## 4. Pantallas

### Comunes
- **Login**: email + contraseña, mensaje de error del backend.
- **Layout**: barra superior + menú por rol, páginas "En construcción", "No tenés permiso", 404.
- **Componentes comunes**: tabla paginada con filtros, badges de estado y urgencia, selector de moneda/monto, modal de confirmación, toast de error/éxito, estados vacío / cargando / error, subida y lista de adjuntos (PDF, JPG, PNG, máx. 10 MB), línea de historial.

### Solicitante (`/solicitudes…`)
1. **Mis solicitudes**: lista con filtro por estado, urgencia, fecha, cantidad de ítems; acceso a detalle.
2. **Nueva solicitud**: formulario con ítems que se agregan y quitan (tipo, detalle, cantidad, categoría con autocompletado o crear una nueva escribiéndola, precio estimado + moneda, adjuntos), urgencia, fecha necesaria, validaciones antes de enviar.
3. **Detalle de solicitud**: estado de cada ítem, resultado (proveedor elegido o motivo del rechazo), precio estimado vs. total, cadena de aprobación del ítem, adjuntos, **historial de cambios**; editar/quitar ítems mientras estén Pendientes; cancelar la solicitud.

### Aprobador (`/aprobaciones…`)
4. **Aprobaciones**: pestañas **Para decidir** · **En curso** (lo que viene, solo lectura) · **Decididas**; (Admin: **Todas**). Cada fila: solicitante, sector, categoría, detalle, urgencia, precio estimado y total, regla aplicada, paso activo. Orden por urgencia y antigüedad.
5. **Detalle de aprobación**: datos del ítem, precio estimado y total lado a lado (se actualiza en vivo), componente **Pasos de aprobación** (línea de pasos con quién decide, estado, quién decidió y cuándo, motivo), botones **Aprobar** y **Rechazar** (rechazo exige motivo) solo si el paso está activo y es del usuario.

### Encargado (`/bandeja…`, `/ordenes`, `/catalogo…`)
6. **Bandeja**: lista de ítems sin resolver ordenada por urgencia y fecha necesaria, filtros, pestaña **Para comprar** (ya aprobados), botones tomar / reasignar, aviso si otro Encargado lo tomó antes.
7. **Gestión de un ítem**: datos del ítem y adjuntos, proveedores sugeridos de la categoría, alta rápida de proveedor, carga de cotizaciones (proveedor, moneda, precio unitario, validez, presupuesto adjunto) y **comparación** de cotizaciones, fijar precio total (eligiendo cotización vigente o a mano), ver la cadena de aprobación, registrar la compra (bloqueado hasta aprobada), rechazar con motivo, corregir precio y cargar envío después de comprar.
8. **Órdenes**: lista con número correlativo, estado, marcar como Enviada al proveedor, detalle.
9. **Catálogo**: proveedores (alta, edición, activo/inactivo, categorías asociadas) y categorías (Nueva → Conocida, unificar duplicadas).

### Admin (`/admin/…`)
10. **Usuarios**: alta, edición, rol (incluye Supervisor), sector, activar/desactivar.
11. **Sectores**: ABM simple.
12. **Reglas de aprobación**: lista por prioridad con activar/desactivar; formulario con condiciones (hoy: monto mayor/menor a X en una moneda), **pasos ordenables** (rol o sector aprobador, mismo orden = paralelo), regla por defecto protegida (no se desactiva ni queda sin pasos); y un **Probador**: "con 600.000 ARS, ¿qué cadena se arma?" → regla elegida y pasos.

### Reportes (`/reportes`, Encargado y Admin)
13. Filtros de fecha desde/hasta; **gasto** por sector, categoría y proveedor (tablas separadas ARS y USD + gráfico de barras); **tiempo promedio de resolución** (general y por urgencia).

## 5. Flujos clave para prototipar
1. Solicitante crea una solicitud de 2 ítems → ve "Pendiente" → recibe mail cuando queda Lista.
2. Encargado toma un ítem, carga 3 cotizaciones, elige una → fija precio total (600.000 ARS) → se arma la cadena (Supervisor, luego Gerencia General).
3. Supervisor ve el ítem en "Para decidir", ve el total aparecer en vivo, aprueba → el paso pasa a Gerencia General.
4. Gerencia General rechaza con motivo → el ítem queda Rechazado y el solicitante ve el motivo.
5. Admin crea una regla ("más de 500.000 ARS → Supervisor y después Gerencia General") y la prueba con el probador.

## 6. Endpoints del backend (para las funciones de API)
- Auth: `POST /api/v1/auth/login`, `GET /api/v1/auth/yo`.
- Solicitudes: `POST/GET /api/v1/solicitudes`, detalle, edición/cancelación de ítems, adjuntos de ítem.
- Compras: bandeja, tomar/reasignar, cotizaciones (+ adjuntos), fijar precio, registrar compra, rechazar.
- Órdenes y reportes: `/api/v1/ordenes`, `/api/v1/reportes/gasto`, `/api/v1/reportes/tiempos-resolucion` (en desarrollo).
- Catálogo: proveedores y categorías.
- Aprobaciones: `GET /api/v1/aprobaciones?vista=para-decidir|en-curso|decididas|todas&page=`, `GET /api/v1/aprobaciones/items/{itemId}`, `GET /api/v1/aprobaciones/resumen` → `{esAprobador, paraDecidir}`, `POST /api/v1/aprobaciones/pasos/{pasoId}/aprobar` `{version}` y `/rechazar` `{motivo, version}`.
- Reglas (Admin): `GET/POST/PUT /api/v1/admin/reglas`, `POST …/{id}/activar|desactivar`, `POST /api/v1/admin/reglas/simular` `{monto, moneda, sectorId?, categoriaId?}`.
- Tiempo real (pendiente de backend, T24.1): stream SSE con señales de ids.
- Errores: JSON con `codigo` y `mensaje`; el front muestra el mensaje y reacciona al `codigo`.

## 7. Qué le pedimos al diseño
- Un **sistema de diseño**: paleta (con color semántico para estados y urgencias), tipografía, espaciados, componentes (tablas, formularios, badges, modales, línea de pasos).
- Las pantallas de la sección 4, empezando por Layout, Login, Mis solicitudes, Nueva solicitud y Detalle de aprobación.
- Todos los estados: vacío, cargando, error, sin permiso, conflicto de versión.
- Accesibilidad básica (contraste, foco visible, no depender solo del color para el estado).
