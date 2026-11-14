package pe.edu.pc8.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pc8.web.ContactoSoporteForm;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Contacto de soporte por correo. No toca Aiven ni crea registros en MySQL. */
@Controller
@RequestMapping("/soporte")
public class SoporteContactoController {

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "Cuenta e inicio de sesión", "Crear un reporte", "Ubicación y mapa",
            "Seguimiento de reportes", "Accesibilidad", "Otro");

    private final JavaMailSender correo;
    private final boolean habilitado;
    private final String destino;
    private final String remitente;

    // Limitación básica por IP para un prototipo; no sustituye un anti-spam en producción.
    private final ConcurrentMap<String, Instant> ultimoEnvio = new ConcurrentHashMap<>();

    public SoporteContactoController(
            JavaMailSender correo,
            @Value("${app.soporte.mail.enabled:false}") boolean habilitado,
            @Value("${app.soporte.mail.to:}") String destino,
            @Value("${app.soporte.mail.from:}") String remitente) {
        this.correo = correo;
        this.habilitado = habilitado;
        this.destino = destino;
        this.remitente = remitente;
    }

    @GetMapping
    public String mostrar(Model modelo, HttpServletRequest solicitud) {
        // Preparar el token CSRF y su sesión antes de renderizar
        // esta página grande, para evitar crear la sesión después
        // de que se haya enviado parte de la respuesta HTTP.
        Object atributoCsrf = solicitud.getAttribute(CsrfToken.class.getName());
        if (atributoCsrf instanceof CsrfToken csrf) {
            csrf.getToken();
        }

        modelo.addAttribute("contactoForm", new ContactoSoporteForm());
        return vista(modelo);
    }

    @PostMapping
    public String enviar(
            @Valid @ModelAttribute("contactoForm") ContactoSoporteForm formulario,
            BindingResult errores,
            Model modelo,
            HttpServletRequest solicitud,
            RedirectAttributes flash) {

        if (!TIPOS_VALIDOS.contains(formulario.getTipo())) {
            errores.rejectValue("tipo", "tipo.invalido", "Selecciona un motivo válido.");
        }
        // Evita que un valor no confiable se inserte como cabecera del mensaje.
        if (formulario.getCorreo() != null &&
                (formulario.getCorreo().contains("\r") || formulario.getCorreo().contains("\n"))) {
            errores.rejectValue("correo", "correo.invalido", "Escribe un correo válido.");
        }
        if (errores.hasErrors()) {
            return vista(modelo);
        }
        if (formulario.getSitioWeb() != null && !formulario.getSitioWeb().isBlank()) {
            modelo.addAttribute("errorEnvio", "No se pudo procesar la consulta. Revisa el formulario.");
            return vista(modelo);
        }
        if (!correoDisponible()) {
            modelo.addAttribute("errorEnvio", "El envío de consultas todavía no está configurado.");
            return vista(modelo);
        }

        Instant ahora = Instant.now();
        String ip = solicitud.getRemoteAddr(); // No se confía en cabeceras X-Forwarded-For.
        Instant anterior = ultimoEnvio.get(ip);
        if (anterior != null && anterior.isAfter(ahora.minusSeconds(60))) {
            modelo.addAttribute("errorEnvio", "Espera un minuto antes de enviar otra consulta.");
            return vista(modelo);
        }

        String referencia = "PC8-SOP-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase(Locale.ROOT);

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destino);
        mensaje.setFrom(remitente);
        mensaje.setReplyTo(formulario.getCorreo().strip());
        mensaje.setSubject("[CiudadConecta PC8] Consulta " + referencia);
        mensaje.setText("Referencia: " + referencia
                + "\nMotivo: " + formulario.getTipo()
                + "\nNombre: " + formulario.getNombre().strip()
                + "\nCorreo de respuesta: " + formulario.getCorreo().strip()
                + "\nAsunto: " + formulario.getAsunto().strip()
                + "\n\nDescripción:\n" + formulario.getMensaje().strip()
                + "\n\nEnviado desde el formulario público de soporte de CiudadConecta PC8.\n");

        try {
            correo.send(mensaje);
        } catch (MailException ex) {
            // No registrar en logs los datos personales ni las credenciales SMTP.
            modelo.addAttribute("errorEnvio", "No se pudo enviar el correo. Intenta nuevamente más tarde.");
            return vista(modelo);
        }

        ultimoEnvio.put(ip, ahora);
        // Limpieza acotada de entradas antiguas para el prototipo.
        if (ultimoEnvio.size() > 2000) {
            ultimoEnvio.entrySet().removeIf(e -> e.getValue().isBefore(ahora.minusSeconds(3600)));
        }
        flash.addFlashAttribute("soporteEnviado", true);
        flash.addFlashAttribute("referenciaSoporte", referencia);
        return "redirect:/soporte#contacto-soporte";
    }

    private boolean correoDisponible() {
        return habilitado && !destino.isBlank() && !remitente.isBlank();
    }

    private String vista(Model modelo) {
        modelo.addAttribute("correoSoporteDisponible", correoDisponible());
        return "soporte";
    }
}
