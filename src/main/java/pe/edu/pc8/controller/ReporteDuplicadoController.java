
package pe.edu.pc8.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.edu.pc8.model.Categoria;
import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.model.Rol;
import pe.edu.pc8.model.Usuario;
import pe.edu.pc8.repository.ReporteRepository;
import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.service.ChorrillosService;

@RestController
@RequestMapping("/api/reportes")
public class ReporteDuplicadoController {

    // Distancia para considerar una posible coincidencia.
    private static final double RADIO_METROS = 150.0;

    // Solo incidencias registradas recientemente.
    private static final int DIAS_RECIENTES = 30;

    private static final double RADIO_TIERRA_METROS = 6_371_000.0;

    private final ReporteRepository reportes;
    private final UsuarioRepository usuarios;
    private final ChorrillosService chorrillos;

    public ReporteDuplicadoController(
            ReporteRepository reportes,
            UsuarioRepository usuarios,
            ChorrillosService chorrillos) {

        this.reportes = reportes;
        this.usuarios = usuarios;
        this.chorrillos = chorrillos;
    }

    // Respuesta pública para el ciudadano autenticado.
    // No contiene identidades ni datos individuales.
    public record Resultado(long cantidad) {
    }

    @GetMapping("/posibles-duplicados")
    public Resultado buscar(
            @RequestParam String categoriaOpcion,
            @RequestParam Double latitud,
            @RequestParam Double longitud,
            Authentication autenticacion) {

        // ==========================================
        // COMPROBAR IDENTIDAD Y ROL
        // ==========================================

        if (autenticacion == null ||
                !autenticacion.isAuthenticated()) {

            throw new AccessDeniedException(
                "Inicia sesión para registrar reportes"
            );
        }

        Usuario actual = usuarios
            .findByCorreoIgnoreCase(autenticacion.getName())
            .orElseThrow(() ->
                new AccessDeniedException("Cuenta no encontrada")
            );

        if (!actual.isActivo() ||
                actual.getRol() != Rol.CIUDADANO) {

            throw new AccessDeniedException(
                "Solo los ciudadanos pueden consultar coincidencias"
            );
        }

        // ==========================================
        // VALIDAR COORDENADAS
        // ==========================================

        if (latitud == null ||
                longitud == null ||
                !Double.isFinite(latitud) ||
                !Double.isFinite(longitud) ||
                latitud < -90 ||
                latitud > 90 ||
                longitud < -180 ||
                longitud > 180 ||
                !chorrillos.contiene(latitud, longitud)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Selecciona una ubicación válida dentro de Chorrillos"
            );
        }

        // ==========================================
        // VALIDAR CATEGORÍA
        // ==========================================

        if (!categoriaValida(categoriaOpcion)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Categoría no válida"
            );
        }

        // ==========================================
        // DEFINIR PERÍODO
        // ==========================================

        LocalDateTime ahora = LocalDateTime.now();

        LocalDateTime desde =
            ahora.minusDays(DIAS_RECIENTES);

        // ==========================================
        // BUSCAR POSIBLES COINCIDENCIAS
        // ==========================================

        List<Reporte> registros = reportes.findAll();

        long coincidencias = registros.stream()

            // El reporte debe seguir en proceso.
            .filter(r -> r.enProceso())

            // Solo los últimos 30 días.
            .filter(r ->
                r.getCreadoEn() != null &&
                !r.getCreadoEn().isBefore(desde) &&
                !r.getCreadoEn().isAfter(ahora)
            )

            // Misma categoría base o personalizada.
            .filter(r ->
                mismaCategoria(r, categoriaOpcion)
            )

            // Coordenadas disponibles.
            .filter(r ->
                r.getLatitud() != null &&
                r.getLongitud() != null
            )

            // Coordenadas numéricamente válidas.
            .filter(r ->
                Double.isFinite(r.getLatitud()) &&
                Double.isFinite(r.getLongitud())
            )

            // Máximo 150 metros de distancia.
            .filter(r ->
                distanciaMetros(
                    latitud,
                    longitud,
                    r.getLatitud(),
                    r.getLongitud()
                ) <= RADIO_METROS
            )

            .count();

        // No devolver códigos, ciudadanos, fotografías
        // ni direcciones de reportes existentes.
        return new Resultado(coincidencias);
    }

    // ==========================================
    // VALIDACIÓN DE LA CATEGORÍA SELECCIONADA
    // ==========================================

    private boolean categoriaValida(String opcion) {

        if (opcion == null) {
            return false;
        }

        if (opcion.startsWith("BASE:")) {

            try {
                Categoria.valueOf(
                    opcion.substring(5)
                );
                return true;

            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        if (opcion.startsWith("P:")) {

            try {
                return Long.parseLong(
                    opcion.substring(2)
                ) > 0;

            } catch (NumberFormatException ex) {
                return false;
            }
        }

        return false;
    }

    // ==========================================
    // COMPARAR CATEGORÍAS
    // ==========================================

    private boolean mismaCategoria(
            Reporte r,
            String opcion) {

        if (opcion.startsWith("BASE:")) {

            return r.getCategoriaPersonalizada() == null &&
                r.getCategoria() != null &&
                r.getCategoria().name().equals(
                    opcion.substring(5)
                );
        }

        if (opcion.startsWith("P:")) {

            return r.getCategoriaPersonalizada() != null &&
                String.valueOf(
                    r.getCategoriaPersonalizada().getId()
                ).equals(
                    opcion.substring(2)
                );
        }

        return false;
    }

    // ==========================================
    // DISTANCIA GEOGRÁFICA EN METROS
    // FÓRMULA DE HAVERSINE
    // ==========================================

    private double distanciaMetros(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double diferenciaLat =
            Math.toRadians(lat2 - lat1);

        double diferenciaLon =
            Math.toRadians(lon2 - lon1);

        double a =
            Math.pow(
                Math.sin(diferenciaLat / 2),
                2
            )
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.pow(
                Math.sin(diferenciaLon / 2),
                2
            );

        return 2 * RADIO_TIERRA_METROS *
            Math.asin(
                Math.min(
                    1.0,
                    Math.sqrt(Math.max(0.0, a))
                )
            );
    }
}
