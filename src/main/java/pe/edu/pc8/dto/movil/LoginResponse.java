package pe.edu.pc8.dto.movil;

public record LoginResponse(

    String accessToken,

    String tokenType,

    long expiresIn,

    UsuarioInfo usuario

) {

    public record UsuarioInfo(

        Long id,

        String nombre,

        String correo,

        String rol

    ) {}
}
