package com.juangarrido.practicantes.analisis;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class ExtractorTextoPdf {

    /**
     * Devuelve el texto del PDF. Un documento escaneado (solo imágenes) devuelve texto vacío:
     * PDFBox no hace OCR.
     */
    public String extraer(Path rutaPdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(rutaPdf.toFile())) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
