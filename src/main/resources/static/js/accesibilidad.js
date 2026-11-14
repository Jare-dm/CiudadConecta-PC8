(() => {
  'use strict';

  const escalas = [100, 125, 150, 175, 200];

  function leer(clave, defecto) {
    try { return localStorage.getItem(clave) || defecto; }
    catch (_) { return defecto; }
  }

  function guardar(clave, valor) {
    try { localStorage.setItem(clave, valor); }
    catch (_) {}
  }

  let escala = Number(leer('pc8-escala', '100'));
  if (!escalas.includes(escala)) escala = 100;

  let contraste = leer('pc8-contraste', 'no') === 'si';
  let movimientoReducido = leer('pc8-reducir-movimiento', 'no') === 'si';

  const estado = () => document.getElementById('estado-accesibilidad');

  function aplicar(mensaje) {
    document.documentElement.style.fontSize = escala + '%';
    document.documentElement.classList.toggle('contraste-alto', contraste);
    document.documentElement.classList.toggle('pc8-motion-off', movimientoReducido);

    const botonContraste = document.getElementById('contraste-btn');
    const botonMovimiento = document.getElementById('movimiento-btn');

    if (botonContraste) botonContraste.setAttribute('aria-pressed', String(contraste));
    if (botonMovimiento) botonMovimiento.setAttribute('aria-pressed', String(movimientoReducido));

    // Indicar los límites del ajuste y evitar clics sin efecto.
    const menos = document.getElementById('texto-menos');
    const mas = document.getElementById('texto-mas');
    if (menos) menos.disabled = escala === escalas[0];
    if (mas) mas.disabled = escala === escalas[escalas.length - 1];
    if (mensaje && estado()) estado().textContent = mensaje;

    guardar('pc8-escala', String(escala));
    guardar('pc8-contraste', contraste ? 'si' : 'no');
    guardar('pc8-reducir-movimiento', movimientoReducido ? 'si' : 'no');
  }

  document.addEventListener('DOMContentLoaded', () => {
    aplicar('');

    const activar = (id, fn) => {
      const boton = document.getElementById(id);
      if (boton) boton.addEventListener('click', fn);
    };

    activar('texto-mas', () => {
      escala = escalas[Math.min(escalas.indexOf(escala) + 1, escalas.length - 1)];
      aplicar('Texto al ' + escala + ' por ciento');
    });

    activar('texto-menos', () => {
      escala = escalas[Math.max(escalas.indexOf(escala) - 1, 0)];
      aplicar('Texto al ' + escala + ' por ciento');
    });

    activar('contraste-btn', () => {
      contraste = !contraste;
      aplicar(contraste ? 'Alto contraste activado' : 'Alto contraste desactivado');
    });

    activar('movimiento-btn', () => {
      movimientoReducido = !movimientoReducido;
      aplicar(movimientoReducido ? 'Animaciones reducidas' : 'Animaciones activadas');
    });

    activar('reiniciar-accesibilidad', () => {
      escala = 100;
      contraste = false;
      movimientoReducido = false;
      aplicar('Opciones restablecidas');
    });
  });
})();
