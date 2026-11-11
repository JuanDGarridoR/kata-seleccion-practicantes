package com.juangarrido.practicantes.archivo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

@Service
public class AlmacenamientoArchivoService {

    private static final String TIPO_PDF = "application/pdf";
    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F'};

    private final Path directorio;

    public AlmacenamientoArchivoService(@Value("${app.uploads.dir}") String directorio) throws IOException {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
        Files.createDirectories(this.directorio);
    }

    /**
     * Guarda el PDF con un nombre UUID: el nombre original no se usa en disco
     * para evitar colisiones y rutas maliciosas (p. ej. "../../algo.pdf").
     *
     * @return nombre con el que quedó guardado el archivo
     */
    public String guardarPdf(MultipartFile archivo) {
        validarPdf(archivo);
        String nombreGuardado = UUID.randomUUID() + ".pdf";
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, directorio.resolve(nombreGuardado));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la hoja de vida", e);
        }
        return nombreGuardado;
    }

    public Resource cargar(String nombreGuardado) {
        Path ruta = directorio.resolve(nombreGuardado).normalize();
        if (!ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw new ArchivoNoEncontradoException(nombreGuardado);
        }
        return new PathResource(ruta);
    }

    public void eliminar(String nombreGuardado) {
        try {
            Files.deleteIfExists(directorio.resolve(nombreGuardado));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el archivo " + nombreGuardado, e);
        }
    }

    /** El content-type lo manda el cliente y se puede falsificar, por eso también se revisa la firma %PDF. */
    private void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("La hoja de vida (PDF) es obligatoria");
        }
        if (!TIPO_PDF.equalsIgnoreCase(archivo.getContentType())) {
            throw new ArchivoInvalidoException("La hoja de vida debe ser un archivo PDF");
        }
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] cabecera = entrada.readNBytes(FIRMA_PDF.length);
            if (!Arrays.equals(cabecera, FIRMA_PDF)) {
                throw new ArchivoInvalidoException("El archivo no es un PDF válido");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }
}
