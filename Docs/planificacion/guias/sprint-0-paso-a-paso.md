# Guía paso a paso — Sprint 0 (tareas T01 y T02)

Anexo con el código probado de las subtareas T01.1 a T02.3. Las tareas y sus fechas están en `../02-tareas-especificas.md` y `../03-area-dificultad-fechas.md`.

**Fechas:** lunes 28/09 → domingo 04/10/2026
**Objetivo:** que todo el equipo pueda correr el back, el front y los tests en su máquina, y que exista el esqueleto de módulos sobre el que se construye el resto.
**Hito H0:** cualquier integrante clona el repo, completa sus `.env`, y `./mvnw test` y `npm run dev` funcionan.

Es un sprint liviano a propósito: también es la semana para que todos lean `Docs/enunciado.md` y la sección de Spring Modulith de `Docs/guia/README.md`.

> Todo lo que está en este archivo se probó el 27/09 en una copia del proyecto: con estos pasos, los 3 tests pasan y Modulith detecta los 8 módulos.

---

## Cómo tomar una tarea

No hay roles fijos: cualquiera toma cualquier tarea.

1. Elegir una tarea sin responsable cuyas dependencias estén terminadas (columna "Depende de" en `../03-area-dificultad-fechas.md`).
2. Avisar al grupo y anotarse como responsable en `../03-area-dificultad-fechas.md` (estado 🟨).
3. Crear la rama desde `main` actualizado:
   ```bash
   git switch main
   git pull
   git switch -c feature/T02.1-esqueleto-modulos
   ```
4. Commits con el ID adelante: `T02.1: agrega paquetes de módulos`.
5. Al terminar, merge directo a `main` (sin PR):
   ```bash
   git switch main
   git pull
   git merge <rama>
   ./mvnw test        # desde back/: comprobar que main sigue en verde antes de subir
   git push
   ```
   Cuando está en `main`, marcar ✅.

## Orden de trabajo

```
Día 1-2   T01.1-2 (limpieza)   T01.3 (base de datos)      T02.1 (esqueleto de módulos)
             │                      │                            │
Día 2-4      │                  T02.2 (migraciones Flyway) ◄────┘
             │                      │
Día 3-5      │                  T02.3 (tests con Testcontainers)
             ▼                      ▼
Día 6-7   Verificar H0 en la máquina de cada integrante
```

T01.1-T01.2, T01.3 y T02.1 no dependen entre sí: pueden hacerlas tres personas distintas a la vez.
T02.2 y T02.3 tocan archivos distintos, pero T02.3 necesita las migraciones de T02.2 para que el test de contexto pase. Si las hace la misma persona, mejor.

---

## T01.1 y T01.2 · Limpieza del repo, plantilla de PR y protección de `main`

**Área: repo/Git · Depende de: nada**

### Qué hay que hacer y por qué

El repo tiene archivos que no deberían estar versionados, y un `.gitignore` con reglas equivocadas:

| Qué | Por qué sacarlo o cambiarlo |
|---|---|
| `.idea/` (5 archivos) | Configuración personal de IntelliJ. Cada uno tiene la suya y genera conflictos en los PR. Ya está en `.gitignore`, pero se subió antes de ignorarla. |
| `HELP.md` | Archivo autogenerado por Spring Initializr. Sus links ya están en `Docs/guia/README.md`. |
| `back/resources/` | Carpeta creada por error fuera del árbol de Maven. Maven solo lee `back/src/main/resources/`, así que nada de lo que hay acá se usa. Tiene `aplication.properties` vacío (mal escrito) y una carpeta `db/migration/` vacía. |
| Líneas `mvnw` y `mvnw.cmd` en `.gitignore` | El wrapper de Maven **tiene** que estar en el repo, porque es lo que usa cada integrante para compilar sin instalar Maven. Hoy está subido, pero la regla confunde. |
| Línea `.gitignore` dentro de `.gitignore` | El archivo se ignora a sí mismo. No tiene efecto porque ya está subido, pero es un error. |

### Pasos

