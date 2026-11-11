package com.juangarrido.practicantes.analisis;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DetectorHabilidades {

    /** Una habilidad del catálogo con sus alias ya convertidos en expresiones regulares. */
    private record HabilidadCompilada(String nombre, List<Pattern> patrones) {
    }

    private final List<HabilidadCompilada> habilidades = new ArrayList<>();

    /** Los patrones se compilan una sola vez al arrancar, no en cada hoja de vida. */
    public DetectorHabilidades(CatalogoHabilidades catalogo) {
        catalogo.categorias().values().forEach(lista -> lista.forEach(habilidad ->
                habilidades.add(new HabilidadCompilada(
                        habilidad.nombre(),
                        habilidad.alias().stream().map(DetectorHabilidades::patron).toList()))));
    }

    /** Devuelve los nombres visibles de las habilidades encontradas, en el orden del catálogo. */
    public Set<String> detectar(String texto) {
        Set<String> encontradas = new LinkedHashSet<>();
        if (texto == null || texto.isBlank()) {
            return encontradas;
        }
        String normalizado = normalizar(texto);
        for (HabilidadCompilada habilidad : habilidades) {
            if (habilidad.patrones().stream().anyMatch(p -> p.matcher(normalizado).find())) {
                encontradas.add(habilidad.nombre());
            }
        }
        return encontradas;
    }

    /** Minúsculas y sin tildes: "Inglés" y "INGLES" quedan iguales a "ingles". */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    /**
     * Coincidencia por palabra completa, para que "java" no aparezca dentro de "javascript"
     * ni "excel" dentro de "excelente".
     *
     * \b marca el límite entre una letra/número y algo que no lo es. En "c#" o "c++" el alias
     * termina en símbolo, y después del símbolo suele venir un espacio: entre "#" y " " no hay
     * límite \b, así que el patrón nunca coincidiría. Para esos bordes con símbolo se usa
     * "que no haya letra ni número al lado" (lookahead / lookbehind negativos).
     */
    private static Pattern patron(String alias) {
        String inicio = Character.isLetterOrDigit(alias.charAt(0)) ? "\\b" : "(?<![a-z0-9])";
        String fin = Character.isLetterOrDigit(alias.charAt(alias.length() - 1)) ? "\\b" : "(?![a-z0-9])";
        return Pattern.compile(inicio + Pattern.quote(alias) + fin);
    }
}
