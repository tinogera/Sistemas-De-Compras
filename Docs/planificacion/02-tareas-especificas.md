# Nivel 2 — Tareas específicas

Cada tarea general de `01-tareas-generales.md` dividida en subtareas. Cada subtarea dice qué hay que crear, qué campos, métodos, endpoints y reglas tiene que cumplir, y dónde está la documentación. El área, la dificultad y la fecha de cada una están en `03-area-dificultad-fechas.md`.

- Las rutas de backend son relativas a `back/src/main/java/com/Sistem/Solicitude_Compra/` salvo que se diga otra cosa. Las de frontend, a `frontend/src/`.
- Las secciones con § son de `Docs/diseno-del-sistema.md`.
- La estructura de carpetas de cada módulo y el nombre de las migraciones siguen las convenciones de `README.md`.
- Cada subtarea termina con **Documentación**: links a la documentación oficial del tema exacto que se ve en esa subtarea (la versión que usa el proyecto: Spring Boot 4.1, Hibernate 7.4, Spring Modulith 2.1, Java 25). Cuando existe una traducción oficial, el link va a la versión en español y lo dice.
- **Requisito nuevo del 08/10 (reglas de aprobación):** las decisiones 7 a 13 de abajo lo explican. Las tareas **T23, T24 y T25 son nuevas** (están al final del archivo) y las tareas ya existentes se modificaron donde las toca. Lo que ya estaba mergeado (T01 a T03) no se reescribe: lo que le falta se agrega como subtarea nueva (T23.1).

## Decisiones que cambian el diseño original

Surgieron al bajar el diseño a tareas. Hay que reflejarlas en el documento de diseño al cierre (T22.1).

1. **Los eventos y los tipos compartidos van en un módulo `compartido`, no en cada módulo.** Solicitudes escucha eventos de Compras y Compras escucha eventos de Solicitudes. Si cada evento vive en su módulo, eso es una dependencia circular y `ModularityTests` falla (probado el 27/09: `Cycle detected: Slice compras -> ...`). Con los eventos en `compartido/eventos/` y el módulo declarado como abierto, el test pasa.
2. **Todo evento lleva `UUID eventoId` y `Instant ocurridoEn`.** El id sirve para que los listeners detecten duplicados (§9); la fecha, para el historial.
3. **Columnas que el diseño no tiene y hacen falta:**
   - `auth.usuario.password_hash` para el login.
   - En `compras.gestion_item`: `sector_id` (reportes), `tipo` y `detalle` (para mostrar en la bandeja), `prioridad` (ordenar por urgencia) y `solicitado_en` (tiempos de resolución y recordatorios).
   - En `solicitudes.item`: `proveedor_comprado` (antes `proveedor_aprobado`, ver 9), `motivo_rechazo`, `rechazado_por` y `orden_enviada`, para mostrarle el resultado al solicitante y saber cuándo cerrar la solicitud. Las columnas de precio y de aprobación van en la decisión 12.
   - En `ordenes.orden_pedido`: `tipo` y `detalle`, para que la orden y su PDF digan qué se compra (T11.1).
   - Tablas nuevas: `solicitudes.clave_idempotencia` (T08.7) y `auditoria.item_solicitud` (T13.1).
   - En `compras.adjunto_cotizacion`: `subido_por` y los mismos `CHECK` de tipo y tamaño que `adjunto_item` (T14.3).
4. **Eventos que el diseño no tiene:** `ItemEditado`, `ItemCancelado`, `SolicitudCancelada` y `CancelacionRechazada` (sincronizar Solicitudes con Compras), `CotizacionCargada` (historial), `CategoriaUnificada` (reportes), `EncargadoDesasignado` (caso borde 15).
5. **Adjuntos:** subida directa (multipart) a una carpeta del servidor, en vez de URL prefirmada a S3.
6. **El historial lo ve también el solicitante dueño** de la solicitud, no solo el Encargado y el Admin (T13.3 y T20.4). El enunciado no lo restringe y le sirve para "saber siempre en qué quedó cada pedido".
7. **Aprobaciones por reglas (requisito nuevo del 08/10). Pisa al enunciado original** (que ya se actualizó el 08/10, igual que el diseño), que decía "no hay aprobaciones en cadena por monto o por sector", que el Encargado aprueba (regla 4) y que la orden se genera al aprobar (regla 7). Ahora:
   - **Toda solicitud llega siempre al Encargado.** Puede rechazarla (no hace falta, está mal, no se esperaba), negociar con los proveedores sugeridos y cargar cotizaciones y/o el precio total.
   - **En paralelo**, el módulo nuevo `aprobaciones` elige la regla que corresponde al monto del ítem y arma una **cadena de pasos**, por ejemplo `Supervisor → Gerencia General`. Un paso lo cumple un **rol** (Supervisor: una persona que puede variar) o un **sector** (Gerencia General). Los pasos con el mismo número de orden corren en paralelo; el número siguiente se activa cuando todos los anteriores aprobaron.
   - Mientras la cadena no esté completa se puede buscar precio y hablar con proveedores, pero **no se puede registrar la compra**.
   - **Rechazar es por ítem**, nunca por orden. Lo puede hacer el Encargado o cualquier aprobador en su paso, siempre con motivo.
   - **Las reglas las gestiona el Admin sin tocar código** (T23.3). Hoy solo por monto y moneda, pero el modelo deja lugar para condiciones nuevas (categoría, sector, urgencia). Una regla sin pasos aprueba sola ("compras de menos de X: aceptar"). Pasar de `Supervisor` a `Supervisor → GG` es editar la regla por defecto, no programar.
8. **Dos precios por ítem y un monto de envío.**
   - `precio_estimado` (con `moneda_estimada`): lo carga el **solicitante** al crear, como total estimado del ítem. Obligatorio y mayor a 0. Se asume que ya lo habló con su jefe y averiguó precios.
   - `precio_total` (con `moneda_total`): lo carga el **Encargado** al negociar, o sale de la cotización elegida. Puede no cargarse. Se actualiza en pantalla en tiempo real para todos los que ven el ítem (decisión 11).
   - `monto_envio`: opcional, lo carga el Encargado **después de la compra**, en la moneda del total.
   - **Monto que mira la regla:** el `precio_total` si existe; si no, el `precio_estimado`. Si el total cambia, la cadena se **re-evalúa**: los pasos ya aprobados se conservan, se agregan los que falten y se omiten los pendientes que ya no hacen falta. Si el solicitante edita el ítem, la cadena se reinicia.
   - **Sin conversión de moneda** (caso borde 7): una condición en ARS solo se cumple con importes en ARS. Para cubrir dólares el Admin crea una regla en USD.
9. **El estado del ítem va en dos vías.** `estado`: PENDIENTE, EN_COTIZACION, **COMPRADO**, RECHAZADO, CANCELADO. `estado_aprobacion`: EN_CURSO o APROBADA (`EstadoAprobacion` en `compartido/tipos/`). **APROBADO deja de ser un estado del ítem:** "listo para comprar" = EN_COTIZACION + APROBADA. COMPRADO reemplaza a APROBADO como estado final, así que la solicitud pasa a LISTA cuando todo está COMPRADO, RECHAZADO o CANCELADO (más tarde que antes, porque ahora hay que comprar). El Encargado tiene que tomar el ítem antes de comprarlo, aunque la cadena ya esté completa.
10. **La orden se genera al registrar la compra, no al aprobar.** El Encargado ve los ítems aprobados, hace la compra y recién ahí se emite la orden (`CompraRegistrada`, T10.7). Después de la compra se puede corregir el precio total y cargar el monto de envío (T10.8): la orden y los reportes se actualizan (`CompraActualizada`, T11.6 y T16.2) y todo queda en el historial. Reemplaza a `ItemAprobado`.
11. **Tiempo real con Server-Sent Events (SSE).** El backend expone un stream por usuario (T24.1) que manda **señales con ids** ("el ítem 4021 cambió"), sin datos de negocio. El front vuelve a pedir el detalle por REST, que es donde se controlan los permisos (T24.2). Con un solo backend alcanza con un registro en memoria; con varias réplicas haría falta Postgres `LISTEN/NOTIFY` o Redis (solo propuesta, T22.5).
12. **Tablas, columnas y eventos que el requisito agrega:**
    - Schema nuevo `aprobaciones` con `regla`, `regla_condicion`, `regla_paso`, `aprobacion_item`, `paso_item` y `evento_procesado` (T23.1). La regla por defecto (`Supervisor`) se crea **en la migración**, no en los datos de prueba, porque sin ella ningún ítem tendría cadena.
    - En `solicitudes.item`: `precio_estimado`, `moneda_estimada`, `precio_total`, `moneda_total` (copia de Compras para mostrarla sin consultar otro módulo) y `estado_aprobacion`.
    - En `compras.gestion_item`: `precio_estimado`, `moneda_estimada`, `precio_total`, `moneda_total`, `monto_envio`, `estado_aprobacion`, `proveedor_compra_id`, `comprado_en`. `EstadoGestion` pierde APROBADO y gana COMPRADO.
    - En `ordenes.orden_pedido`: `monto_envio`; `cotizacion_id` pasa a ser opcional (se puede comprar con un precio total cargado a mano). En `reportes.hecho_gasto`: `item_id` y `monto_envio`.
    - Eventos nuevos: `PrecioTotalActualizado`, `PasoAprobacionActivado`, `PasoDecidido`, `AprobacionCompletada`, `AprobacionReabierta`, `AprobacionRechazada`, `ReglaModificada`, `CompraRegistrada` y `CompraActualizada`. `ItemAprobado` desaparece y `ItemResuelto` pasa a tener resultado COMPRADO o RECHAZADO (tabla completa en T07.3).
13. **Roles y aprobadores.** `Rol` suma `SUPERVISOR` (sigue siendo un solo rol por usuario). Gerencia General es un **sector** más, que crea el Admin. Quien decide un paso de rol es cualquier usuario activo con ese rol; quien decide un paso de sector es cualquier usuario activo de ese sector. **Nadie decide sobre su propio pedido.** Casos borde nuevos: 24 a 28, en T23.

---

## T01 · Preparar el repositorio y el entorno

Todos los comandos y archivos exactos están en `guias/sprint-0-paso-a-paso.md`.

