package pe.edu.pc8.controller;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
@ControllerAdvice
class ErroresController {
  @ExceptionHandler(IllegalArgumentException.class)
  public String noEncontrado(IllegalArgumentException ex,Model model){model.addAttribute("mensaje",ex.getMessage());return "error-app";}
}
