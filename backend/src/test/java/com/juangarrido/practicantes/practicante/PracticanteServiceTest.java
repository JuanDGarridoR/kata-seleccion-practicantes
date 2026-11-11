package com.juangarrido.practicantes.practicante;

import com.juangarrido.practicantes.analisis.DetectorHabilidades;
import com.juangarrido.practicantes.analisis.ExtractorTextoPdf;
import com.juangarrido.practicantes.archivo.AlmacenamientoArchivoService;
import com.juangarrido.practicantes.practicante.dto.CambioEstadoRequest;
import com.juangarrido.practicantes.practicante.dto.PracticanteResponse;
import com.juangarrido.practicantes.practicante.dto.RegistroPracticanteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticanteServiceTest {

    @Mock
    private PracticanteRepository repository;

    @Mock
    private AlmacenamientoArchivoService almacenamiento;

    @Mock
    private ExtractorTextoPdf extractor;

    @Mock
    private DetectorHabilidades detector;

    private PracticanteService service;

    private final MockMultipartFile pdf =
            new MockMultipartFile("hojaVida", "mi-hv.pdf", "application/pdf", "%PDF-1.4".getBytes());

    @BeforeEach
    void setUp() {
        service = new PracticanteService(repository, almacenamiento, extractor, detector);
    }

    @Test
    void registrar_conTextoLegible_guardaLasHabilidadesDetectadas() throws IOException {
        var datos = new RegistroPracticanteRequest("Ana", "ana@correo.com", "Derecho", 3);
        Path ruta = Path.of("uploads", "uuid.pdf");
        when(almacenamiento.guardarPdf(pdf)).thenReturn("uuid.pdf");
        when(almacenamiento.ruta("uuid.pdf")).thenReturn(ruta);
        when(extractor.extraer(ruta)).thenReturn("Java y SQL");
        when(detector.detectar("Java y SQL")).thenReturn(Set.of("Java", "SQL"));
        when(repository.save(any(Practicante.class))).thenAnswer(inv -> inv.getArgument(0));

        PracticanteResponse respuesta = service.registrar(datos, pdf);

        assertThat(respuesta.textoExtraible()).isTrue();
        assertThat(respuesta.habilidadesDetectadas()).containsExactlyInAnyOrder("Java", "SQL");
    }

    @Test
    void registrar_siNoSePuedeLeerElPdf_igualRegistraSinHabilidades() throws IOException {
        var datos = new RegistroPracticanteRequest("Ana", "ana@correo.com", "Derecho", 3);
        when(almacenamiento.guardarPdf(pdf)).thenReturn("uuid.pdf");
        when(extractor.extraer(any())).thenThrow(new IOException("PDF dañado"));
        when(repository.save(any(Practicante.class))).thenAnswer(inv -> inv.getArgument(0));

        PracticanteResponse respuesta = service.registrar(datos, pdf);

        assertThat(respuesta.textoExtraible()).isFalse();
        assertThat(respuesta.habilidadesDetectadas()).isEmpty();
        verify(almacenamiento, never()).eliminar(any());
    }

    @Test
    void registrar_conPdfSinTexto_marcaTextoNoExtraible() throws IOException {
        var datos = new RegistroPracticanteRequest("Ana", "ana@correo.com", "Derecho", 3);
        when(almacenamiento.guardarPdf(pdf)).thenReturn("uuid.pdf");
        when(extractor.extraer(any())).thenReturn("  \n ");
        when(repository.save(any(Practicante.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.registrar(datos, pdf).textoExtraible()).isFalse();
        verify(detector, never()).detectar(any());
    }

    @Test
    void registrar_guardaArchivoYPracticanteConCorreoNormalizado() {
        var datos = new RegistroPracticanteRequest("  Ana Pérez ", " Ana.Perez@Correo.com ", "Ingeniería de Sistemas", 7);
        when(repository.existsByCorreoIgnoreCase("ana.perez@correo.com")).thenReturn(false);
        when(almacenamiento.guardarPdf(pdf)).thenReturn("uuid.pdf");
        when(repository.save(any(Practicante.class))).thenAnswer(inv -> inv.getArgument(0));

        PracticanteResponse respuesta = service.registrar(datos, pdf);

        ArgumentCaptor<Practicante> captor = ArgumentCaptor.forClass(Practicante.class);
        verify(repository).save(captor.capture());
        Practicante guardado = captor.getValue();
        assertThat(guardado.getNombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(guardado.getCorreo()).isEqualTo("ana.perez@correo.com");
        assertThat(guardado.getRutaHojaVida()).isEqualTo("uuid.pdf");
        assertThat(guardado.getNombreOriginalHojaVida()).isEqualTo("mi-hv.pdf");
        assertThat(respuesta.estado()).isEqualTo(EstadoPracticante.PENDIENTE);
    }

    @Test
    void registrar_conCorreoDuplicado_lanzaExcepcionSinGuardarArchivo() {
        var datos = new RegistroPracticanteRequest("Ana", "ana@correo.com", "Derecho", 3);
        when(repository.existsByCorreoIgnoreCase("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(datos, pdf))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(almacenamiento, never()).guardarPdf(any());
        verify(repository, never()).save(any());
    }

    @Test
    void registrar_siFallaLaBaseDeDatos_eliminaElArchivoGuardado() {
        var datos = new RegistroPracticanteRequest("Ana", "ana@correo.com", "Derecho", 3);
        when(repository.existsByCorreoIgnoreCase(any())).thenReturn(false);
        when(almacenamiento.guardarPdf(pdf)).thenReturn("uuid.pdf");
        when(repository.save(any())).thenThrow(new RuntimeException("BD caída"));

        assertThatThrownBy(() -> service.registrar(datos, pdf)).hasMessage("BD caída");

        verify(almacenamiento).eliminar("uuid.pdf");
    }

    @Test
    void cambiarEstado_aViable_asignaObservacionYFechaDecision() {
        var practicante = new Practicante("Ana", "ana@correo.com", "Derecho", 3, "uuid.pdf", "hv.pdf");
        when(repository.findById(1L)).thenReturn(Optional.of(practicante));

        PracticanteResponse respuesta = service.cambiarEstado(1L,
                new CambioEstadoRequest(EstadoPracticante.VIABLE, "  Buen perfil  "));

        assertThat(respuesta.estado()).isEqualTo(EstadoPracticante.VIABLE);
        assertThat(respuesta.observacion()).isEqualTo("Buen perfil");
        assertThat(respuesta.fechaDecision()).isNotNull();
    }

    @Test
    void cambiarEstado_aPendiente_noEstaPermitido() {
        var practicante = new Practicante("Ana", "ana@correo.com", "Derecho", 3, "uuid.pdf", "hv.pdf");
        when(repository.findById(1L)).thenReturn(Optional.of(practicante));

        assertThatThrownBy(() -> service.cambiarEstado(1L,
                new CambioEstadoRequest(EstadoPracticante.PENDIENTE, null)))
                .isInstanceOf(CambioEstadoInvalidoException.class);
    }

    @Test
    void cambiarEstado_dePracticanteInexistente_lanzaNoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(99L,
                new CambioEstadoRequest(EstadoPracticante.VIABLE, null)))
                .isInstanceOf(PracticanteNoEncontradoException.class);
    }
}