### T01.1 · Limpiar el repo
- Sacar `.idea/` del repo sin borrarla del disco (`git rm -r --cached .idea`).
- Borrar `HELP.md` y `back/resources/` (con su `aplication.properties` vacío y su `db/migration` vacía).
- En `.gitignore` (raíz): borrar las líneas `mvnw`, `mvnw.cmd` y `.gitignore`. El wrapper de Maven tiene que estar versionado.
- Verificar con `git ls-files` que no quede nada de eso.
- **Documentación:**
  - [Git: `git rm`](https://git-scm.com/docs/git-rm)
  - [Git: `.gitignore`](https://git-scm.com/docs/gitignore)
  - [Pro Git: guardar cambios en el repositorio (ignorar y borrar archivos) (en español)](https://git-scm.com/book/es/v2/Fundamentos-de-Git-Guardando-cambios-en-el-Repositorio)

### T01.2 · Plantilla de PR y protección de `main`
- Crear `.github/pull_request_template.md` con: ID de la tarea, qué cambia, cómo probarlo y un checklist (tests pasan, lint pasa, sin credenciales).
- En GitHub (lo hace el dueño del repo): regla sobre `main` que exija PR con 1 aprobación y bloquee force push. Agregar a los integrantes como colaboradores.
- **Documentación:**
  - [GitHub: plantilla de Pull Request (en español)](https://docs.github.com/es/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository)
  - [GitHub: reglas de ramas (rulesets) (en español)](https://docs.github.com/es/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
  - [GitHub: invitar colaboradores (en español)](https://docs.github.com/es/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository)

### T01.3 · Base de datos y `.env` de cada integrante
- Opción A (recomendada): Postgres con el `back/compose.yaml` que ya existe. Cada uno completa `back/.env` con `POSTGRES_DB`, `POSTGRES_USER` y `POSTGRES_PASSWORD` propios.
- Opción B: Postgres instalado. Además de lo anterior, `DB_HOST`, `DB_PORT` y `USAR_DOCKER_COMPOSE=false` en el `.env`, y en `application.properties` la línea `spring.docker.compose.enabled=${USAR_DOCKER_COMPOSE:true}`. Agregar esas claves vacías a `back/.env.example`.
- Verificar que `./mvnw spring-boot:run` desde `back/` arranque.
- **Documentación:**
  - [Spring Boot: soporte de Docker Compose](https://docs.spring.io/spring-boot/4.1.1/reference/features/dev-services.html#features.dev-services.docker-compose)
  - [Spring Boot: configuración externa (`application.properties` y variables de entorno)](https://docs.spring.io/spring-boot/4.1.1/reference/features/external-config.html)
  - [Docker Compose](https://docs.docker.com/compose/)

### T01.4 · Fijar la versión de Postgres
- En `back/compose.yaml`, cambiar `postgres:latest` por una versión fija (por ejemplo `postgres:17`), así todos usan la misma. Usar la misma en Testcontainers (T02.3).
- Por qué: con `latest`, cada integrante tiene la versión que bajó el día que hizo `docker compose up`, y cuando sale una versión mayor nueva unos tienen una y otros otra. Una migración que anda en una puede fallar en la otra.
- Elegir una versión mayor con soporte (ver la tabla de versiones de postgresql.org). Fijar la mayor (`postgres:17`) alcanza: las versiones menores (17.x) no cambian el formato de los datos.
- **Cuidado al cambiar la versión mayor:** Postgres no abre una carpeta de datos creada con otra versión mayor. Si alguien ya tenía un volumen de `latest` (por ejemplo 18) y pasa a 17, el contenedor no arranca. Solución en desarrollo: `docker compose down -v` (borra el volumen y los datos locales) y volver a levantar. Avisar al equipo en el PR.
- En Testcontainers: `new PostgreSQLContainer("postgres:17")`, la misma que en `compose.yaml`.
- Verificar con `docker compose exec postgres psql -U $POSTGRES_USER -c "select version();"` (el nombre del servicio es el de `compose.yaml`).
- **Documentación:**
  - [Imagen oficial de PostgreSQL en Docker Hub (versiones)](https://hub.docker.com/_/postgres)
  - [Docker Compose: referencia de servicios (`image`, `ports`, `volumes`, `extra_hosts`)](https://docs.docker.com/reference/compose-file/services/)
  - [Docker Compose: volúmenes](https://docs.docker.com/reference/compose-file/volumes/)
  - [Testcontainers: módulo PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/)

---

## T02 · Esqueleto modular, migraciones base y tests

Código probado en `guias/sprint-0-paso-a-paso.md`.

### T02.1 · Paquetes de los 8 módulos y test de arquitectura
- Crear un `package-info.java` con `@ApplicationModule(displayName = "...")` en cada paquete: `auth`, `catalogo`, `solicitudes`, `compras`, `ordenes`, `reportes`, `integraciones`, `auditoria`.
- Crear `back/src/test/java/com/Sistem/Solicitude_Compra/ModularityTests.java` con dos tests: uno que llama a `ApplicationModules.verify()` y otro que genera la documentación de módulos con `Documenter`.
- Debe pasar `./mvnw test -Dtest=ModularityTests` y listar los 8 módulos.
- **Documentación:**
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
  - [Spring Modulith: verificar la estructura](https://docs.spring.io/spring-modulith/reference/verification.html)

### T02.2 · Migraciones iniciales y validación de Hibernate
- `back/src/main/resources/db/migration/V1__schemas_iniciales.sql`: `CREATE SCHEMA` para `auth`, `catalogo`, `solicitudes`, `compras`, `ordenes`, `reportes`, `auditoria`.
- `V2__event_publication.sql`: tabla `event_publication` con el SQL oficial de Modulith para PostgreSQL.
- En `application.properties`: `spring.jpa.hibernate.ddl-auto=validate`.
- Al arrancar, Flyway aplica V1 y V2 y ya no aparece el error de `event_publication`.
- **Documentación:**
  - [Spring Modulith: SQL de `event_publication` para PostgreSQL](https://docs.spring.io/spring-modulith/reference/appendix.html#schemas.postgresql)
  - [Spring Boot: migraciones con Flyway](https://docs.spring.io/spring-boot/4.1.1/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
  - [Flyway: migraciones (nombres y versiones)](https://documentation.red-gate.com/flyway/flyway-concepts/migrations)
  - [PostgreSQL: schemas](https://www.postgresql.org/docs/current/ddl-schemas.html)
  - [Spring Boot: bases SQL y JPA](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html)

### T02.3 · Tests con Testcontainers
- En `back/pom.xml`, dependencias de test: `spring-boot-testcontainers`, `org.testcontainers:testcontainers-junit-jupiter` y `org.testcontainers:testcontainers-postgresql` (sin versión, la maneja Spring Boot).
- Crear `back/src/test/java/.../TestcontainersConfiguration.java` con un bean `PostgreSQLContainer` anotado con `@ServiceConnection`. La clase está en `org.testcontainers.postgresql` (Testcontainers 2.x).
- `SolicitudeCompraApplicationTests` lleva `@Import(TestcontainersConfiguration.class)`. Todo test futuro que use la base, también.
- `./mvnw test` pasa sin ninguna base creada a mano (3 tests).
- **Documentación:**
  - [Spring Boot: Testcontainers](https://docs.spring.io/spring-boot/4.1.1/reference/testing/testcontainers.html)
  - [Testcontainers: módulo PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/)
  - [Maven: dependencias y scopes](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)

---

## T03 · Base común del backend

### T03.1 · Módulo `compartido`
- Crear el paquete `compartido/` con un `package-info.java` que lo declare **abierto**: `@ApplicationModule(displayName = "Compartido", type = ApplicationModule.Type.OPEN)`. Abierto significa que todos sus subpaquetes son visibles para los demás módulos.
- Subpaquetes:
  - `compartido/tipos/`: enums usados por varios módulos: `Urgencia` (BAJA, MEDIA, ALTA, con un método `prioridad()` que devuelve 1 para ALTA, 2 para MEDIA y 3 para BAJA), `TipoItem` (PRODUCTO, SERVICIO), `Moneda` (ARS, USD), `Rol` (SOLICITANTE, ENCARGADO, ADMIN).
  - `compartido/eventos/`: los eventos (se definen en T07.3).
  - `compartido/errores/`: las excepciones de T03.2.
  - `compartido/web/`: DTOs comunes (T03.3) y el manejador de errores (T03.2).
- Regla: `compartido` no puede depender de ningún otro módulo.
- Verificar que `ModularityTests` siga pasando.
- **Documentación:**
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
  - [Spring Modulith: interfaces nombradas y módulos abiertos](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.named-interfaces)
  - [Java: enums](https://dev.java/learn/classes-objects/enums/)

### T03.2 · Formato de errores y manejador global
- En `compartido/errores/`, excepciones de negocio que cualquier service puede lanzar:
  - `NoEncontradoException(String mensaje)` → 404, código `NO_ENCONTRADO`.
  - `ReglaNegocioException(String codigo, String mensaje)` → 422. El código lo elige quien la lanza, por ejemplo `COTIZACION_VENCIDA` o `ITEM_NO_PENDIENTE`.
  - `ConflictoException(String codigo, String mensaje)` → 409.
  - `SinPermisoException(String mensaje)` → 403, para cuando el usuario está logueado pero la regla de negocio no lo deja (por ejemplo, ver la solicitud de otro).
- En `compartido/web/`:
  - `ErrorApi`: record con `codigo`, `mensaje` y `campos` (mapa campo → mensaje, solo para errores de validación). Es el formato del diseño §7.2.
  - `ManejadorErrores` (`@RestControllerAdvice`) que traduce:
    - Las 4 excepciones de arriba.
    - `MethodArgumentNotValidException` (falla `@Valid`) → 400, `VALIDACION`, con `campos`.
    - `ObjectOptimisticLockingFailureException` (choque de `@Version`) → 409, `CONFLICTO_VERSION`, con el mensaje "Otro usuario modificó este dato. Recargá la página."
    - `DataIntegrityViolationException` (se rompió una restricción única) → 409, `DUPLICADO`.
    - Errores del propio Spring MVC, que si no se mapean caen en el 500 de abajo:
      - `HttpMessageNotReadableException` (JSON mal formado o un enum con un valor que no existe) → 400, `CUERPO_INVALIDO`.
      - `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` (por ejemplo `?page=abc`) y `HandlerMethodValidationException` (falla una validación en un `@RequestParam` o `@PathVariable`) → 400, `PARAMETRO_INVALIDO`.
      - `NoResourceFoundException` (la ruta no existe) → 404, `NO_ENCONTRADO`.
      - `HttpRequestMethodNotSupportedException` → 405, `METODO_NO_PERMITIDO`.
    - Cualquier otra excepción → 500, `ERROR_INTERNO`: se loguea completa y al cliente solo le llega un mensaje genérico.
- **Ojo con el 500 genérico:** captura todo lo que no tenga un mapeo propio. Cuando se agregue Spring Security (T04.4), sus excepciones de permiso también llegan a este manejador; si no se mapean, un 403 sale como 500. Por eso T04.4 agrega el mapeo de 401 y 403 a este mismo manejador.
- Tests: un `@WebMvcTest` con un controller de prueba que lanza cada excepción y verifica código HTTP y cuerpo.
- **Documentación:**
  - [Spring MVC: `@RestControllerAdvice`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
  - [Spring MVC: errores en APIs REST](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
  - [Spring Boot: manejo de errores en Spring MVC](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html#web.servlet.spring-mvc.error-handling)
  - [Spring Boot: validación](https://docs.spring.io/spring-boot/4.1.1/reference/io/validation.html)
  - [Spring Boot: tests de la capa web con `@WebMvcTest`](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)
  - [MDN: códigos de estado HTTP (en español)](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Status)

### T03.3 · Respuesta paginada estándar
- En `compartido/web/`, record `Pagina<T>` con `contenido` (lista), `pagina`, `tamanio`, `totalElementos` y `totalPaginas`, y un método estático que lo arma desde un `Page<T>` de Spring Data.
- Todos los listados reciben `?page=0&size=20` (Spring los convierte solo en `Pageable`) y devuelven `Pagina<T>`. Así el front siempre recibe la misma forma.
- Tamaño máximo de página: 100 (`spring.data.web.pageable.max-page-size=100`).
- **Documentación:**
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Data: `Pageable` en controllers (soporte web)](https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web)
  - [Java: records](https://dev.java/learn/records/)

### T03.4 · Documentación de la API con Swagger
- Agregar al `pom.xml` `org.springdoc:springdoc-openapi-starter-webmvc-ui`. Usar la versión compatible con Spring Boot 4 que indica springdoc.org (la 3.x).
- Queda disponible en `http://localhost:8080/swagger-ui.html`. Cuando exista seguridad (T04.4), esa ruta y `/v3/api-docs/**` tienen que quedar abiertas y Swagger tiene que permitir cargar el token (botón "Authorize").
- **Documentación:**
  - [springdoc-openapi: primeros pasos](https://springdoc.org/#getting-started)
  - [springdoc-openapi: botón "Authorize" con token](https://springdoc.org/#how-do-i-add-authorization-header-in-requests)
  - [OpenAPI: autenticación Bearer](https://swagger.io/docs/specification/v3_0/authentication/bearer-authentication/)

### T03.5 · Integración continua con GitHub Actions
- Crear `.github/workflows/ci.yml` que corra en cada PR y en cada push a `main`, con dos jobs:
  - **backend:** instala Java 25 (temurin) con caché de Maven y corre `./mvnw -B verify` en `back/`. Los runners de GitHub tienen Docker, así que Testcontainers funciona.
  - **frontend:** instala Node 22 con caché de npm y, en `frontend/`, corre `npm ci`, `npm run lint` y `npm run build`.
- En la regla de `main` (T01.2), exigir que los dos jobs pasen antes de mergear.
- **Documentación:**
  - [GitHub Actions: compilar y testear Java con Maven (en español)](https://docs.github.com/es/actions/tutorials/build-and-test-code/java-with-maven)
  - [GitHub Actions: compilar y testear Node.js (en español)](https://docs.github.com/es/actions/tutorials/build-and-test-code/nodejs)
  - [GitHub Actions: sintaxis de workflows (en español)](https://docs.github.com/es/actions/reference/workflows-and-actions/workflow-syntax)
  - [Acción `actions/setup-java`](https://github.com/actions/setup-java)
  - [Acción `actions/setup-node`](https://github.com/actions/setup-node)
  - [GitHub: reglas de ramas (rulesets) (en español)](https://docs.github.com/es/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)

---

## T04 · Usuarios, sectores y login (módulo `auth`)

### T04.1 · Schema y dominio de `auth`
- **Migración** `V<fecha>_1__auth_tablas.sql`: tablas `auth.sector` y `auth.usuario` como en §6.1, más `password_hash VARCHAR(100) NOT NULL` en usuario.
- **Dominio** en `auth/dominio/`:
  - `Sector`: `id`, `nombre` (único, obligatorio, hasta 100 caracteres).
  - `Usuario`: `id`, `email` (único, obligatorio), `nombre`, `sector` (`@ManyToOne` a Sector), `rol` (enum `Rol` de `compartido/tipos`, guardado como texto con `@Enumerated(EnumType.STRING)`), `passwordHash`, `activo` (por defecto true).
- **Rol nuevo:** agregar `SUPERVISOR` al enum `compartido/tipos/Rol` (que ya está mergeado: hoy tiene SOLICITANTE, ENCARGADO y ADMIN) y revisar que el `CHECK` de `auth.usuario.rol` lo acepte. Es el rol de quien aprueba el primer paso de la cadena (decisión 7). Gerencia General **no** es un rol: es un sector.
- Hibernate con `validate` tiene que arrancar sin errores: los nombres y tipos de las columnas tienen que coincidir con la migración.
- **Documentación:**
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
  - [Hibernate 7: entidades](https://docs.hibernate.org/orm/7.4/introduction/html_single/#entities)
  - [Hibernate 7: `@ManyToOne`](https://docs.hibernate.org/orm/7.4/introduction/html_single/#many-to-one)
  - [Hibernate 7: enums (`@Enumerated`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#enums)
  - [Guía oficial: Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### T04.2 · Repositories de `auth`
- En `auth/repositorio/`:
  - `SectorRepository extends JpaRepository<Sector, Long>`: `existsByNombreIgnoreCase(String)`.
  - `UsuarioRepository extends JpaRepository<Usuario, Long>`: `findByEmailIgnoreCase(String)`, `findByRolAndActivoTrue(Rol)` (los Encargados o Supervisores activos, para avisos), `findBySectorIdAndActivoTrue(Long)` (los aprobadores de un paso de sector), `countByRolAndActivoTrue(Rol)` (para la regla del último Encargado), `existsByRolAndActivoTrue(Rol)` y `existsBySectorIdAndActivoTrue(Long)` (para que T23.3 compruebe que cada paso tiene a alguien que lo pueda decidir), `existsByEmailIgnoreCase(String)`.
- Test `@DataJpaTest` (con `@Import(TestcontainersConfiguration.class)`) que guarda y busca por email sin distinguir mayúsculas.
- **Documentación:**
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)
  - [Spring Data: conceptos de repositories](https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html)
  - [Spring Boot: tests de repositories con `@DataJpaTest`](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa)

### T04.3 · Datos de prueba
- Clase `auth/servicio/DatosDePrueba` que implementa `CommandLineRunner` y solo corre si en el `.env` está `DATOS_PRUEBA=true` (con `@ConditionalOnProperty`).
- Si la tabla de usuarios está vacía, crea:
  - Sectores: Recursos Humanos, Compras, Administración, Sistemas y **Gerencia General**.
  - Un usuario por rol: `solicitante@test.com` (Recursos Humanos), `encargado@test.com` (Compras), `admin@test.com` (Administración) y `supervisor@test.com` (Administración, rol SUPERVISOR). Más `gg@test.com` (Gerencia General, rol SOLICITANTE): es el aprobador del paso de sector. La contraseña sale de `DATOS_PRUEBA_PASSWORD` en el `.env` y se guarda cifrada con el `PasswordEncoder`.
- Agregar `DATOS_PRUEBA` y `DATOS_PRUEBA_PASSWORD` a `back/.env.example`.
- No se hace con una migración porque las migraciones también corren en producción, y no queremos usuarios de prueba ahí.
- **Documentación:**
  - [Spring Boot: `CommandLineRunner` (código al arrancar)](https://docs.spring.io/spring-boot/4.1.1/reference/features/spring-application.html#features.spring-application.command-line-runner)
  - [Spring Boot: `@ConditionalOnProperty`](https://docs.spring.io/spring-boot/4.1.1/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.condition-annotations.property-conditions)
  - [Spring Boot: configuración externa (`application.properties` y variables de entorno)](https://docs.spring.io/spring-boot/4.1.1/reference/features/external-config.html)
  - [Spring Security: guardado de contraseñas (BCrypt)](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)

### T04.4 · Login con JWT y configuración de seguridad
- Dependencias: `spring-boot-starter-security-oauth2-resource-server` (trae Spring Security y el soporte de JWT) y, para tests, `spring-boot-starter-security-test`.
- **Clave de firma:** `JWT_SECRET` en `.env` (al menos 32 caracteres, porque HS256 exige 256 bits) y `JWT_EXPIRACION_HORAS=8`. Agregarlas a `.env.example` sin valor.
- **Configuración** en `auth/servicio/SeguridadConfig` (o `auth/web/`):
  - `PasswordEncoder`: `BCryptPasswordEncoder`.
  - `JwtEncoder` y `JwtDecoder` con la clave HMAC (`NimbusJwtEncoder` y `NimbusJwtDecoder.withSecretKey`).
  - `JwtAuthenticationConverter` que lee el claim `rol` y lo convierte en la authority `ROLE_<rol>`. Así se puede usar `hasRole('ENCARGADO')`.
  - `SecurityFilterChain`: sin sesión (stateless), CSRF desactivado (la API no usa cookies), `oauth2ResourceServer(jwt)`. Rutas abiertas: `POST /api/v1/auth/login`, Swagger y `/actuator/health`. Todo lo demás requiere token.
- **401 y 403 con el formato común (`ErrorApi` de T03.2).** Spring Security genera estos errores en dos lugares distintos, y hay que cubrir los dos:
  - **En los filtros**, antes de llegar a un controller: falta el token, está vencido o su firma no es válida. Esto no pasa por el `@RestControllerAdvice`. En el `SecurityFilterChain`, configurar `.exceptionHandling(e -> e.authenticationEntryPoint(...).accessDeniedHandler(...))` y también el `authenticationEntryPoint` de `oauth2ResourceServer`. Los dos escriben un `ErrorApi` en JSON con el `ObjectMapper`: 401 `NO_AUTENTICADO` ("Tenés que iniciar sesión") y 403 `SIN_PERMISO`. Crear las dos clases en `auth/servicio/` (por ejemplo `EntradaNoAutenticado` y `ManejadorAccesoDenegado`).
  - **En los métodos con `@PreAuthorize`** (T05.1): la excepción (`AuthorizationDeniedException`, que extiende `AccessDeniedException`) se lanza dentro del controller y **sí** llega al `ManejadorErrores`. Agregarle un `@ExceptionHandler(AccessDeniedException.class)` → 403 `SIN_PERMISO`. Sin esto cae en el 500 genérico.
  - `SinPermisoException` (la de las reglas de negocio de T03.2) también devuelve 403 `SIN_PERMISO`, así el front trata igual los dos casos.
- **Service** `auth/servicio/LoginService.login(email, password)`:
  - Busca el usuario por email. Si no existe, está inactivo o la contraseña no coincide, lanza el **mismo** error 401 "Email o contraseña incorrectos" (no hay que revelar cuál de las tres falló). Para eso, crear `CredencialesInvalidasException` en `compartido/errores/` y mapearla a 401 con código `CREDENCIALES_INVALIDAS` en el manejador de T03.2.
  - Si está bien, arma un JWT con `sub` = id del usuario, y los claims `email`, `nombre`, `rol` y `sectorId`, con vencimiento a las `JWT_EXPIRACION_HORAS`.
- **Endpoint** `auth/web/AuthController`:
  - `POST /api/v1/auth/login`, body `{ "email", "password" }` (ambos obligatorios) → 200 `{ "token", "usuario": { "id", "nombre", "email", "rol", "sectorId" } }`.
- **Usuario actual:** en `compartido/seguridad/UsuarioActual`, un componente con `id()`, `rol()`, `sectorId()` (del claim `sectorId`) y `esEncargado()` que lee el JWT del `SecurityContextHolder`. Los services de todos los módulos lo usan para saber quién hace la acción; `aprobaciones` lo usa para saber si el usuario puede decidir un paso de rol o de sector.
- **Tests:**
  - Login correcto devuelve un token.
  - Contraseña incorrecta devuelve 401.
  - Un endpoint protegido sin token devuelve 401 con cuerpo `{ "codigo": "NO_AUTENTICADO", ... }`.
  - Un token vencido o con la firma alterada devuelve 401.
  - En los tests de otros endpoints, simular el usuario con `.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO")))` en MockMvc.
- Aviso al equipo: cuando esto se mergea, todos los endpoints piden token. Los tests de los endpoints ya existentes tienen que empezar a simular el usuario.
- **Documentación:**
  - [Spring Boot: Spring Security](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html)
  - [Spring Security: JWT en un resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
  - [Spring Security: sacar los roles de un claim del JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-authorization-extraction)
  - [Spring Security: arquitectura (cadena de filtros)](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-securityfilterchain)
  - [Spring Security: cómo se manejan los 401 y 403 (`ExceptionTranslationFilter`)](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-exceptiontranslationfilter)
  - [Spring Security: autorizar requests HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
  - [Spring Security: guardado de contraseñas (BCrypt)](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
  - [Spring Security: CSRF (por qué se desactiva en una API sin cookies)](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
  - [Spring Security: simular un JWT en tests con MockMvc (`jwt()`)](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
  - [RFC 7519: JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)
  - [OWASP: autenticación (mensajes de error genéricos)](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
  - [MDN: 401 Unauthorized (en español)](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Status/401)

### T04.5 · Usuario logueado y `AuthApi`
- **Endpoint** `GET /api/v1/auth/yo` → los datos del usuario del token, más el nombre del sector. El front lo llama al recargar la página.
- **`auth/AuthApi`** (clase pública en la raíz del módulo, que usan otros módulos):
  - `UsuarioResumen obtenerUsuario(Long id)`: id, nombre, email, rol, sectorId, activo. `UsuarioResumen` es un record público en la raíz de `auth/`.
  - `Long sectorDe(Long usuarioId)`.
  - `String nombreSector(Long sectorId)`.
  - `boolean existeSector(Long sectorId)`, `boolean hayUsuariosActivosConRol(Rol)` y `boolean hayUsuariosActivosEnSector(Long sectorId)`: las usa T23.3 al guardar una regla.
  - `List<String> emailsActivosConRol(Rol)` y `List<String> emailsActivosDelSector(Long sectorId)`: las usa T15.2 para avisar a los aprobadores.
- **Documentación:**
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
  - [Spring Security: JWT en un resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
  - [Spring MVC: mapeo de rutas (`@GetMapping`, `@PathVariable`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)

---

## T05 · Permisos por rol y administración de usuarios

### T05.1 · Permisos por rol en todos los endpoints
- Activar `@EnableMethodSecurity` y poner `@PreAuthorize` en cada controller o método:

| Endpoints | Quién puede |
|---|---|
| `/api/v1/solicitudes/**`, `/api/v1/items/**/adjuntos` | Cualquier usuario logueado (el service controla que sea dueño) |
| `/api/v1/bandeja/**`, `/api/v1/cotizaciones/**`, `/api/v1/ordenes/**`, `POST/PATCH /api/v1/proveedores`, `PATCH/POST /api/v1/categorias/**` | `ENCARGADO` |
| `GET /api/v1/categorias`, `GET /api/v1/proveedores` | Cualquier usuario logueado |
| `/api/v1/aprobaciones/**` | Cualquier usuario logueado (el service controla que sea aprobador de ese paso, el dueño del pedido, un Encargado o el Admin, T23.5 y T23.6) |
| `GET /api/v1/tiempo-real` | Cualquier usuario logueado (T24.1) |
| `/api/v1/admin/**` (incluye `/api/v1/admin/reglas/**`) | `ADMIN` |
| `/api/v1/reportes/**` | `ENCARGADO` o `ADMIN` |
| `/api/v1/historial/**` | Cualquier usuario logueado (el service controla: un Solicitante solo ve el historial de sus solicitudes, T13.3) |

- Reglas que no se resuelven por rol y van en el service, con `SinPermisoException`: un Solicitante solo ve y edita sus propias solicitudes; un Encargado solo carga cotizaciones, fija el precio, rechaza o registra la compra de ítems asignados a él; un aprobador solo decide el paso que le corresponde y nunca sobre su propio pedido.
- El 403 por rol lo devuelve el mapeo de `AccessDeniedException` que agrega T04.4. Verificar que no salga 500.
- Tests: para cada grupo, un caso con el rol correcto (200) y uno con otro rol (403 con `codigo: SIN_PERMISO`).
- **Documentación:**
  - [Spring Security: seguridad por método (`@EnableMethodSecurity`)](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#activate-method-security)
  - [Spring Security: `@PreAuthorize`](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-preauthorize)
  - [Spring Security: autorizar requests HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
  - [Spring Security: cómo se manejan los 401 y 403 (`ExceptionTranslationFilter`)](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-exceptiontranslationfilter)
  - [Spring Security: simular un JWT en tests con MockMvc (`jwt()`)](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
  - [MDN: 403 Forbidden (en español)](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Status/403)

### T05.2 · ABM de sectores (Admin)
- `GET /api/v1/admin/sectores` → lista completa.
- `POST /api/v1/admin/sectores` `{ "nombre" }` → 201. Nombre duplicado (sin distinguir mayúsculas) → 409.
- `PATCH /api/v1/admin/sectores/{id}` `{ "nombre" }` → 200.
- No se borran sectores: los usuarios y solicitudes los referencian.
- **Documentación:**
  - [Spring MVC: mapeo de rutas (`@GetMapping`, `@PathVariable`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Spring MVC: `ResponseEntity` (código HTTP y headers de la respuesta)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)
  - [Spring Boot: validación](https://docs.spring.io/spring-boot/4.1.1/reference/io/validation.html)

### T05.3 · ABM de usuarios (Admin)
- `GET /api/v1/admin/usuarios?q=&rol=&page=` → `Pagina` de usuarios (sin el hash de la contraseña).
- `POST /api/v1/admin/usuarios` `{ "email", "nombre", "sectorId", "rol", "passwordInicial" }` → 201. Email válido y único; contraseña de 8 caracteres o más, guardada cifrada. `rol` puede ser SOLICITANTE, ENCARGADO, SUPERVISOR o ADMIN: así el Admin decide quién es Supervisor (decisión 13: es el rol de una persona que puede variar).
- **Por qué es prerrequisito de las reglas:** para armar la regla `Supervisor → Gerencia General` el Admin tiene que poder crear el sector (T05.2) y poner a alguien como Supervisor o en ese sector (esta subtarea).
- `PATCH /api/v1/admin/usuarios/{id}` `{ "nombre", "sectorId", "rol", "activo" }` (todos opcionales) → 200.
- `POST /api/v1/admin/usuarios/{id}/password` `{ "passwordNueva" }` → 204.
- **Documentación:**
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Security: guardado de contraseñas (BCrypt)](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
  - [OWASP: guardado de contraseñas](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
  - [Hibernate Validator: anotaciones de validación (`@NotNull`, `@Positive`, `@Email`...)](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints)

### T05.4 · Reglas del último Encargado y quitar el rol
- Al desactivar un usuario o cambiarle el rol: si es el único `ENCARGADO` activo (`countByRolAndActivoTrue`), lanzar `ReglaNegocioException("ULTIMO_ENCARGADO", "Asigná otro Encargado antes de desactivar a este")` (caso borde 16).
- Si a un Encargado se le quita el rol o se lo desactiva, publicar el evento `EncargadoDesasignado(eventoId, ocurridoEn, usuarioId, adminId)`.
- Compras escucha ese evento: sus ítems en `EN_COTIZACION` quedan sin encargado (vuelven a la bandeja general, siguen En cotización) y se publica `ItemAsignado` con `encargadoNuevoId = null` para cada uno, así queda en el historial (caso borde 15).
- El mail a los demás Encargados ("los ítems de X volvieron a la bandeja") se hace en T15.2 (backend) y T15.3 (workflow). Esta subtarea solo publica el evento.
- **Documentación:**
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)

---

## T06 · Catálogo de categorías y proveedores (módulo `catalogo`)

### T06.1 · Schema y dominio del catálogo
- **Migración** `V<fecha>_1__catalogo_tablas.sql`: tablas `catalogo.categoria`, `catalogo.proveedor` y `catalogo.proveedor_categoria`, y los índices de §6.3. Incluir el índice único `(tipo, lower(nombre))`.
- **Dominio** en `catalogo/dominio/`:
  - `EstadoCategoria`: NUEVA, CONOCIDA, UNIFICADA.
  - `Categoria`: `id`, `nombre` (obligatorio, hasta 120), `tipo` (`TipoItem`), `estado` (por defecto NUEVA), `unificadaEn` (`@ManyToOne` a Categoria, puede ser null).
  - `Proveedor`: `id`, `razonSocial` (obligatorio, hasta 200), `cuit` (opcional, único, formato `XX-XXXXXXXX-X`), `telefono`, `email` (validado con `@Email`), `contacto`, `activo` (por defecto true), `categorias` (`@ManyToMany` a Categoria sobre la tabla `catalogo.proveedor_categoria`).
- Regla del enunciado: el proveedor **no guarda precios**.
- **Documentación:**
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
  - [Hibernate 7: `@ManyToMany`](https://docs.hibernate.org/orm/7.4/introduction/html_single/#many-to-many)
  - [Hibernate 7: `@ManyToOne`](https://docs.hibernate.org/orm/7.4/introduction/html_single/#many-to-one)
  - [Hibernate 7: enums (`@Enumerated`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#enums)
  - [PostgreSQL: índices sobre expresiones (`lower(nombre)`)](https://www.postgresql.org/docs/current/indexes-expressional.html)
  - [PostgreSQL: índices únicos](https://www.postgresql.org/docs/current/indexes-unique.html)
  - [Hibernate Validator: anotaciones de validación (`@NotNull`, `@Positive`, `@Email`...)](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints)

### T06.2 · Repositories del catálogo
- `CategoriaRepository`:
  - `findByTipoAndNombreIgnoreCase(TipoItem, String)`: para no crear duplicados.
  - `findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(TipoItem, String, EstadoCategoria)`: autocompletado (excluye las UNIFICADAS).
  - `findByEstado(EstadoCategoria, Pageable)`: para que el Encargado vea las Nuevas.
- `ProveedorRepository`:
  - `findByActivoTrueAndCategorias_Id(Long categoriaId)`: proveedores sugeridos.
  - `findByRazonSocialContainingIgnoreCase(String, Pageable)`: buscador.
  - `existsByCuit(String)`.
- **Documentación:**
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Data JPA: métodos de consulta](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)

### T06.3 · ABM de proveedores
- `GET /api/v1/proveedores?q=&categoriaId=&activo=&page=` → `Pagina` de `{ id, razonSocial, cuit, telefono, email, contacto, activo, categorias: [{ id, nombre }] }`.
- `GET /api/v1/proveedores/{id}`.
- `POST /api/v1/proveedores` `{ razonSocial, cuit, telefono, email, contacto, categoriaIds: [] }` → 201. Al menos una categoría; CUIT único (409 si se repite).
- `PATCH /api/v1/proveedores/{id}` → mismos campos, más `activo`, todos opcionales. Dar de baja = `activo: false` (no se borra, por el caso borde 8).
- Tests del service: alta, CUIT duplicado, baja.
- **Documentación:**
  - [Spring MVC: mapeo de rutas (`@GetMapping`, `@PathVariable`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Spring MVC: `@RequestParam`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)
  - [Spring MVC: `ResponseEntity` (código HTTP y headers de la respuesta)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Hibernate Validator: anotaciones de validación (`@NotNull`, `@Positive`, `@Email`...)](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints)

### T06.4 · Búsqueda de categorías
- `GET /api/v1/categorias?tipo=PRODUCTO&q=glo` → hasta 10 `{ id, nombre, tipo, estado }`, sin las UNIFICADAS, ordenadas por nombre. Es lo que usa el autocompletado del formulario.
  - `tipo` es obligatorio cuando viene `q` (una categoría es de productos o de servicios).
  - `q` se recorta con `trim()`. Si tiene menos de 2 letras, devolver lista vacía sin consultar la base (el front llama en cada tecla).
  - Usa `findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot` de T06.2.
- `GET /api/v1/categorias?estado=NUEVA&page=` → `Pagina`, para que el Encargado revise las nuevas.
- Sin `q` ni `estado` → 400 `PARAMETRO_INVALIDO`.
- Dónde va: `catalogo/servicio/CategoriaService.buscar(...)` y `catalogo/web/CategoriaController`. El controller recibe los filtros con `@RequestParam(required = false)` y devuelve DTOs, nunca la entidad.
- Tests: búsqueda sin distinguir mayúsculas ("GLO" encuentra "Globos"); no devuelve UNIFICADAS; `q` de una letra → lista vacía.
- **Documentación:**
  - [Spring MVC: `@RequestParam`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)
  - [Spring Data: `Pageable` en controllers (soporte web)](https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web)

### T06.5 · `CatalogoApi` para otros módulos
- Clase pública `catalogo/CatalogoApi` con:
  - `boolean existeCategoria(Long id)`.
  - `Long obtenerOCrearCategoria(TipoItem tipo, String nombre)`: busca sin distinguir mayúsculas. Si existe, devuelve su id (si está UNIFICADA, devuelve el id de la destino). Si no existe, la crea con estado NUEVA, publica `CategoriaCreada` y devuelve el id nuevo. La usa Solicitudes cuando el usuario escribe una categoría a mano.
  - `String nombreCategoria(Long id)`.
  - `ProveedorResumen obtenerProveedor(Long id)`: id, razón social, activo. `ProveedorResumen` es un record público en la raíz de `catalogo/`.
  - `List<ProveedorResumen> proveedoresActivosDe(Long categoriaId)`.
- La necesita T07.4 en S1, por eso vence antes que el resto del catálogo.
- **Documentación:**
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Spring: publicar eventos con `ApplicationEventPublisher`](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)

### T06.6 · Confirmar una categoría Nueva
- `PATCH /api/v1/categorias/{id}` `{ "estado": "CONOCIDA" }` → 200 con `{ id, nombre, tipo, estado }`. Solo de NUEVA a CONOCIDA; otro cambio → 422 `TRANSICION_INVALIDA`. Si no existe → 404.
- Opcional en el mismo body: `nombre`, para corregir cómo lo escribió el solicitante ("globos" → "Globos"). Si el nombre nuevo choca con otra categoría del mismo tipo, 409 `DUPLICADO` (lo dispara el índice único de T06.1 y lo traduce el manejador de T03.2). En ese caso lo que corresponde es unificar (T06.8).
- La regla va en la entidad: `Categoria.confirmar()` lanza `ReglaNegocioException("TRANSICION_INVALIDA", ...)` si el estado no es NUEVA.
- Solo `ENCARGADO` (T05.1).
- Tests: NUEVA → CONOCIDA pasa; CONOCIDA → CONOCIDA da 422; UNIFICADA → CONOCIDA da 422.
- **Documentación:**
  - [Spring MVC: mapeo de rutas (`@GetMapping`, `@PathVariable`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [MDN: 422 Unprocessable Content](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/422)

### T06.7 · Proveedores sugeridos
- `proveedoresActivosDe` de `CatalogoApi` (T06.5) la usa Compras en T10.5. Acá hay que asegurar que funcione con categorías unificadas: si se pide una categoría UNIFICADA, devolver los proveedores de la destino.
- Cómo: buscar la categoría; si está UNIFICADA, usar el id de `unificadaEn`. Alcanza con un salto porque T06.8 no deja unificar contra una categoría que ya está UNIFICADA. Después, `ProveedorRepository.findByActivoTrueAndCategorias_Id(id)`, ordenado por razón social.
- Por qué pasa esto: un ítem viejo guarda el `categoriaId` que tenía al crearse (caso borde 9). Si después esa categoría se unificó, el Encargado igual tiene que ver los proveedores correctos.
- Si no hay ninguno, devuelve lista vacía: el front ofrece dar de alta un proveedor (caso borde 10).
- Si la categoría no existe → lista vacía (no es un error para quien llama).
- Tests: categoría con 2 proveedores activos y 1 inactivo → devuelve 2; categoría UNIFICADA → devuelve los de la destino; sin proveedores → lista vacía.
- **Documentación:**
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)

### T06.8 · Unificar categorías duplicadas
- `POST /api/v1/categorias/{id}/unificar` `{ "destinoId" }` → 200. Mismo tipo; la destino no puede estar UNIFICADA.
- La categoría de origen queda UNIFICADA con `unificadaEn` = destino. Los proveedores que tenía pasan a tener la destino (sin duplicar filas).
- Los ítems viejos **no se modifican** (caso borde 9: no se reescribe el pasado).
- Publicar `CategoriaUnificada(eventoId, ocurridoEn, origenId, destinoId, usuarioId)`. Reportes la escucha para agrupar bajo la destino (T16.2).
- **Documentación:**
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring Data JPA: consultas de modificación (`@Modifying`)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.modifying-queries)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

---

## T07 · Solicitudes: crear y consultar (módulo `solicitudes`)

### T07.1 · Schema y dominio de solicitudes
- **Migración** `V<fecha>_1__solicitudes_tablas.sql`: `solicitudes.solicitud` y `solicitudes.item` como en §6.2, con estos cambios en `item` (decisiones 8, 9 y 12):
  - El `CHECK` de `estado` es `PENDIENTE, EN_COTIZACION, COMPRADO, RECHAZADO, CANCELADO` (sin APROBADO).
  - `precio_estimado NUMERIC(16,2) NOT NULL CHECK (precio_estimado > 0)` y `moneda_estimada CHAR(3) NOT NULL CHECK (moneda_estimada IN ('ARS','USD'))`: lo carga el solicitante.
  - `precio_total NUMERIC(16,2)` y `moneda_total CHAR(3)`, ambos nulos hasta que el Encargado fije el precio (copia que mantiene el listener de T08.5).
  - `estado_aprobacion VARCHAR(10) NOT NULL DEFAULT 'EN_CURSO' CHECK (estado_aprobacion IN ('EN_CURSO','APROBADA'))`.
  - `proveedor_comprado VARCHAR(200)`, `motivo_rechazo TEXT`, `rechazado_por VARCHAR(200)` (texto para mostrar: "Encargado de compras", "Supervisor" o "Gerencia General") y `orden_enviada BOOLEAN NOT NULL DEFAULT FALSE`.
  - La tabla de adjuntos se hace en T14.2.
- **Dominio** en `solicitudes/dominio/`:
  - `EstadoSolicitud`: EN_REVISION, LISTA, CERRADA, CANCELADA.
  - `EstadoItem`: PENDIENTE, EN_COTIZACION, COMPRADO, RECHAZADO, CANCELADO. Método `esFinal()`: true para COMPRADO, RECHAZADO y CANCELADO.
  - `EstadoAprobacion` (EN_CURSO, APROBADA) vive en `compartido/tipos/` porque también lo usan Compras y Aprobaciones.
  - `Solicitud`: `id`, `solicitanteId`, `sectorId`, `urgencia`, `fechaNecesaria`, `observaciones`, `estado` (por defecto EN_REVISION), `creadaEn`, `version` (`@Version`), `items` (`@OneToMany(mappedBy = "solicitud", cascade = ALL, orphanRemoval = true)`).
  - `Item`: `id`, `solicitud` (`@ManyToOne`), `tipo`, `categoriaId`, `detalle`, `cantidad` (`BigDecimal`, 12 dígitos con 2 decimales, mayor a 0), `precioEstimado` y `monedaEstimada` (`Moneda`), `precioTotal` y `monedaTotal` (opcionales), `estado` (por defecto PENDIENTE), `estadoAprobacion` (por defecto EN_CURSO), `creadoEn`, `version`, `proveedorComprado`, `motivoRechazo`, `rechazadoPor`, `ordenEnviada`.
- **Reglas dentro de las entidades** (así se testean sin base de datos):
  - `Solicitud.agregarItem(...)`.
  - `Item.puedeEditarse()`: solo si está PENDIENTE (aunque la cadena de aprobación ya haya empezado: editar la reinicia, T23.4).
  - `Solicitud.puedeCancelarse()`: solo si todos los ítems están PENDIENTES.
  - `Solicitud.recalcularEstado()`: se implementa en T08.6.
- Test unitario: una solicitud sin ítems no es válida; un ítem con cantidad 0 no es válido; un ítem sin precio estimado, o con precio estimado 0, no es válido.
- **Documentación:**
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
  - [Hibernate 7: relaciones entre entidades](https://docs.hibernate.org/orm/7.4/introduction/html_single/#associations)
  - [Hibernate 7: atributo de versión (`@Version`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#version-attributes)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [Java: `BigDecimal`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html)
  - [PostgreSQL: tipos numéricos (`NUMERIC`)](https://www.postgresql.org/docs/current/datatype-numeric.html)

### T07.2 · Repositories de solicitudes
- En `solicitudes/repositorio/`:
  - `SolicitudRepository extends JpaRepository<Solicitud, Long>`:
    - `Page<Solicitud> findBySolicitanteId(Long solicitanteId, Pageable)`: "mis solicitudes".
    - `Page<Solicitud> findBySolicitanteIdAndEstado(Long solicitanteId, EstadoSolicitud, Pageable)`: con filtro de estado.
    - `Page<Solicitud> findByEstado(EstadoSolicitud, Pageable)`: para el Encargado o el Admin (T07.5).
    - `@EntityGraph(attributePaths = "items") Optional<Solicitud> findConItemsById(Long id)`: trae la solicitud y sus ítems en una sola consulta para el detalle. Sin esto, JPA hace una consulta más al recorrer `items`.
  - `ItemRepository extends JpaRepository<Item, Long>`:
    - `Optional<Item> findByIdAndSolicitudId(Long itemId, Long solicitudId)`: para T08.1 y T08.2. Así un ítem de otra solicitud da 404 aunque el id exista.
- El orden "más nuevas primero" no va en el nombre del método: se pasa en el `Pageable` (`Sort.by("creadaEn").descending()`), o con `@PageableDefault(sort = "creadaEn", direction = DESC)` en el controller.
- Test `@DataJpaTest` (con `@Import(TestcontainersConfiguration.class)` y `@AutoConfigureTestDatabase(replace = NONE)`, para que use el Postgres de Testcontainers): guardar una solicitud con 2 ítems, buscarla con `findConItemsById` y verificar que vienen los 2.
- **Documentación:**
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)
  - [Spring Data JPA: `@EntityGraph`](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph)
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Boot: tests de repositories con `@DataJpaTest`](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa)

### T07.3 · Definir los eventos del sistema
En `compartido/eventos/`, un `record` por evento. Todos empiezan con `UUID eventoId` y `Instant ocurridoEn`. Los datos que lleva cada uno son los que necesitan sus listeners, para que no tengan que consultar otro módulo:

| Evento | Publica | Escuchan | Datos (además de `eventoId` y `ocurridoEn`) |
|---|---|---|---|
| `SolicitudCreada` | Solicitudes | Compras, Aprobaciones, Integraciones, Auditoría | `solicitudId`, `solicitanteId`, `sectorId`, `urgencia`, `fechaNecesaria`, `items`: lista de `ItemCreado(itemId, tipo, categoriaId, cantidad, detalle, precioEstimado, monedaEstimada)` |
| `ItemEditado` | Solicitudes | Compras, Aprobaciones, Auditoría | `itemId`, `solicitudId`, `categoriaId`, `cantidad`, `detalle`, `precioEstimado`, `monedaEstimada`, `usuarioId` |
| `ItemCancelado` | Solicitudes | Compras, Aprobaciones, Auditoría | `itemId`, `solicitudId`, `usuarioId` |
| `SolicitudCancelada` | Solicitudes | Compras, Aprobaciones, Auditoría | `solicitudId`, `itemIds`, `usuarioId` |
| `SolicitudLista` | Solicitudes | Integraciones, Auditoría | `solicitudId`, `solicitanteId`, `resultados`: lista de `ResultadoItem(itemId, categoriaId, detalle, estado, proveedorRazonSocial, motivoRechazo, rechazadoPor)` |
| `SolicitudCerrada` | Solicitudes | Auditoría | `solicitudId` |
| `CategoriaCreada` | Catálogo | Auditoría | `categoriaId`, `nombre`, `tipo` |
| `CategoriaUnificada` | Catálogo | Reportes, Auditoría | `origenId`, `destinoId`, `usuarioId` |
| `ItemAsignado` | Compras | Solicitudes, Auditoría | `itemId`, `solicitudId`, `encargadoAnteriorId` (null si nadie lo tenía), `encargadoNuevoId` (null si queda sin asignar) |
| `CancelacionRechazada` | Compras | Solicitudes | `itemId`, `solicitudId` (el ítem ya estaba tomado cuando llegó la cancelación) |
| `CotizacionCargada` | Compras | Auditoría | `cotizacionId`, `itemId`, `proveedorId`, `moneda`, `precioTotal`, `usuarioId` |
| `PrecioTotalActualizado` | Compras | Aprobaciones, Solicitudes, Integraciones, Auditoría | `itemId`, `solicitudId`, `precioTotal` (null si el Encargado lo quitó), `moneda`, `cotizacionId` (null si se cargó a mano), `proveedorRazonSocial` (null si se cargó a mano), `usuarioId` |
| `PasoAprobacionActivado` | Aprobaciones | Integraciones, Auditoría | `itemId`, `solicitudId`, `pasoId`, `tipoAprobador` (ROL o SECTOR), `rol`, `sectorId`, `aprobadorNombre` ("Supervisor", "Gerencia General"), `monto`, `moneda` |
| `PasoDecidido` | Aprobaciones | Integraciones, Auditoría | `itemId`, `solicitudId`, `pasoId`, `decision` (APROBADO o RECHAZADO), `usuarioId`, `motivo` |
| `AprobacionCompletada` | Aprobaciones | Compras, Solicitudes, Auditoría | `itemId`, `solicitudId`, `automatica` (true si la regla no pedía ningún paso) |
| `AprobacionReabierta` | Aprobaciones | Compras, Solicitudes, Auditoría | `itemId`, `solicitudId`, `motivo` (PRECIO_CAMBIO o EDICION) |
| `AprobacionRechazada` | Aprobaciones | Compras, Auditoría | `itemId`, `solicitudId`, `pasoId`, `usuarioId`, `aprobadorNombre`, `motivo` |
| `ReglaModificada` | Aprobaciones | Auditoría | `reglaId`, `nombre`, `accion` (CREADA, EDITADA, ACTIVADA o DESACTIVADA), `usuarioId` |
| `CompraRegistrada` | Compras | Órdenes, Auditoría | `itemId`, `solicitudId`, `sectorId`, `categoriaId`, `tipo`, `detalle`, `cantidad`, `proveedorId`, `proveedorRazonSocial`, `cotizacionId` (null si se compró con el precio cargado a mano), `moneda`, `precioTotal`, `encargadoId` |
| `CompraActualizada` | Compras | Órdenes, Reportes, Auditoría | `itemId`, `solicitudId`, `precioTotalAnterior`, `precioTotal`, `montoEnvioAnterior`, `montoEnvio`, `moneda`, `usuarioId` |
| `ItemResuelto` | Compras | Solicitudes, Reportes, Aprobaciones, Auditoría | `itemId`, `solicitudId`, `resultado` (**COMPRADO** o RECHAZADO), `proveedorRazonSocial`, `motivoRechazo`, `rechazadoPor` ("Encargado de compras" o el nombre del paso que rechazó), `urgencia`, `solicitadoEn`, `resueltoEn`, `encargadoId` |
| `OrdenGenerada` | Órdenes | Reportes, Auditoría | `ordenId`, `numero`, `itemId`, `solicitudId`, `sectorId`, `categoriaId`, `proveedorId`, `moneda`, `precioTotal` |
| `OrdenEnviada` | Órdenes | Solicitudes, Auditoría | `ordenId`, `itemId`, `solicitudId`, `usuarioId` |
| `EncargadoDesasignado` | Auth | Compras, Auditoría | `usuarioId`, `adminId` |

- `ItemAprobado` ya no existe (decisión 10): la orden se genera con `CompraRegistrada`. `AprobacionCompletada` solo habilita la compra.
- Quién rechaza qué: si rechaza el Encargado, Compras publica `ItemResuelto` (RECHAZADO) y Aprobaciones lo escucha para cancelar los pasos pendientes. Si rechaza un aprobador, Aprobaciones publica `AprobacionRechazada` y Compras lo escucha, rechaza la gestión y publica `ItemResuelto`. Así `ItemResuelto` tiene un solo publicador.
- Para publicar: inyectar `ApplicationEventPublisher` en el service y llamar a `publishEvent(...)` dentro del método `@Transactional`. Modulith guarda el evento en `event_publication` en la misma transacción.
- Para escuchar: un método con `@ApplicationModuleListener` en un componente del módulo que escucha. Corre después del commit, en otra transacción y en otro hilo.
- Esta subtarea es solo definir los records: la publican y escuchan las demás subtareas.
- **Documentación:**
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring: publicar eventos con `ApplicationEventPublisher`](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
  - [Java: records](https://dev.java/learn/records/)

### T07.4 · Crear una solicitud
- **Endpoint** `POST /api/v1/solicitudes` con body (DTO con `@Valid`):
  ```
  { urgencia, fechaNecesaria, observaciones,
    items: [ { tipo, categoriaId | categoriaNueva, cantidad, detalle, precioEstimado, monedaEstimada } ] }
  ```
  - `urgencia`, `fechaNecesaria` y al menos un ítem son obligatorios. `fechaNecesaria` no puede ser anterior a hoy.
  - Cada ítem tiene `tipo`, `cantidad` > 0, **exactamente uno** de `categoriaId` o `categoriaNueva`, y `precioEstimado` > 0 con su `monedaEstimada` (ARS o USD). El precio estimado es el total del ítem, no por unidad (decisión 8): lo usa la regla de aprobación mientras el Encargado no cargue el precio total.
- **Service** `SolicitudService.crear(...)`, `@Transactional`:
  1. Solicitante = `UsuarioActual.id()`; sector = `AuthApi.sectorDe(solicitanteId)`. El sector se copia, así si el usuario cambia de sector los reportes viejos no cambian (caso borde 17).
  2. Por cada ítem: si trae `categoriaId`, verificar `CatalogoApi.existeCategoria` (si no existe, 422 `CATEGORIA_INEXISTENTE`). Si trae `categoriaNueva`, usar `CatalogoApi.obtenerOCrearCategoria(tipo, nombre)`.
  3. Guardar la solicitud con sus ítems.
  4. Publicar `SolicitudCreada`.
- **Respuesta:** 201, header `Location: /api/v1/solicitudes/{id}`, body `{ id, estado, items: [{ id, estado }] }` (ejemplo en §7.1).
- **Tests:** crear con 2 ítems (uno con categoría nueva) y verificar que se publicó el evento (con `Scenario` o `@RecordApplicationEvents`) con `precioEstimado` y `monedaEstimada`; sin ítems → 400; cantidad 0 → 400; sin precio estimado o precio 0 → 400; categoría inexistente → 422.
- **Documentación:**
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Hibernate Validator: anotaciones de validación (`@NotNull`, `@Positive`, `@Email`...)](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints)
  - [Hibernate Validator: validaciones sobre toda la clase ("exactamente uno de dos campos")](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-class-level-constraints)
  - [Spring MVC: `ResponseEntity` (código HTTP y headers de la respuesta)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)
  - [Spring: `@RecordApplicationEvents` en tests](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/application-events.html)

### T07.5 · Consultar solicitudes
- `GET /api/v1/solicitudes?mias=true&estado=&page=` → `Pagina` de `{ id, creadaEn, urgencia, fechaNecesaria, estado, cantidadItems }`, más nuevas primero.
  - Un Solicitante siempre ve solo las suyas, aunque mande `mias=false`.
  - Un Encargado o Admin con `mias=false` ve todas.
- `GET /api/v1/solicitudes/{id}` → `{ id, solicitante: { id, nombre }, sector, urgencia, fechaNecesaria, observaciones, estado, creadaEn, version, items: [{ id, tipo, categoriaId, categoriaNombre, detalle, cantidad, precioEstimado, monedaEstimada, precioTotal, monedaTotal, estado, estadoAprobacion, proveedorComprado, motivoRechazo, rechazadoPor, version }] }`. La cadena de pasos de cada ítem no viene acá: el front la pide a `GET /api/v1/aprobaciones/items/{itemId}` (T23.6).
  - Si es un Solicitante y la solicitud no es suya → 403. Si no existe → 404.
  - Nombres de categoría y solicitante vía `CatalogoApi` y `AuthApi`.
- **Documentación:**
  - [Spring MVC: `@RequestParam`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Data JPA: `@EntityGraph`](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph)
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)

### T07.6 · Tests del módulo Solicitudes
- Tests de endpoints con MockMvc para T07.4 y T07.5, incluyendo: un Solicitante no puede ver la solicitud de otro (403).
- Un `@ApplicationModuleTest` del módulo `solicitudes` que cree una solicitud y verifique con `Scenario` que se publicó `SolicitudCreada` con los datos correctos.
- **Documentación:**
  - [Spring: MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html)
  - [Spring Boot: tests de la capa web con `@WebMvcTest`](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)
  - [Spring Security: simular un JWT en tests con MockMvc (`jwt()`)](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
  - [Spring Modulith: tests de módulos (`@ApplicationModuleTest`)](https://docs.spring.io/spring-modulith/reference/testing.html)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)

---

## T08 · Solicitudes: editar, cancelar y estado automático

### T08.1 · Editar un ítem Pendiente
- `PATCH /api/v1/solicitudes/{id}/items/{itemId}` `{ categoriaId | categoriaNueva, cantidad, detalle, precioEstimado, monedaEstimada, version }` → 200 con el ítem actualizado.
- Reglas: solo el dueño (si no, 403); solo si el ítem está PENDIENTE (si no, 422 `ITEM_NO_PENDIENTE`, "Este ítem ya está siendo cotizado"); la `version` tiene que coincidir (si no, 409); `precioEstimado` > 0.
- Publicar `ItemEditado` (con el precio estimado). Aprobaciones lo escucha y **reinicia la cadena** (T23.4): si algún paso ya había aprobado, el aprobador tiene que volver a mirar el ítem editado. El front avisa al editar un ítem con pasos ya aprobados.
- **Documentación:**
  - [Spring MVC: mapeo de rutas (`@GetMapping`, `@PathVariable`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [MDN: 409 Conflict](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/409)
  - [MDN: 422 Unprocessable Content](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/422)

### T08.2 · Borrar (cancelar) un ítem Pendiente
- `DELETE /api/v1/solicitudes/{id}/items/{itemId}?version=` → 204.
- No se borra la fila: el ítem pasa a CANCELADO. Mismas reglas que T08.1.
- Publicar `ItemCancelado` y recalcular el estado de la solicitud (si todos quedaron cancelados, la solicitud pasa a CANCELADA, caso borde 12).
- **Documentación:**
  - [Spring MVC: mapeo de rutas (`@GetMapping`, `@PathVariable`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

### T08.3 · Cancelar la solicitud entera
- `POST /api/v1/solicitudes/{id}/cancelar` `{ version }` → 200.
- Solo si **todos** los ítems están PENDIENTES. Si no, 422 `SOLICITUD_NO_CANCELABLE`, con un mensaje que indique cancelar ítem por ítem (caso borde 3).
- Todos los ítems pasan a CANCELADO y la solicitud a CANCELADA. Publicar `SolicitudCancelada`.
- **Documentación:**
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

### T08.4 · Sincronizar ediciones y cancelaciones con Compras
- Compras tiene su propia copia de cada ítem (`gestion_item`) y los eventos llegan unos milisegundos después. En ese lapso, un Encargado puede tomar un ítem que el solicitante acaba de cancelar.
- Aprobaciones también tiene su copia (`aprobacion_item`) y escucha los mismos tres eventos, pero eso se hace en T23.4.
- En `compras/`, listeners:
  - `ItemEditado` → actualizar la copia (categoría, cantidad, detalle, precio estimado) solo si sigue PENDIENTE.
  - `ItemCancelado` y `SolicitudCancelada` → si la gestión sigue PENDIENTE, pasa a CANCELADO. Si ya estaba EN_COTIZACION (la carrera), publicar `CancelacionRechazada`.
- En `solicitudes/`, listener de `CancelacionRechazada`: el ítem vuelve a EN_COTIZACION y la solicitud a EN_REVISION.
- Test: con `Scenario`, cancelar un ítem cuya gestión ya está tomada y verificar que vuelve a EN_COTIZACION.
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)
  - [Spring: eventos ligados a una transacción](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)

### T08.5 · Actualizar el estado de los ítems desde Compras
- Listener de `ItemAsignado`: si el ítem estaba PENDIENTE, pasa a EN_COTIZACION.
- Listeners de `PrecioTotalActualizado` y `CompraActualizada`: guardar `precioTotal` y `monedaTotal` en el ítem (null si el Encargado lo quitó), para mostrarlos sin consultar Compras.
- Listeners de `AprobacionCompletada` y `AprobacionReabierta`: `estadoAprobacion` pasa a APROBADA o a EN_CURSO. No cambian `estado`: "listo para comprar" es EN_COTIZACION + APROBADA (decisión 9).
- Listener de `ItemResuelto`: pasa a COMPRADO (guardando `proveedorComprado`) o RECHAZADO (guardando `motivoRechazo` y `rechazadoPor`). Después, recalcular el estado de la solicitud (T08.6).
- Listener de `OrdenEnviada`: marcar `ordenEnviada = true` en el ítem y recalcular.
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

### T08.6 · Estado automático de la solicitud
- `Solicitud.recalcularEstado()` en el dominio, con estas reglas (enunciado "Estados de una solicitud" y casos borde 12 a 14):
  1. Todos los ítems CANCELADOS → CANCELADA. No se avisa como resuelta.
  2. Todos los ítems en estado final (COMPRADO, RECHAZADO o CANCELADO), y al menos uno no cancelado → LISTA. Un ítem aprobado que todavía no se compró **no** es final (decisión 9).
  3. LISTA, y todos los ítems COMPRADOS con `ordenEnviada = true` → CERRADA. Si no hay ningún comprado (todos rechazados), pasa a CERRADA enseguida.
  4. Si no, EN_REVISION.
- Cuando pasa a LISTA, el service publica `SolicitudLista` (una sola vez) con el resultado de cada ítem. Cuando pasa a CERRADA, publica `SolicitudCerrada`.
- Tests unitarios de `recalcularEstado()` para cada caso: todos cancelados; comprados y rechazados mezclados; todos rechazados; comprados sin orden enviada; comprados con orden enviada; un ítem con cadena APROBADA pero sin comprar mantiene la solicitud en EN_REVISION.
- **Documentación:**
  - [Java: enums](https://dev.java/learn/classes-objects/enums/)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Spring: publicar eventos con `ApplicationEventPublisher`](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)

### T08.7 · Evitar solicitudes duplicadas por doble click
- Problema: si el usuario hace doble click en "Enviar", o la red corta la respuesta y el front reintenta, se crean dos solicitudes iguales (diseño §7: "Las creaciones aceptan el header `Idempotency-Key`").
- `POST /api/v1/solicitudes` acepta el header opcional `Idempotency-Key` (un UUID que genera el front al abrir el formulario, T18.1). En el controller: `@RequestHeader(value = "Idempotency-Key", required = false) UUID clave`.
- **Migración** `V<fecha>_1__solicitudes_idempotencia.sql`: tabla `solicitudes.clave_idempotencia (clave UUID PRIMARY KEY, solicitante_id BIGINT NOT NULL, solicitud_id BIGINT NOT NULL REFERENCES solicitudes.solicitud(id), creada_en TIMESTAMPTZ NOT NULL DEFAULT now())`.
- **En `SolicitudService.crear`** (dentro de la misma transacción):
  1. Si viene clave y ya existe para este solicitante → no crear nada: devolver la solicitud ya creada, con 200 en vez de 201.
  2. Si no existe → crear la solicitud como siempre y guardar la clave con su `solicitud_id`.
  3. Si la misma clave llega de otro usuario → 422 `CLAVE_IDEMPOTENCIA_INVALIDA` (no se le muestra la solicitud de otro).
- **Carrera:** si los dos requests llegan a la vez, los dos ven que la clave no existe. El segundo en guardar choca con la `PRIMARY KEY` y la transacción entera se deshace (no queda una solicitud duplicada). Ese segundo request recibe 409 `DUPLICADO` (T03.2) y el front, al recargar, ve una sola solicitud.
- Sin header funciona igual que antes (la clave es opcional).
- Tests: el mismo POST dos veces con la misma clave → una sola solicitud y la segunda respuesta trae el mismo id; con claves distintas → dos solicitudes.
- **Documentación:**
  - [IETF: header `Idempotency-Key`](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)
  - [Spring MVC: `@RequestHeader`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestheader.html)
  - [PostgreSQL: tipo `UUID`](https://www.postgresql.org/docs/current/datatype-uuid.html)
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring MVC: `ResponseEntity` (código HTTP y headers de la respuesta)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)

---

## T09 · Compras: bandeja y asignación (módulo `compras`)

### T09.1 · Schema y dominio de Compras
- **Migración** `V<fecha>_1__compras_gestion_item.sql`: `compras.gestion_item` como en §6.4, más `sector_id BIGINT NOT NULL`, `tipo VARCHAR(10) NOT NULL`, `detalle TEXT`, `prioridad SMALLINT NOT NULL` y `solicitado_en TIMESTAMPTZ NOT NULL`. El índice de la bandeja pasa a ser `(estado, prioridad, fecha_necesaria)`. Cambios por las decisiones 8 a 10 (el diseño §6.4 todavía no los tiene):
  - `estado` acepta `PENDIENTE, EN_COTIZACION, COMPRADO, RECHAZADO, CANCELADO`.
  - `precio_estimado NUMERIC(16,2) NOT NULL` y `moneda_estimada CHAR(3) NOT NULL` (copiados de la solicitud), `precio_total NUMERIC(16,2)` y `moneda_total CHAR(3)` (los carga el Encargado, T10.3) y `monto_envio NUMERIC(14,2)` (T10.8).
  - `estado_aprobacion VARCHAR(10) NOT NULL DEFAULT 'EN_CURSO'` (copia que mantiene el listener de T10.7, para filtrar "Para comprar" sin consultar Aprobaciones).
  - `proveedor_compra_id BIGINT` y `comprado_en TIMESTAMPTZ`.
  - Los `CHECK` de §6.4 pasan a ser: `estado <> 'COMPRADO' OR (precio_total IS NOT NULL AND proveedor_compra_id IS NOT NULL AND estado_aprobacion = 'APROBADA')` y `estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL`. Con esto, "comprado = tiene precio, proveedor y cadena aprobada" lo garantiza la base y no solo el código.
  - `cotizacion_elegida_id` queda opcional: se puede comprar con un precio total cargado a mano.
- **Dominio** en `compras/dominio/`:
  - `EstadoGestion`: PENDIENTE, EN_COTIZACION, COMPRADO, RECHAZADO, CANCELADO.
  - `GestionItem`: `itemId` (clave primaria, sin autogenerar: es el id del ítem de Solicitudes), `solicitudId`, `sectorId`, `categoriaId`, `tipo`, `detalle`, `cantidad`, `urgencia`, `prioridad`, `fechaNecesaria`, `solicitadoEn`, `precioEstimado`, `monedaEstimada`, `precioTotal`, `monedaTotal`, `montoEnvio`, `estado`, `estadoAprobacion`, `encargadoId`, `cotizacionElegidaId`, `proveedorCompraId`, `compradoEn`, `motivoRechazo`, `resueltoEn`, `ultimoRecordatorioEn`, `version` (`@Version`).
- **Por qué `prioridad`:** si se ordena por el texto de la urgencia, sale ALTA, BAJA, MEDIA (orden alfabético). Con `prioridad` (1 = Alta, 2 = Media, 3 = Baja, de `Urgencia.prioridad()`) el orden es el correcto.
- **Reglas en la entidad:** `tomar(encargadoId)`, `reasignar(encargadoId)`, `fijarPrecioTotal(importe, moneda, cotizacionId)`, `registrarCompra(proveedorId, ...)`, `corregirCompra(...)` y `rechazar(motivo)`, cada una validando el estado actual y lanzando `ReglaNegocioException` si no corresponde. `registrarCompra` exige `estado = EN_COTIZACION` y `estadoAprobacion = APROBADA`.
- **Documentación:**
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
  - [Hibernate 7: atributo de versión (`@Version`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#version-attributes)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [PostgreSQL: índices parciales](https://www.postgresql.org/docs/current/indexes-partial.html)
  - [Hibernate 7: enums (`@Enumerated`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#enums)

### T09.2 · Crear las gestiones al entrar una solicitud
- En `compras/servicio/`, listener `@ApplicationModuleListener` de `SolicitudCreada`: por cada ítem crea una `GestionItem` en PENDIENTE y `estadoAprobacion` EN_CURSO, copiando `solicitudId`, `sectorId`, `categoriaId`, `tipo`, `detalle`, `cantidad`, `precioEstimado`, `monedaEstimada`, `urgencia`, `prioridad`, `fechaNecesaria` y `solicitadoEn` (= `ocurridoEn` del evento).
- **Toda solicitud entra a la bandeja**, sin esperar ninguna aprobación: la cadena corre en paralelo (decisión 7).
- Idempotente: si ya existe la gestión de ese `itemId`, no hace nada.
- Test con `Scenario`: publicar `SolicitudCreada` con 2 ítems y verificar que se crearon 2 gestiones.
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)

### T09.3 · Bandeja
- `GET /api/v1/bandeja?asignadosAMi=&urgencia=&paraComprar=&page=` → `Pagina` de `{ itemId, solicitudId, tipo, categoriaId, categoriaNombre, detalle, cantidad, urgencia, fechaNecesaria, precioEstimado, monedaEstimada, precioTotal, monedaTotal, estado, estadoAprobacion, encargadoId, encargadoNombre, version }`.
- Solo ítems en PENDIENTE o EN_COTIZACION, ordenados por `prioridad` y después por `fechaNecesaria` (enunciado: "dentro de la misma urgencia, primero aparece lo que se necesita antes").
- `asignadosAMi=true` filtra por `encargadoId = UsuarioActual.id()`.
- `paraComprar=true` trae solo los ítems EN_COTIZACION con `estadoAprobacion = APROBADA`: es la pestaña "Para comprar" (la lista de "ítems aprobados" de la decisión 10).
- `GET /api/v1/bandeja/{itemId}` → la gestión más sus cotizaciones (T10.2).
- **Documentación:**
  - [Spring MVC: `@RequestParam`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Data JPA: consultas con `@Query` (JPQL y nativas)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query)
  - [PostgreSQL: índices parciales](https://www.postgresql.org/docs/current/indexes-partial.html)

### T09.4 · Tomar y reasignar
- `POST /api/v1/bandeja/{itemId}/tomar` → 200. Se permite si está PENDIENTE, o EN_COTIZACION sin encargado (caso T05.4). Pasa a EN_COTIZACION con `encargadoId` = yo. Si está asignado a otro → 409 `YA_ASIGNADO`.
- `POST /api/v1/bandeja/{itemId}/reasignar` → 200. Solo si está EN_COTIZACION y asignado a otro Encargado. Pasa a ser mío.
- Los dos publican `ItemAsignado` (con el encargado anterior y el nuevo).
- **Concurrencia (caso borde 1):** si dos Encargados toman el mismo ítem a la vez, `@Version` hace que el segundo `save` falle con `ObjectOptimisticLockingFailureException`, y el manejador de T03.2 la convierte en 409.
- **Documentación:**
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [Hibernate 7: atributo de versión (`@Version`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#version-attributes)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [MDN: 409 Conflict](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/409)

### T09.5 · Test de concurrencia
- Test de integración: dos hilos llaman a `tomar` sobre el mismo ítem al mismo tiempo (con un `CountDownLatch` para que arranquen juntos). Uno termina bien y el otro recibe la excepción de versión; el ítem queda asignado a uno solo.
- Cómo armarlo:
  - `@SpringBootTest` con `@Import(TestcontainersConfiguration.class)`: tiene que ser una base real, porque el choque de versiones lo detecta el `UPDATE ... WHERE version = ?` en Postgres.
  - **Sin** `@Transactional` en el test: cada hilo tiene que tener su propia transacción, que abre el service. Si el test es transaccional, los dos hilos no ven los cambios del otro y el test no prueba nada.
  - Preparar una `GestionItem` PENDIENTE guardada con el repository.
  - Dos Encargados distintos (ids 1 y 2). Como `UsuarioActual` lee el token, en el test conviene llamar a un método del service que reciba el `encargadoId` como parámetro, o simular el usuario en cada hilo con `SecurityContextHolder`.
  - `ExecutorService` con 2 hilos; cada uno hace `latch.await()` y después `servicio.tomar(itemId)`. Guardar el resultado de cada uno (bien o excepción).
  - Verificar: exactamente uno terminó bien, el otro lanzó `ObjectOptimisticLockingFailureException` (o el 409 `YA_ASIGNADO` si leyó después del commit del primero), y en la base el `encargadoId` es el del que ganó.
- Repetir el test varias veces (`@RepeatedTest(10)`) para que un resultado correcto no dependa de la suerte.
- **Documentación:**
  - [Java: `CountDownLatch`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/CountDownLatch.html)
  - [Java: `ExecutorService`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html)
  - [Spring Boot: testing de aplicaciones](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html)
  - [Spring Boot: Testcontainers](https://docs.spring.io/spring-boot/4.1.1/reference/testing/testcontainers.html)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)

---

## T10 · Compras: cotizaciones, precio total, rechazo y compra

### T10.1 · Schema y dominio de cotizaciones
- **Migración** `V<fecha>_1__compras_cotizaciones.sql`: `compras.cotizacion` como en §6.4, con el índice único parcial `ux_una_seleccionada`. La tabla de adjuntos de cotización va en T14.3.
- **Dominio:**
  - `EstadoCotizacion`: CANDIDATA, SELECCIONADA, DESCARTADA.
  - `Cotizacion`: `id`, `itemId`, `proveedorId`, `proveedorRazonSocial` (copiado), `moneda`, `precioUnitario`, `precioTotal`, `fechaValidez`, `entregaEstimada`, `observaciones`, `estado`, `cargadaPor`, `cargadaEn`.
  - Método `estaVigente(LocalDate hoy)`: `fechaValidez` mayor o igual a hoy. Hoy se calcula en hora de Argentina (`ZoneId.of("America/Argentina/Buenos_Aires")`, caso borde 19).
- Agregar `proveedor_razon_social` a la migración (el diseño no lo tiene; evita consultar el catálogo cada vez que se listan cotizaciones).
- Repository: `CotizacionRepository.findByItemIdOrderByCargadaEnDesc(Long)`.
- **Documentación:**
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
  - [PostgreSQL: índices parciales](https://www.postgresql.org/docs/current/indexes-partial.html)
  - [PostgreSQL: índices únicos](https://www.postgresql.org/docs/current/indexes-unique.html)
  - [Java: `ZoneId` (zonas horarias)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/ZoneId.html)
  - [Java: `BigDecimal`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html)

### T10.2 · Cargar y listar cotizaciones
- `POST /api/v1/bandeja/{itemId}/cotizaciones` `{ proveedorId, moneda, precioUnitario, fechaValidez, entregaEstimada, observaciones }` → 201.
  - El ítem tiene que estar EN_COTIZACION y asignado a mí (si no, 422 o 403).
  - El proveedor tiene que existir y estar activo (`CatalogoApi.obtenerProveedor`); si no, 422 `PROVEEDOR_INACTIVO`.
  - `precioUnitario` > 0; `fechaValidez` no anterior a hoy.
  - `precioTotal = precioUnitario × cantidad` del ítem, calculado en el backend con `BigDecimal` y redondeado a 2 decimales (nunca con `double`).
  - Publicar `CotizacionCargada`.
- `GET /api/v1/bandeja/{itemId}` incluye `cotizaciones: [{ id, proveedorId, proveedorRazonSocial, moneda, precioUnitario, precioTotal, fechaValidez, entregaEstimada, observaciones, estado, vigente }]`.
- Monedas distintas en el mismo ítem se permiten y se muestran separadas, sin conversión (caso borde 7).
- **Documentación:**
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Hibernate Validator: anotaciones de validación (`@NotNull`, `@Positive`, `@Email`...)](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints)
  - [Java: `BigDecimal`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html)
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

### T10.3 · Elegir una cotización y fijar el precio total
- Acá **ya no se aprueba**: aprobar es de la cadena de Aprobaciones (decisión 7). El Encargado negocia y fija el precio del ítem, de dos formas. En las dos, el ítem tiene que estar EN_COTIZACION y asignado a mí.
- **Elegir una cotización:** `POST /api/v1/bandeja/{itemId}/cotizaciones/{cotizacionId}/elegir` `{ version }` → 200.
  - Validaciones, en orden: 1. el ítem está EN_COTIZACION y asignado a mí; 2. la cotización es de este ítem; 3. la cotización está vigente **ahora** (si no: 422 `COTIZACION_VENCIDA`, "La cotización venció el {fecha}. Cargá una nueva.", §7.2); 4. la `version` coincide.
  - Efectos, en una transacción: la cotización pasa a SELECCIONADA; la que estaba seleccionada antes vuelve a CANDIDATA (el Encargado puede cambiar de idea mientras no compre, y el índice único `ux_una_seleccionada` no admite dos); la gestión guarda `cotizacionElegidaId`, `precioTotal` = total de la cotización y `monedaTotal` = su moneda.
- **Cargar el precio total a mano:** `PUT /api/v1/bandeja/{itemId}/precio-total` `{ precioTotal, moneda, version }` → 200. Para cuando se negoció por teléfono y no hay cotización formal. `precioTotal` > 0. Si había una cotización elegida, vuelve a CANDIDATA y `cotizacionElegidaId` queda en null.
- **Quitar el precio total:** `DELETE /api/v1/bandeja/{itemId}/precio-total?version=` → 204. El ítem vuelve a evaluarse con el precio estimado. No se puede si ya está COMPRADO.
- Las tres operaciones publican `PrecioTotalActualizado`. Ese evento hace dos cosas: Aprobaciones **re-evalúa la cadena** con el monto nuevo (T23.4) y la pantalla de todos los que ven el ítem se actualiza **en tiempo real** (T24). No publican `ItemResuelto`: elegir un precio no resuelve nada.
- Doble click (caso borde 4): lo frena `@Version` y el índice único de cotización seleccionada.
- **Tests:** elegir una cotización vencida → 422; elegir otra cotización deja una sola SELECCIONADA; cargar el precio a mano des-selecciona la cotización; los tres casos publican `PrecioTotalActualizado` con los datos correctos; ítem no asignado a mí → 403.
- **Documentación:**
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [PostgreSQL: índices únicos](https://www.postgresql.org/docs/current/indexes-unique.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Spring MVC: errores en APIs REST](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
  - [MDN: 422 Unprocessable Content](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/422)

### T10.4 · Rechazar un ítem
- **Rechaza el Encargado:** `POST /api/v1/bandeja/{itemId}/rechazar` `{ motivo, version }` → 200.
  - `motivo` obligatorio y no vacío (regla 6: "Rechazado = tiene motivo").
  - Se puede rechazar desde PENDIENTE (sin haber cotizado; enunciado: "se puede rechazar sin haber cotizado") o desde EN_COTIZACION si está asignado a mí. No se puede si ya está COMPRADO (422).
  - La gestión pasa a RECHAZADO, con `motivoRechazo` y `resueltoEn`. Publicar `ItemResuelto` (RECHAZADO, `rechazadoPor` = "Encargado de compras"). Aprobaciones lo escucha y cancela los pasos pendientes (T23.4).
- **Rechaza un aprobador:** listener de `AprobacionRechazada` (lo publica T23.5). La gestión pasa a RECHAZADO con el `motivo` del aprobador y se publica `ItemResuelto` (RECHAZADO, `rechazadoPor` = `aprobadorNombre`, por ejemplo "Gerencia General"). Aplica aunque el ítem esté asignado a un Encargado: queda RECHAZADO y el Encargado lo ve así.
  - **Idempotente:** si la gestión ya estaba RECHAZADA, COMPRADA o CANCELADA, no hace nada (por ejemplo, el Encargado y el aprobador rechazaron casi a la vez: gana el primero).
- El rechazo es **por ítem**, nunca por orden ni por solicitud (decisión 7).
- **Tests:** el Encargado rechaza sin haber cotizado; rechazo sin motivo → 400; rechazar un ítem COMPRADO → 422; con `Scenario`, publicar `AprobacionRechazada` y verificar que la gestión queda RECHAZADA y se publica `ItemResuelto`; publicarlo dos veces no duplica el evento.
- **Documentación:**
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Hibernate Validator: anotaciones de validación (`@NotNull`, `@Positive`, `@Email`...)](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)

### T10.5 · Proveedores sugeridos para un ítem
- `GET /api/v1/bandeja/{itemId}/proveedores-sugeridos` → `CatalogoApi.proveedoresActivosDe(categoriaId del ítem)`, como `[{ id, razonSocial }]`.
- Flujo: `compras/web/BandejaController` → `compras/servicio/BandejaService.proveedoresSugeridos(itemId)`, que busca la `GestionItem` (404 si no existe), toma su `categoriaId` y llama a `CatalogoApi`. Compras no lee las tablas de `catalogo`: pasa siempre por su API pública (lo verifica `ModularityTests`).
- Las categorías unificadas ya las resuelve `CatalogoApi` (T06.7).
- Lista vacía → 200 con `[]`. El front muestra "No hay proveedores para esta categoría" y el botón de alta rápida (caso borde 10, T19.2).
- Solo `ENCARGADO`. Cualquier Encargado puede consultarlo, aunque el ítem no sea suyo (sirve para decidir si lo toma).
- **Documentación:**
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
  - [Spring MVC: mapeo de rutas (`@GetMapping`, `@PathVariable`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)

### T10.6 · Marcar cotizaciones vencidas en la bandeja
- En la respuesta de la bandeja (T09.3), agregar `todasVencidas: true` cuando el ítem tiene cotizaciones y ninguna está vigente (caso borde 6). El front lo muestra como aviso.
- Reglas:
  - Sin cotizaciones → `todasVencidas: false` (no hay nada vencido; simplemente no se cotizó).
  - Con al menos una vigente → `false`.
  - Vigente = `Cotizacion.estaVigente(hoy)` de T10.1, con hoy en hora de Argentina (caso borde 19).
- Cómo calcularlo sin hacer una consulta por cada fila (el problema "N+1"): después de traer la página de gestiones, juntar sus `itemId` y hacer **una** consulta, por ejemplo `CotizacionRepository.findItemIdsConCotizacionVigente(List<Long> itemIds, LocalDate hoy)` con `@Query("select distinct c.itemId from Cotizacion c where c.itemId in :ids and c.fechaValidez >= :hoy")`, y otra igual para saber cuáles tienen alguna cotización. Con esos dos conjuntos se arma el campo en el service.
- Agregar el mismo campo en `GET /api/v1/bandeja/{itemId}`.
- Test: ítem con dos cotizaciones vencidas → `true`; con una vencida y una vigente → `false`; sin cotizaciones → `false`.
- **Documentación:**
  - [Spring Data JPA: consultas con `@Query` (JPQL y nativas)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query)
  - [Java: `ZoneId` (zonas horarias)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/ZoneId.html)

### T10.7 · Registrar la compra
- Cuando la cadena está completa, el Encargado ve el ítem en "Para comprar" (T09.3), compra por fuera del sistema (teléfono, mail) y lo registra acá. **Recién ahora se genera la orden** (decisión 10).
- **Listeners de aprobación** (en `compras/servicio/`, idempotentes): `AprobacionCompletada` → `estadoAprobacion = APROBADA`; `AprobacionReabierta` → `estadoAprobacion = EN_CURSO`. Son los que habilitan o bloquean la compra.
- `POST /api/v1/bandeja/{itemId}/comprar` `{ proveedorId, version }` → 200.
  - `proveedorId` es obligatorio solo si no hay cotización elegida; si la hay, el proveedor es el de esa cotización (y si viene `proveedorId` tiene que coincidir).
  - **El precio no viaja en el request:** se compra al `precioTotal` que tiene la gestión, que es el mismo que miraron los aprobadores. Para cambiarlo, primero se fija otro precio (T10.3), lo que puede reabrir la cadena.
- Validaciones, en orden:
  1. El ítem está EN_COTIZACION y asignado a mí.
  2. `estadoAprobacion = APROBADA`; si no, 422 `APROBACION_PENDIENTE`, "Este ítem todavía no tiene todas las aprobaciones".
  3. Tiene `precioTotal`; si no, 422 `FALTA_PRECIO_TOTAL`.
  4. El proveedor existe y está activo (`CatalogoApi`); si no, 422 `PROVEEDOR_INACTIVO`.
  5. Si hay cotización elegida, sigue **vigente al momento de comprar** (422 `COTIZACION_VENCIDA`; caso borde 5).
  6. La `version` coincide (409 si no).
- Efectos, en una transacción: la gestión pasa a COMPRADO con `proveedorCompraId`, `compradoEn` y `resueltoEn`; las cotizaciones que no se eligieron pasan a DESCARTADA; se publican `CompraRegistrada` (con todo lo que necesita la orden) e `ItemResuelto` (COMPRADO).
- Doble click (caso borde 4): lo frena `@Version` y el `CHECK` de la tabla (COMPRADO exige precio, proveedor y cadena APROBADA).
- **Carrera aceptada:** si el Encargado cambia el precio y, a los pocos milisegundos, compra, el evento `AprobacionReabierta` todavía puede no haber llegado. El cambio de precio ya subió la `version` de la gestión, así que el `comprar` con la versión vieja falla con 409; con la versión nueva, el front ya recibió la señal en tiempo real. No se agrega más protección.
- **Tests:** comprar sin cadena completa → 422; sin precio total → 422; con cotización vencida → 422; compra feliz → 200, gestión COMPRADO, y se publican `CompraRegistrada` e `ItemResuelto`; con `Scenario`, `AprobacionCompletada` y `AprobacionReabierta` cambian `estadoAprobacion`.
- **Documentación:**
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [PostgreSQL: restricciones `CHECK`](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-CHECK-CONSTRAINTS)
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

### T10.8 · Editar la compra y cargar el monto de envío
- Después de comprar puede haber cambios (el proveedor ajustó el precio) y puede haber un cargo de envío.
- `PATCH /api/v1/bandeja/{itemId}/compra` `{ precioTotal, montoEnvio, version }` (los dos opcionales, al menos uno) → 200.
  - Solo si la gestión está COMPRADO y es mía (si no, 422 o 403).
  - `precioTotal` > 0. `montoEnvio` >= 0; en la moneda del total, que no se cambia acá. Mandar `montoEnvio: 0` o `null` deja el ítem sin envío.
  - **No se re-evalúa la cadena de aprobación:** la compra ya está hecha (decisión 10). Como esto permite subir el precio después de que se aprobó, **cada cambio queda en el historial con el valor anterior y el nuevo** (T13.2), para poder controlarlo.
- Publica `CompraActualizada` con los valores anteriores y nuevos. Escuchan Órdenes (actualiza la orden, T11.6), Reportes (actualiza el gasto, T16.2) y Auditoría.
- Solicitudes también escucha `CompraActualizada` y actualiza el `precioTotal` del ítem (T08.5), así el solicitante ve el valor real. Aprobaciones no la escucha: su cadena ya está cerrada.
- **Tests:** editar el precio de un ítem no comprado → 422; cargar el envío en un ítem comprado → 200 y se publica `CompraActualizada` con anterior y nuevo; `montoEnvio` negativo → 400.
- **Documentación:**
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Java: `BigDecimal`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

---

## T11 · Órdenes de pedido (módulo `ordenes`)

### T11.1 · Schema y dominio de órdenes
- **Migración** `V<fecha>_1__ordenes_tablas.sql`: secuencia `ordenes.numero_orden_seq` y tabla `ordenes.orden_pedido` como en §6.5, con `item_id` único. Agregar columnas que el diseño no tiene: `tipo VARCHAR(10) NOT NULL` y `detalle TEXT` (el PDF de T11.5 y la lista de órdenes tienen que decir **qué** se compra, y la orden es un documento que no depende de otro módulo) y `monto_envio NUMERIC(14,2)` (nulo hasta que el Encargado lo carga, T10.8). `cotizacion_id` pasa a ser **opcional**: se puede comprar con un precio total cargado a mano (decisión 12). Todo viene en `CompraRegistrada` (T07.3).
- **Dominio:**
  - `EstadoOrden`: GENERADA, ENVIADA.
  - `OrdenPedido` con todos los campos de §6.5, más `tipo`, `detalle` y `montoEnvio`. Sin setters: los datos copiados no cambian nunca, con **una excepción**: `precioTotal`, `precioUnitario` y `montoEnvio` se corrigen con el método `actualizarMontos(...)` cuando el Encargado edita la compra (T11.6). `precioUnitario` es `precioTotal / cantidad` redondeado a 2 decimales, porque ya no siempre hay una cotización de donde sacarlo. Lo otro que cambia es `estado`, `enviadaPor` y `enviadaEn`, con el update condicional de T11.4.
- **Repository:** `OrdenPedidoRepository` con `existsByItemId(Long)`, `findByEstado(EstadoOrden, Pageable)`, y una consulta nativa `select nextval('ordenes.numero_orden_seq')` para obtener el número antes de guardar. JPA solo sabe generar el id con secuencias, no otro campo.
- **Documentación:**
  - [PostgreSQL: `CREATE SEQUENCE`](https://www.postgresql.org/docs/current/sql-createsequence.html)
  - [PostgreSQL: `nextval` y funciones de secuencias](https://www.postgresql.org/docs/current/functions-sequence.html)
  - [Spring Data JPA: consultas con `@Query` (JPQL y nativas)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query)
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)

### T11.2 · Generar la orden al registrarse la compra
- Listener de `CompraRegistrada` en `ordenes/servicio/` (decisión 10: la orden ya no sale al aprobar, sino cuando el Encargado compra un ítem aprobado):
  1. Si ya existe una orden para ese `itemId`, no hace nada (idempotencia, §9).
  2. Si no, crea la orden con número de la secuencia, copiando proveedor, razón social, tipo, detalle, cantidad, moneda y precio total del evento (la orden es un documento: no cambia si después se edita el proveedor).
  3. Publica `OrdenGenerada`.
- Se genera una orden **por ítem**, aunque varios ítems vayan al mismo proveedor (enunciado "Orden de pedido").
- Test con `Scenario`: publicar `CompraRegistrada` dos veces con el mismo `itemId` y verificar que hay una sola orden.
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)

### T11.3 · Listar órdenes
- `GET /api/v1/ordenes?estado=GENERADA&page=` → `Pagina` de `{ id, numero, itemId, solicitudId, tipo, detalle, proveedorRazonSocial, cantidad, moneda, precioUnitario, precioTotal, montoEnvio, estado, emitidaEn, enviadaEn }`.
  - `estado` es opcional: sin él, trae todas.
  - Orden: `emitidaEn` descendente (las más nuevas primero). Las GENERADAS son las que el Encargado tiene que mandar (§7.4: "Órdenes pendientes de envío").
- `GET /api/v1/ordenes/{id}` → el detalle: lo mismo más `proveedorId`, `cotizacionId` (puede ser null), `enviadaPor` y `enviadaPorNombre` (con `AuthApi.obtenerUsuario`). Si no existe → 404.
- Dónde va: `ordenes/servicio/OrdenService` y `ordenes/web/OrdenController`; paginado con `Pagina` (T03.3).
- Solo `ENCARGADO` (T05.1).
- Test: con 2 órdenes GENERADAS y 1 ENVIADA, `?estado=GENERADA` devuelve 2.
- **Documentación:**
  - [Spring MVC: `@RequestParam`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Data: `Pageable` en controllers (soporte web)](https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web)

### T11.4 · Marcar una orden como enviada
- `POST /api/v1/ordenes/{id}/marcar-enviada` → 200 con la orden actualizada. Solo desde GENERADA (si no, 422 `ORDEN_YA_ENVIADA`). Si no existe → 404.
- Guarda `enviadaPor` = yo y `enviadaEn` = ahora. Publica `OrdenEnviada` con `ordenId`, `itemId`, `solicitudId` y `usuarioId`.
- **Doble click:** dos requests pueden leer la orden como GENERADA al mismo tiempo, y los dos publicarían `OrdenEnviada`. La tabla no tiene columna `version`, así que para evitarlo el cambio se hace con un update condicional: `@Modifying @Query("update OrdenPedido o set o.estado = ENVIADA, o.enviadaPor = :u, o.enviadaEn = :ahora where o.id = :id and o.estado = GENERADA")`, que devuelve cuántas filas cambió. Si devuelve 0, la orden ya estaba enviada → 422, y el evento no se publica. Solo el request que cambió la fila publica.
- Efecto en cadena: Solicitudes escucha `OrdenEnviada`, marca el ítem con `ordenEnviada = true` (T08.5) y, si era la última, la solicitud pasa a CERRADA (T08.6).
- Test: marcar dos veces la misma orden → la primera 200 y la segunda 422; se publicó un solo `OrdenEnviada`.
- **Documentación:**
  - [Spring Data JPA: consultas de modificación (`@Modifying`)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.modifying-queries)
  - [PostgreSQL: `UPDATE`](https://www.postgresql.org/docs/current/sql-update.html)
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

### T11.5 · Orden en PDF
- `GET /api/v1/ordenes/{id}/pdf` → `application/pdf` con número, fecha, proveedor, cantidad, detalle, moneda, precios y, si lo hay, el monto de envío.
- Usar OpenPDF (agregar la dependencia `com.github.librepdf:openpdf` con la versión que indica su repo).
- **Documentación:**
  - [OpenPDF](https://github.com/LibrePDF/OpenPDF)
  - [Spring MVC: `ResponseEntity` (código HTTP y headers de la respuesta)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)
  - [MDN: header `Content-Disposition` (en español)](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Headers/Content-Disposition)

### T11.6 · Actualizar la orden cuando se edita la compra
- Listener de `CompraActualizada` en `ordenes/servicio/`: busca la orden por `itemId` y llama a `actualizarMontos(precioTotal, montoEnvio)`. Idempotente: aplicar dos veces los mismos valores no cambia nada.
- Si la orden ya estaba ENVIADA, se actualiza igual (el Encargado corrige lo que se pagó de verdad), pero el PDF que ya se mandó no cambia. El historial guarda el valor anterior (T13.2).
- Si todavía no existe la orden (el evento llegó antes que `CompraRegistrada`, porque Modulith no garantiza el orden entre eventos), lanzar una excepción: el evento queda pendiente en `event_publication` y se reintenta (T12.1).
- Test con `Scenario`: generar la orden, publicar `CompraActualizada` con envío 5.000 y verificar `montoEnvio`; publicar `CompraActualizada` sin que exista la orden y verificar que queda pendiente.
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: registro de publicación de eventos](https://docs.spring.io/spring-modulith/reference/events.html#publication-registry)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)

---

## T12 · Eventos confiables e idempotencia

### T12.1 · Reintento de eventos pendientes
- En `application.properties`:
  - `spring.modulith.events.republish-outstanding-events-on-restart=true`: si la app se cae después de guardar un cambio pero antes de que un listener termine, al reiniciar se vuelve a procesar el evento (§9: "Falla a mitad de una operación").
  - `spring.modulith.events.completion-mode=delete`: los eventos ya procesados se borran de la tabla, para que no crezca sin límite.
- Probar a mano: poner un `throw` temporal en un listener, crear una solicitud, ver que la fila queda sin completar en `event_publication`, sacar el `throw`, reiniciar y ver que se procesa.
- **Documentación:**
  - [Spring Modulith: registro de publicación de eventos](https://docs.spring.io/spring-modulith/reference/events.html#publication-registry)
  - [Spring Modulith: propiedades de configuración](https://docs.spring.io/spring-modulith/reference/appendix.html#configuration-properties)

### T12.2 · Listeners idempotentes
- Revisar cada listener y asegurar que procesar el mismo evento dos veces no duplique nada:
  - Los que crean una fila con clave natural (gestión por `itemId`, orden por `itemId`, hecho de gasto por `ordenId`, aprobación por `itemId`, paso por `itemId` + orden + aprobador): verificar si ya existe antes de crear.
  - Los que no tienen clave natural (historial de auditoría): tabla `<schema>.evento_procesado (evento_id UUID PRIMARY KEY, procesado_en TIMESTAMPTZ)` como en §6.7. Guardar el `eventoId` en la misma transacción y, si ya estaba, salir sin hacer nada.
- **Documentación:**
  - [Spring Modulith: registro de publicación de eventos](https://docs.spring.io/spring-modulith/reference/events.html#publication-registry)
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

### T12.3 · Tests entre módulos
- Un `@ApplicationModuleTest` por módulo que escucha eventos, usando `Scenario`: publicar el evento de entrada y esperar el resultado. Por ejemplo, `CompraRegistrada` → hay una orden; `ItemResuelto` → el ítem de Solicitudes cambió de estado.
- Cómo se escribe: el test recibe un `Scenario` como parámetro y hace `scenario.publish(evento).andWaitForStateChange(() -> repositorio.findByItemId(id)).andVerify(orden -> ...)`. Como los listeners corren en otro hilo después del commit, no se puede verificar justo después de publicar: `andWaitForStateChange` espera hasta que el resultado aparezca. Para verificar que un módulo **publicó** un evento: `scenario.stimulate(...).andWaitForEventOfType(OrdenGenerada.class)`.
- `@ApplicationModuleTest` levanta solo ese módulo (y `compartido`). Si el módulo llama a la API de otro (por ejemplo `CatalogoApi`), hay que simularla con `@MockitoBean`.
- Tests mínimos:

| Módulo | Evento de entrada | Qué verificar |
|---|---|---|
| `compras` | `SolicitudCreada` con 2 ítems | 2 gestiones PENDIENTES |
| `compras` | `ItemCancelado` de un ítem ya tomado | se publica `CancelacionRechazada` |
| `compras` | `AprobacionCompletada`, y después `AprobacionReabierta` | `estadoAprobacion` pasa a APROBADA y vuelve a EN_CURSO |
| `compras` | `AprobacionRechazada` dos veces | la gestión queda RECHAZADA y se publica un solo `ItemResuelto` |
| `aprobaciones` | `SolicitudCreada` con 2 ítems | 2 cadenas con los pasos de la regla que corresponde al monto |
| `aprobaciones` | `PrecioTotalActualizado` que supera el umbral de una regla | se agrega el paso que faltaba y, si estaba APROBADA, se publica `AprobacionReabierta` |
| `solicitudes` | `ItemAsignado` | el ítem pasa a EN_COTIZACION |
| `solicitudes` | `ItemResuelto` del último ítem | la solicitud pasa a LISTA y se publica `SolicitudLista` |
| `solicitudes` | `OrdenEnviada` del último ítem comprado | la solicitud pasa a CERRADA |
| `ordenes` | `CompraRegistrada` dos veces con el mismo `itemId` | una sola orden y un solo `OrdenGenerada` |
| `ordenes` | `CompraActualizada` | la orden cambia `precioTotal` y `montoEnvio` |
| `auditoria` | `SolicitudCreada` | una fila en `historial` |
| `reportes` | `OrdenGenerada`, y después `CompraActualizada` | un `HechoGasto`, y su importe y envío cambian |

- Cada test importa `TestcontainersConfiguration` (T02.3).
- **Documentación:**
  - [Spring Modulith: tests de módulos (`@ApplicationModuleTest`)](https://docs.spring.io/spring-modulith/reference/testing.html)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)
  - [Spring Boot: Testcontainers](https://docs.spring.io/spring-boot/4.1.1/reference/testing/testcontainers.html)

---

## T13 · Historial de auditoría (módulo `auditoria`)

### T13.1 · Schema, dominio y repository
- **Migración** `V<fecha>_1__auditoria_tablas.sql`:
  - `auditoria.historial` como en §6.7.
  - `auditoria.evento_procesado` (§6.7), para que los listeners sean idempotentes (T12.2).
  - `auditoria.item_solicitud (item_id BIGINT PRIMARY KEY, solicitud_id BIGINT NOT NULL, solicitante_id BIGINT NOT NULL)`, con índice por `solicitud_id`. El diseño no la tiene. Sirve para dos cosas de T13.3: traer las entradas de los ítems de una solicitud y saber quién es el dueño, sin consultar el módulo `solicitudes`.
- **Dominio:**
  - `EntradaHistorial` con los campos de §6.7. Sin setters: se crea una vez y nunca se modifica (enunciado, regla 11). Un constructor o un método estático `EntradaHistorial.de(...)` con todos los datos.
  - Enums `EntidadHistorial` (SOLICITUD, ITEM, COTIZACION, ORDEN, CATEGORIA, USUARIO, REGLA) y `AccionHistorial` (CREACION, EDICION, CAMBIO_ESTADO, ASIGNACION, REASIGNACION, UNIFICACION, CAMBIO_ROL, APROBACION, RECHAZO).
  - `ItemSolicitud` (`itemId`, `solicitudId`, `solicitanteId`).
- **Repositories:**
  - `HistorialRepository`: solo guardar y `findByEntidadAndEntidadIdOrderByOcurridoEnAsc`. No exponer `delete`. Se puede extender `Repository<EntradaHistorial, Long>` en vez de `JpaRepository`, para declarar solo los métodos que se permiten.
  - `ItemSolicitudRepository`: `findBySolicitudId(Long)`.
  - `EventoProcesadoRepository`.
- **Documentación:**
  - [Spring Data: definir un repository exponiendo solo algunos métodos](https://docs.spring.io/spring-data/jpa/reference/repositories/definition.html#repositories.definition-tuning)
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
  - [Hibernate 7: enums (`@Enumerated`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#enums)

### T13.2 · Registrar los eventos
- Un componente en `auditoria/servicio/` con un `@ApplicationModuleListener` por evento, que traduce cada uno a una fila (idempotente con `evento_procesado`):

| Evento | entidad | accion | estado anterior → nuevo | usuario |
|---|---|---|---|---|
| `SolicitudCreada` | SOLICITUD | CREACION | — → EN_REVISION | solicitante |
| `ItemEditado` | ITEM | EDICION | — | quien editó |
| `ItemCancelado` | ITEM | CAMBIO_ESTADO | PENDIENTE → CANCELADO | quien canceló |
| `SolicitudCancelada` | SOLICITUD | CAMBIO_ESTADO | EN_REVISION → CANCELADA | quien canceló |
| `ItemAsignado` | ITEM | ASIGNACION, o REASIGNACION si había anterior | PENDIENTE → EN_COTIZACION | encargado nuevo |
| `CotizacionCargada` | COTIZACION | CREACION | — → CANDIDATA | encargado |
| `PrecioTotalActualizado` | ITEM | EDICION | — (comentario = "Precio total: {anterior} → {nuevo} {moneda}", y el proveedor si salió de una cotización) | encargado |
| `PasoDecidido` | ITEM | APROBACION o RECHAZO | — (comentario = "{aprobadorNombre}" y el motivo si rechazó) | quien decidió |
| `AprobacionCompletada` | ITEM | CAMBIO_ESTADO | EN_CURSO → APROBADA (comentario "automática" si la regla no pedía pasos) | — (sistema) |
| `AprobacionReabierta` | ITEM | CAMBIO_ESTADO | APROBADA → EN_CURSO (comentario = motivo) | — (sistema) |
| `CompraRegistrada` | ITEM | CAMBIO_ESTADO | EN_COTIZACION → COMPRADO | encargado |
| `CompraActualizada` | ITEM | EDICION | — (comentario = valores anteriores y nuevos de precio y envío) | encargado |
| `ReglaModificada` | REGLA | CREACION / EDICION / CAMBIO_ESTADO | — (comentario = acción) | admin |
| `ItemResuelto` | ITEM | CAMBIO_ESTADO | EN_COTIZACION → RECHAZADO (comentario = motivo y quién rechazó). El caso COMPRADO ya lo registra `CompraRegistrada`: no duplicar. | encargado o aprobador |
| `SolicitudLista` / `SolicitudCerrada` | SOLICITUD | CAMBIO_ESTADO | → LISTA / → CERRADA | — (sistema) |
| `OrdenGenerada` | ORDEN | CREACION | — → GENERADA | — (sistema) |
| `OrdenEnviada` | ORDEN | CAMBIO_ESTADO | GENERADA → ENVIADA | quien la envió |
| `CategoriaCreada` / `CategoriaUnificada` | CATEGORIA | CREACION / UNIFICACION | | |
| `EncargadoDesasignado` | USUARIO | CAMBIO_ROL | | admin |

- Además, el listener de `SolicitudCreada` guarda una fila en `auditoria.item_solicitud` por cada ítem (`itemId`, `solicitudId`, `solicitanteId`), en la misma transacción que la entrada del historial. La usa T13.3.
- Idempotencia (T12.2): al principio de cada listener, si el `eventoId` ya está en `evento_procesado`, salir; si no, guardarlo junto con la entrada.
- Test: publicar `SolicitudCreada` con 2 ítems → una entrada de SOLICITUD y 2 filas en `item_solicitud`; publicarlo otra vez → no cambia nada. Publicar `CompraActualizada` con precio 100 → 95 → una entrada que dice los dos valores.
- Regla 11 del enunciado ("todo cambio queda en el historial"): cambiar una regla de aprobación **también** es un cambio. Por eso `ReglaModificada` queda registrada con quién y cuándo.
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)

### T13.3 · Consultar el historial
- `GET /api/v1/historial?entidad=SOLICITUD&id=123` → lista ordenada por fecha de `{ ocurridoEn, entidad, entidadId, accion, estadoAnterior, estadoNuevo, usuarioNombre, comentario }`.
- Para el detalle de una solicitud conviene que devuelva también las entradas de sus ítems: agregar `?incluirItems=true`, que busca en `auditoria.item_solicitud` (T13.1, se llena en T13.2) los ítems de esa solicitud y trae también sus entradas de ITEM, todo junto y ordenado por fecha.
- `usuarioNombre` sale de `AuthApi.obtenerUsuario`. Si `usuario_id` es null, es un cambio automático: mostrar "Sistema".
- **Permisos** (T05.1):
  - `ENCARGADO` y `ADMIN` ven cualquier historial.
  - Un Solicitante solo el de sus solicitudes: si `entidad=SOLICITUD` y el `solicitante_id` de `item_solicitud` no es `UsuarioActual.id()` → 403. Un Solicitante no puede pedir otras entidades (ORDEN, COTIZACION...) → 403.
- `entidad` inválida → 400; sin entradas → `[]`.
- Test: el dueño ve su historial; otro Solicitante recibe 403.
- **Documentación:**
  - [Spring MVC: `@RequestParam`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)
  - [Spring Data JPA: consultas a partir del nombre del método](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation)
  - [Spring Security: seguridad por método (`@EnableMethodSecurity`)](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#activate-method-security)

---

## T14 · Adjuntos

### T14.1 · Servicio de almacenamiento
- En `compartido/archivos/AlmacenamientoArchivos`:
  - `String guardar(InputStream contenido, String extension)`: guarda en la carpeta `ADJUNTOS_DIR` (del `.env`) con nombre `UUID.extension` y devuelve esa clave.
  - `Resource abrir(String clave)`.
  - `void borrar(String clave)`.
- Nunca usar el nombre que manda el usuario como nombre de archivo en disco (evita que alguien escriba fuera de la carpeta con `../`).
- `ADJUNTOS_DIR=./adjuntos` en `.env.example`, y `back/adjuntos/` en `.gitignore`.
- En `application.properties`: `spring.servlet.multipart.max-file-size=10MB` y `spring.servlet.multipart.max-request-size=11MB`.
- **Documentación:**
  - [Java: `Files` (leer y escribir archivos)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html)
  - [Spring: la interfaz `Resource`](https://docs.spring.io/spring-framework/reference/core/resources.html)
  - [Spring Boot: `@ConfigurationProperties`](https://docs.spring.io/spring-boot/4.1.1/reference/features/external-config.html#features.external-config.typesafe-configuration-properties)
  - [Spring MVC: configuración de multipart](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/multipart.html)
  - [OWASP: path traversal (`../` en nombres de archivo)](https://owasp.org/www-community/attacks/Path_Traversal)

### T14.2 · Adjuntos de un ítem
- **Migración:** `solicitudes.adjunto_item` como en §6.2.
- `POST /api/v1/items/{itemId}/adjuntos` (multipart, campo `archivo`) → 201 `{ id, nombre, mimeType, tamanoBytes, subidoEn }`. Solo el dueño de la solicitud y mientras el ítem no esté en estado final.
- **Validación del tipo real** con Apache Tika (`org.apache.tika:tika-core`, versión 3.x de Maven Central): detectar el tipo por el contenido, no por la extensión. Aceptar solo `application/pdf`, `image/jpeg` e `image/png`; si no, 422 `TIPO_NO_PERMITIDO`. Más de 10 MB → 413 (lo corta Spring solo; mapearlo en el manejador de T03.2).
- `GET /api/v1/items/{itemId}/adjuntos` → lista.
- `GET /api/v1/adjuntos-item/{id}` → descarga el archivo, con `Content-Disposition: attachment` y el nombre original. Pueden descargarlo el dueño y los Encargados.
- **Documentación:**
  - [Spring MVC: formularios multipart (subir archivos)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html)
  - [Spring MVC: configuración de multipart](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/multipart.html)
  - [Apache Tika: detección del tipo de contenido](https://tika.apache.org/3.2.0/detection.html)
  - [MDN: header `Content-Disposition` (en español)](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Headers/Content-Disposition)
  - [OWASP: subida segura de archivos](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
  - [Guía oficial: Uploading Files](https://spring.io/guides/gs/uploading-files/)

### T14.3 · Adjuntos de una cotización
- **Migración** `V<fecha>_1__compras_adjunto_cotizacion.sql`: `compras.adjunto_cotizacion` como en §6.4, más `subido_por BIGINT NOT NULL` y el mismo `CHECK` de `mime_type` y tamaño que `solicitudes.adjunto_item` (§6.2), que el diseño no puso en esta tabla.
- **Dominio y repository** en `compras/`: `AdjuntoCotizacion` (`id`, `cotizacionId`, `claveObjeto`, `nombre`, `mimeType`, `tamanoBytes`, `subidoPor`, `subidoEn`) y `AdjuntoCotizacionRepository.findByCotizacionIdOrderBySubidoEnAsc(Long)`.
- **Endpoints** (en `compras/web/`):
  - `POST /api/v1/cotizaciones/{id}/adjuntos` (multipart, campo `archivo`) → 201 `{ id, nombre, mimeType, tamanoBytes, subidoEn }`. Es el presupuesto del proveedor (§7.2: "Adjunta el presupuesto").
  - `GET /api/v1/cotizaciones/{id}/adjuntos` → lista.
  - `GET /api/v1/adjuntos-cotizacion/{id}` → descarga, con `Content-Disposition: attachment` y el nombre original.
- **Reglas:**
  - Solo `ENCARGADO` (T05.1).
  - Para **subir**: la cotización existe (si no, 404) y su ítem está asignado a mí. Si el ítem ya está resuelto (COMPRADO o RECHAZADO), no se aceptan adjuntos nuevos → 422 `ITEM_RESUELTO`.
  - Para **ver y descargar**: cualquier Encargado.
- **Reusar lo de T14.1 y T14.2, no copiarlo:** el guardado en disco es `AlmacenamientoArchivos`, y la validación del tipo real con Tika conviene moverla a un componente en `compartido/archivos/` (por ejemplo `ValidadorArchivos.validar(MultipartFile)`), así los dos módulos usan la misma. Mismos tipos permitidos (PDF, JPG, PNG), mismo límite de 10 MB y mismos errores (422 `TIPO_NO_PERMITIDO`, 413).
- Nombre original: guardarlo solo en la base (columna `nombre`) y limpiarlo de rutas (`Paths.get(nombre).getFileName()`) antes de devolverlo en el header. En disco se usa siempre la clave UUID (T14.1).
- Test: subir un PDF → 201; subir un `.txt` renombrado a `.pdf` → 422 (Tika detecta el contenido real); un Encargado que no tiene el ítem asignado intenta subir → 403.
- **Documentación:**
  - [Spring MVC: formularios multipart (subir archivos)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html)
  - [Apache Tika: detección del tipo de contenido](https://tika.apache.org/3.2.0/detection.html)
  - [MDN: header `Content-Disposition` (en español)](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Headers/Content-Disposition)
  - [OWASP: subida segura de archivos](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)

### T14.4 · (Opcional) Almacenamiento S3/MinIO
- Solo si sobra tiempo: otra implementación de `AlmacenamientoArchivos` que use MinIO, elegida por configuración. Es lo que propone el diseño (almacenamiento de objetos, §4 y §5); en el TP se simplificó a una carpeta local.
- Pasos:
  1. Convertir `AlmacenamientoArchivos` (T14.1) en una **interfaz** con dos implementaciones: `AlmacenamientoLocal` (la actual) y `AlmacenamientoS3`.
  2. Elegir con una propiedad: `ALMACENAMIENTO=local|s3` en el `.env`. Cada implementación lleva `@ConditionalOnProperty(name = "almacenamiento.tipo", havingValue = "...")`; la local es la opción por defecto (`matchIfMissing = true`).
  3. Agregar MinIO a `back/compose.yaml`: imagen `minio/minio`, comando `server /data --console-address ":9001"`, puertos 9000 (API) y 9001 (consola web), un volumen, y usuario y clave desde el `.env` (`MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`).
  4. `AlmacenamientoS3` usa el AWS SDK for Java v2 (`software.amazon.awssdk:s3`), apuntando al endpoint de MinIO con `endpointOverride` y `forcePathStyle(true)`. `guardar` → `putObject`, `abrir` → `getObject`, `borrar` → `deleteObject`. El bucket (`S3_BUCKET`) se crea al arrancar si no existe.
  5. Agregar las claves nuevas a `back/.env.example`.
- La API REST y las tablas no cambian: solo cambia dónde quedan los bytes. Con `ALMACENAMIENTO=local` todo tiene que seguir funcionando igual.
- **Documentación:**
  - [Spring Boot: `@ConditionalOnProperty`](https://docs.spring.io/spring-boot/4.1.1/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.condition-annotations.property-conditions)
  - [Docker Compose: referencia de servicios (`image`, `ports`, `volumes`, `extra_hosts`)](https://docs.docker.com/reference/compose-file/services/)
  - [AWS SDK for Java v2: trabajar con S3](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3.html)
  - [MinIO: repositorio oficial (cómo correrlo con Docker)](https://github.com/minio/minio)

---

## T15 · Notificaciones y recordatorios con n8n (módulo `integraciones`)

### T15.1 · n8n y Mailpit en Docker
- Agregar dos servicios a `back/compose.yaml`:
  - `n8n`: imagen `docker.n8n.io/n8nio/n8n`, puerto `5678`, un volumen para no perder los workflows, zona horaria `America/Argentina/Buenos_Aires` (variables `GENERIC_TIMEZONE` y `TZ`), y `extra_hosts: ["host.docker.internal:host-gateway"]` para que n8n llegue al backend que corre en la máquina (en Linux no existe por defecto).
  - `mailpit`: imagen `axllent/mailpit`, puertos `8025` (interfaz web para ver los mails) y `1025` (SMTP).
- En n8n, credencial SMTP: host `mailpit`, puerto `1025`, sin usuario (van en la misma red de Docker).
- Verificar: n8n en `http://localhost:5678`, Mailpit en `http://localhost:8025`.
- **Documentación:**
  - [n8n: instalación con Docker](https://docs.n8n.io/deploy/host-n8n/install-options/install-with-docker)
  - [n8n: zona horaria](https://docs.n8n.io/deploy/host-n8n/configure-n8n/basic-configuration/use-environment-variables/timezone-and-localization)
  - [Mailpit: instalación con Docker](https://mailpit.axllent.org/docs/install/docker/)
  - [Docker Compose: referencia de servicios (`image`, `ports`, `volumes`, `extra_hosts`)](https://docs.docker.com/reference/compose-file/services/)
  - [n8n: credencial SMTP de Send Email](https://docs.n8n.io/integrations/builtin/credentials/send-email)

### T15.2 · Enviar los avisos a n8n
- `.env`: `N8N_WEBHOOK_URL=http://localhost:5678/webhook` (y en `.env.example` sin valor).
- En `integraciones/servicio/`, un `RestClient` configurado con esa URL, y listeners:
  - `SolicitudCreada` → `POST {N8N_WEBHOOK_URL}/solicitud-creada` con `{ solicitudId, solicitanteNombre, sector, urgencia, fechaNecesaria, cantidadItems, emailsEncargados: [] }`.
  - `SolicitudLista` → `POST {N8N_WEBHOOK_URL}/solicitud-lista` con `{ solicitudId, solicitanteEmail, solicitanteNombre, items: [{ categoria, detalle, resultado, proveedor, motivoRechazo, rechazadoPor }] }`.
  - `PasoAprobacionActivado` → `POST {N8N_WEBHOOK_URL}/aprobacion-pendiente` con `{ itemId, solicitudId, detalle, solicitanteNombre, sector, aprobadorNombre, monto, moneda, emailsAprobadores: [] }`. Los destinatarios son los usuarios activos con el rol del paso (`AuthApi.emailsActivosConRol`) o los del sector (`emailsActivosDelSector`), **menos el solicitante** (nadie aprueba su propio pedido, decisión 13). Si la lista queda vacía, no se llama a n8n y se loguea una advertencia (caso borde 24).
  - `EncargadoDesasignado` (caso borde 15, lo publica T05.4) → `POST {N8N_WEBHOOK_URL}/encargado-desasignado` con `{ encargadoNombre, emailsEncargados: [] }`, para avisar a los demás Encargados que los ítems de esa persona volvieron a la bandeja general. Los destinatarios son los Encargados activos **menos** el desasignado. Si T05.4 no se hizo, este listener no se dispara nunca y no molesta.
- Agregar a `AuthApi` el método `List<String> emailsEncargadosActivos()`.
- Si n8n no responde, el listener lanza una excepción y el evento queda pendiente en `event_publication`; se reintenta al reiniciar (T12.1). Así un n8n caído no pierde avisos (§9).
- **Documentación:**
  - [Spring Boot: llamar servicios REST con `RestClient`](https://docs.spring.io/spring-boot/4.1.1/reference/io/rest-client.html)
  - [Spring: `RestClient`](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: registro de publicación de eventos](https://docs.spring.io/spring-modulith/reference/events.html#publication-registry)
  - [n8n: nodo Webhook](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook)

### T15.3 · Workflow: aviso de solicitud nueva
- En n8n: nodo **Webhook** (POST, path `solicitud-creada`) → nodo **Send Email** a `emailsEncargados`, con asunto "Nueva solicitud #{id} — urgencia {urgencia}" y el resumen en el cuerpo.
- Verificar el mail en Mailpit creando una solicitud.
- En el mismo workflow (un workflow de n8n puede tener varios nodos Webhook de entrada), un segundo **Webhook** (POST, path `encargado-desasignado`) → **Send Email** a `emailsEncargados` con asunto "Hay ítems sin asignar en la bandeja" y el texto "Los ítems de {encargadoNombre} volvieron a la bandeja general". Es el aviso del caso borde 15 que publica T05.4 y manda T15.2.
- Si `emailsEncargados` viene vacío, cortar el flujo con un nodo **If** antes de mandar el mail (Send Email falla sin destinatarios).
- Mientras se prueba se usa la **Test URL** del Webhook (`/webhook-test/...`). El backend llama a la **Production URL** (`/webhook/...`), que solo responde con el workflow activado (switch "Active" arriba a la derecha).
- **Documentación:**
  - [n8n: nodo Webhook](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook)
  - [n8n: nodo Send Email](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.sendemail)
  - [n8n: nodo If](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.if)
  - [n8n: expresiones y datos de nodos anteriores](https://docs.n8n.io/build/work-with-data/reference-data)
  - [Mailpit: documentación](https://mailpit.axllent.org/docs/)

### T15.4 · Workflow: resultado al solicitante
- Webhook `solicitud-lista` → **Send Email** al solicitante con una tabla: por cada ítem, si se compró (y a qué proveedor) o se rechazó (por quién y por qué). Enunciado, paso 9 del ejemplo de Laura.
- Los datos llegan del backend (T15.2): `{ solicitudId, solicitanteEmail, solicitanteNombre, items: [{ categoria, detalle, resultado, proveedor, motivoRechazo, rechazadoPor }] }`. Los ítems cancelados por el solicitante no se incluyen: ya los sabe.
- Nodos:
  1. **Webhook** (POST, path `solicitud-lista`).
  2. **Code** (JavaScript) o **HTML**: arma el cuerpo del mail recorriendo `items`. Una fila por ítem con categoría, detalle y resultado: "Comprado — {proveedor}" o "Rechazado por {rechazadoPor} — {motivoRechazo}".
  3. **Send Email**: para `{{ $json.solicitanteEmail }}`, asunto "Tu solicitud #{{ $json.solicitudId }} está lista", formato HTML, con la tabla del paso 2.
- Ejemplo del cuerpo:

  > Hola Laura, tu solicitud #1587 ya fue resuelta:
  >
  > | Ítem | Resultado |
  > |---|---|
  > | Pelotero para 30 chicos | Comprado — Inflables Don José |
  > | Globos (200) | Rechazado por Gerencia General — Hay stock en depósito |

- Verificar en Mailpit (`http://localhost:8025`): resolver todos los ítems de una solicitud y ver que llega un mail, y uno solo (el backend publica `SolicitudLista` una sola vez, T08.6).
- **Documentación:**
  - [n8n: nodo Webhook](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook)
  - [n8n: nodo HTML](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.html)
  - [n8n: nodo Send Email](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.sendemail)
  - [n8n: expresiones y datos de nodos anteriores](https://docs.n8n.io/build/work-with-data/reference-data)
  - [Mailpit: documentación](https://mailpit.axllent.org/docs/)

### T15.5 · Endpoints de recordatorios
- En `compras/ComprasApi` (pública):
  - `List<RecordatorioPendiente> recordatoriosPendientes(Instant ahora)`: gestiones en PENDIENTE o EN_COTIZACION donde `(ultimoRecordatorioEn, o solicitadoEn si nunca hubo) + intervalo <= ahora`. Intervalo: 3 días para ALTA, 21 días para MEDIA y BAJA.
  - `void registrarRecordatorio(Long itemId, Instant cuando)`.
- En `integraciones/web/`:
  - `GET /api/v1/integraciones/recordatorios-pendientes` → `[{ itemId, solicitudId, detalle, urgencia, fechaNecesaria, diasSinResolver, destinatarios: [] }]`. Destinatarios: el email del Encargado asignado o, si no hay, el de todos los Encargados (enunciado: "si nadie lo tomó, les llega a todos"; caso borde 20).
  - `POST /api/v1/integraciones/recordatorios/{itemId}/enviado` → 204. Así el mismo recordatorio no se repite (§7.5).
- **Documentación:**
  - [Spring Data JPA: consultas con `@Query` (JPQL y nativas)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query)
  - [Java: `Duration`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/Duration.html)
  - [Java: `ZoneId` (zonas horarias)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/ZoneId.html)
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)

### T15.6 · Workflow: recordatorios diarios
- **Schedule Trigger** todos los días hábiles a las 9:00 (hora de Argentina, caso borde 19) → **HTTP Request** GET recordatorios pendientes (con el header de API key, T15.7) → por cada ítem, **Send Email** a sus destinatarios → **HTTP Request** POST `.../enviado`.
- Como el backend calcula por fecha, si n8n estuvo caído varios días, al volver manda los atrasados una sola vez (§9).
- **Documentación:**
  - [n8n: nodo Schedule Trigger](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.scheduletrigger)
  - [n8n: nodo HTTP Request](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.httprequest)
  - [n8n: nodo Loop Over Items](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.splitinbatches)
  - [n8n: nodo Send Email](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.sendemail)
  - [n8n: zona horaria](https://docs.n8n.io/deploy/host-n8n/configure-n8n/basic-configuration/use-environment-variables/timezone-and-localization)

### T15.7 · Seguridad de la integración
- **API key:** `N8N_API_KEY` en `.env`. Un `SecurityFilterChain` aparte para `/api/v1/integraciones/**`, que exige el header `X-API-Key` con ese valor en vez del JWT.
  - Cómo: un segundo bean `SecurityFilterChain` con `@Order(1)` y `http.securityMatcher("/api/v1/integraciones/**")`, así solo aplica a esas rutas. La cadena general de T04.4 queda para todo lo demás.
  - En esa cadena, un filtro propio (`OncePerRequestFilter`) lee `X-API-Key`. Compararlo con `MessageDigest.isEqual(...)` y no con `equals`: `equals` corta en el primer carácter distinto y, midiendo tiempos, se puede adivinar la clave de a un carácter.
  - Si coincide, autenticar como un usuario técnico con la authority `ROLE_INTEGRACION`. Si falta o no coincide → 401 con `ErrorApi` (`NO_AUTENTICADO`).
  - Sin sesión y sin CSRF, igual que la cadena principal.
  - Con esto, un JWT de usuario no sirve en `/integraciones`, y la API key no sirve en el resto de la API (diseño §7.5: "permisos limitados a `/integraciones`").
  - En n8n, cargar la clave como credencial **Header Auth** (nombre `X-API-Key`) y usarla en los nodos HTTP Request de T15.6. Nunca escribirla en el workflow, porque se exporta al repo (T15.8).
- **Firma de los webhooks (opcional):** el backend agrega el header `X-Firma` = HMAC-SHA256 del body con `N8N_WEBHOOK_SECRET`, y n8n lo verifica con el nodo Crypto antes de mandar el mail.
  - En Java: `Mac.getInstance("HmacSHA256")` inicializado con la clave, sobre los bytes exactos del body que se envía; resultado en hexadecimal.
  - En n8n: nodo **Crypto** (acción Hmac, SHA256, misma clave) sobre el body crudo, y un nodo **If** que compara con el header. Si no coincide, el flujo termina sin mandar mail.
- Agregar `N8N_API_KEY` y `N8N_WEBHOOK_SECRET` a `back/.env.example`, sin valor.
- Test: `GET /api/v1/integraciones/recordatorios-pendientes` sin header → 401; con la clave correcta → 200; con un JWT de Encargado y sin clave → 401.
- **Documentación:**
  - [Spring Security: varias cadenas de seguridad (`securityMatcher`)](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html#_multiple_httpsecurity_instances)
  - [Spring Security: arquitectura (cadena de filtros)](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-securityfilterchain)
  - [Java: `Mac` (HMAC)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/crypto/Mac.html)
  - [RFC 2104: HMAC](https://www.rfc-editor.org/rfc/rfc2104.html)
  - [n8n: nodo Crypto](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.crypto)
  - [n8n: nodo HTTP Request](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.httprequest)

### T15.8 · Exportar los workflows al repo
- Exportar los 4 workflows desde n8n como JSON a `n8n/workflows/` (carpeta nueva en la raíz), sin credenciales.
  - Desde la interfaz: abrir el workflow → menú "…" → **Download**. Guardar con nombres fijos: `aviso-solicitud-nueva.json`, `resultado-solicitante.json`, `recordatorios-diarios.json` y `aviso-aprobacion-pendiente.json`.
  - Las credenciales (SMTP de Mailpit, Header Auth de T15.7) **no** se exportan con el workflow: el JSON solo guarda el nombre y el id de la credencial. Antes de commitear, revisar con `grep -i -E "password|apikey|x-api-key" n8n/workflows/*.json` que no haya quedado ninguna clave escrita a mano en un nodo.
- Agregar en el README (T22.2) cómo importarlos:
  1. Levantar n8n (`docker compose up -d n8n mailpit` desde `back/`).
  2. En n8n: **Import from File** con cada JSON (o por consola: `docker compose exec n8n n8n import:workflow --separate --input=/ruta/montada`).
  3. Crear las dos credenciales (SMTP: host `mailpit`, puerto `1025`; Header Auth: `X-API-Key` con el valor del `.env`) y asignarlas en los nodos que las piden.
  4. Activar los 4 workflows.
- Cada vez que alguien cambia un workflow, vuelve a exportarlo y lo sube en su PR, así el repo tiene siempre la última versión.
- **Documentación:**
  - [n8n: exportar e importar workflows](https://docs.n8n.io/build/manage-workflows/export-and-import)
  - [GitHub: sintaxis de Markdown (README) (en español)](https://docs.github.com/es/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)

### T15.9 · Workflow: aviso de aprobación pendiente
- En n8n: **Webhook** (POST, path `aprobacion-pendiente`) → **If** (`emailsAprobadores` no vacío) → **Send Email** a `emailsAprobadores`, con asunto "Tenés una compra para aprobar: {detalle}" y en el cuerpo quién la pidió, de qué sector, el monto y su moneda, qué paso te toca ({aprobadorNombre}) y el link a `/aprobaciones/{itemId}` del front.
- Los datos llegan de T15.2 cuando Aprobaciones activa un paso. Como los pasos se activan **en orden** (el GG recibe el mail recién cuando el Supervisor aprobó), cada aprobador se entera en el momento en que le toca.
- Verificar en Mailpit: crear una solicitud por encima del umbral de la regla `Supervisor → Gerencia General` y ver primero el mail al Supervisor; aprobar su paso y ver después el mail a los de Gerencia General; el solicitante no recibe ninguno.
- **Documentación:**
  - [n8n: nodo Webhook](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook)
  - [n8n: nodo If](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.if)
  - [n8n: nodo Send Email](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.sendemail)
  - [Mailpit: documentación](https://mailpit.axllent.org/docs/)

---

## T16 · Reportes (módulo `reportes`)

### T16.1 · Schema y dominio de reportes
- **Qué es este módulo:** un **modelo de lectura** (diseño §6.6). En vez de sumar consultando las tablas de Órdenes, Compras y Solicitudes (que son de otros módulos), Reportes arma sus propias tablas, que solo llenan sus listeners (T16.2). Así las consultas de reportes son simples y no tocan otro módulo.
- **Migración** `V<fecha>_1__reportes_tablas.sql`:
  - `reportes.hecho_gasto`: `orden_id` (PK), `item_id BIGINT NOT NULL UNIQUE` (para encontrar la fila cuando llega `CompraActualizada`, que trae el ítem y no la orden), `fecha DATE`, `sector_id`, `categoria_id`, `proveedor_id`, `moneda CHAR(3)`, `importe NUMERIC(16,2)` y `monto_envio NUMERIC(14,2) NOT NULL DEFAULT 0`, con el índice `(fecha, moneda)`. Una fila por orden generada.
  - `reportes.hecho_resolucion`: `item_id` (PK), `urgencia`, `creado_en TIMESTAMPTZ`, `resuelto_en TIMESTAMPTZ`, `resultado` (COMPRADO o RECHAZADO). Una fila por ítem resuelto: ahora "resuelto" incluye la espera de las aprobaciones y la compra. Agregar un índice por `resuelto_en`, que es el filtro de T16.4.
  - `reportes.evento_procesado` (§6.7), para los listeners (T12.2).
- **Dominio** en `reportes/dominio/`: `HechoGasto` y `HechoResolucion` con esos campos. La clave primaria es el id que viene del evento (`ordenId`, `itemId`), así que no se autogenera (sin `@GeneratedValue`). Eso hace natural la idempotencia: si ya existe, no se inserta de nuevo.
- `moneda` y `urgencia` usan los enums de `compartido/tipos` (`Moneda`, `Urgencia`) con `@Enumerated(EnumType.STRING)`.
- **Repositories:** `HechoGastoRepository` y `HechoResolucionRepository`. Las consultas agrupadas se agregan en T16.3 y T16.4.
- Hibernate con `validate` tiene que arrancar sin errores.
- **Documentación:**
  - [Spring Data JPA: persistir entidades](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
  - [Hibernate 7: enums (`@Enumerated`)](https://docs.hibernate.org/orm/7.4/introduction/html_single/#enums)
  - [PostgreSQL: `CREATE TABLE`](https://www.postgresql.org/docs/current/sql-createtable.html)
  - [PostgreSQL: tipos numéricos (`NUMERIC`)](https://www.postgresql.org/docs/current/datatype-numeric.html)

### T16.2 · Alimentar los reportes con eventos
- Listener de `OrdenGenerada` → crea un `HechoGasto`: `ordenId`, `itemId`, `fecha` (día de emisión), `sectorId`, `categoriaId`, `proveedorId`, `moneda`, `importe` = `precioTotal`, `montoEnvio` = 0. Idempotente por `ordenId`.
- Listener de `CompraActualizada` → actualiza `importe` y `monto_envio` del `HechoGasto` de ese `itemId`. Idempotente (aplicar los mismos valores dos veces no cambia nada). Si el hecho todavía no existe (el evento llegó antes que `OrdenGenerada`), lanzar una excepción para que se reintente (T12.1), igual que en T11.6.
- Listener de `ItemResuelto` → crea un `HechoResolucion`: `itemId`, `urgencia`, `creadoEn` = `solicitadoEn`, `resueltoEn`, `resultado` (COMPRADO o RECHAZADO).
- Listener de `CategoriaUnificada` → actualiza `categoriaId` de origen a destino en `hecho_gasto`. Es un modelo de lectura propio del módulo, así que acá sí se puede reescribir (caso borde 9: "los reportes siguen la redirección").
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Data JPA: consultas de modificación (`@Modifying`)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.modifying-queries)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)

### T16.3 · Reporte de gasto
- `GET /api/v1/reportes/gasto?agrupar=sector|categoria|proveedor&desde=&hasta=` → `[{ clave, nombre, moneda, total, envio, cantidadOrdenes }]`. `total` es la suma de `importe` (lo comprado) y `envio` la de `monto_envio`; se devuelven por separado y el front muestra también su suma ("total con envío"). Pesos y dólares nunca se suman entre sí.
- Consulta con `@Query` agrupando por la columna elegida **y por moneda**. Pesos y dólares nunca se suman (regla 10).
- El gasto cuenta lo **comprado** (hay una orden), no lo aprobado ni lo estimado. Un ítem aprobado que todavía no se compró no aparece.
- Nombres vía `AuthApi.nombreSector` y `CatalogoApi` (categoría y proveedor).
- `desde` y `hasta` obligatorios; `desde` no puede ser posterior a `hasta` (400).
- **Documentación:**
  - [Spring Data JPA: consultas con `@Query` (JPQL y nativas)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query)
  - [Spring Data JPA: proyecciones (leer solo algunos campos)](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html)
  - [PostgreSQL: `GROUP BY`](https://www.postgresql.org/docs/current/queries-table-expressions.html#QUERIES-GROUP)
  - [PostgreSQL: funciones de agregación (`sum`, `avg`, `count`)](https://www.postgresql.org/docs/current/functions-aggregate.html)
  - [Spring MVC: `@RequestParam`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)

### T16.4 · Tiempo de resolución
- `GET /api/v1/reportes/tiempos-resolucion?desde=&hasta=` → `{ cantidadItems, promedioHoras, porUrgencia: { ALTA: { cantidad, promedioHoras }, MEDIA: ..., BAJA: ... } }`.
- Promedio de `resueltoEn - creadoEn` de los ítems resueltos en el rango.
- **Rango:** se filtra por `resuelto_en` (cuándo se resolvió). `desde` y `hasta` son fechas; se toman como días completos en hora de Argentina: desde las 00:00 de `desde` hasta las 00:00 del día siguiente a `hasta` (caso borde 19). Mismas validaciones que T16.3: los dos obligatorios, `desde` <= `hasta`, si no 400.
- **Consulta:** conviene hacerla nativa en Postgres, que resta fechas directamente:

  ```sql
  SELECT urgencia, count(*) AS cantidad,
         avg(extract(epoch FROM resuelto_en - creado_en)) / 3600 AS promedio_horas
  FROM reportes.hecho_resolucion
  WHERE resuelto_en >= :desde AND resuelto_en < :hasta
  GROUP BY urgencia
  ```

  El promedio general se calcula en Java con estos tres grupos (ponderando por cantidad), sin otra consulta. Para leer el resultado, una interfaz de proyección (`interface TiempoPorUrgencia { String getUrgencia(); long getCantidad(); Double getPromedioHoras(); }`).
- Una urgencia sin ítems resueltos aparece con `cantidad: 0` y `promedioHoras: null` (no 0, que parecería "resuelto al instante"). Redondear a 1 decimal.
- Se cuentan comprados y rechazados: los dos son "resueltos". El tiempo incluye la espera de las aprobaciones (decisión 9).
- Solo `ENCARGADO` y `ADMIN`.
- Test: 2 ítems ALTA resueltos en 10 h y 20 h → ALTA promedio 15; MEDIA sin ítems → cantidad 0 y promedio null.
- **Documentación:**
  - [Spring Data JPA: consultas con `@Query` (JPQL y nativas)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query)
  - [Spring Data JPA: proyecciones (leer solo algunos campos)](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html)
  - [PostgreSQL: funciones de agregación (`sum`, `avg`, `count`)](https://www.postgresql.org/docs/current/functions-aggregate.html)
  - [PostgreSQL: fechas y horas (`DATE`, `TIMESTAMPTZ`)](https://www.postgresql.org/docs/current/datatype-datetime.html)

---

## T17 · Base del frontend, login y navegación

### T17.1 · Estructura, rutas y layout
- Borrar el contenido de ejemplo de Vite (`App.css`, `assets/`, el contador de `App.jsx`) y crear las carpetas `api/`, `auth/`, `components/`, `pages/` y `utils/`.
- `npm install react-router` y definir las rutas en `App.jsx`:

| Ruta | Página | Roles |
|---|---|---|
| `/login` | `pages/Login.jsx` | Público |
| `/solicitudes` | `pages/solicitante/MisSolicitudes.jsx` | Todos |
| `/solicitudes/nueva` | `pages/solicitante/NuevaSolicitud.jsx` | Todos |
| `/solicitudes/:id` | `pages/solicitante/DetalleSolicitud.jsx` | Todos |
| `/aprobaciones` y `/aprobaciones/:itemId` | `pages/aprobador/Aprobaciones.jsx` y `DetalleAprobacion.jsx` | Todos (el menú solo lo muestra si `GET /aprobaciones/resumen` dice `esAprobador`, y a los SUPERVISOR siempre) |
| `/bandeja` | `pages/encargado/Bandeja.jsx` | ENCARGADO |
| `/bandeja/:itemId` | `pages/encargado/GestionItem.jsx` | ENCARGADO |
| `/ordenes` | `pages/encargado/Ordenes.jsx` | ENCARGADO |
| `/catalogo/proveedores` y `/catalogo/categorias` | `pages/encargado/Proveedores.jsx` y `Categorias.jsx` | ENCARGADO |
| `/admin/usuarios` y `/admin/sectores` | `pages/admin/Usuarios.jsx` y `Sectores.jsx` | ADMIN |
| `/admin/reglas` | `pages/admin/Reglas.jsx` | ADMIN |
| `/reportes` | `pages/reportes/Reportes.jsx` | ENCARGADO, ADMIN |

- `components/Layout.jsx`: barra superior con el nombre del usuario, botón "Salir", y menú con solo las opciones de su rol. Las páginas que todavía no existen muestran "En construcción".
- **Documentación:**
  - [React Router: instalación (modo declarativo)](https://reactrouter.com/start/declarative/installation)
  - [React Router: rutas y layouts](https://reactrouter.com/start/declarative/routing)
  - [React: aprender React (en español)](https://es.react.dev/learn)
  - [React: renderizado condicional (en español)](https://es.react.dev/learn/conditional-rendering)

### T17.2 · Cliente HTTP
- `api/cliente.js` con una función `api(ruta, { method, body, params })`:
  - Llama a `/api/v1` + ruta con `fetch`.
  - Agrega `Authorization: Bearer <token>` si hay token guardado.
  - Manda y recibe JSON.
  - Si la respuesta es 401: borra el token y redirige a `/login`.
  - Si no es 2xx: lee el `{ codigo, mensaje, campos }` del backend (T03.2) y lanza un `ApiError` con esos datos.
- Otra función `apiArchivo` para subir `FormData` y descargar archivos (la usa T20.3).
- Un archivo por área con una función por endpoint: `api/auth.js`, `api/solicitudes.js`, `api/catalogo.js`, `api/compras.js`, `api/ordenes.js`, `api/admin.js`, `api/reportes.js`, `api/historial.js`. Por ejemplo, `crearSolicitud(datos)` llama a `api('/solicitudes', { method: 'POST', body: datos })`.
- **Documentación:**
  - [MDN: usar Fetch (en español)](https://developer.mozilla.org/es/docs/Web/API/Fetch_API/Using_Fetch)
  - [MDN: header `Authorization` (en español)](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Headers/Authorization)
  - [MDN: `Error` (para crear `ApiError`) (en español)](https://developer.mozilla.org/es/docs/Web/JavaScript/Reference/Global_Objects/Error)
  - [MDN: `localStorage` (en español)](https://developer.mozilla.org/es/docs/Web/API/Window/localStorage)
  - [Vite: `server.proxy` (en español)](https://es.vite.dev/config/server-options#server-proxy)
  - [Vite: variables de entorno (en español)](https://es.vite.dev/guide/env-and-mode)

### T17.3 · Login, usuario actual y rutas protegidas
- `auth/AuthContext.jsx`: contexto con `usuario`, `login(email, password)` y `logout()`.
  - `login` llama a `POST /auth/login`, guarda el token en `localStorage` y el usuario en el estado.
  - Al cargar la app, si hay token, llama a `GET /auth/yo`. Si falla, `logout`.
- `auth/RutaProtegida.jsx`: si no hay usuario, redirige a `/login`; si el rol no está entre los permitidos, muestra "No tenés permiso".
- `pages/Login.jsx`: formulario de email y contraseña, mensaje de error del backend, y al entrar redirige según el rol (Encargado a `/bandeja`, Admin a `/admin/usuarios`, el resto a `/solicitudes`).
- **Documentación:**
  - [React: pasar datos con contexto (en español)](https://es.react.dev/learn/passing-data-deeply-with-context)
  - [React: `useContext` (en español)](https://es.react.dev/reference/react/useContext)
  - [React: `useEffect` (en español)](https://es.react.dev/reference/react/useEffect)
  - [React Router: navegar (`useNavigate`, `<Navigate>`)](https://reactrouter.com/start/declarative/navigating)
  - [MDN: `localStorage` (en español)](https://developer.mozilla.org/es/docs/Web/API/Window/localStorage)

### T17.4 · Componentes comunes
- `components/TablaPaginada.jsx`: recibe columnas y una función que trae una página; muestra la tabla y los botones anterior/siguiente.
- `components/MensajeError.jsx`: muestra el `mensaje` de un `ApiError` y, si hay `campos`, la lista de campos inválidos.
- `components/EstadoBadge.jsx`: etiqueta de color por estado (PENDIENTE gris, EN_COTIZACION azul, COMPRADO verde, RECHAZADO rojo, CANCELADO tachado, y los de solicitud y orden). También el estado de aprobación (EN_CURSO amarillo, APROBADA verde) y los estados de un paso (aprobado, activo, esperando, rechazado, omitido) que usa `PasosAprobacion` (T25.1).
- `components/UrgenciaBadge.jsx`: ALTA rojo, MEDIA amarillo, BAJA gris.
- `utils/formato.js`: fechas en formato `dd/mm/aaaa` e importes con `Intl.NumberFormat('es-AR', { style: 'currency', currency })` para ARS y USD.
- **Documentación:**
  - [React: pasar props a un componente (en español)](https://es.react.dev/learn/passing-props-to-a-component)
  - [React: renderizar listas (en español)](https://es.react.dev/learn/rendering-lists)
  - [MDN: `<table>` (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/table)
  - [MDN: `Intl.NumberFormat` (formato de moneda) (en español)](https://developer.mozilla.org/es/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat)
  - [MDN: `Intl.DateTimeFormat` (formato de fechas) (en español)](https://developer.mozilla.org/es/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat)
  - [MDN: `text-decoration` (tachado) (en español)](https://developer.mozilla.org/es/docs/Web/CSS/Reference/Properties/text-decoration)

---

## T18 · Pantallas del Solicitante

### T18.1 · Nueva solicitud
- `pages/solicitante/NuevaSolicitud.jsx`:
  - Campos generales: urgencia (Baja, Media, Alta), fecha necesaria (input date con `min` = hoy) y observaciones.
  - Lista de ítems, con botón "Agregar ítem" y "Quitar" en cada uno (mínimo 1). Cada ítem tiene: tipo (Producto o Servicio), categoría, cantidad (> 0, admite decimales, caso borde 22), detalle y **precio estimado** con su moneda (ARS o USD). Debajo del precio, un texto de ayuda: "Precio total estimado del ítem. Averiguá y consultalo con tu jefe antes de cargarlo". Es obligatorio y mayor a 0.
  - `components/AutocompletarCategoria.jsx`: al escribir (esperando 300 ms desde la última tecla) llama a `GET /categorias?tipo=&q=` y muestra sugerencias. Si el usuario elige una, se manda `categoriaId`; si escribe una que no existe, se manda `categoriaNueva` y se muestra "Se creará como categoría nueva".
  - Validar antes de enviar. Al enviar, `POST /solicitudes` con un `Idempotency-Key` generado con `crypto.randomUUID()` al abrir el formulario. Si sale bien, ir al detalle; si hay errores por campo, mostrarlos.
- **Documentación:**
  - [React: `<form>` (en español)](https://es.react.dev/reference/react-dom/components/form)
  - [React: `<input>` (en español)](https://es.react.dev/reference/react-dom/components/input)
  - [React: `<select>` (en español)](https://es.react.dev/reference/react-dom/components/select)
  - [React: actualizar arrays en el estado (en español)](https://es.react.dev/learn/updating-arrays-in-state)
  - [React: reaccionar a la entrada con estado (en español)](https://es.react.dev/learn/reacting-to-input-with-state)
  - [MDN: `<input type="date">` (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/input/date)
  - [MDN: `setTimeout` (para esperar 300 ms) (en español)](https://developer.mozilla.org/es/docs/Web/API/Window/setTimeout)
  - [MDN: `crypto.randomUUID()`](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/randomUUID)

### T18.2 · Mis solicitudes
- `pages/solicitante/MisSolicitudes.jsx`: `TablaPaginada` con `GET /solicitudes?mias=true&estado=`. Columnas: número, fecha de creación, urgencia, fecha necesaria, cantidad de ítems y estado. Filtro por estado y botón "Nueva solicitud". Clic en una fila abre el detalle.
- Detalles:
  - Filtro de estado: un `<select>` con "Todas", "En revisión", "Lista", "Cerrada" y "Cancelada" (los valores de `EstadoSolicitud`, T07.1). Al cambiarlo, volver a la página 0.
  - Guardar el filtro y la página en la URL (`?estado=LISTA&page=2`) con `useSearchParams` de React Router, así al volver del detalle con "Atrás" se ve lo mismo que antes.
  - Urgencia y estado con `UrgenciaBadge` y `EstadoBadge`; fechas con `utils/formato.js` (T17.4).
  - Clic en la fila: `navigate('/solicitudes/' + id)`.
  - Mientras carga, "Cargando…"; si falla, `MensajeError`; si no hay ninguna, "Todavía no hiciste solicitudes" con el botón para crear la primera.
- La función `listarSolicitudes({ estado, page })` va en `api/solicitudes.js` (T17.2).
- **Documentación:**
  - [React: sincronizar con efectos (cargar datos) (en español)](https://es.react.dev/learn/synchronizing-with-effects)
  - [React Router: parámetros y query de la URL (`useParams`, `useSearchParams`)](https://reactrouter.com/start/declarative/url-values)
  - [React Router: navegar (`useNavigate`, `<Navigate>`)](https://reactrouter.com/start/declarative/navigating)
  - [React: `<select>` (en español)](https://es.react.dev/reference/react-dom/components/select)

### T18.3 · Detalle de una solicitud
- `pages/solicitante/DetalleSolicitud.jsx` con `GET /solicitudes/{id}`:
  - Encabezado con los datos generales y el estado.
  - Tabla de ítems: tipo, categoría, detalle, cantidad, **precio estimado y precio total** (si el Encargado ya lo cargó), estado, y resultado (proveedor comprado, o motivo del rechazo y **quién rechazó**: "Rechazado por Gerencia General").
  - La cadena de aprobación de cada ítem ("Supervisor ✔ → Gerencia General ⏳") la agrega T25.1 con el componente `PasosAprobacion`, y el precio se actualiza solo con T24.2. Mientras tanto la pantalla funciona sin eso.
  - En ítems PENDIENTES: botón "Editar" (formulario inline → `PATCH` con la `version`) y "Borrar" (confirmación → `DELETE`).
  - Botón "Cancelar solicitud", visible solo si todos los ítems están PENDIENTES.
  - Si la API devuelve 409 o 422 (el Encargado ya lo tomó), mostrar el mensaje y recargar los datos.
- **Documentación:**
  - [React Router: parámetros y query de la URL (`useParams`, `useSearchParams`)](https://reactrouter.com/start/declarative/url-values)
  - [React: manejar el estado (en español)](https://es.react.dev/learn/managing-state)
  - [React: renderizado condicional (en español)](https://es.react.dev/learn/conditional-rendering)
  - [MDN: 409 Conflict](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/409)
  - [MDN: 422 Unprocessable Content](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/422)
  - [MDN: `<dialog>` (ventanas modales) (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/dialog)

---

## T19 · Pantallas del Encargado

### T19.1 · Bandeja
- `pages/encargado/Bandeja.jsx` con `GET /bandeja?asignadosAMi=&urgencia=`:
  - Columnas: urgencia, fecha necesaria, categoría, detalle, cantidad, precio (el total si existe; si no, el estimado, indicando cuál es), estado, **aprobación** ("En curso" o "Aprobada", con `EstadoBadge`) y Encargado asignado.
  - Filtros: "Solo los míos", urgencia y una pestaña **"Para comprar"** (`paraComprar=true`): los ítems con la cadena aprobada, que son los que el Encargado puede comprar ya.
  - Botones según el caso: "Tomar" (PENDIENTE o sin asignar), "Reasignarme" (asignado a otro) y "Abrir" (asignado a mí).
  - Aviso visual en los ítems con `todasVencidas`.
  - Si "Tomar" devuelve 409: mostrar "Otro Encargado lo tomó recién" y recargar.
- **Documentación:**
  - [React: sincronizar con efectos (cargar datos) (en español)](https://es.react.dev/learn/synchronizing-with-effects)
  - [React: responder a eventos (en español)](https://es.react.dev/learn/responding-to-events)
  - [React Router: parámetros y query de la URL (`useParams`, `useSearchParams`)](https://reactrouter.com/start/declarative/url-values)
  - [MDN: 409 Conflict](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/409)

### T19.2 · Gestión de un ítem
- `pages/encargado/GestionItem.jsx` con `GET /bandeja/{itemId}`:
  - **Datos del ítem** y de la solicitud (urgencia, fecha necesaria, detalle, cantidad).
  - **Proveedores sugeridos** (`GET /bandeja/{itemId}/proveedores-sugeridos`), con un botón "Cotizar" que completa el proveedor en el formulario. Si no hay, botón "Dar de alta proveedor" con un formulario rápido (`POST /proveedores` con la categoría del ítem).
  - **Formulario de cotización:** proveedor, moneda, precio unitario, válida hasta, entrega estimada y observaciones. Muestra el total calculado (precio × cantidad) mientras se escribe.
  - **Cotizaciones cargadas**, separadas por moneda. Las vencidas se ven en gris y no se pueden elegir. Botón "Elegir" en cada vigente → `POST .../elegir` (T10.3).
  - **Precio del ítem:** muestra el precio estimado del solicitante y el precio total (elegido o cargado a mano). Un campo para cargar el total a mano (`PUT precio-total`) y un botón "Quitar". Si el cambio de precio hace que la cadena pida más aprobaciones, mostrar un aviso después de guardar (llega por tiempo real).
  - **Cadena de aprobación:** el componente `PasosAprobacion` (T25.1) de solo lectura, para que el Encargado sepa quién falta.
  - **Registrar compra:** botón habilitado solo con la cadena APROBADA y precio total cargado; si no, deshabilitado con el motivo ("Faltan aprobaciones" o "Cargá el precio total"). Pide el proveedor si no hay cotización elegida → `POST comprar`.
  - **Después de comprar:** formulario "Corregir precio" y "Monto de envío" (`PATCH compra`, T10.8), con una nota de que el cambio queda en el historial.
  - **Rechazar:** ventana con el motivo (obligatorio) → `POST rechazar`.
  - Mostrar los errores 422 del backend (por ejemplo `COTIZACION_VENCIDA`, `APROBACION_PENDIENTE`) y recargar.
- **Documentación:**
  - [React: manejar el estado (en español)](https://es.react.dev/learn/managing-state)
  - [React: `<form>` (en español)](https://es.react.dev/reference/react-dom/components/form)
  - [React: renderizado condicional (en español)](https://es.react.dev/learn/conditional-rendering)
  - [MDN: `Intl.NumberFormat` (formato de moneda) (en español)](https://developer.mozilla.org/es/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat)
  - [MDN: `<dialog>` (ventanas modales) (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/dialog)
  - [MDN: 422 Unprocessable Content](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/422)

### T19.3 · Órdenes
- `pages/encargado/Ordenes.jsx` con `GET /ordenes?estado=`: pestañas "Por enviar" (GENERADA) y "Enviadas". Columnas: número, fecha, proveedor, cantidad, moneda y total. Botón "Marcar como enviada" (con confirmación) y, cuando exista T11.5, "Descargar PDF".
- Detalles:
  - Columnas: número de orden, fecha de emisión, proveedor, qué se compra (`detalle`), cantidad, total con su moneda (`Intl.NumberFormat`, T17.4) y, si lo hay, el envío. En "Enviadas", además la fecha de envío.
  - Nunca sumar importes de monedas distintas en un total general (regla 10).
  - "Marcar como enviada": confirmación ("¿Ya le mandaste la orden N° 123 a {proveedor}?"), después `POST /ordenes/{id}/marcar-enviada`. Si sale bien, la fila pasa a la pestaña "Enviadas" (recargar la lista). Si devuelve 422 `ORDEN_YA_ENVIADA` (otro Encargado la marcó), mostrar el mensaje y recargar.
  - Deshabilitar el botón mientras espera la respuesta, para evitar el doble click.
  - "Descargar PDF": con `apiArchivo` (T17.2), porque un `<a href>` común no manda el token. Se recibe un `Blob`, se crea una URL con `URL.createObjectURL` y se descarga como `orden-{numero}.pdf`.
  - Guardar la pestaña activa en la URL (`?estado=ENVIADA`).
- Funciones en `api/ordenes.js`: `listarOrdenes`, `marcarEnviada`, `descargarPdf`.
- **Documentación:**
  - [MDN: `URL.createObjectURL` (descargar un Blob) (en español)](https://developer.mozilla.org/es/docs/Web/API/URL/createObjectURL_static)
  - [MDN: `Blob` (en español)](https://developer.mozilla.org/es/docs/Web/API/Blob)
  - [React: responder a eventos (en español)](https://es.react.dev/learn/responding-to-events)
  - [React Router: parámetros y query de la URL (`useParams`, `useSearchParams`)](https://reactrouter.com/start/declarative/url-values)

### T19.4 · Catálogo
- `pages/encargado/Proveedores.jsx`: tabla con buscador, alta y edición en un formulario (razón social, CUIT, teléfono, email, contacto, categorías con selección múltiple usando el autocompletado de T18.1) y activar/desactivar.
- `pages/encargado/Categorias.jsx`: lista de categorías NUEVAS con botones "Confirmar" (`PATCH` a CONOCIDA) y "Unificar con..." (elegir la categoría destino → `POST unificar`).
- Detalles de Proveedores:
  - Buscador por razón social y filtro "Solo activos", con `GET /proveedores?q=&activo=&page=` (T06.3).
  - Validaciones antes de enviar: razón social obligatoria; CUIT opcional con formato `XX-XXXXXXXX-X`; email válido; al menos una categoría.
  - Errores del backend: 409 (CUIT repetido) se muestra junto al campo CUIT.
  - Desactivar en vez de borrar (`PATCH` con `activo: false`), con confirmación. Un proveedor inactivo deja de aparecer en los sugeridos, pero las órdenes viejas lo siguen mostrando (caso borde 8).
- Detalles de Categorías:
  - "Confirmar" permite corregir el nombre antes de confirmar (T06.6).
  - "Unificar con...": un autocompletado (el de T18.1) que solo ofrece categorías del **mismo tipo** y que no estén UNIFICADAS. Antes de enviar, confirmación explicando que la categoría desaparece de las búsquedas y sus proveedores pasan a la otra.
  - Después de cada acción, recargar la lista.
- Funciones en `api/catalogo.js`.
- **Documentación:**
  - [React: `<form>` (en español)](https://es.react.dev/reference/react-dom/components/form)
  - [React: hooks personalizados (en español)](https://es.react.dev/learn/reusing-logic-with-custom-hooks)
  - [MDN: `<dialog>` (ventanas modales) (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/dialog)

---

## T20 · Administración, reportes, adjuntos e historial en el front

### T20.1 · Administración
- `pages/admin/Usuarios.jsx`: tabla con buscador y filtro por rol; formulario de alta (email, nombre, sector, rol, contraseña inicial); edición de nombre, sector, rol y activo; cambio de contraseña. Mostrar el error `ULTIMO_ENCARGADO` si aparece.
- `pages/admin/Sectores.jsx`: lista, alta y edición de nombre.
- Detalles de Usuarios:
  - `TablaPaginada` con `GET /admin/usuarios?q=&rol=&page=` (T05.3). Columnas: nombre, email, sector, rol, activo.
  - El formulario de alta y el de edición pueden ser el mismo componente. En edición el email no se cambia y no se muestra la contraseña.
  - Sector: `<select>` cargado con `GET /admin/sectores`. Rol: Solicitante, Encargado, **Supervisor** o Administrador.
  - Validar antes de enviar: email con formato válido, contraseña de 8 caracteres o más.
  - Errores: 409 (email repetido) junto al campo; 422 `ULTIMO_ENCARGADO` (T05.4) como mensaje arriba del formulario, explicando que primero hay que dar de alta otro Encargado.
  - "Cambiar contraseña": un diálogo aparte con la contraseña nueva y su confirmación → `POST /admin/usuarios/{id}/password`.
  - Desactivar con confirmación. No hay botón de borrar (los usuarios se referencian en solicitudes e historial).
- Detalles de Sectores: nombre obligatorio; 409 si ya existe. No hay borrar (T05.2).
- Funciones en `api/admin.js`.
- **Documentación:**
  - [React: `<form>` (en español)](https://es.react.dev/reference/react-dom/components/form)
  - [React: `<select>` (en español)](https://es.react.dev/reference/react-dom/components/select)
  - [MDN: `<dialog>` (ventanas modales) (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/dialog)

### T20.2 · Reportes
- `pages/reportes/Reportes.jsx`:
  - Filtros: desde, hasta (por defecto, el mes actual) y agrupar por sector, categoría o proveedor.
  - Dos tablas (ARS y USD) con nombre, total, envío, total con envío y cantidad de órdenes, y un gráfico de barras por moneda (`npm install recharts`).
  - Tarjetas de tiempo de resolución: promedio general y por urgencia, en días y horas.
- **Documentación:**
  - [Recharts: primeros pasos](https://recharts.github.io/en-US/guide/getting-started/)
  - [Recharts: `BarChart`](https://recharts.github.io/en-US/api/BarChart/)
  - [MDN: `Intl.NumberFormat` (formato de moneda) (en español)](https://developer.mozilla.org/es/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat)
  - [MDN: `<input type="date">` (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/input/date)

### T20.3 · Adjuntos
- `components/Adjuntos.jsx` (recibe si es de ítem o de cotización, y su id):
  - Lista de adjuntos con botón de descarga (con `fetch` y el token; se descarga como blob, porque un link común no manda el token).
  - Input de archivo con `accept="application/pdf,image/png,image/jpeg"`, que controla los 10 MB antes de subir y sube con `FormData`.
- Usarlo en `DetalleSolicitud` (por ítem) y en `GestionItem` (por cotización).
- **Documentación:**
  - [MDN: `FormData` (en español)](https://developer.mozilla.org/es/docs/Web/API/FormData)
  - [MDN: `<input type="file">` y `accept` (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/input/file)
  - [MDN: `File` (tamaño y tipo de un archivo) (en español)](https://developer.mozilla.org/es/docs/Web/API/File)
  - [MDN: `URL.createObjectURL` (descargar un Blob) (en español)](https://developer.mozilla.org/es/docs/Web/API/URL/createObjectURL_static)
  - [MDN: `Blob` (en español)](https://developer.mozilla.org/es/docs/Web/API/Blob)

### T20.4 · Historial
- `components/Historial.jsx`: línea de tiempo con `GET /historial?entidad=SOLICITUD&id=&incluirItems=true` (fecha, quién, qué cambió y comentario). Se muestra en `DetalleSolicitud`, desplegable.
- Detalles:
  - Recibe `entidad` e `id` por props, así se puede reusar (por ejemplo en `GestionItem` con `entidad=ITEM`).
  - Carga los datos recién cuando se despliega (no en cada visita al detalle).
  - Cada entrada: fecha y hora (`dd/mm/aaaa hh:mm`), quién ("Sistema" si no hay usuario), y una frase armada con `accion`, `estadoAnterior` y `estadoNuevo`. Por ejemplo "Ítem 4021: En cotización → Comprado" o "Gerencia General aprobó". Si hay `comentario` (por ejemplo el motivo de un rechazo), mostrarlo debajo.
  - Pasar los códigos a texto legible con un mapa en `utils/` (`EN_COTIZACION` → "En cotización"). Conviene que sea el mismo que usa `EstadoBadge`.
  - Lo ven el solicitante dueño, los Encargados y el Admin (T13.3). Si la API devuelve 403, no mostrar la sección.
- Función en `api/historial.js`: `obtenerHistorial({ entidad, id, incluirItems })`.
- **Documentación:**
  - [React: sincronizar con efectos (cargar datos) (en español)](https://es.react.dev/learn/synchronizing-with-effects)
  - [React: renderizar listas (en español)](https://es.react.dev/learn/rendering-lists)
  - [MDN: `Intl.DateTimeFormat` (formato de fechas) (en español)](https://developer.mozilla.org/es/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat)

---

## T21 · Calidad y prueba completa

### T21.1 · Tests de reglas de dominio
- Cada subtarea que agrega reglas a una entidad trae sus tests unitarios (sin Spring ni base de datos). Como mínimo:
  - Transiciones de `Item`.
  - `Solicitud.recalcularEstado()`.
  - `GestionItem.tomar`, `reasignar`, `fijarPrecioTotal`, `registrarCompra` (no compra sin cadena aprobada ni sin precio) y `rechazar`.
  - `MotorDeReglas` y `EvaluadorMonto` (la tabla de T23.2), `AprobacionItem.pasosActivos()` y la re-evaluación de la cadena (T23.4).
  - `Cotizacion.estaVigente`.
  - Cálculo del total y del precio unitario de una orden (`precioTotal / cantidad`).
  - Intervalo de recordatorios.
- **Documentación:**
  - [Spring Boot: testing de aplicaciones](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html)
  - [Java: `ZoneId` (zonas horarias)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/ZoneId.html)

### T21.2 · Tests de endpoints
- Cada controller tiene un test con MockMvc que cubre el caso feliz, una validación (400), un permiso (403) y un error de regla (422 o 409). El usuario se simula con `jwt()` (T04.4).
- **Documentación:**
  - [Spring Boot: tests de la capa web con `@WebMvcTest`](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)
  - [Spring: MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html)
  - [Spring Security: simular un JWT en tests con MockMvc (`jwt()`)](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
  - [Guía oficial: Testing the Web Layer](https://spring.io/guides/gs/testing-web/)

### T21.3 · Prueba completa
- Con la app levantada y los datos de prueba, recorrer el ejemplo de Laura del enunciado (pasos 1 a 9), con un navegador por usuario, anotando cada resultado en `Docs/pruebas/prueba-completa.md`. Con el requisito nuevo, el recorrido pasa por: Laura carga el pedido con **precio estimado** → el Encargado lo toma y carga cotizaciones → el **Supervisor** aprueba su paso → (si el monto supera el umbral) **Gerencia General** aprueba el suyo → el Encargado **registra la compra** → se genera la orden → la marca como enviada → Laura recibe el aviso.
- Probar las reglas de aprobación: (1) un pedido chico que se aprueba solo; (2) uno grande con `Supervisor → Gerencia General`, comprobando que el GG recibe el mail recién cuando el Supervisor aprobó; (3) el Encargado intenta comprar antes de que termine la cadena y el sistema se lo impide; (4) **cambiar de `Supervisor` a `Supervisor → GG` editando la regla por defecto, sin tocar código**; (5) un aprobador rechaza solo uno de los dos ítems y el otro sigue su camino.
- **Tiempo real:** con dos navegadores abiertos, el Encargado carga el precio total y el aprobador lo ve cambiar sin recargar. Probar también con el stream cortado (plan B de T24.2).
- Probar los casos borde 1 a 4 (concurrencia) con dos navegadores a la vez, el 12 y el 13 (todos cancelados, todos rechazados) y los nuevos 24 a 28 (T23): sobre todo el 26 (cambia el precio y la cadena crece) y el 28 (dos de Gerencia General deciden a la vez).
- Cada error que aparezca se abre como issue nuevo en el milestone de Cierre (y se agrega a `03-area-dificultad-fechas.md`).
- **Documentación:**
  - [GitHub: milestones (en español)](https://docs.github.com/es/issues/using-labels-and-milestones-to-track-work/about-milestones)
  - [GitHub: sintaxis de Markdown (README) (en español)](https://docs.github.com/es/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)

---

## T22 · Documentación, empaquetado y demo

### T22.1 · Actualizar el diseño
- En `Docs/diseno-del-sistema.md`, reflejar las decisiones del principio de este archivo (módulo compartido, eventos nuevos, columnas agregadas, registro de Modulith en vez de outbox manual, adjuntos locales) y cualquier cambio posterior.
- Secciones a tocar:
  - §1: requisitos nuevos de aprobaciones (reglas por monto, cadena de pasos, precio estimado y total, compra y envío, tiempo real) como RF18 en adelante, y corregir RF9, RF10 y RF11.
  - §4 y §5: módulo `compartido`; **módulo `aprobaciones`**; outbox reemplazado por el registro de eventos de Spring Modulith (`event_publication`); adjuntos en carpeta local; n8n y Mailpit en Docker; SSE para el tiempo real.
  - §6: columnas y tablas agregadas (lista en "Decisiones que cambian el diseño original", incluido el schema `aprobaciones` completo de T23.1). Sacar la tabla `outbox` de §6.7 o marcarla como reemplazada.
  - §7: endpoints que se agregaron o cambiaron (login, `/auth/yo`, admin, historial, adjuntos multipart en vez de URL prefirmada, recordatorios) y el formato de errores real (`ErrorApi`).
  - Una tabla nueva con todos los eventos (la de T07.3).
  - §10: casos borde 24 a 28 (los de T23).
  - **`Docs/enunciado.md`:** ya se actualizó el 08/10 (usuarios, ejemplo de Laura en 9 pasos, estados del ítem, reglas de aprobación, módulos, reglas 4, 6, 7 y 11 a 16). En el cierre, releerlo contra lo que se implementó de verdad.
- Cómo encontrar todo lo que cambió: `git log --oneline` y los PRs mergeados. Revisar cada migración de `db/migration/` contra §6.
- Actualizar la fecha y agregar al principio una sección "Cambios respecto de la versión inicial", para que el corrector vea qué se modificó y por qué.
- **Documentación:**
  - [GitHub: sintaxis de Markdown (README) (en español)](https://docs.github.com/es/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)

### T22.2 · README de instalación
- `README.md` en la raíz: requisitos (Java 25, Docker, Node 22), cómo completar los `.env`, cómo levantar backend, frontend y n8n, cómo importar los workflows, usuarios de prueba y cómo correr los tests.
- Estructura sugerida:
  1. **Qué es**: una línea sobre el sistema y un link al enunciado y al diseño.
  2. **Requisitos**, con la versión exacta de cada uno.
  3. **Configuración**: copiar `back/.env.example` a `back/.env` y `frontend/.env.example` a `frontend/.env`; explicar cada variable (qué es y un valor de ejemplo, **nunca** valores reales).
  4. **Levantar en desarrollo**: backend (`cd back && ./mvnw spring-boot:run`, que levanta Postgres con Docker Compose), frontend (`cd frontend && npm install && npm run dev`), n8n y Mailpit (T15.1), con las URLs de cada uno (app, Swagger, n8n, Mailpit).
  5. **Workflows de n8n**: los pasos de T15.8.
  6. **Usuarios de prueba** (T04.3): los emails, y que la contraseña es la de `DATOS_PRUEBA_PASSWORD`.
  7. **Tests**: `./mvnw test` (necesita Docker, por Testcontainers) y `npm run lint`.
  8. **Demo con un solo comando** (T22.3).
  9. **Problemas comunes**: puerto 5432 ocupado, Docker apagado, volumen de otra versión de Postgres (T01.4).
- Probarlo en una máquina (o carpeta) limpia siguiendo solo el README. Si hace falta algo que no dice, agregarlo.
- **Documentación:**
  - [GitHub: sintaxis de Markdown (README) (en español)](https://docs.github.com/es/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)
  - [Spring Boot: soporte de Docker Compose](https://docs.spring.io/spring-boot/4.1.1/reference/features/dev-services.html#features.dev-services.docker-compose)
  - [Vite: variables de entorno (en español)](https://es.vite.dev/guide/env-and-mode)

### T22.3 · Empaquetado para la demo
- `back/Dockerfile` multi-etapa: compilar con `./mvnw package` y correr con una imagen de Java 25.
- `frontend/Dockerfile`: `npm run build` y servir `dist/` con nginx, redirigiendo `/api` al backend.
- `compose.demo.yaml` que levante todo (Postgres, backend, frontend, n8n y Mailpit) con un solo comando.
- **Documentación:**
  - [Spring Boot: Dockerfiles](https://docs.spring.io/spring-boot/4.1.1/reference/packaging/container-images/dockerfiles.html)
  - [Docker: builds multi-etapa](https://docs.docker.com/build/building/multi-stage/)
  - [Docker: referencia de Dockerfile](https://docs.docker.com/reference/dockerfile/)
  - [Imagen `eclipse-temurin` (Java) en Docker Hub](https://hub.docker.com/_/eclipse-temurin)
  - [Imagen `nginx` en Docker Hub](https://hub.docker.com/_/nginx)
  - [nginx: `proxy_pass`](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)
  - [nginx: `try_files` (rutas de una SPA)](https://nginx.org/en/docs/http/ngx_http_core_module.html#try_files)
  - [Vite: build para producción (en español)](https://es.vite.dev/guide/build)
  - [Vite: desplegar un sitio estático (en español)](https://es.vite.dev/guide/static-deploy)
  - [Docker Compose](https://docs.docker.com/compose/)

### T22.4 · Datos y guion de la demo
- Datos de ejemplo cargados (proveedores, categorías, algunas solicitudes en distintos estados), guion de la presentación siguiendo el ejemplo de Laura, y slides.
- **Datos:** extender `DatosDePrueba` (T04.3), activado con otra variable (`DATOS_DEMO=true`), para que cualquiera levante la demo igual. Como mínimo:
  - 2 Encargados (para mostrar reasignar y el choque de "tomar"), 1 Admin, 2 o 3 solicitantes de sectores distintos, 1 Supervisor y 2 personas de Gerencia General (para mostrar el choque de "dos deciden a la vez").
  - Reglas de aprobación: `Compras chicas` (monto hasta 50.000 ARS: sin pasos, se aprueba sola), `Compras grandes` (más de 500.000 ARS: Supervisor y después Gerencia General) y la por defecto (Supervisor). Creadas con el service de T23.3.
  - 6 a 8 proveedores con categorías, uno inactivo.
  - Categorías conocidas y una NUEVA pendiente de confirmar.
  - Solicitudes en cada estado: EN_REVISION (algunas con la cadena a medio aprobar y otras aprobadas esperando la compra), LISTA, CERRADA y CANCELADA, con órdenes generadas y enviadas, un ítem rechazado por un aprobador, cotizaciones en pesos y en dólares y algún monto de envío, para que los reportes muestren algo.
  - Los datos se crean con los services (no con SQL a mano), así se publican los eventos y se llenan el historial y los reportes.
- **Guion** en `Docs/demo/guion.md`: el ejemplo de Laura paso a paso (enunciado, pasos 1 a 9, con los pasos de aprobación del recorrido de T21.3), quién hace cada paso (qué usuario y qué navegador), qué se muestra en pantalla y qué decir. Incluir los mails en Mailpit, el **cambio de regla en vivo** (de `Supervisor` a `Supervisor → GG` sin tocar código), el precio que cambia en tiempo real en la pantalla del aprobador y un caso borde en vivo (por ejemplo dos Encargados tomando el mismo ítem).
- **Plan B:** capturas o un video corto por si algo falla en vivo.
- **Slides:** problema, arquitectura (módulos y eventos), decisiones importantes, demo y qué quedó afuera.
- Ensayar la demo completa al menos una vez con `compose.demo.yaml` (T22.3) y medir el tiempo.
- **Documentación:**
  - [Spring Boot: `CommandLineRunner` (código al arrancar)](https://docs.spring.io/spring-boot/4.1.1/reference/features/spring-application.html#features.spring-application.command-line-runner)
  - [Spring Boot: `@ConditionalOnProperty`](https://docs.spring.io/spring-boot/4.1.1/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.condition-annotations.property-conditions)

### T22.5 · Escalado y manejo de fallas
- Revisar las secciones §8 y §9 del diseño y marcar qué se implementó (por ejemplo: reintento de eventos, idempotencia, versión) y qué queda como propuesta.
- Para cada punto de §8 (escalado) y §9 (fallas), agregar una línea con el estado:
  - **Implementado**, con dónde está: bloqueo optimista con `@Version` (T09.4 y, para dos aprobadores del mismo paso, T23.5), reintento de eventos al reiniciar (T12.1), listeners idempotentes (T12.2), idempotencia de creación (T08.7), n8n caído no pierde avisos (T15.2), recordatorios atrasados sin duplicar (T15.6).
  - **Solo propuesta**, con por qué no se hizo y qué haría falta: réplicas de lectura, base por módulo, S3, métricas con Prometheus y Grafana, ShedLock (README de planificación, "Alcance"), y el **tiempo real con varias réplicas** (el registro de conexiones SSE de T24.1 está en memoria; haría falta Postgres `LISTEN/NOTIFY` o Redis pub/sub).
- Si se probó alguna falla a mano (por ejemplo apagar n8n y ver que el aviso sale al volver), contarlo: vale más que la teoría para la defensa.
- **Documentación:**
  - [Spring Modulith: registro de publicación de eventos](https://docs.spring.io/spring-modulith/reference/events.html#publication-registry)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)

---

## T23 · Reglas y cadena de aprobación (módulo `aprobaciones`)

**Tarea nueva (requisito del 08/10).** Las decisiones 7, 8, 9, 12 y 13 del principio explican el modelo. Casos borde nuevos, que se suman a los 23 del diseño (§10):

24. **Un paso no tiene a nadie que pueda decidirlo** (se desactivó al último Supervisor, o el sector quedó vacío) → el ABM de reglas lo marca con `sinAprobadores` y el ítem queda esperando; el Admin lo ve y lo arregla. No se rechaza solo.
25. **El solicitante es el único aprobador posible** (por ejemplo, el único Supervisor hace un pedido) → no puede decidir sobre su propio pedido; queda esperando hasta que el Admin dé de alta otro aprobador o cambie la regla.
26. **El precio total cambia y la cadena crece o se achica** → se re-evalúa: los pasos aprobados se conservan, se agregan los que faltan y se omiten los pendientes que ya no hacen falta. Si el ítem estaba APROBADA y aparece un paso nuevo, vuelve a EN_CURSO.
27. **El Admin edita una regla con ítems en curso** → los ítems conservan la cadena que ya tenían (es una copia). La regla nueva se aplica a los ítems nuevos y a los que se re-evalúen por un cambio de precio.
28. **Dos personas del mismo paso deciden a la vez** (dos de Gerencia General) → `@Version`: la primera gana y la segunda recibe 409 "Otra persona ya decidió este paso".

### T23.1 · Módulo `aprobaciones` y sus tablas
- Es el noveno módulo. **Debió hacerse junto con T02** (esqueleto modular y migraciones base, ya mergeadas); por eso nace atrasada.
- `aprobaciones/package-info.java` con `@ApplicationModule(displayName = "Aprobaciones")` y las carpetas `dominio/`, `repositorio/`, `servicio/` y `web/`.
- **Migración** `V<fecha>_1__aprobaciones_tablas.sql`. La `V1` no se edita: el schema se crea acá con `CREATE SCHEMA IF NOT EXISTS aprobaciones;`.

  ```sql
  CREATE TABLE aprobaciones.regla (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL UNIQUE,
    prioridad       INT NOT NULL,                  -- menor número = se evalúa primero
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    por_defecto     BOOLEAN NOT NULL DEFAULT FALSE,
    creada_por      BIGINT,                        -- null = la creó la migración
    creada_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    modificada_por  BIGINT,
    modificada_en   TIMESTAMPTZ,
    version         INT NOT NULL DEFAULT 0
  );
  CREATE UNIQUE INDEX ux_regla_por_defecto ON aprobaciones.regla (por_defecto) WHERE por_defecto;

  CREATE TABLE aprobaciones.regla_condicion (
    id              BIGSERIAL PRIMARY KEY,
    regla_id        BIGINT NOT NULL REFERENCES aprobaciones.regla(id),
    tipo            VARCHAR(20) NOT NULL,          -- hoy solo 'MONTO'
    operador        VARCHAR(15) NOT NULL CHECK (operador IN ('MAYOR','MAYOR_IGUAL','MENOR','MENOR_IGUAL')),
    valor_numerico  NUMERIC(16,2),                 -- MONTO: el umbral
    moneda          CHAR(3) CHECK (moneda IN ('ARS','USD')),
    valor_id        BIGINT                         -- reservado para condiciones futuras (categoría, sector)
  );

  CREATE TABLE aprobaciones.regla_paso (
    id              BIGSERIAL PRIMARY KEY,
    regla_id        BIGINT NOT NULL REFERENCES aprobaciones.regla(id),
    orden           INT NOT NULL CHECK (orden >= 1),   -- mismo número = pasos en paralelo
    tipo_aprobador  VARCHAR(6) NOT NULL CHECK (tipo_aprobador IN ('ROL','SECTOR')),
    rol             VARCHAR(20),
    sector_id       BIGINT,                        -- ref. auth.sector
    CHECK ((tipo_aprobador = 'ROL' AND rol IS NOT NULL AND sector_id IS NULL)
        OR (tipo_aprobador = 'SECTOR' AND sector_id IS NOT NULL AND rol IS NULL))
  );

  CREATE TABLE aprobaciones.aprobacion_item (
    item_id                 BIGINT PRIMARY KEY,    -- ref. solicitudes.item
    solicitud_id            BIGINT NOT NULL,
    solicitante_id          BIGINT NOT NULL,
    sector_id               BIGINT NOT NULL,
    categoria_id            BIGINT NOT NULL,
    tipo                    VARCHAR(10) NOT NULL,
    detalle                 TEXT,
    cantidad                NUMERIC(12,2) NOT NULL,
    urgencia                VARCHAR(5) NOT NULL,
    precio_estimado         NUMERIC(16,2) NOT NULL,
    moneda_estimada         CHAR(3) NOT NULL,
    precio_total            NUMERIC(16,2),
    moneda_total            CHAR(3),
    proveedor_razon_social  VARCHAR(200),
    regla_id                BIGINT,                -- copia, sin FK: la regla puede cambiar después
    regla_nombre            VARCHAR(120),
    estado                  VARCHAR(10) NOT NULL DEFAULT 'EN_CURSO'
                            CHECK (estado IN ('EN_CURSO','APROBADA','RECHAZADA','CANCELADA')),
    cerrada                 BOOLEAN NOT NULL DEFAULT FALSE,  -- true al registrarse la compra: ya no se re-evalúa
    solicitado_en           TIMESTAMPTZ NOT NULL,
    version                 INT NOT NULL DEFAULT 0
  );

  CREATE TABLE aprobaciones.paso_item (
    id                BIGSERIAL PRIMARY KEY,
    item_id           BIGINT NOT NULL REFERENCES aprobaciones.aprobacion_item(item_id),
    orden             INT NOT NULL,
    tipo_aprobador    VARCHAR(6) NOT NULL CHECK (tipo_aprobador IN ('ROL','SECTOR')),
    rol               VARCHAR(20),
    sector_id         BIGINT,
    nombre_aprobador  VARCHAR(120) NOT NULL,       -- copia para mostrar: 'Supervisor', 'Gerencia General'
    estado            VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO','OMITIDO','CANCELADO')),
    decidido_por      BIGINT,
    decidido_en       TIMESTAMPTZ,
    motivo            TEXT,
    version           INT NOT NULL DEFAULT 0,
    CHECK ((tipo_aprobador = 'ROL' AND rol IS NOT NULL AND sector_id IS NULL)
        OR (tipo_aprobador = 'SECTOR' AND sector_id IS NOT NULL AND rol IS NULL)),
    CHECK (estado <> 'RECHAZADO' OR motivo IS NOT NULL)
  );
  CREATE INDEX ix_paso_item ON aprobaciones.paso_item (item_id, orden);
  -- "Mis pendientes": los pasos sin resolver de mi rol o de mi sector
  CREATE INDEX ix_paso_pendiente ON aprobaciones.paso_item (tipo_aprobador, rol, sector_id) WHERE estado = 'PENDIENTE';

  CREATE TABLE aprobaciones.evento_procesado (
    evento_id     UUID PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
  );

  -- La regla por defecto va en la migración y no en los datos de prueba: sin ella ningún ítem tendría cadena.
  INSERT INTO aprobaciones.regla (nombre, prioridad, por_defecto) VALUES ('Por defecto', 1000, TRUE);
  INSERT INTO aprobaciones.regla_paso (regla_id, orden, tipo_aprobador, rol)
    SELECT id, 1, 'ROL', 'SUPERVISOR' FROM aprobaciones.regla WHERE por_defecto;
  ```

- **Por qué `aprobacion_item` copia los datos del ítem** (detalle, cantidad, precios, urgencia): es el modelo de lectura con el que el aprobador ve el ítem y con el que se re-evalúan las reglas, sin consultar Solicitudes ni Compras (misma idea que `gestion_item`).
- `ModularityTests` tiene que seguir pasando. Aprobaciones depende de `auth` (por `AuthApi`) y de `compartido`; con Compras y Solicitudes habla **solo por eventos**. Si aparece un ciclo, se está llamando a la API de otro módulo donde va un evento.
- Actualizar la lista de módulos en `CLAUDE.md` y `back/CLAUDE.md` (son locales), en `README.md` de la planificación (ya está) y en T22.1.
- Hibernate con `validate` tiene que arrancar sin errores.
- **Documentación:**
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
  - [Spring Modulith: verificar la estructura](https://docs.spring.io/spring-modulith/reference/verification.html)
  - [Flyway: migraciones versionadas](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html)
  - [PostgreSQL: restricciones `CHECK`](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-CHECK-CONSTRAINTS)
  - [PostgreSQL: índices parciales](https://www.postgresql.org/docs/current/indexes-partial.html)

### T23.2 · Dominio, repositories y motor de reglas
- **Dominio** en `aprobaciones/dominio/`:
  - Enums: `TipoCondicion` (MONTO), `OperadorCondicion` (MAYOR, MAYOR_IGUAL, MENOR, MENOR_IGUAL), `TipoAprobador` (ROL, SECTOR), `EstadoPaso` (PENDIENTE, APROBADO, RECHAZADO, OMITIDO, CANCELADO) y `EstadoCadena` (EN_CURSO, APROBADA, RECHAZADA, CANCELADA).
  - Entidades: `Regla` (con `condiciones` y `pasos`, `@OneToMany` con cascade), `ReglaCondicion`, `ReglaPaso`, `AprobacionItem` (con `pasos`) y `PasoItem`. `Regla`, `AprobacionItem` y `PasoItem` llevan `@Version`.
  - `AprobacionItem.pasosActivos()`: los pasos PENDIENTES cuyo `orden` es el menor entre los pendientes (decisión 7: mismo orden = en paralelo). Sin pasos pendientes, lista vacía.
  - `PasoItem.puedeDecidir(rol, sectorId)`: true si el paso es de rol y coincide, o de sector y coincide.
- **Motor** en `aprobaciones/servicio/`, sin acceso a base de datos más que para leer las reglas:
  - Records `Monto(importe, moneda)` y `ContextoEvaluacion(monto, sectorId, categoriaId, solicitanteId, urgencia)`. Se pasa **todo** el contexto aunque hoy solo se use el monto: así una condición nueva (categoría, sector, urgencia) no obliga a cambiar la firma.
  - Interfaz `EvaluadorCondicion { TipoCondicion tipo(); boolean cumple(ReglaCondicion condicion, ContextoEvaluacion contexto); }` (patrón Strategy). Hoy hay una sola implementación, `EvaluadorMonto`. Para sumar un tipo de condición hay que agregar el valor al enum, una clase `EvaluadorXxx` y el campo en el ABM (T23.3 y T25.2); el motor no cambia.
  - `EvaluadorMonto`: la condición se cumple solo si la **moneda del monto es la de la condición** (sin conversión, decisión 8) y el importe cumple el operador (comparar con `compareTo` de `BigDecimal`, nunca con `equals`).
  - `MotorDeReglas.evaluar(ContextoEvaluacion)` → `ResultadoEvaluacion(regla, List<PasoRequerido>)`, donde `PasoRequerido(orden, tipoAprobador, rol, sectorId, nombre)`. Recorre las reglas **activas por `prioridad` ascendente** y devuelve la primera cuyas condiciones se cumplan **todas** (una regla sin condiciones, como la por defecto, siempre se cumple). Una regla sin pasos devuelve una lista vacía: **aprobación automática**. El nombre del paso sale de un mapa de `Rol` a texto ("Supervisor") o de `AuthApi.nombreSector`.
  - Si no hay ninguna regla que cumpla (no debería pasar: la por defecto no se puede desactivar), lanzar `IllegalStateException` con un mensaje claro y no inventar una cadena.
- **Repositories:** `ReglaRepository` con `@EntityGraph(attributePaths = {"condiciones", "pasos"}) List<Regla> findByActivaTrueOrderByPrioridadAsc()` (una sola consulta, sin N+1), `AprobacionItemRepository` y `PasoItemRepository`.
- **Tests unitarios del motor** (sin Spring ni base de datos), uno por fila:

  | Monto | Reglas | Resultado esperado |
  |---|---|---|
  | 600.000 ARS | "Grandes": MONTO MAYOR 500.000 ARS → Supervisor (1), Gerencia General (2); y la por defecto | cadena de 2 pasos |
  | 500.000 ARS | la misma | no cumple (`MAYOR` es estricto) → la por defecto, 1 paso |
  | 30.000 ARS | "Chicas": MONTO MENOR_IGUAL 50.000 ARS → sin pasos | lista vacía (automática) |
  | 1.000 USD | reglas en ARS | ninguna condición cumple (otra moneda) → la por defecto |
  | 600.000 ARS | dos reglas que cumplen, prioridades 5 y 10 | gana la de prioridad 5 |
  | 600.000 ARS | la regla que cumple está inactiva | se ignora → la por defecto |
  | cualquiera | dos pasos con el mismo `orden` | `pasosActivos()` devuelve los dos juntos |

- Test `@DataJpaTest` (con `@Import(TestcontainersConfiguration.class)`): después de migrar existe la regla por defecto con 1 paso (SUPERVISOR).
- **Documentación:**
  - [Patrón Strategy (en español)](https://refactoring.guru/es/design-patterns/strategy)
  - [Java: `BigDecimal` (`compareTo`)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html)
  - [Spring Data JPA: `@EntityGraph`](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph)
  - [Hibernate 7: relaciones entre entidades](https://docs.hibernate.org/orm/7.4/introduction/html_single/#associations)
  - [Spring Boot: tests de repositories con `@DataJpaTest`](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa)

### T23.3 · ABM de reglas (Admin)
- Endpoints bajo `/api/v1/admin/reglas`, solo `ADMIN` (T05.1):

  | Método | Endpoint | Qué hace |
  |---|---|---|
  | GET | `/api/v1/admin/reglas` | Todas las reglas por `prioridad`, con condiciones y pasos |
  | GET | `/api/v1/admin/reglas/{id}` | Una regla |
  | POST | `/api/v1/admin/reglas` | Crea una regla → 201 |
  | PUT | `/api/v1/admin/reglas/{id}` | Reemplaza nombre, prioridad, condiciones y pasos (con `version`) |
  | POST | `/api/v1/admin/reglas/{id}/activar` y `/desactivar` | Cambia `activa` |

  No hay `DELETE`: se desactiva (los ítems en curso guardan el id y el nombre de la regla que se les aplicó).
- Ejemplo de cuerpo (la regla del enunciado nuevo, "compras de más de tanta plata: consultar con tales sectores"):

  ```json
  { "nombre": "Compras grandes", "prioridad": 10, "activa": true,
    "condiciones": [ { "tipo": "MONTO", "operador": "MAYOR", "valor": 500000, "moneda": "ARS" } ],
    "pasos": [ { "orden": 1, "tipoAprobador": "ROL", "rol": "SUPERVISOR" },
               { "orden": 2, "tipoAprobador": "SECTOR", "sectorId": 5 } ] }
  ```
- **Validaciones** (si fallan, 422 con el código):
  - Nombre único sin distinguir mayúsculas (409 si se repite) y obligatorio.
  - Condición `MONTO`: `valor` > 0, `moneda` obligatoria y `operador` válido. `tipo` desconocido → 400.
  - Pasos: `orden` desde 1, sin saltos (1, 2, 3...), sin repetir el mismo aprobador dentro de una regla. Un paso de rol no puede ser SOLICITANTE (`ROL_NO_APROBADOR`). Un paso de sector exige que el sector exista (`AuthApi.existeSector`).
  - **Cada paso tiene que tener a alguien que lo pueda decidir** (`AuthApi.hayUsuariosActivosConRol` / `hayUsuariosActivosEnSector`); si no, 422 `SIN_APROBADORES`. Solo se valida al guardar una regla activa. Después puede dejar de cumplirse (caso borde 24): por eso la lista devuelve `sinAprobadores: true/false` en cada paso, calculado al leer.
  - **Regla por defecto** (`por_defecto = true`): no se puede desactivar ni dejar sin pasos (`REGLA_POR_DEFECTO_SIN_PASOS`) ni ponerle condiciones. Sí se pueden editar sus pasos. Es el camino de "hoy el flujo es Supervisor; mañana Supervisor → GG": el Admin edita los pasos de la regla por defecto, sin programar.
- **Alcance de un cambio** (caso borde 27): vale para los ítems nuevos y para los que se re-evalúen. Los ítems en curso conservan su cadena.
- Publica `ReglaModificada` (Auditoría la registra, regla 11).
- **Tests:** crear la regla del ejemplo → 201; paso de sector inexistente → 422; paso sin usuarios activos → 422 `SIN_APROBADORES`; desactivar la por defecto → 422; editar los pasos de la por defecto a `Supervisor → Gerencia General` → 200; un rol SOLICITANTE → 422; permiso: un Encargado recibe 403.
- **Documentación:**
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
  - [Hibernate Validator: validaciones sobre toda la clase](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-class-level-constraints)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [Spring Security: `@PreAuthorize`](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-preauthorize)

### T23.4 · Iniciar y mantener la cadena de cada ítem
- En `aprobaciones/servicio/`, un componente con listeners `@ApplicationModuleListener` (idempotentes: la clave natural es `item_id`; los que no la tienen usan `evento_procesado`, T12.2):
  1. **`SolicitudCreada`:** por cada ítem crea una `AprobacionItem` copiando los datos del evento. Evalúa el motor con `Monto(precioEstimado, monedaEstimada)` y crea los `PasoItem` (con el nombre del aprobador copiado). Si el resultado no tiene pasos → estado APROBADA y se publica `AprobacionCompletada(automatica = true)`. Si tiene pasos, publica `PasoAprobacionActivado` por cada paso activo (los del menor `orden`).
  2. **`PrecioTotalActualizado`:** guarda `precioTotal`, `monedaTotal` y el proveedor. Si la aprobación está `cerrada` o su estado es RECHAZADA o CANCELADA, termina ahí. Si no, **re-evalúa** con el monto nuevo (el total si existe; si no, el estimado). Algoritmo:
     - Calcular la cadena requerida con el motor.
     - Un paso requerido que ya existe en el ítem (mismo tipo y mismo rol o sector) se **conserva**, con su estado, y toma el `orden` nuevo. Un paso requerido que no existe se **agrega** PENDIENTE.
     - Un paso que ya estaba APROBADO y ya no se requiere **se conserva** (queda en el historial). Un paso PENDIENTE que ya no se requiere pasa a OMITIDO.
     - Actualizar `regla_id` y `regla_nombre`.
     - Si el estado era APROBADA y quedó algún paso PENDIENTE → vuelve a EN_CURSO y se publica `AprobacionReabierta(PRECIO_CAMBIO)`. Si era EN_CURSO y ya no queda ninguno PENDIENTE → APROBADA y `AprobacionCompletada`.
     - Publicar `PasoAprobacionActivado` por cada paso que pasó a ser activo.
  3. **`ItemEditado`:** actualiza la copia y **reinicia** la cadena: todos los pasos vuelven a PENDIENTE (sin decisión) y se re-evalúa con el monto actual. Si estaba APROBADA, publica `AprobacionReabierta(EDICION)`; después activa los pasos como en el punto 1. Motivo: el aprobador aprobó algo distinto de lo que ahora está pedido.
  4. **`ItemCancelado` y `SolicitudCancelada`:** los pasos PENDIENTES pasan a CANCELADO y la aprobación a CANCELADA.
  5. **`ItemResuelto`:** si es RECHAZADO y la aprobación seguía EN_CURSO (rechazó el Encargado), los pasos PENDIENTES pasan a CANCELADO y la aprobación a CANCELADA. Si es COMPRADO, `cerrada = true` (ya no se re-evalúa). Si la aprobación ya estaba RECHAZADA (rechazó un aprobador), no hace nada.
- Un paso se publica como activo **una sola vez**: al crearse la cadena, al completarse el orden anterior (T23.5) o al aparecer en una re-evaluación. Así no se mandan mails repetidos (T15.2).
- **Tests unitarios** (sin base de datos): re-evaluación con cada combinación (se agrega un paso; se omite uno pendiente; un aprobado que ya no hace falta se conserva; el estado pasa de APROBADA a EN_CURSO y al revés); el reinicio por edición.
- **Tests con `Scenario`:** los de la tabla de T12.3 (`aprobaciones`); publicar `SolicitudCreada` por 600.000 ARS con la regla de T23.2 → 2 pasos y un solo `PasoAprobacionActivado` (el del Supervisor); publicar dos veces el mismo evento no duplica nada.
- **Documentación:**
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Modulith: tests con `Scenario`](https://docs.spring.io/spring-modulith/reference/testing.html#scenarios)
  - [Spring Modulith: registro de publicación de eventos](https://docs.spring.io/spring-modulith/reference/events.html#publication-registry)
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)

### T23.5 · Aprobar o rechazar un paso
- `POST /api/v1/aprobaciones/pasos/{pasoId}/aprobar` `{ version }` → 200.
- `POST /api/v1/aprobaciones/pasos/{pasoId}/rechazar` `{ motivo, version }` → 200. `motivo` obligatorio y no vacío.
- Validaciones, en orden:
  1. El paso existe (404) y su aprobación está EN_CURSO (si no, 422 `APROBACION_CERRADA`).
  2. El paso está PENDIENTE y **activo**, es decir, de los de menor `orden` entre los pendientes (si no, 422 `PASO_NO_ACTIVO`, "Todavía no le toca a {aprobadorNombre}").
  3. El usuario puede decidirlo: `PasoItem.puedeDecidir(UsuarioActual.rol(), UsuarioActual.sectorId())` (si no, 403 `SIN_PERMISO`).
  4. **Nadie decide sobre su propio pedido:** `UsuarioActual.id()` distinto de `solicitanteId` (si no, 403 con código `PEDIDO_PROPIO`, caso borde 25).
  5. La `version` del paso coincide (409 si no, caso borde 28).
- **Aprobar:** el paso pasa a APROBADO con `decididoPor` y `decididoEn`; se publica `PasoDecidido`. Si quedan pasos PENDIENTES, se activan los del menor orden restante (`PasoAprobacionActivado`); si no queda ninguno, la aprobación pasa a APROBADA y se publica `AprobacionCompletada(automatica = false)`.
- **Rechazar:** el paso pasa a RECHAZADO con el `motivo`; los demás PENDIENTES pasan a CANCELADO; la aprobación pasa a RECHAZADA. Se publican `PasoDecidido` y `AprobacionRechazada`. Compras lo escucha y rechaza el ítem (T10.4). **Rechaza ese ítem, no la solicitud ni los demás ítems** (decisión 7).
- Todo en una transacción. `AprobacionItem` y `PasoItem` llevan `@Version`: el `UPDATE ... WHERE version = ?` frena a dos personas del mismo paso.
- **Tests:** aprobar el último paso completa la aprobación y publica `AprobacionCompletada`; aprobar el primero activa el segundo y publica un solo `PasoAprobacionActivado`; rechazar cancela los pasos pendientes y publica `AprobacionRechazada`; el solicitante intenta aprobar su pedido → 403; un usuario de otro sector → 403; el GG intenta decidir antes que el Supervisor → 422; **test de concurrencia** (como T09.5): dos hilos aprueban el mismo paso, uno gana y el otro recibe el error de versión.
- **Documentación:**
  - [Spring Data JPA: bloqueo optimista (`@Version`)](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
  - [Spring: transacciones con `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  - [Spring Modulith: eventos entre módulos](https://docs.spring.io/spring-modulith/reference/events.html)
  - [Java: `CountDownLatch`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/CountDownLatch.html)
  - [MDN: 409 Conflict](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/409)
  - [MDN: 422 Unprocessable Content](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/422)

### T23.6 · Pendientes y detalle para los aprobadores
- `GET /api/v1/aprobaciones?vista=para-decidir|en-curso|decididas|todas&page=` → `Pagina` de `{ itemId, solicitudId, solicitanteNombre, sector, categoriaNombre, detalle, cantidad, urgencia, precioEstimado, monedaEstimada, precioTotal, monedaTotal, proveedor, reglaNombre, estado, pasoId, aprobadorNombre, puedoDecidir, version }`.
  - `para-decidir`: los pasos activos que yo puedo decidir (mi rol o mi sector) y que no son de un pedido mío. Ordenados por urgencia y antigüedad.
  - `en-curso`: ítems EN_CURSO en los que tengo un paso futuro; solo lectura, para saber qué se viene.
  - `decididas`: los pasos que decidí yo.
  - `todas`: solo `ADMIN`, para ver los ítems trabados (caso borde 24).
- `GET /api/v1/aprobaciones/items/{itemId}` → el detalle: los datos copiados del ítem, el estado, la regla aplicada y los pasos `[{ id, orden, aprobadorNombre, estado, decididoPorNombre, decididoEn, motivo, activo, puedoDecidir, version }]`. Lo puede ver: el dueño del pedido, un Encargado, el Admin y cualquier usuario con un paso en ese ítem; los demás → 403. El front lo usa en el detalle del aprobador, en `DetalleSolicitud` y en `GestionItem`.
- `GET /api/v1/aprobaciones/resumen` → `{ esAprobador, paraDecidir }`. `esAprobador` es true si el rol o el sector del usuario aparece en algún paso de una regla activa. El front lo usa para mostrar o esconder el menú "Aprobaciones" y el contador (T17.1).
- Los nombres (solicitante, categoría, quien decidió) salen de `AuthApi` y `CatalogoApi` al armar la respuesta.
- **Tests:** el Supervisor ve el ítem en `para-decidir` y el de Gerencia General no (todavía no le toca); después de aprobar el Supervisor, al revés; el dueño del pedido ve el detalle y otro Solicitante recibe 403; `resumen` da `esAprobador: false` para un usuario común.
- **Documentación:**
  - [Spring Data: paginación y ordenamiento (`Pageable`, `Sort`)](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters)
  - [Spring Data JPA: consultas con `@Query` (JPQL y nativas)](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query)
  - [Spring Modulith: fundamentos (módulos y su API)](https://docs.spring.io/spring-modulith/reference/fundamentals.html)

### T23.7 · Simulador de reglas (Admin)
- `POST /api/v1/admin/reglas/simular` `{ monto, moneda, sectorId, categoriaId }` (los dos últimos opcionales) → `{ regla: { id, nombre }, automatica, pasos: [{ orden, aprobadorNombre }] }`. Llama al `MotorDeReglas` sin guardar nada.
- Sirve para que el Admin compruebe una regla antes de confiar en ella ("¿qué pasa con una compra de 600.000 pesos?"). La usa la pantalla de T25.2.
- Test: con las reglas de T23.2, 600.000 ARS devuelve 2 pasos y 30.000 ARS devuelve `automatica: true`.
- **Documentación:**
  - [Spring MVC: `@RequestBody` y validación](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)

### T23.8 · (Opcional) Recordatorios a los aprobadores
- Hoy los recordatorios (T15.5 y T15.6) son solo para el Encargado. Un paso que nadie decide frena la compra, así que conviene recordárselo al aprobador con el mismo ritmo (Alta cada 3 días; Media y Baja cada 3 semanas).
- `AprobacionesApi.recordatoriosPendientes(Instant ahora)` y `registrarRecordatorio(pasoId, cuando)`, un endpoint más en `integraciones/web/` y un segundo camino en el workflow de T15.6. Agrega la columna `ultimo_recordatorio_en` a `paso_item`.
- Solo si sobra tiempo: nadie lo pidió explícitamente.
- **Documentación:**
  - [Java: `Duration`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/Duration.html)
  - [n8n: nodo Schedule Trigger](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.scheduletrigger)

---

## T24 · Precio en tiempo real (SSE)

### T24.1 · Señales en tiempo real (backend)
- **Qué se resuelve:** que cuando el Encargado carga el precio total, el Supervisor o el de Gerencia General que tiene la pantalla abierta lo vea cambiar sin recargar y pueda decidir con el valor real (decisión 11).
- **Cómo:** Server-Sent Events. Una conexión HTTP de larga duración, del servidor al navegador. Se eligió SSE y no WebSocket porque solo hace falta que el servidor avise, y SSE usa HTTP común (pasa por el proxy de Vite y por la cadena de seguridad sin nada especial).
- `GET /api/v1/tiempo-real` (`produces = text/event-stream`), con el JWT de siempre, devuelve un `SseEmitter`.
  - Cada conexión se guarda en `integraciones/servicio/CorredorDeSenales`, un registro en memoria `usuarioId → conexiones`. Al terminar, vencer o fallar, se saca del registro.
  - **Latido:** un comentario SSE cada 25 segundos (`:ping`) para que ningún proxy corte la conexión por inactividad.
  - Timeout largo (por ejemplo 30 minutos); el front reconecta solo (T24.2).
- **Qué se manda:** solo **señales con ids**, nada de datos de negocio: `{ "tipo": "PRECIO_ACTUALIZADO", "itemId": 4021, "solicitudId": 1587 }`. El front, al recibirla, vuelve a pedir el detalle por REST, que es donde se controlan los permisos. Así no hace falta repetir ninguna regla de autorización en el stream y no se filtra información por esta vía.
- **Listeners** en `integraciones/servicio/` (`@ApplicationModuleListener`) que convierten eventos en señales: `PrecioTotalActualizado`, `CompraActualizada` (`PRECIO_ACTUALIZADO`), `PasoAprobacionActivado`, `PasoDecidido`, `AprobacionCompletada`, `AprobacionReabierta`, `AprobacionRechazada` (`APROBACION_CAMBIO`), `ItemAsignado` e `ItemResuelto` (`ITEM_CAMBIO`). Se mandan a todos los conectados; ver una señal con un id no da acceso a nada.
- **Un fallo al mandar a un cliente no puede hacer fallar el listener:** si lanzara una excepción, Modulith dejaría el evento sin completar y lo reprocesaría al reiniciar (T12.1) para avisarle a alguien que ya se desconectó. Capturar el error de cada conexión, sacarla del registro y seguir.
- Seguridad: la ruta queda en la cadena JWT normal (T04.4). Un navegador no puede mandar el header `Authorization` con `EventSource`; por eso el front usa `fetch` (T24.2) y el backend no necesita aceptar el token por la URL.
- **Límite conocido:** el registro está en memoria, así que funciona con **una** instancia del backend. Con varias réplicas, una señal solo llegaría a los clientes conectados a la instancia que la generó. La solución sería Postgres `LISTEN/NOTIFY` o Redis pub/sub; queda como propuesta en T22.5.
- **Tests:** unitario del `CorredorDeSenales` con una conexión simulada (publicar una señal llega; una conexión que falla se descarta y no afecta a las demás); con `Scenario`, publicar `PrecioTotalActualizado` produce una señal `PRECIO_ACTUALIZADO` con el `itemId`.
- **Documentación:**
  - [Spring MVC: respuestas asíncronas y SSE (`SseEmitter`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html#mvc-ann-async-sse)
  - [MDN: usar server-sent events (en español)](https://developer.mozilla.org/es/docs/Web/API/Server-sent_events/Using_server-sent_events)
  - [Spring Modulith: `@ApplicationModuleListener`](https://docs.spring.io/spring-modulith/reference/events.html#aml)
  - [Spring Security: JWT en un resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

### T24.2 · Tiempo real en el front
- `npm install @microsoft/fetch-event-source` (permite mandar headers, a diferencia de `EventSource`).
- `hooks/useTiempoReal.js` (o `api/tiempoReal.js`): abre `GET /api/v1/tiempo-real` con el token, y llama al callback que recibe cada componente con la señal `{ tipo, itemId, solicitudId }`. Una sola conexión para toda la app (en un contexto o un módulo singleton), no una por pantalla.
  - **Reconexión** automática con espera creciente (1 s, 2 s, 4 s... hasta 30 s). Al reconectar, avisar a los componentes para que recarguen, porque pudieron perderse señales.
  - **Plan B si el stream no anda** (proxy corporativo, por ejemplo): si falla 3 veces seguidas, pasar a consultar cada 15 segundos. La pantalla sigue funcionando, solo que más lenta.
  - Cerrar la conexión al cerrar sesión, y no mantenerla con la pestaña oculta (`openWhenHidden: false`).
- Usarlo en: `DetalleSolicitud` (precio y cadena del ítem), `GestionItem` y `Bandeja` (cambios de aprobación), `Aprobaciones` y `DetalleAprobacion` (T25.1). Cada pantalla recarga su detalle solo si la señal trae un `itemId` que está mostrando.
- Mostrar un resaltado breve (un par de segundos) en el campo que cambió, y un aviso discreto: "El precio total cambió". Esto importa en la pantalla del aprobador: tiene que notar que el valor sobre el que va a decidir cambió.
- **Documentación:**
  - [@microsoft/fetch-event-source](https://github.com/Azure/fetch-event-source)
  - [React: hooks personalizados (en español)](https://es.react.dev/learn/reusing-logic-with-custom-hooks)
  - [React: sincronizar con efectos (limpieza al desmontar) (en español)](https://es.react.dev/learn/synchronizing-with-effects)
  - [MDN: usar server-sent events (en español)](https://developer.mozilla.org/es/docs/Web/API/Server-sent_events/Using_server-sent_events)
  - [MDN: Page Visibility API](https://developer.mozilla.org/es/docs/Web/API/Page_Visibility_API)

---

## T25 · Pantallas de aprobación y reglas

### T25.1 · Pantallas del aprobador
- `components/PasosAprobacion.jsx`: recibe los pasos de `GET /aprobaciones/items/{itemId}` y los dibuja en orden como una línea (por ejemplo `Supervisor ✔ → Gerencia General ⏳`). Estado de cada paso: aprobado (✔, con quién y cuándo), esperando su turno (○), activo (⏳), rechazado (✖, con el motivo) u omitido (–). Los pasos con el mismo `orden` se muestran juntos, como "en paralelo". Se reutiliza en `DetalleSolicitud` (T18.3) y en `GestionItem` (T19.2).
- `pages/aprobador/Aprobaciones.jsx`: tres pestañas con `TablaPaginada` (`GET /aprobaciones?vista=`): **Para decidir**, **En curso** (solo lectura) y **Decididas**. Columnas: solicitante, sector, qué se pide, cantidad, precio (el total si existe; si no, el estimado, indicando cuál es), urgencia y el paso que me toca. Guardar la pestaña en la URL. Si no hay nada: "No tenés compras para aprobar".
- `pages/aprobador/DetalleAprobacion.jsx` con `GET /aprobaciones/items/{itemId}`:
  - Datos del ítem y del pedido (quién, sector, urgencia, qué es, cantidad) y los adjuntos del ítem (T20.3).
  - **Precio estimado y precio total uno al lado del otro**, con el total resaltado. Se actualiza solo con T24.2: cuando el Encargado carga el precio, el aprobador lo ve cambiar y decide con ese valor. Si el precio cambia mientras la pantalla está abierta, mostrar el aviso "El precio cambió" y la diferencia.
  - El proveedor elegido (si lo hay) y la regla que se aplicó ("Compras grandes").
  - `PasosAprobacion` con la cadena completa.
  - Botones **Aprobar** (con confirmación) y **Rechazar** (ventana con el motivo obligatorio). Si `puedoDecidir` es false, deshabilitados con el motivo: "Todavía le toca a Supervisor" o "Es tu propio pedido".
  - Errores: 409 "Otra persona ya decidió este paso" (recargar); 422 `PASO_NO_ACTIVO` y `APROBACION_CERRADA` (mostrar el mensaje y recargar).
- Menú (T17.1): la opción "Aprobaciones" se muestra a los SUPERVISOR siempre y a los demás solo si `GET /aprobaciones/resumen` dice `esAprobador`, con el contador `paraDecidir`.
- Funciones en `api/aprobaciones.js`.
- **Documentación:**
  - [React: renderizado condicional (en español)](https://es.react.dev/learn/conditional-rendering)
  - [React: renderizar listas (en español)](https://es.react.dev/learn/rendering-lists)
  - [React Router: parámetros y query de la URL (`useParams`, `useSearchParams`)](https://reactrouter.com/start/declarative/url-values)
  - [MDN: `<dialog>` (ventanas modales) (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/dialog)
  - [MDN: `Intl.NumberFormat` (formato de moneda) (en español)](https://developer.mozilla.org/es/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat)

### T25.2 · Administración de reglas
- `pages/admin/Reglas.jsx` con `GET /admin/reglas`:
  - Lista ordenada por prioridad: nombre, resumen de condiciones ("Monto mayor a 500.000 ARS"), cadena ("Supervisor → Gerencia General" o "Aprobación automática"), activa o no. La regla por defecto se marca y no se puede desactivar. Un paso con `sinAprobadores` se muestra con una advertencia (caso borde 24).
  - **Formulario** de alta y edición: nombre, prioridad, activa; **condiciones** (hoy solo "Monto": operador, valor y moneda), con el botón "Agregar condición" armado para que sumar otro tipo sea agregar una opción; **pasos** (lista ordenable): cada uno con "Rol" o "Sector" y su valor (los roles aprobadores y `GET /admin/sectores`) y su número de orden, con una ayuda: "Pasos con el mismo número se aprueban en paralelo; el siguiente número se activa cuando todos aprobaron". Sin pasos, se muestra "Esta regla aprueba sola".
  - **Probar la regla** (T23.7): campos de monto y moneda y el resultado ("Con 600.000 ARS: Supervisor → Gerencia General").
  - Errores del backend por campo (409 nombre repetido; 422 `SIN_APROBADORES`, `ROL_NO_APROBADOR`, `REGLA_POR_DEFECTO_SIN_PASOS`).
  - Aviso al guardar: "Los pedidos en curso conservan su cadena" (caso borde 27).
- Funciones en `api/reglas.js`.
- **Documentación:**
  - [React: `<form>` (en español)](https://es.react.dev/reference/react-dom/components/form)
  - [React: actualizar arrays en el estado (en español)](https://es.react.dev/learn/updating-arrays-in-state)
  - [React: `<select>` (en español)](https://es.react.dev/reference/react-dom/components/select)
  - [MDN: `<dialog>` (ventanas modales) (en español)](https://developer.mozilla.org/es/docs/Web/HTML/Reference/Elements/dialog)
