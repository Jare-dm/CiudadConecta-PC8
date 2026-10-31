
package pe.edu.pc8.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.*;
import pe.edu.pc8.service.ReporteService;
import pe.edu.pc8.web.ActualizacionForm;

@Controller
@RequestMapping("/gestion")
@PreAuthorize("hasAnyRole('FUNCIONARIO','MANTENIMIENTO','SUPERVISOR','ADMIN')")
public class GestionController {

    private final ReporteRepository reportes;
    private final ReporteService servicio;
    private final CategoriaPersonalizadaRepository categoriasPersonalizadas;
    private final UsuarioRepository usuarios;

    public GestionController(
            ReporteRepository r,
            ReporteService s,
            CategoriaPersonalizadaRepository cp,
            UsuarioRepository usuarios) {

        this.reportes = r;
        this.servicio = s;
        this.categoriasPersonalizadas = cp;
        this.usuarios = usuarios;
    }

    // =============================================
    // USUARIO AUTENTICADO
    // =============================================

    private Usuario actor(Authentication auth) {

        Usuario u = usuarios.findByCorreoIgnoreCase(auth.getName())
                .orElseThrow();

        if (!u.isActivo() ||
                (u.getRol() != Rol.FUNCIONARIO &&
                 u.getRol() != Rol.MANTENIMIENTO &&
                 u.getRol() != Rol.SUPERVISOR &&
                 u.getRol() != Rol.ADMIN)) {

            throw new AccessDeniedException(
                    "No tienes acceso vigente al módulo municipal"
            );
        }

        return u;
    }

    // =============================================
    // CONTROL DE ACCESO A REPORTES
    // =============================================

    private void validarAcceso(Reporte r, Usuario u) {

        if (u.getRol() == Rol.MANTENIMIENTO &&
                (r.getResponsable() == null ||
                 !r.getResponsable().getId().equals(u.getId()))) {

            throw new AccessDeniedException(
                    "Solo puedes consultar tus reportes asignados"
            );
        }
    }

    // =============================================
    // NUEVO: ESTADOS DISPONIBLES POR ROL
    // =============================================

    private List<Estado> estadosPermitidos(Reporte r, Usuario u) {

        Estado actual = r.getEstado();

        // Conservar el estado actual permite actualizar
        // campos administrativos sin forzar un avance.
        List<Estado> opciones = new ArrayList<>();
        opciones.add(actual);

        switch (u.getRol()) {

            case FUNCIONARIO, SUPERVISOR, ADMIN -> {

                switch (actual) {

                    case REGISTRADO -> {
                        opciones.add(Estado.EN_REVISION);
                        opciones.add(Estado.RECHAZADO);
                    }

                    case EN_REVISION -> {
                        opciones.add(Estado.ASIGNADO);
                        opciones.add(Estado.RECHAZADO);
                    }

                    case ATENDIDO -> {
                        // Solo supervisor o administrador.
                        if (u.getRol() == Rol.SUPERVISOR ||
                            u.getRol() == Rol.ADMIN) {

                            opciones.add(Estado.CERRADO);
                        }
                    }

                    default -> {
                        // No ofrecer otros avances.
                    }
                }
            }

            case MANTENIMIENTO -> {

                switch (actual) {

                    case ASIGNADO -> {
                        opciones.add(Estado.EN_ATENCION);
                    }

                    case EN_ATENCION -> {
                        opciones.add(Estado.ATENDIDO);
                    }

                    default -> {
                        // Mantenimiento no puede cambiar
                        // otros estados.
                    }
                }
            }

            default -> {
                // Otros roles no están autorizados.
            }
        }

        return opciones;
    }

    // =============================================
    // NUEVO: MENSAJE DE TRANSICIÓN INVALIDA
    // =============================================

    private String mensajeTransicion(
            Reporte r,
            Usuario u,
            Estado solicitado) {

        return "No puedes cambiar el reporte de '" +
                r.getEstado().getEtiqueta() +
                "' a '" +
                solicitado.getEtiqueta() +
                "' con el rol " +
                u.getRol().name() +
                ". Selecciona una de las opciones disponibles.";
    }