1. Crear la rama:
   ```bash
   git switch main && git pull
   git switch -c feature/T01.1-limpieza-repo
   ```
2. Sacar `.idea/` del repo **sin borrarla del disco** (IntelliJ la sigue usando):
   ```bash
   git rm -r --cached .idea
   ```
3. Borrar `HELP.md` y la carpeta sobrante:
   ```bash
   git rm HELP.md
   git rm -r back/resources
   rm -rf back/resources          # borra también las carpetas vacías que git no seguía
   ```
4. Editar `.gitignore` en la raíz del repo:
   - Borrar las líneas `mvnw`, `mvnw.cmd` y `.gitignore`.
   - Dejar el resto como está (`back/target/`, `frontend/node_modules/`, `frontend/dist`, `.idea/`, `*.iml`, `.env`, `CLAUDE.md`, `Claude/`).
5. Crear la plantilla de PR en `.github/pull_request_template.md` (carpeta nueva en la raíz):
   ```markdown
   ## Tarea
   ID: <!-- por ejemplo SOL-02 -->

   ## Qué cambia
   <!-- qué se hizo y en qué archivos -->

   ## Cómo probarlo
   <!-- comandos o pasos para verificarlo -->

   ## Checklist
   - [ ] `./mvnw test` pasa (desde `back/`)
   - [ ] `npm run lint` pasa (desde `frontend/`), si se tocó el front
   - [ ] No hay credenciales en el código
   ```
6. Commit y push:
   ```bash
   git add -A
   git commit -m "T01.1: limpieza del repo y plantilla de PR"
   git push -u origin feature/T01.1-limpieza-repo
   ```
7. Abrir el PR en GitHub y que lo revise otra persona.
8. Proteger `main` en GitHub (solo el dueño del repo, **tinogera**, puede hacerlo): Settings → Branches → Add branch ruleset:
   - Target: rama `main`.
   - Activar "Require a pull request before merging", con 1 aprobación requerida.
   - Activar "Block force pushes".
9. Agregar a los integrantes como colaboradores del repo (Settings → Collaborators) si todavía no lo son.

### Cómo verificar

- `git ls-files | grep -E "^(\.idea|HELP.md|back/resources)"` no devuelve nada.
- Intentar hacer `git push` directo a `main` es rechazado.

### Listo cuando

El repo solo tiene `back/`, `frontend/`, `Docs/`, `.github/`, `.gitignore`, el PR está mergeado y `main` está protegida.

---

## T01.3 y T01.4 · Base de datos y versión de Postgres

**Área: infraestructura · Depende de: nada**

### Qué hay que hacer y por qué

Hoy la app arranca con el Postgres de Docker que levanta Spring solo (`back/compose.yaml`) y con la contraseña de prueba `secret`. Hay que decidir qué base usa el equipo en desarrollo y dejarla configurada.

### Opción A — Postgres con Docker Compose (recomendada)

Es lo que ya está armado. Cada integrante tiene su propia base descartable y no hay que instalar nada salvo Docker.

1. Editar `back/.env` (no se sube a git) y poner valores propios:
   ```properties
   POSTGRES_DB=solicitudes_compra
   POSTGRES_USER=compras_app
   POSTGRES_PASSWORD=<una contraseña propia>
   ```
2. Desde `back/`, arrancar la app:
   ```bash
   ./mvnw spring-boot:run
   ```
   Spring lee `compose.yaml`, levanta el contenedor de Postgres con los datos del `.env` y se conecta solo.
3. En el log tiene que aparecer `Started SolicitudeCompraApplication`.
4. Recomendado: fijar la versión de Postgres en `back/compose.yaml`, cambiando `postgres:latest` por `postgres:17`, para que todos usen la misma y no cambie sola.

> Si ya habías arrancado la app antes con otra contraseña, Postgres conserva los datos viejos del contenedor. Borrarlo con `docker compose down -v` desde `back/` y volver a arrancar.

### Opción B — Postgres instalado en la máquina o en la nube

