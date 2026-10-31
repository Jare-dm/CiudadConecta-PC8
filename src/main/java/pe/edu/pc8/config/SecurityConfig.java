
package pe.edu.pc8.config;

import pe.edu.pc8.repository.UsuarioRepository;

import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository repo) {

        return email -> repo.findByCorreoIgnoreCase(email)
            .map(u -> new org.springframework.security.core.userdetails.User(
                u.getCorreo(),
                u.getClaveHash(),
                u.isActivo(),
                true,
                true,
                true,
                List.of(
                    new SimpleGrantedAuthority(
                        "ROLE_" + u.getRol().name()
                    )
                )
            ))
            .orElseThrow(
                () -> new UsernameNotFoundException(
                    "No existe el usuario"
                )
            );
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth

            // PÁGINAS PÚBLICAS
            .requestMatchers(
                "/",
                "/login",
                "/registro",
                "/accesibilidad",
                "/transparencia",
                "/css/**",
                "/js/**",
                "/favicon.ico",
                "/error"
            ).permitAll()

            // ADMINISTRADOR
            .requestMatchers("/admin/**")
            .hasRole("ADMIN")

            // PANEL MUNICIPAL
            .requestMatchers(
                "/gestion",
                "/gestion/**"
            )
            .hasAnyRole(
                "FUNCIONARIO",
                "MANTENIMIENTO",
                "SUPERVISOR",
                "ADMIN"
            )

            // NUEVO: NOTIFICACIONES PARA DOS ROLES
            .requestMatchers(
                "/notificaciones",
                "/notificaciones/**"
            )
            .hasAnyRole(
                "CIUDADANO",
                "MANTENIMIENTO"
            )

            // PREFERENCIAS SOLO DEL CIUDADANO
            .requestMatchers(
                "/preferencias",
                "/preferencias/**"
            )
            .hasRole("CIUDADANO")

         // CATEGORÍAS PERSONALIZADAS: SOLO CIUDADANOS
            .requestMatchers(
                "/categorias/personalizadas",
                "/categorias/personalizadas/**"
            )
            .hasRole("CIUDADANO")

            // REPORTES DEL CIUDADANO
            .requestMatchers(
                "/mis-reportes",
                "/reportes/nuevo",
                "/reportes/*/confirmar"
            )
            .hasRole("CIUDADANO")

            // OTRAS RUTAS: REQUIEREN SESIÓN
            .anyRequest().authenticated()
        );

        // INICIO DE SESIÓN
        http.formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .defaultSuccessUrl("/inicio", true)
            .failureUrl("/login?error")
            .permitAll()
        );

        // CIERRE DE SESIÓN
        http.logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/?salir")
            .permitAll()
        );

        return http.build();
    }
}
