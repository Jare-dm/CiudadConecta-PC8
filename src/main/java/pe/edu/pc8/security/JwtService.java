package pe.edu.pc8.security;

import pe.edu.pc8.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {

        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                "La duración del JWT debe ser positiva"
            );
        }

        this.expirationMs = expirationMs;

        if (secret == null || secret.isBlank()) {
            this.signingKey = null;
        } else {
            byte[] keyBytes = Decoders.BASE64.decode(secret.trim());

            if (keyBytes.length < 32) {
                throw new IllegalArgumentException(
                    "JWT_SECRET debe contener al menos 32 bytes"
                );
            }

            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            throw new IllegalStateException(
                "Debes configurar la variable de entorno JWT_SECRET"
            );
        }

        return signingKey;
    }

    public String generateToken(Usuario usuario) {

        Instant now = Instant.now();

        return Jwts.builder()
            .subject(usuario.getCorreo())
            .claim("uid", usuario.getId())
            .claim("rol", usuario.getRol().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMs)))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        Claims claims = extractAllClaims(token);

        return claims.getSubject() != null
            && claims.getSubject().equalsIgnoreCase(
                userDetails.getUsername()
            )
            && claims.getExpiration() != null
            && claims.getExpiration().after(new Date())
            && userDetails.isEnabled();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
