# LoginMock · Login (01)

Estado: **APROBADA** (2026-11-25; foto con parallax agregada y aprobada 2026-11-26). Mock: `frontend/MockPaginas/aprobadas/LoginMock.html`.
Estilo: tokens y componentes de [LayoutMock](LayoutMock.md); resplandor naranja de [HomeMock](HomeMock.md).

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/LoginMock.html`.
- **Con Claude Code:** "abrí el LoginMock" o `! start frontend/MockPaginas/aprobadas/LoginMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Ingresar con `martin@empresa.com` / `compras123` | Botón "Ingresando…" ~1 s y aviso verde "Entrando como Encargado… te llevamos a Bandeja" |
| Probar `sofia@`, `laura@`, `pablo@empresa.com` (misma contraseña) | Cambia el rol; todos van a Inicio |
| `baja@empresa.com` con cualquier contraseña | Error "Tu usuario está desactivado" |
| Contraseña incorrecta | Error "Email o contraseña incorrectos", se borra la contraseña y vuelve el foco |
| Enviar vacío o con `martin@empresa` | Errores debajo de cada campo, borde rojo, foco en el primero con error |
| "Mostrar" en la contraseña | Muestra / oculta el texto |
| Escribir la contraseña con Bloq Mayús | Aviso "Tenés activado Bloq Mayús" |
| Botones de la franja MAQUETA | Saltan directo a cada estado: inválidos, cargando, credenciales, desactivado, sin conexión, sesión vencida |
| Mover el mouse por la página | La foto de la izquierda se corre al revés del cursor, el brillo naranja lo sigue y las 2 tarjetitas se mueven más (profundidad) |
| Ancho < 960 px | La foto pasa a franja arriba del login |
| Ancho 390 px (F12 → Ctrl+Shift+M) | Franja de foto chica sin tarjetitas, tarjeta a lo ancho, resplandor más chico |

## Para implementarlo

**Ruta:** `/login` (pública). Si ya hay sesión → redirigir a Inicio.

**Endpoint:** `POST /api/v1/auth/login` con `{ email, contrasena }` → token; después `GET /api/v1/auth/identificate` para saber rol y nombre.

**Sin registro:** no hay "Crear cuenta". Las cuentas las crea el Admin desde Usuarios (pantalla 11), que asigna rol y sector; el backend no tiene endpoint de registro. El pie de la tarjeta indica pedírsela a un administrador.

**Destino después del login:** todos los roles van a **Inicio** (`/inicio`, [InicioMock](InicioMock.md)), que muestra los accesos de cada rol. Cambio aprobado el 2026-11-26; antes iban a Bandeja / Usuarios / Mis solicitudes.

**Errores** (el backend manda `{ codigo, mensaje }`; mostrar el `mensaje` en el aviso de arriba del formulario):

| Caso | Aviso | Qué hace la pantalla |
|---|---|---|
| Credenciales inválidas (401) | rojo | borra la contraseña y pone el foco ahí |
| Usuario desactivado | rojo | nada más |
| Sin conexión / timeout | amarillo | permite reintentar |
| Sesión vencida (redirigido desde otra página con `?expirada=1`) | amarillo | muestra el aviso al cargar |

**Validación del front antes de enviar:** email obligatorio y con formato válido; contraseña obligatoria.
Mientras envía: botón con spinner y campos deshabilitados (evita doble envío).

**Componentes reutilizados:** `Aviso`, botón `acento` (es la única acción de la pantalla), campos de LayoutMock.
**Nuevo:** `CampoContrasena` (mostrar/ocultar + aviso Bloq Mayús).

**Panel de foto (izquierda):** grilla de 2 columnas (`1.1fr 1fr`); en < 960 px una columna y la foto como franja arriba.
Componente `FotoParallax`: un listener `pointermove` en `window` escribe `--mx`/`--my` (de -1 a 1, con `requestAnimationFrame`)
y cada capa usa `translate3d(calc(var(--mx) * Npx), …)` con N distinto: foto -22 px, tarjetitas 34 y 52 px; brillo naranja con `soft-light`.
No se mueve con `prefers-reduced-motion` ni en táctil (`hover: none`). Todo el panel es decorativo (`aria-hidden`).
La foto hoy viene de Unsplash (`photo-1553413077-190dd305871c`): bajarla a `src/assets` (≈1600 px, WebP) para no depender de internet.

**Accesibilidad:** `autocomplete="username"` y `"current-password"`, errores asociados con `aria-describedby`,
aviso con `role="alert"`, foco visible.

**Solo de la maqueta:** la franja MAQUETA y los usuarios de prueba.
