# Nivel 3 — Área, dificultad y fechas

Tablero de todas las subtareas. El detalle de cada una está en `02-tareas-especificas.md` (buscar por ID).

- **Área:** dónde se trabaja. Back = `back/` · Front = `frontend/` · Infra = Docker, base de datos, CI · n8n = workflows · Repo = Git/GitHub · Docs = documentación.
- **Dificultad (D):** 1 trivial · 2 fácil · 3 media · 4 difícil · 5 muy difícil.
- **Importancia (I):** 3 imprescindible (sin esto el flujo del enunciado no funciona) · 2 importante (lo pide el enunciado, pero el flujo anda sin eso) · 1 deseable.
- **Peso = D × I.** Las de peso alto conviene tomarlas primero dentro del sprint.
- **Fecha límite:** las subtareas que bloquean a otras vencen a mitad de sprint; el resto, al final.
- **Responsable:** quien toma la tarea pone su nombre. **Estado:** ⬜ pendiente · 🟨 en curso · ✅ terminada · 🟥 atrasada (la fecha límite ya pasó, o nació atrasada por un cambio de requisito).

Última actualización: 08/10/2026. T01 a T03 están terminadas (CLAUDE.md, "Estado"); el resto sigue pendiente.

## Atrasadas

| ID | Subtarea | Fecha que tendría que haber tenido | Por qué |
|---|---|---|---|
| T23.1 | Módulo `aprobaciones` y sus tablas | 04/10 | Es el noveno módulo del requisito de reglas de aprobación (08/10). Tendría que haber salido con el esqueleto modular y las migraciones base (T02, ya mergeadas). Bloquea T23.2 y todo lo que viene después. |

Ninguna otra subtarea tiene la fecha vencida al 08/10. **Lo que sigue en riesgo:** las de mitad de S1 (11/10) que bloquean a otras —T04.1, T06.1, T06.2, T07.1, T07.3— y que ahora, además, tienen que incluir los cambios del requisito nuevo (rol Supervisor en T04.1; precio estimado, estado de aprobación y la tabla de eventos nueva en T07.1 y T07.3).

## Qué cambió con el requisito de reglas de aprobación (08/10)

Las decisiones de diseño están en `02-tareas-especificas.md` (decisiones 7 a 13).

- **Subtareas nuevas:** T10.7, T10.8, T11.6, T15.9, T23.1, T23.2, T23.3, T23.4, T23.5, T23.6, T23.7, T23.8, T24.1, T24.2, T25.1, T25.2.
- **Modificadas** (cambió el contenido, no solo la fecha): T04.1 a T04.5, T05.1, T05.3, T07.1, T07.3, T07.4, T07.5, T08.1, T08.4 a T08.6, T09.1 a T09.3, T10.3 (antes "Aprobar un ítem"), T10.4, T11.1 a T11.3, T11.5, T12.2, T12.3, T13.1, T13.2, T14.3, T15.2, T15.4, T15.8, T16.1 a T16.4, T17.1, T17.4, T18.1, T18.3, T19.1 a T19.3, T20.1, T20.2, T20.4, T21.1, T21.3, T22.1, T22.4 y T22.5.
- **Cambiaron de sprint** para hacer lugar: T17.4 y T06.4 pasan de S1 a S2; T08.4 pasa de S2 a S3; T09.5, T06.6, T12.2 y T12.3 pasan a S4. T05.2 y T05.3 **se quedan en S2** a propósito: el Admin los necesita para crear Gerencia General y nombrar a un Supervisor antes de poder armar una regla.
- **Carga:** el proyecto pasa de 103 subtareas y peso 551 a 119 subtareas y peso 672 (los 103 + las nuevas). Es +22 % de trabajo con la misma fecha de entrega (4/12, a confirmar con la cátedra). Si se atrasa un sprint, se recortan primero las subtareas de importancia 1 (ver el final de este archivo).

