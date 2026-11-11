package com.juangarrido.practicantes.practicante;

import com.juangarrido.practicantes.analisis.DetectorHabilidades;
import com.juangarrido.practicantes.analisis.ExtractorTextoPdf;
import com.juangarrido.practicantes.archivo.AlmacenamientoArchivoService;
import com.juangarrido.practicantes.practicante.dto.CambioEstadoRequest;
import com.juangarrido.practicantes.practicante.dto.PracticanteResponse;
import com.juangarrido.practicantes.practicante.dto.RegistroPracticanteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PracticanteService {

    private static final Logger log = LoggerFactory.getLogger(PracticanteService.class);

    private final PracticanteRepository repository;
    private final AlmacenamientoArchivoService almacenamiento;
    private final ExtractorTextoPdf extractor;
    private final DetectorHabilidades detector;

    public PracticanteService(PracticanteRepository repository, AlmacenamientoArchivoService almacenamiento,
                              ExtractorTextoPdf extractor, DetectorHabilidades detector) {
        this.repository = repository;
        this.almacenamiento = almacenamiento;
        this.extractor = extractor;
        this.detector = detector;
    }

    @Transactional
    public PracticanteResponse registrar(RegistroPracticanteRequest datos, MultipartFile hojaVida) {
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException(correo);
        }

        // Primero se valida y guarda el archivo; si la BD falla después, se borra para no dejar huérfanos.
        String nombreGuardado = almacenamiento.guardarPdf(hojaVida);
        try {
            Practicante practicante = new Practicante(
                    datos.nombreCompleto().trim(),
                    correo,
                    datos.carrera().trim(),
                    datos.semestre(),
                    nombreGuardado,
                    nombreOriginal(hojaVida)
            );
            analizarHojaVida(practicante);
            return PracticanteResponse.desde(repository.save(practicante));
        } catch (RuntimeException e) {
            almacenamiento.eliminar(nombreGuardado);
            throw e;
        }
    }

    /** Ambos filtros son opcionales y combinables (null = sin filtrar). */
    @Transactional(readOnly = true)
    public List<PracticanteResponse> listar(EstadoPracticante estado, String habilidad) {
        return repository.buscar(estado, habilidad).stream().map(PracticanteResponse::desde).toList();
    }

    /** Vuelve a analizar la hoja de vida, por ejemplo tras ampliar el catálogo de habilidades. */
    @Transactional
    public PracticanteResponse reanalizar(Long id) {
        Practicante practicante = buscar(id);
        analizarHojaVida(practicante);
        return PracticanteResponse.desde(practicante);
    }

    @Transactional(readOnly = true)
    public PracticanteResponse obtener(Long id) {
        return PracticanteResponse.desde(buscar(id));
    }

    @Transactional
    public PracticanteResponse cambiarEstado(Long id, CambioEstadoRequest cambio) {
        Practicante practicante = buscar(id);
        practicante.cambiarEstado(cambio.estado(), limpiar(cambio.observacion()));
        return PracticanteResponse.desde(practicante);
    }

    @Transactional(readOnly = true)
    public HojaVida obtenerHojaVida(Long id) {
        Practicante practicante = buscar(id);
        Resource recurso = almacenamiento.cargar(practicante.getRutaHojaVida());
        return new HojaVida(recurso, practicante.getNombreOriginalHojaVida());
    }

    /**
     * El análisis es un valor agregado: si el PDF no se puede leer, el registro del candidato
     * no debe fallar. Se captura también RuntimeException porque PDFBox puede lanzar errores
     * no verificados con archivos dañados.
     */
    private void analizarHojaVida(Practicante practicante) {
        try {
            String texto = extractor.extraer(almacenamiento.ruta(practicante.getRutaHojaVida()));
            if (texto == null || texto.isBlank()) {
                practicante.registrarAnalisis(false, Set.of());
            } else {
                practicante.registrarAnalisis(true, detector.detectar(texto));
            }
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo analizar la hoja de vida {}: {}", practicante.getRutaHojaVida(), e.getMessage());
            practicante.registrarAnalisis(false, Set.of());
        }
    }

    private Practicante buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PracticanteNoEncontradoException(id));
    }

    private static String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "hoja-de-vida.pdf";
        }
        // Algunos navegadores antiguos envían la ruta completa del cliente; solo interesa el nombre.
        return nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    public record HojaVida(Resource recurso, String nombreOriginal) {
    }
}
