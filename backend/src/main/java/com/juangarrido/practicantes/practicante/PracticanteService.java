package com.juangarrido.practicantes.practicante;

import com.juangarrido.practicantes.archivo.AlmacenamientoArchivoService;
import com.juangarrido.practicantes.practicante.dto.CambioEstadoRequest;
import com.juangarrido.practicantes.practicante.dto.PracticanteResponse;
import com.juangarrido.practicantes.practicante.dto.RegistroPracticanteRequest;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Service
public class PracticanteService {

    private final PracticanteRepository repository;
    private final AlmacenamientoArchivoService almacenamiento;

    public PracticanteService(PracticanteRepository repository, AlmacenamientoArchivoService almacenamiento) {
        this.repository = repository;
        this.almacenamiento = almacenamiento;
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
            return PracticanteResponse.desde(repository.save(practicante));
        } catch (RuntimeException e) {
            almacenamiento.eliminar(nombreGuardado);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PracticanteResponse> listar(EstadoPracticante estado) {
        List<Practicante> practicantes = (estado == null)
                ? repository.findAllByOrderByFechaRegistroDesc()
                : repository.findByEstadoOrderByFechaRegistroDesc(estado);
        return practicantes.stream().map(PracticanteResponse::desde).toList();
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
