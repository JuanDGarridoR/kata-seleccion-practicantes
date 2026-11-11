package com.juangarrido.practicantes.reporte.dto;

import com.juangarrido.practicantes.practicante.EstadoPracticante;

import java.time.LocalDateTime;

/** Fila plana para Power BI. Sin nombre, correo ni rutas de archivo (minimización de datos personales). */
public record FilaDatasetResponse(
        Long id,
        String carrera,
        Integer semestre,
        EstadoPracticante estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaDecision,
        Double diasHastaDecision
) {
}
