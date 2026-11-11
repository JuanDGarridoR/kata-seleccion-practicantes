package com.juangarrido.practicantes.reporte;

import com.juangarrido.practicantes.reporte.dto.ResumenResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    /** BOM UTF-8 para que Excel muestre bien las tildes al abrir el CSV con doble clic. */
    private static final String BOM = "﻿";

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return service.resumen();
    }

    @GetMapping("/practicantes.csv")
    public ResponseEntity<byte[]> exportarCsv() {
        byte[] contenido = (BOM + service.csv()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("practicantes.csv").build().toString())
                .body(contenido);
    }
}
