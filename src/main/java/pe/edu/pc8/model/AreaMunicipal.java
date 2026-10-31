package pe.edu.pc8.model;
public enum AreaMunicipal {
  OBRAS("Obras y mantenimiento vial"), LIMPIEZA("Limpieza pública"), ALUMBRADO("Alumbrado público"),
  PARQUES("Parques y jardines"), SERVICIOS("Servicios públicos");
  private final String etiqueta;
  AreaMunicipal(String e){etiqueta=e;}
  public String getEtiqueta(){return etiqueta;}
}