- **Documentos ya actualizados:** `Docs/enunciado.md` (usuarios, ejemplo de Laura en 9 pasos, estados, reglas de aprobación, módulos y reglas 4, 6, 7 y 11 a 16) y `Docs/diseno-del-sistema.md` (RF, arquitectura, schemas, APIs, fallas y casos borde 24 a 28). T22.1 queda para volcar en el diseño las decisiones 1 a 6 y revisar todo contra lo que se implementó.

## Resumen

| Sprint | Subtareas | Peso total | Por área |
|---|---|---|---|
| S0 | 8 | 32 | Back 4, Infra 2, Repo 2 |
| S1 | 24 | 138 | Back 20, Front 3, Infra 1 |
| S2 | 23 | 146 | Back 19, Front 4 |
| S3 | 20 | 151 | Back 16, Front 4 |
| S4 | 34 | 152 | Back 21, Front 7, n8n 5, Infra 1 |
| Cierre | 6 | 31 | Infra 1, Docs 4, Back + Front 1 |
| Continuo | 2 | 15 | Back 2 |
| Si sobra | 2 | 7 | Back 2 |
| **Total** | **119** | **672** | |

Peso por sprint: S1 138 (de los cuales 18 ya están hechos, T03), S2 146, S3 151 y S4 152. **S3 es el sprint de riesgo:** tiene que dejar andando el ejemplo de Laura de punta a punta con la cadena de aprobación, la compra y las órdenes.

## S0 — Fundamentos (28/09 → 04/10)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T02.1 | Paquetes de los 8 módulos y test de arquitectura | Back | 2 | 3 | 6 | 04/10 | — | — | ✅ |
| T02.2 | Migraciones iniciales y validación de Hibernate | Back | 2 | 3 | 6 | 04/10 | T01.3 | — | ✅ |
| T02.3 | Tests con Testcontainers | Back | 2 | 3 | 6 | 04/10 | T02.2 | — | ✅ |
| T23.1 | Módulo `aprobaciones` y sus tablas | Back | 2 | 3 | 6 | 04/10 | T02.2 | — | 🟥 |
| T01.3 | Base de datos y `.env` de cada integrante | Infra | 1 | 3 | 3 | 04/10 | — | — | ✅ |
| T01.1 | Limpiar el repo | Repo | 1 | 2 | 2 | 04/10 | — | — | ✅ |
| T01.2 | Plantilla de PR y protección de `main` | Repo | 1 | 2 | 2 | 04/10 | — | — | ✅ |
| T01.4 | Fijar la versión de Postgres | Infra | 1 | 1 | 1 | 04/10 | — | — | ✅ |

