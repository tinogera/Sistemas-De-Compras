# Planificación del proyecto

Fuentes: `Docs/enunciado.md` (qué hay que hacer) y `Docs/diseno-del-sistema.md` (cómo está diseñado).

## Los tres niveles

| Nivel | Archivo | Qué tiene |
|---|---|---|
| **1 — Tareas generales** | `01-tareas-generales.md` | Las 25 tareas grandes del proyecto (T01 a T25). Cada una explica en texto qué hay que configurar y crear (dominio, repository, service, endpoints, schema, pantallas), sin código, y con links a la documentación. |
| **2 — Tareas específicas** | `02-tareas-especificas.md` | Cada tarea general dividida en subtareas (T07.1, T07.2...). Detalla qué campos lleva cada entidad, qué métodos cada repository, qué endpoints tiene que cumplir el service, qué reglas del enunciado aplica y dónde está la documentación. |
| **3 — Área, dificultad y fechas** | `03-area-dificultad-fechas.md` | Tabla de todas las subtareas con su área (Back, Front, Infra, n8n, Docs), dificultad, importancia, peso, fecha límite, dependencias, responsable y estado. Es el tablero para seguir el avance. |
| Anexos | `guias/` | Guías paso a paso con código ya probado. Hoy: `guias/sprint-0-paso-a-paso.md` (T01 y T02). |

Para trabajar: elegir una subtarea en el Nivel 3, leer su detalle en el Nivel 2, y si hace falta contexto leer la tarea general en el Nivel 1.

## Calendario

- **Equipo:** 2 o 3 personas. No hay roles fijos: todos trabajan en backend, frontend e infraestructura.
- **Entrega final:** principios de diciembre. Se planifica para el **viernes 4/12/2026**. ⚠️ Confirmar con la cátedra.

| Sprint | Fechas | Mitad de sprint | Qué tiene que funcionar al final |
|---|---|---|---|
| **S0** — Fundamentos | 28/09 → 04/10 | — | Todos corren back, front y tests en su máquina |
| **S1** — Base del dominio | 05/10 → 18/10 | 11/10 | Un solicitante se loguea y crea una solicitud (con precio estimado) desde la API; el motor de reglas de aprobación existe y está testeado |
| **S2** — Compras y reglas de aprobación | 19/10 → 01/11 | 25/10 | Toda solicitud entra a la bandeja y a la vez arma su cadena de aprobación según las reglas; el Encargado cotiza, fija el precio total o rechaza; el Admin gestiona reglas, usuarios y sectores por la API; el solicitante tiene sus pantallas |
| **S3** — Aprobaciones, compra y órdenes | 02/11 → 15/11 | 08/11 | El ejemplo de Laura funciona de punta a punta desde la interfaz, pasando por Supervisor y Gerencia General, la compra y la orden |
| **S4** — Tiempo real, avisos y reportes | 16/11 → 29/11 | 22/11 | Todo terminado (feature freeze): precio en tiempo real, reglas desde la pantalla, avisos y reportes |
| **Cierre** | 30/11 → 04/12 | — | Pruebas completas, documentación y demo |

Las subtareas que bloquean a otras (entidades, schemas, cliente del front) vencen a **mitad de sprint**; el resto, al final.

## Alcance

| Se implementa | Se simplifica | Solo se documenta |
|---|---|---|
| Todo el flujo: solicitud → bandeja del Encargado (cotización, precio total, rechazo) **y en paralelo** cadena de aprobación por reglas (Supervisor, Gerencia General...) → compra → orden → aviso | **Outbox manual** → registro de eventos de Spring Modulith (`event_publication`), que cumple la misma función | Escalado (§8 del diseño) |
| Login con roles (Solicitante, Encargado, Supervisor, Admin) | **Adjuntos** → carpeta en el servidor con subida directa, en vez de S3 con URL prefirmada | Failover, backups, deploy gradual (§9) |
| Catálogo, auditoría, reportes, avisos con n8n | **n8n** → corre local en Docker con un servidor de mail de prueba (Mailpit) | Prometheus/Grafana, ShedLock |

