
(() => {
    'use strict';

    // ==========================================
    // CIUDADCONECTA PC8
    // LECTOR DE VOZ ACCESIBLE
    // Compatible con PC, tablets y celulares.
    // ==========================================

    const sintetizador =
        ('speechSynthesis' in window &&
         'SpeechSynthesisUtterance' in window)
            ? window.speechSynthesis
            : null;

    let voces = [];
    let fragmentos = [];
    let indiceActual = 0;
    let sesion = 0;

    let leyendo = false;
    let pausado = false;

    // Guarda el último elemento enfocado
    // dentro del contenido principal.
    let ultimoElementoEnfocado = null;

    const obtener = id => document.getElementById(id);

    const controles = {
        leer: obtener('pc8-leer-pagina'),
        desdeFoco: obtener('pc8-leer-desde-foco'),
        pausar: obtener('pc8-lectura-pausar'),
        reanudar: obtener('pc8-lectura-reanudar'),
        detener: obtener('pc8-lectura-detener'),
        voz: obtener('pc8-lectura-voz'),
        velocidad: obtener('pc8-lectura-velocidad'),
        tono: obtener('pc8-lectura-tono'),
        velocidadValor: obtener('pc8-velocidad-valor'),
        tonoValor: obtener('pc8-tono-valor'),
        estado: obtener('pc8-lectura-estado')
    };

    // ==========================================
    // MENSAJES DEL LECTOR
    // ==========================================

    function informar(mensaje) {

        if (controles.estado) {
            controles.estado.textContent = mensaje;
        }

    }

    // ==========================================
    // ACTUALIZAR CONTROLES
    // ==========================================

    function actualizarControles() {

        if (controles.pausar) {
            controles.pausar.disabled =
                !leyendo || pausado;
        }

        if (controles.reanudar) {
            controles.reanudar.disabled =
                !leyendo || !pausado;
        }

        if (controles.detener) {
            controles.detener.disabled =
                !leyendo;
        }

    }

    // ==========================================
    // OBTENER CONTENIDO PRINCIPAL
    // ==========================================

    function obtenerContenido() {

        return document.querySelector('#contenido')
            || document.querySelector('main')
            || document.body;

    }

    // ==========================================
    // LIMPIAR TEXTO
    // ==========================================

    function limpiarTexto(texto) {

        return String(texto || '')
            .replace(/\s+/g, ' ')
            .replace(/\u00a0/g, ' ')
            .trim();

    }

    // ==========================================
    // CONSTRUIR TEXTO PARA LA LECTURA
    // ==========================================

    function construirTexto() {

        const original = obtenerContenido();

        // Trabajar sobre una copia para no modificar
        // el contenido ni los elementos originales.

        const copia = original.cloneNode(true);

        // Obtener formularios originales y sus copias.

        const originales = Array.from(
            original.querySelectorAll(
                'input, textarea, select'
            )
        );

        const copias = Array.from(
            copia.querySelectorAll(
                'input, textarea, select'
            )
        );

        // ======================================
        // FORMULARIOS
        // ======================================

        copias.forEach((campo, i) => {

            const originalCampo = originales[i];

            if (!originalCampo) return;

            const tipo = (
                originalCampo.type || ''
            ).toLowerCase();

            let valor = '';

            // Campos ocultos.

            if (tipo === 'hidden') {

                campo.remove();
                return;

            }

            // Contraseñas: proteger información privada.

            if (tipo === 'password') {

                valor =
                    'Campo de contraseña. Por privacidad, no se lee su contenido.';

            }

            // Casillas y opciones.

            else if (
                tipo === 'checkbox' ||
                tipo === 'radio'
            ) {

                valor = originalCampo.checked
                    ? 'Seleccionado'
                    : 'No seleccionado';

            }

            // Archivos adjuntos.

            else if (tipo === 'file') {

                valor =
                    originalCampo.files &&
                    originalCampo.files.length > 0
                        ? 'Archivo adjunto seleccionado'
                        : 'Sin archivo seleccionado';

            }

            // Listas desplegables.

            else if (campo.tagName === 'SELECT') {

                valor =
                    originalCampo.selectedOptions.length
                        ? originalCampo.selectedOptions[0].textContent
                        : 'Sin selección';

            }

            // Botones de formulario.

            else if (
                tipo === 'submit' ||
                tipo === 'button' ||
                tipo === 'reset' ||
                tipo === 'image'
            ) {

                valor =
                    originalCampo.value ||
                    originalCampo.getAttribute('aria-label') ||
                    'Botón';

            }

            // Campos normales.

            else {

                valor =
                    originalCampo.value || 'Sin completar';

            }

            // Obtener etiqueta del campo.

            const etiqueta =
                originalCampo.getAttribute('aria-label') ||
                originalCampo.name ||
                '';

            const tieneEtiqueta =
                originalCampo.labels &&
                originalCampo.labels.length > 0;

            campo.replaceWith(
                document.createTextNode(
                    etiqueta && !tieneEtiqueta
                        ? etiqueta + ': ' + valor
                        : valor
                )
            );

        });

        // ======================================
        // MAPA LEAFLET
        // ======================================
        // No leer los elementos internos del mapa.
        // El mapa original permanece intacto.

        copia.querySelectorAll(
            '.leaflet-container'
        ).forEach(mapa => {

            const descripcion =
                mapa.getAttribute('aria-label') ||
                mapa.getAttribute('title');

            const texto = document.createElement('p');

            texto.textContent = descripcion
                ? 'Mapa interactivo: ' + descripcion
                : 'Mapa interactivo. Puedes utilizar los controles del mapa para explorar la ubicación del reporte.';

            mapa.replaceWith(texto);

        });

        // ======================================
        // IMÁGENES
        // ======================================

        copia.querySelectorAll('img').forEach(imagen => {

            const alt = imagen.getAttribute('alt');

            if (alt && alt.trim()) {

                imagen.replaceWith(
                    document.createTextNode(
                        'Imagen: ' + alt
                    )
                );

            } else {

                imagen.remove();

            }

        });

        // ======================================
        // ELEMENTOS QUE NO DEBEN LEERSE
        // ======================================

        copia.querySelectorAll(
            'script, style, noscript, template,' +
            'nav, header, footer,' +
            '.pc8-lector,' +
            '.pc8-a11y-toolbar,' +
            '.pc8-footer,' +
            '.pc8-skip-link,' +
            '[hidden], [aria-hidden="true"],' +
            '[inert], .d-none'
        ).forEach(elemento => {

            elemento.remove();

        });

        // ======================================
        // EXTRAER TEXTO
        // ======================================

        const texto =
            copia.innerText || copia.textContent;

        return limpiarTexto(texto);

    }

    // ==========================================
    // DIVIDIR TEXTO EN FRAGMENTOS
    // ==========================================
    // Evita problemas al leer textos extensos.

    function dividirTexto(texto, limite = 200) {

        const oraciones =
            texto.match(/[^.!?]+[.!?]?/g) || [texto];

        const resultado = [];

        let actual = '';

        oraciones.forEach(oracion => {

            oracion = oracion.trim();

            if (!oracion) return;

            // Dividir oraciones demasiado largas.

            if (oracion.length > limite) {

                if (actual) {
                    resultado.push(actual);
                    actual = '';
                }

                const palabras = oracion.split(/\s+/);

                let fragmento = '';

                palabras.forEach(palabra => {

                    if (
                        fragmento.length +
                        palabra.length + 1 > limite
                    ) {

                        if (fragmento) {
                            resultado.push(fragmento);
                        }

                        fragmento = palabra;

                    } else {

                        fragmento +=
                            (fragmento ? ' ' : '') + palabra;

                    }

                });

                if (fragmento) {
                    resultado.push(fragmento);
                }

                return;

            }

            // Agrupar oraciones.

            if (
                actual.length +
                oracion.length + 1 > limite
            ) {

                if (actual) {
                    resultado.push(actual);
                }

                actual = oracion;

            } else {

                actual +=
                    (actual ? ' ' : '') + oracion;

            }

        });

        if (actual) {
            resultado.push(actual);
        }

        return resultado;

    }

    // ==========================================
    // CARGAR VOCES DISPONIBLES
    // ==========================================

    function cargarVoces() {

        if (!sintetizador || !controles.voz) return;

        const disponibles = sintetizador.getVoices();

        if (!disponibles.length) return;

        voces = disponibles;

        const seleccionAnterior = controles.voz.value;

        controles.voz.innerHTML = '';

        // Voz automática.

        const automatico = document.createElement('option');

        automatico.value = '';

        automatico.textContent =
            'Automática (español)';

        controles.voz.appendChild(automatico);

        // Ordenar voces en español primero.

        const ordenadas = voces
            .map((voz, indice) => ({
                voz,
                indice
            }))
            .sort((a, b) => {

                const esA =
                    (a.voz.lang || '').toLowerCase().startsWith('es');

                const esB =
                    (b.voz.lang || '').toLowerCase().startsWith('es');

                return Number(esB) - Number(esA);

            });

        ordenadas.forEach(item => {

            const opcion = document.createElement('option');

            opcion.value = String(item.indice);

            opcion.textContent =
                item.voz.name + ' (' + item.voz.lang + ')';

            controles.voz.appendChild(opcion);

        });

        // Conservar selección anterior.

        if (
            seleccionAnterior !== '' &&
            voces[Number(seleccionAnterior)]
        ) {

            controles.voz.value = seleccionAnterior;

        } else {

            controles.voz.value = '';

        }

    }

    // ==========================================
    // SELECCIONAR VOZ
    // ==========================================

    function obtenerVoz() {

        if (!voces.length) return null;

        const seleccion = controles.voz
            ? controles.voz.value
            : '';

        if (seleccion !== '') {

            return voces[Number(seleccion)] || null;

        }

        // Buscar voz en español automáticamente.

        return voces.find(voz =>
            (voz.lang || '').toLowerCase().startsWith('es')
        ) || null;

    }

    // ==========================================
    // LEER SIGUIENTE FRAGMENTO
    // ==========================================

    function leerFragmento(numeroSesion) {

        if (
            numeroSesion !== sesion ||
            !leyendo ||
            pausado
        ) {
            return;
        }

        // Comprobar si terminó la lectura.

        if (indiceActual >= fragmentos.length) {

            leyendo = false;
            pausado = false;

            actualizarControles();

            informar('Lectura finalizada.');

            return;

        }

        const texto = fragmentos[indiceActual];

        const voz = obtenerVoz();

        const mensaje =
            new SpeechSynthesisUtterance(texto);

        mensaje.lang = voz
            ? voz.lang
            : 'es-ES';

        if (voz) {
            mensaje.voice = voz;
        }

        // Velocidad.

        mensaje.rate = controles.velocidad
            ? Number(controles.velocidad.value)
            : 1;

        // Tono.

        mensaje.pitch = controles.tono
            ? Number(controles.tono.value)
            : 1;

        // Al terminar un fragmento.

        mensaje.onend = () => {

            if (numeroSesion !== sesion) return;

            indiceActual++;

            window.setTimeout(() => {

                leerFragmento(numeroSesion);

            }, 60);

        };

        // Manejo de errores.

        mensaje.onerror = evento => {

            if (numeroSesion !== sesion) return;

            leyendo = false;
            pausado = false;

            actualizarControles();

            if (
                evento.error === 'canceled' ||
                evento.error === 'interrupted'
            ) {
                informar('La lectura fue interrumpida.');
                return;
            }

            informar(
                'No se pudo continuar la lectura. Inténtalo nuevamente.'
            );

        };

        sintetizador.speak(mensaje);

    }

    // ==========================================
    // INICIAR LECTURA
    // ==========================================

    function iniciarLectura(texto) {

        if (!sintetizador) {

            informar(
                'Tu navegador no admite la lectura por voz.'
            );

            return;

        }

        texto = limpiarTexto(texto);

        if (!texto) {

            informar(
                'No se encontró contenido para leer.'
            );

            return;

        }

        // Cancelar lectura anterior.

        sesion++;

        sintetizador.cancel();

        indiceActual = 0;

        fragmentos = dividirTexto(texto);

        leyendo = true;

        pausado = false;

        actualizarControles();

        informar('Iniciando lectura del contenido.');

        leerFragmento(sesion);

    }

    // ==========================================
    // LEER PÁGINA COMPLETA
    // ==========================================

    function leerPagina() {

        const texto = construirTexto();

        iniciarLectura(texto);

    }

    // ==========================================
    // LEER DESDE EL ELEMENTO ENFOCADO
    // ==========================================

    function leerDesdeFoco() {

        const textoCompleto = construirTexto();

        const principal = obtenerContenido();

        const elementoActivo = document.activeElement;

        // Si el foco está en el botón del lector,
        // utilizar el último elemento enfocado
        // dentro del contenido principal.

        const activo =
            elementoActivo instanceof Element &&
            principal.contains(elementoActivo) &&
            !elementoActivo.closest(
                '.pc8-lector, .pc8-a11y-toolbar'
            )
                ? elementoActivo
                : ultimoElementoEnfocado;

        // Comprobar posición.

        if (
            !activo ||
            !principal.contains(activo)
        ) {

            informar(
                'No se identificó una posición dentro del contenido principal. Se iniciará desde el principio.'
            );

            iniciarLectura(textoCompleto);

            return;

        }

        // Identificar el bloque de contenido.

        const bloque = activo.closest(
            'h1, h2, h3, h4, h5, h6,' +
            'p, li, a, button, label,' +
            'td, th, legend, summary,' +
            'input, textarea, select'
        );

        const referencia = bloque || activo;

        let textoReferencia = '';

        // Campos de formulario.

        if (
            referencia.matches(
                'input, textarea, select'
            )
        ) {

            const etiqueta =
                referencia.labels &&
                referencia.labels.length
                    ? referencia.labels[0].textContent
                    : referencia.getAttribute('aria-label');

            textoReferencia =
                etiqueta || referencia.name || '';

        }

        // Otros elementos.

        else {

            textoReferencia =
                referencia.innerText ||
                referencia.textContent ||
                referencia.getAttribute('aria-label') ||
                '';

        }

        textoReferencia = limpiarTexto(textoReferencia);

        // Buscar la posición del elemento en el texto.

        const posicion = textoReferencia
            ? textoCompleto.toLowerCase().indexOf(
                textoReferencia.toLowerCase()
            )
            : -1;

        if (posicion >= 0) {

            iniciarLectura(
                textoCompleto.substring(posicion)
            );

        } else {

            informar(
                'No se pudo identificar la posición actual. Se leerá desde el principio.'
            );

            iniciarLectura(textoCompleto);

        }

    }

    // ==========================================
    // PAUSAR
    // ==========================================

    function pausarLectura() {

        if (!sintetizador || !leyendo || pausado) return;

        sintetizador.pause();

        pausado = true;

        actualizarControles();

        informar('Lectura pausada.');

    }

    // ==========================================
    // REANUDAR
    // ==========================================

    function reanudarLectura() {

        if (!sintetizador || !leyendo || !pausado) return;

        sintetizador.resume();

        pausado = false;

        actualizarControles();

        informar('Continuando lectura.');

    }

    // ==========================================
    // DETENER
    // ==========================================

    function detenerLectura() {

        if (sintetizador) {

            sesion++;

            sintetizador.cancel();

        }

        leyendo = false;

        pausado = false;

        fragmentos = [];

        indiceActual = 0;

        actualizarControles();

        informar('Lectura detenida.');

    }

    // ==========================================
    // CONFIGURAR VELOCIDAD
    // ==========================================

    function actualizarVelocidad() {

        if (!controles.velocidad) return;

        const valor = Number(
            controles.velocidad.value
        );

        if (controles.velocidadValor) {

            controles.velocidadValor.textContent =
                valor.toFixed(1) + '×';

        }

    }

    // ==========================================
    // CONFIGURAR TONO
    // ==========================================

    function actualizarTono() {

        if (!controles.tono) return;

        const valor = Number(
            controles.tono.value
        );

        if (controles.tonoValor) {

            controles.tonoValor.textContent =
                valor.toFixed(1);

        }

    }

    // ==========================================
    // RECORDAR ÚLTIMO ELEMENTO ENFOCADO
    // ==========================================

    function registrarElementoEnfocado(evento) {

        const elemento = evento.target;

        if (!(elemento instanceof Element)) return;

        // Ignorar los controles del lector
        // y la barra de accesibilidad.

        if (
            elemento.closest(
                '.pc8-lector, .pc8-a11y-toolbar,' +
                'nav, header, footer'
            )
        ) {
            return;
        }

        const principal = obtenerContenido();

        if (principal.contains(elemento)) {

            ultimoElementoEnfocado = elemento;

        }

    }

    // ==========================================
    // INICIALIZACIÓN
    // ==========================================

    function iniciar() {

        // Comprobar disponibilidad del navegador.

        if (!sintetizador) {

            informar(
                'La lectura por voz no está disponible en este navegador.'
            );

            [
                controles.leer,
                controles.desdeFoco,
                controles.pausar,
                controles.reanudar,
                controles.detener,
                controles.voz,
                controles.velocidad,
                controles.tono
            ].forEach(elemento => {

                if (elemento) {
                    elemento.disabled = true;
                }

            });

            return;

        }

        // ======================================
        // RECORDAR EL ELEMENTO ENFOCADO
        // ======================================

        document.addEventListener(
            'focusin',
            registrarElementoEnfocado,
            true
        );

        // ======================================
        // CARGAR VOCES
        // ======================================

        cargarVoces();

        sintetizador.addEventListener(
            'voiceschanged',
            cargarVoces
        );

        // ======================================
        // BOTONES
        // ======================================

        controles.leer?.addEventListener(
            'click',
            leerPagina
        );

        controles.desdeFoco?.addEventListener(
            'click',
            leerDesdeFoco
        );

        controles.pausar?.addEventListener(
            'click',
            pausarLectura
        );

        controles.reanudar?.addEventListener(
            'click',
            reanudarLectura
        );

        controles.detener?.addEventListener(
            'click',
            detenerLectura
        );

        // ======================================
        // VELOCIDAD
        // ======================================

        controles.velocidad?.addEventListener(
            'input',
            actualizarVelocidad
        );

        // ======================================
        // TONO
        // ======================================

        controles.tono?.addEventListener(
            'input',
            actualizarTono
        );

        // ======================================
        // VALORES INICIALES
        // ======================================

        actualizarVelocidad();

        actualizarTono();

        actualizarControles();

        informar(
            'Lector de voz disponible. Puedes escuchar el contenido de la página.'
        );

    }

    // ==========================================
    // EJECUTAR CUANDO EL DOCUMENTO ESTÉ LISTO
    // ==========================================

    if (document.readyState === 'loading') {

        document.addEventListener(
            'DOMContentLoaded',
            iniciar
        );

    } else {

        iniciar();

    }

})();
