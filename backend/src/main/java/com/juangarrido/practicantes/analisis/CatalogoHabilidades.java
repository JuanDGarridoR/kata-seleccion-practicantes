package com.juangarrido.practicantes.analisis;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo de habilidades que se buscan en las hojas de vida, agrupadas por categoría.
 * Cada habilidad tiene un nombre visible y los alias con que puede aparecer en el texto
 * (en minúsculas y sin tildes, porque así queda el texto después de normalizarlo).
 *
 * Se excluyen a propósito habilidades de una letra o ambiguas como "C", "R" o "Go":
 * aparecen en iniciales, viñetas o palabras comunes ("Go" en inglés) y generarían falsos positivos.
 */
@Component
public class CatalogoHabilidades {

    public record Habilidad(String nombre, List<String> alias) {
    }

    private final Map<String, List<Habilidad>> categorias = new LinkedHashMap<>();

    public CatalogoHabilidades() {
        categorias.put("Lenguajes", List.of(
                new Habilidad("Java", List.of("java")),
                new Habilidad("Python", List.of("python")),
                new Habilidad("JavaScript", List.of("javascript", "js")),
                new Habilidad("TypeScript", List.of("typescript")),
                new Habilidad("C#", List.of("c#", "csharp")),
                new Habilidad("C++", List.of("c++")),
                new Habilidad("Kotlin", List.of("kotlin")),
                new Habilidad("PHP", List.of("php"))));

        categorias.put("Frameworks", List.of(
                new Habilidad("Spring Boot", List.of("spring boot", "spring framework")),
                new Habilidad("Angular", List.of("angular")),
                new Habilidad("React", List.of("react", "reactjs")),
                new Habilidad("Django", List.of("django")),
                new Habilidad(".NET", List.of(".net", "asp.net", "dotnet")),
                new Habilidad("Node.js", List.of("node.js", "nodejs"))));

        categorias.put("Datos", List.of(
                new Habilidad("SQL", List.of("sql")),
                new Habilidad("PostgreSQL", List.of("postgresql", "postgres")),
                new Habilidad("MySQL", List.of("mysql")),
                new Habilidad("MongoDB", List.of("mongodb", "mongo")),
                new Habilidad("Power BI", List.of("power bi", "powerbi")),
                new Habilidad("Excel", List.of("excel"))));

        categorias.put("Herramientas", List.of(
                new Habilidad("Git", List.of("git", "github", "gitlab")),
                new Habilidad("Docker", List.of("docker")),
                new Habilidad("AWS", List.of("aws", "amazon web services")),
                new Habilidad("Azure", List.of("azure")),
                new Habilidad("Jira", List.of("jira"))));

        categorias.put("Metodologías", List.of(
                new Habilidad("Scrum", List.of("scrum")),
                new Habilidad("Kanban", List.of("kanban")),
                new Habilidad("Metodologías ágiles", List.of("agile", "metodologias agiles"))));

        categorias.put("Idiomas", List.of(
                new Habilidad("Inglés", List.of("ingles", "english", "b2", "c1")),
                new Habilidad("Francés", List.of("frances", "french")),
                new Habilidad("Portugués", List.of("portugues", "portuguese"))));
    }

    public Map<String, List<Habilidad>> categorias() {
        return Collections.unmodifiableMap(categorias);
    }
}
