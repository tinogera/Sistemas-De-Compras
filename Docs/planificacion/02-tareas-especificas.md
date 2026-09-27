# Nivel 2 — Tareas específicas

Cada tarea general de `01-tareas-generales.md` dividida en subtareas. Cada subtarea dice qué hay que crear, qué campos, métodos, endpoints y reglas tiene que cumplir, y dónde está la documentación. El área, la dificultad y la fecha de cada una están en `03-area-dificultad-fechas.md`.

- Las rutas de backend son relativas a `back/src/main/java/com/Sistem/Solicitude_Compra/` salvo que se diga otra cosa. Las de frontend, a `frontend/src/`.
- Las secciones con § son de `Docs/diseno-del-sistema.md`.
- La estructura de carpetas de cada módulo y el nombre de las migraciones siguen las convenciones de `README.md`.

## Decisiones que cambian el diseño original

Surgieron al bajar el diseño a tareas. Hay que reflejarlas en el documento de diseño al cierre (T22.1).

1. **Los eventos y los tipos compartidos van en un módulo `compartido`, no en cada módulo.** Solicitudes escucha eventos de Compras y Compras escucha eventos de Solicitudes. Si cada evento vive en su módulo, eso es una dependencia circular y `ModularityTests` falla (probado el 27/09: `Cycle detected: Slice compras -> ...`). Con los eventos en `compartido/eventos/` y el módulo declarado como abierto, el test pasa.
2. **Todo evento lleva `UUID eventoId` y `Instant ocurridoEn`.** El id sirve para que los listeners detecten duplicados (§9); la fecha, para el historial.
3. **Columnas que el diseño no tiene y hacen falta:**
   - `auth.usuario.password_hash` para el login.
   - En `compras.gestion_item`: `sector_id` (reportes), `tipo` y `detalle` (para mostrar en la bandeja), `prioridad` (ordenar por urgencia) y `solicitado_en` (tiempos de resolución y recordatorios).
   - En `solicitudes.item`: `proveedor_aprobado`, `motivo_rechazo` y `orden_enviada`, para mostrarle el resultado al solicitante y saber cuándo cerrar la solicitud.
4. **Eventos que el diseño no tiene:** `ItemEditado`, `ItemCancelado`, `SolicitudCancelada` y `CancelacionRechazada` (sincronizar Solicitudes con Compras), `CotizacionCargada` (historial), `CategoriaUnificada` (reportes), `EncargadoDesasignado` (caso borde 15).
5. **Adjuntos:** subida directa (multipart) a una carpeta del servidor, en vez de URL prefirmada a S3.

---

## T01 · Preparar el repositorio y el entorno

Todos los comandos y archivos exactos están en `guias/sprint-0-paso-a-paso.md`.

