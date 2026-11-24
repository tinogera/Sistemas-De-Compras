# Graph Report - back  (2026-10-25)

## Corpus Check
- Corpus is ~10,100 words - fits in a single context window. You may not need a graph.

## Summary
- 624 nodes · 1548 edges · 30 communities (11 shown, 19 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 90 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Unit Test Utilities
- Integration Tests & JPA
- Auth API & Security Context
- Auth Repositories & Modulith Tests
- Spring Data Repositories
- Error Handling & Web Layer
- Domain Entities & Types
- Aprobaciones Domain
- Auth Service & Users
- JWT & Security Config
- Error Handler Test Controller
- Check Status Enum
- Docker Compose Postgres
- Spring Boot Application
- EstadoSolicitud Enum
- Item Class
- Maven Project

## God Nodes (most connected - your core abstractions)
1. `Rol` - 41 edges
2. `AuthControllerTest` - 25 edges
3. `UsuarioRepository` - 23 edges
4. `ManejadorErroresTest` - 23 edges
5. `Regla` - 22 edges
6. `Usuario` - 22 edges
7. `AuthApiTest` - 22 edges
8. `AuthApi` - 21 edges
9. `MotorDeReglasTest` - 21 edges
10. `AuthRepositoriesTest` - 21 edges

## Surprising Connections (you probably didn't know these)
- `AprobacionItem` --references--> `Moneda`  [EXTRACTED]
  src/main/java/com/Sistem/Solicitude_Compra/aprobaciones/dominio/AprobacionItem.java → src/main/java/com/Sistem/Solicitude_Compra/compartido/tipos/Moneda.java
- `AprobacionItem` --references--> `TipoItem`  [EXTRACTED]
  src/main/java/com/Sistem/Solicitude_Compra/aprobaciones/dominio/AprobacionItem.java → src/main/java/com/Sistem/Solicitude_Compra/compartido/tipos/TipoItem.java
- `AprobacionItem` --references--> `Urgencia`  [EXTRACTED]
  src/main/java/com/Sistem/Solicitude_Compra/aprobaciones/dominio/AprobacionItem.java → src/main/java/com/Sistem/Solicitude_Compra/compartido/tipos/Urgencia.java
- `PasoItem` --references--> `Rol`  [EXTRACTED]
  src/main/java/com/Sistem/Solicitude_Compra/aprobaciones/dominio/PasoItem.java → src/main/java/com/Sistem/Solicitude_Compra/compartido/tipos/Rol.java
- `Regla` --references--> `ReglaPaso`  [EXTRACTED]
  src/main/java/com/Sistem/Solicitude_Compra/aprobaciones/dominio/Regla.java → src/main/java/com/Sistem/Solicitude_Compra/aprobaciones/dominio/ReglaPaso.java

## Import Cycles
- None detected.

## Communities (30 total, 19 thin omitted)

### Community 0 - "Unit Test Utilities"
Cohesion: 0.05
Nodes (27): OperadorCondicion, MAYOR, MAYOR_IGUAL, MENOR, MENOR_IGUAL, Regla, ReglaCondicion, TipoCondicion (+19 more)

### Community 1 - "Integration Tests & JPA"
Cohesion: 0.06
Nodes (12): EstadoCategoria, CONOCIDA, NUEVA, UNIFICADA, ReglaNegocioException, ReglaRepositoryIntegrationTest, AuthControllerIdentificateTest, BaseDeDatosTest (+4 more)

### Community 2 - "Auth API & Security Context"
Cohesion: 0.06
Nodes (13): AuthApi, UsuarioResumen, AuthController, UsuarioActualResponse, SesionInvalidaException, UsuarioActual, Rol, ADMIN (+5 more)

### Community 3 - "Auth Repositories & Modulith Tests"
Cohesion: 0.06
Nodes (5): AuthRepositoriesTest, AuthControllerTest, CatalogoDominioTest, ManejadorErroresTest, ModularityTests

### Community 4 - "Spring Data Repositories"
Cohesion: 0.06
Nodes (16): SectorRepository, Categoria, Proveedor, CategoriaRepository, ProveedorRepository, TipoItem, PRODUCTO, SERVICIO (+8 more)

### Community 5 - "Error Handling & Web Layer"
Cohesion: 0.08
Nodes (5): EvaluadorMonto, EntradaNoAutenticado, ManejadorAccesoDenegado, ErrorApi, ManejadorErrores

### Community 6 - "Domain Entities & Types"
Cohesion: 0.10
Nodes (4): ReglaPaso, Sector, Usuario, LoginRequest

### Community 7 - "Aprobaciones Domain"
Cohesion: 0.09
Nodes (19): AprobacionItem, EstadoCadena, APROBADA, CANCELADA, EN_CURSO, RECHAZADA, EstadoPaso, APROBADO (+11 more)

### Community 8 - "Auth Service & Users"
Cohesion: 0.08
Nodes (9): UsuarioRepository, LoginService, LoginResponse, UsuarioLogin, ConflictoException, CredencialesInvalidasException, ErrorNegocioException, NoEncontradoException (+1 more)

### Community 12 - "Check Status Enum"
Cohesion: 0.29
Nodes (6): Check, CANCELADO, COMPRADO, EN_COTIZACION, PENDIENTE, RECHAZADO

### Community 13 - "Docker Compose Postgres"
Cohesion: 0.33
Nodes (5): environment, image, ports, services, postgres

## Knowledge Gaps
- **43 isolated node(s):** `com.Sistem:Solicitude-Compra`, `EN_CURSO`, `APROBADA`, `RECHAZADA`, `CANCELADA` (+38 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 148 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **19 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Rol` connect `Auth API & Security Context` to `Unit Test Utilities`, `Integration Tests & JPA`, `Auth Repositories & Modulith Tests`, `Spring Data Repositories`, `Domain Entities & Types`, `Aprobaciones Domain`, `Auth Service & Users`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **What connects `com.Sistem:Solicitude-Compra`, `EN_CURSO`, `APROBADA` to the rest of the system?**
  _43 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Unit Test Utilities` be split into smaller, more focused modules?**
  _Cohesion score 0.050436953807740326 - nodes in this community are weakly interconnected._
- **Why does `ManejadorErrores` connect `Error Handling & Web Layer` to `Integration Tests & JPA`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Should `Integration Tests & JPA` be split into smaller, more focused modules?**
  _Cohesion score 0.06128364389233954 - nodes in this community are weakly interconnected._
- **Why does `AprobacionItem` connect `Aprobaciones Domain` to `Unit Test Utilities`, `Spring Data Repositories`, `Domain Entities & Types`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Should `Auth API & Security Context` be split into smaller, more focused modules?**
  _Cohesion score 0.056535504296698326 - nodes in this community are weakly interconnected._