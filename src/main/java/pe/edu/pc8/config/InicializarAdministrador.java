package pe.edu.pc8.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pe.edu.pc8.model.Rol;
import pe.edu.pc8.model.Usuario;
import pe.edu.pc8.repository.UsuarioRepository;

@Component
public class InicializarAdministrador implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final boolean habilitado;
    private final String nombre;
    private final String dni;
    private final String correo;
    private final String clave;

    public InicializarAdministrador(
            UsuarioRepository usuarios,
            PasswordEncoder encoder,
            @Value("${app.admin.bootstrap.enabled:false}") boolean habilitado,
            @Value("${app.admin.bootstrap.nombre:}") String nombre,
            @Value("${app.admin.bootstrap.dni:}") String dni,
            @Value("${app.admin.bootstrap.correo:}") String correo,
            @Value("${app.admin.bootstrap.password:}") String clave) {

        this.usuarios = usuarios;
        this.encoder = encoder;
        this.habilitado = habilitado;
        this.nombre = nombre;
        this.dni = dni;
        this.correo = correo;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (!habilitado) {
            return;
        }

        // Nunca crear un segundo administrador.
        if (usuarios.existsByRol(Rol.ADMIN)) {
            System.out.println(
                "[ADMIN-BOOTSTRAP] Ya existe un administrador."
            );
            return;
        }

        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String dniLimpio = dni == null ? "" : dni.trim();
        String correoLimpio = correo == null
            ? ""
            : correo.trim().toLowerCase(Locale.ROOT);

        if (nombreLimpio.isBlank() || nombreLimpio.length() > 110) {
            throw new IllegalStateException("Nombre ADMIN inválido.");
        }

        if (!dniLimpio.matches("\\d{8}")) {
            throw new IllegalStateException("El DNI debe tener 8 dígitos.");
        }

        if (!correoLimpio.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || correoLimpio.length() > 150) {
            throw new IllegalStateException("Correo ADMIN inválido.");
        }

        if (clave == null || clave.length() < 12
                || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                "La contraseña ADMIN debe tener al menos 12 caracteres "
                + "y no superar 72 bytes."
            );
        }

        if (usuarios.existsByCorreoIgnoreCase(correoLimpio)
                || usuarios.existsByDni(dniLimpio)) {
            throw new IllegalStateException(
                "El correo o DNI del administrador ya está registrado."
            );
        }

        Usuario admin = new Usuario();

        admin.setNombre(nombreLimpio);
        admin.setDni(dniLimpio);
        admin.setCorreo(correoLimpio);
        admin.setClaveHash(encoder.encode(clave));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);

        usuarios.save(admin);

        System.out.println(
            "[ADMIN-BOOTSTRAP] Cuenta ADMIN inicial creada correctamente."
        );
    }
}