1. Crear el usuario y la base (ejemplo en Fedora con Postgres instalado):
   ```bash
   sudo -u postgres createuser --pwprompt compras_app
   sudo -u postgres createdb --owner=compras_app solicitudes_compra
   ```
2. Completar `back/.env` con esos datos, más la ubicación de la base:
   ```properties
   POSTGRES_DB=solicitudes_compra
   POSTGRES_USER=compras_app
   POSTGRES_PASSWORD=<la contraseña elegida>
   DB_HOST=localhost
   DB_PORT=5432
   USAR_DOCKER_COMPOSE=false
   ```
3. Para que Spring no levante el Docker, agregar esta línea en `back/src/main/resources/application.properties`:
   ```properties
   spring.docker.compose.enabled=${USAR_DOCKER_COMPOSE:true}
   ```
   Así, quien tenga `USAR_DOCKER_COMPOSE=false` en su `.env` usa su base propia, y el resto sigue con Docker.
4. Agregar `DB_HOST=`, `DB_PORT=` y `USAR_DOCKER_COMPOSE=` vacíos en `back/.env.example`, para que se sepa que existen.
5. Arrancar la app con `./mvnw spring-boot:run` desde `back/` y verificar el log.

### Para los dos casos

- Pasarle a cada integrante los valores de su `.env` por un canal privado (nunca por el repo ni por un issue).
- Anotar en `Claude/contexto/estado.md` qué opción se eligió.

### Listo cuando

La app arranca contra la base elegida en la máquina de al menos dos integrantes.

---

## T02.1 · Esqueleto de módulos

**Área: backend · Depende de: nada**

### Qué hay que hacer y por qué

Spring Modulith trata cada **subpaquete directo** de `com.Sistem.Solicitude_Compra` como un módulo, y controla que los módulos no se usen entre sí por dentro. Hay que crear los 8 paquetes ahora, aunque estén vacíos, para que todo el código nuevo tenga dónde ir y para que un test avise si alguien rompe las reglas.

Los módulos salen de la tabla "Los módulos" del enunciado, más `auth` (usuarios y login) y `auditoria` (historial), que están en el diseño:

| Paquete | Nombre visible | Qué va a tener |
|---|---|---|
| `auth` | Auth | Usuarios, sectores, roles, login |
| `catalogo` | Catálogo | Proveedores, categorías |
| `solicitudes` | Solicitudes | Solicitudes e ítems |
| `compras` | Compras | Bandeja, cotizaciones, aprobar y rechazar |
| `ordenes` | Órdenes | Órdenes de pedido |
| `reportes` | Reportes | Gasto y tiempos de resolución |
| `integraciones` | Integraciones | Conexión con n8n |
| `auditoria` | Auditoría | Historial de cambios |

### Archivos a crear

Todos dentro de `back/src/main/java/com/Sistem/Solicitude_Compra/`.

**1. Un `package-info.java` por módulo** (8 archivos). Por ejemplo, `back/src/main/java/com/Sistem/Solicitude_Compra/solicitudes/package-info.java`:

```java
@org.springframework.modulith.ApplicationModule(displayName = "Solicitudes")
package com.Sistem.Solicitude_Compra.solicitudes;
```

Repetir para cada paquete de la tabla, cambiando el nombre del paquete y el `displayName`. La anotación hace que Modulith reconozca el módulo aunque todavía no tenga clases.

**2. El test de arquitectura** en `back/src/test/java/com/Sistem/Solicitude_Compra/ModularityTests.java`:

```java
package com.Sistem.Solicitude_Compra;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

	ApplicationModules modules = ApplicationModules.of(SolicitudeCompraApplication.class);

	@Test
	void verificaLaEstructuraDeModulos() {
		modules.forEach(System.out::println);
		modules.verify();
	}

	@Test
	void generaLaDocumentacionDeModulos() {
		new Documenter(modules).writeDocumentation();
	}
}
```

