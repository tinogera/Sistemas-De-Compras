# Guía de aprendizaje: plugins y dependencias del proyecto

Links a la documentación oficial de cada dependencia del backend (`back/pom.xml`) y del frontend, ordenados en el orden sugerido para aprenderlos.

Versiones del proyecto: **Spring Boot 4.1.1**, **Spring Modulith 2.1.1**, **Java 25**. Los links de Spring Boot apuntan a la versión exacta 4.1.1. Los de Spring Modulith apuntan a la última versión publicada (puede ser un poco más nueva que la 2.1.1).

---

## 0. Base: Spring Boot y Maven

| Qué | Para qué sirve en el proyecto | Links |
|---|---|---|
| Spring Boot (referencia general) | El framework sobre el que corre todo el backend | [Documentación de referencia](https://docs.spring.io/spring-boot/4.1.1/reference/index.html) |
| Configuración externa | Cómo `application.properties` lee variables del `.env` | [Externalized Configuration](https://docs.spring.io/spring-boot/4.1.1/reference/features/external-config.html) |
| Maven | Compila, testea y empaqueta el proyecto (`./mvnw`) | [Guías de Maven](https://maven.apache.org/guides/index.html) |
| `spring-boot-maven-plugin` | `./mvnw spring-boot:run`, generar el `.jar` o una imagen Docker | [Referencia del plugin](https://docs.spring.io/spring-boot/4.1.1/maven-plugin) · [Crear una imagen OCI](https://docs.spring.io/spring-boot/4.1.1/maven-plugin/build-image.html) |

## 1. API REST: `spring-boot-starter-webmvc`

Sirve para crear los endpoints `/api/...` que consume el frontend.

- [Spring Boot: aplicaciones web servlet](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)
- [Spring Framework: Web MVC (controladores, `@RestController`, `@RequestMapping`)](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- Guía práctica: [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- Guía práctica: [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

## 2. Validación: `spring-boot-starter-validation`

Sirve para validar los datos que llegan a la API (`@NotNull`, `@Size`, `@Valid`, etc.).

- [Spring Boot: Validation](https://docs.spring.io/spring-boot/4.1.1/reference/io/validation.html)
- [Hibernate Validator: referencia completa de anotaciones](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/)
- Guía práctica: [Validating Form Input](https://spring.io/guides/gs/validating-form-input/)

## 3. Lombok

Genera getters, setters, constructores y builders con anotaciones (`@Getter`, `@Builder`, `@RequiredArgsConstructor`...).

- [Lista de features de Lombok](https://projectlombok.org/features/)

## 4. Base de datos

### PostgreSQL + driver `postgresql`

- [Documentación de PostgreSQL](https://www.postgresql.org/docs/current/)
- [Driver JDBC de PostgreSQL](https://jdbc.postgresql.org/documentation/)
- [Imagen oficial de Postgres en Docker Hub](https://hub.docker.com/_/postgres) (la que usa `back/compose.yaml`)

### JPA: `spring-boot-starter-data-jpa`

Sirve para mapear tablas a clases Java (`@Entity`) y consultar con repositorios (`JpaRepository`).

- [Spring Boot: JPA y Spring Data](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
- [Spring Data JPA: referencia (repositorios, queries derivadas, `@Query`)](https://docs.spring.io/spring-data/jpa/reference/)
- Guía práctica: [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### Migraciones: `spring-boot-starter-flyway` + `flyway-database-postgresql`

Crea y modifica las tablas con scripts SQL versionados (`src/main/resources/db/migration/V1__descripcion.sql`).

- [Spring Boot: usar Flyway](https://docs.spring.io/spring-boot/4.1.1/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
- [Documentación oficial de Flyway](https://documentation.red-gate.com/fd)

## 5. Entorno de desarrollo

### `spring-boot-docker-compose`

Al correr la app, levanta solo el Postgres definido en `back/compose.yaml`.

- [Spring Boot: Docker Compose Support](https://docs.spring.io/spring-boot/4.1.1/reference/features/dev-services.html#features.dev-services.docker-compose)
- [Documentación de Docker Compose](https://docs.docker.com/compose/)

### `spring-boot-devtools`

Reinicia la app automáticamente cuando cambias código.

- [Spring Boot: DevTools](https://docs.spring.io/spring-boot/4.1.1/reference/using/devtools.html)

## 6. Arquitectura modular: Spring Modulith

`spring-modulith-starter-core`, `spring-modulith-starter-jpa`, `spring-modulith-runtime`

Organiza el backend en módulos (un subpaquete de `com.Sistem.Solicitude_Compra` por funcionalidad) que se comunican con eventos.

- [Referencia de Spring Modulith (inicio)](https://docs.spring.io/spring-modulith/reference/)
- [Fundamentos: qué es un módulo](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
- [Verificar la estructura de módulos](https://docs.spring.io/spring-modulith/reference/verification.html)
- [Eventos entre módulos (y la tabla `event_publication`)](https://docs.spring.io/spring-modulith/reference/events.html)
- [Soporte en tiempo de ejecución](https://docs.spring.io/spring-modulith/reference/runtime.html)

## 7. Monitoreo: Actuator y observabilidad

`spring-boot-starter-actuator`, `spring-modulith-actuator`, `spring-modulith-observability-api`, `spring-modulith-observability-core`

Exponen endpoints de salud y métricas (`/actuator/health`, etc.) y trazas por módulo.

- [Spring Boot: Actuator](https://docs.spring.io/spring-boot/4.1.1/reference/actuator/index.html)
- [Lista de endpoints de Actuator](https://docs.spring.io/spring-boot/4.1.1/reference/actuator/endpoints.html)
- [Spring Modulith: Actuator y observabilidad](https://docs.spring.io/spring-modulith/reference/production-ready.html)
- Guía práctica: [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

## 8. Tests

`spring-boot-starter-*-test`, `spring-modulith-starter-test`

- [Spring Boot: Testing](https://docs.spring.io/spring-boot/4.1.1/reference/testing/index.html)
- [Testear aplicaciones Spring Boot (`@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`)](https://docs.spring.io/spring-boot/4.1.1/reference/testing/spring-boot-applications.html)
- [Spring Modulith: tests por módulo (`@ApplicationModuleTest`)](https://docs.spring.io/spring-modulith/reference/testing.html)
- Guía práctica: [Testing the Web Layer](https://spring.io/guides/gs/testing-web/)

---

## Frontend (React + Vite)

- [Aprender React](https://react.dev/learn)
- [Vite: variables de entorno y `.env`](https://vite.dev/guide/env-and-mode.html)
- [Vite: proxy del servidor de desarrollo (`/api` → backend)](https://vite.dev/config/server-options.html#server-proxy)
