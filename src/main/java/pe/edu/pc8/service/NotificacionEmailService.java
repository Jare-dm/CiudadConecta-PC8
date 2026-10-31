
package pe.edu.pc8.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pe.edu.pc8.repository.UsuarioRepository;

@Component
@ConditionalOnProperty(
    prefix = "app.mail",
    name = "enabled",
    havingValue = "true"
)
public class NotificacionEmailService {

    private static final Logger log =
        LoggerFactory.getLogger(NotificacionEmailService.class);

    private final JavaMailSender correo;
    private final UsuarioRepository usuarios;
    private final String remitente;

    public NotificacionEmailService(
            JavaMailSender correo,
            UsuarioRepository usuarios,
            @Value("${app.mail.from}") String remitente) {

        this.correo = correo;
        this.usuarios = usuarios;
        this.remitente = remitente;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void avisar(EventoEstadoReporte evento) {

        try {
            usuarios.findById(evento.ciudadanoId())
                .ifPresent(usuario -> {

                    if (!usuario.isCorreoNotificaciones()
                            || !usuario.isActivo()) {
                        return;
                    }

                    SimpleMailMessage mensaje =
                        new SimpleMailMessage();

                    mensaje.setFrom(remitente);
                    mensaje.setTo(usuario.getCorreo());

                    mensaje.setSubject(
                        "CiudadConecta - actualización de tu reporte "
                        + evento.codigo()
                    );

                    mensaje.setText(
                        "Hola, " + usuario.getNombre() + ".\n\n"
                        + "El estado de tu reporte "
                        + evento.codigo()
                        + " ha cambiado a: "
                        + evento.estado() + ".\n\n"
                        + "Ingresa de forma segura a CiudadConecta "
                        + "y consulta el detalle en Mis reportes.\n\n"
                        + "Si no deseas recibir estos correos, "
                        + "desactiva la opción en Preferencias "
                        + "de notificaciones.\n\n"
                        + "CiudadConecta PC8"
                    );

                    correo.send(mensaje);

                    log.info(
                        "Aviso SMTP enviado correctamente "
                        + "para el reporte {}",
                        evento.codigo()
                    );
                });

        } catch (MailAuthenticationException ex) {

            log.warn(
                "ERROR SMTP: autenticación rechazada. "
                + "Revisa SMTP_USER y SMTP_PASSWORD. "
                + "Utiliza una contraseña de aplicación "
                + "de Google vigente y sin espacios."
            );

        } catch (MailException ex) {

            Throwable causa = ex;

            while (causa.getCause() != null
                    && causa.getCause() != causa) {
                causa = causa.getCause();
            }

            log.warn(
                "ERROR SMTP: tipo={}, causa={}",
                ex.getClass().getSimpleName(),
                causa.getClass().getSimpleName()
            );

        } catch (RuntimeException ex) {

            log.error(
                "Error procesando el aviso de correo "
                + "posterior al cambio de estado",
                ex
            );
        }
    }
}
