
/*
 * CiudadConecta PC8
 * Formulario de reportes en tres pasos.
 * Detección preventiva de posibles duplicados.
 * Moderación municipal de categorías personalizadas.
 * Mapa OpenStreetMap delimitado a Chorrillos.
 */

(() => {
    'use strict';

    const form = document.getElementById('form-nuevo-reporte');
    if (!form) return;

    // ==========================================
    // ELEMENTOS DEL FORMULARIO
    // ==========================================

    const categoria = document.getElementById('categoria');
    const panel = document.getElementById('categoria-panel');
    const nombre = document.getElementById('categoria-nombre');
    const avisoCategoria = document.getElementById('categoria-respuesta');
    const btnCategoria = document.getElementById('guardar-categoria');

    const descripcion = document.getElementById('descripcion');
    const contador = document.getElementById('pc8-descripcion-contador');
    const ubicacion = document.getElementById('ubicacion');

    const lat = document.getElementById('latitud');
    const lon = document.getElementById('longitud');
    const avisoMapa = document.getElementById('mapa-ayuda');
    const btnUbicacion = document.getElementById('mi-ubicacion');

    const foto = document.getElementById('foto');
    const fotoMensaje = document.getElementById('pc8-foto-mensaje');
    const fotoVista = document.getElementById('pc8-foto-vista');

    const pasos = [...document.querySelectorAll('.pc8-form-step')];
    const indicadores = [...document.querySelectorAll('[data-indicador]')];

    const progreso = document.getElementById('pc8-paso-estado');
    const botonAnterior = document.getElementById('pc8-anterior');
    const botonSiguiente = document.getElementById('pc8-siguiente');
    const botonEnviar = document.getElementById('pc8-enviar');

    const mensaje = document.getElementById('pc8-form-mensaje');
    const detallesCoordenadas =
        document.getElementById('pc8-coordenadas-detalles');

    // ==========================================
    // VARIABLES GENERALES
    // ==========================================

    let pasoActual = 0;
    let mapa = null;
    let marcador = null;
    let geo = null;
    let limitesChorrillos = null;
    let vistaFotoUrl = null;

    // Control de consultas de posibles duplicados.
    let solicitudDuplicados = 0;
    let consultaDuplicadosPendiente = false;

    // ==========================================
    // MENSAJES
    // ==========================================

    function mostrarMensaje(texto) {
        mensaje.textContent = texto;
        mensaje.hidden = !texto;
    }

    function enfocarCampo(campo) {
        if (!campo) return;
        campo.focus({ preventScroll: false });
    }

    // ==========================================
    // NAVEGACIÓN ENTRE LOS TRES PASOS
    // ==========================================

    function irPaso(numero, enfocar = true) {

        // Ignorar consultas antiguas de duplicados.
        solicitudDuplicados++;
        consultaDuplicadosPendiente = false;

        pasoActual = numero;

        pasos.forEach((p, i) => {
            p.hidden = i !== numero;
        });

        indicadores.forEach((item, i) => {

            if (i === numero) {
                item.setAttribute('aria-current', 'step');
            } else {
                item.removeAttribute('aria-current');
            }

            item.classList.toggle(
                'pc8-paso-completado',
                i < numero
            );
        });

        const nombresPasos = [
            'Información del problema',
            'Ubicación en Chorrillos',
            'Evidencia y revisión'
        ];

        progreso.textContent =
            `Paso ${numero + 1} de 3: ${nombresPasos[numero]}`;

        botonAnterior.hidden = numero === 0;
        botonSiguiente.hidden = numero === 2;
        botonEnviar.hidden = numero !== 2;

        botonSiguiente.textContent =
            numero === 0
                ? 'Siguiente: ubicación →'
                : 'Siguiente: revisión →';

        mostrarMensaje('');

        if (numero === 1) {
            encuadrarChorrillos();
        }

        if (numero === 2) {

            actualizarResumen();

            // Verificar posibles reportes duplicados.
            void comprobarDuplicados();
        }

        if (enfocar) {

            enfocarCampo(
                pasos[numero].querySelector('h2')
            );

            pasos[numero].scrollIntoView({
                block: 'start',
                behavior: 'auto'
            });
        }
    }

    // ==========================================
    // VALIDACIÓN DE CAMPOS
    // ==========================================

    function comprobarCampo(campo) {

        if (!campo.checkValidity()) {

            campo.reportValidity();
            enfocarCampo(campo);

            return false;
        }

        return true;
    }

    function comprobarPaso(numero) {

        // PASO 1: INFORMACIÓN DEL PROBLEMA

        if (numero === 0) {

            if (categoria.value === 'CREAR') {

                panel.hidden = false;

                mostrarMensaje(
                    'Debes enviar la categoría nueva a revisión ' +
                    'y seleccionar una categoría disponible ' +
                    'antes de continuar.'
                );

                nombre.focus();
                return false;
            }

            return comprobarCampo(categoria) &&
                   comprobarCampo(descripcion);
        }

        // PASO 2: UBICACIÓN EN CHORRILLOS

        if (numero === 1) {

            if (!comprobarCampo(ubicacion)) {
                return false;
            }

            if (
                !lat.value ||
                !lon.value ||
                !lat.checkValidity() ||
                !lon.checkValidity()
            ) {

                detallesCoordenadas.open = true;

                mostrarMensaje(
                    'Selecciona una ubicación dentro de Chorrillos ' +
                    'o introduce coordenadas válidas.'
                );

                avisoMapa.textContent =
                    'Faltan coordenadas válidas para el reporte.';

                (lat.value ? lon : lat).focus();

                return false;
            }

            if (
                geo &&
                !dentro(
                    Number(lat.value),
                    Number(lon.value)
                )
            ) {

                detallesCoordenadas.open = true;

                mostrarMensaje(
                    'La ubicación está fuera de los límites de Chorrillos.'
                );

                lat.focus();
                return false;
            }

            // Spring Boot validará también la ubicación.
            return true;
        }

        // PASO 3: EVIDENCIA

        return comprobarFoto();
    }

    // ==========================================
    // RESUMEN DEL REPORTE
    // ==========================================

    function actualizarResumen() {

        const opt = categoria.selectedOptions[0];

        document.getElementById(
            'pc8-resumen-categoria'
        ).textContent =
            opt && categoria.value !== 'CREAR'
                ? opt.textContent.trim()
                : 'Sin categoría';

        document.getElementById(
            'pc8-resumen-descripcion'
        ).textContent =
            descripcion.value.trim() || 'Sin descripción';

        document.getElementById(
            'pc8-resumen-ubicacion'
        ).textContent =
            ubicacion.value.trim() || 'Sin ubicación';

        document.getElementById(
            'pc8-resumen-coordenadas'
        ).textContent =
            lat.value && lon.value
                ? `${lat.value}, ${lon.value}`
                : 'Sin coordenadas';

        document.getElementById(
            'pc8-resumen-foto'
        ).textContent =
            foto.files.length
                ? foto.files[0].name
                : 'Sin fotografía adjunta';
    }

    // ==========================================
    // DETECCIÓN DE POSIBLES DUPLICADOS
    // ==========================================

    const cuerpoRevision =
        pasos[2].querySelector('.pc8-panel-cuerpo');

    const resumenRevision =
        cuerpoRevision?.querySelector('.pc8-resumen');

    const avisoDuplicados =
        document.createElement('div');

    avisoDuplicados.id = 'pc8-aviso-duplicados';

    avisoDuplicados.className =
        'alert alert-warning mb-4';

    avisoDuplicados.setAttribute('role', 'status');
    avisoDuplicados.setAttribute('aria-live', 'polite');

    avisoDuplicados.hidden = true;

    if (cuerpoRevision) {

        cuerpoRevision.insertBefore(
            avisoDuplicados,
            resumenRevision || null
        );
    }

    // MOSTRAR ADVERTENCIA

    function informarDuplicados(texto, tipo = 'warning') {

        avisoDuplicados.className =
            `alert alert-${tipo} mb-4`;

        avisoDuplicados.textContent = texto;
        avisoDuplicados.hidden = !texto;
    }

    // CONSULTAR SPRING BOOT

    async function comprobarDuplicados() {

        const idSolicitud = ++solicitudDuplicados;

        const a = Number(lat.value);
        const b = Number(lon.value);

        if (
            !categoria.value ||
            categoria.value === 'CREAR' ||
            !lat.value ||
            !lon.value ||
            !Number.isFinite(a) ||
            !Number.isFinite(b)
        ) {

            informarDuplicados('');
            return;
        }

        consultaDuplicadosPendiente = true;

        botonEnviar.disabled = true;
        botonEnviar.textContent =
            'Buscando reportes similares…';

        informarDuplicados(
            'Consultando si ya existen incidencias similares cerca…',
            'info'
        );

        try {

            const parametros = new URLSearchParams({
                categoriaOpcion: categoria.value,
                latitud: lat.value,
                longitud: lon.value
            });

            const respuesta = await fetch(
                `/api/reportes/posibles-duplicados?${parametros.toString()}`,
                {
                    method: 'GET',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/json'
                    },
                    cache: 'no-store'
                }
            );

            if (!respuesta.ok) {

                throw new Error(
                    'No se pudo realizar la consulta.'
                );
            }

            const datos = await respuesta.json();
            const cantidad = Number(datos.cantidad);

            if (
                !Number.isInteger(cantidad) ||
                cantidad < 0
            ) {

                throw new Error(
                    'Respuesta de consulta no válida.'
                );
            }

            // Ignorar respuestas de consultas anteriores.

            if (
                idSolicitud !== solicitudDuplicados ||
                pasoActual !== 2
            ) {
                return;
            }

            if (cantidad > 0) {

                informarDuplicados(
                    `⚠ Posible duplicado: existen ${cantidad} ` +
                    `${cantidad === 1 ? 'reporte' : 'reportes'} ` +
                    'de esta categoría, registrados en los ' +
                    'últimos 30 días, a 150 metros o menos ' +
                    'y todavía en proceso. ' +
                    'Verifica si se trata del mismo problema. ' +
                    'Si es una incidencia distinta, ' +
                    'puedes enviar tu reporte.',
                    'warning'
                );

            } else {

                informarDuplicados('');
            }

        } catch (error) {

            // No bloquear el reporte si falla la consulta.

            if (
                idSolicitud === solicitudDuplicados &&
                pasoActual === 2
            ) {

                informarDuplicados(
                    'No se pudo comprobar si existen reportes ' +
                    'similares en este momento. Puedes continuar; ' +
                    'el personal municipal revisará la incidencia.',
                    'secondary'
                );
            }

        } finally {

            if (
                idSolicitud === solicitudDuplicados &&
                pasoActual === 2
            ) {

                consultaDuplicadosPendiente = false;

                botonEnviar.disabled = false;
                botonEnviar.textContent = 'Enviar reporte';
            }
        }
    }

    // ==========================================
    // BOTONES SIGUIENTE, ANTERIOR Y ENVIAR
    // ==========================================

    form.noValidate = true;

    document.getElementById('pc8-pasos').hidden = false;

    botonSiguiente.hidden = false;

    const pasoError = pasos.findIndex(p =>
        [...p.querySelectorAll('.text-danger')]
            .some(el => el.textContent.trim().length > 0)
    );

    irPaso(
        pasoError >= 0 ? pasoError : 0,
        false
    );

    // BOTÓN SIGUIENTE

    botonSiguiente.addEventListener('click', () => {

        if (!comprobarPaso(pasoActual)) {
            return;
        }

        irPaso(pasoActual + 1);
    });

    // BOTÓN ANTERIOR

    botonAnterior.addEventListener('click', () => {

        irPaso(pasoActual - 1);
    });

    // ENVIAR REPORTE

    form.addEventListener('submit', event => {

        if (consultaDuplicadosPendiente) {

            event.preventDefault();

            mostrarMensaje(
                'Espera a que termine la comprobación ' +
                'de reportes similares.'
            );

            return;
        }

        if (pasoActual !== 2) {

            event.preventDefault();
            return;
        }

        // VALIDAR PROBLEMA

        if (
            categoria.value === 'CREAR' ||
            !categoria.checkValidity() ||
            !descripcion.checkValidity()
        ) {

            event.preventDefault();

            irPaso(0);
            comprobarPaso(0);

            return;
        }

        // VALIDAR UBICACIÓN

        if (
            !ubicacion.checkValidity() ||
            !lat.value ||
            !lon.value ||
            !lat.checkValidity() ||
            !lon.checkValidity() ||
            (
                geo &&
                !dentro(
                    Number(lat.value),
                    Number(lon.value)
                )
            )
        ) {

            event.preventDefault();

            irPaso(1);
            comprobarPaso(1);

            return;
        }

        // VALIDAR FOTOGRAFÍA

        if (!comprobarFoto()) {

            event.preventDefault();
            return;
        }

        botonEnviar.disabled = true;
        botonEnviar.textContent = 'Enviando reporte…';
    });

    // ==========================================
    // CONTADOR DE CARACTERES
    // ==========================================

    function contar() {

        contador.textContent =
            `${descripcion.value.length} / 1000`;
    }

    descripcion.addEventListener('input', contar);

    contar();

    // ==========================================
    // CATEGORÍAS PERSONALIZADAS
    // CON MODERACIÓN MUNICIPAL
    // ==========================================

    // Este aviso permanece visible después
    // de cerrar el panel de crear categoría.

    const avisoPropuesta =
        document.createElement('div');

    avisoPropuesta.id =
        'pc8-aviso-categoria-pendiente';

    avisoPropuesta.className =
        'alert alert-info mt-3';

    avisoPropuesta.setAttribute('role', 'status');
    avisoPropuesta.setAttribute('aria-live', 'polite');

    avisoPropuesta.hidden = true;

    panel.insertAdjacentElement(
        'afterend',
        avisoPropuesta
    );

    // MOSTRAR U OCULTAR EL PANEL

    categoria.addEventListener('change', () => {

        panel.hidden = categoria.value !== 'CREAR';

        if (!panel.hidden) {
            nombre.focus();
        }

        avisoCategoria.textContent = '';

        avisoPropuesta.textContent = '';
        avisoPropuesta.hidden = true;
    });

    panel.hidden = categoria.value !== 'CREAR';

    // GUARDAR PROPUESTA DE CATEGORÍA

    btnCategoria.addEventListener('click', async event => {

        event.preventDefault();

        const valor = nombre.value
            .trim()
            .replace(/\s+/g, ' ');

        if (
            valor.length < 3 ||
            valor.length > 65
        ) {

            avisoCategoria.textContent =
                'Escribe entre 3 y 65 caracteres.';

            nombre.focus();
            return;
        }

        btnCategoria.disabled = true;

        avisoCategoria.textContent =
            'Guardando categoría…';

        try {

            const datos = new URLSearchParams({
                nombre: valor
            });

            // CONSERVAR CSRF DE SPRING SECURITY

            datos.append(
                panel.dataset.csrfName,
                panel.dataset.csrf
            );

            const resp = await fetch(
                panel.dataset.url,
                {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type':
                            'application/x-www-form-urlencoded;charset=UTF-8',
                        'Accept': 'application/json'
                    },
                    body: datos
                }
            );

            const contenido = await resp.json();

            if (!resp.ok) {

                throw new Error(
                    contenido.error ||
                    'No se pudo guardar la categoría'
                );
            }

            // ==================================
            // NUEVO COMPORTAMIENTO
            // ==================================
            //
            // La categoría se guarda PENDIENTE.
            // NO se añade al selector.
            // NO se puede usar antes de aprobarse.

            categoria.value = '';

            panel.hidden = true;
            nombre.value = '';
            avisoCategoria.textContent = '';

            avisoPropuesta.textContent =
                `La categoría «${contenido.nombre}» fue enviada ` +
                'a revisión municipal. Todavía no está disponible ' +
                'para registrar reportes. Selecciona una categoría ' +
                'existente para continuar.';

            avisoPropuesta.hidden = false;

            categoria.focus();

        } catch (error) {

            avisoCategoria.textContent =
                error.message ||
                'No se pudo guardar la categoría.';

        } finally {

            btnCategoria.disabled = false;
        }
    });

    // ==========================================
    // FOTOGRAFÍAS Y VISTA PREVIA
    // ==========================================

    function comprobarFoto() {

        const archivo = foto.files[0];

        if (!archivo) {
            return true;
        }

        if (
            !['image/jpeg', 'image/png'].includes(
                archivo.type
            ) ||
            archivo.size > 3_000_000
        ) {

            fotoMensaje.textContent =
                'Selecciona una imagen JPG o PNG de hasta 3 MB.';

            foto.focus();
            return false;
        }

        return true;
    }

    // VISTA PREVIA DE FOTOGRAFÍAS

    foto.addEventListener('change', () => {

        if (vistaFotoUrl) {
            URL.revokeObjectURL(vistaFotoUrl);
        }

        vistaFotoUrl = null;

        fotoVista.hidden = true;
        fotoMensaje.textContent = '';

        if (!foto.files.length) {
            return;
        }

        if (!comprobarFoto()) {
            return;
        }

        vistaFotoUrl =
            URL.createObjectURL(foto.files[0]);

        fotoVista.src = vistaFotoUrl;
        fotoVista.hidden = false;

        fotoMensaje.textContent =
            'Fotografía lista para adjuntar: ' +
            foto.files[0].name;

        actualizarResumen();
    });

    // LIBERAR RECURSOS DE LA FOTOGRAFÍA

    window.addEventListener('pagehide', () => {

        if (vistaFotoUrl) {
            URL.revokeObjectURL(vistaFotoUrl);
        }
    });

    // ==========================================
    // VALIDACIÓN GEOGRÁFICA DE CHORRILLOS
    // ==========================================

    const dentroAnillo = (anillo, x, y) => {

        let hallado = false;

        for (
            let i = 0, j = anillo.length - 1;
            i < anillo.length;
            j = i++
        ) {

            const [xi, yi] = anillo[i];
            const [xj, yj] = anillo[j];

            const cruz =
                (xj - xi) * (y - yi) -
                (yj - yi) * (x - xi);

            // Puntos ubicados sobre el borde.

            if (
                Math.abs(cruz) < 1e-10 &&
                x >= Math.min(xi, xj) - 1e-10 &&
                x <= Math.max(xi, xj) + 1e-10 &&
                y >= Math.min(yi, yj) - 1e-10 &&
                y <= Math.max(yi, yj) + 1e-10
            ) {

                return true;
            }

            if (
                (yi > y) !== (yj > y) &&
                x < (xj - xi) * (y - yi) /
                    (yj - yi) + xi
            ) {

                hallado = !hallado;
            }
        }

        return hallado;
    };

    const enPoligono = (anillos, x, y) =>
        anillos.length > 0 &&
        dentroAnillo(anillos[0], x, y) &&
        !anillos.slice(1).some(
            a => dentroAnillo(a, x, y)
        );

    function dentro(a, b) {

        if (!geo) {
            return false;
        }

        return geo.features.some(f => {

            const g = f.geometry;

            return g.type === 'Polygon'
                ? enPoligono(g.coordinates, b, a)
                : g.type === 'MultiPolygon'
                    ? g.coordinates.some(
                        p => enPoligono(p, b, a)
                    )
                    : false;
        });
    }

    // ==========================================
    // SELECCIÓN DE UBICACIONES
    // ==========================================

    function marcar(a, b, enfocar) {

        if (!dentro(a, b)) {

            avisoMapa.textContent =
                'La ubicación está fuera de Chorrillos. ' +
                'Selecciona otro punto dentro del contorno.';

            lat.value = '';
            lon.value = '';

            if (marcador) {
                mapa.removeLayer(marcador);
                marcador = null;
            }

            return;
        }

        lat.value = a.toFixed(6);
        lon.value = b.toFixed(6);

        if (marcador) {
            mapa.removeLayer(marcador);
        }

        marcador = L.marker([a, b])
            .addTo(mapa);

        if (enfocar) {
            mapa.setView([a, b], 16);
        }

        avisoMapa.textContent =
            'Ubicación seleccionada correctamente dentro de Chorrillos.';
    }

    function revisarManual() {

        if (!lat.value || !lon.value) {

            if (marcador && mapa) {

                mapa.removeLayer(marcador);
                marcador = null;
            }

            return;
        }

        if (geo && mapa) {

            marcar(
                Number(lat.value),
                Number(lon.value),
                true
            );
        }
    }

    lat.addEventListener('change', revisarManual);
    lon.addEventListener('change', revisarManual);

    // ==========================================
    // ENCUADRAR EL MAPA SOBRE CHORRILLOS
    // ==========================================

    function encuadrarChorrillos() {

        if (
            !mapa ||
            !limitesChorrillos ||
            pasoActual !== 1
        ) {
            return;
        }

        requestAnimationFrame(() => {

            mapa.invalidateSize({
                pan: false
            });

            const encuadre =
                limitesChorrillos.pad(0.04);

            mapa.setMinZoom(0);

            mapa.fitBounds(
                encuadre,
                {
                    padding: [16, 16],
                    animate: false,
                    maxZoom: 15
                }
            );

            // Conservar zoom útil sobre Chorrillos.

            mapa.setMinZoom(
                Math.max(
                    0,
                    mapa.getZoom() - 0.25
                )
            );

            mapa.panInsideBounds(
                limitesChorrillos.pad(0.04),
                {
                    animate: false
                }
            );
        });
    }

    // ==========================================
    // INICIALIZACIÓN DEL MAPA LEAFLET
    // ==========================================

    btnUbicacion.disabled = true;

    if (typeof L === 'undefined') {

        avisoMapa.textContent =
            'No se pudo cargar el mapa. ' +
            'Puedes introducir coordenadas manuales, ' +
            'que serán comprobadas al enviarlas.';

        return;
    }

    (async () => {

        try {

            // OBTENER CONTORNO DE CHORRILLOS

            const respuesta = await fetch(
                '/api/distritos/chorrillos',
                {
                    credentials: 'same-origin'
                }
            );

            if (!respuesta.ok) {

                throw new Error(
                    'No se pudo consultar el límite de Chorrillos.'
                );
            }

            const datos = await respuesta.json();

            if (
                !datos.features ||
                !datos.features.length
            ) {

                throw new Error(
                    'El límite distrital no está disponible.'
                );
            }

            // CONTORNO AZUL SIN RELLENO

            const capaBorde = L.geoJSON(
                datos,
                {
                    style: {
                        color: '#075aa1',
                        weight: 4,
                        opacity: 1,
                        fillOpacity: 0
                    }
                }
            );

            const limites =
                capaBorde.getBounds();

            // COMPROBAR QUE EL CONTORNO SEA VÁLIDO

            if (
                !limites.isValid() ||
                limites.getSouth() < -12.6 ||
                limites.getNorth() > -11.8 ||
                limites.getWest() < -77.3 ||
                limites.getEast() > -76.7
            ) {

                throw new Error(
                    'El servicio entregó un límite fuera de la zona de Chorrillos.'
                );
            }

            geo = datos;
            limitesChorrillos = limites;

            // CREAR MAPA

            mapa = L.map(
                'mapa-reporte',
                {
                    maxBounds: limites.pad(0.04),
                    maxBoundsViscosity: 1,
                    worldCopyJump: false,
                    zoomSnap: 0.25
                }
            );

            // CAPA DE OPENSTREETMAP

            L.tileLayer(
                'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                {
                    maxZoom: 19,
                    attribution:
                        '© OpenStreetMap contributors'
                }
            ).addTo(mapa);

            // MOSTRAR LÍMITE AZUL

            capaBorde.addTo(mapa);

            // CENTRAR EL MAPA

            mapa.setView(
                limites.getCenter(),
                13,
                {
                    animate: false
                }
            );

            // SELECCIÓN POR CLIC

            mapa.on('click', ev => {

                marcar(
                    ev.latlng.lat,
                    ev.latlng.lng,
                    false
                );
            });

            btnUbicacion.disabled = false;

            avisoMapa.textContent =
                'El contorno azul indica Chorrillos. ' +
                'Solo puedes marcar puntos dentro del distrito.';

            if (pasoActual === 1) {
                encuadrarChorrillos();
            }

            if (lat.value && lon.value) {
                revisarManual();
            }

        } catch (error) {

            geo = null;

            avisoMapa.textContent =
                error.message +
                ' Intenta actualizar la página. ' +
                'El servidor validará las coordenadas.';

            document.getElementById(
                'mapa-reporte'
            ).textContent =
                'No se pudo cargar el mapa de Chorrillos. ' +
                'Puedes introducir las coordenadas manualmente.';
        }
    })();

    // ==========================================
    // GEOLOCALIZACIÓN DEL CIUDADANO
    // ==========================================

    btnUbicacion.addEventListener('click', () => {

        if (!navigator.geolocation) {

            avisoMapa.textContent =
                'Tu navegador no admite GPS. ' +
                'Selecciona un punto en el mapa.';

            return;
        }

        navigator.geolocation.getCurrentPosition(
            pos => {

                marcar(
                    pos.coords.latitude,
                    pos.coords.longitude,
                    true
                );
            },
            () => {

                avisoMapa.textContent =
                    'No se obtuvo tu ubicación. ' +
                    'Selecciona un punto en el mapa ' +
                    'o usa coordenadas.';
            },
            {
                enableHighAccuracy: false,
                timeout: 10000,
                maximumAge: 60000
            }
        );
    });

})();