    // =============================================
    // PANEL MUNICIPAL
    // =============================================

    @GetMapping({"", "/"})
    public String panel(
            @RequestParam(required = false) Estado estado,
            @RequestParam(defaultValue = "") String categoria,
            @RequestParam(defaultValue = "") String buscar,
            Authentication auth,
            Model m) {

        Usuario u = actor(auth);

        String filtro = buscar.trim()
                .toLowerCase(Locale.ROOT);

        List<Reporte> todos =
                reportes.findAllByOrderByCreadoEnDesc()
                        .stream()
                        .filter(r ->
                                u.getRol() != Rol.MANTENIMIENTO ||
                                (r.getResponsable() != null &&
                                 r.getResponsable()
                                         .getId()
                                         .equals(u.getId())))
                        .collect(Collectors.toList());

        List<Reporte> lista = todos.stream()

                .filter(r ->
                        estado == null ||
                        r.getEstado() == estado)

                .filter(r ->
                        categoria.isBlank() ||

                        (categoria.startsWith("BASE:") &&
                         ("BASE:" + r.getCategoria().name())
                                 .equals(categoria) &&
                         r.getCategoriaPersonalizada() == null) ||

                        (r.getCategoriaPersonalizada() != null &&
                         ("P:" +
                          r.getCategoriaPersonalizada().getId())
                                 .equals(categoria)))

                .filter(r ->
                        filtro.isBlank() ||
                        r.getCodigo()
                                .toLowerCase(Locale.ROOT)
                                .contains(filtro) ||
                        r.getUbicacion()
                                .toLowerCase(Locale.ROOT)
                                .contains(filtro))

                .collect(Collectors.toList());

        lista.sort(
                Comparator.comparing(Reporte::isVencido)
                        .reversed()
                        .thenComparing(
                                Reporte::getCreadoEn,
                                Comparator.reverseOrder()
                        )
        );

        m.addAttribute("reportes", lista);

        // En los filtros sí mostramos todos los estados.
        m.addAttribute("estados", Estado.values());
        m.addAttribute("categorias", Categoria.values());

        m.addAttribute(
                "categoriasPersonalizadas",
                categoriasPersonalizadas.findAllByOrderByNombreAsc()
        );

        m.addAttribute("estadoSeleccionado", estado);
        m.addAttribute("categoriaSeleccionada", categoria);
        m.addAttribute("buscar", buscar);

        m.addAttribute(
                "mantenimiento",
                u.getRol() == Rol.MANTENIMIENTO
        );

        m.addAttribute("total", todos.size());

        m.addAttribute(
                "pendientes",
                todos.stream()
                        .filter(Reporte::enProceso)
                        .count()
        );

        m.addAttribute(
                "atendidos",
                todos.stream()
                        .filter(r ->
                                r.getEstado() == Estado.ATENDIDO ||
                                r.getEstado() == Estado.CERRADO)
                        .count()
        );

        m.addAttribute(
                "rechazados",
                todos.stream()
                        .filter(r ->
                                r.getEstado() == Estado.RECHAZADO)
                        .count()
        );

        m.addAttribute(
                "vencidos",
                todos.stream()
                        .filter(Reporte::isVencido)
                        .count()
        );

        return "gestion";
    }