## S1 — Base del dominio (05/10 → 18/10)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T07.3 | Definir los eventos del sistema | Back | 3 | 3 | 9 | 11/10 | T03.1 | — | ⬜ |
| T03.1 | Módulo `compartido` | Back | 2 | 3 | 6 | 11/10 | T02.1 | — | ✅ |
| T03.2 | Formato de errores y manejador global | Back | 2 | 3 | 6 | 11/10 | T03.1 | — | ✅ |
| T04.1 | Schema y dominio de `auth` | Back | 2 | 3 | 6 | 11/10 | T02.2, T03.1 | — | ⬜ |
| T06.1 | Schema y dominio del catálogo | Back | 2 | 3 | 6 | 11/10 | T02.2 | — | ⬜ |
| T06.2 | Repositories del catálogo | Back | 2 | 3 | 6 | 11/10 | T06.1 | — | ⬜ |
| T07.1 | Schema y dominio de solicitudes | Back | 2 | 3 | 6 | 11/10 | T02.2, T03.1 | — | ⬜ |
| T17.1 | Estructura, rutas y layout | Front | 2 | 3 | 6 | 11/10 | — | — | ⬜ |
| T17.2 | Cliente HTTP | Front | 2 | 3 | 6 | 11/10 | — | — | ⬜ |
| T04.2 | Repositories de `auth` | Back | 1 | 3 | 3 | 11/10 | T04.1 | — | ⬜ |
| T07.2 | Repositories de solicitudes | Back | 1 | 3 | 3 | 11/10 | T07.1 | — | ⬜ |
| T03.3 | Respuesta paginada estándar | Back | 1 | 2 | 2 | 11/10 | T03.1 | — | ✅ |
| T04.4 | Login con JWT y configuración de seguridad | Back | 4 | 3 | 12 | 18/10 | T04.2, T03.2 | — | ⬜ |
| T07.4 | Crear una solicitud | Back | 3 | 3 | 9 | 18/10 | T07.2, T07.3, T06.5, T04.5 | — | ⬜ |
| T17.3 | Login, usuario actual y rutas protegidas | Front | 3 | 3 | 9 | 18/10 | T17.1, T17.2, T04.4 | — | ⬜ |
| T23.2 | Dominio, repositories y motor de reglas | Back | 3 | 3 | 9 | 18/10 | T23.1, T03.1 | — | ⬜ |
| T04.5 | Usuario logueado y `AuthApi` | Back | 2 | 3 | 6 | 18/10 | T04.2, T04.4 | — | ⬜ |
| T06.3 | ABM de proveedores | Back | 2 | 3 | 6 | 18/10 | T06.2, T03.2 | — | ⬜ |
| T06.5 | `CatalogoApi` para otros módulos | Back | 2 | 3 | 6 | 18/10 | T06.2, T07.3 | — | ⬜ |
| T07.5 | Consultar solicitudes | Back | 2 | 3 | 6 | 18/10 | T07.2, T03.3 | — | ⬜ |
| T07.6 | Tests del módulo Solicitudes | Back | 2 | 2 | 4 | 18/10 | T07.4, T07.5 | — | ⬜ |
| T03.4 | Swagger | Back | 1 | 2 | 2 | 18/10 | T02.3 | — | ✅ |
| T03.5 | CI con GitHub Actions | Infra | 2 | 1 | 2 | 18/10 | T02.3 | — | ✅ |
| T04.3 | Datos de prueba | Back | 1 | 2 | 2 | 18/10 | T04.4 | — | ⬜ |

## S2 — Compras y reglas de aprobación (19/10 → 01/11)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T09.2 | Crear las gestiones al entrar una solicitud | Back | 3 | 3 | 9 | 25/10 | T09.1, T07.4 | — | ⬜ |
| T09.1 | Schema y dominio de Compras | Back | 2 | 3 | 6 | 25/10 | T02.2, T03.1 | — | ⬜ |
| T10.1 | Schema y dominio de cotizaciones | Back | 2 | 3 | 6 | 25/10 | T09.1 | — | ⬜ |
| T18.2 | Mis solicitudes | Front | 2 | 3 | 6 | 25/10 | T17.4, T07.5 | — | ⬜ |
| T06.4 | Búsqueda de categorías | Back | 2 | 2 | 4 | 25/10 | T06.2 | — | ⬜ |
| T17.4 | Componentes comunes | Front | 2 | 2 | 4 | 25/10 | T17.1 | — | ⬜ |
| T06.7 | Proveedores sugeridos (con unificadas) | Back | 1 | 3 | 3 | 25/10 | T06.5 | — | ⬜ |
| T23.4 | Iniciar y mantener la cadena de cada ítem | Back | 4 | 3 | 12 | 01/11 | T23.2, T07.3, T07.4 | — | ⬜ |
| T09.4 | Tomar y reasignar | Back | 3 | 3 | 9 | 01/11 | T09.3 | — | ⬜ |
| T10.3 | Elegir una cotización y fijar el precio total | Back | 3 | 3 | 9 | 01/11 | T10.2 | — | ⬜ |
| T18.1 | Nueva solicitud | Front | 3 | 3 | 9 | 01/11 | T17.3, T07.4, T06.4 | — | ⬜ |
| T18.3 | Detalle de una solicitud | Front | 3 | 3 | 9 | 01/11 | T18.2, T08.1, T08.2, T08.3 | — | ⬜ |
| T23.3 | ABM de reglas (Admin) | Back | 3 | 3 | 9 | 01/11 | T23.2, T05.1, T04.5 | — | ⬜ |
| T05.1 | Permisos por rol en todos los endpoints | Back | 2 | 3 | 6 | 01/11 | T04.4 | — | ⬜ |
| T08.1 | Editar un ítem Pendiente | Back | 2 | 3 | 6 | 01/11 | T07.4 | — | ⬜ |
| T08.2 | Borrar (cancelar) un ítem Pendiente | Back | 2 | 3 | 6 | 01/11 | T07.4 | — | ⬜ |
| T08.3 | Cancelar la solicitud entera | Back | 2 | 3 | 6 | 01/11 | T07.4 | — | ⬜ |
| T09.3 | Bandeja | Back | 2 | 3 | 6 | 01/11 | T09.2 | — | ⬜ |
| T10.2 | Cargar y listar cotizaciones | Back | 2 | 3 | 6 | 01/11 | T10.1, T09.4, T06.5 | — | ⬜ |
| T10.4 | Rechazar un ítem | Back | 2 | 3 | 6 | 01/11 | T09.4 | — | ⬜ |
| T05.3 | ABM de usuarios | Back | 2 | 2 | 4 | 01/11 | T05.1 | — | ⬜ |
| T10.5 | Proveedores sugeridos para un ítem | Back | 1 | 3 | 3 | 01/11 | T06.7, T09.3 | — | ⬜ |
| T05.2 | ABM de sectores | Back | 1 | 2 | 2 | 01/11 | T05.1 | — | ⬜ |

