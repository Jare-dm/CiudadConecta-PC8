
package pe.edu.pc8.repository;

import pe.edu.pc8.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialRepository
        extends JpaRepository<HistorialReporte, Long> {

    // Historial de un reporte específico.
    List<HistorialReporte> findByReporteOrderByFechaDesc(
        Reporte reporte
    );

    // Historial de todos los reportes por estado.
    List<HistorialReporte> findByEstado(Estado estado);

    // Historial general ordenado cronológicamente.
    List<HistorialReporte> findAllByOrderByFechaAsc();
}
