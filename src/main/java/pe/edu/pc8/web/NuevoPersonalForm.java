package pe.edu.pc8.web;
import jakarta.validation.constraints.*;
import pe.edu.pc8.model.Rol;
public class NuevoPersonalForm {
  @NotBlank @Size(min=3,max=110) private String nombre;
  @NotBlank @Pattern(regexp="\\d{8}",message="Ingresa un DNI de ocho dígitos para la cuenta de prueba") private String dni;
  @NotBlank @Email @Size(max=150) private String correo;
  @NotBlank @Size(min=10,max=72) private String clave;
  @NotNull private Rol rol=Rol.FUNCIONARIO;
  public String getNombre(){return nombre;}public void setNombre(String v){nombre=v;}
  public String getDni(){return dni;}public void setDni(String v){dni=v;}
  public String getCorreo(){return correo;}public void setCorreo(String v){correo=v;}
  public String getClave(){return clave;}public void setClave(String v){clave=v;}
  public Rol getRol(){return rol;}public void setRol(Rol v){rol=v;}
}
