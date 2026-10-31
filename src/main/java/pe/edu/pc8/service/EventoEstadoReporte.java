package pe.edu.pc8.service;
/** Evento sin datos personales; el destinatario se consulta de forma segura después del commit. */
public record EventoEstadoReporte(Long ciudadanoId, String codigo, String estado) {}
