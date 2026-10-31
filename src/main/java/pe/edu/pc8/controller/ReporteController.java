
package pe.edu.pc8.controller;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.*;
import pe.edu.pc8.service.*;
import pe.edu.pc8.web.*;

@Controller
public class ReporteController {

    private final ReporteRepository reportes;
    private final UsuarioRepository usuarios;
    private final ReporteService servicio;
    private final FotoService fotos;
    private final HistorialRepository historial;
    private final CategoriaPersonalizadaRepository categoriasPersonalizadas;

    public ReporteController(
            ReporteRepository r,
            UsuarioRepository u,
            ReporteService s,
            FotoService f,
            HistorialRepository h,
            CategoriaPersonalizadaRepository cp) {

        reportes = r;
        usuarios = u;
        servicio = s;
        fotos = f;
        historial = h;
        categoriasPersonalizadas = cp;
    }

    // ==========================================
    // USUARIO AUTENTICADO
    // ==========================================

    private Usuario usuario(Authentication auth) {

        Usuario u = usuarios
            .findByCorreoIgnoreCase(auth.getName())
            .orElseThrow();

        if (!u.isActivo()) {
            throw new AccessDeniedException(
                "Cuenta desactivada"
            );
        }

        return u;
    }

    // ==========================================
    // IDENTIFICAR PERSONAL MUNICIPAL
    // ==========================================

    private boolean esPersonal(Authentication auth) {

        Rol rol = usuario(auth).getRol();

        return rol == Rol.FUNCIONARIO
            || rol == Rol.MANTENIMIENTO
            || rol == Rol.SUPERVISOR
            || rol == Rol.ADMIN;
    }

    // ==========================================
    // CONTROL DE ACCESO A CADA REPORTE
    // ==========================================

    private Reporte autorizado(
            Long id,
            Authentication auth) {

        Reporte r = reportes.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Reporte no encontrado"
                )
            );

        if (esPersonal(auth)) {

            Usuario actual = usuario(auth);

            if (
                actual.getRol() == Rol.MANTENIMIENTO &&
                (
                    r.getResponsable() == null ||
                    !r.getResponsable().getId()
                        .equals(actual.getId())
                )
            ) {

                throw new AccessDeniedException(
                    "Solo puedes ver reportes asignados"
                );
            }

        } else if (
            !r.getCiudadano().getCorreo()
                .equalsIgnoreCase(auth.getName())
        ) {

            throw new AccessDeniedException(
                "No autorizado"
            );
        }

        return r;
    }

    // ==========================================
    // MIS REPORTES
    // ==========================================

    @GetMapping("/mis-reportes")
    public String lista(
            Authentication auth,
            Model m) {

        Usuario u = usuario(auth);

        m.addAttribute("usuario", u);

        m.addAttribute(
            "reportes",
            reportes.findByCiudadanoOrderByCreadoEnDesc(u)
        );

        return "mis-reportes";
    }

    // ==========================================
    // CARGAR CATEGORÍAS
    // ==========================================



