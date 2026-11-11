package com.juangarrido.practicantes.analisis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas con texto plano: la extracción del PDF se prueba aparte, aquí solo la detección. */
class DetectorHabilidadesTest {

    private final DetectorHabilidades detector = new DetectorHabilidades(new CatalogoHabilidades());

    @Test
    void detectaSinImportarTildesNiMayusculas() {
        var texto = "Nivel de INGLÉS avanzado. Experiencia con PYTHON, Sql Server y Power BI.";

        assertThat(detector.detectar(texto)).contains("Inglés", "Python", "SQL", "Power BI");
    }

    @Test
    void javascriptNoDetectaJava() {
        var texto = "Desarrollo frontend con JavaScript y TypeScript.";

        assertThat(detector.detectar(texto))
                .contains("JavaScript", "TypeScript")
                .doesNotContain("Java");
    }

    @Test
    void detectaJavaYJavascriptCuandoAparecenAmbos() {
        assertThat(detector.detectar("Java 21, JavaScript")).contains("Java", "JavaScript");
    }

    @Test
    void detectaCSharpYCPlusPlusPeseALosSimbolos() {
        var texto = "Lenguajes: C#, C++ y algo de Kotlin.";

        assertThat(detector.detectar(texto)).contains("C#", "C++", "Kotlin");
    }

    @Test
    void cSharpAlFinalDeLaLineaTambienSeDetecta() {
        assertThat(detector.detectar("Experiencia en C#\nOtro dato")).contains("C#");
    }

    @Test
    void noDetectaPalabrasQueSoloContienenElAlias() {
        // "excelente" contiene "excel" y "pythonic" contiene "python", pero no son la habilidad.
        var texto = "Excelente trabajo en equipo, código pythonic.";

        assertThat(detector.detectar(texto)).doesNotContain("Excel", "Python");
    }

    @Test
    void noIncluyeHabilidadesAmbiguasDeUnaLetra() {
        // C, R y Go no están en el catálogo a propósito: generarían falsos positivos.
        var texto = "Carrera C. Pérez, Go to market, R. Gómez";

        assertThat(detector.detectar(texto)).isEmpty();
    }

    @Test
    void textoVacioONuloNoDetectaNada() {
        assertThat(detector.detectar("")).isEmpty();
        assertThat(detector.detectar("   ")).isEmpty();
        assertThat(detector.detectar(null)).isEmpty();
    }

    @Test
    void devuelveNombresVisiblesSinRepetir() {
        // "js" y "javascript" son alias de la misma habilidad: aparece una sola vez.
        assertThat(detector.detectar("JS, javascript, Node.js")).containsOnlyOnce("JavaScript");
    }
}
