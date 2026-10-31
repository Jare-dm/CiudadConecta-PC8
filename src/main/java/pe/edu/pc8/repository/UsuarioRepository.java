package pe.edu.pc8.repository;
import pe.edu.pc8.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
  Optional<Usuario> findByCorreoIgnoreCase(String correo);
  boolean existsByCorreoIgnoreCase(String correo);
  boolean existsByDni(String dni);
  long countByRolAndActivoTrue(pe.edu.pc8.model.Rol rol);
}
