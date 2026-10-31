
package pe.edu.pc8.controller;

import java.util.Map;

import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.service.CategoriaPersonalizadaService;

@RestController
@RequestMapping("/categorias/personalizadas")
@PreAuthorize("hasRole('CIUDADANO')")
public class CategoriaPersonalizadaController {

    private final CategoriaPersonalizadaService servicio;
    private final UsuarioRepository usuarios;

    public CategoriaPersonalizadaController(
            CategoriaPersonalizadaService servicio,
            UsuarioRepository usuarios) {

        this.servicio = servicio;
        this.usuarios = usuarios;
    }

    // ==========================================
    // PROPONER CATEGORÍA
    // ==========================================

    @PostMapping(
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> crear(
            @RequestParam String nombre,
            Authentication auth) {

        if (auth == null) {
            throw new AccessDeniedException(
                "Debes iniciar sesión"
            );
        }

        Usuario ciudadano = usuarios
            .findByCorreoIgnoreCase(auth.getName())
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Usuario no encontrado"
                )
            );

        if (
            !ciudadano.isActivo() ||
            ciudadano.getRol() != Rol.CIUDADANO
        ) {

            throw new AccessDeniedException(
                "No autorizado para proponer categorías"
            );
        }

        try {

            CategoriaPersonalizada c =
                servicio.crear(nombre, ciudadano);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    Map.of(
                        "id", c.getId(),
                        "nombre", c.getNombre(),
                        "estado", c.getEstado().name()
                    )
                );

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "error", ex.getMessage()
                    )
                );
        }
    }
}
