
package pe.edu.pc8.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.CategoriaPersonalizadaRepository;

@Service
public class CategoriaPersonalizadaService {

    private final CategoriaPersonalizadaRepository categorias;

    public CategoriaPersonalizadaService(
            CategoriaPersonalizadaRepository categorias) {

        this.categorias = categorias;
    }

    // ==========================================
    // NORMALIZAR NOMBRE
    // ==========================================

    public static String normalizar(String entrada) {

        String s = Normalizer.normalize(
            entrada.strip().replaceAll("\\s+", " "),
            Normalizer.Form.NFD
        );

        return s.replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    // ==========================================
    // REGISTRAR PROPUESTA CIUDADANA
    // ==========================================

    @Transactional
    public CategoriaPersonalizada crear(
            String entrada,
            Usuario proponente) {

        // Solo ciudadanos activos pueden proponer.

        if (
            proponente == null ||
            !proponente.isActivo() ||
            proponente.getRol() != Rol.CIUDADANO
        ) {

            throw new AccessDeniedException(
                "Solo un ciudadano activo puede proponer categorías"
            );
        }

        if (entrada == null) {
            throw new IllegalArgumentException(
                "Escribe el nombre de la categoría"
            );
        }

        String nombre = entrada.strip()
            .replaceAll("\\s+", " ");

        if (
            nombre.length() < 3 ||
            nombre.length() > 65
        ) {

            throw new IllegalArgumentException(
                "El nombre debe tener entre 3 y 65 caracteres"
            );
        }

        if (
            !nombre.matches(
                "[\\p{L}\\p{N} .,()/\\-]+"
            )
        ) {

            throw new IllegalArgumentException(
                "Usa letras, números y signos sencillos para la categoría"
            );
        }

        String normal = normalizar(nombre);

        // Evitar nombres ya presentes en categorías base.

        for (Categoria c : Categoria.values()) {

            if (
                normal.equals(
                    normalizar(c.getEtiqueta())
                )
            ) {

                throw new IllegalArgumentException(
                    "Esta categoría ya está en la lista"
                );
            }
        }

        // Evitar categorías duplicadas, incluidas
        // las pendientes y las rechazadas.

        if (
            categorias
                .findByNombreNormalizado(normal)
                .isPresent()
        ) {

            throw new IllegalArgumentException(
                "La categoría ya fue registrada o propuesta"
            );
        }

        CategoriaPersonalizada c =
            new CategoriaPersonalizada();

        c.setNombre(nombre);
        c.setNombreNormalizado(normal);

        // NUEVO: guardar autor de la propuesta.
        c.setProponente(proponente);

        // NUEVO: esperar aprobación municipal.
        c.setEstado(
            EstadoCategoriaPersonalizada.PENDIENTE
        );

        try {

            return categorias.saveAndFlush(c);

        } catch (DataIntegrityViolationException ex) {

            throw new IllegalArgumentException(
                "La categoría ya está registrada"
            );
        }
    }

    // ==========================================
    // CONSULTAR CATEGORÍA
    // ==========================================

    public CategoriaPersonalizada obtener(Long id) {

        return categorias.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Categoría no encontrada"
                )
            );
    }
}
