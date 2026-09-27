# Nivel 3 — Área, dificultad y fechas

Tablero de todas las subtareas. El detalle de cada una está en `02-tareas-especificas.md` (buscar por ID).

- **Área:** dónde se trabaja. Back = `back/` · Front = `frontend/` · Infra = Docker, base de datos, CI · n8n = workflows · Repo = Git/GitHub · Docs = documentación.
- **Dificultad (D):** 1 trivial · 2 fácil · 3 media · 4 difícil · 5 muy difícil.
- **Importancia (I):** 3 imprescindible (sin esto el flujo del enunciado no funciona) · 2 importante (lo pide el enunciado, pero el flujo anda sin eso) · 1 deseable.
- **Peso = D × I.** Las de peso alto conviene tomarlas primero dentro del sprint.
- **Fecha límite:** las subtareas que bloquean a otras vencen a mitad de sprint; el resto, al final.
- **Responsable:** quien toma la tarea pone su nombre. **Estado:** ⬜ pendiente · 🟨 en curso · ✅ terminada.

## Resumen

| Sprint | Subtareas | Peso total | Por área |
|---|---|---|---|
| S0 | 7 | 26 | Back 3, Repo 2, Infra 2 |
| S1 | 25 | 134 | Back 20, Front 4, Infra 1 |
| S2 | 22 | 134 | Back 19, Front 3 |
| S3 | 16 | 112 | Back 13, Front 3 |
| S4 | 24 | 100 | Back 14, Front 5, n8n 4, Infra 1 |
| Cierre | 6 | 26 | Docs 4, Back + Front 1, Infra 1 |
| Continuo | 2 | 15 | Back 2 |
| Si sobra | 1 | 4 | Back 1 |
| **Total** | **103** | **551** | |

S1 y S2 son los sprints más cargados (peso 134 cada uno): en S1 se arma toda la base, y en S2 el circuito de Compras. Si al cierre de un sprint hay atraso, se mueven primero las subtareas de importancia 1.

## S0 — Fundamentos (28/09 → 04/10)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T02.1 | Paquetes de los 8 módulos y test de arquitectura | Back | 2 | 3 | 6 | 04/10 | — | — | ⬜ |
| T02.2 | Migraciones iniciales y validación de Hibernate | Back | 2 | 3 | 6 | 04/10 | T01.3 | — | ⬜ |
| T02.3 | Tests con Testcontainers | Back | 2 | 3 | 6 | 04/10 | T02.2 | — | ⬜ |
| T01.3 | Base de datos y `.env` de cada integrante | Infra | 1 | 3 | 3 | 04/10 | — | — | ⬜ |
| T01.1 | Limpiar el repo | Repo | 1 | 2 | 2 | 04/10 | — | — | ⬜ |
| T01.2 | Plantilla de PR y protección de `main` | Repo | 1 | 2 | 2 | 04/10 | — | — | ⬜ |
| T01.4 | Fijar la versión de Postgres | Infra | 1 | 1 | 1 | 04/10 | — | — | ⬜ |

