package pe.edu.pc8.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Consulta el límite referencial del distrito (UBIGEO 150108) y lo valida en servidor. */
@Service
public class ChorrillosService {
  private static final String[] FUENTES={
    "https://gisem.osinergmin.gob.pe/serverosih/rest/services/Cartografia/LIMITES_DISTRITALES/MapServer/0/query",
    "https://sisfor.osinfor.gob.pe/osinfor/rest/services/SISFOR_v5/PUBLICABLE_CARTOBASE/MapServer/3/query"
  };
  private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final ObjectMapper mapper;
  private final Path cache;
  private volatile JsonNode geometria;
  private volatile JsonNode respuesta;
  public ChorrillosService(ObjectMapper mapper, @Value("${app.chorrillos.cache:./data/chorrillos.geojson}") String cacheFile){
    this.mapper=mapper;this.cache=Path.of(cacheFile);
  }

  public synchronized JsonNode geojson(){
    if(respuesta!=null)return respuesta;
    if(Files.isRegularFile(cache)){
      try{
        JsonNode root=mapper.readTree(Files.readString(cache));
        if(aceptar(root))return respuesta;
      }catch(Exception ignored){ /* Actualizar desde fuentes públicas */ }
    }
    for(String base:FUENTES){
      try {
        String q=base+"?where=UBIGEO%3D%27150108%27&outFields=UBIGEO&returnGeometry=true&outSR=4326&f=geojson";
        HttpRequest peticion=HttpRequest.newBuilder(URI.create(q)).timeout(Duration.ofSeconds(12))
          .header("Accept","application/geo+json, application/json").GET().build();
        HttpResponse<String> r=http.send(peticion,HttpResponse.BodyHandlers.ofString());
        if(r.statusCode()!=200)continue;
        JsonNode root=mapper.readTree(r.body());
        if(!aceptar(root))continue;
        try {Files.createDirectories(cache.toAbsolutePath().getParent());Files.writeString(cache,root.toString());}
        catch(Exception ignored){ /* La caché es opcional; los datos siguen en memoria */ }
        return respuesta;
      }catch(Exception ignored){ /* Probar otra fuente; jamás autorizar sin límite válido. */ }
    }
    throw new IllegalStateException("No se pudo cargar el límite cartográfico de Chorrillos. Comprueba la conexión a Internet o el servicio cartográfico.");
  }
  private boolean aceptar(JsonNode root){
    JsonNode features=root.path("features");
    if(!features.isArray()||features.size()!=1)return false;
    JsonNode feature=features.get(0);
    if(!"150108".equals(feature.path("properties").path("UBIGEO").asText()))return false;
    JsonNode geo=feature.path("geometry");
    if(!geo.isObject()||!geo.path("coordinates").isArray()||
        !("Polygon".equals(geo.path("type").asText())||"MultiPolygon".equals(geo.path("type").asText())))return false;
    geometria=geo;respuesta=root;return true;
  }
  public boolean contiene(Double lat,Double lon){
    if(lat==null||lon==null||!Double.isFinite(lat)||!Double.isFinite(lon))return false;
    JsonNode geo=geometria==null?geojson().get("features").get(0).get("geometry"):geometria;
    String tipo=geo.path("type").asText();JsonNode coords=geo.path("coordinates");
    if("Polygon".equals(tipo))return poligono(coords,lon,lat);
    if("MultiPolygon".equals(tipo)){
      for(JsonNode pol:coords)if(poligono(pol,lon,lat))return true;
    }
    return false;
  }
  static boolean poligono(JsonNode rings,double x,double y){
    if(!rings.isArray()||rings.isEmpty()||!anillo(rings.get(0),x,y))return false;
    for(int i=1;i<rings.size();i++)if(anillo(rings.get(i),x,y))return false;
    return true;
  }
  static boolean anillo(JsonNode points,double x,double y){
    if(!points.isArray()||points.size()<4)return false;
    boolean dentro=false;
    for(int i=0,j=points.size()-1;i<points.size();j=i++){
      double xi=points.get(i).get(0).asDouble(),yi=points.get(i).get(1).asDouble();
      double xj=points.get(j).get(0).asDouble(),yj=points.get(j).get(1).asDouble();
      double cross=(xj-xi)*(y-yi)-(yj-yi)*(x-xi);
      if(Math.abs(cross)<1e-10 && x>=Math.min(xi,xj)-1e-10 && x<=Math.max(xi,xj)+1e-10
          && y>=Math.min(yi,yj)-1e-10 && y<=Math.max(yi,yj)+1e-10)return true;
      if((yi>y)!=(yj>y) && x<(xj-xi)*(y-yi)/(yj-yi)+xi)dentro=!dentro;
    }
    return dentro;
  }
}
