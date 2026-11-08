package pe.edu.pc8.controller.api;

import pe.edu.pc8.dto.movil.LoginRequest;
import pe.edu.pc8.dto.movil.LoginResponse;
import pe.edu.pc8.model.Usuario;
import pe.edu.pc8.model.Rol;
import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.security.JwtService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/movil/auth")
public class MobileAuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public MobileAuthController(
            AuthenticationManager authenticationManager,
            UsuarioRepository usuarioRepository,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        Authentication authentication;

        try {

            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.correo().trim(),
                    request.password()
                )
            );

        } catch (AuthenticationException ex) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "message",
                    "Correo o contraseña incorrectos"
                ));
        }

        Usuario usuario = usuarioRepository
            .findByCorreoIgnoreCase(authentication.getName())
            .orElseThrow(() ->
                new IllegalStateException(
                    "Usuario autenticado no encontrado"
                )
            );

        if (usuario.getRol() != Rol.CIUDADANO) {

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "message",
                    "La aplicación móvil está habilitada solo para ciudadanos"
                ));
        }

        String token = jwtService.generateToken(usuario);

        LoginResponse response = new LoginResponse(
            token,
            "Bearer",
            jwtService.getExpirationSeconds(),
            obtenerInformacion(usuario)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> perfil(Authentication authentication) {

        Usuario usuario = usuarioRepository
            .findByCorreoIgnoreCase(authentication.getName())
            .orElseThrow(() ->
                new IllegalStateException(
                    "Usuario no encontrado"
                )
            );

        return ResponseEntity.ok(obtenerInformacion(usuario));
    }

    private LoginResponse.UsuarioInfo obtenerInformacion(
            Usuario usuario) {

        return new LoginResponse.UsuarioInfo(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCorreo(),
            usuario.getRol().name()
        );
    }
}
