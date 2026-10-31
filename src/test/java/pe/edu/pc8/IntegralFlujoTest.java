package pe.edu.pc8;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.*;
import pe.edu.pc8.service.*;

class IntegralFlujoTest {
  @Test void alertaDeVencimientoSoloEnProcesosAbiertos(){
    Reporte r=new Reporte();
    r.setFechaLimite(LocalDateTime.now().minusHours(2));
    r.setEstado(Estado.ASIGNADO);
    assertTrue(r.isVencido());
    r.setEstado(Estado.ATENDIDO);
    assertFalse(r.isVencido());
  }
  @Test void ciudadanoConfirmaYElReporteSeCierra(){
    Usuario u=new Usuario();u.setId(1L);u.setRol(Rol.CIUDADANO);
    Reporte r=new Reporte();r.setCiudadano(u);r.setEstado(Estado.ATENDIDO);r.setCodigo("PC8-PRUEBA");
    ReporteRepository repo=mock(ReporteRepository.class);
    HistorialRepository hist=mock(HistorialRepository.class);
    NotificacionRepository avisos=mock(NotificacionRepository.class);
    ReporteService svc=new ReporteService(repo,hist,avisos,mock(UsuarioRepository.class),
      mock(FotoService.class),mock(ApplicationEventPublisher.class),
      mock(CategoriaPersonalizadaService.class),mock(ChorrillosService.class),72);
    svc.confirmarAtencion(r,u,true,null);
    assertEquals(Estado.CERRADO,r.getEstado());
    verify(repo).save(r);
    verify(hist).save(any(HistorialReporte.class));
    verify(avisos).save(any(Notificacion.class));
  }
  @Test void otroCiudadanoNoPuedeConfirmarUnReporteAjeno(){
    Usuario duenio=new Usuario();duenio.setId(1L);duenio.setRol(Rol.CIUDADANO);
    Usuario ajeno=new Usuario();ajeno.setId(2L);ajeno.setRol(Rol.CIUDADANO);
    Reporte r=new Reporte();r.setCiudadano(duenio);r.setEstado(Estado.ATENDIDO);
    ReporteService svc=new ReporteService(mock(ReporteRepository.class),mock(HistorialRepository.class),
      mock(NotificacionRepository.class),mock(UsuarioRepository.class),mock(FotoService.class),
      mock(ApplicationEventPublisher.class),mock(CategoriaPersonalizadaService.class),mock(ChorrillosService.class),72);
    assertThrows(AccessDeniedException.class,()->svc.confirmarAtencion(r,ajeno,true,null));
  }
}