## S3 — Aprobaciones, compra y órdenes (02/11 → 15/11)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T10.7 | Registrar la compra | Back | 3 | 3 | 9 | 05/11 | T10.3, T09.4, T07.3 | — | ⬜ |
| T23.5 | Aprobar o rechazar un paso | Back | 4 | 3 | 12 | 08/11 | T23.4, T05.1 | — | ⬜ |
| T11.2 | Generar la orden al registrarse la compra | Back | 3 | 3 | 9 | 08/11 | T11.1, T10.7 | — | ⬜ |
| T19.1 | Bandeja | Front | 3 | 3 | 9 | 08/11 | T09.4 | — | ⬜ |
| T08.5 | Actualizar el estado de los ítems desde Compras | Back | 2 | 3 | 6 | 08/11 | T09.4, T10.3, T10.7 | — | ⬜ |
| T11.1 | Schema y dominio de órdenes | Back | 2 | 3 | 6 | 08/11 | T02.2 | — | ⬜ |
| T12.1 | Reintento de eventos pendientes | Back | 2 | 3 | 6 | 08/11 | T02.2 | — | ⬜ |
| T13.1 | Schema, dominio y repository de auditoría | Back | 2 | 3 | 6 | 08/11 | T02.2 | — | ⬜ |
| T23.6 | Pendientes y detalle para los aprobadores | Back | 2 | 3 | 6 | 08/11 | T23.4 | — | ⬜ |
| T14.1 | Servicio de almacenamiento de archivos | Back | 2 | 2 | 4 | 08/11 | T03.1 | — | ⬜ |
| T08.6 | Estado automático de la solicitud | Back | 4 | 3 | 12 | 15/11 | T08.5, T11.4 | — | ⬜ |
| T19.2 | Gestión de un ítem | Front | 4 | 3 | 12 | 15/11 | T10.3, T10.4, T10.5, T10.7, T25.1 | — | ⬜ |
| T13.2 | Registrar los eventos en el historial | Back | 3 | 3 | 9 | 15/11 | T13.1, T07.3 | — | ⬜ |
| T24.1 | Señales en tiempo real (backend) | Back | 3 | 3 | 9 | 15/11 | T07.3, T04.4 | — | ⬜ |
| T25.1 | Pantallas del aprobador | Front | 3 | 3 | 9 | 15/11 | T23.5, T23.6, T17.4 | — | ⬜ |
| T08.4 | Sincronizar ediciones y cancelaciones con Compras | Back | 3 | 2 | 6 | 15/11 | T08.1, T08.2, T08.3, T09.2 | — | ⬜ |
| T11.4 | Marcar una orden como enviada | Back | 2 | 3 | 6 | 15/11 | T11.2 | — | ⬜ |
| T14.2 | Adjuntos de un ítem | Back | 3 | 2 | 6 | 15/11 | T14.1, T07.1 | — | ⬜ |
| T19.3 | Órdenes | Front | 2 | 3 | 6 | 15/11 | T11.4 | — | ⬜ |
| T11.3 | Listar órdenes | Back | 1 | 3 | 3 | 15/11 | T11.1 | — | ⬜ |

