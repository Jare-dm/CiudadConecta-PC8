/* Mapa accesible para el formulario público de incidencias (no autenticado). */
(() => {
    'use strict';
    const mapaEl = document.getElementById('pc8-mapa-invitado');
    const lat = document.getElementById('pc8-inv-lat');
    const lon = document.getElementById('pc8-inv-lon');
    const estado = document.getElementById('pc8-mapa-invitado-estado');
    const foto = document.getElementById('pc8-inv-foto');
    const form = document.getElementById('pc8-form-invitado');
    const boton = document.getElementById('pc8-inv-enviar');
    if (!form) return;

    let mapa = null;
    let marcador = null;
    let geojson = null;
    const centro = [-12.175, -77.012];
    const anunciar = mensaje => { if (estado) estado.textContent = mensaje; };

    function puntoValido() {
        return lat?.value && lon?.value &&
            Number.isFinite(Number(lat.value)) && Number.isFinite(Number(lon.value));
    }

    function marcar() {
        if (!mapa || !puntoValido()) return;
        const punto = [Number(lat.value), Number(lon.value)];
        if (!marcador) marcador = L.marker(punto).addTo(mapa);
        else marcador.setLatLng(punto);
    }

    function actualizarDesdeMapa(e) {
        lat.value = e.latlng.lat.toFixed(6);
        lon.value = e.latlng.lng.toFixed(6);
        marcar();
        anunciar('Punto seleccionado. Verifica la dirección y completa el formulario.');
    }

    if (mapaEl && typeof L !== 'undefined') {
        mapa = L.map(mapaEl, {scrollWheelZoom: false}).setView(centro, 12);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 18,
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(mapa);
        mapa.on('click', actualizarDesdeMapa);
        lat.addEventListener('change', marcar);
        lon.addEventListener('change', marcar);
        marcar();
        fetch('/api/distritos/chorrillos', {headers: {Accept: 'application/geo+json'}})
            .then(r => r.ok ? r.json() : null)
            .then(datos => {
                if (!datos) {
                    anunciar('No se pudo cargar el contorno. La ubicación se verificará al enviar.');
                    return;
                }
                geojson = L.geoJSON(datos, {
                    style: {color: '#145f97', weight: 3, fillColor: '#4da3cb', fillOpacity: .08},
                    interactive: false
                }).addTo(mapa);
                if (geojson.getBounds().isValid()) mapa.fitBounds(geojson.getBounds().pad(.08));
                marcar();
                anunciar('Mapa listo. Haz clic dentro del contorno de Chorrillos.');
            })
            .catch(() => anunciar('El contorno no está disponible; el servidor validará la ubicación.'));
    } else {
        anunciar('El mapa no está disponible; puedes escribir las coordenadas manualmente.');
    }

    foto?.addEventListener('change', () => {
        if (foto.files?.[0] && foto.files[0].size > 3_000_000) {
            foto.value = '';
            window.alert('La fotografía debe pesar menos de 3 MB.');
        }
    });

    form.addEventListener('submit', e => {
        if (!puntoValido()) {
            e.preventDefault();
            anunciar('Introduce latitud y longitud válidas antes de enviar.');
            lat?.focus();
            return;
        }
        if (!form.checkValidity()) return;
        if (boton) {
            boton.disabled = true;
            boton.textContent = 'Enviando reporte…';
        }
    });
})();
