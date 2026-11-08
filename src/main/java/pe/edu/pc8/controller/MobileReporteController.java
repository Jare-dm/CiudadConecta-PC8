
package pe.edu.pc8.controller;

import jakarta.validation.Valid;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.*;
import pe.edu.pc8.service.*;
import pe.edu.pc8.web.ReporteForm;

@RestController
@RequestMapping("/api/movil/reportes")
public class MobileReporteController {

    private final ReporteRepository reportes;
    private final UsuarioRepository usuarios;
    private final CategoriaPersonalizadaRepository categorias;
    private final ReporteService servicio;
    private final FotoService fotos;

    public MobileReporteController(
            ReporteRepository reportes,
            UsuarioRepository usuarios,
            CategoriaPersonalizadaRepository categorias,
            ReporteService servicio,
            FotoService fotos) {

        this.reportes = reportes;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.servicio = servicio;
        this.fotos = fotos;
    }

    // ==========================================
    // IDENTIFICAR CIUDADANO AUTENTICADO
    // ==========================================

    private Usuario ciudadano(Authentication auth) {

        if (auth == null) {
            throw new AccessDeniedException("No autorizado");
        }

        Usuario usuario = usuarios
            .findByCorreoIgnoreCase(auth.getName())
            .orElseThrow(() ->
                new AccessDeniedException("Usuario no encontrado")
            );

        if (
            !usuario.isActivo() ||
            usuario.getRol() != Rol.CIUDADANO
        ) {
            throw new AccessDeniedException(
                "Solo los ciudadanos activos pueden utilizar esta función"
            );
        }

        return usuario;
    }

    // ==========================================
    // COMPROBAR PROPIEDAD DEL REPORTE
    // ==========================================

