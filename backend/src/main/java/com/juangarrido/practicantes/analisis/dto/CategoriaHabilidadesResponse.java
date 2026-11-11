package com.juangarrido.practicantes.analisis.dto;

import java.util.List;

/** Una categoría del catálogo con los nombres visibles de sus habilidades (sin alias internos). */
public record CategoriaHabilidadesResponse(String categoria, List<String> habilidades) {
}
