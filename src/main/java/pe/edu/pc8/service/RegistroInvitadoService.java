package pe.edu.pc8.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pc8.model.Categoria;
import pe.edu.pc8.model.Estado;
import pe.edu.pc8.model.HistorialReporte;
import pe.edu.pc8.model.Reporte;
import pe.edu.pc8.model.Rol;
import pe.edu.pc8.model.Usuario;
import pe.edu.pc8.repository.HistorialRepository;
import pe.edu.pc8.repository.ReporteRepository;
import pe.edu.pc8.repository.UsuarioRepository;
import pe.edu.pc8.web.ReporteForm;
import pe.edu.pc8.web.ReporteInvitadoForm;

/**
 * Registro anónimo compatible con el esquema de reportes existente.
 * Usa una cuenta TÉCNICA INACTIVA como FK obligatoria; NO crea credenciales
 * para el visitante ni concede acceso a los reportes de otros invitados.
 *
 * El limitador por IP funciona en memoria: adecuado para el prototipo local.
 * Para producción multi-instancia sustituir por almacenamiento compartido,
 * CAPTCHA y controles antiabuso en el borde.
 */
@Service
public class RegistroInvitadoService {

    private static final String CORREO_TECNICO = "pc8-invitado-sistema@ciudadconecta.invalid";
    private static final String DNI_TECNICO = "00000000";

    private static final Pattern DATO_SENSIBLE = Pattern.compile(
        "(?i)[A-Z0-9._%+\\-]+@[A-Z0-9.\\-]+\\.[A-Z]{2,}" +
        "|(?<!\\d)(?:\\+?51[ .-]?)?9(?:[ .-]?\\d){8}(?!\\d)" +
        "|(?i)\\b(?:DNI|documento(?: de identidad)?)\\s*[:#-]?\\s*\\d{8}\\b"
    );

    private final UsuarioRepository usuarios;
    private final ReporteRepository reportes;
    private final HistorialRepository historial;
    private final ReporteService reporteService;
    private final FotoService fotos;
    private final PasswordEncoder encoder;

    private final byte[] salIp = new byte[32];
    private final Map<String, ArrayDeque<Long>> enviosPorIp = new ConcurrentHashMap<>();

    public RegistroInvitadoService(UsuarioRepository usuarios,
                                   ReporteRepository reportes,
                                   HistorialRepository historial,
                                   ReporteService reporteService,
                                   FotoService fotos,
                                   PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.reportes = reportes;
        this.historial = historial;
        this.reporteService = reporteService;
        this.fotos = fotos;
        this.encoder = encoder;
        new SecureRandom().nextBytes(salIp);
    }

    /** Cuenta con acceso bloqueado; se crea UNA sola vez, no una por visitante. */
    private Usuario cuentaTecnica() {
        return usuarios.findByCorreoIgnoreCase(CORREO_TECNICO)
            .map(u -> {
                if (u.isActivo() || u.getRol() != Rol.CIUDADANO ||
                    !DNI_TECNICO.equals(u.getDni())) {
                    throw new IllegalStateException("Cuenta técnica de invitados incompatible. Revisa su configuración.");
                }
                return u;
            })
            .orElseGet(() -> {
                if (usuarios.existsByDni(DNI_TECNICO)) {
                    throw new IllegalStateException("El DNI reservado para la cuenta técnica está ocupado. No se guardó el reporte.");
                }
                Usuario u = new Usuario();
                u.setNombre("Invitado anónimo (sistema)");
                u.setCorreo(CORREO_TECNICO);
                u.setDni(DNI_TECNICO);
                // Nunca se comunica esta contraseña aleatoria; la cuenta sigue desactivada.
                u.setClaveHash(encoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
                u.setRol(Rol.CIUDADANO);
                u.setActivo(false);
                u.setCorreoNotificaciones(false);
                return usuarios.saveAndFlush(u);
            });
    }

    private static void comprobarPrivacidad(String texto) {
        if (texto != null && DATO_SENSIBLE.matcher(texto).find()) {
            throw new IllegalArgumentException(
                "Por privacidad, no escribas correo, teléfono ni DNI en el reporte."
            );
        }
    }

    private String claveDeIp(String ip) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(salIp);
            byte[] hash = sha.digest((ip == null ? "" : ip).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Protección de frecuencia no disponible", e);
        }
    }

