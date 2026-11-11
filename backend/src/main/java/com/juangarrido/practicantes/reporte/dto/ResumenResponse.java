package com.juangarrido.practicantes.reporte.dto;

import java.util.Map;

/**
 * KPIs de la vista de Reportes.
 * tasaViabilidad se calcula sobre los candidatos ya decididos, no sobre el total,
 * para que los pendientes no la bajen artificialmente.
 */
public record ResumenResponse(
        long total,
        long pendientes,
        long viables,
        long noViables,
        Double tasaViabilidad,
        Double promedioDiasDecision,
        Map<String, Long> porCarrera,
        Map<Integer, Long> porSemestre,
        Map<String, Long> registrosPorFecha
) {
}
