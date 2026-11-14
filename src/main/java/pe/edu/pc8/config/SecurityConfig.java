
package pe.edu.pc8.config;

import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.security.JwtService;
import pe.edu.pc8.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    // ==========================================
    // API MOVIL: AUTENTICACION JWT
    // ==========================================

    @Bean
    @Order(1)
    SecurityFilterChain mobileApiFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService)
            throws Exception {

        http
            .securityMatcher("/api/movil/**")

            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .formLogin(AbstractHttpConfigurer::disable)

            .httpBasic(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/movil/auth/login"
                ).permitAll()

                .anyRequest().hasRole("CIUDADANO")
            )

            .exceptionHandling(ex -> ex

                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(
                        HttpStatus.UNAUTHORIZED
                    )
                )

                .accessDeniedHandler(
                    (request, response, exception) ->
                        response.sendError(
                            HttpStatus.FORBIDDEN.value()
                        )
                )
            )

            .addFilterBefore(
                new JwtAuthenticationFilter(
                    jwtService,
                    userDetailsService
                ),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // ==========================================
    // APLICACION WEB
    // ==========================================

    @Bean
    @Order(2)
    SecurityFilterChain webFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth

            // =====================================
            // PAGINAS PUBLICAS EXISTENTES
            // =====================================
            .requestMatchers(
                "/",
                "/login",
                "/registro",
                "/que-reportar",
                "/manual",
                "/invitado",
                "/soporte",
                "/accesibilidad",
                "/transparencia",
                "/transparencia/mapa-publico",
                "/css/**",
                "/js/**",
                "/favicon.ico",
                "/error"
            ).permitAll()

            // =====================================
            // MODO INVITADO: CONSULTAR FORMULARIO
            // =====================================
            .requestMatchers(
                HttpMethod.GET,
                "/invitado/reportar",
                "/invitado/recibido",
                "/api/distritos/chorrillos"
            ).permitAll()

            // =====================================
            // MODO INVITADO: ENVIAR REPORTE
            // =====================================
            .requestMatchers(
                HttpMethod.POST,
                "/invitado/reportar"
            ).permitAll()

            // =====================================
            // ADMINISTRADOR
            // =====================================
            .requestMatchers(
                "/admin/**"
            ).hasRole("ADMIN")

            // =====================================
            // GESTION MUNICIPAL
            // =====================================
            .requestMatchers(
                "/gestion",
                "/gestion/**"
            ).hasAnyRole(
                "FUNCIONARIO",
                "MANTENIMIENTO",
                "SUPERVISOR",
                "ADMIN"
            )

            // =====================================
            // NOTIFICACIONES
            // =====================================
            .requestMatchers(
                "/notificaciones",
                "/notificaciones/**"
            ).hasAnyRole(
                "CIUDADANO",
                "MANTENIMIENTO"
            )

            // =====================================
            // PREFERENCIAS DEL CIUDADANO
            // =====================================
            .requestMatchers(
                "/preferencias",
                "/preferencias/**"
            ).hasRole("CIUDADANO")

            // =====================================
            // CATEGORIAS PERSONALIZADAS
            // =====================================
            .requestMatchers(
                "/categorias/personalizadas",
                "/categorias/personalizadas/**"
            ).hasRole("CIUDADANO")

            // =====================================
            // REGISTRO Y SEGUIMIENTO DE REPORTES
            // PARA CIUDADANOS AUTENTICADOS
            // =====================================
            .requestMatchers(
                "/mis-reportes",
                "/reportes/nuevo",
                "/reportes/*/confirmar"
            ).hasRole("CIUDADANO")

            // =====================================
            // DEMAS RUTAS: REQUIEREN INICIAR SESION
            // =====================================
            .anyRequest().authenticated()
        );

        // ==========================================
        // INICIO DE SESION WEB
        // ==========================================
        http.formLogin(form -> form

            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .defaultSuccessUrl("/inicio", true)
            .failureUrl("/login?error")
            .permitAll()
        );

        // ==========================================
        // CIERRE DE SESION WEB
        // ==========================================
        http.logout(logout -> logout

            .logoutUrl("/logout")
            .logoutSuccessUrl("/?salir")
            .permitAll()
        );

        return http.build();
    }
}