## S4 — Tiempo real, avisos y reportes (16/11 → 29/11)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T24.2 | Tiempo real en el front | Front | 3 | 3 | 9 | 22/11 | T24.1, T17.2 | — | ⬜ |
| T09.5 | Test de concurrencia | Back | 3 | 2 | 6 | 22/11 | T09.4 | — | ⬜ |
| T12.2 | Listeners idempotentes | Back | 3 | 2 | 6 | 22/11 | T11.2, T13.2 | — | ⬜ |
| T12.3 | Tests entre módulos | Back | 3 | 2 | 6 | 22/11 | T11.2, T08.5 | — | ⬜ |
| T15.5 | Endpoints de recordatorios | Back | 3 | 2 | 6 | 22/11 | T09.4 | — | ⬜ |
| T16.2 | Alimentar los reportes con eventos | Back | 3 | 2 | 6 | 22/11 | T16.1, T11.2, T10.8 | — | ⬜ |
| T10.8 | Editar la compra y cargar el monto de envío | Back | 2 | 2 | 4 | 22/11 | T10.7 | — | ⬜ |
| T13.3 | Consultar el historial | Back | 2 | 2 | 4 | 22/11 | T13.2 | — | ⬜ |
| T14.3 | Adjuntos de una cotización | Back | 2 | 2 | 4 | 22/11 | T14.2, T10.1 | — | ⬜ |
| T15.1 | n8n y Mailpit en Docker | Infra | 2 | 2 | 4 | 22/11 | T01.3 | — | ⬜ |
| T15.2 | Enviar los avisos a n8n | Back | 2 | 2 | 4 | 22/11 | T15.1, T08.6, T23.4 | — | ⬜ |
| T16.1 | Schema y dominio de reportes | Back | 2 | 2 | 4 | 22/11 | T02.2 | — | ⬜ |
| T06.6 | Confirmar una categoría Nueva | Back | 1 | 2 | 2 | 22/11 | T06.4 | — | ⬜ |
| T15.4 | Workflow: resultado al solicitante | n8n | 3 | 3 | 9 | 29/11 | T15.2 | — | ⬜ |
| T25.2 | Administración de reglas | Front | 3 | 3 | 9 | 29/11 | T23.3, T05.2 | — | ⬜ |
| T15.6 | Workflow: recordatorios diarios | n8n | 3 | 2 | 6 | 29/11 | T15.5 | — | ⬜ |
| T16.3 | Reporte de gasto | Back | 3 | 2 | 6 | 29/11 | T16.2 | — | ⬜ |
| T19.4 | Catálogo (proveedores y categorías) | Front | 3 | 2 | 6 | 29/11 | T06.6, T06.8 | — | ⬜ |
| T20.2 | Reportes | Front | 3 | 2 | 6 | 29/11 | T16.3, T16.4 | — | ⬜ |
| T11.6 | Actualizar la orden cuando se edita la compra | Back | 2 | 2 | 4 | 29/11 | T11.2, T10.8 | — | ⬜ |
| T15.3 | Workflow: aviso de solicitud nueva | n8n | 2 | 2 | 4 | 29/11 | T15.2 | — | ⬜ |
| T15.9 | Workflow: aviso de aprobación pendiente | n8n | 2 | 2 | 4 | 29/11 | T15.2 | — | ⬜ |
| T16.4 | Tiempo de resolución | Back | 2 | 2 | 4 | 29/11 | T16.2 | — | ⬜ |
| T20.1 | Administración de usuarios y sectores | Front | 2 | 2 | 4 | 29/11 | T05.3 | — | ⬜ |
| T20.3 | Adjuntos | Front | 2 | 2 | 4 | 29/11 | T14.2, T14.3 | — | ⬜ |
| T05.4 | Reglas del último Encargado y quitar el rol | Back | 3 | 1 | 3 | 29/11 | T05.3, T09.4 | — | ⬜ |
| T06.8 | Unificar categorías duplicadas | Back | 3 | 1 | 3 | 29/11 | T06.6 | — | ⬜ |
| T11.5 | Orden en PDF | Back | 3 | 1 | 3 | 29/11 | T11.2 | — | ⬜ |
| T08.7 | Evitar duplicados por doble click | Back | 2 | 1 | 2 | 29/11 | T07.4 | — | ⬜ |
| T10.6 | Marcar cotizaciones vencidas en la bandeja | Back | 2 | 1 | 2 | 29/11 | T10.2 | — | ⬜ |
| T15.7 | Seguridad de la integración | Back | 2 | 1 | 2 | 29/11 | T15.5 | — | ⬜ |
| T15.8 | Exportar los workflows al repo | n8n | 1 | 2 | 2 | 29/11 | T15.3, T15.4, T15.6, T15.9 | — | ⬜ |
| T20.4 | Historial | Front | 1 | 2 | 2 | 29/11 | T13.3 | — | ⬜ |
| T23.7 | Simulador de reglas (Admin) | Back | 2 | 1 | 2 | 29/11 | T23.2 | — | ⬜ |

