
package pe.edu.pc8.web;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

public class ReporteForm {

    // ==========================================
    // CATEGORÍA PRINCIPAL
    // ==========================================

    @NotBlank(message = "Elige la categoría")
    private String categoriaOpcion;

    // ==========================================
    // NUEVO: SUBCATEGORÍA
    // ==========================================
    //
    // Ejemplos:
    // LUMINARIA_APAGADA
    // PISTA_AGRIETADA
    // ARBOL_CAIDO
    //
    // Es opcional para mantener compatibilidad
    // con el formulario actual y las categorías
    // que no tienen subcategorías.

    @Size(
        max = 60,
        message = "Subcategoría no válida"
    )
    private String subcategoriaOpcion;

    // ==========================================
    // DESCRIPCIÓN DEL PROBLEMA
    // ==========================================

    @NotBlank
    @Size(
        min = 12,
        max = 1000,
        message = "Describe el problema en 12 a 1000 caracteres"
    )
    private String descripcion;

    // ==========================================
    // UBICACIÓN
    // ==========================================

    @NotBlank
    @Size(
        min = 8,
        max = 260,
        message = "Indica una dirección o referencia de al menos 8 caracteres"
    )
    private String ubicacion;

    // ==========================================
    // LATITUD
    // ==========================================

    @DecimalMin(
        value = "-90.0",
        message = "Latitud fuera del rango permitido"
    )
    @DecimalMax(
        value = "90.0",
        message = "Latitud fuera del rango permitido"
    )
    @NotNull(
        message = "Selecciona un punto dentro de Chorrillos o introduce coordenadas"
    )
    private Double latitud;

    // ==========================================
    // LONGITUD
    // ==========================================

    @DecimalMin(
        value = "-180.0",
        message = "Longitud fuera del rango permitido"
    )
    @DecimalMax(
        value = "180.0",
        message = "Longitud fuera del rango permitido"
    )
    @NotNull(
        message = "Selecciona un punto dentro de Chorrillos o introduce coordenadas"
    )
    private Double longitud;

    // ==========================================
    // FOTOGRAFÍA
    // ==========================================

    private MultipartFile foto;

    // ==========================================
    // PRIVACIDAD DEL REPORTE
    // ==========================================

    // Por defecto, protegemos la identidad
    // del ciudadano hasta que elija otra opción.

    private boolean identidadReservada = true;

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public String getCategoriaOpcion() {
        return categoriaOpcion;
    }

    public void setCategoriaOpcion(String v) {
        categoriaOpcion = v;
    }

    // NUEVOS MÉTODOS DE SUBCATEGORÍA

    public String getSubcategoriaOpcion() {
        return subcategoriaOpcion;
    }

    public void setSubcategoriaOpcion(String v) {
        subcategoriaOpcion = v;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String v) {
        descripcion = v;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String v) {
        ubicacion = v;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double v) {
        latitud = v;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double v) {
        longitud = v;
    }

    public MultipartFile getFoto() {
        return foto;
    }

    public void setFoto(MultipartFile v) {
        foto = v;
    }

    // PRIVACIDAD

    public boolean isIdentidadReservada() {
        return identidadReservada;
    }

    public void setIdentidadReservada(boolean v) {
        identidadReservada = v;
    }
}
