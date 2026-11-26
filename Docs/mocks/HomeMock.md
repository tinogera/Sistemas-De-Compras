# HomeMock · Home (landing)

Estado: **APROBADA** (2026-11-25), base visual de todos los mocks.
Mock: `frontend/MockPaginas/aprobadas/HomeMock/HomeMock.html`.

A diferencia del resto, este mock son varios archivos:

| Archivo | Qué es |
|---|---|
| `HomeMock.html` | La página: nav, hero y los 4 pasos |
| `css/comun.css` | Estructura (mobile-first, escritorio desde 900 px) |
| `css/oscuro.css` | Colores del tema oscuro, resplandor naranja y layout de escritorio |
| `js/hero.js` | Imagen fija que cambia con el scroll, barritas clickeables, menú del celular |

## Cómo verlo

Ver la guía general [README.md](README.md). Resumen:

- **Sin Claude Code:** doble clic en `frontend/MockPaginas/aprobadas/HomeMock/HomeMock.html`.
- **Con Claude Code:** "abrí el HomeMock" o `! start frontend/MockPaginas/aprobadas/HomeMock/HomeMock.html`.

### Qué probar

| Qué hacer | Qué tenés que ver |
|---|---|
| Scrollear | La imagen queda fija y cambia con fundido entre los 4 pasos (01 Cargás tu pedido → 04 Te avisamos) |
| Clic en las barritas bajo la imagen | Scrollea hasta ese paso; la barrita activa es más larga y el contador dice `0N / 04` |
| Mirar los bordes de la imagen | Resplandor naranja ovalado detrás, que asoma un poco por los bordes |
| Ancho < 900 px (F12 → Ctrl+Shift+M) | Versión celular: texto centrado, botones a lo ancho, pasos como secciones debajo de la imagen fija |
| Botón ≡ en celular | Abre el menú; Esc lo cierra |
| Sistema con "reducir movimiento" activado | Sin animaciones (halos, fundidos y scroll suave se apagan) |

## Para implementarlo

- Las imágenes son placeholders ("IMAGEN GENÉRICA"). Para poner fotos, reemplazá el contenido de cada `.panel`
  (`data-panel="0..3"`) por `<img src="…" alt="…" style="width:100%;height:100%;object-fit:cover">` y quitá la `.etiqueta`.
- La lógica de `hero.js` (paso activo = el último cuyo borde superior pasó el 60 % de la ventana) se puede pasar
  a un hook `usePasoActivo(refs)` con un listener de scroll + `requestAnimationFrame`.
- "Ingresar" lleva al Login (`LoginMock`).