    private Reporte obtenerReportePropio(
            Long id,
            Usuario ciudadano) {

        Reporte reporte = reportes.findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reporte no encontrado"
                )
            );

        if (
            reporte.getCiudadano() == null ||
            !Objects.equals(
                reporte.getCiudadano().getId(),
                ciudadano.getId()
            )
        ) {
            throw new AccessDeniedException(
                "No tienes permiso para consultar este reporte"
            );
        }

        return reporte;
    }

    // ==========================================
    // 1. CATÁLOGOS PARA FLUTTER
    // ==========================================

    @GetMapping("/catalogos")
    public CatalogosResponse catalogos() {

        // Categorías principales y sus subcategorías.

        List<CategoriaResponse> categoriasBase =
            java.util.Arrays.stream(Categoria.values())
                .map(categoria -> {

                    List<SubcategoriaResponse> subcategorias =
                        Subcategoria.porCategoria(categoria)
                            .stream()
                            .map(sub -> new SubcategoriaResponse(
                                sub.name(),
                                sub.getEtiqueta()
                            ))
                            .toList();

                    return new CategoriaResponse(
                        categoria.name(),
                        categoria.getEtiqueta(),
                        subcategorias
                    );
                })
                .toList();

        // Solamente categorías personalizadas aprobadas.

        List<CategoriaPersonalizadaResponse> personalizadas =
            categorias.findByEstadoOrderByNombreAsc(
                EstadoCategoriaPersonalizada.APROBADA
            )
            .stream()
            .map(c -> new CategoriaPersonalizadaResponse(
                c.getId(),
                c.getNombre()
            ))
            .toList();

        return new CatalogosResponse(
            categoriasBase,
            personalizadas
        );
    }

    // ==========================================
    // 2. REGISTRAR NUEVO REPORTE
    // ==========================================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @Valid @ModelAttribute("form") ReporteForm form,
            BindingResult errores,
            Authentication auth) {

        // Validar campos del formulario.

        if (errores.hasErrors()) {

            Map<String, String> mensajes = new LinkedHashMap<>();

            errores.getFieldErrors().forEach(error ->
                mensajes.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );

            return ResponseEntity.badRequest().body(
                new ApiError(
                    "Revisa los datos del reporte.",
                    mensajes
                )
            );
        }

        Usuario ciudadano = ciudadano(auth);

        try {

            // Validaciones antes de guardar archivos.

            servicio.verificarFrecuencia(ciudadano);

            servicio.validarUbicacion(form);

            servicio.validarCategoriaDisponible(form);

            servicio.validarSubcategoria(form);

            // Guardar fotografía.

            String archivo = fotos.guardar(
                form.getFoto()
            );

            // Registrar utilizando el servicio existente.

            Reporte reporte = servicio.crear(
                form,
                ciudadano,
                archivo
            );

            return ResponseEntity
                .created(
                    URI.create(
                        "/api/movil/reportes/" + reporte.getId()
                    )
                )
                .body(
                    new CrearReporteResponse(
                        "Reporte registrado correctamente.",
                        convertir(reporte)
                    )
                );

        } catch (IllegalStateException ex) {

            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(
                    new ApiError(
                        ex.getMessage(),
                        Map.of()
                    )
                );

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                .badRequest()
                .body(
                    new ApiError(
                        ex.getMessage(),
                        Map.of()
                    )
                );

        } catch (IOException ex) {

            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    new ApiError(
                        "No se pudo guardar la fotografía.",
                        Map.of()
                    )
                );
        }
    }

    // ==========================================
    // 3. CONSULTAR MIS REPORTES
    // ==========================================

    @GetMapping
    public List<ReporteResponse> misReportes(
            Authentication auth) {

        Usuario ciudadano = ciudadano(auth);

        return reportes
            .findByCiudadanoOrderByCreadoEnDesc(ciudadano)
            .stream()
            .map(this::convertir)
            .toList();
    }

    // ==========================================
    // 4. CONSULTAR DETALLE DE UN REPORTE
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<ReporteResponse> detalle(
            @PathVariable Long id,
            Authentication auth) {

        Usuario ciudadano = ciudadano(auth);

        Reporte reporte = obtenerReportePropio(
            id,
            ciudadano
        );

        return ResponseEntity.ok(
            convertir(reporte)
        );
    }

    // ==========================================
    // 5. CONSULTAR FOTOGRAFÍA DEL REPORTE
    // ==========================================

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(
            @PathVariable Long id,
            Authentication auth) {

        Usuario ciudadano = ciudadano(auth);

        Reporte reporte = obtenerReportePropio(
            id,
            ciudadano
        );

        if (reporte.getArchivoImagen() == null) {
            return ResponseEntity.notFound().build();
        }

        return fotos.mostrar(
            reporte.getArchivoImagen()
        );
    }

    // ==========================================
    // CONVERTIR REPORTE A RESPUESTA JSON
    // ==========================================

    private ReporteResponse convertir(Reporte r) {

        String categoriaOpcion =
            r.getCategoriaPersonalizada() != null
                ? "P:" + r.getCategoriaPersonalizada().getId()
                : "BASE:" + r.getCategoria().name();

        String subcategoriaOpcion =
            r.getSubcategoria() == null
                ? null
                : r.getSubcategoria().name();

        String subcategoriaEtiqueta =
            r.getSubcategoria() == null
                ? null
                : r.getSubcategoria().getEtiqueta();

        String fotoUrl =
            r.getArchivoImagen() == null
                ? null
                : "/api/movil/reportes/" + r.getId() + "/foto";

        return new ReporteResponse(
            r.getId(),
            r.getCodigo(),
            categoriaOpcion,
            r.getCategoriaEtiqueta(),
            subcategoriaOpcion,
            subcategoriaEtiqueta,
            r.getDescripcion(),
            r.getUbicacion(),
            r.getLatitud(),
            r.getLongitud(),
            r.getEstado().name(),
            r.getEstado().getEtiqueta(),
            r.getCreadoEn(),
            r.isIdentidadReservada(),
            fotoUrl
        );
    }

    // ==========================================
    // DTO: CATÁLOGOS
    // ==========================================

    public record CatalogosResponse(
        List<CategoriaResponse> categorias,
        List<CategoriaPersonalizadaResponse> categoriasPersonalizadas
    ) {}

    public record CategoriaResponse(
        String codigo,
        String etiqueta,
        List<SubcategoriaResponse> subcategorias
    ) {}

    public record SubcategoriaResponse(
        String codigo,
        String etiqueta
    ) {}

    public record CategoriaPersonalizadaResponse(
        Long id,
        String nombre
    ) {}

    // ==========================================
    // DTO: REPORTES
    // ==========================================

    public record ReporteResponse(
        Long id,
        String codigo,
        String categoriaOpcion,
        String categoriaEtiqueta,
        String subcategoriaOpcion,
        String subcategoriaEtiqueta,
        String descripcion,
        String ubicacion,
        Double latitud,
        Double longitud,
        String estado,
        String estadoEtiqueta,
        java.time.LocalDateTime creadoEn,
        boolean identidadReservada,
        String fotoUrl
    ) {}

    public record CrearReporteResponse(
        String message,
        ReporteResponse reporte
    ) {}

    public record ApiError(
        String message,
        Map<String, String> errors
    ) {}
}