## S1 — Base del dominio (05/10 → 18/10)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T03.1 | Módulo `compartido` | Back | 2 | 3 | 6 | 11/10 | T02.1 | — | ⬜ |
| T03.2 | Formato de errores y manejador global | Back | 2 | 3 | 6 | 11/10 | T03.1 | — | ⬜ |
| T04.1 | Schema y dominio de `auth` | Back | 2 | 3 | 6 | 11/10 | T02.2, T03.1 | — | ⬜ |
| T06.1 | Schema y dominio del catálogo | Back | 2 | 3 | 6 | 11/10 | T02.2 | — | ⬜ |
| T06.2 | Repositories del catálogo | Back | 2 | 3 | 6 | 11/10 | T06.1 | — | ⬜ |
| T07.1 | Schema y dominio de solicitudes | Back | 2 | 3 | 6 | 11/10 | T02.2, T03.1 | — | ⬜ |
| T07.3 | Definir los eventos del sistema | Back | 2 | 3 | 6 | 11/10 | T03.1 | — | ⬜ |
| T17.1 | Estructura, rutas y layout | Front | 2 | 3 | 6 | 11/10 | — | — | ⬜ |
| T17.2 | Cliente HTTP | Front | 2 | 3 | 6 | 11/10 | — | — | ⬜ |
| T04.2 | Repositories de `auth` | Back | 1 | 3 | 3 | 11/10 | T04.1 | — | ⬜ |
| T07.2 | Repositories de solicitudes | Back | 1 | 3 | 3 | 11/10 | T07.1 | — | ⬜ |
| T03.3 | Respuesta paginada estándar | Back | 1 | 2 | 2 | 11/10 | T03.1 | — | ⬜ |
| T04.4 | Login con JWT y configuración de seguridad | Back | 4 | 3 | 12 | 18/10 | T04.2, T03.2 | — | ⬜ |
| T07.4 | Crear una solicitud | Back | 3 | 3 | 9 | 18/10 | T07.2, T07.3, T06.5, T04.5 | — | ⬜ |
| T17.3 | Login, usuario actual y rutas protegidas | Front | 3 | 3 | 9 | 18/10 | T17.1, T17.2, T04.4 | — | ⬜ |
| T04.5 | Usuario logueado y `AuthApi` | Back | 2 | 3 | 6 | 18/10 | T04.2, T04.4 | — | ⬜ |
| T06.3 | ABM de proveedores | Back | 2 | 3 | 6 | 18/10 | T06.2, T03.2 | — | ⬜ |
| T06.5 | `CatalogoApi` para otros módulos | Back | 2 | 3 | 6 | 18/10 | T06.2, T07.3 | — | ⬜ |
| T07.5 | Consultar solicitudes | Back | 2 | 3 | 6 | 18/10 | T07.2, T03.3 | — | ⬜ |
| T06.4 | Búsqueda de categorías | Back | 2 | 2 | 4 | 18/10 | T06.2 | — | ⬜ |
| T07.6 | Tests del módulo Solicitudes | Back | 2 | 2 | 4 | 18/10 | T07.4, T07.5 | — | ⬜ |
| T17.4 | Componentes comunes | Front | 2 | 2 | 4 | 18/10 | T17.1 | — | ⬜ |
| T03.4 | Swagger | Back | 1 | 2 | 2 | 18/10 | T02.3 | — | ⬜ |
| T03.5 | CI con GitHub Actions | Infra | 2 | 1 | 2 | 18/10 | T02.3 | — | ⬜ |
| T04.3 | Datos de prueba | Back | 1 | 2 | 2 | 18/10 | T04.4 | — | ⬜ |