private void cargarCategorias(Model m) {

    // Categorías principales.

    m.addAttribute(
        "categorias",
        Categoria.values()
    );

    // NUEVO: catálogo de subcategorías.

    m.addAttribute(
        "subcategorias",
        Subcategoria.values()
    );

    // Categorías personalizadas aprobadas.

    m.addAttribute(
        "categoriasPersonalizadas",
        categoriasPersonalizadas.findByEstadoOrderByNombreAsc(
            EstadoCategoriaPersonalizada.APROBADA
        )
    );
}



    // ==========================================
    // MOSTRAR FORMULARIO DE NUEVO REPORTE
    // ==========================================

    @GetMapping("/reportes/nuevo")
    public String nuevo(Model m) {

        m.addAttribute(
            "form",
            new ReporteForm()
        );

        cargarCategorias(m);

        return "nuevo";
    }

    // ==========================================
    // CREAR REPORTE
    // NUEVO: CONTROL DE FRECUENCIA
    // ==========================================

    @PostMapping("/reportes/nuevo")
    public String crear(
            @Valid @ModelAttribute("form") ReporteForm f,
            BindingResult e,
            Authentication auth,
            Model m,
            RedirectAttributes flash) {

        // --------------------------------------
        // 1. VALIDAR CAMPOS DEL FORMULARIO
        // --------------------------------------

        if (e.hasErrors()) {

            cargarCategorias(m);

            return "nuevo";
        }

        try {

            // ----------------------------------
            // 2. IDENTIFICAR AL CIUDADANO
            // ----------------------------------

            Usuario ciudadano = usuario(auth);

            // ----------------------------------
            // 3. NUEVO: CONTROL DE FRECUENCIA
            // ----------------------------------
            //
            // Reglas propuestas:
            // - Mínimo 1 minuto entre reportes.
            // - Máximo 5 reportes por hora.
            // - Máximo 15 reportes en 24 horas.
            //
            // Verificamos antes de guardar
            // cualquier fotografía.

            servicio.verificarFrecuencia(
                ciudadano
            );

            // ----------------------------------
            // 4. VALIDAR UBICACIÓN
            // ----------------------------------

            servicio.validarUbicacion(f);



         // Comprobar aprobación antes de guardar la foto.
            servicio.validarCategoriaDisponible(f);


         // Validar la subcategoría antes de guardar archivos.
            servicio.validarSubcategoria(f);

            // ----------------------------------
            // 5. GUARDAR FOTOGRAFÍA
            // ----------------------------------

            String foto = fotos.guardar(
                f.getFoto()
            );

            // ----------------------------------
            // 6. CREAR REPORTE EN MARIA DB
            // ----------------------------------
            //
            // El servicio mantiene la categoría,
            // ubicación, identidad reservada,
            // historial y demás datos existentes.

            Reporte r = servicio.crear(
                f,
                ciudadano,
                foto
            );

            // ----------------------------------
            // 7. CONFIRMACIÓN
            // ----------------------------------

            flash.addFlashAttribute(
                "exito",
                "¡Reporte registrado! Código: " +
                r.getCodigo()
            );

            return "redirect:/reportes/" +
                r.getId();

        } catch (
            IOException |
            IllegalArgumentException |
            IllegalStateException ex
        ) {

            // Si se supera el límite de
            // frecuencia, mostrar el mensaje
            // dentro del formulario.

            m.addAttribute(
                "error",
                ex.getMessage()
            );

            cargarCategorias(m);

            return "nuevo";
        }
    }

    // ==========================================
    // DETALLE E HISTORIAL DEL REPORTE
    // ==========================================

    @GetMapping("/reportes/{id}")
    public String detalle(
            @PathVariable Long id,
            Authentication auth,
            Model m) {

        Reporte r = autorizado(id, auth);

        m.addAttribute(
            "reporte",
            r
        );

        m.addAttribute(
            "historial",
            historial.findByReporteOrderByFechaDesc(r)
        );

        m.addAttribute(
            "personal",
            esPersonal(auth)
        );

        m.addAttribute(
            "confirmacionPendiente",
            !esPersonal(auth) &&
            r.getEstado() == Estado.ATENDIDO
        );

        return "detalle";
    }

    // ==========================================
    // CONFIRMACIÓN DE ATENCIÓN
    // ==========================================

    @PostMapping("/reportes/{id}/confirmar")
    public String confirmarAtencion(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String motivo,
            Authentication auth,
            RedirectAttributes flash) {

        Reporte r = autorizado(id, auth);

        if (esPersonal(auth)) {

            throw new AccessDeniedException(
                "Solo el ciudadano puede confirmar la atención"
            );
        }

        try {

            if (
                !decision.equals("confirmar") &&
                !decision.equals("revisar")
            ) {

                throw new IllegalArgumentException(
                    "Acción de confirmación no válida"
                );
            }

            servicio.confirmarAtencion(
                r,
                usuario(auth),
                decision.equals("confirmar"),
                motivo
            );

            flash.addFlashAttribute(
                "exito",
                decision.equals("confirmar")
                    ? "Gracias: tu reporte fue cerrado"
                    : "Registramos tu solicitud de nueva revisión"
            );

        } catch (IllegalArgumentException ex) {

            flash.addFlashAttribute(
                "error",
                ex.getMessage()
            );
        }

        return "redirect:/reportes/" + id;
    }

    // ==========================================
    // FOTOGRAFÍA DE ATENCIÓN MUNICIPAL
    // ==========================================

    @GetMapping("/reportes/{id}/foto-atencion")
    @ResponseBody
    public ResponseEntity<Resource> fotoAtencion(
            @PathVariable Long id,
            Authentication auth) {

        Reporte r = autorizado(id, auth);

        return r.getArchivoAtencion() == null
            ? ResponseEntity.notFound().build()
            : fotos.mostrar(
                r.getArchivoAtencion()
            );
    }

    // ==========================================
    // FOTOGRAFÍA ORIGINAL DEL CIUDADANO
    // ==========================================

    @GetMapping("/reportes/{id}/foto")
    @ResponseBody
    public ResponseEntity<Resource> foto(
            @PathVariable Long id,
            Authentication auth) {

        Reporte r = autorizado(id, auth);

        return r.getArchivoImagen() == null
            ? ResponseEntity.notFound().build()
            : fotos.mostrar(
                r.getArchivoImagen()
            );
    }
}
