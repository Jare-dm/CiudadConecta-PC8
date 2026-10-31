package pe.edu.pc8.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller public class AccesibilidadController {
  @GetMapping("/accesibilidad") public String ayuda(){return "accesibilidad";}
}