## S2 — Circuito de Compras (19/10 → 01/11)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T10.3 | Aprobar un ítem | Back | 4 | 3 | 12 | 01/11 | T10.2 | — | ⬜ |
| T09.4 | Tomar y reasignar | Back | 3 | 3 | 9 | 01/11 | T09.3 | — | ⬜ |
| T18.1 | Nueva solicitud | Front | 3 | 3 | 9 | 01/11 | T17.3, T07.4, T06.4 | — | ⬜ |
| T18.3 | Detalle de una solicitud | Front | 3 | 3 | 9 | 01/11 | T18.2, T08.1, T08.2, T08.3 | — | ⬜ |
| T05.1 | Permisos por rol en todos los endpoints | Back | 2 | 3 | 6 | 01/11 | T04.4 | — | ⬜ |
| T08.1 | Editar un ítem Pendiente | Back | 2 | 3 | 6 | 01/11 | T07.4 | — | ⬜ |
| T08.2 | Borrar (cancelar) un ítem Pendiente | Back | 2 | 3 | 6 | 01/11 | T07.4 | — | ⬜ |
| T08.3 | Cancelar la solicitud entera | Back | 2 | 3 | 6 | 01/11 | T07.4 | — | ⬜ |
| T08.4 | Sincronizar ediciones y cancelaciones con Compras | Back | 3 | 2 | 6 | 01/11 | T08.1, T08.2, T08.3, T09.2 | — | ⬜ |
| T09.3 | Bandeja | Back | 2 | 3 | 6 | 01/11 | T09.2 | — | ⬜ |
| T09.5 | Test de concurrencia | Back | 3 | 2 | 6 | 01/11 | T09.4 | — | ⬜ |
| T10.2 | Cargar y listar cotizaciones | Back | 2 | 3 | 6 | 01/11 | T10.1, T09.4, T06.5 | — | ⬜ |
| T10.4 | Rechazar un ítem | Back | 2 | 3 | 6 | 01/11 | T09.4 | — | ⬜ |
| T05.3 | ABM de usuarios | Back | 2 | 2 | 4 | 01/11 | T05.1 | — | ⬜ |
| T10.5 | Proveedores sugeridos para un ítem | Back | 1 | 3 | 3 | 01/11 | T06.7, T09.3 | — | ⬜ |
| T05.2 | ABM de sectores | Back | 1 | 2 | 2 | 01/11 | T05.1 | — | ⬜ |
| T06.6 | Confirmar una categoría Nueva | Back | 1 | 2 | 2 | 01/11 | T06.4 | — | ⬜ |
| T09.2 | Crear las gestiones al entrar una solicitud | Back | 3 | 3 | 9 | 25/10 | T09.1, T07.4 | — | ⬜ |
| T09.1 | Schema y dominio de Compras | Back | 2 | 3 | 6 | 25/10 | T02.2, T03.1 | — | ⬜ |
| T10.1 | Schema y dominio de cotizaciones | Back | 2 | 3 | 6 | 25/10 | T09.1 | — | ⬜ |
| T18.2 | Mis solicitudes | Front | 2 | 3 | 6 | 25/10 | T17.4, T07.5 | — | ⬜ |
| T06.7 | Proveedores sugeridos (con unificadas) | Back | 1 | 3 | 3 | 25/10 | T06.5 | — | ⬜ |

## S3 — Órdenes y cierre del flujo (02/11 → 15/11)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T11.2 | Generar la orden al aprobarse un ítem | Back | 3 | 3 | 9 | 08/11 | T11.1, T10.3 | — | ⬜ |
| T19.1 | Bandeja | Front | 3 | 3 | 9 | 08/11 | T09.4 | — | ⬜ |
| T08.5 | Actualizar el estado de los ítems desde Compras | Back | 2 | 3 | 6 | 08/11 | T09.4, T10.3 | — | ⬜ |
| T11.1 | Schema y dominio de órdenes | Back | 2 | 3 | 6 | 08/11 | T02.2 | — | ⬜ |
| T12.1 | Reintento de eventos pendientes | Back | 2 | 3 | 6 | 08/11 | T02.2 | — | ⬜ |
| T13.1 | Schema, dominio y repository de auditoría | Back | 2 | 3 | 6 | 08/11 | T02.2 | — | ⬜ |
| T14.1 | Servicio de almacenamiento de archivos | Back | 2 | 2 | 4 | 08/11 | T03.1 | — | ⬜ |
| T08.6 | Estado automático de la solicitud | Back | 4 | 3 | 12 | 15/11 | T08.5, T11.4 | — | ⬜ |
| T19.2 | Gestión de un ítem | Front | 4 | 3 | 12 | 15/11 | T10.3, T10.4, T10.5 | — | ⬜ |
| T13.2 | Registrar los eventos en el historial | Back | 3 | 3 | 9 | 15/11 | T13.1, T07.3 | — | ⬜ |
| T11.4 | Marcar una orden como enviada | Back | 2 | 3 | 6 | 15/11 | T11.2 | — | ⬜ |
| T12.2 | Listeners idempotentes | Back | 3 | 2 | 6 | 15/11 | T11.2, T13.2 | — | ⬜ |
| T12.3 | Tests entre módulos | Back | 3 | 2 | 6 | 15/11 | T11.2, T08.5 | — | ⬜ |
| T14.2 | Adjuntos de un ítem | Back | 3 | 2 | 6 | 15/11 | T14.1, T07.1 | — | ⬜ |
| T19.3 | Órdenes | Front | 2 | 3 | 6 | 15/11 | T11.4 | — | ⬜ |
| T11.3 | Listar órdenes | Back | 1 | 3 | 3 | 15/11 | T11.1 | — | ⬜ |

