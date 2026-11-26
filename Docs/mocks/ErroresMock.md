# ErroresMock · Páginas de error (15)

Estado: **APROBADA** (2026-11-26). Mock: `frontend/MockPaginas/aprobadas/ErroresMock.html`.
Estilo: textos de la sección 05 de [LayoutMock](LayoutMock.md) (aprobada), pasados a página completa; tarjeta con resplandor naranja como [LoginMock](LoginMock.md); barra superior del rol que está navegando.

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/ErroresMock.html`.
- **Con Claude Code:** "abrí el ErroresMock" o `! start frontend/MockPaginas/aprobadas/ErroresMock.html`.
- Atajos: `?pag=403|404|404r|obra&rol=SOLICITANTE|ENCARGADO|ADMIN`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| **403 Sin permiso** como Solicitante | Candado naranja, "No tenés permiso para ver esto", **qué sección** pidió y **para quién es** ("La Bandeja es para el equipo de Compras"), la ruta, **Ir al inicio** + Volver atrás, y "Lo que sí podés hacer" con los atajos de su rol |
| 403 como Encargado / Admin | Cambia la sección pedida (Reglas / gestión de ítems), el menú y los atajos |
| **404 Página** | Brújula, "Esta página no existe", la ruta mal escrita, inicio del rol + atajos, y a quién avisar si el link vino del sistema |
| **404 Solicitud inexistente** | "No existe la solicitud #9999": para cuando el backend responde `NO_ENCONTRADO` sobre un recurso; sin atajos, con el botón a la lista que corresponde |
| **En construcción** | "Esta pantalla todavía no está disponible", qué va a tener (ejemplo: notificaciones) y adónde seguir mientras tanto |
| Cualquier página | El número grande (403 / 404) va de fondo, muy tenue; al cargar, el foco va al título para que el lector de pantalla anuncie qué pasó |
| Ancho 390 px | Tarjeta a lo ancho, botones apilados |

## Para implementarlo

**Rutas:** `/sin-permiso`, `*` (cualquier ruta que no existe → 404) y `EnConstruccion` como componente para las rutas que todavía no están.

| Cuándo | Qué mostrar |
|---|---|
| El rol no tiene la ruta (guard del router) | 403 con la sección pedida y para qué rol es |
| El backend responde **403** `SIN_PERMISO` al cargar una pantalla | 403 (lo mismo) |
| Ruta inexistente | 404 Página |
| El backend responde **404** `NO_ENCONTRADO` al cargar un recurso (`/solicitudes/:id`, `/bandeja/:id`, `/ordenes/:id`) | 404 del recurso con el `mensaje` del backend |
| Pantalla planificada sin hacer | En construcción |
| **401** / sesión vencida | No es esta página: vuelve al Login con el aviso "Tu sesión venció" (ya está en LoginMock) |
| Error 5xx o sin conexión | Tampoco: se usa el estado de error dentro de cada pantalla (con Reintentar) que ya tienen todas |

**Botón principal:** "Ir al inicio" (`/inicio`, [InicioMock](InicioMock.md)), el mismo destino que después del login. Los atajos de "Lo que sí podés hacer" siguen siendo los del rol.

**Componentes:** `PaginaEstado` (ícono, chip, título, texto, ruta, acciones, atajos, código) con tres configuraciones; reusa la barra superior del layout. El título recibe el foco al montar (`tabindex="-1"`).

**A decidir:** el texto exacto de "qué va a tener" en En construcción depende de qué pantalla quede pendiente al lanzar; en la maqueta es un ejemplo.

**Solo de la maqueta:** la franja MAQUETA y los atajos por URL.