    // =============================================
    // ESTADÍSTICAS MUNICIPALES
    // =============================================


@PreAuthorize(
    "hasAnyRole('FUNCIONARIO','MANTENIMIENTO','SUPERVISOR','ADMIN')"
)
@GetMapping("/estadisticas")
public String estadisticas(
        @RequestParam(defaultValue = "todo") String periodo,
        @RequestParam(defaultValue = "") String categoria,
        Authentication auth,
        Model m) {

    Usuario u = actor(auth);

    boolean soloAsignados =
        u.getRol() == Rol.MANTENIMIENTO;

    // =====================================
    // VALIDAR PERÍODO SOLICITADO
    // =====================================

    String periodoValido =
        Set.of("todo", "7", "30", "90").contains(periodo)
            ? periodo
            : "todo";

    java.time.LocalDateTime ahora =
        java.time.LocalDateTime.now();

    java.time.LocalDateTime inicio =
        switch (periodoValido) {

            case "7" ->
                ahora.minusDays(7);

            case "30" ->
                ahora.minusDays(30);

            case "90" ->
                ahora.minusDays(90);

            default ->
                null;
        };

    // =====================================
    // FILTRAR REPORTES SEGÚN EL ROL
    // =====================================

    List<Reporte> visibles = reportes.findAll()
        .stream()

        // Mantenimiento solo ve sus asignaciones.
        .filter(r ->
            !soloAsignados ||
            (r.getResponsable() != null &&
             r.getResponsable()
                .getId()
                .equals(u.getId()))
        )

        // Filtrar por fecha de creación.
        .filter(r ->
            inicio == null ||
            (r.getCreadoEn() != null &&
             !r.getCreadoEn().isBefore(inicio))
        )

        // Filtrar por categoría.
        .filter(r -> {

            if (categoria.isBlank()) {
                return true;
            }

            if (r.getCategoriaPersonalizada() != null) {

                return (
                    "P:" +
                    r.getCategoriaPersonalizada().getId()
                ).equals(categoria);
            }

            return r.getCategoria() != null &&
                (
                    "BASE:" +
                    r.getCategoria().name()
                ).equals(categoria);
        })

        .toList();

    // =====================================
    // INDICADORES GENERALES
    // =====================================

    long total = visibles.size();

    long pendientes = visibles.stream()
        .filter(Reporte::enProceso)
        .count();

    long atendidos = visibles.stream()
        .filter(r ->
            r.getEstado() == Estado.ATENDIDO ||
            r.getEstado() == Estado.CERRADO)
        .count();

    long rechazados = visibles.stream()
        .filter(r ->
            r.getEstado() == Estado.RECHAZADO)
        .count();

    // =====================================
    // ESTADÍSTICAS POR ESTADO
    // =====================================

    List<Barra> porEstado = new ArrayList<>();

    for (Estado e : Estado.values()) {

        long cantidad = visibles.stream()
            .filter(r -> r.getEstado() == e)
            .count();

        int porcentaje = total == 0
            ? 0
            : (int) Math.round(
                100.0 * cantidad / total
            );

        porEstado.add(
            new Barra(
                e.getEtiqueta(),
                cantidad,
                porcentaje
            )
        );
    }

    // =====================================
    // ESTADÍSTICAS POR CATEGORÍA
    // =====================================

    Map<String, Long> contadores =
        new TreeMap<>();

    for (Reporte r : visibles) {

        contadores.merge(
            r.getCategoriaEtiqueta(),
            1L,
            Long::sum
        );
    }

    List<Barra> porCategoria =
        new ArrayList<>();

    for (var entrada : contadores.entrySet()) {

        long cantidad = entrada.getValue();

        int porcentaje = total == 0
            ? 0
            : (int) Math.round(
                100.0 * cantidad / total
            );

        porCategoria.add(
            new Barra(
                entrada.getKey(),
                cantidad,
                porcentaje
            )
        );
    }

    // =====================================
    // DATOS PARA THYMELEAF
    // =====================================

    m.addAttribute("total", total);
    m.addAttribute("pendientes", pendientes);
    m.addAttribute("atendidos", atendidos);
    m.addAttribute("rechazados", rechazados);

    m.addAttribute("porEstado", porEstado);
    m.addAttribute("porCategoria", porCategoria);

    m.addAttribute(
        "vistaPersonal",
        soloAsignados
    );

    m.addAttribute(
        "periodoSeleccionado",
        periodoValido
    );

    m.addAttribute(
        "categoriaSeleccionada",
        categoria
    );

    m.addAttribute(
        "categorias",
        Categoria.values()
    );

    m.addAttribute(
        "categoriasPersonalizadas",
        categoriasPersonalizadas.findAllByOrderByNombreAsc()
    );

    return "estadisticas";
}


