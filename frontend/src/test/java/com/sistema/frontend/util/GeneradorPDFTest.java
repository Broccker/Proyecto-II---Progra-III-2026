package com.sistema.frontend.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneradorPDFTest {

    private static final String[] ENCABEZADOS = {"Categoría", "Reservas"};

    @Test
    @DisplayName("El PDF se cierra después de escribir el documento")
    void generaUnPdfConContenido(@TempDir Path carpeta) throws Exception {
        Path destino = carpeta.resolve("reporte.pdf");

        GeneradorPDF.generarTabla("Categorías reservadas", ENCABEZADOS,
                List.<String[]>of(new String[]{"Proyectores", "5"}), destino);

        assertTrue(Files.size(destino) > 0, "El archivo no puede quedar vacío.");
        assertTrue(esPdf(destino));
    }

    @Test
    @DisplayName("El PDF admite una imagen del gráfico")
    void generaUnPdfConGrafico(@TempDir Path carpeta) throws Exception {
        Path destino = carpeta.resolve("estadisticas.pdf");
        BufferedImage grafico = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);

        GeneradorPDF.generarTablaConGrafico("Actividades por semana", ENCABEZADOS,
                List.<String[]>of(new String[]{"02/11/2026", "3"}), grafico, destino);

        assertTrue(Files.size(destino) > 0);
        assertTrue(esPdf(destino));
    }

    private static boolean esPdf(Path archivo) throws Exception {
        byte[] contenido = Files.readAllBytes(archivo);
        return new String(contenido, 0, Math.min(5, contenido.length), StandardCharsets.ISO_8859_1)
                .startsWith("%PDF-");
    }
}
