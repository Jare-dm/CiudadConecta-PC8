
package pe.edu.pc8.web;

import pe.edu.pc8.model.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

public class ActualizacionForm {

    // ==========================================
    // ESTADO DEL REPORTE
    // ==========================================

    @NotNull(message = "Selecciona un estado")
    private Estado estado;

    // ==========================================
    // ÁREA MUNICIPAL
    // ==========================================

    private AreaMunicipal area;

    // ==========================================
    // RESPONSABLE MUNICIPAL
    // ==========================================

    @Positive(message = "El responsable seleccionado no es válido")
    private Long responsableId;

    // ==========================================
    // OBSERVACIÓN
    // ==========================================

    @Size(
        max = 700,
        message = "La observación no puede superar los 700 caracteres"
    )
    private String observacion;

    // ==========================================
    // FOTOGRAFÍA DE ATENCIÓN
    // ==========================================

    private MultipartFile fotoAtencion;

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public MultipartFile getFotoAtencion() {
        return fotoAtencion;
    }

    public void setFotoAtencion(MultipartFile v) {
        fotoAtencion = v;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado v) {
        estado = v;
    }

    public Long getResponsableId() {
        return responsableId;
    }

    public void setResponsableId(Long v) {
        responsableId = v;
    }

    public AreaMunicipal getArea() {
        return area;
    }

    public void setArea(AreaMunicipal v) {
        area = v;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String v) {
        observacion = v;
    }
}
