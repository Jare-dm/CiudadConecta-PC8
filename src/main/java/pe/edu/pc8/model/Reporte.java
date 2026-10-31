
package pe.edu.pc8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
public class Reporte {

    private static final long HORAS_ALERTA_ASIGNACION = 24;
    private static final long HORAS_MAXIMAS_SIN_ASIGNAR = 48;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 25)
    private String codigo;

    // ========================================
    // CATEGORÍA PRINCIPAL
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    // Los reportes antiguos mantienen el enum.
    // Las categorías nuevas enlazan a esta tabla.

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_personalizada_id")
    private CategoriaPersonalizada categoriaPersonalizada;

    // ========================================
    // NUEVO: SUBCATEGORÍA
    // ========================================
    //
    // Puede ser null para:
    // - Reportes registrados anteriormente.
    // - Categorías personalizadas.
    // - Categorías sin subcategorías definidas.
    //
    // Se guarda el nombre del enum en MariaDB.

    @Enumerated(EnumType.STRING)
    @Column(name = "subcategoria", length = 60)
    private Subcategoria subcategoria;

    // ========================================
    // INFORMACIÓN DE LA INCIDENCIA
    // ========================================

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(nullable = false, length = 260)
    private String ubicacion;

    private Double latitud;
    private Double longitud;

    @Column(length = 50)
    private String archivoImagen;

    @Column(length = 50)
    private String archivoAtencion;

    // ========================================
    // ESTADO Y GESTIÓN MUNICIPAL
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.REGISTRADO;

    @Enumerated(EnumType.STRING)
    private AreaMunicipal area;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    private LocalDateTime fechaLimite;

    @Column(length = 700)
    private String observacion;

    // ========================================
    // CIUDADANO Y FECHAS
    // ========================================

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Usuario ciudadano;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    private LocalDateTime actualizadoEn = LocalDateTime.now();

    // ========================================
    // IDENTIDAD RESERVADA
    // ========================================

    @Column(name = "identidad_reservada", nullable = false)
    private boolean identidadReservada = true;

    // ========================================
    // GETTERS Y SETTERS
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String v) {
        codigo = v;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria v) {
        categoria = v;
    }

    public CategoriaPersonalizada getCategoriaPersonalizada() {
        return categoriaPersonalizada;
    }

    public void setCategoriaPersonalizada(
            CategoriaPersonalizada v) {

        categoriaPersonalizada = v;
    }

    public String getCategoriaEtiqueta() {

        return categoriaPersonalizada == null
            ? categoria.getEtiqueta()
            : categoriaPersonalizada.getNombre();
    }

    // ========================================
    // NUEVO: MÉTODOS DE SUBCATEGORÍA
    // ========================================

    public Subcategoria getSubcategoria() {
        return subcategoria;
    }

    public void setSubcategoria(Subcategoria v) {
        subcategoria = v;
    }

    // Nombre legible para las vistas Thymeleaf.

    @Transient
    public String getSubcategoriaEtiqueta() {

        return subcategoria == null
            ? "Sin especificar"
            : subcategoria.getEtiqueta();
    }

    // ========================================
    // DATOS DE LA INCIDENCIA
    // ========================================

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String v) {
        descripcion = v;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String v) {
        ubicacion = v;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double v) {
        latitud = v;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double v) {
        longitud = v;
    }

    public String getArchivoAtencion() {
        return archivoAtencion;
    }

    public void setArchivoAtencion(String v) {
        archivoAtencion = v;
    }

    public String getArchivoImagen() {
        return archivoImagen;
    }

    public void setArchivoImagen(String v) {
        archivoImagen = v;
    }

    // ========================================
    // ESTADO Y RESPONSABLE
    // ========================================

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado v) {
        estado = v;
    }

    public AreaMunicipal getArea() {
        return area;
    }

    public void setArea(AreaMunicipal v) {
        area = v;
    }

    public Usuario getResponsable() {
        return responsable;
    }

    public void setResponsable(Usuario v) {
        responsable = v;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDateTime v) {
        fechaLimite = v;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String v) {
        observacion = v;
    }

    // ========================================
    // CIUDADANO Y FECHAS
    // ========================================

    public Usuario getCiudadano() {
        return ciudadano;
    }

    public void setCiudadano(Usuario v) {
        ciudadano = v;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime v) {
        creadoEn = v;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime v) {
        actualizadoEn = v;
    }

    // ========================================
    // PRIVACIDAD DEL CIUDADANO
    // ========================================

    public boolean isIdentidadReservada() {
        return identidadReservada;
    }

    public void setIdentidadReservada(boolean identidadReservada) {
        this.identidadReservada = identidadReservada;
    }

    // ========================================
    // ESTADOS DE ATENCIÓN EXISTENTES
    // ========================================

    public boolean enProceso() {

        return estado != Estado.ATENDIDO
            && estado != Estado.CERRADO
            && estado != Estado.RECHAZADO;
    }

    @Transient
    public boolean isVencido() {

        return enProceso()
            && fechaLimite != null
            && LocalDateTime.now().isAfter(fechaLimite);
    }

    @Transient
    public boolean isPorVencer() {

        return enProceso()
            && fechaLimite != null
            && !isVencido()
            && !LocalDateTime.now()
                .plusHours(24)
                .isBefore(fechaLimite);
    }

    // ========================================
    // REPORTES PENDIENTES DE ASIGNACIÓN
    // ========================================

    @Transient
    public boolean isPendienteDeAsignacion() {

        boolean estadoInicial =
            estado == Estado.REGISTRADO
            || estado == Estado.EN_REVISION;

        return estadoInicial
            && responsable == null
            && creadoEn != null;
    }

    // ========================================
    // ASIGNACIÓN ATRASADA
    //
    // Un reporte lleva 48 horas o más
    // sin responsable y sigue en una etapa
    // inicial del proceso.
    // ========================================

    @Transient
    public boolean isSinAsignarAtrasado() {

        if (!isPendienteDeAsignacion()) {
            return false;
        }

        LocalDateTime limite =
            creadoEn.plusHours(
                HORAS_MAXIMAS_SIN_ASIGNAR
            );

        return !LocalDateTime.now().isBefore(limite);
    }

    // ========================================
    // ASIGNACIÓN PRÓXIMA A ATRASARSE
    //
    // El reporte lleva entre 24 y 48 horas
    // sin responsable.
    // ========================================

    @Transient
    public boolean isSinAsignarPorVencer() {

        if (!isPendienteDeAsignacion()) {
            return false;
        }

        LocalDateTime ahora =
            LocalDateTime.now();

        LocalDateTime inicioAlerta =
            creadoEn.plusHours(
                HORAS_ALERTA_ASIGNACION
            );

        LocalDateTime limite =
            creadoEn.plusHours(
                HORAS_MAXIMAS_SIN_ASIGNAR
            );

        return !ahora.isBefore(inicioAlerta)
            && ahora.isBefore(limite);
    }
}
