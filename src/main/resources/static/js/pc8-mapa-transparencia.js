/* CiudadConecta PC8: mapa público mejorado de transparencia */
(() => {
    'use strict';

    const el = id => document.getElementById(id);

    const contenedor = el('pc8-mapa-publico');
    if (!contenedor) return;

    const estado = el('pc8-mapa-publico-estado');
    const resumen = el('pc8-mapa-publico-resumen');
    const tbody = el('pc8-mapa-publico-zonas');

    const botonCalor = el('pc8-publico-calor');
    const botonAreas = el('pc8-publico-areas');
    const botonCentrar = el('pc8-publico-centrar');

    const centroChorrillos = [-12.175, -77.012];

    let mapa = null;
    let capaCalor = null;
    let capaAreas = null;
    let zonas = [];
    let vista = 'calor';

    function anunciar(mensaje) {
        if (estado) estado.textContent = mensaje;
    }

    function actualizarBotones() {
        if (botonCalor) {
            botonCalor.setAttribute('aria-pressed', String(vista === 'calor'));
        }
        if (botonAreas) {
            botonAreas.setAttribute('aria-pressed', String(vista === 'areas'));
        }
    }

    function calcularRadio(cantidad) {
        // radio más moderado que antes, para evitar círculos exagerados
        return Math.min(900, 420 + (cantidad * 65));
    }

    function construirTabla() {
        if (!tbody) return;

        tbody.replaceChildren();

        if (!zonas.length) {
            const fila = tbody.insertRow();
            const celda = fila.insertCell();
            celda.colSpan = 3;
            celda.textContent = 'No hay suficientes zonas públicas para mostrar.';
            return;
        }

        zonas.forEach((zona, indice) => {
            const fila = tbody.insertRow();

            const celdaNombre = fila.insertCell();
            const celdaCantidad = fila.insertCell();
            const celdaAccion = fila.insertCell();

            celdaNombre.textContent = `Área aproximada ${indice + 1}`;
            celdaCantidad.textContent = String(zona.cantidad);

            const boton = document.createElement('button');
            boton.type = 'button';
            boton.className = 'pc8-mapa-boton pc8-mapa-boton-tabla';
            boton.textContent = 'Ver zona';
            boton.disabled = !mapa;
            boton.setAttribute(
                'aria-label',
                `Ver área aproximada ${indice + 1} con ${zona.cantidad} incidencias agrupadas`
            );

            boton.addEventListener('click', () => {
                if (!mapa) return;
                mapa.setView([zona.latitud, zona.longitud], 14, { animate: false });
                anunciar(`Mostrando área aproximada ${indice + 1} con ${zona.cantidad} incidencias agrupadas.`);
                contenedor.focus({ preventScroll: true });
            });

            celdaAccion.appendChild(boton);
        });
    }

    function centrarMapa() {
        if (!mapa) return;

        if (zonas.length) {
            const limites = L.latLngBounds(zonas.map(z => [z.latitud, z.longitud]));
            mapa.fitBounds(limites.pad(0.22), { maxZoom: 13 });
        } else {
            mapa.setView(centroChorrillos, 12);
        }
    }

    function cambiarVista(nuevaVista) {
        vista = nuevaVista;
        actualizarBotones();

        if (!mapa) return;

        if (capaCalor && mapa.hasLayer(capaCalor)) {
            mapa.removeLayer(capaCalor);
        }

        if (capaAreas && mapa.hasLayer(capaAreas)) {
            mapa.removeLayer(capaAreas);
        }

        if (vista === 'calor' && capaCalor) {
            capaCalor.addTo(mapa);
            anunciar('Vista de calor pública visible.');
        } else if (capaAreas) {
            capaAreas.addTo(mapa);
            anunciar('Vista de áreas agrupadas visible.');
        }
    }

    function crearCapaAreas() {
        const grupo = L.layerGroup();

        zonas.forEach((zona, indice) => {
            L.circle([zona.latitud, zona.longitud], {
                radius: calcularRadio(zona.cantidad),
                color: '#145b93',
                fillColor: '#2b86c5',
                fillOpacity: 0.16,
                weight: 2
            })
            .bindPopup(
                `<strong>Área aproximada ${indice + 1}</strong><br>` +
                `${zona.cantidad} incidencias agrupadas.<br>` +
                `No representa ubicaciones individuales.`
            )
            .addTo(grupo);
        });

        return grupo;
    }

    function crearCapaCalor() {
        if (typeof L.heatLayer !== 'function') {
            return null;
        }

        const maximo = Math.max(4, ...zonas.map(z => z.cantidad));

        const puntosCalor = zonas.map(z => [
            z.latitud,
            z.longitud,
            z.cantidad / maximo
        ]);

        return L.heatLayer(puntosCalor, {
            radius: 34,
            blur: 24,
            minOpacity: 0.30,
            max: 1,
            maxZoom: 15,
            gradient: {
                0.25: '#28a37b',
                0.48: '#f1e16c',
                0.72: '#f49a38',
                1.0: '#cf353a'
            }
        });
    }

    function montarMapa() {
        if (typeof L === 'undefined') {
            anunciar('No se pudo cargar el mapa. La tabla pública sigue disponible.');
            if (botonCalor) botonCalor.disabled = true;
            if (botonAreas) botonAreas.disabled = true;
            if (botonCentrar) botonCentrar.disabled = true;
            return;
        }

        mapa = L.map(contenedor, {
            scrollWheelZoom: false,
            preferCanvas: true
        }).setView(centroChorrillos, 12);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 18,
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(mapa);

        capaAreas = crearCapaAreas();
        capaCalor = crearCapaCalor();

        if (!capaCalor) {
            vista = 'areas';
            if (botonCalor) botonCalor.disabled = true;
            anunciar('No se cargó el mapa de calor. Se muestran áreas agrupadas.');
        }

        cambiarVista(vista);
        centrarMapa();

        if (botonCalor) {
            botonCalor.addEventListener('click', () => cambiarVista('calor'));
        }

        if (botonAreas) {
            botonAreas.addEventListener('click', () => cambiarVista('areas'));
        }

        if (botonCentrar) {
            botonCentrar.addEventListener('click', centrarMapa);
        }

        window.setTimeout(() => {
            if (mapa) mapa.invalidateSize();
        }, 80);
    }

    async function iniciar() {
        try {
            const url = contenedor.dataset.url || '/transparencia/mapa-publico';

            const respuesta = await fetch(url, {
                credentials: 'same-origin',
                headers: {
                    'Accept': 'application/json'
                }
            });

            const contentType = respuesta.headers.get('content-type') || '';

            if (!respuesta.ok || !contentType.includes('json')) {
                throw new Error('No se pudo consultar el mapa público');
            }

            const datos = await respuesta.json();

            if (!Array.isArray(datos)) {
                throw new Error('Formato de datos no válido');
            }

            zonas = datos.filter(z =>
                Number.isFinite(z.latitud) &&
                Number.isFinite(z.longitud) &&
                Number.isInteger(z.cantidad) &&
                z.cantidad >= 4
            );

            if (resumen) {
                resumen.textContent = zonas.length
                    ? `${zonas.length} áreas con al menos 4 incidencias agrupadas (todo el historial).`
                    : 'No hay suficientes concentraciones públicas para mostrar en el mapa.';
            }

            construirTabla();
            montarMapa();

            if (zonas.length) {
                anunciar(`Mapa público cargado con ${zonas.length} áreas agrupadas.`);
            } else {
                anunciar('No hay suficientes datos agrupados para mostrar en el mapa público.');
            }

        } catch (error) {
            if (resumen) {
                resumen.textContent = 'No se pudieron cargar las zonas del mapa público.';
            }

            anunciar('El mapa público no está disponible en este momento.');

            if (tbody) {
                tbody.replaceChildren();
                const fila = tbody.insertRow();
                const celda = fila.insertCell();
                celda.colSpan = 3;
                celda.textContent = 'Mapa temporalmente no disponible.';
            }

            if (botonCalor) botonCalor.disabled = true;
            if (botonAreas) botonAreas.disabled = true;
            if (botonCentrar) botonCentrar.disabled = true;
        }
    }

    iniciar();
})();