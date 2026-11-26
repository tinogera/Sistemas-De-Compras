// Hero con scroll: la imagen queda fija y cambia de paso según cuánto se scrolleó.
// Mismo criterio que el diseño original: el paso activo es el último cuyo borde
// superior ya pasó el 60 % de la altura de la ventana.

(function () {
  const hero = document.querySelector('[data-hero]');
  if (!hero) return;

  const LINEA = 0.6;
  const pasos = Array.from(hero.querySelectorAll('[data-step]'));
  const marcables = Array.from(hero.querySelectorAll('[data-panel], [data-leyenda], [data-ir]'));
  const botones = Array.from(hero.querySelectorAll('[data-ir]'));
  const contador = hero.querySelector('[data-contador]');
  const movimientoReducido = window.matchMedia('(prefers-reduced-motion: reduce)');
  const total = String(pasos.length).padStart(2, '0');

  let activo = -1;
  let raf = 0;

  function indiceDe(el) {
    return Number(el.dataset.panel ?? el.dataset.leyenda ?? el.dataset.ir);
  }

  function pintar(a) {
    if (a === activo) return;
    activo = a;
    marcables.forEach((el) => el.classList.toggle('is-activo', indiceDe(el) === a));
    botones.forEach((b) => {
      if (indiceDe(b) === a) b.setAttribute('aria-current', 'step');
      else b.removeAttribute('aria-current');
    });
    if (contador) contador.textContent = String(a + 1).padStart(2, '0') + ' / ' + total;
  }

  function calcular() {
    raf = 0;
    const linea = window.innerHeight * LINEA;
    let a = 0;
    pasos.forEach((el) => {
      if (el.getBoundingClientRect().top < linea) a = Number(el.dataset.step) || 0;
    });
    pintar(a);
  }

  function alScrollear() {
    if (!raf) raf = requestAnimationFrame(calcular);
  }

  function irAlPaso(i) {
    const el = pasos.find((p) => Number(p.dataset.step) === i);
    if (!el) return;
    const y = el.getBoundingClientRect().top + window.scrollY - window.innerHeight * LINEA + 40;
    window.scrollTo({ top: Math.max(0, y), behavior: movimientoReducido.matches ? 'auto' : 'smooth' });
  }

  botones.forEach((b) => b.addEventListener('click', () => irAlPaso(indiceDe(b))));
  window.addEventListener('scroll', alScrollear, { passive: true });
  window.addEventListener('resize', alScrollear);
  calcular();

  // Menú del celular
  const nav = document.querySelector('[data-nav]');
  const botonMenu = document.querySelector('[data-menu]');
  if (nav && botonMenu) {
    const cerrar = () => {
      nav.classList.remove('is-abierto');
      botonMenu.setAttribute('aria-expanded', 'false');
    };
    botonMenu.addEventListener('click', () => {
      const abierto = nav.classList.toggle('is-abierto');
      botonMenu.setAttribute('aria-expanded', String(abierto));
    });
    nav.querySelectorAll('.nav__links a').forEach((a) => a.addEventListener('click', cerrar));
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrar(); });
  }
})();
