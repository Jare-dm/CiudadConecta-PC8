package pe.edu.pc8.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos de una consulta de soporte; no se guardan en la base de datos. */
public class ContactoSoporteForm {

    @NotBlank(message = "Escribe tu nombre.")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Escribe tu correo electrónico.")
    @Email(message = "Escribe un correo electrónico válido.")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
             message = "Escribe un correo electrónico válido.")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres.")
    private String correo;

    @NotBlank(message = "Selecciona un motivo de consulta.")
    private String tipo;

    @NotBlank(message = "Escribe el asunto.")
    @Size(min = 5, max = 120, message = "El asunto debe tener entre 5 y 120 caracteres.")
    private String asunto;

    @NotBlank(message = "Describe tu consulta.")
    @Size(min = 20, max = 2500, message = "La descripción debe tener entre 20 y 2500 caracteres.")
    private String mensaje;

    @AssertTrue(message = "Debes aceptar el uso de tus datos para responder a la consulta.")
    private boolean aceptaDatos;

    /** Campo trampa antispam; debe permanecer vacío. */
    private String sitioWeb;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public boolean isAceptaDatos() { return aceptaDatos; }
    public void setAceptaDatos(boolean aceptaDatos) { this.aceptaDatos = aceptaDatos; }

    public String getSitioWeb() { return sitioWeb; }
    public void setSitioWeb(String sitioWeb) { this.sitioWeb = sitioWeb; }
}
