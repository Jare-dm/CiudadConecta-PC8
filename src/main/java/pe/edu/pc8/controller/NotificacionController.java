
package pe.edu.pc8.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.*;

@Controller
@RequestMapping("/notificaciones")
@PreAuthorize("hasAnyRole('CIUDADANO','MANTENIMIENTO')")
public class NotificacionController {

    private final UsuarioRepository usuarios;
    private final NotificacionRepository notificaciones;

    public NotificacionController(
            UsuarioRepository u,
            NotificacionRepository n) {

        this.usuarios = u;
        this.notificaciones = n;
    }

    // =========================================
    // OBTENER USUARIO ACTUAL
    // =========================================

    private Usuario usuarioActual(Authentication auth) {

        if (auth == null) {
            throw new AccessDeniedException(
                "Debes iniciar sesión"
            );
        }

        Usuario u = usuarios
            .findByCorreoIgnoreCase(auth.getName())
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Usuario no encontrado"
                )
            );

        if (!u.isActivo()) {
            throw new AccessDeniedException(
                "Tu cuenta está inactiva"
            );
        }

        return u;
    }

    // =========================================
    // MOSTRAR NOTIFICACIONES
    // =========================================

    @GetMapping
    public String lista(
            Authentication auth,
            Model model) {

        Usuario u = usuarioActual(auth);

        // Cada usuario consulta únicamente
        // las notificaciones dirigidas a su cuenta.
        model.addAttribute(
            "notificaciones",
            notificaciones.findByDestinatarioOrderByCreadaEnDesc(u)
        );

        // Contar notificaciones sin leer.
        model.addAttribute(
            "noLeidas",
            notificaciones.countByDestinatarioAndLeidaFalse(u)
        );

        return "notificaciones";
    }

    // =========================================
    // MARCAR NOTIFICACIÓN COMO LEÍDA
    // =========================================

    @Transactional
    @PostMapping("/{id}/leida")
    public String marcar(
            @PathVariable Long id,
            Authentication auth) {

        Usuario u = usuarioActual(auth);

        // El ID y el destinatario deben coincidir.
        // No permite modificar avisos de otra cuenta.
        Notificacion n = notificaciones
            .findByIdAndDestinatario(id, u)
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Notificación ajena o inexistente"
                )
            );

        n.setLeida(true);

        return "redirect:/notificaciones";
    }
}
