package pe.edu.pc8.dto.movil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank
    @Email
    String correo,

    @NotBlank
    String password

) {}
