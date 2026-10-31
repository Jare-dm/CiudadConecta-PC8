
package pe.edu.pc8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "categorias_personalizadas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_categoria_personalizada_normalizada",
        columnNames = "nombre_normalizado"
    )
)
public class CategoriaPersonalizada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 65)
    private String nombre;

    @Column(
        name = "nombre_normalizado",
        nullable = false,
        length = 80
    )
    private String nombreNormalizado;

    @Column(nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(
        name = "estado",
        nullable = false,
        length = 20
    )
    private EstadoCategoriaPersonalizada estado =
        EstadoCategoriaPersonalizada.APROBADA;

    // NUEVO: ciudadano que propuso la categoría.
    // Se permite null para categorías históricas.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "proponente_id",
        nullable = true
    )
    private Usuario proponente;

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreNormalizado() {
        return nombreNormalizado;
    }

    public void setNombreNormalizado(String nombreNormalizado) {
        this.nombreNormalizado = nombreNormalizado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public EstadoCategoriaPersonalizada getEstado() {
        return estado;
    }

    public void setEstado(EstadoCategoriaPersonalizada estado) {
        if (estado == null) {
            throw new IllegalArgumentException(
                "El estado de la categoría es obligatorio"
            );
        }

        this.estado = estado;
    }

    public Usuario getProponente() {
        return proponente;
    }

    public void setProponente(Usuario proponente) {
        this.proponente = proponente;
    }
}
