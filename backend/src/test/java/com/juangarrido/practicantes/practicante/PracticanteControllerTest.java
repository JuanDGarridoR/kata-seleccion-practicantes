package com.juangarrido.practicantes.practicante;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''hv.pdf")));
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
        mockMvc.perform(get("/api/reportes/dataset")).andExpect(status().isUnauthorized());
    }

    @Test
    void rutasDeAnalista_conPasswordIncorrecto_devuelven401() throws Exception {
        mockMvc.perform(get("/api/practicantes").with(httpBasic(USUARIO, "otra")))
                .andExpect(status().isUnauthorized());
    }
}
