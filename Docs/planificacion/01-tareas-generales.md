# Nivel 1 — Tareas generales

Las 22 tareas grandes del proyecto. Cada una explica qué hay que configurar y qué hay que crear, sin código. El detalle de cada parte está en `02-tareas-especificas.md` y las fechas en `03-area-dificultad-fechas.md`.

Las secciones con § se refieren a `Docs/diseno-del-sistema.md`. Las convenciones de carpetas y nombres están en `README.md`.

| Tema | Tareas |
|---|---|
| Fundamentos | T01 · T02 · T03 |
| Usuarios y seguridad | T04 · T05 |
| Módulos de negocio (backend) | T06 Catálogo · T07-T08 Solicitudes · T09-T10 Compras · T11 Órdenes |
| Transversales (backend) | T12 Eventos · T13 Auditoría · T14 Adjuntos · T15 Notificaciones · T16 Reportes |
| Frontend | T17 · T18 · T19 · T20 |
| Cierre | T21 Calidad · T22 Documentación y demo |

---

## Fundamentos

### T01 · Preparar el repositorio y el entorno — S0

En esta tarea hay que dejar el repo limpio y que cada integrante tenga su base de datos funcionando. Hay que sacar del repo los archivos que no corresponden (configuración de IntelliJ, `HELP.md`, la carpeta `back/resources/` que quedó fuera de Maven), corregir el `.gitignore`, crear una plantilla de Pull Request y proteger la rama `main` para que todo entre por PR con revisión. Después, cada integrante configura su base PostgreSQL (con el Docker Compose que ya existe, o una base instalada) y completa su `back/.env` con credenciales propias.

**Qué hay que crear o configurar:** plantilla de PR, reglas de la rama `main`, `back/.env` de cada integrante, versión fija de Postgres en `compose.yaml`.

