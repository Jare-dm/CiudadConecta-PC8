
package pe.edu.pc8.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.pc8.model.Categoria;
import pe.edu.pc8.model.Estado;
import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.model.Usuario;

public interface ReporteRepository
        extends JpaRepository<Reporte, Long> {

    // ==========================================
    // MÉTODOS EXISTENTES
    // ==========================================

    List<Reporte> findByCiudadanoOrderByCreadoEnDesc(
        Usuario ciudadano
    );

    List<Reporte> findAllByOrderByCreadoEnDesc();

    long countByEstado(Estado estado);

    long countByCategoria(Categoria categoria);

    // ==========================================
    // NUEVO: PREVENCIÓN DE REPORTES EXCESIVOS
    // ==========================================

    // Cuenta los reportes registrados por un
    // ciudadano desde una fecha determinada.
    //
    // Se utilizará para comprobar:
    // - Último minuto
    // - Última hora
    // - Últimas 24 horas

    long countByCiudadanoAndCreadoEnGreaterThanEqual(
        Usuario ciudadano,
        LocalDateTime desde
    );
}
