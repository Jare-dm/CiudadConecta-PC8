package pe.edu.pc8.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.repository.ReporteRepository;
import pe.edu.pc8.web.RegistroForm;
@Controller
public class InicioController {
  private final UsuarioRepository usuarios; private final PasswordEncoder encoder;
  private final ReporteRepository reportes;
  public InicioController(UsuarioRepository usuarios, PasswordEncoder encoder, ReporteRepository reportes){
    this.usuarios=usuarios; this.encoder=encoder; this.reportes=reportes;
  }
  @GetMapping("/")
  public String portada(Model m){
    long total=reportes.count();
    long atendidos=reportes.countByEstado(Estado.ATENDIDO)+reportes.countByEstado(Estado.CERRADO);
    long rechazados=reportes.countByEstado(Estado.RECHAZADO);
    m.addAttribute("inicioTotal",total);
    m.addAttribute("inicioPendientes",Math.max(0L,total-atendidos-rechazados));
    m.addAttribute("inicioAtendidos",atendidos);
    m.addAttribute("inicioRechazados",rechazados);
    return "portada";
  }
  @GetMapping("/login") public String login(Authentication a){return a!=null&&a.isAuthenticated() && !(a instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)?"redirect:/inicio":"login";}
  @GetMapping("/registro") public String registro(Model m){m.addAttribute("form",new RegistroForm());return "registro";}
  @PostMapping("/registro") public String registrar(@Valid @ModelAttribute("form") RegistroForm f,BindingResult errores,Model m){
    if(usuarios.existsByCorreoIgnoreCase(f.getCorreo()))errores.rejectValue("correo","correo.duplicado","El correo ya está registrado");
    if(usuarios.existsByDni(f.getDni()))errores.rejectValue("dni","dni.duplicado","El DNI ya está registrado");
    if(errores.hasErrors())return "registro";
    Usuario u=new Usuario();u.setNombre(f.getNombre().trim());u.setDni(f.getDni());
    u.setCorreo(f.getCorreo().trim().toLowerCase());u.setTelefono(f.getTelefono());u.setClaveHash(encoder.encode(f.getClave()));
    u.setRol(Rol.CIUDADANO);usuarios.save(u);
    return "redirect:/login?registrado";
  }
  @GetMapping("/inicio") public String inicio(Authentication auth){
    return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_FUNCIONARIO")||a.getAuthority().equals("ROLE_MANTENIMIENTO")||a.getAuthority().equals("ROLE_SUPERVISOR")||a.getAuthority().equals("ROLE_ADMIN"))
      ?"redirect:/gestion":"redirect:/mis-reportes";
  }
}
