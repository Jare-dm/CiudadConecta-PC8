package pe.edu.pc8.controller;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.repository.ReporteRepository;

/**
 * Mapa público de transparencia.
 * Publica SOLO zonas agregadas y nunca coordenadas individuales.
 */
@RestController
public class MapaPublicoTransparenciaController {

    /**
     * Ajuste visual:
     * antes era muy grande y por eso salían muy pocas zonas.
     * Estos pasos generan agrupaciones más naturales.
     *
     * Aproximadamente:
     *  - PASO_LAT 0.012  ~ 1.3 km
     *  - PASO_LON 0.016  ~ 1.7 km
     */
    private static final double PASO_LAT = 0.012;
    private static final double PASO_LON = 0.016;

    /**
     * Rango aproximado de Chorrillos y alrededores cercanos
     * para evitar puntos fuera de zona.
     */
    private static final double LAT_MIN = -12.28;
    private static final double LAT_MAX = -12.10;
    private static final double LON_MIN = -77.08;
    private static final double LON_MAX = -76.97;

    /**
     * Mínimo por zona para proteger privacidad.
     * Si luego quieres que aparezcan más zonas públicas,
     * puedes probar con 3.
     */
    private static final long MINIMO_POR_ZONA = 4L;

    private final ReporteRepository reportes;

    public MapaPublicoTransparenciaController(ReporteRepository reportes) {
        this.reportes = reportes;
    }

    public record Zona(double latitud, double longitud, long cantidad) { }

    private record Cuadricula(int fila, int columna) { }

    private static final class AcumuladoZona {
        private long cantidad;
        private double sumaLatitud;
        private double sumaLongitud;

        void agregar(double latitud, double longitud) {
            cantidad++;
            sumaLatitud += latitud;
            sumaLongitud += longitud;
        }

        long getCantidad() {
            return cantidad;
        }

        Zona aZona() {
            double latPromedio = sumaLatitud / cantidad;
            double lonPromedio = sumaLongitud / cantidad;

            return new Zona(
                    redondear(latPromedio),
                    redondear(lonPromedio),
                    cantidad
            );
        }
    }

    @GetMapping(
            value = "/transparencia/mapa-publico",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<Zona> consultarZonas() {

        Map<Cuadricula, AcumuladoZona> agrupadas = new HashMap<>();

        for (Reporte reporte : reportes.findAll()) {
            Double lat = reporte.getLatitud();
            Double lon = reporte.getLongitud();

            if (lat == null || lon == null) {
                continue;
            }

            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                continue;
            }

            if (lat < LAT_MIN || lat > LAT_MAX || lon < LON_MIN || lon > LON_MAX) {
                continue;
            }

            int fila = (int) Math.floor((lat - LAT_MIN) / PASO_LAT);
            int columna = (int) Math.floor((lon - LON_MIN) / PASO_LON);

            Cuadricula clave = new Cuadricula(fila, columna);

            agrupadas
                    .computeIfAbsent(clave, k -> new AcumuladoZona())
                    .agregar(lat, lon);
        }

        return agrupadas.values().stream()
                .filter(z -> z.getCantidad() >= MINIMO_POR_ZONA)
                .map(AcumuladoZona::aZona)
                .sorted(Comparator.comparingLong(Zona::cantidad).reversed())
                .toList();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1000.0) / 1000.0;
    }
}