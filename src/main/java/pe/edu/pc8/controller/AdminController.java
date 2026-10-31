
package pe.edu.pc8.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.repository.CategoriaPersonalizadaRepository;
import pe.edu.pc8.repository.NotificacionRepository;
import pe.edu.pc8.model.*;
import pe.edu.pc8.web.NuevoPersonalForm;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final CategoriaPersonalizadaRepository categorias;

    // NUEVO
    private final NotificacionRepository notificaciones;

    public AdminController(
            UsuarioRepository usuarios,
            PasswordEncoder encoder,
            CategoriaPersonalizadaRepository categorias,
            NotificacionRepository notificaciones) {

        this.usuarios = usuarios;
        this.encoder = encoder;
        this.categorias = categorias;
        this.notificaciones = notificaciones;
    }

    // ==========================================
    // COMPROBAR ADMINISTRADOR
    // ==========================================

    private void comprobarAdmin(Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException(
                "Debes iniciar sesión"
            );
        }

        Usuario actual = usuarios
            .findByCorreoIgnoreCase(auth.getName())
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Usuario no encontrado"
                )
            );

        if (
            !actual.isActivo() ||
            actual.getRol() != Rol.ADMIN
        ) {

            throw new AccessDeniedException(
                "Se requiere una cuenta administradora activa"
            );
        }
    }

    // ==========================================
    // CARGAR USUARIOS
    // ==========================================

    private void cargar(Model model) {

        var lista = usuarios.findAll();

        lista.sort(
            Comparator.comparing(
                Usuario::getNombre,
                String.CASE_INSENSITIVE_ORDER
            )
        );

        model.addAttribute("usuarios", lista);
        model.addAttribute("roles", Rol.values());

        model.addAttribute(
            "rolesPersonal",
            List.of(
                Rol.FUNCIONARIO,
                Rol.MANTENIMIENTO,
                Rol.SUPERVISOR
            )
        );
    }

    // ==========================================
    // LISTADO DE USUARIOS
    // ==========================================

    @GetMapping("/usuarios")
    public String lista(
            Authentication auth,
            Model model) {

        comprobarAdmin(auth);

        if (!model.containsAttribute("nuevoPersonalForm")) {

            model.addAttribute(
                "nuevoPersonalForm",
                new NuevoPersonalForm()
            );
        }

        cargar(model);

        return "admin-usuarios";
    }

    // ==========================================
    // CREAR PERSONAL MUNICIPAL
    // ==========================================

    @Transactional
    @PostMapping("/personal/nuevo")
    public String crearPersonal(
            @Valid
            @ModelAttribute("nuevoPersonalForm")
            NuevoPersonalForm f,
            BindingResult errores,
            Authentication auth,
            Model model,
            RedirectAttributes flash) {

        comprobarAdmin(auth);

        if (
            f.getRol() != null &&
            f.getRol() != Rol.FUNCIONARIO &&
            f.getRol() != Rol.MANTENIMIENTO &&
            f.getRol() != Rol.SUPERVISOR
        ) {

            errores.rejectValue(
                "rol",
                "rol.invalido",
                "Este formulario solo crea personal municipal"
            );
        }

        if (usuarios.existsByCorreoIgnoreCase(f.getCorreo())) {

            errores.rejectValue(
                "correo",
                "correo.duplicado",
                "El correo ya está registrado"
            );
        }

        if (usuarios.existsByDni(f.getDni())) {

            errores.rejectValue(
                "dni",
                "dni.duplicado",
                "El DNI ya está registrado"
            );
        }

        if (errores.hasErrors()) {

            cargar(model);
            return "admin-usuarios";
        }

        Usuario u = new Usuario();

        u.setNombre(f.getNombre().trim());
        u.setDni(f.getDni());

        u.setCorreo(
            f.getCorreo().trim().toLowerCase(Locale.ROOT)
        );

        u.setClaveHash(
            encoder.encode(f.getClave())
        );

        u.setRol(f.getRol());

        usuarios.save(u);

        flash.addFlashAttribute(
            "exito",
            "Cuenta de personal municipal creada correctamente"
        );

        return "redirect:/admin/usuarios";
    }

    // ==========================================
    // ACTUALIZAR CUENTA
    // ==========================================

    @Transactional
    @PostMapping("/usuarios/{id}")
    public String actualizar(
            @PathVariable Long id,
            @RequestParam Rol rol,
            @RequestParam(defaultValue = "false")
            boolean activo,
            Authentication auth,
            RedirectAttributes flash) {

        comprobarAdmin(auth);

        Usuario u = usuarios.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Usuario inexistente"
                )
            );

        Usuario actual = usuarios
            .findByCorreoIgnoreCase(auth.getName())
            .orElseThrow();

        if (
            u.getId().equals(actual.getId()) &&
            (!activo || rol != Rol.ADMIN)
        ) {

            flash.addFlashAttribute(
                "error",
                "No puedes desactivar tu cuenta ni quitarte el rol administrador"
            );

            return "redirect:/admin/usuarios";
        }

        if (
            u.getRol() == Rol.ADMIN &&
            u.isActivo() &&
            (rol != Rol.ADMIN || !activo) &&
            usuarios.countByRolAndActivoTrue(Rol.ADMIN) <= 1
        ) {

            flash.addFlashAttribute(
                "error",
                "Debe permanecer al menos un administrador activo"
            );

            return "redirect:/admin/usuarios";
        }

        u.setRol(rol);
        u.setActivo(activo);

        usuarios.save(u);

        flash.addFlashAttribute(
            "exito",
            "Cuenta actualizada. Los cambios de rol se aplican al volver a iniciar sesión"
        );

        return "redirect:/admin/usuarios";
    }

    // ==========================================
    // MODERACIÓN DE CATEGORÍAS
    // ==========================================

    @GetMapping("/categorias")
    public String listarCategorias(
            Authentication auth,
            Model model) {

        comprobarAdmin(auth);

        model.addAttribute(
            "pendientes",
            categorias.findByEstadoOrderByCreadaEnAsc(
                EstadoCategoriaPersonalizada.PENDIENTE
            )
        );

        model.addAttribute(
            "aprobadas",
            categorias.findByEstadoOrderByNombreAsc(
                EstadoCategoriaPersonalizada.APROBADA
            )
        );

        model.addAttribute(
            "rechazadas",
            categorias.findByEstadoOrderByNombreAsc(
                EstadoCategoriaPersonalizada.RECHAZADA
            )
        );

        return "admin-categorias";
    }

    // ==========================================
    // APROBAR O RECHAZAR CATEGORÍA
    // Y NOTIFICAR AL CIUDADANO
    // ==========================================

    @Transactional
    @PostMapping("/categorias/{id}/decision")
    public String decidirCategoria(
            @PathVariable Long id,
            @RequestParam String decision,
            Authentication auth,
            RedirectAttributes flash) {

        comprobarAdmin(auth);

        if (
            !"APROBAR".equals(decision) &&
            !"RECHAZAR".equals(decision)
        ) {

            flash.addFlashAttribute(
                "error",
                "Decisión de categoría no válida"
            );

            return "redirect:/admin/categorias";
        }

        CategoriaPersonalizada categoria =
            categorias.findById(id).orElse(null);

        if (categoria == null) {

            flash.addFlashAttribute(
                "error",
                "La categoría no existe"
            );

            return "redirect:/admin/categorias";
        }

        // No volver a procesar propuestas revisadas.

        if (
            categoria.getEstado() !=
            EstadoCategoriaPersonalizada.PENDIENTE
        ) {

            flash.addFlashAttribute(
                "error",
                "Esta categoría ya fue revisada"
            );

            return "redirect:/admin/categorias";
        }

        final String mensajeNotificacion;

        if ("APROBAR".equals(decision)) {

            categoria.setEstado(
                EstadoCategoriaPersonalizada.APROBADA
            );

            mensajeNotificacion =
                "Tu propuesta de categoría «" +
                categoria.getNombre() +
                "» fue APROBADA por la administración municipal. " +
                "Ya puedes utilizarla para registrar reportes.";

            flash.addFlashAttribute(
                "exito",
                "Categoría aprobada: " +
                categoria.getNombre()
            );

        } else {

            categoria.setEstado(
                EstadoCategoriaPersonalizada.RECHAZADA
            );

            mensajeNotificacion =
                "Tu propuesta de categoría «" +
                categoria.getNombre() +
                "» fue RECHAZADA por la administración municipal. " +
                "Puedes seleccionar una categoría existente.";

            flash.addFlashAttribute(
                "exito",
                "Categoría rechazada: " +
                categoria.getNombre()
            );
        }

        // GUARDAR DECISIÓN MUNICIPAL

        categorias.save(categoria);

        // ======================================
        // NUEVO: NOTIFICACIÓN AL PROPONENTE
        // ======================================

        Usuario proponente = categoria.getProponente();

        // Las categorías históricas pueden
        // no tener un autor registrado.

        if (proponente != null) {

            Notificacion notificacion =
                new Notificacion();

            notificacion.setDestinatario(proponente);

            // No está relacionada con un reporte.
            notificacion.setReporte(null);

            notificacion.setMensaje(
                mensajeNotificacion
            );

            notificaciones.save(notificacion);
        }

        return "redirect:/admin/categorias";
    }
}
