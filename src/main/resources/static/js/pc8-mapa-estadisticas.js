
/* CiudadConecta PC8 - Mapa municipal de incidencias */
(() => {
    'use strict';

    const contenedor =
    document.getElementById('pc8-mapa-incidencias');

    if (!contenedor) return;

    const resumen = document.getElementById('pc8-mapa-resumen');
    const estado = document.getElementById('pc8-mapa-estado');
    const tabla = document.getElementById('pc8-zonas-cuerpo');
    const botonCalor = document.getElementById('pc8-vista-calor');
    const botonPuntos = document.getElementById('pc8-vista-puntos');
    const botonAjustar = document.getElementById('pc8-mapa-ajustar');
    const avisoSinDatos = document.getElementById('pc8-mapa-vacio');

    const chorrillos = [-12.175, -77.012];

    let mapa = null;
    let calor = null;
    let marcadores = null;
    let puntos = [];
    let modo = 'calor';

    function anunciar(texto) {
        if (estado) estado.textContent = texto;
    }

    function cambiarVista(vista) {
        modo = vista;

        botonCalor.setAttribute(
            'aria-pressed', String(vista === 'calor')
        );
        botonPuntos.setAttribute(
            'aria-pressed', String(vista === 'puntos')
        );

        if (!mapa) return;

        if (calor && mapa.hasLayer(calor)) {
            mapa.removeLayer(calor);
        }

        if (marcadores && mapa.hasLayer(marcadores)) {
            mapa.removeLayer(marcadores);
        }

        if (vista === 'calor' && calor) {
            calor.addTo(mapa);
            anunciar('Mapa de calor visible.');
        } else if (marcadores) {
            marcadores.addTo(mapa);
            anunciar('Marcadores individuales visibles.');
        }
    }

    function centrarMapa() {
        if (!mapa) return;

        if (puntos.length > 0) {
            const limites = L.latLngBounds(
                puntos.map(p => [p.latitud, p.longitud])
            );

            mapa.fitBounds(limites.pad(0.18), {
                maxZoom: 15
            });
        } else {
            mapa.setView(chorrillos, 12);
        }
    }

    function mostrarZonas() {
        if (!tabla) return;

        tabla.replaceChildren();

        // Cuadrículas aproximadas de 500 metros.
        const paso = 0.0045;
        const celdas = new Map();

        puntos.forEach(p => {
            const fila =
            Math.floor((p.latitud + 13) / paso);

            const columna =
            Math.floor((p.longitud + 78) / paso);

            const clave = `${fila}:${columna}`;

            const celda = celdas.get(clave) || {
                cantidad: 0,
                latitud: 0,
                longitud: 0
            };

            celda.cantidad++;
            celda.latitud += p.latitud;
            celda.longitud += p.longitud;

            celdas.set(clave, celda);
        });

        const primeras = [...celdas.values()]
        .sort((a, b) => b.cantidad - a.cantidad)
        .slice(0, 5);

        if (primeras.length === 0) {
            const fila = tabla.insertRow();
            const celda = fila.insertCell();

            celda.colSpan = 3;
            celda.textContent =
            'No hay concentraciones con los filtros actuales.';

            return;
        }

        primeras.forEach((zona, indice) => {
            const lat = zona.latitud / zona.cantidad;
            const lon = zona.longitud / zona.cantidad;

            const fila = tabla.insertRow();
            const nombre = fila.insertCell();
            const cantidad = fila.insertCell();
            const accion = fila.insertCell();

            nombre.textContent =
            `Zona aproximada ${indice + 1}: ` +
            `${lat.toFixed(3)}, ${lon.toFixed(3)}`;

            cantidad.textContent =
            String(zona.cantidad);

            cantidad.className =
            'text-end fw-semibold';

            const boton =
            document.createElement('button');

            boton.type = 'button';
            boton.className =
            'btn btn-sm btn-outline-primary';

            boton.textContent = 'Ver en mapa';

            boton.setAttribute(
                'aria-label',
                `Ubicar zona aproximada ${indice + 1}, ` +
                `${zona.cantidad} reportes`
            );

            boton.disabled = !mapa;

            boton.addEventListener('click', () => {
                if (!mapa) return;

                mapa.setView([lat, lon], 15, {
                    animate: false
                });

                anunciar(
                    `Mostrando zona aproximada ${indice + 1}.`
                );

                contenedor.focus({
                    preventScroll: true
                });
            });

            accion.appendChild(boton);
        });
    }

    function inicializarMapa() {
        if (typeof L === 'undefined') {
            anunciar(
                'No se pudo cargar Leaflet. ' +
                'La tabla de concentraciones sigue disponible.'
            );
            return;
        }

        mapa = L.map(contenedor, {
            scrollWheelZoom: false,
            preferCanvas: true
        }).setView(chorrillos, 12);

        L.tileLayer(
            'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            {
                maxZoom: 18,
                attribution:
                '&copy; OpenStreetMap contributors'
            }
        ).addTo(mapa);

        marcadores = L.layerGroup();

        puntos.forEach(p => {
            L.circleMarker(
                [p.latitud, p.longitud],
                {
                    radius: 6,
                    color: '#0c4271',
                    fillColor: '#168ac5',
                    fillOpacity: 0.72,
                    weight: 1
                }
            )
            .bindPopup(
                'Incidencia registrada (sin datos personales)'
            )
            .addTo(marcadores);
        });

        if (typeof L.heatLayer === 'function') {
            calor = L.heatLayer(
                puntos.map(p => [
                    p.latitud,
                    p.longitud,
                    1
                ]),
                {
                    radius: 28,
                    blur: 20,
                    minOpacity: 0.32,
                    maxZoom: 16,
                    gradient: {
                        0.25: '#28a37b',
                        0.48: '#f1e16c',
                        0.72: '#f49a38',
                        1.00: '#cf353a'
                    }
                }
            );
        } else {
            botonCalor.disabled = true;
            modo = 'puntos';

            anunciar(
                'No cargó el mapa de calor. ' +
                'Se muestran los marcadores.'
            );
        }

        cambiarVista(modo);
        centrarMapa();

        // Mostrar el límite referencial de Chorrillos.
        fetch('/api/distritos/chorrillos', {
            credentials: 'same-origin'
        })
        .then(r => r.ok ? r.json() : null)
        .then(geojson => {
            if (!geojson || !mapa) return;

            const borde = L.geoJSON(geojson, {
                style: {
                    color: '#165b98',
                    weight: 2,
                    fill: false,
                    dashArray: '5 5'
                },
                interactive: false
            }).addTo(mapa);

            if (
                borde.getBounds().isValid() &&
                puntos.length === 0
            ) {
                mapa.fitBounds(
                    borde.getBounds().pad(0.06)
                );
            }
        })
        .catch(() => {
            // El mapa funciona incluso sin el contorno.
        });

        botonCalor.addEventListener(
            'click',
            () => cambiarVista('calor')
        );

        botonPuntos.addEventListener(
            'click',
            () => cambiarVista('puntos')
        );

        botonAjustar.addEventListener(
            'click',
            centrarMapa
        );

        window.setTimeout(
            () => mapa.invalidateSize(),
                          50
        );
    }

    async function cargar() {
        try {
            const respuesta = await fetch(
                contenedor.dataset.url,
                {
                    credentials: 'same-origin',
                    headers: {
                        Accept: 'application/json'
                    }
                }
            );

            if (
                !respuesta.ok ||
                !respuesta.headers
                .get('content-type')
                ?.includes('json')
            ) {
                throw new Error(
                    'Respuesta no disponible'
                );
            }

            const datos = await respuesta.json();

            if (!Array.isArray(datos)) {
                throw new Error(
                    'Formato inesperado'
                );
            }

            puntos = datos.filter(p =>
            Number.isFinite(p.latitud) &&
            Number.isFinite(p.longitud)
            );

            resumen.textContent =
            `${puntos.length} incidencias con ` +
            `coordenadas para los filtros seleccionados.`;

            avisoSinDatos.hidden =
            puntos.length !== 0;

            inicializarMapa();
            mostrarZonas();

            if (puntos.length > 0) {
                anunciar(
                    `Mapa cargado con ${puntos.length} incidencias.`
                );
            }

        } catch (error) {
            resumen.textContent =
            'No se pudieron consultar las coordenadas.';

                    anunciar(
                        'Error al cargar los datos del mapa.'
                    );

                    avisoSinDatos.hidden = false;

                    avisoSinDatos.textContent =
                    'No fue posible cargar los datos. ' +
                    'Comprueba tu sesión y vuelve a intentar.';

                    botonCalor.disabled = true;
                    botonPuntos.disabled = true;
                    botonAjustar.disabled = true;

                    if (tabla) {
                        tabla.replaceChildren();

                        const fila = tabla.insertRow();
                        const celda = fila.insertCell();

                        celda.colSpan = 3;
                        celda.textContent =
                        'Datos no disponibles.';
                    }
        }
    }

    cargar();
})();
