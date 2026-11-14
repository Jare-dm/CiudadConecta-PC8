/* CiudadConecta PC8 - navegación definitiva, enlaces por rol y menú móvil. */
(() => {
  'use strict';
  document.addEventListener('DOMContentLoaded', () => {
    const normalizar = ruta => ruta.replace(/\/+$/, '') || '/';
    const actual = normalizar(window.location.pathname);
    const enlaces = [...document.querySelectorAll([
      '.pc8-nav-link[href]',
      '.pc8-nav-cta[href]',
      '.pc8-mobile-link[href]',
      '.pc8-nav-dropdown a[href]'
    ].join(', '))];

    function coincide(destino) {
      if (destino === actual) return 3;
      if (destino === '/mis-reportes' && /^\/reportes\/\d+(?:\/.*)?$/.test(actual)) return 2;
      if (destino === '/invitado' && actual.startsWith('/invitado/')) return 2;
      if (destino === '/gestion' && actual.startsWith('/gestion/')) return 1;
      if (destino === '/admin/usuarios' && actual.startsWith('/admin/usuarios/')) return 2;
      if (destino === '/transparencia' && actual.startsWith('/transparencia/')) return 2;
      return 0;
    }

    const candidatos = enlaces.map(enlace => {
      let destino = '';
      try {
        const url = new URL(enlace.href, window.location.href);
        if (url.origin !== window.location.origin) return null;
        destino = normalizar(url.pathname);
      } catch (_) { return null; }
      return {enlace, destino, prioridad: coincide(destino)};
    }).filter(Boolean);
    const mejor = Math.max(0, ...candidatos.map(e => e.prioridad));
    // Se priorizan las rutas exactas para no marcar Gestión y Estadísticas a la vez.
    candidatos.forEach(({enlace, prioridad}) => {
      if (prioridad > 0 && prioridad === mejor) {
        enlace.setAttribute('aria-current', 'page');
      } else {
        enlace.removeAttribute('aria-current');
      }
    });

    document.querySelectorAll('.pc8-nav-dropdown').forEach(menu => {
      const id = menu.getAttribute('aria-labelledby');
      const boton = id ? document.getElementById(id) : null;
      if (boton && menu.querySelector('[aria-current="page"]')) {
        boton.classList.add('pc8-nav-active-section');
      }
    });

    const contenedor = document.getElementById('pc8-menu');
    if (!contenedor) return;
    contenedor.querySelectorAll('a[href]').forEach(enlace => {
      enlace.addEventListener('click', () => {
        if (window.innerWidth >= 1400 || !window.bootstrap?.Collapse) return;
        const instancia = window.bootstrap.Collapse.getInstance(contenedor);
        if (instancia) instancia.hide();
      });
    });
  });
})();
