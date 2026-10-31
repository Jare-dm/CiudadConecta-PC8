
package pe.edu.pc8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "notificaciones",
    indexes = {
        @Index(
            name = "idx_notif_destinatario",
            columnList = "destinatario_id"
        )
    }
)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // DESTINATARIO
    // ==========================================

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    private Usuario destinatario;

    // ==========================================
    // REPORTE ASOCIADO (AHORA OPCIONAL)
    // ==========================================
    //
    // Si la notificación corresponde al seguimiento
    // de un reporte, este campo tendrá un Reporte.
    //
    // Si corresponde a la aprobación o rechazo de
    // una categoría, podrá ser null.

    @ManyToOne(
        fetch = FetchType.EAGER,
        optional = true
    )
    @JoinColumn(
        name = "reporte_id",
        nullable = true
    )
    private Reporte reporte;

    // ==========================================
    // MENSAJE DE NOTIFICACIÓN
    // ==========================================

    @Column(
        nullable = false,
        length = 300
    )
    private String mensaje;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario v) {
        destinatario = v;
    }

    public Reporte getReporte() {
        return reporte;
    }

    public void setReporte(Reporte v) {
        reporte = v;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String v) {
        mensaje = v;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean v) {
        leida = v;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
}
