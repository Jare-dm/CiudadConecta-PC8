
package pe.edu.pc8.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.pc8.model.CategoriaPersonalizada;
import pe.edu.pc8.model.EstadoCategoriaPersonalizada;

public interface CategoriaPersonalizadaRepository
        extends JpaRepository<CategoriaPersonalizada, Long> {

    // ==========================================
    // MÉTODOS EXISTENTES
    // ==========================================

    Optional<CategoriaPersonalizada> findByNombreNormalizado(
        String normalizado
    );

    List<CategoriaPersonalizada> findAllByOrderByNombreAsc();

    // ==========================================
    // NUEVO: CONSULTAR CATEGORÍAS POR ESTADO
    // ==========================================

    List<CategoriaPersonalizada> findByEstadoOrderByNombreAsc(
        EstadoCategoriaPersonalizada estado
    );

    // ==========================================
    // NUEVO: PROPUESTAS EN ORDEN DE CREACIÓN
    // ==========================================

    List<CategoriaPersonalizada> findByEstadoOrderByCreadaEnAsc(
        EstadoCategoriaPersonalizada estado
    );

    // ==========================================
    // NUEVO: CONTAR CATEGORÍAS POR ESTADO
    // ==========================================

    long countByEstado(
        EstadoCategoriaPersonalizada estado
    );
}