## Cierre (30/11 → 04/12)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T21.3 | Prueba completa (ejemplo de Laura y casos borde) | Back + Front | 3 | 3 | 9 | 02/12 | Todo S3 | — | ⬜ |
| T22.4 | Datos y guion de la demo | Docs | 3 | 3 | 9 | 03/12 | T21.3 | — | ⬜ |
| T22.1 | Actualizar el diseño | Docs | 3 | 2 | 6 | 03/12 | — | — | ⬜ |
| T22.2 | README de instalación | Docs | 1 | 3 | 3 | 03/12 | — | — | ⬜ |
| T22.3 | Empaquetado para la demo | Infra | 3 | 1 | 3 | 03/12 | — | — | ⬜ |
| T22.5 | Escalado y manejo de fallas | Docs | 1 | 1 | 1 | 03/12 | — | — | ⬜ |

## Continuo (durante todo el proyecto)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T21.1 | Tests de reglas de dominio | Back | 3 | 3 | 9 | 29/11 | — | — | ⬜ |
| T21.2 | Tests de endpoints | Back | 3 | 2 | 6 | 29/11 | T04.4 | — | ⬜ |

## Si sobra tiempo

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T14.4 | (Opcional) Almacenamiento S3/MinIO | Back | 4 | 1 | 4 | — | T14.1 | — | ⬜ |
| T23.8 | (Opcional) Recordatorios a los aprobadores | Back | 3 | 1 | 3 | — | T23.5, T15.5 | — | ⬜ |

## Si hay que recortar (importancia 1)

Según el README de planificación, lo primero que se saca de un sprint atrasado es lo de importancia 1. Hoy son: T05.4 (3), T06.8 (3), T08.7 (2), T10.6 (2), T11.5 (3), T14.4 (4), T15.7 (2), T22.3 (3), T22.5 (1), T23.7 (2), T23.8 (3). Suman 28 de peso.
