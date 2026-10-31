package pe.edu.pc8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_reportes")
public class HistorialReporte {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(optional = false)
	private Reporte reporte;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Estado estado;
	@Column(nullable = false, length = 110)
	private String autor;
	@Column(length = 700)
	private String detalle;
	private LocalDateTime fecha = LocalDateTime.now();

	public Long getId() {
		return id;
	}

	public void setId(Long v) {
		id = v;
	}

	public Reporte getReporte() {
		return reporte;
	}

	public void setReporte(Reporte v) {
		reporte = v;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado v) {
		estado = v;
	}

	public String getAutor() {
		return autor;
	}

	public void setAutor(String v) {
		autor = v;
	}

	public String getDetalle() {
		return detalle;
	}

	public void setDetalle(String v) {
		detalle = v;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime v) {
		fecha = v;
	}
}
