package com.juangarrido.practicantes.practicante.dto;

import com.juangarrido.practicantes.practicante.EstadoPracticante;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambioEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPracticante estado,

        @Size(max = 500, message = "La observación no puede superar 500 caracteres")
        String observacion
) {
}
