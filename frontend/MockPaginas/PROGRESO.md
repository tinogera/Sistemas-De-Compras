# Progreso de diseño de pantallas

Fuente de verdad: `Docs/brief-diseno-front.md` (sección 4).
Estados: PENDIENTE / EN REVISIÓN / APROBADA. Las APROBADAS quedan congeladas.
Cómo ver los mocks (sin y con Claude Code): `Docs/mocks/README.md`.

Reglas:
- **Me gusta / ok / aprobada / va** → se mueve a `aprobadas/NombreMock.html`, se marca APROBADA con fecha y decisión, queda congelada. Se anuncia la próxima y se genera recién con "dale".
- **No me gusta** → con detalle: se cambia solo eso; sin detalle: se ofrecen 2-3 alternativas antes de tocar nada. Sigue EN REVISIÓN.
- Una pantalla por vez, con el estilo de las aprobadas.
- Cada mock lleva su documentación en `Docs/mocks/NombreMock.md`: cómo verlo, qué probar y cómo implementarlo.

Base visual: la home (landing variante oscura) en `aprobadas/HomeMock/HomeMock.html`.
Fondo #1b1916, texto #fbfaf8, Public Sans + JetBrains Mono, acento naranja #e08a3c.

| # | Pantalla | Mock | Estado |
|---|---|---|---|
| — | Home (landing oscura) | `aprobadas/HomeMock/HomeMock.html` | APROBADA (base de formato) |
| 00 | Componentes comunes + layout | `aprobadas/LayoutMock.html` | APROBADA (2026-11-25) |
| 01 | Login | `aprobadas/LoginMock.html` | APROBADA (2026-11-25, cambio 2026-11-26) |
| 02 | Mis solicitudes | `aprobadas/MisSolicitudesMock.html` | APROBADA (2026-11-25) |
| 03 | Nueva solicitud | `aprobadas/NuevaSolicitudMock.html` | APROBADA (2026-11-25) |
| 04 | Detalle de solicitud | `aprobadas/DetalleSolicitudMock.html` | APROBADA (2026-11-25) |
| 05 | Aprobaciones | `aprobadas/AprobacionesMock.html` | APROBADA (2026-11-26) |
| 06 | Detalle de aprobación | `aprobadas/DetalleAprobacionMock.html` | APROBADA (2026-11-25) |
| 07 | Bandeja | `aprobadas/BandejaMock.html` | APROBADA (2026-11-26) |
| 08 | Gestión de un ítem | `aprobadas/GestionItemMock.html` | APROBADA (2026-11-26) |
| 09 | Órdenes | `aprobadas/OrdenesMock.html` | APROBADA (2026-11-26) |
| 10 | Catálogo | `aprobadas/CatalogoMock.html` | APROBADA (2026-11-26) |
| 11 | Usuarios | `aprobadas/UsuariosMock.html` | APROBADA (2026-11-26) |
| 12 | Sectores | `aprobadas/SectoresMock.html` | APROBADA (2026-11-26) |
| 13 | Reglas de aprobación | `aprobadas/ReglasAprobacionMock.html` | APROBADA (2026-11-26) |
| 14 | Reportes | `aprobadas/ReportesMock.html` | APROBADA (2026-11-26) |
| 15 | Páginas de error (403, 404, En construcción) | `aprobadas/ErroresMock.html` | APROBADA (2026-11-26) |
| 16 | Inicio (bienvenida + accesos por rol) | `aprobadas/InicioMock.html` | APROBADA (2026-11-26) |

Otros archivos en esta carpeta:
- `Landing - heroes.html`: export original de Claude Design del hero (referencia, no se edita).
- `02-mis-solicitudes.html`: apareció el 2026-11-25 18:47, origen a confirmar.

## Decisiones

