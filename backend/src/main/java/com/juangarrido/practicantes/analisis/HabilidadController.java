package com.juangarrido.practicantes.analisis;

import com.juangarrido.practicantes.analisis.CatalogoHabilidades.Habilidad;
import com.juangarrido.practicantes.analisis.dto.CategoriaHabilidadesResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/habilidades")
public class HabilidadController {

    private final CatalogoHabilidades catalogo;

    public HabilidadController(CatalogoHabilidades catalogo) {
        this.catalogo = catalogo;
    }

    /** Alimenta el filtro de la lista y la agrupación por categoría en el detalle. */
    @GetMapping("/catalogo")
    public List<CategoriaHabilidadesResponse> catalogo() {
        return catalogo.categorias().entrySet().stream()
                .map(categoria -> new CategoriaHabilidadesResponse(
                        categoria.getKey(),
                        categoria.getValue().stream().map(Habilidad::nombre).toList()))
                .toList();
    }
}
