# UsuariosMock · Usuarios (11)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/UsuariosMock.html`.
Estilo: patrón **lista + formulario** de [SectoresMock](SectoresMock.md) (aprobado), con el formulario más largo y scroll propio. Usuario de ejemplo: Pablo Ferreyra, Admin.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/UsuariosMock.html`.
- **Con Claude Code:** "abrí el UsuariosMock" o `! start frontend/MockPaginas/aprobadas/UsuariosMock.html`.
- Atajos: `?editar=3` abre la edición de Lucía Pérez; `?sector=Ventas` abre filtrado (es lo que usaría "Ver usuarios de Ventas" desde Sectores).

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Estado inicial | Lista alfabética (iniciales, nombre, email, sector, rol con color); formulario **Nuevo usuario** a la derecha. Desactivados ocultos |
| Chips de rol / sector / buscar "méndez" o "@empresa" | Filtran; los chips muestran cuántos hay de cada rol con los otros filtros aplicados |
| "Mostrar desactivados" | Aparecen Ricardo Molina y Hernán Ortiz con avatar punteado y "Desactivado" |
| Jorge Medina | "Bloqueado 8 min" en amarillo (5 intentos fallidos) |
| Crear: completar nombre, email, sector, rol y **Generar** contraseña | Barra de fuerza; al crear, ventana **"Listo: X ya puede entrar"** con URL + email + contraseña y **Copiar los datos** |
| Email repetido (`sofia.alvarez@empresa.com`) o mal escrito | Error en vivo debajo del campo |
| Enviar vacío | Errores en todos los obligatorios y foco en el primero |
| Clic en un usuario | **EDITANDO**: email en solo lectura (con la explicación), sector, rol en tarjetas con lo que puede hacer cada uno, sección **Acceso** |
| Lucía Pérez (Encargado) → cambiar a Solicitante o desactivar | Recuadro "Al guardar": sus 4 ítems vuelven a la Bandeja sin Encargado |
| Laura Méndez (Supervisor) → cambiar de rol | "Tiene 2 pasos para decidir: los decide otro Supervisor" |
| Cambiar de sector | "Pasa de X a Y: sus solicitudes nuevas van con el sector nuevo" |
| Pablo Ferreyra (VOS) | Rol e interruptor deshabilitados: "No podés cambiar tu propio rol ni desactivarte" |
| **Restablecer** contraseña | Modal con contraseña generada → ventana con los datos para pasarle; si estaba bloqueado, se destraba |
| MAQUETA → **Queda un solo Encargado** | Abre a Martín Ruiz; al cambiarle el rol o desactivarlo: "No se puede · ULTIMO_ENCARGADO" y Guardar deshabilitado |
| MAQUETA → **Email ya usado** → crear | Aviso `EMAIL_DUPLICADO · 409` arriba del formulario |
| Cambios sin guardar + otra fila / Esc | "¿Descartar los cambios?" |
| Ancho < 1000 px | Lista sola; el formulario se abre a pantalla completa |

## Para implementarlo

**Ruta:** `/admin/usuarios` y `/admin/usuarios/:id` (rol `ADMIN`; se llega desde Inicio o el menú).

| Qué | Endpoint |
|---|---|
| Lista | `GET /api/v1/admin/usuarios?q=&rol=&page=` → `Pagina<UsuarioAdminResponse>` (20 por página, por nombre) |
| Crear | `POST /api/v1/admin/usuarios` `{email, nombre, sectorId, rol, passwordInicial}` · 409 `EMAIL_DUPLICADO` |
| Editar | `PATCH /api/v1/admin/usuarios/{id}` `{nombre?, sectorId?, rol?, activo?}` (los `null` no se tocan) · 422 `ULTIMO_ENCARGADO` |
| Contraseña | `POST /api/v1/admin/usuarios/{id}/password` `{passwordNueva}` → 204 (también destraba el bloqueo) |
| Sectores para el select | `GET /api/v1/admin/sectores` |

**Reglas que la pantalla refleja:** email único sin distinguir mayúsculas y que **no se edita**; contraseña de 8 a 72 caracteres; no hay borrado, solo desactivar; sacarle el rol de Encargado o desactivarlo **desasigna sus ítems** (`EncargadoDesasignado`); no se puede dejar al sistema sin Encargado activo; el bloqueo por intentos (10 min desde el 5.º fallo, 20 desde el 10.º) se destraba restableciendo la contraseña.

**Colores de rol:** Solicitante gris · Supervisor violeta · Encargado azul · Admin verde. El naranja sigue reservado para acciones.

**Componentes nuevos:** `SelectorRol` (tarjetas con descripción), `CampoContrasena` con Generar + fuerza, `ModalCredenciales`, `Consecuencias` (recuadro "Al guardar"). El resto es el patrón de Sectores.

**Falta en backend / a decidir:**
- Filtros por **sector** y **activo** en `GET /admin/usuarios` (hoy solo `q` y `rol`).
- `UsuarioAdminResponse` no trae **bloqueado / bloqueadoHasta**, ni cuántos **ítems** tiene como Encargado o **pasos** pendientes como aprobador (para el recuadro "Al guardar").
- El backend **no impide que un Admin se desactive o se saque el rol a sí mismo**: hoy lo frena solo el front.
- No hay "cambiar la contraseña en el primer ingreso": la que pone el Admin queda. Decidir si se agrega.
- Ningún mail al crear: los datos se pasan a mano (por eso la ventana con Copiar).
- El link "Ver usuarios de X" en Sectores (aprobada) debería apuntar a `/admin/usuarios?sector=X`.

**Solo de la maqueta:** la franja MAQUETA, los atajos por URL, los conteos y las simulaciones.