- `verify()` falla si un módulo usa clases internas de otro, o si hay dependencias circulares entre módulos.
- `Documenter` genera diagramas de los módulos en `back/target/spring-modulith-docs/` (archivos `.puml` y `.adoc`). Se pueden ver con el plugin PlantUML de IntelliJ.
- No hace falta agregar dependencias: `spring-modulith-starter-test`, que ya está en el `pom.xml`, incluye todo.

### Cómo probar

Desde `back/`:

```bash
./mvnw test -Dtest=ModularityTests
```

Este test no necesita base de datos. En la salida aparece un bloque por módulo (`# Auditoría`, `# Auth`, `# Catálogo`, ...).

### Listo cuando

`ModularityTests` pasa y la salida lista los 8 módulos.

### Regla para todo el proyecto

A partir de acá, cada clase nueva va dentro del paquete de su módulo. Lo que otro módulo puede usar va en la raíz del paquete (por ejemplo `solicitudes/SolicitudCreada.java`); lo interno va en subpaquetes (por ejemplo `solicitudes/internal/`). `ModularityTests` avisa si alguien lo rompe.

---

## T02.2 · Migraciones Flyway iniciales

**Área: backend / base de datos · Depende de: T01.3**

### Qué hay que hacer y por qué

1. El diseño (§6) usa **un schema de PostgreSQL por módulo**. Hay que crearlos.
2. Spring Modulith guarda los eventos entre módulos en una tabla `event_publication`, que hoy no existe. Por eso al cerrar la app aparece `relation "event_publication" does not exist`, y cuando se publique el primer evento va a fallar.
3. Hay que impedir que Hibernate cree o modifique tablas por su cuenta: todas las tablas se crean con migraciones de Flyway, versionadas en el repo.

Flyway corre solo al arrancar la app: busca los archivos en `back/src/main/resources/db/migration/` y aplica, en orden, los que todavía no se aplicaron. Anota cuáles aplicó en la tabla `flyway_schema_history`.

**Regla de oro:** una migración ya mergeada **nunca se edita**. Si hay que cambiar algo, se crea una nueva (`V3__...`). Si se edita una vieja, Flyway detecta que cambió y la app no arranca en las máquinas de los demás.

### Archivos a crear o modificar

**1. `back/src/main/resources/db/migration/V1__schemas_iniciales.sql`** (la carpeta `db/migration` ya existe, vacía):

```sql
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS catalogo;
CREATE SCHEMA IF NOT EXISTS solicitudes;
CREATE SCHEMA IF NOT EXISTS compras;
CREATE SCHEMA IF NOT EXISTS ordenes;
CREATE SCHEMA IF NOT EXISTS reportes;
CREATE SCHEMA IF NOT EXISTS auditoria;
```

`integraciones` no tiene schema porque no guarda datos propios.

