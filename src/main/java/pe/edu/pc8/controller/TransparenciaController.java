
package pe.edu.pc8.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.pc8.model.Categoria;
import pe.edu.pc8.model.Estado;
import pe.edu.pc8.model.HistorialReporte;
import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.repository.HistorialRepository;
import pe.edu.pc8.repository.ReporteRepository;

@Controller
public class TransparenciaController {

    private final ReporteRepository reportes;
    private final HistorialRepository historial;

    public TransparenciaController(
            ReporteRepository reportes,
            HistorialRepository historial) {

        this.reportes = reportes;
        this.historial = historial;
    }

    // ==========================================
    // OBJETO PARA LOS GRÁFICOS
    // ==========================================

    public static class Dato {

        private final String etiqueta;
        private final long cantidad;

        public Dato(String etiqueta, long cantidad) {
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
    // OPCIONES DEL FILTRO DE CATEGORÍAS
    // ==========================================

    public static class OpcionCategoria {

        private final String valor;
        private final String etiqueta;

        public OpcionCategoria(String valor, String etiqueta) {
            this.valor = valor;
            this.etiqueta = etiqueta;
        }

        public String getValor() {
            return valor;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    // ==========================================
    // COMPROBAR SI UNA FECHA ESTÁ EN EL PERÍODO
    // ==========================================

    private boolean dentroDePeriodo(
            Reporte reporte,
            LocalDateTime inicio,
            LocalDateTime fin) {

        LocalDateTime fecha = reporte.getCreadoEn();

        return fecha != null
                && !fecha.isBefore(inicio)
                && !fecha.isAfter(fin);
    }

    // ==========================================
    // FILTRAR POR CATEGORÍA
    // ==========================================

    private boolean coincideCategoria(
            Reporte reporte,
            String categoria) {

        if ("todas".equals(categoria)) {
            return true;
        }

        if ("PERSONALIZADAS".equals(categoria)) {
            return reporte.getCategoriaPersonalizada() != null;
        }

        if (reporte.getCategoriaPersonalizada() != null) {
            return false;
        }

        return reporte.getCategoria() != null
                && reporte.getCategoria().name().equals(categoria);
    }

    // ==========================================
    // DASHBOARD DE TRANSPARENCIA
    // ==========================================

    @GetMapping("/transparencia")
    public String dashboard(

            @RequestParam(
                    name = "periodo",
                    defaultValue = "todo"
            ) String periodo,

            @RequestParam(
                    name = "categoria",
                    defaultValue = "todas"
            ) String categoria,

            Model model) {

        LocalDateTime ahora = LocalDateTime.now();

        // ======================================
        // VALIDAR PERÍODO
        // ======================================

        String periodoValido = switch (periodo) {
            case "mes", "7", "30", "90", "todo" -> periodo;
            default -> "todo";
        };

        // ======================================
        // VALIDAR CATEGORÍA
        // ======================================

        boolean categoriaValida =
                "todas".equals(categoria)
                || "PERSONALIZADAS".equals(categoria);

        if (!categoriaValida) {
            try {
                Categoria.valueOf(categoria);
                categoriaValida = true;
            } catch (IllegalArgumentException ex) {
                categoriaValida = false;
            }
        }

        String categoriaValidaFinal =
                categoriaValida ? categoria : "todas";

        // ======================================
        // DETERMINAR PERÍODO
        // ======================================

        LocalDateTime inicioMesActual =
                ahora.toLocalDate()
                        .withDayOfMonth(1)
                        .atStartOfDay();

        LocalDateTime desde = switch (periodoValido) {

            case "mes" -> inicioMesActual;

            case "7" -> ahora.minusDays(7);

            case "30" -> ahora.minusDays(30);

            case "90" -> ahora.minusDays(90);

            default -> null;
        };

        String periodoEtiqueta = switch (periodoValido) {

            case "mes" -> "Mes actual";

            case "7" -> "Últimos 7 días";

            case "30" -> "Últimos 30 días";

            case "90" -> "Últimos 90 días";

            default -> "Todo el historial";
        };

        // ======================================
        // CONSULTAR REPORTES
        // ======================================

        // CiudadConecta trabaja exclusivamente
        // con reportes del distrito de Chorrillos.

        List<Reporte> todos = reportes.findAll();

        // ======================================
        // APLICAR FILTROS
        // ======================================

        List<Reporte> registros = todos.stream()
                .filter(r -> {

                    if (desde != null
                            && !dentroDePeriodo(r, desde, ahora)) {
                        return false;
                    }

                    return coincideCategoria(
                            r,
                            categoriaValidaFinal
                    );
                })
                .toList();

        // ======================================
        // KPI 1: TOTAL DE REPORTES
        // ======================================

        long total = registros.size();

        // ======================================
        // KPI 2: REPORTES ATENDIDOS
        // ======================================

        long atendidos = registros.stream()
                .filter(r ->
                        r.getEstado() == Estado.ATENDIDO
                        || r.getEstado() == Estado.CERRADO
                )
                .count();

        // ======================================
        // KPI 3: REPORTES PENDIENTES
        // ======================================

        long pendientes = registros.stream()
                .filter(Reporte::enProceso)
                .count();

        // ======================================
        // INDICADOR COMPLEMENTARIO: RECHAZADOS
        // ======================================

        long rechazados = registros.stream()
                .filter(r -> r.getEstado() == Estado.RECHAZADO)
                .count();

        // ======================================
        // KPI 4: TIEMPO PROMEDIO HASTA ATENCIÓN
        // ======================================

        Map<Long, LocalDateTime> primeraAtencion =
                new HashMap<>();

        historial.findByEstado(Estado.ATENDIDO)
                .stream()
                .filter(h -> h.getFecha() != null)
                .sorted(Comparator.comparing(
                        HistorialReporte::getFecha
                ))
                .forEach(h -> {

                    Reporte reporte = h.getReporte();

                    if (reporte != null && reporte.getId() != null) {

                        primeraAtencion.putIfAbsent(
                                reporte.getId(),
                                h.getFecha()
                        );
                    }
                });

        double sumaDias = 0;
        long reportesConTiempo = 0;

        for (Reporte r : registros) {

            if (r.getId() == null || r.getCreadoEn() == null) {
                continue;
            }

            LocalDateTime fechaAtencion =
                    primeraAtencion.get(r.getId());

            if (fechaAtencion == null) {
                continue;
            }

            long segundos = Duration.between(
                    r.getCreadoEn(),
                    fechaAtencion
            ).getSeconds();

            if (segundos >= 0) {

                sumaDias += segundos / 86400.0;
                reportesConTiempo++;
            }
        }

        double promedioDias = reportesConTiempo > 0
                ? sumaDias / reportesConTiempo
                : 0.0;

        promedioDias = Math.round(promedioDias * 10.0) / 10.0;

        String promedioTexto = reportesConTiempo > 0
                ? promedioDias + " días"
                : "Sin datos";

        // ======================================
        // KPI 5: TIEMPO PROMEDIO HASTA CIERRE
        // ======================================
        // Se calcula solamente para reportes que tienen
        // al menos una transición CERRADO en el historial.
        // Se toma el primer cierre válido posterior a la creación.

        Map<Long, LocalDateTime> primerCierre = new HashMap<>();

        historial.findByEstado(Estado.CERRADO).stream()
                .filter(h -> h.getFecha() != null)
                .sorted(Comparator.comparing(HistorialReporte::getFecha))
                .forEach(h -> {
                    Reporte r = h.getReporte();
                    if (r != null && r.getId() != null) {
                        primerCierre.putIfAbsent(r.getId(), h.getFecha());
                    }
                });

        double sumaDiasCierre = 0.0;
        long reportesConCierre = 0;

        for (Reporte r : registros) {
            if (r.getId() == null || r.getCreadoEn() == null) {
                continue;
            }
            LocalDateTime fechaCierre = primerCierre.get(r.getId());
            if (fechaCierre == null) {
                continue;
            }
            long segundos = Duration.between(r.getCreadoEn(), fechaCierre).getSeconds();
            if (segundos >= 0) {
                sumaDiasCierre += segundos / 86400.0;
                reportesConCierre++;
            }
        }

        double promedioCierreDias = reportesConCierre > 0
                ? sumaDiasCierre / reportesConCierre
                : 0.0;
        promedioCierreDias = Math.round(promedioCierreDias * 10.0) / 10.0;

        String promedioCierreTexto = reportesConCierre > 0
                ? promedioCierreDias + " días"
                : "Sin datos";

        // ======================================
        // GRÁFICO 1: REPORTES POR CATEGORÍA
        // ======================================

        Map<String, Long> conteoCategorias =
                new TreeMap<>();

        for (Reporte r : registros) {

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

        List<Dato> categorias = conteoCategorias.entrySet()
                .stream()
                .map(e -> new Dato(e.getKey(), e.getValue()))
                .toList();

        // ======================================
        // GRÁFICO 2: DISTRIBUCIÓN POR ESTADO
        // ======================================

        List<Dato> estados = new ArrayList<>();

        estados.add(new Dato("En proceso", pendientes));
        estados.add(new Dato("Atendidos o cerrados", atendidos));
        estados.add(new Dato("Rechazados", rechazados));

        // ======================================
        // GRÁFICO 3: EVOLUCIÓN MENSUAL
        // ÚLTIMOS SEIS MESES
        // ======================================

        Map<YearMonth, Long> conteoMensual =
                new TreeMap<>();

        YearMonth mesActual =
                YearMonth.from(ahora);

        // Inicializar seis meses con cero.

        for (int i = 5; i >= 0; i--) {

            YearMonth mes =
                    mesActual.minusMonths(i);

            conteoMensual.put(mes, 0L);
        }

        // Contabilizar reportes según los filtros.

        for (Reporte r : registros) {

            if (r.getCreadoEn() == null) {
                continue;
            }

            YearMonth mes =
                    YearMonth.from(r.getCreadoEn());

            if (conteoMensual.containsKey(mes)) {

                conteoMensual.merge(
                        mes,
                        1L,
                        Long::sum
                );
            }
        }

        DateTimeFormatter formatoMes =
                DateTimeFormatter.ofPattern(
                        "MMM yyyy",
                        Locale.forLanguageTag("es-PE")
                );

        List<Dato> evolucionMensual =
                conteoMensual.entrySet()
                        .stream()
                        .map(e -> new Dato(
                                e.getKey().format(formatoMes),
                                e.getValue()
                        ))
                        .toList();

        // ======================================
        // OPCIONES DEL FILTRO DE CATEGORÍA
        // ======================================

        List<OpcionCategoria> opcionesCategorias =
                new ArrayList<>();

        opcionesCategorias.add(
                new OpcionCategoria(
                        "todas",
                        "Todas las categorías"
                )
        );

        for (Categoria c : Categoria.values()) {

            opcionesCategorias.add(
                    new OpcionCategoria(
                            c.name(),
                            c.getEtiqueta()
                    )
            );
        }

        opcionesCategorias.add(
                new OpcionCategoria(
                        "PERSONALIZADAS",
                        "Categorías personalizadas"
                )
        );

        // ======================================
        // ENTREGAR INFORMACIÓN A THYMELEAF
        // ======================================

        model.addAttribute("total", total);
        model.addAttribute("atendidos", atendidos);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("rechazados", rechazados);

        model.addAttribute("promedioDias", promedioDias);
        model.addAttribute("promedioTexto", promedioTexto);
        model.addAttribute("reportesConTiempo", reportesConTiempo);
        model.addAttribute("promedioCierreDias", promedioCierreDias);
        model.addAttribute("promedioCierreTexto", promedioCierreTexto);
        model.addAttribute("reportesConCierre", reportesConCierre);

        model.addAttribute("categorias", categorias);
        model.addAttribute("estados", estados);
        model.addAttribute("evolucionMensual", evolucionMensual);

        model.addAttribute("opcionesCategorias", opcionesCategorias);

        model.addAttribute("periodoSeleccionado", periodoValido);
        model.addAttribute("categoriaSeleccionada", categoriaValidaFinal);
        model.addAttribute("periodoEtiqueta", periodoEtiqueta);

        model.addAttribute("totalFiltrado", registros.size());

        model.addAttribute("distrito", "Chorrillos");

        return "transparencia";
    }
}
