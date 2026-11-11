package com.juangarrido.practicantes.practicante.dto;

import com.juangarrido.practicantes.practicante.EstadoPracticante;
import com.juangarrido.practicantes.practicante.Practicante;

import java.time.LocalDateTime;
import java.util.List;

/** No expone la ruta interna del archivo, solo el nombre original para mostrarlo. */
public record PracticanteResponse(
        Long id,
        String nombreCompleto,
        String correo,
        String carrera,
        Integer semestre,
        String nombreHojaVida,
        EstadoPracticante estado,
        String observacion,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaDecision,
        List<String> habilidadesDetectadas,
        boolean textoExtraible
) {

    public static PracticanteResponse desde(Practicante p) {
        return new PracticanteResponse(
                p.getId(),
                p.getNombreCompleto(),
                p.getCorreo(),
                p.getCarrera(),
                p.getSemestre(),
                p.getNombreOriginalHojaVida(),
                p.getEstado(),
                p.getObservacion(),
                p.getFechaRegistro(),
                p.getFechaDecision(),
                // Copia: la colección de la entidad es perezosa y no se puede leer fuera de la transacción.
                List.copyOf(p.getHabilidadesDetectadas()),
                p.isTextoExtraible()
        );
    }
}
