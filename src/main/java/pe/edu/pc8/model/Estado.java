package pe.edu.pc8.model;
public enum Estado {
  REGISTRADO("Registrado"), EN_REVISION("En revisión"), ASIGNADO("Asignado"),
  EN_ATENCION("En atención"), ATENDIDO("Atendido"), CERRADO("Cerrado"), RECHAZADO("Rechazado");
  private final String etiqueta;
  Estado(String etiqueta){ this.etiqueta=etiqueta; }
  public String getEtiqueta(){ return etiqueta; }
}
