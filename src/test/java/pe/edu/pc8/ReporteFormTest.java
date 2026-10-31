package pe.edu.pc8;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import pe.edu.pc8.web.ReporteForm;
import static org.junit.jupiter.api.Assertions.*;
class ReporteFormTest {
  @Test void coordenadasFueraDeRangoSeRechazan(){
    try(var validador=Validation.buildDefaultValidatorFactory()){
      var f=new ReporteForm();f.setLatitud(91.0);f.setLongitud(-181.0);
      assertTrue(validador.getValidator().validate(f).stream().anyMatch(v->v.getPropertyPath().toString().equals("latitud")));
      assertTrue(validador.getValidator().validate(f).stream().anyMatch(v->v.getPropertyPath().toString().equals("longitud")));
    }
  }
}
