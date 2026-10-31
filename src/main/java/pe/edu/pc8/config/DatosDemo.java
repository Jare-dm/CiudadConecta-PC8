
package pe.edu.pc8.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.model.Rol;
import pe.edu.pc8.model.Usuario;

@Configuration
public class DatosDemo {

    @Bean
    CommandLineRunner usuariosDemo(
            UsuarioRepository repo,
            PasswordEncoder codificador,
            @Value("${app.demo.enabled:false}") boolean demo,
            @Value("${app.demo.password:}") String claveDemo) {

        return args -> {

            if (!demo) {
                return;
            }

            if (claveDemo == null || claveDemo.isBlank()) {
                throw new IllegalStateException(
                    "Las cuentas DEMO requieren configurar DEMO_PASSWORD."
                );
            }

            crear(repo, codificador, "Ciudadano de Prueba",
                    "90000001", "demo.ciudadano@example.com",
                    claveDemo, Rol.CIUDADANO);

            crear(repo, codificador, "Funcionario Municipal",
                    "90000002", "demo.funcionario@example.com",
                    claveDemo, Rol.FUNCIONARIO);

            crear(repo, codificador, "Administrador de Prueba",
                    "90000003", "demo.admin@example.com",
                    claveDemo, Rol.ADMIN);

            crear(repo, codificador, "Personal de Mantenimiento",
                    "90000004", "demo.mantenimiento@example.com",
                    claveDemo, Rol.MANTENIMIENTO);

            crear(repo, codificador, "Supervisor Municipal",
                    "90000005", "demo.supervisor@example.com",
                    claveDemo, Rol.SUPERVISOR);

            System.out.println(
                "ATENCION: cuentas DEMO activas. No publicar este perfil en Internet."
            );
        };
    }

    private void crear(
            UsuarioRepository r,
            PasswordEncoder encoder,
            String nombre,
            String dni,
            String correo,
            String claveDemo,
            Rol rol) {

        if (r.existsByCorreoIgnoreCase(correo)) {
            return;
        }

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(correo);
        u.setClaveHash(encoder.encode(claveDemo));
        u.setRol(rol);

        r.save(u);
    }
}