## S4 — Avisos y reportes (16/11 → 29/11)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T15.5 | Endpoints de recordatorios | Back | 3 | 2 | 6 | 22/11 | T09.4 | — | ⬜ |
| T16.2 | Alimentar los reportes con eventos | Back | 3 | 2 | 6 | 22/11 | T16.1, T11.2 | — | ⬜ |
| T13.3 | Consultar el historial | Back | 2 | 2 | 4 | 22/11 | T13.2 | — | ⬜ |
| T14.3 | Adjuntos de una cotización | Back | 2 | 2 | 4 | 22/11 | T14.2, T10.1 | — | ⬜ |
| T15.1 | n8n y Mailpit en Docker | Infra | 2 | 2 | 4 | 22/11 | T01.3 | — | ⬜ |
| T15.2 | Enviar los avisos a n8n | Back | 2 | 2 | 4 | 22/11 | T15.1, T08.6 | — | ⬜ |
| T16.1 | Schema y dominio de reportes | Back | 2 | 2 | 4 | 22/11 | T02.2 | — | ⬜ |
| T15.4 | Workflow: resultado al solicitante | n8n | 3 | 3 | 9 | 29/11 | T15.2 | — | ⬜ |
| T15.6 | Workflow: recordatorios diarios | n8n | 3 | 2 | 6 | 29/11 | T15.5 | — | ⬜ |
| T16.3 | Reporte de gasto | Back | 3 | 2 | 6 | 29/11 | T16.2 | — | ⬜ |
| T19.4 | Catálogo (proveedores y categorías) | Front | 3 | 2 | 6 | 29/11 | T06.6, T06.8 | — | ⬜ |
| T20.2 | Reportes | Front | 3 | 2 | 6 | 29/11 | T16.3, T16.4 | — | ⬜ |
| T15.3 | Workflow: aviso de solicitud nueva | n8n | 2 | 2 | 4 | 29/11 | T15.2 | — | ⬜ |
| T16.4 | Tiempo de resolución | Back | 2 | 2 | 4 | 29/11 | T16.2 | — | ⬜ |
| T20.1 | Administración de usuarios y sectores | Front | 2 | 2 | 4 | 29/11 | T05.3 | — | ⬜ |
| T20.3 | Adjuntos | Front | 2 | 2 | 4 | 29/11 | T14.2, T14.3 | — | ⬜ |
| T05.4 | Reglas del último Encargado y quitar el rol | Back | 3 | 1 | 3 | 29/11 | T05.3, T09.4 | — | ⬜ |
| T06.8 | Unificar categorías duplicadas | Back | 3 | 1 | 3 | 29/11 | T06.6 | — | ⬜ |
| T11.5 | Orden en PDF | Back | 3 | 1 | 3 | 29/11 | T11.2 | — | ⬜ |
| T08.7 | Evitar duplicados por doble click | Back | 2 | 1 | 2 | 29/11 | T07.4 | — | ⬜ |
| T10.6 | Marcar cotizaciones vencidas en la bandeja | Back | 2 | 1 | 2 | 29/11 | T10.2 | — | ⬜ |
| T15.7 | Seguridad de la integración | Back | 2 | 1 | 2 | 29/11 | T15.5 | — | ⬜ |
| T15.8 | Exportar los workflows al repo | n8n | 1 | 2 | 2 | 29/11 | T15.3, T15.4, T15.6 | — | ⬜ |
| T20.4 | Historial | Front | 1 | 2 | 2 | 29/11 | T13.3 | — | ⬜ |

## Cierre (30/11 → 04/12)

| ID | Subtarea | Área | D | I | Peso | Fecha límite | Depende de | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|---|
| T21.3 | Prueba completa (ejemplo de Laura y casos borde) | Back + Front | 3 | 3 | 9 | 02/12 | Todo S3 | — | ⬜ |
| T22.4 | Datos y guion de la demo | Docs | 2 | 3 | 6 | 03/12 | T21.3 | — | ⬜ |
| T22.1 | Actualizar el diseño | Docs | 2 | 2 | 4 | 03/12 | — | — | ⬜ |
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