    // =============================================
    // CLASE AUXILIAR DE ESTADÍSTICAS
    // =============================================

    public static class Barra {

        private final String etiqueta;
        private final long cantidad;
        private final int porcentaje;

        public Barra(String e, long c, int p) {
            etiqueta = e;
            cantidad = c;
            porcentaje = p;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public long getCantidad() {
            return cantidad;
        }

        public int getPorcentaje() {
            return porcentaje;
        }
    }

    // =============================================
    // PREPARAR FORMULARIO DE EDICIÓN
    // =============================================

    private void cargarEditar(
            Model m,
            Reporte r,
            Usuario u) {

        m.addAttribute("reporte", r);

        // CAMBIO PRINCIPAL:
        // Mostrar solamente las transiciones autorizadas.
        m.addAttribute(
                "estados",
                estadosPermitidos(r, u)
        );

        m.addAttribute("areas", AreaMunicipal.values());

        m.addAttribute(
                "mantenimiento",
                u.getRol() == Rol.MANTENIMIENTO
        );

        m.addAttribute(
                "puedeCerrar",
                u.getRol() == Rol.SUPERVISOR ||
                u.getRol() == Rol.ADMIN
        );

        m.addAttribute(
                "responsables",
                usuarios.findAll()
                        .stream()
                        .filter(v ->
                                v.isActivo() &&
                                (v.getRol() == Rol.FUNCIONARIO ||
                                 v.getRol() == Rol.MANTENIMIENTO))
                        .sorted(
                                Comparator.comparing(
                                        Usuario::getNombre,
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList()
        );
    }

    // =============================================
    // ABRIR REPORTE PARA GESTIONAR
    // =============================================

    @GetMapping("/{id}")
    public String editar(
            @PathVariable Long id,
            Authentication auth,
            Model m) {

        Reporte r = reportes.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Reporte no encontrado"
                        )
                );

        Usuario u = actor(auth);

        validarAcceso(r, u);

        ActualizacionForm form = new ActualizacionForm();

        form.setEstado(r.getEstado());
        form.setArea(r.getArea());

        form.setResponsableId(
                r.getResponsable() == null
                        ? null
                        : r.getResponsable().getId()
        );

        form.setObservacion(r.getObservacion());

        m.addAttribute("form", form);

        cargarEditar(m, r, u);

        return "gestionar";
    }

    // =============================================
    // GUARDAR CAMBIOS DEL REPORTE
    // =============================================

    @PostMapping("/{id}")
    public String guardar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ActualizacionForm f,
            BindingResult errores,
            Authentication auth,
            Model m,
            RedirectAttributes flash) {

        Reporte r = reportes.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Reporte no encontrado"
                        )
                );

        Usuario u = actor(auth);

        validarAcceso(r, u);

        if (!errores.hasErrors()) {

            Estado solicitado = f.getEstado();

            // Comprobar el estado incluso si alguien modifica
            // manualmente el formulario desde el navegador.
            if (solicitado == null) {

                m.addAttribute(
                        "error",
                        "Debes seleccionar un estado válido."
                );

            } else if (!estadosPermitidos(r, u)
                    .contains(solicitado)) {

                m.addAttribute(
                        "error",
                        mensajeTransicion(r, u, solicitado)
                );

            } else {

                try {

                    // ReporteService conserva la validación
                    // definitiva y las reglas de negocio.
                    servicio.actualizar(r, f, u);

                    flash.addFlashAttribute(
                            "exito",
                            "Actualización registrada"
                    );

                    return "redirect:/gestion/" + id;

                } catch (
                        IllegalArgumentException |
                        IOException ex) {

                    m.addAttribute(
                            "error",
                            ex.getMessage()
                    );
                }
            }
        }

        cargarEditar(m, r, u);

        return "gestionar";
    }
}