Si se atrasa un sprint, se recortan primero las subtareas de importancia 1.

**Requisito agregado el 08/10:** reglas de aprobación configurables por el Admin (módulo `aprobaciones`, T23 a T25, más cambios en casi todas las tareas de negocio). Pisa al enunciado original, que decía que no había aprobaciones por monto. El detalle está en las decisiones 7 a 13 de `02-tareas-especificas.md`, y el efecto en las fechas, al principio de `03-area-dificultad-fechas.md`.

## Cómo tomar una tarea

1. Elegir una subtarea en `03-area-dificultad-fechas.md` que no tenga responsable y cuyas dependencias estén terminadas. Conviene empezar por las de peso alto.
2. Avisar al grupo, poner tu nombre en "Responsable" y el estado en 🟨.
3. Crear la rama desde `main` actualizado: `feature/T07.4-crear-solicitud`.
4. Commits con el ID adelante: `T07.4: endpoint para crear solicitud`.
5. Merge directo a `main` (sin PR). En el último commit poner `Closes #<n>` para que GitHub cierre el issue solo. Al mergear, estado ✅.

Una tarea en curso por persona. Si alguien hace el backend de una funcionalidad, conviene que otra persona haga su pantalla, así todos conocen las dos partes.

## Convenciones de código (valen para todas las tareas)

### Backend: estructura de cada módulo

Cada módulo es un paquete dentro de `back/src/main/java/com/Sistem/Solicitude_Compra/`:

```
<modulo>/
├── package-info.java      declara el módulo para Spring Modulith
├── <Evento>.java          eventos que publica el módulo (públicos: otros módulos los escuchan)
├── <Modulo>Api.java       métodos que otros módulos pueden llamar (si hace falta)
├── dominio/               entidades JPA y enums
├── repositorio/           interfaces Spring Data JPA
├── servicio/              lógica de negocio y reglas; publica eventos
└── web/                   controllers REST y DTOs (records de entrada y salida)
```

- Solo lo que está en la raíz del paquete es visible para otros módulos. `dominio`, `repositorio`, `servicio` y `web` son internos, y `ModularityTests` falla si otro módulo los usa.
- Los módulos se comunican con **eventos** (`@ApplicationModuleListener`), no llamándose entre sí. La excepción son las consultas simples a otro módulo, a través de su `<Modulo>Api`.
- Las entidades nunca salen por la API: los controllers devuelven DTOs.
- Los endpoints van bajo `/api/v1/...` (diseño §7).

### Backend: migraciones Flyway

- Van en `back/src/main/resources/db/migration/`. Cada tabla nueva se crea con una migración.
- Para que dos personas no usen el mismo número de versión, el nombre lleva la fecha: `V20261005_1__auth_tablas.sql` (año, mes, día, número del día).
- Una migración ya mergeada **nunca se edita**. Si hay que cambiar algo, se hace una nueva.

### Frontend: estructura

Dentro de `frontend/src/`:

```
api/          cliente HTTP y una función por endpoint (solicitudes.js, compras.js...)
auth/         contexto de usuario, login y rutas protegidas
components/   componentes reutilizables (tabla paginada, mensajes de error, badges de estado)
pages/        una carpeta por área: solicitante/, encargado/, admin/, reportes/
```

- Siempre se llama a rutas relativas `/api/v1/...`: el proxy de Vite las manda al backend.

## Ciclo de refinamiento (al cierre de cada sprint)

1. Actualizar estados en `03-area-dificultad-fechas.md` y `Claude/contexto/estado.md`.
2. Pasar lo no terminado al sprint siguiente. Si no entra, recortar lo de importancia 1.
3. Releer en el enunciado y el diseño las secciones que usa el próximo sprint. Si aparecen subtareas nuevas o una resulta más grande, agregarla o dividirla en los niveles 2 y 3.
4. Si conviene, escribir una guía paso a paso con código probado en `guias/` para las subtareas difíciles del próximo sprint.
