
package pe.edu.pc8.model;

import java.util.Arrays;
import java.util.List;

/**
 * Subcategorías de incidencias de CiudadConecta PC8.
 *
 * Cada subcategoría pertenece a una categoría
 * principal del sistema.
 */
public enum Subcategoria {

    // ==========================================
    // BACHES O PISTAS DAÑADAS
    // ==========================================

    BACHE_PROFUNDO(
        Categoria.BACHE,
        "Bache profundo"
    ),

    PISTA_AGRIETADA(
        Categoria.BACHE,
        "Pista agrietada"
    ),

    VEREDA_DANADA(
        Categoria.BACHE,
        "Vereda dañada"
    ),

    PISTA_HUNDIDA(
        Categoria.BACHE,
        "Pista hundida"
    ),

    // ==========================================
    // BASURA ACUMULADA
    // ==========================================

    BASURA_VIA_PUBLICA(
        Categoria.BASURA,
        "Basura en la vía pública"
    ),

    CONTENEDOR_DESBORDADO(
        Categoria.BASURA,
        "Contenedor desbordado"
    ),

    ESCOMBROS(
        Categoria.BASURA,
        "Escombros acumulados"
    ),

    // ==========================================
    // ALUMBRADO PÚBLICO
    // ==========================================

    LUMINARIA_APAGADA(
        Categoria.ALUMBRADO,
        "Luminaria apagada"
    ),

    LUZ_INTERMITENTE(
        Categoria.ALUMBRADO,
        "Luz intermitente"
    ),

    POSTE_DANADO(
        Categoria.ALUMBRADO,
        "Poste de alumbrado dañado"
    ),

    // ==========================================
    // PARQUES Y JARDINES
    // ==========================================

    ARBOL_CAIDO(
        Categoria.PARQUES,
        "Árbol caído"
    ),

    JUEGOS_DANADOS(
        Categoria.PARQUES,
        "Juegos infantiles dañados"
    ),

    AREAS_VERDES_DETERIORADAS(
        Categoria.PARQUES,
        "Áreas verdes deterioradas"
    ),

    // ==========================================
    // FUGA DE AGUA
    // ==========================================

    TUBERIA_ROTA(
        Categoria.FUGA_AGUA,
        "Tubería rota"
    ),

    FUGA_PISTA(
        Categoria.FUGA_AGUA,
        "Fuga de agua en la pista"
    ),

    FUGA_VEREDA(
        Categoria.FUGA_AGUA,
        "Fuga de agua en la vereda"
    ),

    // ==========================================
    // SEÑALIZACIÓN DAÑADA
    // ==========================================

    SENAL_TRANSITO_DANADA(
        Categoria.SENALIZACION,
        "Señal de tránsito dañada"
    ),

    SEMAFORO_AVERIADO(
        Categoria.SENALIZACION,
        "Semáforo averiado"
    ),

    SENALIZACION_DESPINTADA(
        Categoria.SENALIZACION,
        "Señalización despintada"
    );

    // ==========================================
    // ATRIBUTOS
    // ==========================================

    private final Categoria categoria;

    private final String etiqueta;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    Subcategoria(
            Categoria categoria,
            String etiqueta) {

        this.categoria = categoria;
        this.etiqueta = etiqueta;
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public Categoria getCategoria() {
        return categoria;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    // ==========================================
    // FILTRAR POR CATEGORÍA PRINCIPAL
    // ==========================================

    public static List<Subcategoria> porCategoria(
            Categoria categoria) {

        return Arrays.stream(values())
            .filter(subcategoria ->
                subcategoria.getCategoria() == categoria
            )
            .toList();
    }
}
