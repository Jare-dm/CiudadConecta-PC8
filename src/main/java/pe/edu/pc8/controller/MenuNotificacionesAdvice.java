package pe.edu.pc8.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.edu.pc8.repository.NotificacionRepository;
import pe.edu.pc8.repository.UsuarioRepository;

@ControllerAdvice
public class MenuNotificacionesAdvice {
  private final NotificacionRepository avisos;
  private final UsuarioRepository usuarios;
  public MenuNotificacionesAdvice(NotificacionRepository avisos,UsuarioRepository usuarios){
    this.avisos=avisos;this.usuarios=usuarios;
  }
  @ModelAttribute("avisosPendientes")
  public long pendientes(Authentication auth){
    if(auth==null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken ||
      auth.getAuthorities().stream().noneMatch(a->"ROLE_CIUDADANO".equals(a.getAuthority())))return 0L;
    return usuarios.findByCorreoIgnoreCase(auth.getName())
      .map(u->avisos.countByDestinatarioAndLeidaFalse(u)).orElse(0L);
  }
}
