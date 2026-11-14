package pe.edu.pc8.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/** Campos enviados por un invitado. No solicita nombre, DNI ni correo. */
public class ReporteInvitadoForm {

    @NotBlank(message = "Selecciona el tipo de problema")
    private String categoriaOpcion;

    @NotBlank(message = "Describe el problema")
    @Size(min = 12, max = 1000, message = "Escribe entre 12 y 1000 caracteres")
    private String descripcion;

    @NotBlank(message = "Indica una dirección o referencia")
    @Size(min = 8, max = 260, message = "Escribe entre 8 y 260 caracteres")
    private String ubicacion;

    @NotNull(message = "Señala una ubicación en el mapa o escribe la latitud")
    @DecimalMin(value = "-90", message = "Latitud no válida")
    @DecimalMax(value = "90", message = "Latitud no válida")
    private Double latitud;

    @NotNull(message = "Señala una ubicación en el mapa o escribe la longitud")
    @DecimalMin(value = "-180", message = "Longitud no válida")
    @DecimalMax(value = "180", message = "Longitud no válida")
    private Double longitud;

    private MultipartFile foto;

    @AssertTrue(message = "Debes aceptar las condiciones de envío")
    private boolean aceptaCondiciones;

    // Honeypot: debe quedar vacío. No mostrar al usuario.
    private String sitioWeb;

    @NotBlank(message = "Actualiza el formulario antes de enviarlo")
    private String tokenFormulario;

    public String getCategoriaOpcion() { return categoriaOpcion; }
    public void setCategoriaOpcion(String v) { categoriaOpcion = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String v) { ubicacion = v; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double v) { latitud = v; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double v) { longitud = v; }
    public MultipartFile getFoto() { return foto; }
    public void setFoto(MultipartFile v) { foto = v; }
    public boolean isAceptaCondiciones() { return aceptaCondiciones; }
    public void setAceptaCondiciones(boolean v) { aceptaCondiciones = v; }
    public String getSitioWeb() { return sitioWeb; }
    public void setSitioWeb(String v) { sitioWeb = v; }
    public String getTokenFormulario() { return tokenFormulario; }
    public void setTokenFormulario(String v) { tokenFormulario = v; }
}
