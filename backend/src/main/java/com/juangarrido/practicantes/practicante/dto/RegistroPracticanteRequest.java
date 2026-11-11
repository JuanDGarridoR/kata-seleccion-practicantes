package com.juangarrido.practicantes.practicante.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroPracticanteRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "La carrera es obligatoria")
        @Size(max = 120, message = "La carrera no puede superar 120 caracteres")
        String carrera,

        @NotNull(message = "El semestre es obligatorio")
        @Min(value = 1, message = "El semestre debe estar entre 1 y 12")
        @Max(value = 12, message = "El semestre debe estar entre 1 y 12")
        Integer semestre
) {
}
