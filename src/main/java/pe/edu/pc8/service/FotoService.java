package pe.edu.pc8.service;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoService {
	private final Path directorio;

	public FotoService(@Value("${app.upload.dir:./uploads}") String dir) {
		directorio = Paths.get(dir).toAbsolutePath().normalize();
	}

	public String guardar(MultipartFile archivo) throws IOException {
		if (archivo == null || archivo.isEmpty())
			return null;
		if (archivo.getSize() > 3_000_000L)
			throw new IllegalArgumentException("La foto supera 3 MB");
		String mime = archivo.getContentType();
		if (!"image/jpeg".equals(mime) && !"image/png".equals(mime))
			throw new IllegalArgumentException("Solo se permiten archivos JPG y PNG");
		BufferedImage img;
		try (InputStream input = archivo.getInputStream()) {
			img = ImageIO.read(input);
		}
		if (img == null || img.getWidth() > 7000 || img.getHeight() > 7000)
			throw new IllegalArgumentException("Imagen dañada o dimensiones demasiado grandes");
		Files.createDirectories(directorio);
		String nombre = UUID.randomUUID().toString() + ("image/png".equals(mime) ? ".png" : ".jpg");
		Path dest = directorio.resolve(nombre);
		try (OutputStream salida = Files.newOutputStream(dest, StandardOpenOption.CREATE_NEW)) {
			if (!ImageIO.write(img, "image/png".equals(mime) ? "png" : "jpg", salida))
				throw new IOException("No se pudo guardar la imagen");
		}
		return nombre;
	}

	public ResponseEntity<Resource> mostrar(String nombre) {
		if (!nombre.matches("[0-9a-f-]{36}\\.(png|jpg)"))
			return ResponseEntity.notFound().build();
		Path p = directorio.resolve(nombre).normalize();
		if (!p.startsWith(directorio) || !Files.isRegularFile(p))
			return ResponseEntity.notFound().build();
		MediaType tipo = nombre.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
		return ResponseEntity.ok().contentType(tipo).header(HttpHeaders.CACHE_CONTROL, "private, no-store")
				.header("X-Content-Type-Options", "nosniff").body(new FileSystemResource(p));
	}
}
