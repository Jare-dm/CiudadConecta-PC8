
package pe.edu.pc8.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.model.Rol;
import pe.edu.pc8.model.Usuario;
import pe.edu.pc8.repository.ReporteRepository;
import pe.edu.pc8.repository.UsuarioRepository;

@RestController
@RequestMapping("/gestion/estadisticas/mapa")
@PreAuthorize(
    "hasAnyRole('FUNCIONARIO','MANTENIMIENTO','SUPERVISOR','ADMIN')"
)
public class MapaIncidenciasController {

    private final ReporteRepository reportes;
    private final UsuarioRepository usuarios;

    public MapaIncidenciasController(
            ReporteRepository reportes,
            UsuarioRepository usuarios) {
        this.reportes = reportes;
        this.usuarios = usuarios;
    }

    // Solo devolver coordenadas, sin datos personales.
    public record Punto(double latitud, double longitud) {}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Punto> puntos(
            @RequestParam(defaultValue = "todo") String periodo,
            @RequestParam(defaultValue = "") String categoria,
            Authentication autenticacion) {

        if (autenticacion == null ||
                !autenticacion.isAuthenticated()) {
            throw new AccessDeniedException(
                "Se requiere una cuenta municipal"
            );
        }

        Usuario usuario = usuarios
            .findByCorreoIgnoreCase(autenticacion.getName())
            .orElseThrow(() ->
                new AccessDeniedException("Usuario no encontrado")
            );

        if (!usuario.isActivo() ||
            (usuario.getRol() != Rol.FUNCIONARIO &&
             usuario.getRol() != Rol.MANTENIMIENTO &&
             usuario.getRol() != Rol.SUPERVISOR &&
             usuario.getRol() != Rol.ADMIN)) {

            throw new AccessDeniedException(
                "Sin acceso municipal vigente"
            );
        }

        String periodoValido =
            Set.of("todo", "7", "30", "90").contains(periodo)
                ? periodo
                : "todo";

        LocalDateTime ahora = LocalDateTime.now();

        LocalDateTime desde = switch (periodoValido) {
            case "7" -> ahora.minusDays(7);
            case "30" -> ahora.minusDays(30);
            case "90" -> ahora.minusDays(90);
            default -> null;
        };

        boolean mantenimiento =
            usuario.getRol() == Rol.MANTENIMIENTO;

        String categoriaSolicitada =
            categoria == null ? "" : categoria.trim();

        return reportes.findAll()
            .stream()
            .filter(r -> !mantenimiento ||
                (r.getResponsable() != null &&
                 usuario.getId().equals(
                     r.getResponsable().getId()
                 )))
            .filter(r -> desde == null ||
                (r.getCreadoEn() != null &&
                 !r.getCreadoEn().isBefore(desde)))
            .filter(r ->
                categoriaCoincide(r, categoriaSolicitada))
            .filter(
                MapaIncidenciasController::tieneCoordenadasValidas
            )
            .map(r -> new Punto(
                r.getLatitud(),
                r.getLongitud()
            ))
            .toList();
    }

    private static boolean categoriaCoincide(
            Reporte r, String categoria) {

        if (categoria.isBlank()) {
            return true;
        }

        if (r.getCategoriaPersonalizada() != null) {
            return (
                "P:" + r.getCategoriaPersonalizada().getId()
            ).equals(categoria);
        }

        return r.getCategoria() != null &&
            (
                "BASE:" + r.getCategoria().name()
            ).equals(categoria);
    }

    private static boolean tieneCoordenadasValidas(
            Reporte r) {

        Double lat = r.getLatitud();
        Double lon = r.getLongitud();

        return lat != null &&
               lon != null &&
               Double.isFinite(lat) &&
               Double.isFinite(lon) &&
               lat >= -12.50 &&
               lat <= -12.00 &&
               lon >= -77.25 &&
               lon <= -76.75;
    }
}
