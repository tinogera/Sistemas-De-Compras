# Mocks de pantallas del frontend

Los mocks son páginas HTML de muestra para ver cómo va a quedar cada pantalla **antes** de implementarla en React.
No se conectan al backend: usan datos de ejemplo y tienen controles de "MAQUETA" para ver los distintos estados
(cargando, vacío, error, cada rol, etc.). Después el equipo los implementa en `frontend/src`.

## Dónde está cada cosa

| Qué | Dónde |
|---|---|
| Mocks en revisión | `frontend/MockPaginas/NombreMock.html` |
| Mocks aprobados (congelados) | `frontend/MockPaginas/aprobadas/NombreMock.html` |
| Estado de cada pantalla | `frontend/MockPaginas/PROGRESO.md` |
| Documentación de cada mock | `Docs/mocks/NombreMock.md` (esta carpeta) |
| Brief de diseño (fuente de verdad) | `Docs/brief-diseno-front.md` |

Cada mock es **un solo archivo HTML autocontenido** (estilos y JS adentro), salvo `HomeMock`, que es una carpeta con su HTML, CSS y JS. No hace falta instalar nada ni compilar.
Solo usa internet para cargar las fuentes (Public Sans y JetBrains Mono) desde Google Fonts.

## Mocks disponibles

| Mock | Pantalla | Estado | Documentación |
|---|---|---|---|
| `aprobadas/HomeMock/HomeMock.html` | Home (landing) | APROBADA | [HomeMock.md](HomeMock.md) |
| `aprobadas/LayoutMock.html` | Componentes comunes + layout | APROBADA | [LayoutMock.md](LayoutMock.md) |
| `aprobadas/LoginMock.html` | Login | APROBADA | [LoginMock.md](LoginMock.md) |
| `aprobadas/MisSolicitudesMock.html` | Mis solicitudes | APROBADA | [MisSolicitudesMock.md](MisSolicitudesMock.md) |
| `aprobadas/DetalleSolicitudMock.html` | Detalle de solicitud | APROBADA | [DetalleSolicitudMock.md](DetalleSolicitudMock.md) |
| `aprobadas/AprobacionesMock.html` | Aprobaciones | APROBADA | [AprobacionesMock.md](AprobacionesMock.md) |
| `aprobadas/DetalleAprobacionMock.html` | Detalle de aprobación | APROBADA | [DetalleAprobacionMock.md](DetalleAprobacionMock.md) |
| `aprobadas/NuevaSolicitudMock.html` | Nueva solicitud | APROBADA | [NuevaSolicitudMock.md](NuevaSolicitudMock.md) |
| `aprobadas/BandejaMock.html` | Bandeja | APROBADA | [BandejaMock.md](BandejaMock.md) |
| `aprobadas/GestionItemMock.html` | Gestión de un ítem | APROBADA | [GestionItemMock.md](GestionItemMock.md) |
| `aprobadas/OrdenesMock.html` | Órdenes | APROBADA | [OrdenesMock.md](OrdenesMock.md) |
| `aprobadas/SectoresMock.html` | Sectores | APROBADA | [SectoresMock.md](SectoresMock.md) |
| `aprobadas/UsuariosMock.html` | Usuarios | APROBADA | [UsuariosMock.md](UsuariosMock.md) |
| `aprobadas/CatalogoMock.html` | Catálogo | APROBADA | [CatalogoMock.md](CatalogoMock.md) |
| `aprobadas/ReglasAprobacionMock.html` | Reglas de aprobación | APROBADA | [ReglasAprobacionMock.md](ReglasAprobacionMock.md) |
| `aprobadas/ReportesMock.html` | Reportes | APROBADA | [ReportesMock.md](ReportesMock.md) |
| `aprobadas/ErroresMock.html` | Páginas de error | APROBADA | [ErroresMock.md](ErroresMock.md) |
| `aprobadas/InicioMock.html` | Inicio | APROBADA | [InicioMock.md](InicioMock.md) |

(La lista completa con las pendientes está en `frontend/MockPaginas/PROGRESO.md`.)

---

## Opción A · Sin Claude Code

### A1. Abrirlo directo (lo más simple)

1. Abrí la carpeta `frontend/MockPaginas/` (o `aprobadas/`) en el Explorador de archivos.
2. Doble clic en el `.html`. Se abre en tu navegador.

Desde una terminal:

```powershell
# PowerShell (Windows)
start frontend\MockPaginas\aprobadas\LayoutMock.html
```

```bash
# Git Bash / macOS / Linux
start frontend/MockPaginas/aprobadas/LayoutMock.html   # Git Bash
open  frontend/MockPaginas/aprobadas/LayoutMock.html   # macOS
xdg-open frontend/MockPaginas/aprobadas/LayoutMock.html # Linux
```

> En Git Bash usá barras normales `/`. Con `\` Bash las toma como escape y no encuentra el archivo.

### A2. Con un servidor local (para verlo desde el celular o compartirlo en la red)

Desde la raíz del repo, con cualquiera de estos:

```bash
npx serve frontend/MockPaginas          # http://localhost:3000
python -m http.server 8080 -d frontend/MockPaginas   # http://localhost:8080
```

Abrí `http://localhost:3000/aprobadas/LayoutMock.html`.

**Desde el celular** (misma red Wi-Fi):
1. Buscá la IP de tu PC: `ipconfig` → "Dirección IPv4" (ej. `192.168.0.15`).
2. En el celular: `http://192.168.0.15:3000/aprobadas/LayoutMock.html`.
3. Si no carga, aceptá el aviso del Firewall de Windows la primera vez.

### A3. Con VS Code

Instalá la extensión **Live Server**, clic derecho en el `.html` → *Open with Live Server*. Se recarga solo cuando cambia el archivo.

### Ver cómo queda en celular o tablet sin un celular

En Chrome o Edge: **F12** → **Ctrl+Shift+M** (modo dispositivo) → elegí un modelo (iPhone 12, iPad) o escribí el ancho (390, 768).

---

## Opción B · Con Claude Code

### B1. Pedírselo en lenguaje natural

En el chat de Claude Code, por ejemplo:

- "abrí el LayoutMock"
- "abrí todos los mocks aprobados"
- "levantá un servidor para ver los mocks desde el celular"

Claude abre el archivo en tu navegador o levanta el servidor y te pasa la URL.

### B2. Correrlo vos con `!`

El prefijo `!` ejecuta el comando en la sesión de Claude Code (usa Bash, así que van barras `/`):

```
! start frontend/MockPaginas/aprobadas/LayoutMock.html
! npx serve frontend/MockPaginas
```

### B3. Pedirle una captura

"sacale captura al LayoutMock en celular y escritorio": Claude abre el mock en Chrome sin ventana, saca capturas
a 390 px y 1280 px y te las muestra en el chat. Sirve para revisar sin salir de la terminal.

### B4. Revisar y aprobar

El flujo de trabajo con Claude Code está en `frontend/MockPaginas/PROGRESO.md`:

- **"me gusta" / "ok"** → el mock pasa a `aprobadas/`, se marca APROBADA y queda congelado.
- **"no me gusta"** → Claude cambia solo lo que le digas, o te ofrece alternativas.
- **"dale"** → Claude genera la próxima pantalla pendiente, con su `.md` en esta carpeta.

---

## Qué tener en cuenta al implementar

- Lo marcado como **MAQUETA** (selector de rol, botones de estado) no va a la app.
- Los colores, fuentes y componentes salen de `LayoutMock`: ver [LayoutMock.md](LayoutMock.md).
- Cada `NombreMock.md` dice qué endpoint usa la pantalla y qué estados tiene que manejar.
