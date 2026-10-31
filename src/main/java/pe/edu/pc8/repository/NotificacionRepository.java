package pe.edu.pc8.repository;
import pe.edu.pc8.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface NotificacionRepository extends JpaRepository<Notificacion,Long> {
  List<Notificacion> findByDestinatarioOrderByCreadaEnDesc(Usuario destinatario);
  long countByDestinatarioAndLeidaFalse(Usuario destinatario);
  Optional<Notificacion> findByIdAndDestinatario(Long id,Usuario destinatario);
}