### T01.1 · Limpiar el repo
- Sacar `.idea/` del repo sin borrarla del disco (`git rm -r --cached .idea`).
- Borrar `HELP.md` y `back/resources/` (con su `aplication.properties` vacío y su `db/migration` vacía).
- En `.gitignore` (raíz): borrar las líneas `mvnw`, `mvnw.cmd` y `.gitignore`. El wrapper de Maven tiene que estar versionado.
- Verificar con `git ls-files` que no quede nada de eso.
- Docs: [git rm](https://git-scm.com/docs/git-rm).

### T01.2 · Plantilla de PR y protección de `main`
- Crear `.github/pull_request_template.md` con: ID de la tarea, qué cambia, cómo probarlo y un checklist (tests pasan, lint pasa, sin credenciales).
- En GitHub (lo hace el dueño del repo): regla sobre `main` que exija PR con 1 aprobación y bloquee force push. Agregar a los integrantes como colaboradores.
- Docs: [plantillas de PR](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository), [rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets).

### T01.3 · Base de datos y `.env` de cada integrante
- Opción A (recomendada): Postgres con el `back/compose.yaml` que ya existe. Cada uno completa `back/.env` con `POSTGRES_DB`, `POSTGRES_USER` y `POSTGRES_PASSWORD` propios.
- Opción B: Postgres instalado. Además de lo anterior, `DB_HOST`, `DB_PORT` y `USAR_DOCKER_COMPOSE=false` en el `.env`, y en `application.properties` la línea `spring.docker.compose.enabled=${USAR_DOCKER_COMPOSE:true}`. Agregar esas claves vacías a `back/.env.example`.
- Verificar que `./mvnw spring-boot:run` desde `back/` arranque.
- Docs: [Docker Compose en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/reference/features/dev-services.html#features.dev-services.docker-compose).

### T01.4 · Fijar la versión de Postgres
- En `back/compose.yaml`, cambiar `postgres:latest` por una versión fija (por ejemplo `postgres:17`), así todos usan la misma. Usar la misma en Testcontainers (T02.3).

---

## T02 · Esqueleto modular, migraciones base y tests

Código probado en `guias/sprint-0-paso-a-paso.md`.

### T02.1 · Paquetes de los 8 módulos y test de arquitectura
- Crear un `package-info.java` con `@ApplicationModule(displayName = "...")` en cada paquete: `auth`, `catalogo`, `solicitudes`, `compras`, `ordenes`, `reportes`, `integraciones`, `auditoria`.
- Crear `back/src/test/java/com/Sistem/Solicitude_Compra/ModularityTests.java` con dos tests: uno que llama a `ApplicationModules.verify()` y otro que genera la documentación de módulos con `Documenter`.
- Debe pasar `./mvnw test -Dtest=ModularityTests` y listar los 8 módulos.
- Docs: [fundamentos](https://docs.spring.io/spring-modulith/reference/fundamentals.html), [verificación](https://docs.spring.io/spring-modulith/reference/verification.html).

### T02.2 · Migraciones iniciales y validación de Hibernate
- `back/src/main/resources/db/migration/V1__schemas_iniciales.sql`: `CREATE SCHEMA` para `auth`, `catalogo`, `solicitudes`, `compras`, `ordenes`, `reportes`, `auditoria`.
- `V2__event_publication.sql`: tabla `event_publication` con el SQL oficial de Modulith para PostgreSQL.
- En `application.properties`: `spring.jpa.hibernate.ddl-auto=validate`.
- Al arrancar, Flyway aplica V1 y V2 y ya no aparece el error de `event_publication`.
- Docs: [SQL de Modulith](https://docs.spring.io/spring-modulith/reference/appendix.html#schemas.postgresql), [Flyway en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway).

### T02.3 · Tests con Testcontainers
- En `back/pom.xml`, dependencias de test: `spring-boot-testcontainers`, `org.testcontainers:testcontainers-junit-jupiter` y `org.testcontainers:testcontainers-postgresql` (sin versión, la maneja Spring Boot).
- Crear `back/src/test/java/.../TestcontainersConfiguration.java` con un bean `PostgreSQLContainer` anotado con `@ServiceConnection`. La clase está en `org.testcontainers.postgresql` (Testcontainers 2.x).
- `SolicitudeCompraApplicationTests` lleva `@Import(TestcontainersConfiguration.class)`. Todo test futuro que use la base, también.
- `./mvnw test` pasa sin ninguna base creada a mano (3 tests).
- Docs: [Testcontainers en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/reference/testing/testcontainers.html), [módulo Postgres](https://java.testcontainers.org/modules/databases/postgres/).

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
- Docs: [Spring Modulith: fundamentos (tipos de módulo)](https://docs.spring.io/spring-modulith/reference/fundamentals.html).

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
    - Cualquier otra excepción → 500, `ERROR_INTERNO`: se loguea completa y al cliente solo le llega un mensaje genérico.
- Tests: un `@WebMvcTest` con un controller de prueba que lanza cada excepción y verifica código HTTP y cuerpo.
- Docs: [`@RestControllerAdvice`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html), [errores REST](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html), [validación](https://docs.spring.io/spring-boot/4.1.1/reference/io/validation.html).

### T03.3 · Respuesta paginada estándar
- En `compartido/web/`, record `Pagina<T>` con `contenido` (lista), `pagina`, `tamanio`, `totalElementos` y `totalPaginas`, y un método estático que lo arma desde un `Page<T>` de Spring Data.
- Todos los listados reciben `?page=0&size=20` (Spring los convierte solo en `Pageable`) y devuelven `Pagina<T>`. Así el front siempre recibe la misma forma.
- Tamaño máximo de página: 100 (`spring.data.web.pageable.max-page-size=100`).
- Docs: [paginación en Spring Data](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html).

### T03.4 · Documentación de la API con Swagger
- Agregar al `pom.xml` `org.springdoc:springdoc-openapi-starter-webmvc-ui`. Usar la versión compatible con Spring Boot 4 que indica springdoc.org (la 3.x).
- Queda disponible en `http://localhost:8080/swagger-ui.html`. Cuando exista seguridad (T04.4), esa ruta y `/v3/api-docs/**` tienen que quedar abiertas y Swagger tiene que permitir cargar el token (botón "Authorize").
- Docs: [springdoc](https://springdoc.org/).

### T03.5 · Integración continua con GitHub Actions
- Crear `.github/workflows/ci.yml` que corra en cada PR y en cada push a `main`, con dos jobs:
  - **backend:** instala Java 25 (temurin) con caché de Maven y corre `./mvnw -B verify` en `back/`. Los runners de GitHub tienen Docker, así que Testcontainers funciona.
  - **frontend:** instala Node 22 con caché de npm y, en `frontend/`, corre `npm ci`, `npm run lint` y `npm run build`.
- En la regla de `main` (T01.2), exigir que los dos jobs pasen antes de mergear.
- Docs: [GitHub Actions con Java y Maven](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven).

---

## T04 · Usuarios, sectores y login (módulo `auth`)

### T04.1 · Schema y dominio de `auth`
- **Migración** `V<fecha>_1__auth_tablas.sql`: tablas `auth.sector` y `auth.usuario` como en §6.1, más `password_hash VARCHAR(100) NOT NULL` en usuario.
- **Dominio** en `auth/dominio/`:
  - `Sector`: `id`, `nombre` (único, obligatorio, hasta 100 caracteres).
  - `Usuario`: `id`, `email` (único, obligatorio), `nombre`, `sector` (`@ManyToOne` a Sector), `rol` (enum `Rol` de `compartido/tipos`, guardado como texto con `@Enumerated(EnumType.STRING)`), `passwordHash`, `activo` (por defecto true).
- Hibernate con `validate` tiene que arrancar sin errores: los nombres y tipos de las columnas tienen que coincidir con la migración.
- Docs: [entidades JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html), [guía Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/).

### T04.2 · Repositories de `auth`
- En `auth/repositorio/`:
  - `SectorRepository extends JpaRepository<Sector, Long>`: `existsByNombreIgnoreCase(String)`.
  - `UsuarioRepository extends JpaRepository<Usuario, Long>`: `findByEmailIgnoreCase(String)`, `findByRolAndActivoTrue(Rol)` (los Encargados activos, para avisos), `countByRolAndActivoTrue(Rol)` (para la regla del último Encargado), `existsByEmailIgnoreCase(String)`.
- Test `@DataJpaTest` (con `@Import(TestcontainersConfiguration.class)`) que guarda y busca por email sin distinguir mayúsculas.
- Docs: [métodos de consulta](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).

### T04.3 · Datos de prueba
- Clase `auth/servicio/DatosDePrueba` que implementa `CommandLineRunner` y solo corre si en el `.env` está `DATOS_PRUEBA=true` (con `@ConditionalOnProperty`).
- Si la tabla de usuarios está vacía, crea:
  - Sectores: Recursos Humanos, Compras, Administración, Sistemas.
  - Un usuario por rol: `solicitante@test.com` (Recursos Humanos), `encargado@test.com` (Compras), `admin@test.com` (Administración). La contraseña sale de `DATOS_PRUEBA_PASSWORD` en el `.env` y se guarda cifrada con el `PasswordEncoder`.
- Agregar `DATOS_PRUEBA` y `DATOS_PRUEBA_PASSWORD` a `back/.env.example`.
- No se hace con una migración porque las migraciones también corren en producción, y no queremos usuarios de prueba ahí.

### T04.4 · Login con JWT y configuración de seguridad
- Dependencias: `spring-boot-starter-security-oauth2-resource-server` (trae Spring Security y el soporte de JWT) y, para tests, `spring-boot-starter-security-test`.
- **Clave de firma:** `JWT_SECRET` en `.env` (al menos 32 caracteres, porque HS256 exige 256 bits) y `JWT_EXPIRACION_HORAS=8`. Agregarlas a `.env.example` sin valor.
- **Configuración** en `auth/servicio/SeguridadConfig` (o `auth/web/`):
  - `PasswordEncoder`: `BCryptPasswordEncoder`.
  - `JwtEncoder` y `JwtDecoder` con la clave HMAC (`NimbusJwtEncoder` y `NimbusJwtDecoder.withSecretKey`).
  - `JwtAuthenticationConverter` que lee el claim `rol` y lo convierte en la authority `ROLE_<rol>`. Así se puede usar `hasRole('ENCARGADO')`.
  - `SecurityFilterChain`: sin sesión (stateless), CSRF desactivado (la API no usa cookies), `oauth2ResourceServer(jwt)`. Rutas abiertas: `POST /api/v1/auth/login`, Swagger y `/actuator/health`. Todo lo demás requiere token.
- **Service** `auth/servicio/LoginService.login(email, password)`:
  - Busca el usuario por email. Si no existe, está inactivo o la contraseña no coincide, lanza el **mismo** error 401 "Email o contraseña incorrectos" (no hay que revelar cuál de las tres falló). Para eso, crear `CredencialesInvalidasException` en `compartido/errores/` y mapearla a 401 con código `CREDENCIALES_INVALIDAS` en el manejador de T03.2.
  - Si está bien, arma un JWT con `sub` = id del usuario, y los claims `email`, `nombre`, `rol` y `sectorId`, con vencimiento a las `JWT_EXPIRACION_HORAS`.
- **Endpoint** `auth/web/AuthController`:
  - `POST /api/v1/auth/login`, body `{ "email", "password" }` (ambos obligatorios) → 200 `{ "token", "usuario": { "id", "nombre", "email", "rol", "sectorId" } }`.
- **Usuario actual:** en `compartido/seguridad/UsuarioActual`, un componente con `id()`, `rol()` y `esEncargado()` que lee el JWT del `SecurityContextHolder`. Los services de todos los módulos lo usan para saber quién hace la acción.
- **Tests:**
  - Login correcto devuelve un token.
  - Contraseña incorrecta devuelve 401.
  - Un endpoint protegido sin token devuelve 401.
  - En los tests de otros endpoints, simular el usuario con `.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO")))` en MockMvc.
- Aviso al equipo: cuando esto se mergea, todos los endpoints piden token. Los tests de los endpoints ya existentes tienen que empezar a simular el usuario.
- Docs: [Spring Boot y Security](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html), [JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [contraseñas](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html), [autorizar requests](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).

### T04.5 · Usuario logueado y `AuthApi`
- **Endpoint** `GET /api/v1/auth/yo` → los datos del usuario del token, más el nombre del sector. El front lo llama al recargar la página.
- **`auth/AuthApi`** (clase pública en la raíz del módulo, que usan otros módulos):
  - `UsuarioResumen obtenerUsuario(Long id)`: id, nombre, email, rol, sectorId, activo. `UsuarioResumen` es un record público en la raíz de `auth/`.
  - `Long sectorDe(Long usuarioId)`.
  - `String nombreSector(Long sectorId)`.
- Docs: [Spring Modulith: fundamentos (API de un módulo)](https://docs.spring.io/spring-modulith/reference/fundamentals.html).

---

## T05 · Permisos por rol y administración de usuarios

### T05.1 · Permisos por rol en todos los endpoints
- Activar `@EnableMethodSecurity` y poner `@PreAuthorize` en cada controller o método:

| Endpoints | Quién puede |
|---|---|
| `/api/v1/solicitudes/**`, `/api/v1/items/**/adjuntos` | Cualquier usuario logueado (el service controla que sea dueño) |
| `/api/v1/bandeja/**`, `/api/v1/cotizaciones/**`, `/api/v1/ordenes/**`, `POST/PATCH /api/v1/proveedores`, `PATCH/POST /api/v1/categorias/**` | `ENCARGADO` |
| `GET /api/v1/categorias`, `GET /api/v1/proveedores` | Cualquier usuario logueado |
| `/api/v1/admin/**` | `ADMIN` |
| `/api/v1/reportes/**`, `/api/v1/historial/**` | `ENCARGADO` o `ADMIN` |

- Reglas que no se resuelven por rol y van en el service, con `SinPermisoException`: un Solicitante solo ve y edita sus propias solicitudes; un Encargado solo carga cotizaciones, aprueba o rechaza ítems asignados a él.
- Tests: para cada grupo, un caso con el rol correcto (200) y uno con otro rol (403).
- Docs: [seguridad a nivel de método](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html).

### T05.2 · ABM de sectores (Admin)
- `GET /api/v1/admin/sectores` → lista completa.
- `POST /api/v1/admin/sectores` `{ "nombre" }` → 201. Nombre duplicado (sin distinguir mayúsculas) → 409.
- `PATCH /api/v1/admin/sectores/{id}` `{ "nombre" }` → 200.
- No se borran sectores: los usuarios y solicitudes los referencian.

### T05.3 · ABM de usuarios (Admin)
- `GET /api/v1/admin/usuarios?q=&rol=&page=` → `Pagina` de usuarios (sin el hash de la contraseña).
- `POST /api/v1/admin/usuarios` `{ "email", "nombre", "sectorId", "rol", "passwordInicial" }` → 201. Email válido y único; contraseña de 8 caracteres o más, guardada cifrada.
- `PATCH /api/v1/admin/usuarios/{id}` `{ "nombre", "sectorId", "rol", "activo" }` (todos opcionales) → 200.
- `POST /api/v1/admin/usuarios/{id}/password` `{ "passwordNueva" }` → 204.

### T05.4 · Reglas del último Encargado y quitar el rol
- Al desactivar un usuario o cambiarle el rol: si es el único `ENCARGADO` activo (`countByRolAndActivoTrue`), lanzar `ReglaNegocioException("ULTIMO_ENCARGADO", "Asigná otro Encargado antes de desactivar a este")` (caso borde 16).
- Si a un Encargado se le quita el rol o se lo desactiva, publicar el evento `EncargadoDesasignado(eventoId, ocurridoEn, usuarioId, adminId)`.
- Compras escucha ese evento: sus ítems en `EN_COTIZACION` quedan sin encargado (vuelven a la bandeja general, siguen En cotización) y se publica `ItemAsignado` con `encargadoNuevoId = null` para cada uno, así queda en el historial (caso borde 15).
- El aviso a los demás Encargados se agrega en T15 si hay tiempo.

---

## T06 · Catálogo de categorías y proveedores (módulo `catalogo`)

### T06.1 · Schema y dominio del catálogo
- **Migración** `V<fecha>_1__catalogo_tablas.sql`: tablas `catalogo.categoria`, `catalogo.proveedor` y `catalogo.proveedor_categoria`, y los índices de §6.3. Incluir el índice único `(tipo, lower(nombre))`.
- **Dominio** en `catalogo/dominio/`:
  - `EstadoCategoria`: NUEVA, CONOCIDA, UNIFICADA.
  - `Categoria`: `id`, `nombre` (obligatorio, hasta 120), `tipo` (`TipoItem`), `estado` (por defecto NUEVA), `unificadaEn` (`@ManyToOne` a Categoria, puede ser null).
  - `Proveedor`: `id`, `razonSocial` (obligatorio, hasta 200), `cuit` (opcional, único, formato `XX-XXXXXXXX-X`), `telefono`, `email` (validado con `@Email`), `contacto`, `activo` (por defecto true), `categorias` (`@ManyToMany` a Categoria sobre la tabla `catalogo.proveedor_categoria`).
- Regla del enunciado: el proveedor **no guarda precios**.

### T06.2 · Repositories del catálogo
- `CategoriaRepository`:
  - `findByTipoAndNombreIgnoreCase(TipoItem, String)`: para no crear duplicados.
  - `findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(TipoItem, String, EstadoCategoria)`: autocompletado (excluye las UNIFICADAS).
  - `findByEstado(EstadoCategoria, Pageable)`: para que el Encargado vea las Nuevas.
- `ProveedorRepository`:
  - `findByActivoTrueAndCategorias_Id(Long categoriaId)`: proveedores sugeridos.
  - `findByRazonSocialContainingIgnoreCase(String, Pageable)`: buscador.
  - `existsByCuit(String)`.

### T06.3 · ABM de proveedores
- `GET /api/v1/proveedores?q=&categoriaId=&activo=&page=` → `Pagina` de `{ id, razonSocial, cuit, telefono, email, contacto, activo, categorias: [{ id, nombre }] }`.
- `GET /api/v1/proveedores/{id}`.
- `POST /api/v1/proveedores` `{ razonSocial, cuit, telefono, email, contacto, categoriaIds: [] }` → 201. Al menos una categoría; CUIT único (409 si se repite).
- `PATCH /api/v1/proveedores/{id}` → mismos campos, más `activo`, todos opcionales. Dar de baja = `activo: false` (no se borra, por el caso borde 8).
- Tests del service: alta, CUIT duplicado, baja.

### T06.4 · Búsqueda de categorías
- `GET /api/v1/categorias?tipo=PRODUCTO&q=glo` → hasta 10 `{ id, nombre, tipo, estado }`, sin las UNIFICADAS. Es lo que usa el autocompletado del formulario.
- `GET /api/v1/categorias?estado=NUEVA&page=` → `Pagina`, para que el Encargado revise las nuevas.

### T06.5 · `CatalogoApi` para otros módulos
- Clase pública `catalogo/CatalogoApi` con:
  - `boolean existeCategoria(Long id)`.
  - `Long obtenerOCrearCategoria(TipoItem tipo, String nombre)`: busca sin distinguir mayúsculas. Si existe, devuelve su id (si está UNIFICADA, devuelve el id de la destino). Si no existe, la crea con estado NUEVA, publica `CategoriaCreada` y devuelve el id nuevo. La usa Solicitudes cuando el usuario escribe una categoría a mano.
  - `String nombreCategoria(Long id)`.
  - `ProveedorResumen obtenerProveedor(Long id)`: id, razón social, activo. `ProveedorResumen` es un record público en la raíz de `catalogo/`.
  - `List<ProveedorResumen> proveedoresActivosDe(Long categoriaId)`.
- La necesita T07.4 en S1, por eso vence antes que el resto del catálogo.

### T06.6 · Confirmar una categoría Nueva
- `PATCH /api/v1/categorias/{id}` `{ "estado": "CONOCIDA" }` → 200. Solo de NUEVA a CONOCIDA; otro cambio → 422 `TRANSICION_INVALIDA`.

### T06.7 · Proveedores sugeridos
- `proveedoresActivosDe` de `CatalogoApi` (T06.5) la usa Compras en T10.5. Acá hay que asegurar que funcione con categorías unificadas: si se pide una categoría UNIFICADA, devolver los proveedores de la destino.
- Si no hay ninguno, devuelve lista vacía: el front ofrece dar de alta un proveedor (caso borde 10).

### T06.8 · Unificar categorías duplicadas
- `POST /api/v1/categorias/{id}/unificar` `{ "destinoId" }` → 200. Mismo tipo; la destino no puede estar UNIFICADA.
- La categoría de origen queda UNIFICADA con `unificadaEn` = destino. Los proveedores que tenía pasan a tener la destino (sin duplicar filas).
- Los ítems viejos **no se modifican** (caso borde 9: no se reescribe el pasado).
- Publicar `CategoriaUnificada(eventoId, ocurridoEn, origenId, destinoId, usuarioId)`. Reportes la escucha para agrupar bajo la destino (T16.2).

---

## T07 · Solicitudes: crear y consultar (módulo `solicitudes`)

### T07.1 · Schema y dominio de solicitudes
- **Migración** `V<fecha>_1__solicitudes_tablas.sql`: `solicitudes.solicitud` y `solicitudes.item` como en §6.2, más en `item`: `proveedor_aprobado VARCHAR(200)`, `motivo_rechazo TEXT` y `orden_enviada BOOLEAN NOT NULL DEFAULT FALSE`. La tabla de adjuntos se hace en T14.2.
- **Dominio** en `solicitudes/dominio/`:
  - `EstadoSolicitud`: EN_REVISION, LISTA, CERRADA, CANCELADA.
  - `EstadoItem`: PENDIENTE, EN_COTIZACION, APROBADO, RECHAZADO, CANCELADO. Método `esFinal()`: true para APROBADO, RECHAZADO y CANCELADO.
  - `Solicitud`: `id`, `solicitanteId`, `sectorId`, `urgencia`, `fechaNecesaria`, `observaciones`, `estado` (por defecto EN_REVISION), `creadaEn`, `version` (`@Version`), `items` (`@OneToMany(mappedBy = "solicitud", cascade = ALL, orphanRemoval = true)`).
  - `Item`: `id`, `solicitud` (`@ManyToOne`), `tipo`, `categoriaId`, `detalle`, `cantidad` (`BigDecimal`, 12 dígitos con 2 decimales, mayor a 0), `estado` (por defecto PENDIENTE), `creadoEn`, `version`, `proveedorAprobado`, `motivoRechazo`, `ordenEnviada`.
- **Reglas dentro de las entidades** (así se testean sin base de datos):
  - `Solicitud.agregarItem(...)`.
  - `Item.puedeEditarse()`: solo si está PENDIENTE.
  - `Solicitud.puedeCancelarse()`: solo si todos los ítems están PENDIENTES.
  - `Solicitud.recalcularEstado()`: se implementa en T08.6.
- Test unitario: una solicitud sin ítems no es válida; un ítem con cantidad 0 no es válido.

### T07.2 · Repositories de solicitudes
- `SolicitudRepository`: `findBySolicitanteId(Long, Pageable)`, `findBySolicitanteIdAndEstado(Long, EstadoSolicitud, Pageable)`, `findByEstado(EstadoSolicitud, Pageable)` (para el Encargado), y un método que traiga la solicitud con sus ítems en una sola consulta (`@EntityGraph(attributePaths = "items")`) para el detalle.
- `ItemRepository`: `findById`.

### T07.3 · Definir los eventos del sistema
En `compartido/eventos/`, un `record` por evento. Todos empiezan con `UUID eventoId` y `Instant ocurridoEn`. Los datos que lleva cada uno son los que necesitan sus listeners, para que no tengan que consultar otro módulo:

| Evento | Publica | Escuchan | Datos (además de `eventoId` y `ocurridoEn`) |
|---|---|---|---|
| `SolicitudCreada` | Solicitudes | Compras, Integraciones, Auditoría | `solicitudId`, `solicitanteId`, `sectorId`, `urgencia`, `fechaNecesaria`, `items`: lista de `ItemCreado(itemId, tipo, categoriaId, cantidad, detalle)` |
| `ItemEditado` | Solicitudes | Compras, Auditoría | `itemId`, `solicitudId`, `categoriaId`, `cantidad`, `detalle`, `usuarioId` |
| `ItemCancelado` | Solicitudes | Compras, Auditoría | `itemId`, `solicitudId`, `usuarioId` |
| `SolicitudCancelada` | Solicitudes | Compras, Auditoría | `solicitudId`, `itemIds`, `usuarioId` |
| `SolicitudLista` | Solicitudes | Integraciones, Auditoría | `solicitudId`, `solicitanteId`, `resultados`: lista de `ResultadoItem(itemId, categoriaId, detalle, estado, proveedorRazonSocial, motivoRechazo)` |
| `SolicitudCerrada` | Solicitudes | Auditoría | `solicitudId` |
| `CategoriaCreada` | Catálogo | Auditoría | `categoriaId`, `nombre`, `tipo` |
| `CategoriaUnificada` | Catálogo | Reportes, Auditoría | `origenId`, `destinoId`, `usuarioId` |
| `ItemAsignado` | Compras | Solicitudes, Auditoría | `itemId`, `solicitudId`, `encargadoAnteriorId` (null si nadie lo tenía), `encargadoNuevoId` (null si queda sin asignar) |
| `CancelacionRechazada` | Compras | Solicitudes | `itemId`, `solicitudId` (el ítem ya estaba tomado cuando llegó la cancelación) |
| `CotizacionCargada` | Compras | Auditoría | `cotizacionId`, `itemId`, `proveedorId`, `moneda`, `precioTotal`, `usuarioId` |
| `ItemAprobado` | Compras | Órdenes | `itemId`, `solicitudId`, `sectorId`, `categoriaId`, `cotizacionId`, `proveedorId`, `proveedorRazonSocial`, `cantidad`, `moneda`, `precioUnitario`, `precioTotal`, `encargadoId` |
| `ItemResuelto` | Compras | Solicitudes, Reportes, Auditoría | `itemId`, `solicitudId`, `resultado` (APROBADO o RECHAZADO), `proveedorRazonSocial`, `motivoRechazo`, `urgencia`, `solicitadoEn`, `resueltoEn`, `encargadoId` |
| `OrdenGenerada` | Órdenes | Reportes, Auditoría | `ordenId`, `numero`, `itemId`, `solicitudId`, `sectorId`, `categoriaId`, `proveedorId`, `moneda`, `precioTotal` |
| `OrdenEnviada` | Órdenes | Solicitudes, Auditoría | `ordenId`, `itemId`, `solicitudId`, `usuarioId` |
| `EncargadoDesasignado` | Auth | Compras, Auditoría | `usuarioId`, `adminId` |

- Para publicar: inyectar `ApplicationEventPublisher` en el service y llamar a `publishEvent(...)` dentro del método `@Transactional`. Modulith guarda el evento en `event_publication` en la misma transacción.
- Para escuchar: un método con `@ApplicationModuleListener` en un componente del módulo que escucha. Corre después del commit, en otra transacción y en otro hilo.
- Esta subtarea es solo definir los records: la publican y escuchan las demás subtareas.
- Docs: [Spring Modulith: eventos](https://docs.spring.io/spring-modulith/reference/events.html).

### T07.4 · Crear una solicitud
- **Endpoint** `POST /api/v1/solicitudes` con body (DTO con `@Valid`):
  ```
  { urgencia, fechaNecesaria, observaciones,
    items: [ { tipo, categoriaId | categoriaNueva, cantidad, detalle } ] }
  ```
  - `urgencia`, `fechaNecesaria` y al menos un ítem son obligatorios. `fechaNecesaria` no puede ser anterior a hoy.
  - Cada ítem tiene `tipo`, `cantidad` > 0, y **exactamente uno** de `categoriaId` o `categoriaNueva`.
- **Service** `SolicitudService.crear(...)`, `@Transactional`:
  1. Solicitante = `UsuarioActual.id()`; sector = `AuthApi.sectorDe(solicitanteId)`. El sector se copia, así si el usuario cambia de sector los reportes viejos no cambian (caso borde 17).
  2. Por cada ítem: si trae `categoriaId`, verificar `CatalogoApi.existeCategoria` (si no existe, 422 `CATEGORIA_INEXISTENTE`). Si trae `categoriaNueva`, usar `CatalogoApi.obtenerOCrearCategoria(tipo, nombre)`.
  3. Guardar la solicitud con sus ítems.
  4. Publicar `SolicitudCreada`.
- **Respuesta:** 201, header `Location: /api/v1/solicitudes/{id}`, body `{ id, estado, items: [{ id, estado }] }` (ejemplo en §7.1).
- **Tests:** crear con 2 ítems (uno con categoría nueva) y verificar que se publicó el evento (con `Scenario` o `@RecordApplicationEvents`); sin ítems → 400; cantidad 0 → 400; categoría inexistente → 422.

### T07.5 · Consultar solicitudes
- `GET /api/v1/solicitudes?mias=true&estado=&page=` → `Pagina` de `{ id, creadaEn, urgencia, fechaNecesaria, estado, cantidadItems }`, más nuevas primero.
  - Un Solicitante siempre ve solo las suyas, aunque mande `mias=false`.
  - Un Encargado o Admin con `mias=false` ve todas.
- `GET /api/v1/solicitudes/{id}` → `{ id, solicitante: { id, nombre }, sector, urgencia, fechaNecesaria, observaciones, estado, creadaEn, version, items: [{ id, tipo, categoriaId, categoriaNombre, detalle, cantidad, estado, proveedorAprobado, motivoRechazo, version }] }`.
  - Si es un Solicitante y la solicitud no es suya → 403. Si no existe → 404.
  - Nombres de categoría y solicitante vía `CatalogoApi` y `AuthApi`.

### T07.6 · Tests del módulo Solicitudes
- Tests de endpoints con MockMvc para T07.4 y T07.5, incluyendo: un Solicitante no puede ver la solicitud de otro (403).
- Un `@ApplicationModuleTest` del módulo `solicitudes` que cree una solicitud y verifique con `Scenario` que se publicó `SolicitudCreada` con los datos correctos.
- Docs: [tests de módulos](https://docs.spring.io/spring-modulith/reference/testing.html).

---

## T08 · Solicitudes: editar, cancelar y estado automático

### T08.1 · Editar un ítem Pendiente
- `PATCH /api/v1/solicitudes/{id}/items/{itemId}` `{ categoriaId | categoriaNueva, cantidad, detalle, version }` → 200 con el ítem actualizado.
- Reglas: solo el dueño (si no, 403); solo si el ítem está PENDIENTE (si no, 422 `ITEM_NO_PENDIENTE`, "Este ítem ya está siendo cotizado"); la `version` tiene que coincidir (si no, 409).
- Publicar `ItemEditado`.

### T08.2 · Borrar (cancelar) un ítem Pendiente
- `DELETE /api/v1/solicitudes/{id}/items/{itemId}?version=` → 204.
- No se borra la fila: el ítem pasa a CANCELADO. Mismas reglas que T08.1.
- Publicar `ItemCancelado` y recalcular el estado de la solicitud (si todos quedaron cancelados, la solicitud pasa a CANCELADA, caso borde 12).

### T08.3 · Cancelar la solicitud entera
- `POST /api/v1/solicitudes/{id}/cancelar` `{ version }` → 200.
- Solo si **todos** los ítems están PENDIENTES. Si no, 422 `SOLICITUD_NO_CANCELABLE`, con un mensaje que indique cancelar ítem por ítem (caso borde 3).
- Todos los ítems pasan a CANCELADO y la solicitud a CANCELADA. Publicar `SolicitudCancelada`.

### T08.4 · Sincronizar ediciones y cancelaciones con Compras
- Compras tiene su propia copia de cada ítem (`gestion_item`) y los eventos llegan unos milisegundos después. En ese lapso, un Encargado puede tomar un ítem que el solicitante acaba de cancelar.
- En `compras/`, listeners:
  - `ItemEditado` → actualizar la copia (categoría, cantidad, detalle) solo si sigue PENDIENTE.
  - `ItemCancelado` y `SolicitudCancelada` → si la gestión sigue PENDIENTE, pasa a CANCELADO. Si ya estaba EN_COTIZACION (la carrera), publicar `CancelacionRechazada`.
- En `solicitudes/`, listener de `CancelacionRechazada`: el ítem vuelve a EN_COTIZACION y la solicitud a EN_REVISION.
- Test: con `Scenario`, cancelar un ítem cuya gestión ya está tomada y verificar que vuelve a EN_COTIZACION.

### T08.5 · Actualizar el estado de los ítems desde Compras
- Listener de `ItemAsignado`: si el ítem estaba PENDIENTE, pasa a EN_COTIZACION.
- Listener de `ItemResuelto`: pasa a APROBADO (guardando `proveedorAprobado`) o RECHAZADO (guardando `motivoRechazo`). Después, recalcular el estado de la solicitud (T08.6).
- Listener de `OrdenEnviada`: marcar `ordenEnviada = true` en el ítem y recalcular.

### T08.6 · Estado automático de la solicitud
- `Solicitud.recalcularEstado()` en el dominio, con estas reglas (enunciado "Estados de una solicitud" y casos borde 12 a 14):
  1. Todos los ítems CANCELADOS → CANCELADA. No se avisa como resuelta.
  2. Todos los ítems en estado final, y al menos uno no cancelado → LISTA.
  3. LISTA, y todos los ítems APROBADOS con `ordenEnviada = true` → CERRADA. Si no hay ningún aprobado (todos rechazados), pasa a CERRADA enseguida.
  4. Si no, EN_REVISION.
- Cuando pasa a LISTA, el service publica `SolicitudLista` (una sola vez) con el resultado de cada ítem. Cuando pasa a CERRADA, publica `SolicitudCerrada`.
- Tests unitarios de `recalcularEstado()` para cada caso: todos cancelados; aprobados y rechazados mezclados; todos rechazados; aprobados sin orden enviada; aprobados con orden enviada.

### T08.7 · Evitar solicitudes duplicadas por doble click
- `POST /api/v1/solicitudes` acepta el header opcional `Idempotency-Key` (un UUID que genera el front al abrir el formulario).
- Tabla `solicitudes.clave_idempotencia (clave UUID PRIMARY KEY, solicitud_id BIGINT, creada_en TIMESTAMPTZ)`. Si la clave ya existe, devolver la solicitud ya creada en vez de crear otra.

---

## T09 · Compras: bandeja y asignación (módulo `compras`)

### T09.1 · Schema y dominio de Compras
- **Migración** `V<fecha>_1__compras_gestion_item.sql`: `compras.gestion_item` como en §6.4, más `sector_id BIGINT NOT NULL`, `tipo VARCHAR(10) NOT NULL`, `detalle TEXT`, `prioridad SMALLINT NOT NULL` y `solicitado_en TIMESTAMPTZ NOT NULL`. El índice de la bandeja pasa a ser `(estado, prioridad, fecha_necesaria)`.
- **Dominio** en `compras/dominio/`:
  - `EstadoGestion`: PENDIENTE, EN_COTIZACION, APROBADO, RECHAZADO, CANCELADO.
  - `GestionItem`: `itemId` (clave primaria, sin autogenerar: es el id del ítem de Solicitudes), `solicitudId`, `sectorId`, `categoriaId`, `tipo`, `detalle`, `cantidad`, `urgencia`, `prioridad`, `fechaNecesaria`, `solicitadoEn`, `estado`, `encargadoId`, `cotizacionElegidaId`, `motivoRechazo`, `resueltoEn`, `ultimoRecordatorioEn`, `version` (`@Version`).
- **Por qué `prioridad`:** si se ordena por el texto de la urgencia, sale ALTA, BAJA, MEDIA (orden alfabético). Con `prioridad` (1 = Alta, 2 = Media, 3 = Baja, de `Urgencia.prioridad()`) el orden es el correcto.
- **Reglas en la entidad:** `tomar(encargadoId)`, `reasignar(encargadoId)`, `aprobar(cotizacion)` y `rechazar(motivo)`, cada una validando el estado actual y lanzando `ReglaNegocioException` si no corresponde.

### T09.2 · Crear las gestiones al entrar una solicitud
- En `compras/servicio/`, listener `@ApplicationModuleListener` de `SolicitudCreada`: por cada ítem crea una `GestionItem` en PENDIENTE, copiando `solicitudId`, `sectorId`, `categoriaId`, `tipo`, `detalle`, `cantidad`, `urgencia`, `prioridad`, `fechaNecesaria` y `solicitadoEn` (= `ocurridoEn` del evento).
- Idempotente: si ya existe la gestión de ese `itemId`, no hace nada.
- Test con `Scenario`: publicar `SolicitudCreada` con 2 ítems y verificar que se crearon 2 gestiones.

### T09.3 · Bandeja
- `GET /api/v1/bandeja?asignadosAMi=&urgencia=&page=` → `Pagina` de `{ itemId, solicitudId, tipo, categoriaId, categoriaNombre, detalle, cantidad, urgencia, fechaNecesaria, estado, encargadoId, encargadoNombre, version }`.
- Solo ítems en PENDIENTE o EN_COTIZACION, ordenados por `prioridad` y después por `fechaNecesaria` (enunciado: "dentro de la misma urgencia, primero aparece lo que se necesita antes").
- `asignadosAMi=true` filtra por `encargadoId = UsuarioActual.id()`.
- `GET /api/v1/bandeja/{itemId}` → la gestión más sus cotizaciones (T10.2).

### T09.4 · Tomar y reasignar
- `POST /api/v1/bandeja/{itemId}/tomar` → 200. Se permite si está PENDIENTE, o EN_COTIZACION sin encargado (caso T05.4). Pasa a EN_COTIZACION con `encargadoId` = yo. Si está asignado a otro → 409 `YA_ASIGNADO`.
- `POST /api/v1/bandeja/{itemId}/reasignar` → 200. Solo si está EN_COTIZACION y asignado a otro Encargado. Pasa a ser mío.
- Los dos publican `ItemAsignado` (con el encargado anterior y el nuevo).
- **Concurrencia (caso borde 1):** si dos Encargados toman el mismo ítem a la vez, `@Version` hace que el segundo `save` falle con `ObjectOptimisticLockingFailureException`, y el manejador de T03.2 la convierte en 409.

### T09.5 · Test de concurrencia
- Test de integración: dos hilos llaman a `tomar` sobre el mismo ítem al mismo tiempo (con un `CountDownLatch` para que arranquen juntos). Uno termina bien y el otro recibe la excepción de versión; el ítem queda asignado a uno solo.

---

## T10 · Compras: cotizaciones, aprobar y rechazar

### T10.1 · Schema y dominio de cotizaciones
- **Migración** `V<fecha>_1__compras_cotizaciones.sql`: `compras.cotizacion` como en §6.4, con el índice único parcial `ux_una_seleccionada`. La tabla de adjuntos de cotización va en T14.3.
- **Dominio:**
  - `EstadoCotizacion`: CANDIDATA, SELECCIONADA, DESCARTADA.
  - `Cotizacion`: `id`, `itemId`, `proveedorId`, `proveedorRazonSocial` (copiado), `moneda`, `precioUnitario`, `precioTotal`, `fechaValidez`, `entregaEstimada`, `observaciones`, `estado`, `cargadaPor`, `cargadaEn`.
  - Método `estaVigente(LocalDate hoy)`: `fechaValidez` mayor o igual a hoy. Hoy se calcula en hora de Argentina (`ZoneId.of("America/Argentina/Buenos_Aires")`, caso borde 19).
- Agregar `proveedor_razon_social` a la migración (el diseño no lo tiene; evita consultar el catálogo cada vez que se listan cotizaciones).
- Repository: `CotizacionRepository.findByItemIdOrderByCargadaEnDesc(Long)`.

### T10.2 · Cargar y listar cotizaciones
- `POST /api/v1/bandeja/{itemId}/cotizaciones` `{ proveedorId, moneda, precioUnitario, fechaValidez, entregaEstimada, observaciones }` → 201.
  - El ítem tiene que estar EN_COTIZACION y asignado a mí (si no, 422 o 403).
  - El proveedor tiene que existir y estar activo (`CatalogoApi.obtenerProveedor`); si no, 422 `PROVEEDOR_INACTIVO`.
  - `precioUnitario` > 0; `fechaValidez` no anterior a hoy.
  - `precioTotal = precioUnitario × cantidad` del ítem, calculado en el backend con `BigDecimal` y redondeado a 2 decimales (nunca con `double`).
  - Publicar `CotizacionCargada`.
- `GET /api/v1/bandeja/{itemId}` incluye `cotizaciones: [{ id, proveedorId, proveedorRazonSocial, moneda, precioUnitario, precioTotal, fechaValidez, entregaEstimada, observaciones, estado, vigente }]`.
- Monedas distintas en el mismo ítem se permiten y se muestran separadas, sin conversión (caso borde 7).

### T10.3 · Aprobar un ítem
- `POST /api/v1/bandeja/{itemId}/aprobar` `{ cotizacionId, version }` → 200.
- Validaciones, en orden:
  1. El ítem está EN_COTIZACION y asignado a mí.
  2. La cotización es de este ítem.
  3. La cotización está vigente **al momento de aprobar**. Si no: 422 `COTIZACION_VENCIDA`, "La cotización venció el {fecha}. Cargá una nueva." (§7.2 y caso borde 5).
  4. La `version` coincide.
- Efectos, en una transacción:
  - La cotización pasa a SELECCIONADA y las demás del ítem a DESCARTADA.
  - La gestión pasa a APROBADO, con `cotizacionElegidaId` y `resueltoEn` = ahora.
  - Publicar `ItemAprobado` (con todos los datos para la orden) e `ItemResuelto` (resultado APROBADO).
- Doble click (caso borde 4): lo frena `@Version` y el índice único de cotización seleccionada.

### T10.4 · Rechazar un ítem
- `POST /api/v1/bandeja/{itemId}/rechazar` `{ motivo, version }` → 200.
- `motivo` obligatorio y no vacío (regla 6: "Rechazado = tiene motivo").
- Se puede rechazar desde PENDIENTE (sin haber cotizado; enunciado: "se puede rechazar sin haber cotizado") o desde EN_COTIZACION si está asignado a mí.
- La gestión pasa a RECHAZADO, con `motivoRechazo` y `resueltoEn`. Publicar `ItemResuelto` (resultado RECHAZADO).

### T10.5 · Proveedores sugeridos para un ítem
- `GET /api/v1/bandeja/{itemId}/proveedores-sugeridos` → `CatalogoApi.proveedoresActivosDe(categoriaId del ítem)`, como `[{ id, razonSocial }]`.

### T10.6 · Marcar cotizaciones vencidas en la bandeja
- En la respuesta de la bandeja (T09.3), agregar `todasVencidas: true` cuando el ítem tiene cotizaciones y ninguna está vigente (caso borde 6). El front lo muestra como aviso.

---

## T11 · Órdenes de pedido (módulo `ordenes`)

### T11.1 · Schema y dominio de órdenes
- **Migración** `V<fecha>_1__ordenes_tablas.sql`: secuencia `ordenes.numero_orden_seq` y tabla `ordenes.orden_pedido` como en §6.5, con `item_id` único.
- **Dominio:**
  - `EstadoOrden`: GENERADA, ENVIADA.
  - `OrdenPedido` con todos los campos de §6.5.
- **Repository:** `OrdenPedidoRepository` con `existsByItemId(Long)`, `findByEstado(EstadoOrden, Pageable)`, y una consulta nativa `select nextval('ordenes.numero_orden_seq')` para obtener el número antes de guardar. JPA solo sabe generar el id con secuencias, no otro campo.

### T11.2 · Generar la orden al aprobarse un ítem
- Listener de `ItemAprobado` en `ordenes/servicio/`:
  1. Si ya existe una orden para ese `itemId`, no hace nada (idempotencia, §9).
  2. Si no, crea la orden con número de la secuencia, copiando proveedor, razón social, cantidad, moneda y precios del evento (la orden es un documento: no cambia si después se edita el proveedor).
  3. Publica `OrdenGenerada`.
- Se genera una orden **por ítem**, aunque varios ítems vayan al mismo proveedor (enunciado "Orden de pedido").
- Test con `Scenario`: publicar `ItemAprobado` dos veces con el mismo `itemId` y verificar que hay una sola orden.

### T11.3 · Listar órdenes
- `GET /api/v1/ordenes?estado=GENERADA&page=` → `Pagina` de `{ id, numero, itemId, solicitudId, proveedorRazonSocial, cantidad, moneda, precioUnitario, precioTotal, estado, emitidaEn, enviadaEn }`.
- `GET /api/v1/ordenes/{id}` → el detalle.

### T11.4 · Marcar una orden como enviada
- `POST /api/v1/ordenes/{id}/marcar-enviada` → 200. Solo desde GENERADA (si no, 422 `ORDEN_YA_ENVIADA`).
- Guarda `enviadaPor` = yo y `enviadaEn` = ahora. Publica `OrdenEnviada`.

### T11.5 · Orden en PDF
- `GET /api/v1/ordenes/{id}/pdf` → `application/pdf` con número, fecha, proveedor, cantidad, detalle, moneda y precios.
- Usar OpenPDF (agregar la dependencia `com.github.librepdf:openpdf` con la versión que indica su repo).
- Docs: [OpenPDF](https://github.com/LibrePDF/OpenPDF).

---

## T12 · Eventos confiables e idempotencia

### T12.1 · Reintento de eventos pendientes
- En `application.properties`:
  - `spring.modulith.events.republish-outstanding-events-on-restart=true`: si la app se cae después de guardar un cambio pero antes de que un listener termine, al reiniciar se vuelve a procesar el evento (§9: "Falla a mitad de una operación").
  - `spring.modulith.events.completion-mode=delete`: los eventos ya procesados se borran de la tabla, para que no crezca sin límite.
- Probar a mano: poner un `throw` temporal en un listener, crear una solicitud, ver que la fila queda sin completar en `event_publication`, sacar el `throw`, reiniciar y ver que se procesa.
- Docs: [Event Publication Registry](https://docs.spring.io/spring-modulith/reference/events.html).

### T12.2 · Listeners idempotentes
- Revisar cada listener y asegurar que procesar el mismo evento dos veces no duplique nada:
  - Los que crean una fila con clave natural (gestión por `itemId`, orden por `itemId`, hecho de gasto por `ordenId`): verificar si ya existe antes de crear.
  - Los que no tienen clave natural (historial de auditoría): tabla `<schema>.evento_procesado (evento_id UUID PRIMARY KEY, procesado_en TIMESTAMPTZ)` como en §6.7. Guardar el `eventoId` en la misma transacción y, si ya estaba, salir sin hacer nada.

### T12.3 · Tests entre módulos
- Un `@ApplicationModuleTest` por módulo que escucha eventos, usando `Scenario`: publicar el evento de entrada y esperar el resultado. Por ejemplo, `ItemAprobado` → hay una orden; `ItemResuelto` → el ítem de Solicitudes cambió de estado.
- Docs: [tests de módulos](https://docs.spring.io/spring-modulith/reference/testing.html).

---

## T13 · Historial de auditoría (módulo `auditoria`)

### T13.1 · Schema, dominio y repository
- **Migración**: `auditoria.historial` como en §6.7, más `auditoria.evento_procesado`.
- **Dominio** `EntradaHistorial` con los campos de §6.7. Sin setters: se crea una vez y nunca se modifica.
- **Repository:** solo guardar y `findByEntidadAndEntidadIdOrderByOcurridoEnAsc`. No exponer `delete`. Se puede extender `Repository<EntradaHistorial, Long>` en vez de `JpaRepository`, para declarar solo los métodos que se permiten.

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
| `ItemResuelto` | ITEM | CAMBIO_ESTADO | EN_COTIZACION → APROBADO/RECHAZADO (comentario = motivo) | encargado |
| `SolicitudLista` / `SolicitudCerrada` | SOLICITUD | CAMBIO_ESTADO | → LISTA / → CERRADA | — (sistema) |
| `OrdenGenerada` | ORDEN | CREACION | — → GENERADA | — (sistema) |
| `OrdenEnviada` | ORDEN | CAMBIO_ESTADO | GENERADA → ENVIADA | quien la envió |
| `CategoriaCreada` / `CategoriaUnificada` | CATEGORIA | CREACION / UNIFICACION | | |
| `EncargadoDesasignado` | USUARIO | CAMBIO_ROL | | admin |

### T13.3 · Consultar el historial
- `GET /api/v1/historial?entidad=SOLICITUD&id=123` → lista ordenada por fecha de `{ ocurridoEn, entidad, entidadId, accion, estadoAnterior, estadoNuevo, usuarioNombre, comentario }`.
- Para el detalle de una solicitud conviene que devuelva también las entradas de sus ítems: agregar `?incluirItems=true`, que busca las entradas de ITEM cuyos ids son los de esa solicitud. Los ids de ítems llegan en `SolicitudCreada`; guardarlos en una tabla `auditoria.item_solicitud (item_id, solicitud_id)`.

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

### T14.2 · Adjuntos de un ítem
- **Migración:** `solicitudes.adjunto_item` como en §6.2.
- `POST /api/v1/items/{itemId}/adjuntos` (multipart, campo `archivo`) → 201 `{ id, nombre, mimeType, tamanoBytes, subidoEn }`. Solo el dueño de la solicitud y mientras el ítem no esté en estado final.
- **Validación del tipo real** con Apache Tika (`org.apache.tika:tika-core`, versión 3.x de Maven Central): detectar el tipo por el contenido, no por la extensión. Aceptar solo `application/pdf`, `image/jpeg` e `image/png`; si no, 422 `TIPO_NO_PERMITIDO`. Más de 10 MB → 413 (lo corta Spring solo; mapearlo en el manejador de T03.2).
- `GET /api/v1/items/{itemId}/adjuntos` → lista.
- `GET /api/v1/adjuntos-item/{id}` → descarga el archivo, con `Content-Disposition: attachment` y el nombre original. Pueden descargarlo el dueño y los Encargados.
- Docs: [Uploading Files](https://spring.io/guides/gs/uploading-files/), [multipart](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/multipart.html), [Tika](https://tika.apache.org/).

### T14.3 · Adjuntos de una cotización
- **Migración:** `compras.adjunto_cotizacion` como en §6.4.
- `POST /api/v1/cotizaciones/{id}/adjuntos`, `GET /api/v1/cotizaciones/{id}/adjuntos` y `GET /api/v1/adjuntos-cotizacion/{id}`: igual que T14.2, pero solo para Encargados.

### T14.4 · (Opcional) Almacenamiento S3/MinIO
- Solo si sobra tiempo: otra implementación de `AlmacenamientoArchivos` que use MinIO, elegida por configuración.

---

## T15 · Notificaciones y recordatorios con n8n (módulo `integraciones`)

### T15.1 · n8n y Mailpit en Docker
- Agregar dos servicios a `back/compose.yaml`:
  - `n8n`: imagen `docker.n8n.io/n8nio/n8n`, puerto `5678`, un volumen para no perder los workflows, zona horaria `America/Argentina/Buenos_Aires` (variables `GENERIC_TIMEZONE` y `TZ`), y `extra_hosts: ["host.docker.internal:host-gateway"]` para que n8n llegue al backend que corre en la máquina (en Linux no existe por defecto).
  - `mailpit`: imagen `axllent/mailpit`, puertos `8025` (interfaz web para ver los mails) y `1025` (SMTP).
- En n8n, credencial SMTP: host `mailpit`, puerto `1025`, sin usuario (van en la misma red de Docker).
- Verificar: n8n en `http://localhost:5678`, Mailpit en `http://localhost:8025`.
- Docs: [n8n con Docker](https://docs.n8n.io/hosting/installation/docker/), [Mailpit](https://mailpit.axllent.org/docs/).

### T15.2 · Enviar los avisos a n8n
- `.env`: `N8N_WEBHOOK_URL=http://localhost:5678/webhook` (y en `.env.example` sin valor).
- En `integraciones/servicio/`, un `RestClient` configurado con esa URL, y listeners:
  - `SolicitudCreada` → `POST {N8N_WEBHOOK_URL}/solicitud-creada` con `{ solicitudId, solicitanteNombre, sector, urgencia, fechaNecesaria, cantidadItems, emailsEncargados: [] }`.
  - `SolicitudLista` → `POST {N8N_WEBHOOK_URL}/solicitud-lista` con `{ solicitudId, solicitanteEmail, solicitanteNombre, items: [{ categoria, detalle, resultado, proveedor, motivoRechazo }] }`.
- Agregar a `AuthApi` el método `List<String> emailsEncargadosActivos()`.
- Si n8n no responde, el listener lanza una excepción y el evento queda pendiente en `event_publication`; se reintenta al reiniciar (T12.1). Así un n8n caído no pierde avisos (§9).
- Docs: [RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html).

### T15.3 · Workflow: aviso de solicitud nueva
- En n8n: nodo **Webhook** (POST, path `solicitud-creada`) → nodo **Send Email** a `emailsEncargados`, con asunto "Nueva solicitud #{id} — urgencia {urgencia}" y el resumen en el cuerpo.
- Verificar el mail en Mailpit creando una solicitud.
- Docs: [Webhook](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook/), [Send Email](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.sendemail/).

### T15.4 · Workflow: resultado al solicitante
- Webhook `solicitud-lista` → **Send Email** al solicitante con una tabla: por cada ítem, si se aprobó (y a qué proveedor) o se rechazó (y por qué). Enunciado, paso 8 del ejemplo de Laura.

### T15.5 · Endpoints de recordatorios
- En `compras/ComprasApi` (pública):
  - `List<RecordatorioPendiente> recordatoriosPendientes(Instant ahora)`: gestiones en PENDIENTE o EN_COTIZACION donde `(ultimoRecordatorioEn, o solicitadoEn si nunca hubo) + intervalo <= ahora`. Intervalo: 3 días para ALTA, 21 días para MEDIA y BAJA.
  - `void registrarRecordatorio(Long itemId, Instant cuando)`.
- En `integraciones/web/`:
  - `GET /api/v1/integraciones/recordatorios-pendientes` → `[{ itemId, solicitudId, detalle, urgencia, fechaNecesaria, diasSinResolver, destinatarios: [] }]`. Destinatarios: el email del Encargado asignado o, si no hay, el de todos los Encargados (enunciado: "si nadie lo tomó, les llega a todos"; caso borde 20).
  - `POST /api/v1/integraciones/recordatorios/{itemId}/enviado` → 204. Así el mismo recordatorio no se repite (§7.5).

### T15.6 · Workflow: recordatorios diarios
- **Schedule Trigger** todos los días hábiles a las 9:00 (hora de Argentina, caso borde 19) → **HTTP Request** GET recordatorios pendientes (con el header de API key, T15.7) → por cada ítem, **Send Email** a sus destinatarios → **HTTP Request** POST `.../enviado`.
- Como el backend calcula por fecha, si n8n estuvo caído varios días, al volver manda los atrasados una sola vez (§9).
- Docs: [Schedule Trigger](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.scheduletrigger/), [HTTP Request](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.httprequest/).

### T15.7 · Seguridad de la integración
- **API key:** `N8N_API_KEY` en `.env`. Un `SecurityFilterChain` aparte para `/api/v1/integraciones/**`, que exige el header `X-API-Key` con ese valor en vez del JWT.
- **Firma de los webhooks (opcional):** el backend agrega el header `X-Firma` = HMAC-SHA256 del body con `N8N_WEBHOOK_SECRET`, y n8n lo verifica con el nodo Crypto antes de mandar el mail.

### T15.8 · Exportar los workflows al repo
- Exportar los 3 workflows desde n8n como JSON a `n8n/workflows/` (carpeta nueva en la raíz), sin credenciales.
- Agregar en el README cómo importarlos.

---

## T16 · Reportes (módulo `reportes`)

### T16.1 · Schema y dominio de reportes
- **Migración:** `reportes.hecho_gasto` y `reportes.hecho_resolucion` como en §6.6, más `reportes.evento_procesado`.
- **Dominio:** `HechoGasto` y `HechoResolucion` con los campos de §6.6.

### T16.2 · Alimentar los reportes con eventos
- Listener de `OrdenGenerada` → crea un `HechoGasto`: `ordenId`, `fecha` (día de emisión), `sectorId`, `categoriaId`, `proveedorId`, `moneda`, `importe` = `precioTotal`. Idempotente por `ordenId`.
- Listener de `ItemResuelto` → crea un `HechoResolucion`: `itemId`, `urgencia`, `creadoEn` = `solicitadoEn`, `resueltoEn`, `resultado`.
- Listener de `CategoriaUnificada` → actualiza `categoriaId` de origen a destino en `hecho_gasto`. Es un modelo de lectura propio del módulo, así que acá sí se puede reescribir (caso borde 9: "los reportes siguen la redirección").

### T16.3 · Reporte de gasto
- `GET /api/v1/reportes/gasto?agrupar=sector|categoria|proveedor&desde=&hasta=` → `[{ clave, nombre, moneda, total, cantidadOrdenes }]`.
- Consulta con `@Query` agrupando por la columna elegida **y por moneda**. Pesos y dólares nunca se suman (regla 10).
- Nombres vía `AuthApi.nombreSector` y `CatalogoApi` (categoría y proveedor).
- `desde` y `hasta` obligatorios; `desde` no puede ser posterior a `hasta` (400).

### T16.4 · Tiempo de resolución
- `GET /api/v1/reportes/tiempos-resolucion?desde=&hasta=` → `{ cantidadItems, promedioHoras, porUrgencia: { ALTA: { cantidad, promedioHoras }, MEDIA: ..., BAJA: ... } }`.
- Promedio de `resueltoEn - creadoEn` de los ítems resueltos en el rango.

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
| `/bandeja` | `pages/encargado/Bandeja.jsx` | ENCARGADO |
| `/bandeja/:itemId` | `pages/encargado/GestionItem.jsx` | ENCARGADO |
| `/ordenes` | `pages/encargado/Ordenes.jsx` | ENCARGADO |
| `/catalogo/proveedores` y `/catalogo/categorias` | `pages/encargado/Proveedores.jsx` y `Categorias.jsx` | ENCARGADO |
| `/admin/usuarios` y `/admin/sectores` | `pages/admin/Usuarios.jsx` y `Sectores.jsx` | ADMIN |
| `/reportes` | `pages/reportes/Reportes.jsx` | ENCARGADO, ADMIN |

- `components/Layout.jsx`: barra superior con el nombre del usuario, botón "Salir", y menú con solo las opciones de su rol. Las páginas que todavía no existen muestran "En construcción".
- Docs: [React Router](https://reactrouter.com/home).

### T17.2 · Cliente HTTP
- `api/cliente.js` con una función `api(ruta, { method, body, params })`:
  - Llama a `/api/v1` + ruta con `fetch`.
  - Agrega `Authorization: Bearer <token>` si hay token guardado.
  - Manda y recibe JSON.
  - Si la respuesta es 401: borra el token y redirige a `/login`.
  - Si no es 2xx: lee el `{ codigo, mensaje, campos }` del backend (T03.2) y lanza un `ApiError` con esos datos.
- Otra función `apiArchivo` para subir `FormData` y descargar archivos (la usa T20.3).
- Un archivo por área con una función por endpoint: `api/auth.js`, `api/solicitudes.js`, `api/catalogo.js`, `api/compras.js`, `api/ordenes.js`, `api/admin.js`, `api/reportes.js`. Por ejemplo, `crearSolicitud(datos)` llama a `api('/solicitudes', { method: 'POST', body: datos })`.
- Docs: [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch).

### T17.3 · Login, usuario actual y rutas protegidas
- `auth/AuthContext.jsx`: contexto con `usuario`, `login(email, password)` y `logout()`.
  - `login` llama a `POST /auth/login`, guarda el token en `localStorage` y el usuario en el estado.
  - Al cargar la app, si hay token, llama a `GET /auth/yo`. Si falla, `logout`.
- `auth/RutaProtegida.jsx`: si no hay usuario, redirige a `/login`; si el rol no está entre los permitidos, muestra "No tenés permiso".
- `pages/Login.jsx`: formulario de email y contraseña, mensaje de error del backend, y al entrar redirige según el rol (Encargado a `/bandeja`, Admin a `/admin/usuarios`, el resto a `/solicitudes`).
- Docs: [React: contexto](https://react.dev/learn/managing-state).

### T17.4 · Componentes comunes
- `components/TablaPaginada.jsx`: recibe columnas y una función que trae una página; muestra la tabla y los botones anterior/siguiente.
- `components/MensajeError.jsx`: muestra el `mensaje` de un `ApiError` y, si hay `campos`, la lista de campos inválidos.
- `components/EstadoBadge.jsx`: etiqueta de color por estado (PENDIENTE gris, EN_COTIZACION azul, APROBADO verde, RECHAZADO rojo, CANCELADO tachado, y los de solicitud y orden).
- `components/UrgenciaBadge.jsx`: ALTA rojo, MEDIA amarillo, BAJA gris.
- `utils/formato.js`: fechas en formato `dd/mm/aaaa` e importes con `Intl.NumberFormat('es-AR', { style: 'currency', currency })` para ARS y USD.

---

## T18 · Pantallas del Solicitante

### T18.1 · Nueva solicitud
- `pages/solicitante/NuevaSolicitud.jsx`:
  - Campos generales: urgencia (Baja, Media, Alta), fecha necesaria (input date con `min` = hoy) y observaciones.
  - Lista de ítems, con botón "Agregar ítem" y "Quitar" en cada uno (mínimo 1). Cada ítem tiene: tipo (Producto o Servicio), categoría, cantidad (> 0, admite decimales, caso borde 22) y detalle.
  - `components/AutocompletarCategoria.jsx`: al escribir (esperando 300 ms desde la última tecla) llama a `GET /categorias?tipo=&q=` y muestra sugerencias. Si el usuario elige una, se manda `categoriaId`; si escribe una que no existe, se manda `categoriaNueva` y se muestra "Se creará como categoría nueva".
  - Validar antes de enviar. Al enviar, `POST /solicitudes` con un `Idempotency-Key` generado con `crypto.randomUUID()` al abrir el formulario. Si sale bien, ir al detalle; si hay errores por campo, mostrarlos.
- Docs: [React: formularios](https://react.dev/reference/react-dom/components/form).

### T18.2 · Mis solicitudes
- `pages/solicitante/MisSolicitudes.jsx`: `TablaPaginada` con `GET /solicitudes?mias=true&estado=`. Columnas: número, fecha de creación, urgencia, fecha necesaria, cantidad de ítems y estado. Filtro por estado y botón "Nueva solicitud". Clic en una fila abre el detalle.

### T18.3 · Detalle de una solicitud
- `pages/solicitante/DetalleSolicitud.jsx` con `GET /solicitudes/{id}`:
  - Encabezado con los datos generales y el estado.
  - Tabla de ítems: tipo, categoría, detalle, cantidad, estado, y resultado (proveedor aprobado o motivo del rechazo).
  - En ítems PENDIENTES: botón "Editar" (formulario inline → `PATCH` con la `version`) y "Borrar" (confirmación → `DELETE`).
  - Botón "Cancelar solicitud", visible solo si todos los ítems están PENDIENTES.
  - Si la API devuelve 409 o 422 (el Encargado ya lo tomó), mostrar el mensaje y recargar los datos.

---

## T19 · Pantallas del Encargado

### T19.1 · Bandeja
- `pages/encargado/Bandeja.jsx` con `GET /bandeja?asignadosAMi=&urgencia=`:
  - Columnas: urgencia, fecha necesaria, categoría, detalle, cantidad, estado y Encargado asignado.
  - Filtros: "Solo los míos" y urgencia.
  - Botones según el caso: "Tomar" (PENDIENTE o sin asignar), "Reasignarme" (asignado a otro) y "Abrir" (asignado a mí).
  - Aviso visual en los ítems con `todasVencidas`.
  - Si "Tomar" devuelve 409: mostrar "Otro Encargado lo tomó recién" y recargar.

### T19.2 · Gestión de un ítem
- `pages/encargado/GestionItem.jsx` con `GET /bandeja/{itemId}`:
  - **Datos del ítem** y de la solicitud (urgencia, fecha necesaria, detalle, cantidad).
  - **Proveedores sugeridos** (`GET /bandeja/{itemId}/proveedores-sugeridos`), con un botón "Cotizar" que completa el proveedor en el formulario. Si no hay, botón "Dar de alta proveedor" con un formulario rápido (`POST /proveedores` con la categoría del ítem).
  - **Formulario de cotización:** proveedor, moneda, precio unitario, válida hasta, entrega estimada y observaciones. Muestra el total calculado (precio × cantidad) mientras se escribe.
  - **Cotizaciones cargadas**, separadas por moneda. Las vencidas se ven en gris y no se pueden elegir.
  - **Aprobar:** elegir una cotización vigente y confirmar → `POST aprobar` con `cotizacionId` y `version`.
  - **Rechazar:** ventana con el motivo (obligatorio) → `POST rechazar`.
  - Mostrar los errores 422 del backend (por ejemplo `COTIZACION_VENCIDA`) y recargar.

### T19.3 · Órdenes
- `pages/encargado/Ordenes.jsx` con `GET /ordenes?estado=`: pestañas "Por enviar" (GENERADA) y "Enviadas". Columnas: número, fecha, proveedor, cantidad, moneda y total. Botón "Marcar como enviada" (con confirmación) y, cuando exista T11.5, "Descargar PDF".

### T19.4 · Catálogo
- `pages/encargado/Proveedores.jsx`: tabla con buscador, alta y edición en un formulario (razón social, CUIT, teléfono, email, contacto, categorías con selección múltiple usando el autocompletado de T18.1) y activar/desactivar.
- `pages/encargado/Categorias.jsx`: lista de categorías NUEVAS con botones "Confirmar" (`PATCH` a CONOCIDA) y "Unificar con..." (elegir la categoría destino → `POST unificar`).

---

## T20 · Administración, reportes, adjuntos e historial en el front

### T20.1 · Administración
- `pages/admin/Usuarios.jsx`: tabla con buscador y filtro por rol; formulario de alta (email, nombre, sector, rol, contraseña inicial); edición de nombre, sector, rol y activo; cambio de contraseña. Mostrar el error `ULTIMO_ENCARGADO` si aparece.
- `pages/admin/Sectores.jsx`: lista, alta y edición de nombre.

### T20.2 · Reportes
- `pages/reportes/Reportes.jsx`:
  - Filtros: desde, hasta (por defecto, el mes actual) y agrupar por sector, categoría o proveedor.
  - Dos tablas (ARS y USD) con nombre, total y cantidad de órdenes, y un gráfico de barras por moneda (`npm install recharts`).
  - Tarjetas de tiempo de resolución: promedio general y por urgencia, en días y horas.
- Docs: [Recharts](https://recharts.org/).

### T20.3 · Adjuntos
- `components/Adjuntos.jsx` (recibe si es de ítem o de cotización, y su id):
  - Lista de adjuntos con botón de descarga (con `fetch` y el token; se descarga como blob, porque un link común no manda el token).
  - Input de archivo con `accept="application/pdf,image/png,image/jpeg"`, que controla los 10 MB antes de subir y sube con `FormData`.
- Usarlo en `DetalleSolicitud` (por ítem) y en `GestionItem` (por cotización).
- Docs: [FormData](https://developer.mozilla.org/en-US/docs/Web/API/FormData).

### T20.4 · Historial
- `components/Historial.jsx`: línea de tiempo con `GET /historial?entidad=SOLICITUD&id=&incluirItems=true` (fecha, quién, qué cambió y comentario). Se muestra en `DetalleSolicitud`, desplegable.

---

## T21 · Calidad y prueba completa

### T21.1 · Tests de reglas de dominio
- Cada subtarea que agrega reglas a una entidad trae sus tests unitarios (sin Spring ni base de datos). Como mínimo:
  - Transiciones de `Item`.
  - `Solicitud.recalcularEstado()`.
  - `GestionItem.tomar`, `reasignar`, `aprobar` y `rechazar`.
  - `Cotizacion.estaVigente`.
  - Cálculo del total.
  - Intervalo de recordatorios.

### T21.2 · Tests de endpoints
- Cada controller tiene un test con MockMvc que cubre el caso feliz, una validación (400), un permiso (403) y un error de regla (422 o 409). El usuario se simula con `jwt()` (T04.4).
- Docs: [Testing the Web Layer](https://spring.io/guides/gs/testing-web/), [testing en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html).

### T21.3 · Prueba completa
- Con la app levantada y los datos de prueba, recorrer el ejemplo de Laura del enunciado (pasos 1 a 8), con un navegador por usuario, anotando cada resultado en `Docs/pruebas/prueba-completa.md`.
- Probar los casos borde 1 a 4 (concurrencia) con dos navegadores a la vez, y el 12 y el 13 (todos cancelados, todos rechazados).
- Cada error que aparezca se abre como issue nuevo en el milestone de Cierre (y se agrega a `03-area-dificultad-fechas.md`).

---

## T22 · Documentación, empaquetado y demo

### T22.1 · Actualizar el diseño
- En `Docs/diseno-del-sistema.md`, reflejar las decisiones del principio de este archivo (módulo compartido, eventos nuevos, columnas agregadas, registro de Modulith en vez de outbox manual, adjuntos locales) y cualquier cambio posterior.

### T22.2 · README de instalación
- `README.md` en la raíz: requisitos (Java 25, Docker, Node 22), cómo completar los `.env`, cómo levantar backend, frontend y n8n, cómo importar los workflows, usuarios de prueba y cómo correr los tests.

### T22.3 · Empaquetado para la demo
- `back/Dockerfile` multi-etapa: compilar con `./mvnw package` y correr con una imagen de Java 25.
- `frontend/Dockerfile`: `npm run build` y servir `dist/` con nginx, redirigiendo `/api` al backend.
- `compose.demo.yaml` que levante todo (Postgres, backend, frontend, n8n y Mailpit) con un solo comando.
- Docs: [Dockerfiles en Spring Boot](https://docs.spring.io/spring-boot/4.1.1/reference/packaging/container-images/dockerfiles.html), [build de Vite](https://vite.dev/guide/static-deploy.html).

### T22.4 · Datos y guion de la demo
- Datos de ejemplo cargados (proveedores, categorías, algunas solicitudes en distintos estados), guion de la presentación siguiendo el ejemplo de Laura, y slides.

### T22.5 · Escalado y manejo de fallas
- Revisar las secciones §8 y §9 del diseño y marcar qué se implementó (por ejemplo: reintento de eventos, idempotencia, versión) y qué queda como propuesta.
