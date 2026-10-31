package pe.edu.pc8.repository;
import pe.edu.pc8.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HistorialRepository extends JpaRepository<HistorialReporte,Long> {
  List<HistorialReporte> findByReporteOrderByFechaDesc(Reporte reporte);
}