**Documentación:**
- [git rm (sacar archivos del repo)](https://git-scm.com/docs/git-rm)
- [Plantillas de Pull Request en GitHub (en español)](https://docs.github.com/es/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository)
- [Reglas de ramas (rulesets) en GitHub (en español)](https://docs.github.com/es/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
- [Soporte de Docker Compose en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/reference/features/dev-services.html#features.dev-services.docker-compose)
- Guía con los comandos exactos: `guias/sprint-0-paso-a-paso.md`

### T02 · Esqueleto modular, migraciones base y tests — S0

En esta tarea hay que armar la estructura sobre la que se construye todo el backend. Hay que crear los 8 paquetes de módulos (`auth`, `catalogo`, `solicitudes`, `compras`, `ordenes`, `reportes`, `integraciones`, `auditoria`) y un test que verifique que ningún módulo usa el interior de otro. Hay que crear las primeras migraciones de Flyway: un schema de PostgreSQL por módulo y la tabla donde Spring Modulith guarda los eventos. Hay que configurar Hibernate para que solo valide las tablas y no las cree. Por último, hay que hacer que los tests tengan una base de datos propia con Testcontainers, porque hoy `./mvnw test` falla.

**Qué hay que crear o configurar:** paquetes de módulos, test de arquitectura, migraciones iniciales (schemas + `event_publication`), configuración de tests con Testcontainers.

**Documentación:**
- [Spring Modulith: fundamentos](https://docs.spring.io/spring-modulith/reference/fundamentals.html) y [verificación de módulos](https://docs.spring.io/spring-modulith/reference/verification.html)
- [Spring Modulith: SQL de `event_publication` para PostgreSQL](https://docs.spring.io/spring-modulith/reference/appendix.html#schemas.postgresql)
- [Flyway en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
- [Schemas en PostgreSQL](https://www.postgresql.org/docs/current/ddl-schemas.html)
- [Testcontainers en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/reference/testing/testcontainers.html) y [módulo PostgreSQL de Testcontainers](https://java.testcontainers.org/modules/databases/postgres/)
- Guía con el código probado: `guias/sprint-0-paso-a-paso.md`

### T03 · Base común del backend — S1

En esta tarea hay que crear lo que usan todos los módulos. Hay que definir un formato único de errores para toda la API (código, mensaje y, si aplica, los campos inválidos) y un manejador global que traduzca cada tipo de error a su código HTTP: 400 datos inválidos, 401 sin login, 403 sin permiso, 404 no existe, 409 conflicto de versión, 422 regla de negocio. Hay que definir la forma estándar de las respuestas paginadas. Hay que agregar la documentación automática de la API (Swagger) para que el front vea los endpoints, y configurar GitHub Actions para que compile y corra los tests en cada PR.

**Qué hay que crear o configurar:** excepciones de negocio, manejador global de errores, DTO de página, Swagger, workflow de CI.

**Documentación:**
- [Spring MVC: errores en APIs REST (ProblemDetail)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring MVC: `@RestControllerAdvice`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
- [Validación en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/reference/io/validation.html)
- [Paginación con Spring Data](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html)
- [springdoc-openapi (Swagger)](https://springdoc.org/)
- [GitHub Actions con Java y Maven (en español)](https://docs.github.com/es/actions/tutorials/build-and-test-code/java-with-maven)

---

## Usuarios y seguridad

### T04 · Usuarios, sectores y login (módulo `auth`) — S1

En esta tarea hay que crear el módulo de usuarios y el login. Hay que crear el **schema** `auth` con las tablas de sectores y usuarios (§6.1, más la contraseña cifrada que el diseño no tiene), el **dominio** (Sector, Usuario y el enum de roles Solicitante, Encargado y Administrador), los **repositories** y datos de prueba (sectores y un usuario por rol). Después hay que configurar Spring Security para que la API use tokens JWT: un endpoint de login recibe email y contraseña, las valida y devuelve un token firmado; todos los demás endpoints exigen ese token. La clave para firmar va en el `.env`. También hay que exponer un endpoint que devuelva el usuario logueado, que el front necesita.

**Qué hay que crear:** schema `auth`, dominio, repositories, datos semilla, configuración de seguridad, service y endpoint de login, endpoint de usuario actual, tests.

**Documentación:**
- [Spring Boot y Spring Security](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html)
- [Spring Security: JWT en un resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security: guardado de contraseñas (BCrypt)](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [Spring Data JPA: repositories](https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html)

### T05 · Permisos por rol y administración de usuarios — S2 y S4

En esta tarea hay que restringir cada endpoint según el rol: el Solicitante solo ve y maneja lo suyo, solo el Encargado cotiza, aprueba, rechaza y maneja órdenes, y solo el Administrador maneja usuarios y sectores. Hay que crear el ABM (alta, baja, modificación) de sectores y usuarios para el Administrador, y una API interna del módulo para que otros módulos consulten datos de usuarios (por ejemplo, los emails de los Encargados para los avisos). En S4 hay que cubrir dos casos borde: no se puede desactivar al último Encargado, y si a un Encargado se le quita el rol, sus ítems vuelven a la bandeja general.

**Qué hay que crear:** reglas de autorización, services y endpoints de ABM, `AuthApi` para otros módulos, evento de Encargado desasignado.

**Documentación:**
- [Spring Security: autorizar requests HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
- [Spring Security: seguridad a nivel de método (`@PreAuthorize`)](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- Casos borde 15 y 16 del diseño (§10)

---

## Módulos de negocio (backend)

### T06 · Catálogo de categorías y proveedores (módulo `catalogo`) — S1, S2 y S4

En esta tarea hay que crear el catálogo. Hay que crear el **schema** `catalogo` (§6.3) con categorías, proveedores y la relación entre ambos (un proveedor ofrece varias categorías), el **dominio**, los **repositories** con las búsquedas necesarias y los **services** con sus endpoints (§7.3): ABM de proveedores, búsqueda de categorías para el autocompletado del formulario, alta de categorías Nuevas desde una solicitud, promoción a Conocida y unificación de duplicados. También una API interna para que Compras pida los proveedores activos de una categoría. Reglas del enunciado: los proveedores no guardan precios, y el nombre de una categoría es único por tipo sin distinguir mayúsculas.

**Qué hay que crear:** schema, dominio (Categoria, Proveedor), repositories, services, endpoints, `CatalogoApi`, evento `CategoriaCreada`, tests.

**Documentación:**
- [Spring Data JPA: métodos de consulta](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Guía: Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
- [Guía: Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- Enunciado: "Categoría" y "Proveedor"; diseño casos borde 9 a 11

### T07 · Solicitudes: crear y consultar (módulo `solicitudes`) — S1

En esta tarea hay que crear el corazón del sistema. Hay que crear el **schema** `solicitudes` (§6.2) con solicitudes e ítems, el **dominio** con sus enums (urgencia, tipo, estados de solicitud y de ítem) y control de versión para detectar ediciones simultáneas, y los **repositories**. Antes de programar la lógica hay que definir **todos los eventos del sistema** (qué módulo publica y cuál escucha cada uno), porque los usan todos los módulos. Después, el **service** y los endpoints (§7.1) para crear una solicitud con sus ítems (publicando `SolicitudCreada`) y para que el solicitante vea sus solicitudes y el detalle de cada una. Reglas: al menos un ítem, cantidad mayor a 0, el sector se copia del usuario al crear.

**Qué hay que crear:** schema, dominio, repositories, eventos del sistema, service, endpoints de alta y consulta, tests.

**Documentación:**
- [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
- [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Transacciones (`@Transactional`)](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

### T08 · Solicitudes: editar, cancelar y estado automático — S2, S3 y S4

En esta tarea hay que completar el ciclo de vida de la solicitud. Hay que agregar al **service** y a los endpoints (§7.1) editar y borrar ítems que sigan Pendientes, y cancelar la solicitud entera si todos sus ítems siguen Pendientes. Como Compras tiene su propia copia de cada ítem, hay que avisarle de estos cambios con eventos, y resolver el caso en que el Encargado toma el ítem justo mientras el solicitante lo cancela. Después, en S3, hay que hacer que el estado de los ítems y de la solicitud se actualice solo escuchando los eventos de Compras y Órdenes: la solicitud pasa a Lista cuando todos sus ítems están resueltos (y ahí se avisa), a Cerrada cuando todas sus órdenes se enviaron, y a Cancelada si todos sus ítems se cancelaron. En S4, protección contra doble envío del formulario.

**Qué hay que crear:** métodos de service y endpoints de edición y cancelación, eventos de sincronización con Compras, listeners de eventos de Compras y Órdenes, cálculo del estado de la solicitud, tests.

**Documentación:**
- [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring Data JPA: bloqueo optimista](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- Enunciado: "Estados de un ítem" y "Estados de una solicitud"; diseño casos borde 2, 3, 12, 13 y 14

### T09 · Compras: bandeja y asignación (módulo `compras`) — S2

En esta tarea hay que crear la bandeja del Encargado. Hay que crear el **schema** `compras` (§6.4) con la gestión de cada ítem, el **dominio** y los **repositories**. Un **listener** escucha `SolicitudCreada` y crea una gestión por cada ítem, copiando lo necesario para ordenar la bandeja sin consultar otro módulo (urgencia, fecha necesaria, categoría, cantidad). El **service** y los endpoints (§7.2) muestran la bandeja ordenada por urgencia (Alta primero) y fecha necesaria, y permiten tomar un ítem o reasignarse uno de otro Encargado. Si dos Encargados toman el mismo ítem a la vez, solo uno gana y el otro recibe un error 409.

**Qué hay que crear:** schema, dominio, repositories, listener de `SolicitudCreada` (y de edición y cancelación), service y endpoints de bandeja, tomar y reasignar, evento `ItemAsignado`, tests de concurrencia.

**Documentación:**
- [Spring Modulith: eventos](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring Data JPA: bloqueo optimista](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Índices parciales en PostgreSQL](https://www.postgresql.org/docs/current/indexes-partial.html)
- Enunciado: "Urgencia y recordatorios"; diseño caso borde 1

### T10 · Compras: cotizaciones, aprobar y rechazar — S2 y S4

En esta tarea hay que completar el trabajo del Encargado. Hay que agregar al **dominio** y al **schema** las cotizaciones (§6.4): proveedor, moneda (ARS o USD), precio unitario, total calculado, fecha de validez. El **service** y los endpoints (§7.2) permiten cargar cotizaciones, ver los proveedores sugeridos para el ítem (preguntándole al Catálogo), aprobar el ítem eligiendo una cotización vigente, o rechazarlo con un motivo. Al aprobar se publica `ItemAprobado` con todo lo que Órdenes necesita para generar la orden; al aprobar o rechazar se publica `ItemResuelto`. Reglas: una cotización vencida no se puede elegir, aprobado exige cotización elegida, rechazado exige motivo, y se puede rechazar sin haber cotizado.

**Qué hay que crear:** dominio y schema de cotizaciones, repositories, service y endpoints de cotizar, aprobar, rechazar y proveedores sugeridos, eventos `ItemAprobado` e `ItemResuelto`, tests.

**Documentación:**
- [Spring MVC: errores REST (422 para reglas de negocio)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Transacciones](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- Enunciado: "Cotización" y "Reglas para no olvidarse"; diseño casos borde 4 a 8

### T11 · Órdenes de pedido (módulo `ordenes`) — S3 y S4

En esta tarea hay que generar las órdenes. Hay que crear el **schema** `ordenes` (§6.5) con la orden de pedido y una secuencia para el número correlativo, el **dominio** y el **repository**. Un **listener** escucha `ItemAprobado` y genera la orden en el momento, una por ítem, copiando los datos del proveedor y el precio (la orden es un documento y no cambia si después se edita el proveedor). Tiene que ser idempotente: si el evento llega dos veces, no se generan dos órdenes. El **service** y los endpoints (§7.4) permiten listar las órdenes y marcarlas como Enviadas, publicando `OrdenEnviada`. En S4, descargar la orden en PDF.

**Qué hay que crear:** schema con secuencia, dominio, repository, listener de `ItemAprobado`, service y endpoints, eventos `OrdenGenerada` y `OrdenEnviada`, PDF, tests.

**Documentación:**
- [Secuencias en PostgreSQL](https://www.postgresql.org/docs/current/sql-createsequence.html)
- [OpenPDF (generar PDF en Java)](https://github.com/LibrePDF/OpenPDF)
- Enunciado: "Orden de pedido"; diseño §9 ("Un evento se entrega dos veces") y caso borde 23

---

## Transversales (backend)

### T12 · Eventos confiables e idempotencia — S3

En esta tarea hay que asegurar que ningún evento entre módulos se pierda ni se procese dos veces. Hay que configurar Spring Modulith para que, si la app se cae después de guardar un cambio pero antes de procesar su evento, el evento se vuelva a procesar al reiniciar. Hay que revisar que cada listener soporte recibir el mismo evento dos veces sin duplicar nada. Hay que escribir tests de módulo que publiquen un evento y verifiquen el resultado en el otro módulo.

**Qué hay que configurar y crear:** propiedades del registro de eventos, controles de idempotencia en los listeners, tests con la API `Scenario`.

**Documentación:**
- [Spring Modulith: Event Publication Registry](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring Modulith: tests de módulos](https://docs.spring.io/spring-modulith/reference/testing.html)
- Diseño §4 (decisión 2) y §9

### T13 · Historial de auditoría (módulo `auditoria`) — S3 y S4

En esta tarea hay que registrar todo cambio. Hay que crear el **schema** `auditoria` con la tabla de historial (§6.7), en la que solo se inserta y nunca se edita ni se borra, el **dominio** y el **repository**. Un conjunto de **listeners** escucha los eventos de cambio de todos los módulos (creación, asignación, reasignación, aprobación, rechazo, cancelación, orden generada y enviada) y registra quién hizo qué, cuándo y el estado anterior y nuevo. En S4, un endpoint para consultar el historial de una solicitud o ítem.

**Qué hay que crear:** schema, dominio, repository, listeners, endpoint de consulta.

**Documentación:**
- [Spring Modulith: eventos](https://docs.spring.io/spring-modulith/reference/events.html)
- Enunciado: "Historial" y regla 11

### T14 · Adjuntos — S3 y S4

En esta tarea hay que permitir subir archivos. Hay que crear un servicio de almacenamiento que guarde los archivos en una carpeta del servidor (configurada en el `.env`) con nombres únicos. Hay que crear las tablas de metadatos de adjuntos (§6.2 y §6.4), y los endpoints para subir (multipart) y descargar adjuntos de un ítem y de una cotización. Reglas: solo PDF o imágenes, hasta 10 MB, y se valida el tipo real del contenido, no solo la extensión.

**Qué hay que crear:** servicio de almacenamiento, schema de adjuntos, endpoints de subida y descarga, validaciones.

**Documentación:**
- [Guía: Uploading Files](https://spring.io/guides/gs/uploading-files/)
- [Spring MVC: multipart](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/multipart.html)
- [Apache Tika (detectar el tipo real de un archivo)](https://tika.apache.org/)
- Enunciado: "Adjuntos"; diseño caso borde 21

### T15 · Notificaciones y recordatorios con n8n (módulo `integraciones`) — S4

En esta tarea hay que conectar el sistema con n8n para los avisos. Hay que levantar n8n y un servidor de mail de prueba (Mailpit) en Docker. En el backend, el módulo `integraciones` escucha `SolicitudCreada` y `SolicitudLista` y llama a webhooks de n8n con los datos del aviso. En n8n hay que armar tres workflows: aviso a los Encargados cuando entra una solicitud, mail al solicitante cuando su solicitud está Lista (con el resultado de cada ítem), y un workflow diario que pide al backend los recordatorios pendientes (Alta cada 3 días, Media y Baja cada 3 semanas), manda los mails y avisa al backend que los envió. Los workflows se exportan al repo para que todos los importen.

**Qué hay que crear:** servicios en `compose.yaml`, cliente de webhooks, endpoints de recordatorios, API key para n8n, 3 workflows exportados.

**Documentación:**
- [n8n: documentación](https://docs.n8n.io/) e [instalación con Docker](https://docs.n8n.io/deploy/host-n8n/install-options/install-with-docker)
- [n8n: nodo Webhook](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook), [Schedule Trigger](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.scheduletrigger), [Send Email](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.sendemail) y [HTTP Request](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.httprequest)
- [Mailpit](https://mailpit.axllent.org/docs/)
- [Spring: clientes REST (`RestClient`)](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- Diseño §7.5; enunciado "¿Y n8n para qué?" y "Urgencia y recordatorios"

### T16 · Reportes (módulo `reportes`) — S4

En esta tarea hay que armar los reportes de gasto. Hay que crear el **schema** `reportes` (§6.6) con dos tablas de lectura que el módulo alimenta solo, escuchando eventos: una fila de gasto por cada orden generada y una fila de tiempo de resolución por cada ítem resuelto. El **service** y los endpoints (§7.4) calculan el gasto por sector, categoría o proveedor en un rango de fechas, con pesos y dólares siempre separados, y el tiempo promedio de resolución, general y por urgencia.

**Qué hay que crear:** schema, dominio, repositories con consultas agrupadas, listeners, service y endpoints.

**Documentación:**
- [Spring Data JPA: consultas con `@Query`](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- Enunciado: "Reportes"; regla 10

---

## Frontend

### T17 · Base del frontend, login y navegación — S1

En esta tarea hay que armar la estructura del frontend. Hay que instalar React Router y definir las rutas y el layout con un menú que cambia según el rol. Hay que crear un cliente HTTP que agregue el token a cada llamada, redirija al login si el token venció y muestre los errores de la API. Hay que crear la pantalla de login, guardar el usuario logueado en un contexto accesible desde toda la app y proteger las rutas según el rol. También los componentes comunes: tabla paginada, mensaje de error, etiqueta de estado.

**Qué hay que crear:** estructura de carpetas, rutas, layout, cliente HTTP, contexto de usuario, login, rutas protegidas, componentes comunes.

**Documentación:**
- [React: aprender](https://es.react.dev/learn) y [manejo de estado](https://es.react.dev/learn/managing-state) (en español)
- [React Router](https://reactrouter.com/home)
- [Fetch API (en español)](https://developer.mozilla.org/es/docs/Web/API/Fetch_API/Using_Fetch)
- [Vite: variables de entorno (en español)](https://es.vite.dev/guide/env-and-mode)

### T18 · Pantallas del Solicitante — S2

En esta tarea hay que hacer lo que usa cualquier empleado. Hay que crear el formulario de nueva solicitud, con ítems que se agregan y quitan, autocompletado de categoría (o crear una nueva escribiéndola) y validaciones antes de enviar. Hay que crear la lista de "mis solicitudes" con filtro por estado, y el detalle de una solicitud con el estado de cada ítem, el resultado (a qué proveedor se aprobó o por qué se rechazó) y las acciones permitidas: editar o borrar un ítem Pendiente y cancelar la solicitud.

**Qué hay que crear:** páginas de nueva solicitud, mis solicitudes y detalle; funciones de API de solicitudes y categorías.

**Documentación:**
- [React: formularios (en español)](https://es.react.dev/reference/react-dom/components/form)
- Enunciado: "Cómo funciona, con un ejemplo" (pasos 1, 2 y 8)

### T19 · Pantallas del Encargado — S3 y S4

En esta tarea hay que hacer lo que usa el Encargado de compras. Hay que crear la bandeja (lista ordenada, filtros, tomar y reasignar, aviso si otro lo tomó antes), la pantalla de un ítem (sus datos, proveedores sugeridos, alta rápida de proveedor, carga y comparación de cotizaciones, aprobar eligiendo una o rechazar con motivo) y la lista de órdenes para marcarlas como enviadas. En S4, el ABM de proveedores y categorías (promover Nuevas y unificar duplicadas).

**Qué hay que crear:** páginas de bandeja, gestión del ítem, órdenes y catálogo; funciones de API de compras, órdenes y catálogo.

**Documentación:**
- [React: manejo de estado (en español)](https://es.react.dev/learn/managing-state)
- Enunciado: "Cómo funciona, con un ejemplo" (pasos 3 a 7)

### T20 · Administración, reportes, adjuntos e historial en el front — S4

En esta tarea hay que completar las pantallas restantes: administración de usuarios y sectores, reportes (filtros de fecha, tablas por moneda y un gráfico), subir y descargar adjuntos en ítems y cotizaciones, y ver el historial de cambios en el detalle de una solicitud.

**Qué hay que crear:** páginas de admin y reportes, componentes de adjuntos e historial.

**Documentación:**
- [Recharts (gráficos en React)](https://recharts.github.io/en-US/guide/getting-started/)
- [FormData, para subir archivos (en español)](https://developer.mozilla.org/es/docs/Web/API/FormData)

---

## Cierre

### T21 · Calidad y prueba completa — continuo y cierre

En esta tarea hay que asegurar que todo funcione. Cada subtarea de backend trae sus tests (reglas de dominio y endpoints). Al cierre hay que recorrer el ejemplo de Laura completo desde la interfaz y probar los casos borde de concurrencia.

**Documentación:**
- [Spring Boot: testing](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html)
- [Guía: Testing the Web Layer](https://spring.io/guides/gs/testing-web/)

### T22 · Documentación, empaquetado y demo — cierre

En esta tarea hay que preparar la entrega: actualizar el documento de diseño con las decisiones reales, escribir un README de instalación, empaquetar la app con Docker para la demo, preparar datos de ejemplo y el guion de la presentación, y explicar en el diseño cómo escalaría el sistema y cómo maneja fallas.

**Documentación:**
- [Spring Boot: Dockerfiles](https://docs.spring.io/spring-boot/4.1.1/reference/packaging/container-images/dockerfiles.html)
- [Vite: build para producción (en español)](https://es.vite.dev/guide/static-deploy)
