
package pe.edu.pc8.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pc8.model.*;
import pe.edu.pc8.repository.*;
import pe.edu.pc8.web.*;

@Service
public class ReporteService {

	private final ReporteRepository reportes;
	private final HistorialRepository historial;
	private final NotificacionRepository notificaciones;
	private final UsuarioRepository usuarios;
	private final FotoService fotos;
	private final ApplicationEventPublisher eventos;
	private final CategoriaPersonalizadaService categoriasPersonalizadas;
	private final ChorrillosService chorrillos;
	private final int horasPlazo;

	public ReporteService(ReporteRepository reportes, HistorialRepository historial,
			NotificacionRepository notificaciones, UsuarioRepository usuarios, FotoService fotos,
			ApplicationEventPublisher eventos, CategoriaPersonalizadaService categoriasPersonalizadas,
			ChorrillosService chorrillos, @Value("${app.sla.horas-default:72}") int horasPlazo) {

		this.reportes = reportes;
		this.historial = historial;
		this.notificaciones = notificaciones;
		this.usuarios = usuarios;
		this.fotos = fotos;
		this.eventos = eventos;
		this.categoriasPersonalizadas = categoriasPersonalizadas;
		this.chorrillos = chorrillos;

		if (horasPlazo < 1 || horasPlazo > 8760) {
			throw new IllegalArgumentException("Plazo municipal fuera de rango");
		}

		this.horasPlazo = horasPlazo;
	}

	// ===========================================
	// VALIDACIÓN GEOGRÁFICA
	// ===========================================

	public void validarUbicacion(ReporteForm form) {

		if (!chorrillos.contiene(form.getLatitud(), form.getLongitud())) {

			throw new IllegalArgumentException("Selecciona una ubicación dentro del distrito de Chorrillos");
		}
	}

	// ==========================================
	// NUEVO: CONTROL DE FRECUENCIA DE REPORTES
	// ==========================================

	public void verificarFrecuencia(Usuario ciudadano) {

		if (ciudadano == null || !ciudadano.isActivo() || ciudadano.getRol() != Rol.CIUDADANO) {

			throw new AccessDeniedException("No autorizado para registrar reportes");
		}

		LocalDateTime ahora = LocalDateTime.now();

		// ======================================
		// MÍNIMO 1 MINUTO ENTRE REPORTES
		// ======================================

		long ultimoMinuto = reportes.countByCiudadanoAndCreadoEnGreaterThanEqual(ciudadano, ahora.minusMinutes(1));

		if (ultimoMinuto >= 1) {

			throw new IllegalStateException(
					"Ya registraste un reporte recientemente. " + "Espera al menos un minuto antes de enviar otro.");
		}

		// ======================================
		// MÁXIMO 5 REPORTES POR HORA
		// ======================================

		long ultimaHora = reportes.countByCiudadanoAndCreadoEnGreaterThanEqual(ciudadano, ahora.minusHours(1));

		if (ultimaHora >= 5) {

			throw new IllegalStateException(
					"Alcanzaste el límite de 5 reportes " + "en una hora. Intenta nuevamente más tarde.");
		}

		// ======================================
		// MÁXIMO 15 REPORTES EN 24 HORAS
		// ======================================

		long ultimas24Horas = reportes.countByCiudadanoAndCreadoEnGreaterThanEqual(ciudadano, ahora.minusHours(24));

		if (ultimas24Horas >= 15) {

			throw new IllegalStateException(
					"Alcanzaste el límite de 15 reportes " + "en 24 horas. Intenta nuevamente más tarde.");
		}
	}

	// ==========================================
	// VALIDAR CATEGORÍA DISPONIBLE
	// ==========================================

	public void validarCategoriaDisponible(ReporteForm form) {

		if (form == null) {
			throw new IllegalArgumentException("Formulario de reporte no válido");
		}

		String opcion = form.getCategoriaOpcion();

		if (opcion == null || opcion.isBlank()) {
			throw new IllegalArgumentException("Selecciona una categoría");
		}

		// Categorías predeterminadas del sistema.
		if (opcion.startsWith("BASE:")) {

			try {
				Categoria.valueOf(opcion.substring(5));
				return;

			} catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Categoría no válida");
			}
		}

