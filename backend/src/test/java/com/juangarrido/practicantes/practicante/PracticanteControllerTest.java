package com.juangarrido.practicantes.practicante;

import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de punta a punta de la API: HTTP + seguridad + validación + SQLite + disco. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PracticanteControllerTest {

    private static final String USUARIO = "analista";
    private static final String PASSWORD = "Analista2026*";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PracticanteRepository repository;

    private final MockMultipartFile pdf =
            new MockMultipartFile("hojaVida", "hv.pdf", "application/pdf", "%PDF-1.4 contenido".getBytes());

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void flujoCompleto_registroListadoDecisionYDescarga() throws Exception {
        String ubicacion = mockMvc.perform(multipart("/api/practicantes").file(pdf)
                        .param("nombreCompleto", "Ana Pérez")
                        .param("correo", "ana@correo.com")
                        .param("carrera", "Ingeniería de Sistemas")
                        .param("semestre", "8"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                // El contenido "%PDF-1.4 contenido" no es un PDF legible: el registro igual funciona.
                .andExpect(jsonPath("$.textoExtraible").value(false))
                .andExpect(jsonPath("$.habilidadesDetectadas", hasSize(0)))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get("/api/practicantes").with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch(ubicacion + "/estado").with(httpBasic(USUARIO, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"VIABLE\",\"observacion\":\"Buen perfil\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VIABLE"))
                .andExpect(jsonPath("$.fechaDecision").isNotEmpty());

        mockMvc.perform(get(ubicacion + "/hoja-vida").with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")));

        mockMvc.perform(get(ubicacion + "/hoja-vida/vista").with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''hv.pdf")));
    }

    @Test
    void analisis_detectaHabilidadesYPermiteFiltrarPorHabilidadYEstado() throws Exception {
        registrar("ana@correo.com", pdfConTexto("Experiencia en Java, SQL y nivel de Inglés B2"));
        String idLaura = registrar("laura@correo.com", pdfConTexto("Desarrollo con JavaScript y React"));

        mockMvc.perform(get("/api/practicantes").param("habilidad", "SQL").with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].correo").value("ana@correo.com"))
                .andExpect(jsonPath("$[0].textoExtraible").value(true))
                // Conserva todas sus habilidades, no solo la filtrada.
                .andExpect(jsonPath("$[0].habilidadesDetectadas", containsInAnyOrder("Java", "SQL", "Inglés")));

        mockMvc.perform(get("/api/practicantes").param("habilidad", "Java").with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].correo").value("ana@correo.com"));

        mockMvc.perform(patch("/api/practicantes/" + idLaura + "/estado").with(httpBasic(USUARIO, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"VIABLE\"}"));

        mockMvc.perform(get("/api/practicantes").param("habilidad", "React").param("estado", "VIABLE")
                        .with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].correo").value("laura@correo.com"));

        mockMvc.perform(get("/api/practicantes").param("habilidad", "React").param("estado", "PENDIENTE")
                        .with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void reanalizar_vuelveAProcesarLaHojaDeVida() throws Exception {
        String id = registrar("ana@correo.com", pdfConTexto("Python y Docker"));

        mockMvc.perform(post("/api/practicantes/" + id + "/reanalizar").with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textoExtraible").value(true))
                .andExpect(jsonPath("$.habilidadesDetectadas", containsInAnyOrder("Python", "Docker")));

        mockMvc.perform(post("/api/practicantes/" + id + "/reanalizar")).andExpect(status().isUnauthorized());
    }

    @Test
    void catalogo_devuelveCategoriasConNombresVisibles() throws Exception {
        mockMvc.perform(get("/api/habilidades/catalogo").with(httpBasic(USUARIO, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("Lenguajes"))
                .andExpect(jsonPath("$[0].habilidades", hasItem("C#")));

        mockMvc.perform(get("/api/habilidades/catalogo")).andExpect(status().isUnauthorized());
    }

    /** Registra un practicante con el PDF dado y devuelve su id. */
    private String registrar(String correo, byte[] contenidoPdf) throws Exception {
        var archivo = new MockMultipartFile("hojaVida", "hv.pdf", "application/pdf", contenidoPdf);
        String json = mockMvc.perform(multipart("/api/practicantes").file(archivo)
                        .param("nombreCompleto", "Candidato")
                        .param("correo", correo)
                        .param("carrera", "Ingeniería de Sistemas")
                        .param("semestre", "8"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf(JsonPath.<Integer>read(json, "$.id"));
    }

    /** Genera un PDF real de una página con el texto dado (como lo haría Word al exportar). */
    private static byte[] pdfConTexto(String texto) throws IOException {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                contenido.beginText();
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contenido.newLineAtOffset(50, 700);
                contenido.showText(texto);
                contenido.endText();
            }
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    @Test
    void registro_conCamposInvalidos_devuelve400ConDetallePorCampo() throws Exception {
        mockMvc.perform(multipart("/api/practicantes").file(pdf)
                        .param("nombreCompleto", "")
                        .param("correo", "no-es-correo")
                        .param("carrera", "Derecho")
                        .param("semestre", "15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombreCompleto").exists())
                .andExpect(jsonPath("$.campos.correo").exists())
                .andExpect(jsonPath("$.campos.semestre").exists());
    }

    @Test
    void registro_conArchivoQueNoEsPdf_devuelve400() throws Exception {
        var falso = new MockMultipartFile("hojaVida", "virus.pdf", "application/pdf", "MZ ejecutable".getBytes());

        mockMvc.perform(multipart("/api/practicantes").file(falso)
                        .param("nombreCompleto", "Ana")
                        .param("correo", "ana@correo.com")
                        .param("carrera", "Derecho")
                        .param("semestre", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.hojaVida").exists());
    }

    @Test
    void registro_conCorreoDuplicado_devuelve409() throws Exception {
        for (int intento = 0; intento < 2; intento++) {
            var resultado = mockMvc.perform(multipart("/api/practicantes").file(pdf)
                    .param("nombreCompleto", "Ana")
                    .param("correo", "ana@correo.com")
                    .param("carrera", "Derecho")
                    .param("semestre", "3"));
            resultado.andExpect(intento == 0 ? status().isCreated() : status().isConflict());
        }
    }

    @Test
    void rutasDeAnalista_sinCredenciales_devuelven401() throws Exception {
        mockMvc.perform(get("/api/practicantes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reportes/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    void rutasDeAnalista_conPasswordIncorrecto_devuelven401() throws Exception {
        mockMvc.perform(get("/api/practicantes").with(httpBasic(USUARIO, "otra")))
                .andExpect(status().isUnauthorized());
    }
}
