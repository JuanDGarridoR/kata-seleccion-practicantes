package com.juangarrido.practicantes.practicante;

import com.juangarrido.practicantes.practicante.dto.CambioEstadoRequest;
import com.juangarrido.practicantes.practicante.dto.PracticanteResponse;
import com.juangarrido.practicantes.practicante.dto.RegistroPracticanteRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/practicantes")
public class PracticanteController {

    private final PracticanteService service;

    public PracticanteController(PracticanteService service) {
        this.service = service;
    }

    /** Público: los datos llegan como campos del formulario multipart junto con el PDF. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PracticanteResponse> registrar(
            @Valid @ModelAttribute RegistroPracticanteRequest datos,
            @RequestPart(name = "hojaVida", required = false) MultipartFile hojaVida) {
        PracticanteResponse creado = service.registrar(datos, hojaVida);
        return ResponseEntity.created(URI.create("/api/practicantes/" + creado.id())).body(creado);
    }

    @GetMapping
    public List<PracticanteResponse> listar(@RequestParam(required = false) EstadoPracticante estado,
                                            @RequestParam(required = false) String habilidad) {
        return service.listar(estado, (habilidad == null || habilidad.isBlank()) ? null : habilidad.trim());
    }

    @PostMapping("/{id}/reanalizar")
    public PracticanteResponse reanalizar(@PathVariable Long id) {
        return service.reanalizar(id);
    }

    @GetMapping("/{id}")
    public PracticanteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PatchMapping("/{id}/estado")
    public PracticanteResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest cambio) {
        return service.cambiarEstado(id, cambio);
    }

    /** "attachment": el navegador descarga el archivo. */
    @GetMapping("/{id}/hoja-vida")
    public ResponseEntity<Resource> descargarHojaVida(@PathVariable Long id) {
        return respuestaHojaVida(id, ContentDisposition.attachment());
    }

    /** "inline": el navegador lo muestra en su visor de PDF (vista previa en el detalle). */
    @GetMapping("/{id}/hoja-vida/vista")
    public ResponseEntity<Resource> verHojaVida(@PathVariable Long id) {
        return respuestaHojaVida(id, ContentDisposition.inline());
    }

    /** Los dos endpoints solo difieren en el tipo de Content-Disposition. */
    private ResponseEntity<Resource> respuestaHojaVida(Long id, ContentDisposition.Builder tipo) {
        PracticanteService.HojaVida hojaVida = service.obtenerHojaVida(id);
        ContentDisposition disposicion = tipo.filename(hojaVida.nombreOriginal(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(hojaVida.recurso());
    }
}