		// Categorías propuestas por ciudadanos.
		if (opcion.startsWith("P:")) {

			final long id;

			try {
				id = Long.parseLong(opcion.substring(2));

			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Categoría personalizada no válida");
			}

			CategoriaPersonalizada personalizada = categoriasPersonalizadas.obtener(id);

			// IMPORTANTE:
			// El ciudadano solo puede registrar
			// reportes con categorías aprobadas.
			if (personalizada.getEstado() != EstadoCategoriaPersonalizada.APROBADA) {

				throw new IllegalArgumentException(
						"Esta categoría todavía no está aprobada " + "para registrar reportes. Selecciona otra.");
			}

			return;
		}

		throw new IllegalArgumentException("Selecciona una categoría válida");
	}

	// ==========================================
	// VALIDAR SUBCATEGORÍA DEL REPORTE
	// ==========================================

	public Subcategoria validarSubcategoria(ReporteForm form) {

		String opcion = form.getSubcategoriaOpcion();

		// Opcional para mantener compatibilidad.
		if (opcion == null || opcion.isBlank()) {
			return null;
		}

		String categoriaOpcion = form.getCategoriaOpcion();

		// Una subcategoría solamente puede
		// pertenecer a una categoría municipal.

		if (categoriaOpcion == null || !categoriaOpcion.startsWith("BASE:")) {

			throw new IllegalArgumentException(
					"La subcategoría seleccionada no corresponde " + "a la categoría principal");
		}

		final Categoria categoriaPrincipal;
		final Subcategoria subcategoria;

		try {

			categoriaPrincipal = Categoria.valueOf(categoriaOpcion.substring(5));

			subcategoria = Subcategoria.valueOf(opcion.trim());

		} catch (IllegalArgumentException ex) {

			throw new IllegalArgumentException("La subcategoría seleccionada no es válida");
		}

		// Comprobar que coincidan.

		if (subcategoria.getCategoria() != categoriaPrincipal) {

			throw new IllegalArgumentException("La subcategoría no pertenece " + "a la categoría seleccionada");
		}

		return subcategoria;
	}

	// ===========================================
	// REGISTRO DE REPORTE CIUDADANO
	// ===========================================

	@Transactional
	public Reporte crear(ReporteForm form, Usuario ciudadano, String foto) {

		if (ciudadano.getRol() != Rol.CIUDADANO) {
			throw new IllegalArgumentException("Solo un ciudadano puede registrar reportes");
		}

		// NUEVO: COMPROBAR LÍMITES ANTES DE CREAR
		verificarFrecuencia(ciudadano);

		validarUbicacion(form);
		validarDatosPersonales(form.getDescripcion());

		// NUEVO: IMPEDIR CATEGORÍAS PENDIENTES O RECHAZADAS
		validarCategoriaDisponible(form);

		Reporte r = new Reporte();
		r.setCiudadano(ciudadano);

		String opcion = form.getCategoriaOpcion();

		if (opcion == null) {
			throw new IllegalArgumentException("Elige una categoría");
		}

		if (opcion.startsWith("BASE:")) {

			try {
				r.setCategoria(Categoria.valueOf(opcion.substring(5)));

			} catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Categoría no válida");
			}

		} else if (opcion.startsWith("P:")) {

			try {
				long id = Long.parseLong(opcion.substring(2));

				r.setCategoriaPersonalizada(categoriasPersonalizadas.obtener(id));

			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Categoría no válida");
			}

			r.setCategoria(Categoria.OTRO);

		} else {
			throw new IllegalArgumentException("Selecciona una categoría existente o agrega una nueva"

			);

		}

		// NUEVO: validar y guardar la subcategoría.
		r.setSubcategoria(validarSubcategoria(form));

		r.setDescripcion(form.getDescripcion().trim());

		r.setUbicacion(form.getUbicacion().trim());

		r.setLatitud(form.getLatitud());
		r.setLongitud(form.getLongitud());
		r.setArchivoImagen(foto);

		// NUEVO: GUARDAR LA PRIVACIDAD ELEGIDA
		r.setIdentidadReservada(form.isIdentidadReservada());

		r.setCodigo("PC8-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

		reportes.save(r);

		anotar(r, Estado.REGISTRADO, ciudadano.getCorreo(), "Reporte registrado");

		return r;
	}

	// ===========================================
	// VERIFICACIÓN DE TRANSICIONES POR ROL
	// ===========================================

	private void verificarTransicionPorRol(Estado antes, Estado nuevo, Rol rol) {

		// Mantener el mismo estado permite guardar
		// observaciones o cambios administrativos.
		if (antes == nuevo) {
			return;
		}

		if (!transicionPermitida(antes, nuevo)) {
			throw new IllegalArgumentException(
					"Transición no permitida desde " + antes.getEtiqueta() + " hacia " + nuevo.getEtiqueta());
		}

		if (rol == Rol.MANTENIMIENTO) {

			boolean permitido = (antes == Estado.ASIGNADO && nuevo == Estado.EN_ATENCION) ||

					(antes == Estado.EN_ATENCION && nuevo == Estado.ATENDIDO);

			if (!permitido) {
				throw new AccessDeniedException(
						"Mantenimiento solo puede iniciar " + "o finalizar una atención asignada");
			}

			return;
		}

		if (rol == Rol.FUNCIONARIO || rol == Rol.SUPERVISOR || rol == Rol.ADMIN) {

			boolean permitido = (antes == Estado.REGISTRADO
					&& (nuevo == Estado.EN_REVISION || nuevo == Estado.RECHAZADO)) ||

					(antes == Estado.EN_REVISION && (nuevo == Estado.ASIGNADO || nuevo == Estado.RECHAZADO)) ||

					(antes == Estado.ATENDIDO && nuevo == Estado.CERRADO
							&& (rol == Rol.SUPERVISOR || rol == Rol.ADMIN));

			if (!permitido) {
				throw new AccessDeniedException("Tu rol no puede realizar " + "esta transición de estado");
			}

			return;
		}

		throw new AccessDeniedException("Tu rol no puede modificar reportes");
	}

	// ===========================================
	// ACTUALIZACIÓN MUNICIPAL
	// ===========================================

	@Transactional
	public void actualizar(Reporte r, ActualizacionForm form, Usuario actor) throws IOException {

		Rol rol = actor.getRol();

		boolean mantenimiento = rol == Rol.MANTENIMIENTO;

		boolean coordinador = rol == Rol.FUNCIONARIO || rol == Rol.SUPERVISOR || rol == Rol.ADMIN;

		if (!actor.isActivo() || (!coordinador && !mantenimiento)) {

			throw new AccessDeniedException("No autorizado a gestionar incidencias");
		}

		if (mantenimiento
				&& (r.getResponsable() == null || !Objects.equals(r.getResponsable().getId(), actor.getId()))) {

			throw new AccessDeniedException("Este reporte no está asignado a tu cuenta");
		}

		Estado antes = r.getEstado();
		Estado nuevo = form.getEstado();

		if (nuevo == null) {
			throw new IllegalArgumentException("Selecciona un estado");
		}

		if (antes == Estado.CERRADO || antes == Estado.RECHAZADO) {

			throw new IllegalArgumentException("Un reporte cerrado o rechazado no puede modificarse");
		}

		// Nueva validación por rol.
		verificarTransicionPorRol(antes, nuevo, rol);

		// =======================================
		// RESTRICCIONES PARA MANTENIMIENTO
		// =======================================

		if (mantenimiento) {

			if (form.getArea() != null && form.getArea() != r.getArea()) {

				throw new AccessDeniedException("Mantenimiento no puede cambiar el área");
			}

			if (form.getResponsableId() != null && !Objects.equals(form.getResponsableId(), actor.getId())) {

				throw new AccessDeniedException("Mantenimiento no puede cambiar el responsable");
			}
		}

		// =======================================
		// VALIDACIONES DEL COMENTARIO
		// =======================================

		String comentario = form.getObservacion() == null ? "" : form.getObservacion().trim();
		validarDatosPersonales(comentario);

		if (nuevo == Estado.RECHAZADO && comentario.isBlank()) {

			throw new IllegalArgumentException("Debes registrar una justificación para rechazar");
		}

		if (nuevo == Estado.CERRADO && comentario.isBlank()) {

			throw new IllegalArgumentException("El cierre excepcional debe incluir una justificación");
		}

		// =======================================
		// RESPONSABLE MUNICIPAL
		// =======================================

		Usuario responsableAnterior = r.getResponsable();

		Usuario responsable = responsableAnterior;

		if (!mantenimiento && form.getResponsableId() != null) {

			responsable = usuarios.findById(form.getResponsableId())
					.orElseThrow(() -> new IllegalArgumentException("Responsable no encontrado"));

			if (!responsable.isActivo()
					|| (responsable.getRol() != Rol.FUNCIONARIO && responsable.getRol() != Rol.MANTENIMIENTO)) {

				throw new IllegalArgumentException(
						"Selecciona un funcionario o trabajador " + "de mantenimiento activo");
			}
		}

		AreaMunicipal area = form.getArea() != null && !mantenimiento ? form.getArea() : r.getArea();

		if ((nuevo == Estado.ASIGNADO || nuevo == Estado.EN_ATENCION || nuevo == Estado.ATENDIDO
				|| nuevo == Estado.CERRADO) && (area == null || responsable == null)) {

			throw new IllegalArgumentException("Debes seleccionar un área y una persona responsable");
		}

		// =======================================
		// EVIDENCIA OBLIGATORIA PARA ATENDIDO
		// =======================================

		if (nuevo == Estado.ATENDIDO) {

			if (comentario.isBlank()) {
				throw new IllegalArgumentException("Describe el trabajo realizado");
			}

			if (r.getArchivoAtencion() == null
					&& (form.getFotoAtencion() == null || form.getFotoAtencion().isEmpty())) {

				throw new IllegalArgumentException("Adjunta una fotografía como evidencia de la atención");
			}
		}

		// =======================================
		// GUARDAR FOTOGRAFÍA
		// =======================================

		String fotoAtencion = fotos.guardar(form.getFotoAtencion());

		if (fotoAtencion != null) {
			r.setArchivoAtencion(fotoAtencion);
		}

		// =======================================
		// ACTUALIZAR REPORTE
		// =======================================

		boolean nuevoResponsable = responsable != null
				&& (responsableAnterior == null || !Objects.equals(responsableAnterior.getId(), responsable.getId()));

		r.setArea(area);
		r.setResponsable(responsable);

		if (responsable != null && (r.getFechaLimite() == null || nuevoResponsable)) {

			r.setFechaLimite(LocalDateTime.now().plusHours(horasPlazo));
		}

		r.setEstado(nuevo);
		r.setObservacion(comentario);
		r.setActualizadoEn(LocalDateTime.now());

		reportes.save(r);

		// =======================================
		// REGISTRAR HISTORIAL
		// =======================================

		String detalle = (nuevoResponsable ? "Responsable asignado: " + responsable.getNombre() + ". " : "")
				+ comentario;

		anotar(r, nuevo, actor.getCorreo(), detalle);

		// =======================================
		// NOTIFICAR AL CIUDADANO
		// =======================================

		if (antes != nuevo) {

			notificarCiudadano(r, "Tu reporte " + r.getCodigo() + " cambió a «" + nuevo.getEtiqueta() + "».");
		}

		// =======================================
		// NUEVO: NOTIFICAR A MANTENIMIENTO
		// =======================================

		boolean etapaAsignada = nuevo == Estado.ASIGNADO || nuevo == Estado.EN_ATENCION || nuevo == Estado.ATENDIDO;

		boolean acabaDeAsignarse = antes != Estado.ASIGNADO && nuevo == Estado.ASIGNADO;

		if (responsable != null && responsable.getRol() == Rol.MANTENIMIENTO && etapaAsignada
				&& (nuevoResponsable || acabaDeAsignarse)) {

			notificarInternamente(responsable, r, "Se te asignó el reporte " + r.getCodigo()
					+ ". Revisa la incidencia en tu panel de mantenimiento.");
		}
	}

	// ===========================================
	// CONFIRMACIÓN CIUDADANA
	// ===========================================

	@Transactional
	public void confirmarAtencion(Reporte r, Usuario ciudadano, boolean acepta, String motivo) {

		if (!ciudadano.isActivo() || ciudadano.getRol() != Rol.CIUDADANO
				|| !Objects.equals(r.getCiudadano().getId(), ciudadano.getId())) {

			throw new AccessDeniedException("Este reporte no te pertenece");
		}

		if (r.getEstado() != Estado.ATENDIDO) {
			throw new IllegalArgumentException("Solo puedes responder a reportes que figuren como atendidos");
		}

		String texto = motivo == null ? "" : motivo.trim();
		validarDatosPersonales(texto);

		if (!acepta && (texto.length() < 10 || texto.length() > 700)) {

			throw new IllegalArgumentException("Explica por qué el problema continúa " + "(10 a 700 caracteres)");
		}

		r.setEstado(acepta ? Estado.CERRADO : Estado.EN_ATENCION);

		r.setActualizadoEn(LocalDateTime.now());

		r.setObservacion(acepta ? "Solución confirmada por el ciudadano" : texto);

		if (!acepta) {
			r.setFechaLimite(LocalDateTime.now().plusHours(horasPlazo));
		}

		reportes.save(r);

		anotar(r, r.getEstado(), ciudadano.getCorreo(),
				acepta ? "Atención confirmada por el ciudadano" : "Revisión solicitada: " + texto);

		notificarCiudadano(r,
				acepta ? "Confirmaste la solución de " + r.getCodigo() : "Solicitaste revisión de " + r.getCodigo());

		// NUEVO: si el ciudadano solicita otra
		// revisión, avisar al mantenimiento asignado.
		if (!acepta && r.getResponsable() != null && r.getResponsable().getRol() == Rol.MANTENIMIENTO) {

			notificarInternamente(r.getResponsable(), r, "El ciudadano solicitó una nueva revisión " + "del reporte "
					+ r.getCodigo() + ". Consulta los comentarios de seguimiento.");
		}
	}

	// ===========================================
	// NOTIFICACIONES INTERNAS
	// ===========================================

	private void notificarInternamente(Usuario destinatario, Reporte reporte, String mensaje) {

		if (destinatario == null || !destinatario.isActivo()) {
			return;
		}

		Notificacion n = new Notificacion();

		n.setDestinatario(destinatario);
		n.setReporte(reporte);
		n.setMensaje(mensaje);

		notificaciones.save(n);
	}

	// ===========================================
	// NOTIFICACIONES DEL CIUDADANO Y CORREO
	// ===========================================

	private void notificarCiudadano(Reporte r, String mensaje) {

		notificarInternamente(r.getCiudadano(), r, mensaje);

		// El servicio de correo recibe el evento.
		// Si está habilitado, lo envía después
		// del commit de la transacción.
		eventos.publishEvent(
				new EventoEstadoReporte(r.getCiudadano().getId(), r.getCodigo(), r.getEstado().getEtiqueta()));
	}

	// ===========================================
	// TRANSICIONES GENERALES
	// ===========================================

	public static boolean transicionPermitida(Estado desde, Estado hacia) {

		if (desde == hacia) {
			return true;
		}

		if (hacia == Estado.RECHAZADO) {
			return desde == Estado.REGISTRADO || desde == Estado.EN_REVISION;
		}

		return switch (desde) {

		case REGISTRADO -> hacia == Estado.EN_REVISION;

		case EN_REVISION -> hacia == Estado.ASIGNADO;

		case ASIGNADO -> hacia == Estado.EN_ATENCION;

		case EN_ATENCION -> hacia == Estado.ATENDIDO;

		case ATENDIDO -> hacia == Estado.CERRADO;

		default -> false;
		};
	}

	// ===========================================
	// HISTORIAL DE SEGUIMIENTO
	// ===========================================

	private void anotar(Reporte r, Estado e, String autor, String detalle) {

		HistorialReporte h = new HistorialReporte();

		h.setReporte(r);
		h.setEstado(e);
		h.setAutor(autor);
		h.setDetalle(detalle);

		historial.save(h);
	}

	// ===========================================
	// PROTECCIÓN DE DATOS PERSONALES
	// ===========================================

	private static final Pattern CORREO_PATTERN = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");

	private static final Pattern TELEFONO_PERU_PATTERN = Pattern
			.compile("(?<!\\d)(?:\\+?51[\\s.-]?)?9(?:[\\s.-]?\\d){8}(?!\\d)");

	private static final Pattern DNI_PATTERN = Pattern
			.compile("(?i)\\b(?:DNI|documento(?:\\s+de\\s+identidad)?)\\s*(?:n[°º.]?\\s*)?[:#-]?\\s*\\d{8}\\b");

	private void validarDatosPersonales(String texto) {

		if (texto == null || texto.isBlank()) {
			return;
		}

		if (CORREO_PATTERN.matcher(texto).find() || TELEFONO_PERU_PATTERN.matcher(texto).find()
				|| DNI_PATTERN.matcher(texto).find()) {

			throw new IllegalArgumentException("Por privacidad, no incluyas correos electrónicos, "
					+ "teléfonos ni números de DNI en la descripción "
					+ "u observación. Elimina esos datos e inténtalo nuevamente.");
		}
	}

}