**2. `back/src/main/resources/db/migration/V2__event_publication.sql`**. Es el SQL oficial para PostgreSQL de la [documentación de Spring Modulith](https://docs.spring.io/spring-modulith/reference/appendix.html#schemas.postgresql) ("Standard schema"):

```sql
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
```

La tabla va en el schema `public` porque así la busca Modulith.

**3. `back/src/main/resources/application.properties`**: agregar al final:

```properties
# Las tablas solo las crea Flyway. Hibernate solo verifica que las entidades coincidan con la base.
spring.jpa.hibernate.ddl-auto=validate
```

Con `validate`, si alguien crea una entidad JPA y se olvida de la migración, la app no arranca y el error dice qué tabla o columna falta. Es mejor enterarse ahí que en producción.

### Cómo probar

1. Desde `back/`: `./mvnw spring-boot:run`.
2. En el log tienen que aparecer líneas de Flyway como `Migrating schema "public" to version "1 - schemas iniciales"` y `"2 - event publication"`, y después `Started SolicitudeCompraApplication`.
3. Cortar la app con Ctrl+C: ya no aparece el error de `event_publication`.
4. Opcional, para ver la base por dentro: con el contenedor corriendo, `docker exec -it <nombre-del-contenedor> psql -U <POSTGRES_USER> -d <POSTGRES_DB>` y dentro `\dn` (lista los schemas) y `\dt` (lista las tablas de `public`). El nombre del contenedor sale de `docker ps`.

### Listo cuando

La app arranca y se cierra sin errores, y `flyway_schema_history` tiene las versiones 1 y 2.

---

## T02.3 · Tests con base de datos (Testcontainers)

**Área: backend / tests · Depende de: T02.2 (el test de contexto necesita las migraciones)**

### Qué hay que hacer y por qué

`./mvnw test` falla con `Failed to determine a suitable driver class`. El test `SolicitudeCompraApplicationTests` levanta toda la app, y en los tests Spring **no** levanta el Postgres de `compose.yaml`, así que no hay base.

La solución es **Testcontainers**: al correr los tests, levanta un Postgres descartable en Docker, la app se conecta sola (gracias a `@ServiceConnection`), Flyway aplica las migraciones, y al terminar el contenedor se borra. No depende del `.env` de nadie, así que funciona igual en todas las máquinas y en GitHub Actions.

Requisito: Docker instalado y corriendo.

### Archivos a crear o modificar

**1. `back/pom.xml`**: agregar estas tres dependencias dentro de `<dependencies>`, al final, junto a las otras de `<scope>test</scope>`. No llevan `<version>` porque Spring Boot 4.1.1 ya las define (Testcontainers 2.0.5):

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-testcontainers</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>testcontainers-junit-jupiter</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>testcontainers-postgresql</artifactId>
			<scope>test</scope>
		</dependency>
```

> Ojo con los ejemplos de internet: en Testcontainers 2.x los artefactos cambiaron de nombre (`testcontainers-postgresql`, no `postgresql`) y la clase está en `org.testcontainers.postgresql`, no en `org.testcontainers.containers`.

**2. Crear `back/src/test/java/com/Sistem/Solicitude_Compra/TestcontainersConfiguration.java`**:

```java
package com.Sistem.Solicitude_Compra;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
	}
}
```

Si en T01.4 se fijó una versión de Postgres (por ejemplo `postgres:17`), usar la misma acá.

**3. Modificar `back/src/test/java/com/Sistem/Solicitude_Compra/SolicitudeCompraApplicationTests.java`** para que use esa configuración. Agregar el import y la anotación `@Import`:

```java
package com.Sistem.Solicitude_Compra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SolicitudeCompraApplicationTests {

	@Test
	void contextLoads() {
	}

}
```

**Regla para todo el proyecto:** todo test que levante la app o use la base (`@SpringBootTest`, `@DataJpaTest`, `@ApplicationModuleTest`) lleva `@Import(TestcontainersConfiguration.class)`.

### Cómo probar

Desde `back/`:

```bash
./mvnw test
```

- La primera vez tarda unos 3 minutos porque descarga la imagen de Postgres. Las siguientes son mucho más rápidas.
- Resultado esperado: `Tests run: 3, Failures: 0, Errors: 0` (1 test de contexto + 2 de `ModularityTests`) y `BUILD SUCCESS`.

### Alternativa si Testcontainers da problemas

Crear `back/src/test/resources/application.properties` con `spring.docker.compose.skip.in-tests=false`. Así los tests usan el Postgres de `compose.yaml`. Es más lento y depende del `.env` de cada uno, así que usarlo solo si Testcontainers no anda.

### Listo cuando

`./mvnw test` pasa en una máquina con Docker, sin ninguna base creada a mano.

---

## Cierre del sprint (domingo 04/10)

- [ ] Cada integrante clona el repo desde cero, completa `back/.env` y `frontend/.env` (copiando de los `.env.example`) y corre:
  - `./mvnw test` desde `back/` → pasa.
  - `./mvnw spring-boot:run` desde `back/` → arranca.
  - `npm install && npm run dev` desde `frontend/` → abre la página de Vite.
- [ ] Marcar las subtareas terminadas en `../03-area-dificultad-fechas.md` y actualizar `Claude/contexto/estado.md`.
- [ ] Releer las subtareas de S1 en `../02-tareas-especificas.md` y, si alguna necesita más detalle, escribir una guía como esta en `guias/`.
