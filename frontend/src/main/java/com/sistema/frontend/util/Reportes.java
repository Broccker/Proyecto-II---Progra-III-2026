package com.sistema.frontend.util;

import com.lowagie.text.DocumentException;
import com.sistema.common.excepciones.SistemaException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Fachada de los reportes PDF del frontend.
 *
 * <p>Convierte los fallos de entrada/salida de OpenPDF en una excepción de
 * dominio, para que los controladores informen al usuario sin propagar
 * detalles técnicos.</p>
 */
public final class Reportes {

    private Reportes() {
    }

    /**
     * Genera un PDF con el título y la tabla indicados.
     *
     * @throws SistemaException si no se puede escribir el archivo
     */
    public static void generar(String titulo, String[] encabezados,
                               List<String[]> filas, Path destino) {
        try {
            GeneradorPDF.generarTabla(titulo, encabezados, filas, destino);
        } catch (DocumentException | IOException e) {
            throw new SistemaException("No se pudo generar el reporte PDF: "
                    + e.getMessage(), e);
        }
    }
}
