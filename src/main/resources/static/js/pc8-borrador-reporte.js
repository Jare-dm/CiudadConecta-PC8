
/*
 * CiudadConecta PC8
 * Guardado temporal accesible de reportes
 *
 * No almacena fotos, coordenadas,
 * contraseñas ni información de identidad.
 */

(() => {
    'use strict';

    const CLAVE = 'pc8-borrador-reporte-v1';

    function leerBorrador() {
        try {
            const texto = sessionStorage.getItem(CLAVE);

            if (!texto) return null;

            const datos = JSON.parse(texto);

            if (!datos || typeof datos !== 'object') {
                return null;
            }

            return datos;

        } catch (error) {
            return null;
        }
    }

    function eliminarBorrador() {
        try {
            sessionStorage.removeItem(CLAVE);
        } catch (error) {
            console.warn(
                'No se pudo eliminar el borrador.',
                error
            );
        }
    }

    document.addEventListener('DOMContentLoaded', () => {

        const formulario =
            document.getElementById('form-nuevo-reporte');

        // En otras páginas no hacemos nada.
        if (!formulario) return;

        const categoria =
            document.getElementById('categoria');

        const descripcion =
            document.getElementById('descripcion');

        const ubicacion =
            document.getElementById('ubicacion');

        const aviso =
            document.getElementById('pc8-borrador-estado');

        const botonRecuperar =
            document.getElementById('pc8-recuperar-borrador');

        const botonDescartar =
            document.getElementById('pc8-descartar-borrador');

        if (!categoria || !descripcion || !ubicacion) {
            return;
        }

        function informar(mensaje) {
            if (aviso) {
                aviso.textContent = mensaje;
            }
        }

        function actualizarBotones() {
            const disponible = !!leerBorrador();

            if (botonRecuperar) {
                botonRecuperar.hidden = !disponible;
            }

            if (botonDescartar) {
                botonDescartar.hidden = !disponible;
            }
        }

        function guardarBorrador() {
            const datos = {
                categoria: categoria.value,
                descripcion: descripcion.value,
                ubicacion: ubicacion.value
            };

            const tieneContenido =
                datos.categoria ||
                datos.descripcion.trim() ||
                datos.ubicacion.trim();

            if (!tieneContenido) {
                eliminarBorrador();
                actualizarBotones();
                informar('No hay información guardada.');
                return;
            }

            try {
                sessionStorage.setItem(
                    CLAVE,
                    JSON.stringify(datos)
                );

                actualizarBotones();

                informar(
                    'Borrador temporal guardado en esta pestaña.'
                );

            } catch (error) {
                informar(
                    'No se pudo guardar el borrador. ' +
                    'Revisa la configuración del navegador.'
                );
            }
        }

        let temporizador = null;

        function programarGuardado() {
            clearTimeout(temporizador);

            temporizador = setTimeout(
                guardarBorrador,
                500
            );
        }

        // Guardado automático mientras escribe.
        [categoria, descripcion, ubicacion].forEach(
            campo => {
                campo.addEventListener(
                    'input',
                    programarGuardado
                );

                campo.addEventListener(
                    'change',
                    programarGuardado
                );
            }
        );

        // Recuperación voluntaria del borrador.
        if (botonRecuperar) {
            botonRecuperar.addEventListener('click', () => {

                const datos = leerBorrador();

                if (!datos) {
                    actualizarBotones();
                    informar(
                        'No se encontró ningún borrador.'
                    );
                    return;
                }

                // Si se había elegido una categoría
                // personalizada que ya no existe,
                // no seleccionamos una opción inválida.
                const existeCategoria =
                    [...categoria.options].some(
                        opcion =>
                            opcion.value === datos.categoria
                    );

                if (existeCategoria) {
                    categoria.value = datos.categoria;

                    categoria.dispatchEvent(
                        new Event('change', {
                            bubbles: true
                        })
                    );
                }

                descripcion.value =
                    String(datos.descripcion || '');

                ubicacion.value =
                    String(datos.ubicacion || '');

                // Notificar a los controles existentes
                // para actualizar contadores y resúmenes.
                [descripcion, ubicacion].forEach(campo => {
                    campo.dispatchEvent(
                        new Event('input', {
                            bubbles: true
                        })
                    );

                    campo.dispatchEvent(
                        new Event('change', {
                            bubbles: true
                        })
                    );
                });

                // Conservar actualizado el borrador
                // después de la recuperación.
                clearTimeout(temporizador);
                guardarBorrador();

                informar(
                    'Borrador recuperado. Revisa la ' +
                    'información y selecciona nuevamente ' +
                    'el punto en el mapa.'
                );

                categoria.focus();
            });
        }

        // Descartar borrador sin borrar automáticamente
        // los campos que el usuario esté editando.
        if (botonDescartar) {
            botonDescartar.addEventListener('click', () => {
                clearTimeout(temporizador);
                eliminarBorrador();
                actualizarBotones();

                informar(
                    'Borrador descartado. ' +
                    'Los campos actuales no se han borrado.'
                );
            });
        }

        actualizarBotones();

        if (leerBorrador()) {
            informar(
                'Hay un borrador temporal disponible. ' +
                'Puedes recuperarlo o descartarlo.'
            );
        } else {
            informar(
                'El formulario guardará temporalmente ' +
                'la información mientras escribes.'
            );
        }
    });
})();
