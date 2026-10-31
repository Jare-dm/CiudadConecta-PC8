package pe.edu.pc8.model;

public enum Categoria {
	BACHE("Baches o pistas dañadas"), BASURA("Basura acumulada"), ALUMBRADO("Alumbrado público"),
	PARQUES("Parques y jardines"), FUGA_AGUA("Fuga de agua"), SENALIZACION("Señalización dañada"),
	OTRO("Otro problema urbano");

	private final String etiqueta;

	Categoria(String e) {
		etiqueta = e;
	}

	public String getEtiqueta() {
		return etiqueta;
	}
}
