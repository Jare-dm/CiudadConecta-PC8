package pe.edu.pc8;
import org.junit.jupiter.api.Test;
import pe.edu.pc8.model.Estado;
import pe.edu.pc8.service.ReporteService;
import static org.junit.jupiter.api.Assertions.*;
class ReporteServiceTest {
  @Test void transicionValida(){assertTrue(ReporteService.transicionPermitida(Estado.REGISTRADO,Estado.EN_REVISION));}
  @Test void noPermitirSaltarEstados(){assertFalse(ReporteService.transicionPermitida(Estado.REGISTRADO,Estado.CERRADO));}
  @Test void permitirRechazarAlInicio(){assertTrue(ReporteService.transicionPermitida(Estado.REGISTRADO,Estado.RECHAZADO));}
  @Test void noCambiarCerrados(){assertFalse(ReporteService.transicionPermitida(Estado.CERRADO,Estado.EN_ATENCION));}
}
