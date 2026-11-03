package com.sistema.frontend.util;

import com.lowagie.text.DocumentException;
import com.sistema.common.excepciones.SistemaException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


public final class Reportes {

    private Reportes() {
    }


    public static void generar(String titulo, String[] encabezados,
                               List<String[]> filas, Path destino) {
        generarConGrafico(titulo, encabezados, filas, null, destino);
    }


    public static void generarConGrafico(String titulo, String[] encabezados,
                                         List<String[]> filas, BufferedImage grafico,
                                         Path destino) {
        try {
            GeneradorPDF.generarTablaConGrafico(titulo, encabezados, filas, grafico, destino);
        } catch (DocumentException | IOException e) {
            throw new SistemaException("No se pudo generar el reporte PDF: "
                    + e.getMessage(), e);
        }
    }
}
