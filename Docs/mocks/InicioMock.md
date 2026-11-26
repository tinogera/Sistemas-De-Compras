# InicioMock · Inicio (16)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/InicioMock.html`.
Pantalla nueva (no estaba en el brief): bienvenida con el nombre y accesos a todo lo que puede hacer cada rol. Estilo de las pantallas aprobadas; el naranja marca **una sola** tarjeta, la acción principal del rol.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/InicioMock.html`.
- **Con Claude Code:** "abrí el InicioMock" o `! start frontend/MockPaginas/aprobadas/InicioMock.html`.
- Atajo: `?rol=SOLICITANTE|APROBADOR|SUPERVISOR|ENCARGADO|ADMIN`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial (Encargado) | Fecha, **"Te damos la bienvenida, Martín."**, "Buen día. Tenés 5 sin tomar, 4 sin enviar y 4 categorías por revisar." con chips que llevan a cada pantalla |
| Tarjetas | Compactas y sin descripción (va como `title` al pasar el mouse): ícono, nombre, contador y flecha. Las de cada grupo van **en una sola fila que ocupa todo el ancho** (4 en Compras, 2 en Tus pedidos). **Bandeja** marcada en naranja, del mismo tamaño |
| Barra superior | **Inicio** primero (marcado), y después las mismas secciones que las tarjetas, con sus contadores |
| Cambiar de rol en MAQUETA | Solicitante: Nueva solicitud (destacada) + Mis solicitudes · Solicitante que aprueba por sector y Supervisor: Aprobaciones (destacada) + sus pedidos · Admin: Usuarios (destacada), Sectores, Reglas, Reportes y Aprobaciones (solo consulta) |
| MAQUETA → **Todo al día** | "No tenés nada pendiente. Todo al día." y sin contadores |
| Pasar el mouse por una tarjeta | Sube apenas y la flecha avanza |
| Ancho < 640 px | Tarjetas de a dos por fila, sin flecha; el menú pasa al botón ≡ |

## Para implementarlo

**Ruta:** `/inicio` para todos los roles (`/` es la Home pública).

| Dato | De dónde sale |
|---|---|
| Nombre, rol, sector | `GET /api/v1/auth/identificate` (el que ya se usa después del login) |
| Aprobaciones para decidir | `GET /api/v1/aprobaciones/resumen` → `{esAprobador, paraDecidir}` (también decide si se muestra Aprobaciones a un Solicitante) |
| Bandeja sin tomar | `GET /api/v1/bandeja?asignadosAMi=false&size=1` → `totalElements` de los `PENDIENTE` (hoy no filtra por estado: ver abajo) |
| Órdenes sin enviar | `GET /api/v1/ordenes?estado=GENERADA&size=1` → `totalElements` |
| Categorías por revisar | `GET /api/v1/categorias?estado=NUEVA&size=1` → `totalElements` |
| Mis solicitudes con novedades | No existe (ver abajo) |

**Accesos por rol** (los mismos que el menú):

| Rol | Destacada | Resto |
|---|---|---|
| Solicitante | Nueva solicitud | Mis solicitudes |
| Solicitante con `esAprobador` / Supervisor | Aprobaciones | Nueva solicitud · Mis solicitudes |
| Encargado | Bandeja | Órdenes · Catálogo · Reportes · Nueva solicitud · Mis solicitudes |
| Admin | Usuarios | Sectores · Reglas · Reportes · Aprobaciones (consulta) |

**Componentes:** `Saludo` (fecha, nombre de pila, resumen de pendientes con chips), `TarjetaAcceso` (ícono, título, contador; variante principal). Toda la tarjeta es el link. Cada grupo es una grilla de `N` columnas iguales (`N` = cantidad de tarjetas), 2 columnas en celular.

**Cambios aplicados en pantallas aprobadas (2026-11-26):**
- **Login (01):** después de entrar, ir a **Inicio** en lugar de Mis solicitudes / Bandeja / Usuarios. El aviso verde diría "te llevamos al inicio".
- **Barra superior de todas las pantallas:** sumar **Inicio** como primer ítem (y que el logo también lleve a Inicio).
- **Páginas de error (15):** el botón principal pasaría a "Ir al inicio".

**Falta en backend / a decidir:**
- Un **endpoint de resumen** que devuelva todos los contadores en un solo llamado (hoy son 3 o 4 pedidos).
- "Mis solicitudes **con novedades**" no existe: haría falta marcar qué cambió desde la última vez que el usuario las vio. Mientras tanto, la tarjeta va sin contador.
- La Bandeja no filtra solo por "sin tomar" (`PENDIENTE`) para contar.

**Solo de la maqueta:** la franja MAQUETA, los contadores fijos y los destinos (toast).
