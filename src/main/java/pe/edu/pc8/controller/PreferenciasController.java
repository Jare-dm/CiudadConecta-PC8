package pe.edu.pc8.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pc8.model.Usuario;
import pe.edu.pc8.repository.UsuarioRepository;

@Controller @RequestMapping("/preferencias")
public class PreferenciasController {
  private final UsuarioRepository usuarios;
  @Value("${app.mail.enabled:false}") private boolean correoConfigurado;
  public PreferenciasController(UsuarioRepository usuarios){this.usuarios=usuarios;}
  private Usuario actual(Authentication auth){return usuarios.findByCorreoIgnoreCase(auth.getName()).orElseThrow();}

  @GetMapping public String ver(Authentication auth,Model modelo){
    modelo.addAttribute("usuario",actual(auth));
    modelo.addAttribute("correoConfigurado",correoConfigurado);
    return "preferencias";
  }
  @Transactional
  @PostMapping public String guardar(@RequestParam(defaultValue="false") boolean correoNotificaciones,
      Authentication auth,RedirectAttributes flash){
    Usuario usuario=actual(auth);
    usuario.setCorreoNotificaciones(correoNotificaciones);
    usuarios.save(usuario);
    flash.addFlashAttribute("exito","Preferencias guardadas. Las notificaciones de la web permanecen activas.");
    return "redirect:/preferencias";
  }
}