    private void comprobarLimites(ArrayDeque<Long> envios, long ahora) {
        envios.removeIf(t -> t < ahora - 86_400_000L);
        if (!envios.isEmpty() && ahora - envios.peekLast() < 120_000L) {
            throw new IllegalStateException("Espera 2 minutos antes de registrar otro reporte como invitado.");
        }
        long hora = envios.stream().filter(t -> t >= ahora - 3_600_000L).count();
        if (hora >= 3) throw new IllegalStateException("Límite de 3 reportes por hora alcanzado.");
        if (envios.size() >= 8) throw new IllegalStateException("Límite de 8 reportes en 24 horas alcanzado.");
    }

    /**
     * Mantiene todas las reglas geográficas y de categorías del proyecto.
     * La sincronización es por IP, de modo que dos peticiones simultáneas
     * no puedan saltarse fácilmente el límite de frecuencia.
     */
    @Transactional(rollbackFor = Exception.class)
    public Reporte registrar(ReporteInvitadoForm f, String ip) throws IOException {
        String clave = claveDeIp(ip);
        ArrayDeque<Long> envios = enviosPorIp.computeIfAbsent(clave, k -> new ArrayDeque<>());
        Reporte resultado;
        synchronized (envios) {
            long ahora = Instant.now().toEpochMilli();
            comprobarLimites(envios, ahora);

            if (f.getSitioWeb() != null && !f.getSitioWeb().isBlank()) {
                throw new IllegalArgumentException("No se pudo procesar la solicitud.");
            }
            // El invitado utiliza únicamente las categorías base, no propone ni crea nuevas.
            if (f.getCategoriaOpcion() == null || !f.getCategoriaOpcion().startsWith("BASE:")) {
                throw new IllegalArgumentException("Selecciona una categoría del catálogo municipal.");
            }
            ReporteForm datos = new ReporteForm();
            datos.setCategoriaOpcion(f.getCategoriaOpcion());
            datos.setDescripcion(f.getDescripcion());
            datos.setUbicacion(f.getUbicacion());
            datos.setLatitud(f.getLatitud());
            datos.setLongitud(f.getLongitud());
            datos.setIdentidadReservada(true);
            reporteService.validarCategoriaDisponible(datos);
            reporteService.validarUbicacion(datos);
            comprobarPrivacidad(f.getDescripcion());
            comprobarPrivacidad(f.getUbicacion());

            // El usuario técnico siempre está desactivado, de modo que no
            // puede iniciar sesión ni entrar en «Mis reportes».
            Usuario tecnico = cuentaTecnica();
            String nombreFoto = fotos.guardar(f.getFoto());

            Reporte r = new Reporte();
            r.setCiudadano(tecnico);
            r.setCategoria(Categoria.valueOf(f.getCategoriaOpcion().substring(5)));
            r.setDescripcion(f.getDescripcion().strip());
            r.setUbicacion(f.getUbicacion().strip());
            r.setLatitud(f.getLatitud());
            r.setLongitud(f.getLongitud());
            r.setArchivoImagen(nombreFoto);
            r.setIdentidadReservada(true);
            r.setEstado(Estado.REGISTRADO);
            r.setCodigo("PC8-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT));
            reportes.saveAndFlush(r);

            HistorialReporte h = new HistorialReporte();
            h.setReporte(r);
            h.setEstado(Estado.REGISTRADO);
            h.setAutor("Invitado anónimo");
            h.setDetalle("Reporte ciudadano sin registro de cuenta");
            historial.save(h);

            envios.addLast(ahora);
            resultado = r;
        }
        // Limpieza fuera del bloqueo de la IP actual: evita interbloqueos
        // entre peticiones concurrentes de direcciones diferentes.
        if (enviosPorIp.size() > 3_000) {
            long limite = Instant.now().toEpochMilli() - 86_400_000L;
            enviosPorIp.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    e.getValue().removeIf(t -> t < limite);
                    return e.getValue().isEmpty();
                }
            });
        }
        return resultado;
    }
}
