package pe.edu.pc8.controller;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.pc8.service.ChorrillosService;
@RestController
public class ChorrillosController {
  private final ChorrillosService servicio;
  public ChorrillosController(ChorrillosService servicio){this.servicio=servicio;}
  @GetMapping(value="/api/distritos/chorrillos",produces="application/geo+json")
  public ResponseEntity<?> obtener(){
    try {return ResponseEntity.ok(servicio.geojson());}
    catch(IllegalStateException ex){return ResponseEntity.status(503).contentType(MediaType.TEXT_PLAIN).body(ex.getMessage());}
  }
}
