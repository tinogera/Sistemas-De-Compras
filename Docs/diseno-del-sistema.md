# Sistema de Gestión de Solicitudes de Compra

**Trabajo práctico · Diseño de sistema**

- **Autor:** Santino
- **Fecha:** Septiembre 2026
- **Stack:** Java · Spring Boot · PostgreSQL · n8n

| Estimación del escenario base | |
|---|---|
| ~6 QPS | pico en horario laboral |
| 1.500 | ítems por mes |
| ~36 GB | de adjuntos por año |
| 99,5 % | disponibilidad objetivo |

Documento de diseño técnico basado en el Enunciado v3. Sigue la estructura clásica de un ejercicio de *system design*: requisitos funcionales, no funcionales, estimación, arquitectura de alto nivel y, en profundidad, esquema de base de datos, APIs, escalado, manejo de fallas y casos borde.

## Contenido

1. [Requisitos funcionales](#1-requisitos-funcionales)
2. [Requisitos no funcionales](#2-requisitos-no-funcionales)
3. [Estimación](#3-estimación)
4. [Arquitectura de alto nivel](#4-arquitectura-de-alto-nivel)
5. [Tecnologías](#5-tecnologías)
6. [Esquema de base de datos (PostgreSQL)](#6-esquema-de-base-de-datos-postgresql)
7. [APIs](#7-apis)
8. [Estrategias de escalado](#8-estrategias-de-escalado)
9. [Manejo de fallas](#9-manejo-de-fallas)
10. [Casos borde](#10-casos-borde)

---

## 1. Requisitos funcionales

**Solicitudes**

- **RF1.** Un empleado autenticado crea una solicitud con uno o más ítems (producto o servicio), urgencia y fecha necesaria.
- **RF2.** El solicitante adjunta especificaciones (PDF o imagen) a cada ítem.
- **RF3.** El solicitante edita o quita un ítem mientras esté Pendiente, y cancela la solicitud completa si todos sus ítems siguen Pendientes.
- **RF4.** El solicitante consulta sus solicitudes y el estado de cada ítem.

**Compras**

- **RF5.** Los Encargados ven una bandeja de ítems sin resolver, ordenada por urgencia y fecha necesaria.
- **RF6.** Un Encargado toma un ítem (queda asignado a él) o se reasigna uno de otro Encargado.
- **RF7.** El Encargado consulta los proveedores activos de la categoría del ítem y da de alta proveedores nuevos.
- **RF8.** El Encargado carga cotizaciones (proveedor, moneda, precio unitario, validez, presupuesto adjunto).
- **RF9.** El Encargado aprueba un ítem seleccionando una cotización vigente, o lo rechaza con motivo.

**Órdenes**

- **RF10.** Al aprobarse un ítem, el sistema genera automáticamente su orden de pedido con número correlativo.
- **RF11.** El Encargado marca la orden como Enviada al proveedor.

**Catálogo**

- **RF12.** Gestión de proveedores, categorías y su relación N:M; promoción de categorías Nuevas a Conocidas y unificación de duplicados.

**Notificaciones y reportes**

- **RF13.** Aviso a los Encargados cuando entra una solicitud.
- **RF14.** Aviso al solicitante cuando su solicitud queda Lista, con el resultado de cada ítem.
- **RF15.** Recordatorios: cada 3 días para urgencia Alta, cada 3 semanas para Media y Baja.
- **RF16.** Reportes de gasto por sector, categoría y proveedor (ARS y USD por separado) y tiempo promedio de resolución.
- **RF17.** Historial auditable de todo cambio de estado, asignación y reasignación.

## 2. Requisitos no funcionales

| Atributo | Objetivo | Justificación |
|---|---|---|
| **Disponibilidad** | 99,5 % mensual (≈ 3,6 h de caída/mes), con prioridad en horario laboral | Es un sistema interno: una caída molesta pero no frena la operación (se puede comprar "a mano" unas horas). No justifica una arquitectura multi-región. |
| **Latencia** | p95 < 300 ms en la API; < 2 s en reportes | Uso interactivo por personas; los reportes pueden ser algo más lentos. |
| **Consistencia** | Fuerte para estados de ítems, cotizaciones y órdenes | No puede haber un ítem aprobado dos veces ni dos órdenes para el mismo ítem. Se prioriza consistencia sobre disponibilidad. |
| **Consistencia de reportes** | Eventual, con retraso máximo de minutos | Los reportes se alimentan por eventos; unos segundos de atraso no afectan decisiones. |
| **Durabilidad** | Ninguna pérdida de datos confirmados. RPO ≤ 15 min, RTO ≤ 4 h | Las órdenes de pedido son documentos con valor administrativo. |
| **Seguridad** | Autenticación obligatoria, autorización por rol, HTTPS, validación de adjuntos | Contiene precios, proveedores y datos internos. |
| **Auditabilidad** | 100 % de las transiciones registradas, sin posibilidad de edición | Requisito del negocio para trazabilidad del gasto. |
| **Mantenibilidad** | Módulos desacoplados, extraíbles como servicios sin reescribir lógica | Pedido explícito de "escalabilidad independiente por módulo". |

## 3. Estimación

### 3.1 Escenario base (una empresa mediana)

| Variable | Supuesto |
|---|---|
| Empleados con acceso | 300 |
| Encargados de compras | 2 a 5 |
| Solicitudes por mes | 500 |
| Ítems por solicitud (promedio) | 3 → **1.500 ítems/mes** |
| Cotizaciones por ítem | 2 → 3.000 cotizaciones/mes |
| Adjuntos por ítem (especificación + presupuestos) | 2, de ~1 MB promedio |

**Tráfico (QPS)**

- Escrituras: 500 solicitudes + 1.500 ítems + 3.000 cotizaciones + ~1.200 órdenes + ~10.000 cambios de estado ≈ 16.000/mes ≈ **800 por día hábil**.
- Lecturas (bandeja, consultas, listados): ~20 por escritura ≈ 16.000 por día hábil.
- En 8 h laborales: 16.800 req / 28.800 s ≈ **0,6 QPS promedio**. Con un pico de 10× → **~6 QPS**.

**Almacenamiento (por año)**

- Base de datos: ~200.000 filas/año × ~1 KB → **~200 MB/año** (con índices, < 500 MB).
- Historial de estados: ~120.000 filas/año × 300 B → ~36 MB/año.
- Adjuntos: 1.500 ítems × 2 × 1 MB × 12 → **~36 GB/año** en almacenamiento de objetos.

**Conclusión:** la carga es muy baja. Un servidor de aplicación y una base PostgreSQL alcanzan con amplio margen durante años. Lo único que crece de forma significativa son los adjuntos, y por eso van a un almacenamiento de objetos y no a la base.

### 3.2 Escenario de crecimiento (el sistema ofrecido como SaaS a muchas empresas)

Para que el diseño de escalado tenga sentido, se plantea también un escenario hipotético:

| Variable | Supuesto |
|---|---|
| Empresas clientes | 2.000 |
| Usuarios totales | 600.000 |
| Ítems por mes | 3.000.000 |
| Pico | ~6 QPS × 2.000 empresas → **~1.000–2.000 QPS** en horario pico |
| Base de datos | ~400 GB/año |
| Adjuntos | ~70 TB/año |

Este escenario es el que justifica las estrategias de la sección 8.

## 4. Arquitectura de alto nivel

```
                    ┌──────────────────────────┐
  Navegador ───────►│   Frontend (SPA React)   │
                    └────────────┬─────────────┘
                                 │ HTTPS / JSON
                    ┌────────────▼─────────────┐
                    │  Reverse proxy / LB      │  (Nginx o balanceador cloud)
                    │  TLS, rate limiting      │
                    └────────────┬─────────────┘
                                 │
        ┌────────────────────────▼────────────────────────────┐
        │          Backend Spring Boot (monolito modular)     │
        │                                                     │
        │  ┌────────────┐ ┌────────┐ ┌────────┐ ┌─────────┐  │
        │  │Solicitudes │ │Catálogo│ │Compras │ │ Órdenes │  │
        │  └─────┬──────┘ └───┬────┘ └───┬────┘ └────┬────┘  │
        │        │   eventos de dominio (Spring + outbox)     │
        │  ┌─────┴──────┐ ┌──────────────┐ ┌──────────────┐  │
        │  │  Reportes  │ │Integraciones │ │ Auth / Users │  │
        │  └────────────┘ └──────┬───────┘ └──────────────┘  │
        └───────┬─────────────────┼───────────────┬──────────┘
                │                 │               │
     ┌──────────▼───────┐   ┌─────▼─────┐   ┌─────▼──────────────┐
     │  PostgreSQL      │   │   n8n     │   │ Almacenamiento de  │
     │  (un schema por  │   │ (webhooks,│   │ objetos (S3/MinIO) │
     │   módulo)        │   │  cron)    │   │  adjuntos          │
     └──────────────────┘   └─────┬─────┘   └────────────────────┘
                                  │
                           ┌──────▼──────┐
                           │ SMTP / Slack │
                           └─────────────┘
```

**Decisiones clave**

1. **Monolito modular**: un solo deployable, un schema de base de datos por módulo, comunicación por eventos. Es lo adecuado para ~6 QPS y deja abierto el camino para extraer módulos (sección 8).
2. **Patrón Outbox para los eventos**: cada módulo escribe sus eventos en una tabla `outbox` dentro de la misma transacción que el cambio de negocio, y un proceso los publica después. Un evento nunca se pierde ni se publica sin que el cambio se haya guardado.
3. **Adjuntos fuera de la base**: el navegador los sube directo al almacenamiento de objetos con URLs prefirmadas; la base guarda solo metadatos.
4. **n8n afuera**: n8n no guarda estado de negocio. Recibe webhooks del backend y consulta la API para los recordatorios.
5. **Backend sin estado**: la sesión viaja en un token JWT, así se pueden agregar instancias detrás del balanceador sin cambios.

## 5. Tecnologías

| Capa | Tecnología | Para qué |
|---|---|---|
| Lenguaje y framework | Java 25 + Spring Boot 4.1 | Backend único (monolito modular) |
| Módulos | Spring Modulith | Fronteras entre módulos, eventos de dominio y tests de arquitectura |
| Persistencia | Spring Data JPA + PostgreSQL 16 | Base relacional única, un schema por módulo |
| Migraciones | Flyway | Versionado del esquema, migraciones compatibles hacia atrás |
| Seguridad | Spring Security + JWT | Login, roles (Solicitante, Encargado, Administrador) |
| Eventos | Spring Events + patrón Outbox | Comunicación confiable entre módulos; reemplazable por RabbitMQ o Kafka |
| Jobs programados | Spring Scheduler + ShedLock | Publicador del outbox, limpieza de adjuntos, sin ejecución duplicada |
| Automatización | n8n | Mails de aviso, notificación de resultado y recordatorios |
| Archivos | MinIO (compatible S3) o S3 | Adjuntos: especificaciones y presupuestos |
| Frontend | React | Formularios de solicitud, bandeja del Encargado, reportes |
| Caché (opcional) | Caffeine / Redis | Categorías y proveedores por categoría |
| Infraestructura | Docker + Nginx | Contenedores y reverse proxy con TLS |
| Observabilidad | Spring Actuator + Prometheus + Grafana | Health checks, métricas y alertas |

## 6. Esquema de base de datos (PostgreSQL)

Un schema por módulo. Las referencias entre módulos se guardan como IDs **sin foreign key física**, para que cada módulo pueda mudarse a su propia base en el futuro. Dentro de un mismo módulo sí hay FKs.

### 6.1 Schema `auth`

```sql
CREATE TABLE auth.sector (
  id      BIGSERIAL PRIMARY KEY,
  nombre  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE auth.usuario (
  id         BIGSERIAL PRIMARY KEY,
  email      VARCHAR(255) NOT NULL UNIQUE,
  nombre     VARCHAR(150) NOT NULL,
  sector_id  BIGINT NOT NULL REFERENCES auth.sector(id),
  rol        VARCHAR(20) NOT NULL CHECK (rol IN ('SOLICITANTE','ENCARGADO','ADMIN')),
  activo     BOOLEAN NOT NULL DEFAULT TRUE
);
```

### 6.2 Schema `solicitudes`

```sql
CREATE TABLE solicitudes.solicitud (
  id               BIGSERIAL PRIMARY KEY,
  solicitante_id   BIGINT NOT NULL,              -- ref. auth.usuario
  sector_id        BIGINT NOT NULL,              -- copiado al crear (el usuario puede cambiar de sector)
  urgencia         VARCHAR(5) NOT NULL CHECK (urgencia IN ('BAJA','MEDIA','ALTA')),
  fecha_necesaria  DATE NOT NULL,
  observaciones    TEXT,
  estado           VARCHAR(15) NOT NULL DEFAULT 'EN_REVISION'
                   CHECK (estado IN ('EN_REVISION','LISTA','CERRADA','CANCELADA')),
  creada_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
  version          INT NOT NULL DEFAULT 0         -- bloqueo optimista
);
CREATE INDEX ix_solicitud_solicitante ON solicitudes.solicitud (solicitante_id, creada_en DESC);

CREATE TABLE solicitudes.item (
  id            BIGSERIAL PRIMARY KEY,
  solicitud_id  BIGINT NOT NULL REFERENCES solicitudes.solicitud(id),
  tipo          VARCHAR(10) NOT NULL CHECK (tipo IN ('PRODUCTO','SERVICIO')),
  categoria_id  BIGINT NOT NULL,                 -- ref. catalogo.categoria
  detalle       TEXT,
  cantidad      NUMERIC(12,2) NOT NULL CHECK (cantidad > 0),
  estado        VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE'
                CHECK (estado IN ('PENDIENTE','EN_COTIZACION','APROBADO','RECHAZADO','CANCELADO')),
  creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
  version       INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_item_solicitud ON solicitudes.item (solicitud_id);

CREATE TABLE solicitudes.adjunto_item (
  id            BIGSERIAL PRIMARY KEY,
  item_id       BIGINT NOT NULL REFERENCES solicitudes.item(id),
  clave_objeto  VARCHAR(500) NOT NULL,           -- ruta en el almacenamiento de objetos
  nombre        VARCHAR(255) NOT NULL,
  mime_type     VARCHAR(50) NOT NULL
                CHECK (mime_type IN ('application/pdf','image/jpeg','image/png')),
  tamano_bytes  BIGINT NOT NULL CHECK (tamano_bytes <= 10485760),
  subido_por    BIGINT NOT NULL,
  subido_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 6.3 Schema `catalogo`

```sql
CREATE TABLE catalogo.categoria (
  id            BIGSERIAL PRIMARY KEY,
  nombre        VARCHAR(120) NOT NULL,
  tipo          VARCHAR(10) NOT NULL CHECK (tipo IN ('PRODUCTO','SERVICIO')),
  estado        VARCHAR(10) NOT NULL DEFAULT 'NUEVA'
                CHECK (estado IN ('NUEVA','CONOCIDA','UNIFICADA')),
  unificada_en  BIGINT REFERENCES catalogo.categoria(id)   -- si era un duplicado
);
-- Nombre único por tipo, sin distinguir mayúsculas
CREATE UNIQUE INDEX ux_categoria_nombre ON catalogo.categoria (tipo, lower(nombre));

CREATE TABLE catalogo.proveedor (
  id            BIGSERIAL PRIMARY KEY,
  razon_social  VARCHAR(200) NOT NULL,
  cuit          VARCHAR(13) UNIQUE,
  telefono      VARCHAR(50),
  email         VARCHAR(255),
  contacto      VARCHAR(150),
  activo        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE catalogo.proveedor_categoria (
  proveedor_id  BIGINT NOT NULL REFERENCES catalogo.proveedor(id),
  categoria_id  BIGINT NOT NULL REFERENCES catalogo.categoria(id),
  PRIMARY KEY (proveedor_id, categoria_id)
);
CREATE INDEX ix_provcat_categoria ON catalogo.proveedor_categoria (categoria_id);
```

### 6.4 Schema `compras`

Compras es dueño del trabajo sobre el ítem (asignación, cotizaciones, decisión). El estado del ítem que ve el solicitante lo actualiza el módulo Solicitudes al recibir los eventos.

```sql
CREATE TABLE compras.gestion_item (
  item_id                 BIGINT PRIMARY KEY,    -- ref. solicitudes.item
  solicitud_id            BIGINT NOT NULL,
  categoria_id            BIGINT NOT NULL,
  cantidad                NUMERIC(12,2) NOT NULL,
  urgencia                VARCHAR(5) NOT NULL,   -- copiado para ordenar la bandeja sin consultar otro módulo
  fecha_necesaria         DATE NOT NULL,
  estado                  VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
  encargado_id            BIGINT,
  cotizacion_elegida_id   BIGINT,
  motivo_rechazo          TEXT,
  resuelto_en             TIMESTAMPTZ,
  ultimo_recordatorio_en  TIMESTAMPTZ,
  version                 INT NOT NULL DEFAULT 0,
  CHECK (estado <> 'APROBADO'  OR cotizacion_elegida_id IS NOT NULL),
  CHECK (estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL)
);
-- Bandeja: ítems sin resolver ordenados por urgencia y fecha necesaria
CREATE INDEX ix_bandeja ON compras.gestion_item (estado, urgencia, fecha_necesaria)
  WHERE estado IN ('PENDIENTE','EN_COTIZACION');

CREATE TABLE compras.cotizacion (
  id                BIGSERIAL PRIMARY KEY,
  item_id           BIGINT NOT NULL REFERENCES compras.gestion_item(item_id),
  proveedor_id      BIGINT NOT NULL,             -- ref. catalogo.proveedor
  moneda            CHAR(3) NOT NULL CHECK (moneda IN ('ARS','USD')),
  precio_unitario   NUMERIC(14,2) NOT NULL CHECK (precio_unitario > 0),
  precio_total      NUMERIC(16,2) NOT NULL,
  fecha_validez     DATE NOT NULL,
  entrega_estimada  DATE,
  observaciones     TEXT,
  estado            VARCHAR(12) NOT NULL DEFAULT 'CANDIDATA'
                    CHECK (estado IN ('CANDIDATA','SELECCIONADA','DESCARTADA','VENCIDA')),
  cargada_por       BIGINT NOT NULL,
  cargada_en        TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Un ítem nunca puede tener dos cotizaciones seleccionadas
CREATE UNIQUE INDEX ux_una_seleccionada ON compras.cotizacion (item_id) WHERE estado = 'SELECCIONADA';

CREATE TABLE compras.adjunto_cotizacion (
  id             BIGSERIAL PRIMARY KEY,
  cotizacion_id  BIGINT NOT NULL REFERENCES compras.cotizacion(id),
  clave_objeto   VARCHAR(500) NOT NULL,
  nombre         VARCHAR(255) NOT NULL,
  mime_type      VARCHAR(50) NOT NULL,
  tamano_bytes   BIGINT NOT NULL,
  subido_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 6.5 Schema `ordenes`

```sql
CREATE SEQUENCE ordenes.numero_orden_seq;

CREATE TABLE ordenes.orden_pedido (
  id               BIGSERIAL PRIMARY KEY,
  numero           BIGINT NOT NULL UNIQUE DEFAULT nextval('ordenes.numero_orden_seq'),
  item_id          BIGINT NOT NULL UNIQUE,       -- una sola orden por ítem (idempotencia)
  solicitud_id     BIGINT NOT NULL,
  proveedor_id     BIGINT NOT NULL,
  proveedor_razon_social VARCHAR(200) NOT NULL,  -- copia: la orden no cambia si se edita el proveedor
  cotizacion_id    BIGINT NOT NULL,
  cantidad         NUMERIC(12,2) NOT NULL,
  moneda           CHAR(3) NOT NULL,
  precio_unitario  NUMERIC(14,2) NOT NULL,
  precio_total     NUMERIC(16,2) NOT NULL,
  estado           VARCHAR(10) NOT NULL DEFAULT 'GENERADA' CHECK (estado IN ('GENERADA','ENVIADA')),
  emitida_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
  enviada_por      BIGINT,
  enviada_en       TIMESTAMPTZ
);
```

Los datos del proveedor y el precio se **copian** en la orden: es un documento y no debe cambiar si después se edita el proveedor.

### 6.6 Schema `reportes` (modelo de lectura)

```sql
CREATE TABLE reportes.hecho_gasto (
  orden_id      BIGINT PRIMARY KEY,
  fecha         DATE NOT NULL,
  sector_id     BIGINT NOT NULL,
  categoria_id  BIGINT NOT NULL,
  proveedor_id  BIGINT NOT NULL,
  moneda        CHAR(3) NOT NULL,
  importe       NUMERIC(16,2) NOT NULL
);
CREATE INDEX ix_gasto_fecha ON reportes.hecho_gasto (fecha, moneda);

CREATE TABLE reportes.hecho_resolucion (
  item_id      BIGINT PRIMARY KEY,
  urgencia     VARCHAR(5) NOT NULL,
  creado_en    TIMESTAMPTZ NOT NULL,
  resuelto_en  TIMESTAMPTZ NOT NULL,
  resultado    VARCHAR(10) NOT NULL              -- APROBADO / RECHAZADO
);
```

### 6.7 Tablas transversales

```sql
-- Una por schema de módulo (se muestra la genérica)
CREATE TABLE <modulo>.outbox (
  id            UUID PRIMARY KEY,
  tipo_evento   VARCHAR(80) NOT NULL,
  payload       JSONB NOT NULL,
  creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
  publicado_en  TIMESTAMPTZ,
  intentos      INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_pendientes ON <modulo>.outbox (creado_en) WHERE publicado_en IS NULL;

-- Eventos ya procesados por cada consumidor (idempotencia)
CREATE TABLE <modulo>.evento_procesado (
  evento_id     UUID PRIMARY KEY,
  procesado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Historial de auditoría (solo inserciones)
CREATE TABLE auditoria.historial (
  id               BIGSERIAL PRIMARY KEY,
  entidad          VARCHAR(30) NOT NULL,         -- SOLICITUD / ITEM / COTIZACION / ORDEN
  entidad_id       BIGINT NOT NULL,
  accion           VARCHAR(30) NOT NULL,         -- CAMBIO_ESTADO / ASIGNACION / REASIGNACION ...
  estado_anterior  VARCHAR(20),
  estado_nuevo     VARCHAR(20),
  usuario_id       BIGINT,
  comentario       TEXT,
  ocurrido_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_historial_entidad ON auditoria.historial (entidad, entidad_id, ocurrido_en);
```

## 7. APIs

REST sobre JSON, versionada (`/api/v1`). Autenticación por JWT en el header `Authorization`. Listados paginados con `?page=&size=`. Las creaciones aceptan el header `Idempotency-Key` para que un doble click no cree dos veces lo mismo. Los cambios de estado llevan la `version` del recurso para detectar conflictos (`409 Conflict`).

### 7.1 Solicitudes (rol Solicitante)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/solicitudes` | Crea una solicitud con sus ítems |
| GET | `/api/v1/solicitudes?mias=true&estado=` | Lista mis solicitudes |
| GET | `/api/v1/solicitudes/{id}` | Detalle con ítems y su estado |
| PATCH | `/api/v1/solicitudes/{id}/items/{itemId}` | Edita un ítem (solo si Pendiente) |
| DELETE | `/api/v1/solicitudes/{id}/items/{itemId}` | Cancela un ítem (solo si Pendiente) |
| POST | `/api/v1/solicitudes/{id}/cancelar` | Cancela la solicitud (todos los ítems Pendientes) |
| POST | `/api/v1/items/{itemId}/adjuntos/url-subida` | Pide una URL prefirmada para subir un archivo |
| POST | `/api/v1/items/{itemId}/adjuntos` | Confirma el adjunto ya subido |

Ejemplo:

```http
POST /api/v1/solicitudes
Idempotency-Key: 6f1c2e9a-...
{
  "urgencia": "ALTA",
  "fechaNecesaria": "2026-10-15",
  "observaciones": "Para el evento del día de la familia",
  "items": [
    { "tipo": "SERVICIO", "categoriaId": 42, "cantidad": 1, "detalle": "Pelotero para 30 chicos" },
    { "tipo": "PRODUCTO", "categoriaNueva": "Globos", "cantidad": 200 }
  ]
}

201 Created
{ "id": 1587, "estado": "EN_REVISION",
  "items": [ { "id": 4021, "estado": "PENDIENTE" }, { "id": 4022, "estado": "PENDIENTE" } ] }
```

### 7.2 Compras (rol Encargado)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/bandeja?asignadosAMi=&urgencia=` | Ítems sin resolver, ordenados por urgencia y fecha |
| POST | `/api/v1/bandeja/{itemId}/tomar` | Se asigna el ítem (Pendiente → En cotización) |
| POST | `/api/v1/bandeja/{itemId}/reasignar` | Toma un ítem asignado a otro Encargado |
| GET | `/api/v1/bandeja/{itemId}/proveedores-sugeridos` | Proveedores activos de la categoría |
| POST | `/api/v1/bandeja/{itemId}/cotizaciones` | Carga una cotización |
| POST | `/api/v1/cotizaciones/{id}/adjuntos` | Adjunta el presupuesto |
| POST | `/api/v1/bandeja/{itemId}/aprobar` | `{ "cotizacionId": 991, "version": 3 }` |
| POST | `/api/v1/bandeja/{itemId}/rechazar` | `{ "motivo": "Hay stock en depósito", "version": 3 }` |

Ejemplo de error:

```http
POST /api/v1/bandeja/4021/aprobar
{ "cotizacionId": 991, "version": 3 }

422 Unprocessable Entity
{ "codigo": "COTIZACION_VENCIDA", "mensaje": "La cotización venció el 2026-09-20. Cargá una nueva." }
```

### 7.3 Catálogo

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/categorias?tipo=&q=` | Búsqueda para el autocompletado del formulario |
| PATCH | `/api/v1/categorias/{id}` | Pasar a Conocida |
| POST | `/api/v1/categorias/{id}/unificar` | `{ "destinoId": 17 }` |
| GET / POST / PATCH | `/api/v1/proveedores` | ABM de proveedores y sus categorías |

### 7.4 Órdenes y reportes

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/ordenes?estado=GENERADA` | Órdenes pendientes de envío |
| GET | `/api/v1/ordenes/{id}/pdf` | Orden en PDF para mandar al proveedor |
| POST | `/api/v1/ordenes/{id}/marcar-enviada` | Generada → Enviada |
| GET | `/api/v1/reportes/gasto?agrupar=sector\|categoria\|proveedor&desde=&hasta=` | Totales separados por moneda |
| GET | `/api/v1/reportes/tiempos-resolucion?desde=&hasta=` | Promedio general y por urgencia |

### 7.5 Integración con n8n

| Dirección | Endpoint | Uso |
|---|---|---|
| Backend → n8n | `POST {n8n}/webhook/solicitud-creada` | Aviso a los Encargados |
| Backend → n8n | `POST {n8n}/webhook/solicitud-lista` | Mail al solicitante con el resultado |
| n8n → Backend | `GET /api/v1/integraciones/recordatorios-pendientes` | Ítems cuyo recordatorio corresponde hoy |
| n8n → Backend | `POST /api/v1/integraciones/recordatorios/{itemId}/enviado` | Registra el envío para no repetirlo |

n8n usa una API key propia con permisos limitados a `/integraciones`. Los webhooks salientes van firmados (HMAC) para que n8n verifique que vienen del backend.

## 8. Estrategias de escalado

En el escenario base nada de esto hace falta: se aplica en orden, solo cuando una métrica real lo pida.

**Paso 0 — Escenario base (hasta ~50 QPS)**

- 1 PostgreSQL gestionado con backups automáticos.
- 2 instancias de backend detrás del balanceador, más por disponibilidad (deploys sin corte) que por carga.

**Paso 1 — Escalado horizontal de la aplicación**

- El backend es stateless (JWT, adjuntos en almacenamiento externo), así que se agregan instancias.
- El publicador del outbox y los jobs programados corren en una sola instancia a la vez, con un lock distribuido (ShedLock sobre la misma base), para no procesar dos veces.

**Paso 2 — Descargar la base de datos**

- **Réplica de lectura** para Reportes y listados pesados.
- **Caché** (Caffeine en memoria o Redis) para datos que cambian poco: categorías, proveedores por categoría, usuarios. Se invalida por evento (`CategoriaCreada`, etc.).
- **Pool de conexiones** dimensionado (HikariCP) y PgBouncer si hay muchas instancias.

**Paso 3 — Crecimiento de datos**

- **Particionado por fecha** de `auditoria.historial`, `outbox` y `reportes.hecho_*` (particiones mensuales, las viejas se archivan).
- Purga periódica del outbox ya publicado (más de 7 días).
- Adjuntos antiguos a una clase de almacenamiento más barata (reglas de ciclo de vida del bucket).

**Paso 4 — Escalado independiente por módulo (escenario SaaS)**

- Reemplazar el bus de eventos interno por un broker (RabbitMQ o Kafka): el outbox publica al broker en lugar de a Spring.
- Extraer primero los módulos con carga distinta: **Reportes** (consultas pesadas, se beneficia de una base analítica propia) e **Integraciones/Notificaciones** (picos por envíos masivos).
- Como cada módulo ya tiene su schema y no hay FKs entre módulos, mover un schema a otra base es una tarea de infraestructura, no de rediseño.

**Paso 5 — Multi-empresa (escenario SaaS)**

- Columna `empresa_id` en todas las tablas, incluida en los índices y filtrada con Row Level Security de PostgreSQL.
- Si una base no alcanza: *sharding* por `empresa_id` (cada empresa vive completa en un shard, así ninguna consulta cruza shards).
- Rate limiting por empresa para que un cliente no degrade a los demás.

## 9. Manejo de fallas

| Falla | Qué pasa | Cómo se maneja |
|---|---|---|
| **Se cae una instancia del backend** | Fallan las requests en curso | El balanceador la saca por health check (`/actuator/health`); con 2+ instancias no hay corte. El frontend reintenta las lecturas. |
| **Falla a mitad de una operación** (se aprueba el ítem pero se cae antes de generar la orden) | Ítem aprobado sin orden | El cambio y el evento `ItemAprobado` se guardan en la misma transacción (outbox). Al volver, el publicador envía el evento pendiente y Órdenes genera la orden. |
| **Un evento se entrega dos veces** | Riesgo de dos órdenes para un ítem | Consumidores idempotentes: tabla `evento_procesado` y `UNIQUE(item_id)` en `orden_pedido`. El segundo intento no hace nada. |
| **Un consumidor falla siempre con el mismo evento** | Reintentos infinitos | *Backoff* exponencial; tras N intentos el evento queda como fallido y dispara una alerta para revisión manual. |
| **n8n caído** | No salen mails ni recordatorios | Los webhooks salientes pasan por el outbox y se reintentan hasta que n8n vuelva. Los recordatorios se calculan por fecha (`ultimo_recordatorio_en`): al volver se envían los atrasados, sin duplicar. |
| **Falla el SMTP** | El mail no llega | n8n reintenta; además el sistema muestra las notificaciones dentro de la aplicación, así que el mail no es el único canal. |
| **Almacenamiento de objetos no disponible** | No se suben ni se ven adjuntos | El resto del sistema sigue funcionando (los adjuntos no son obligatorios para operar). Error claro en la interfaz. |
| **Subida de adjunto incompleta** | Archivo sin registro, o registro sin archivo | Dos pasos: subida con URL prefirmada y luego confirmación. Un job diario borra objetos sin confirmar de más de 24 h. |
| **Caída de la base de datos** | El sistema no puede operar | Base gestionada con failover a una réplica en espera; backups diarios + archivado continuo de WAL (RPO ≤ 15 min). Restauración probada periódicamente. |
| **Deploy con error** | La nueva versión no funciona | Deploy gradual (una instancia primero), migraciones compatibles hacia atrás (Flyway) y rollback a la imagen anterior. |

**Observabilidad**: logs estructurados con un ID de correlación por request, métricas (latencia, errores, eventos pendientes en el outbox, ítems sin resolver por urgencia) y alertas sobre outbox atascado y tasa de errores.

## 10. Casos borde

**Concurrencia**

1. **Dos Encargados toman el mismo ítem a la vez** → `UPDATE ... WHERE estado='PENDIENTE' AND version=?`: solo uno gana; el otro recibe `409` y la bandeja se refresca.
2. **El solicitante edita un ítem justo cuando un Encargado lo toma** → la edición exige `estado='PENDIENTE'` y la versión esperada; si el Encargado ganó, falla con un mensaje claro ("este ítem ya está siendo cotizado").
3. **El solicitante cancela la solicitud mientras un ítem pasa a En cotización** → la cancelación verifica en una transacción que todos los ítems sigan Pendientes; si no, se rechaza y se ofrece cancelar solo los que quedan Pendientes.
4. **Doble click en Aprobar** → protegido por `version` y por el índice único de cotización seleccionada.

**Cotizaciones y precios**

5. **La cotización vence entre que el Encargado la elige y aprueba** → la vigencia se valida en el backend en el momento de aprobar, no solo en pantalla.
6. **Todas las cotizaciones de un ítem vencieron** → el ítem sigue En cotización y la bandeja lo marca como "cotizaciones vencidas".
7. **Cotizaciones en monedas distintas para el mismo ítem** → se permiten; se muestran por separado, sin conversión automática (no se depende de un tipo de cambio). La decisión es del Encargado.
8. **Un proveedor se da de baja después de haber cotizado** → las cotizaciones y órdenes existentes siguen válidas (la orden copia los datos del proveedor), pero deja de sugerirse en ítems nuevos.

**Catálogo**

9. **Se unifica una categoría Nueva con una existente** → los ítems conservan su `categoria_id` original; la categoría vieja queda `UNIFICADA` apuntando a la destino, y los reportes siguen la redirección. No se reescribe el pasado.
10. **Categoría sin ningún proveedor** → la bandeja lo indica y permite dar de alta un proveedor desde ahí.
11. **Mismo rubro escrito distinto** ("Plomero" / "plomería") → índice único sin distinguir mayúsculas y un buscador con coincidencias aproximadas en el formulario, que sugiere la categoría existente antes de crear una nueva.

**Estados y ciclo de vida**

12. **Todos los ítems cancelados uno por uno** → la solicitud pasa a Cancelada, no a Lista, y no se notifica como resuelta.
13. **Todos los ítems rechazados** → la solicitud pasa a Lista (se notifica) y enseguida a Cerrada, porque no hay órdenes que enviar.
14. **Órdenes que nunca se marcan como Enviadas** → la solicitud queda en Lista; un listado de "órdenes generadas sin enviar" lo hace visible.

**Usuarios y roles**

15. **Se le quita el rol a un Encargado con ítems asignados** → sus ítems vuelven a la bandeja general (sin asignar, En cotización) y se avisa a los demás Encargados.
16. **Se intenta desactivar al último Encargado** → el sistema no lo permite; el Administrador debe asignar otro primero.
17. **El solicitante cambia de sector** → sus solicitudes viejas conservan el sector copiado al crearlas, así los reportes históricos no cambian.
18. **Se da de baja un usuario con solicitudes en curso** → siguen su circuito normal; si su mail ya no existe, la notificación queda registrada dentro del sistema.

**Recordatorios y tiempo**

19. **Zona horaria** → las fechas se guardan en UTC (`TIMESTAMPTZ`) y los recordatorios se calculan en hora de Argentina, para no enviarlos de madrugada.
20. **Recordatorio de un ítem sin asignar** → va a todos los Encargados; cuando alguien lo toma, pasa a ir solo al asignado.

**Adjuntos y datos de entrada**

21. **Archivo que dice ser PDF pero no lo es, o supera 10 MB** → se valida el tipo real del contenido (no solo la extensión) y el tamaño antes de confirmar; opcionalmente se escanea con antivirus (ClamAV).
22. **Cantidades con decimales** ("2,5 horas de servicio") → `cantidad` es numérica con decimales.
23. **Números de orden con saltos** → una secuencia de PostgreSQL puede saltear números si una transacción se revierte. Si el área contable exige numeración sin huecos, se reemplaza por una tabla contador con bloqueo (más lenta, pero con este volumen es irrelevante).
