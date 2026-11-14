
(() => {
  'use strict';

  document.addEventListener('DOMContentLoaded', () => {
    const formulario = document.getElementById(
      'form-nuevo-reporte'
    );

    if (!formulario) return;

    const campos = [
      {
        id: 'categoria',
        mensaje: 'Selecciona un tipo de problema.'
      },
      {
        id: 'descripcion',
        mensaje:
          'Describe el problema con al menos 12 caracteres.'
      },
      {
        id: 'ubicacion',
        mensaje:
          'Escribe una dirección o referencia de al menos 8 caracteres.'
      },
      {
        id: 'latitud',
        mensaje:
          'Introduce una latitud válida entre -90 y 90.'
      },
      {
        id: 'longitud',
        mensaje:
          'Introduce una longitud válida entre -180 y 180.'
      }
    ];

    campos.forEach(({ id, mensaje }) => {
      const campo = document.getElementById(id);

      if (!campo) return;

      const aviso = document.createElement('p');

      aviso.id = `${id}-error-accesible`;
      aviso.className = 'pc8-error-campo';
      aviso.hidden = true;

      campo.insertAdjacentElement('afterend', aviso);

      const descripcionAnterior =
        campo.getAttribute('aria-describedby') || '';

      campo.setAttribute(
        'aria-describedby',
        `${descripcionAnterior} ${aviso.id}`.trim()
      );

      function revisar() {
        const esCoordenada =
          id === 'latitud' || id === 'longitud';

        const latitud = document.getElementById('latitud');
        const longitud = document.getElementById('longitud');

        const ambasVacias =
          esCoordenada &&
          !latitud.value.trim() &&
          !longitud.value.trim();

        // El usuario todavía puede seleccionar el punto
        // utilizando el mapa.
        if (ambasVacias) {
          aviso.hidden = true;
          campo.removeAttribute('aria-invalid');
          return;
        }

        const categoriaPendiente =
          id === 'categoria' &&
          campo.value === 'CREAR';

        const coordenadaFaltante =
          esCoordenada &&
          !campo.value.trim();

        const tieneError =
          !campo.checkValidity() ||
          categoriaPendiente ||
          coordenadaFaltante;

        aviso.textContent = categoriaPendiente
          ? 'Envía tu propuesta de categoría y elige una categoría disponible para continuar.'
          : coordenadaFaltante
            ? 'Completa esta coordenada para indicar la ubicación.'
            : mensaje;

        aviso.hidden = !tieneError;

        if (tieneError) {
          campo.setAttribute('aria-invalid', 'true');
        } else {
          campo.removeAttribute('aria-invalid');
        }
      }

      campo.addEventListener('blur', revisar);
      campo.addEventListener('change', revisar);

      campo.addEventListener('input', () => {
        if (!aviso.hidden) {
          revisar();
        }
      });
    });
  });
})();
