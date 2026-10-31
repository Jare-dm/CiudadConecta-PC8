package pe.edu.pc8.web;
import jakarta.validation.constraints.*;
public class RegistroForm {
  @NotBlank @Size(max=110) private String nombre;
  @NotBlank @Pattern(regexp="\\d{8}",message="El DNI debe tener 8 dígitos") private String dni;
  @Email @NotBlank private String correo;
  @Pattern(regexp="[0-9+ -]{0,15}",message="Número no válido") private String telefono;
  @NotBlank @Size(min=8,max=70,message="Mínimo 8 caracteres") private String clave;
  public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
  public String getDni(){return dni;} public void setDni(String v){dni=v;}
  public String getCorreo(){return correo;} public void setCorreo(String v){correo=v;}
  public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;}
  public String getClave(){return clave;} public void setClave(String v){clave=v;}
}
