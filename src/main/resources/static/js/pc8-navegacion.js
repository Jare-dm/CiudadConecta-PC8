(() => {
  'use strict';
  document.addEventListener('DOMContentLoaded', () => {
    const actual = window.location.pathname.replace(/\/$/, '') || '/';
    document.querySelectorAll('.pc8-nav-link[href], .pc8-nav-cta[href]').forEach((enlace) => {
      const destino = new URL(enlace.href, window.location.origin).pathname.replace(/\/$/, '') || '/';
      const exacto = actual === destino;
      const esDetalle = destino === '/mis-reportes' && /^\/reportes\/\d+$/.test(actual);
      const esSeccion = ['/gestion', '/admin/usuarios'].includes(destino) && actual.startsWith(destino + '/');
      if (exacto || esDetalle || esSeccion) enlace.setAttribute('aria-current', 'page');
    });
  });
})();