- 2026-11-25 · Home: variante oscura con resplandor naranja ovalado detrás de la imagen.
- 2026-11-25 · 00 aprobada: tema oscuro de la home; barra superior con menú por rol y contador naranja; naranja solo para la acción principal; estados con ícono + color; urgencia con barritas; montos en JetBrains Mono con su moneda.
- 2026-11-25 · 01 aprobada: tarjeta centrada con resplandor naranja suave; "Ingresar" en naranja (única acción); error del backend arriba del formulario; sin "Crear cuenta" (las cuentas las crea el Admin), el pie dice que se la pidan a un administrador; endpoint `/auth/identificate`.
- 2026-11-26 · 01 cambio aprobado: pantalla dividida; a la izquierda foto de depósito con parallax según el mouse (foto, brillo naranja y 2 tarjetitas flotantes a distintas profundidades), frase "Pedís, se aprueba y llega"; login igual a la derecha. En celular la foto queda como franja arriba; sin movimiento en táctil o con reducir movimiento. Foto de Unsplash: bajarla a `src/assets` al implementar.
- 2026-11-26 · 07 aprobada: patrón de lista de 02 con resumen de 4 contadores clicables como atajos; pestañas Sin resolver / Para comprar; orden por urgencia + fecha necesaria; tomar con conflicto `YA_ASIGNADO` y "Pasármelo" con confirmación; filas nuevas y cambios en vivo resaltados.
- 2026-11-26 · 08 aprobada: recorrido de 4 pasos (Cotizar → Fijar precio → Aprobación → Comprar); comparador de cotizaciones agrupado por moneda con barra + marca del estimado y chips (más barata, entrega más rápida, vence pronto, vencida); alta rápida de proveedor dentro del modal; lateral precio / cadena compacta / compra bloqueada con motivo; cambiar precio con pasos aprobados pide confirmación; barra fija abajo en celular. Faltan en backend: datos del solicitante y adjuntos en el detalle, indicador de adjunto por cotización, número de OC en la respuesta de comprar.
- 2026-11-26 · 09 aprobada: lista con resumen (sin enviar con demoradas > 48 h, enviadas 7 días, comprado del mes por moneda); pestañas Sin enviar / Enviadas / Todas; GENERADA se muestra "Sin enviar"; número OC-{n}; panel lateral derecho con vista previa del PDF, recorrido y links; marcar enviada con confirmación que aclara que el sistema no envía nada; `ORDEN_YA_ENVIADA`. Faltan en backend: filtros proveedor/moneda/búsqueda, resumen, sector y solicitante en la lista. Pendiente: cambiar "(Creada)" por "(Sin enviar)" en 08.
- 2026-11-26 · 12 aprobada: define el patrón "lista + formulario" (lista 1fr + formulario fijo 400 px; < 1000 px formulario a pantalla completa); modo NUEVO / EDITANDO en naranja; validación en vivo con contador; Enter guarda; botón primario blanco; "Sin guardar" + confirmar descartar; impacto al editar; sin borrado (backend no lo permite). Faltan en backend: cantidad de usuarios y reglas por sector; decidir tildes en duplicados.
- 2026-11-26 · 11 aprobada: patrón lista + formulario con scroll propio; rol en tarjetas con descripción y colores (Solicitante gris, Supervisor violeta, Encargado azul, Admin verde); email no editable; contraseña con Generar + ventana de credenciales para copiar (no hay mail); recuadro "Al guardar" con consecuencias; no se edita a sí mismo; ULTIMO_ENCARGADO bloquea antes de guardar. Faltan en backend: filtros sector/activo, bloqueo y conteos en la respuesta, impedir auto-desactivación, cambio de clave en primer ingreso.
- 2026-11-26 · 10 aprobada: pestañas Proveedores (lista + formulario; CUIT con máscara; categorías en selector múltiple con chips; aviso de proveedor parecido; consecuencias al dar de baja) y Categorías (tarjetas Por revisar con "Se parece a" y unificar como acción principal; Confirmadas en tabla; historial de unificadas; modal origen → destino con consecuencias e irreversible). Contador amarillo de categorías por revisar en el menú. Faltan en backend: conteos y origen de categorías, renombrar/crear confirmadas, cotizaciones vigentes del proveedor.
- 2026-11-26 · 13 aprobada: lista por prioridad (arrastrar + ▲▼ + Deshacer; por defecto fija al final) con condición y cadena en chips; avisos "nunca se aplica" y "sin aprobadores"; Probador lateral que resalta la regla elegida y explica descartes, también con el borrador; editor en vista propia (Nombre / Cuándo se aplica / Quién aprueba) con pasos ordenables por arrastre y Alt+flechas. Pendiente decidir si Admin puede aprobar (backend sí, front no). Faltan en backend: reordenar atómico, simular con borrador, usuarios por rol/sector.
- 2026-11-26 · 14 aprobada: filtro de período fijo (presets + desde/hasta) para los dos reportes; gasto con 4 números, barras horizontales apiladas compra (#338ced) + envío (#00a888) validadas con dataviz, top 8 + Otros, ARS y USD nunca sumados, tooltip con teclado y vista tabla; tiempo de resolución con número héroe y barras por urgencia en horas. Faltan en backend: CSV, separar espera de aprobación, evolución mensual.
- 2026-11-26 · 15 aprobada: textos de LayoutMock 05 a página completa con tarjeta y resplandor del Login y código tenue de fondo; 403 dice qué sección y para qué rol; 404 de ruta y 404 de recurso (NO_ENCONTRADO); En construcción con qué va a tener; botón de inicio y atajos según el rol; foco al título. 401 vuelve al Login; 5xx/red se muestran dentro de cada pantalla.
- 2026-11-26 · 05 aprobada: pestañas Para decidir / En curso / Decididas (+ Todas para Admin), chip VOS, espera en amarillo desde 2 días, fila nueva en vivo.
- 2026-11-26 · 16 aprobada (pantalla nueva): "Te damos la bienvenida, {nombre}" con fecha, saludo y pendientes en chips; accesos por rol en tarjetas compactas sin descripción, una fila por grupo a todo el ancho, la principal en naranja; Inicio como primer ítem del menú. Se aplica a las aprobadas: el login lleva a Inicio, todas las barras suman Inicio y el logo lleva a Inicio, las páginas de error dicen "Ir al inicio".
- 2026-11-25 · 06 aprobada: componente Pasos de aprobación versión completa (regla + condición, progreso por paso, grupos en paralelo "tienen que aprobar todos", chip VOS, nodo final); tarjeta de decisión fija con precio estimado vs total en vivo; aprobar con confirmación, rechazar con motivo obligatorio y chips. Variantes del componente: completa (06) · compacta (04, 08) · editable (13). Faltan en backend: tipo de aprobador (ROL/SECTOR) y cotizaciones para aprobadores.
- 2026-11-25 · 04 aprobada: ítems como tarjetas desplegables (resultado + datos + adjuntos | cadena en lectura); resumen con progreso y montos por moneda; historial lateral "EN VIVO" filtrable por ítem; editar/quitar solo ítems Pendientes; cambio en vivo resalta la tarjeta en naranja.
- 2026-11-25 · 02 aprobada: patrón de lista para toda la app (pestañas de estado con subrayado naranja + filtros con chips + tabla ordenable + paginación; tarjetas en celular). Estados reales de solicitud: En revisión / Lista / Cerrada / Cancelada. Filtros extra y detalle del primer ítem requieren sumar al backend (ver doc). Pendiente: corregir fila "Estado de la solicitud" en LayoutMock y decidir qué hacer con `02-mis-solicitudes.html`.
- 2026-11-25 · 03 aprobada: urgencia en tarjetas con barritas; ítems en tarjetas (producto/servicio, categoría con autocompletado y "Crear"); precio estimado = total del ítem con selector ARS/USD; adjuntos por ítem (se suben después de crear); resumen lateral con totales por moneda (barra fija en celular); detalle obligatorio en el front (falta confirmar `@NotBlank` en backend).
- 2026-11-25 · Estructura: mocks en `frontend/MockPaginas/` con nombre `NombreMock.html`; documentación en `Docs/mocks/`.
