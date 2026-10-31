
package pe.edu.pc8.model;

public enum EstadoCategoriaPersonalizada {

    PENDIENTE("Pendiente de revisión"),

    APROBADA("Aprobada"),

    RECHAZADA("Rechazada");

    private final String etiqueta;

    EstadoCategoriaPersonalizada(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
