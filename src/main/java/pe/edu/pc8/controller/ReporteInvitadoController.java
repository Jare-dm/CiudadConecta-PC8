package pe.edu.pc8.controller;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.pc8.model.Categoria;
import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.service.RegistroInvitadoService;
import pe.edu.pc8.web.ReporteInvitadoForm;

@Controller
@RequestMapping("/invitado")
public class ReporteInvitadoController {

    private static final String TOKEN_SESION = "PC8_FORMULARIO_INVITADO";
    private static final String INICIO_SESION = "PC8_INICIO_INVITADO";
    private final RegistroInvitadoService servicio;

    public ReporteInvitadoController(RegistroInvitadoService servicio) {
        this.servicio = servicio;
    }

    private static void iniciarCsrf(HttpServletRequest request) {
        // Evita el error de inicialización CSRF después del commit de Thymeleaf.
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrf) csrf.getToken();
    }

    private static void categorias(Model model) {
        model.addAttribute("categoriasInvitado", Categoria.values());
    }

    private static boolean comparar(String esperado, String recibido) {
        return esperado != null && recibido != null && MessageDigest.isEqual(
            esperado.getBytes(StandardCharsets.UTF_8),
            recibido.getBytes(StandardCharsets.UTF_8)
        );
    }

    @GetMapping("/reportar")
    public String formulario(Model model, HttpServletRequest request) {
        iniciarCsrf(request);
        ReporteInvitadoForm f = new ReporteInvitadoForm();
        String token = UUID.randomUUID().toString();
        HttpSession session = request.getSession();
        session.setAttribute(TOKEN_SESION, token);
        session.setAttribute(INICIO_SESION, System.currentTimeMillis());
        f.setTokenFormulario(token);
        model.addAttribute("form", f);
        categorias(model);
        return "invitado-reportar";
    }

    @PostMapping("/reportar")
    public String registrar(@Valid @ModelAttribute("form") ReporteInvitadoForm form,
                            BindingResult errores, Model model,
                            HttpServletRequest request, RedirectAttributes flash) {
        iniciarCsrf(request);
        categorias(model);
        HttpSession session = request.getSession(false);
        String esperado = session == null ? null : (String) session.getAttribute(TOKEN_SESION);
        Long inicio = session == null ? null : (Long) session.getAttribute(INICIO_SESION);
        if (!comparar(esperado, form.getTokenFormulario()) || inicio == null) {
            model.addAttribute("errorInvitado", "El formulario expiró. Vuelve a abrir 'Reportar como invitado'.");
            return "invitado-reportar";
        }
        if (System.currentTimeMillis() - inicio < 3_000L) {
            model.addAttribute("errorInvitado", "Revisa los datos del reporte antes de enviarlo.");
            return "invitado-reportar";
        }
        if (errores.hasErrors()) return "invitado-reportar";

        try {
            // No usar cabeceras X-Forwarded-For que un cliente pueda falsificar.
            // Para despliegues con proxy real, configurar forwarded headers SOLO
            // en un proxy confiable y protección antiabuso distribuida.
            Reporte nuevo = servicio.registrar(form, request.getRemoteAddr());
            session.removeAttribute(TOKEN_SESION);
            session.removeAttribute(INICIO_SESION);
            flash.addFlashAttribute("codigoInvitado", nuevo.getCodigo());
            return "redirect:/invitado/recibido";
        } catch (IOException | IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorInvitado", ex.getMessage());
            return "invitado-reportar";
        } catch (org.springframework.dao.DataAccessException ex) {
            model.addAttribute("errorInvitado", "No fue posible guardar el reporte en este momento. Vuelve a intentarlo.");
            return "invitado-reportar";
        }
    }

    @GetMapping("/recibido")
    public String recibido(Model model, HttpServletRequest request) {
        iniciarCsrf(request);
        if (!model.containsAttribute("codigoInvitado")) {
            return "redirect:/invitado/reportar";
        }
        return "invitado-recibido";
    }
}
