
package pe.edu.pc8.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.pc8.model.Estado;
import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.repository.ReporteRepository;

/**
 * Estadísticas públicas de CiudadConecta PC8.
 *
 * Solo se envían cantidades agregadas.
 * No se publican identidades, fotografías,
 * correos ni ubicaciones individuales.
 */
@Controller
public class TransparenciaController {

    private final ReporteRepository reportes;

    public TransparenciaController(
            ReporteRepository reportes) {

        this.reportes = reportes;
    }

    // ==========================================
    // DATOS DE CATEGORÍAS
    // ==========================================

    public static class Dato {

        private final String etiqueta;
        private final long cantidad;

        public Dato(
                String etiqueta,
                long cantidad) {

            this.etiqueta = etiqueta;
            this.cantidad = cantidad;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public long getCantidad() {
            return cantidad;
        }
    }

    // ==========================================
    // COMPROBAR SI UNA FECHA ESTÁ EN UN PERÍODO
    //
    // Inicio incluido, fin excluido.
    // ==========================================

    private boolean dentroDePeriodo(
            Reporte reporte,
            LocalDateTime inicio,
            LocalDateTime fin) {

        LocalDateTime fecha =
            reporte.getCreadoEn();

        return fecha != null
            && !fecha.isBefore(inicio)
            && fecha.isBefore(fin);
    }

    // ==========================================
    // ESTADÍSTICAS PÚBLICAS
    // ==========================================

    @GetMapping("/transparencia")
    public String dashboard(
            @RequestParam(
                name = "periodo",
                defaultValue = "todo"
            ) String periodo,
            Model model) {

        // ======================================
        // VALIDAR FILTRO
        // ======================================

        String periodoValido = switch (periodo) {

            case "mes", "7", "30", "90", "todo" ->
                periodo;

            default ->
                "todo";
        };

        LocalDateTime ahora =
            LocalDateTime.now();

        LocalDateTime inicioMesActual =
            ahora.toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay();

        LocalDateTime inicioMesAnterior =
            inicioMesActual.minusMonths(1);

        // ======================================
        // CONSULTAR DATOS DE MARIA DB
        // ======================================

        List<Reporte> todos =
            reportes.findAll();

        // ======================================
        // RESUMEN MENSUAL INDEPENDIENTE DEL
        // FILTRO SELECCIONADO
        // ======================================

        long recibidosMesActual = todos.stream()
            .filter(r -> {
                LocalDateTime fecha =
                    r.getCreadoEn();

                return fecha != null
                    && !fecha.isBefore(inicioMesActual)
                    && !fecha.isAfter(ahora);
            })
            .count();

        long recibidosMesAnterior = todos.stream()
            .filter(r ->
                dentroDePeriodo(
                    r,
                    inicioMesAnterior,
                    inicioMesActual
                )
            )
            .count();

        // ======================================
        // DETERMINAR INICIO DEL PERÍODO
        // ======================================

        LocalDateTime desde =
            switch (periodoValido) {

                case "mes" ->
                    inicioMesActual;

                case "7" ->
                    ahora.minusDays(7);

                case "30" ->
                    ahora.minusDays(30);

                case "90" ->
                    ahora.minusDays(90);

                default ->
                    null;
            };

        String periodoEtiqueta =
            switch (periodoValido) {

                case "mes" ->
                    "Mes actual";

                case "7" ->
                    "Últimos 7 días";

                case "30" ->
                    "Últimos 30 días";

                case "90" ->
                    "Últimos 90 días";

                default ->
                    "Todo el historial";
            };

        // ======================================
        // FILTRAR SEGÚN FECHA DE REGISTRO
        // ======================================

        List<Reporte> registros = todos.stream()
            .filter(r -> {

                if (desde == null) {
                    return true;
                }

                LocalDateTime fecha =
                    r.getCreadoEn();

                return fecha != null
                    && !fecha.isBefore(desde)
                    && !fecha.isAfter(ahora);
            })
            .toList();

        // ======================================
        // CONTAR ESTADOS
        // ======================================

        long total = registros.size();

        long cerrados = registros.stream()
            .filter(r ->
                r.getEstado() == Estado.CERRADO
            )
            .count();

        long atendidos = registros.stream()
            .filter(r ->
                r.getEstado() == Estado.ATENDIDO
            )
            .count();

        long rechazados = registros.stream()
            .filter(r ->
                r.getEstado() == Estado.RECHAZADO
            )
            .count();

        long pendientes = registros.stream()
            .filter(Reporte::enProceso)
            .count();

        // ======================================
        // CATEGORÍAS AGREGADAS
        // ======================================

        Map<String, Long> conteoCategorias =
            new TreeMap<>();

        for (Reporte r : registros) {

            // Las categorías personalizadas pueden
            // contener texto proporcionado por
            // ciudadanos. En la vista pública
            // se agrupan para evitar publicar
            // accidentalmente datos personales.
            String etiqueta =
                r.getCategoriaPersonalizada() != null
                    ? "Categorías personalizadas"
                    : r.getCategoriaEtiqueta();

            conteoCategorias.merge(
                etiqueta,
                1L,
                Long::sum
            );
        }

        List<Dato> categorias =
            conteoCategorias.entrySet()
                .stream()
                .map(e ->
                    new Dato(
                        e.getKey(),
                        e.getValue()
                    )
                )
                .toList();

        // ======================================
        // ETIQUETAS DE LOS MESES
        // ======================================

        DateTimeFormatter formatoMes =
            DateTimeFormatter.ofPattern(
                "MMMM yyyy",
                Locale.forLanguageTag("es-PE")
            );

        String mesActualNombre =
            inicioMesActual.format(formatoMes);

        String mesAnteriorNombre =
            inicioMesAnterior.format(formatoMes);

        // ======================================
        // ENTREGAR DATOS A THYMELEAF
        // ======================================

        model.addAttribute(
            "total",
            total
        );

        model.addAttribute(
            "pendientes",
            pendientes
        );

        model.addAttribute(
            "atendidos",
            atendidos + cerrados
        );

        model.addAttribute(
            "rechazados",
            rechazados
        );

        model.addAttribute(
            "categorias",
            categorias
        );

        model.addAttribute(
            "periodoSeleccionado",
            periodoValido
        );

        model.addAttribute(
            "periodoEtiqueta",
            periodoEtiqueta
        );

        model.addAttribute(
            "recibidosMesActual",
            recibidosMesActual
        );

        model.addAttribute(
            "recibidosMesAnterior",
            recibidosMesAnterior
        );

        model.addAttribute(
            "mesActualNombre",
            mesActualNombre
        );

        model.addAttribute(
            "mesAnteriorNombre",
            mesAnteriorNombre
        );

        return "transparencia";
    }
}
