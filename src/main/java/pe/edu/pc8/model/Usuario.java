package pe.edu.pc8.model;
import jakarta.persistence.*;
@Entity @Table(name="usuarios",uniqueConstraints={@UniqueConstraint(columnNames="correo"),@UniqueConstraint(columnNames="dni")})
public class Usuario {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false,length=110) private String nombre;
  @Column(nullable=false,length=8) private String dni;
  @Column(nullable=false,length=150) private String correo;
  @Column(length=15) private String telefono;
  @Column(nullable=false) private String claveHash;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Rol rol=Rol.CIUDADANO;
  @Column(nullable=false) private boolean activo=true;
  @Column(nullable=false, columnDefinition="BOOLEAN DEFAULT FALSE") private boolean correoNotificaciones=false;
  public Long getId(){return id;} public void setId(Long v){id=v;}
  public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
  public String getDni(){return dni;} public void setDni(String v){dni=v;}
  public String getCorreo(){return correo;} public void setCorreo(String v){correo=v;}
  public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;}
  public String getClaveHash(){return claveHash;} public void setClaveHash(String v){claveHash=v;}
  public Rol getRol(){return rol;} public void setRol(Rol v){rol=v;}
  public boolean isActivo(){return activo;} public void setActivo(boolean v){activo=v;}
  public boolean isCorreoNotificaciones(){return correoNotificaciones;} public void setCorreoNotificaciones(boolean v){correoNotificaciones=v;}
}
