
package pe.edu.pc8.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;

import jakarta.validation.Valid;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.repository.ReporteRepository;
import pe.edu.pc8.web.RegistroForm;

@Controller
public class InicioController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final ReporteRepository reportes;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public InicioController(
            UsuarioRepository usuarios,
            PasswordEncoder encoder,
            ReporteRepository reportes) {

        this.usuarios = usuarios;
        this.encoder = encoder;
        this.reportes = reportes;
    }

    // ==========================================
    // DTO: INFORMACIÓN PÚBLICA DEL REPORTE
    // ==========================================

    public static class ReportePublico {

        private final String codigo;
        private final String categoria;
        private final String subcategoria;
        private final String estado;
        private final String fecha;

        public ReportePublico(
                String codigo,
                String categoria,
                String subcategoria,
                String estado,
                String fecha) {

            this.codigo = codigo;
            this.categoria = categoria;
            this.subcategoria = subcategoria;
            this.estado = estado;
            this.fecha = fecha;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getCategoria() {
            return categoria;
        }

        public String getSubcategoria() {
            return subcategoria;
        }

        public String getEstado() {
            return estado;
        }

        public String getFecha() {
            return fecha;
        }
    }

    // ==========================================
    // CONVERTIR ESTADO A TEXTO LEGIBLE
    // ==========================================

    private String obtenerEstado(Estado estado) {

        if (estado == null) {
            return "Sin estado";
        }

        return switch (estado) {
            case REGISTRADO -> "Registrado";
            case EN_REVISION -> "En revisión";
            case ATENDIDO -> "Atendido";
            case CERRADO -> "Cerrado";
            case RECHAZADO -> "Rechazado";
            default -> estado.name();
        };
    }

    // ==========================================
    // DASHBOARD PÚBLICO DE INICIO
    // ==========================================

    @GetMapping("/")
    public String portada(Model m) {

        // ======================================
        // INDICADORES GENERALES
        // ======================================

        long total = reportes.count();

        long atendidos =
                reportes.countByEstado(Estado.ATENDIDO)
                + reportes.countByEstado(Estado.CERRADO);

        long rechazados =
                reportes.countByEstado(Estado.RECHAZADO);

        long pendientes =
                Math.max(0L, total - atendidos - rechazados);

        m.addAttribute("inicioTotal", total);
        m.addAttribute("inicioPendientes", pendientes);
        m.addAttribute("inicioAtendidos", atendidos);
        m.addAttribute("inicioRechazados", rechazados);

        // ======================================
        // REPORTES RECIENTES
        // ======================================

        List<ReportePublico> recientes =
                reportes.findAllByOrderByCreadoEnDesc()
                        .stream()
                        .limit(5)
                        .map(r -> {

                            String categoria =
                                    r.getCategoriaPersonalizada() != null
                                    ? r.getCategoriaPersonalizada().getNombre()
                                    : r.getCategoriaEtiqueta();

                            String subcategoria =
                                    r.getSubcategoria() != null
                                    ? r.getSubcategoriaEtiqueta()
                                    : "";

                            String fecha =
                                    r.getCreadoEn() != null
                                    ? r.getCreadoEn().format(FORMATO_FECHA)
                                    : "Fecha no disponible";

                            return new ReportePublico(
                                    r.getCodigo(),
                                    categoria,
                                    subcategoria,
                                    obtenerEstado(r.getEstado()),
                                    fecha
                            );
                        })
                        .toList();

        m.addAttribute("reportesRecientes", recientes);

        m.addAttribute(
                "hayReportesRecientes",
                !recientes.isEmpty()
        );

        return "portada";
    }


    // ==========================================
    // GUÍA PÚBLICA: QUÉ PUEDO REPORTAR
    // ==========================================

    @GetMapping("/que-reportar")
    public String queReportar() {
        return "que-reportar";
    }


    // ==========================================
    // MANUAL PÚBLICO DE USUARIO
    // ==========================================

    @GetMapping("/manual")
    public String manual() {
        return "manual";
    }

    // ==========================================
    // MODO INVITADO: CONSULTA PÚBLICA
    // ==========================================

    @GetMapping("/invitado")
    public String invitado() {
        return "invitado";
    }

    // ==========================================
    // CENTRO PÚBLICO DE SOPORTE
    // ==========================================







    // ==========================================
    // INICIO DE SESIÓN
    // ==========================================

    @GetMapping("/login")
    public String login(Authentication a) {

        return a != null
                && a.isAuthenticated()
                && !(a instanceof AnonymousAuthenticationToken)
                ? "redirect:/inicio"
                : "login";
    }

    // ==========================================
    // REGISTRO
    // ==========================================

    @GetMapping("/registro")
    public String registro(Model m) {

        m.addAttribute("form", new RegistroForm());

        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @Valid @ModelAttribute("form") RegistroForm f,
            BindingResult errores,
            Model m) {

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
            return "registro";
        }

        Usuario u = new Usuario();

        u.setNombre(f.getNombre().trim());
        u.setDni(f.getDni());
        u.setCorreo(f.getCorreo().trim().toLowerCase());
        u.setTelefono(f.getTelefono());
        u.setClaveHash(encoder.encode(f.getClave()));
        u.setRol(Rol.CIUDADANO);

        usuarios.save(u);

        return "redirect:/login?registrado";
    }

    // ==========================================
    // REDIRECCIÓN SEGÚN ROL
    // ==========================================

    @GetMapping("/inicio")
    public String inicio(Authentication auth) {

        return auth.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_FUNCIONARIO")
                        || a.getAuthority().equals("ROLE_MANTENIMIENTO")
                        || a.getAuthority().equals("ROLE_SUPERVISOR")
                        || a.getAuthority().equals("ROLE_ADMIN")
                )
                ? "redirect:/gestion"
                : "redirect:/mis-reportes";
    }
}
