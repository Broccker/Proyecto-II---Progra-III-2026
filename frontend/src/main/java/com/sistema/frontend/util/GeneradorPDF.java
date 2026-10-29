package com.sistema.frontend.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Genera reportes PDF con una tabla y, opcionalmente, un gráfico.
 *
 * <p>La generación ocurre en el frontend: el backend solo entrega los datos.</p>
 */
public final class GeneradorPDF {

    private GeneradorPDF() {
    }

    /**
     * Genera un PDF con el título y la tabla indicados.
     */
    public static void generarTabla(String titulo, String[] encabezados,
                                    List<String[]> filas, Path destino)
            throws DocumentException, IOException {
        generarTablaConGrafico(titulo, encabezados, filas, null, destino);
    }

    /**
     * Genera un PDF con el título, un gráfico opcional y la tabla indicada.
     */
    public static void generarTablaConGrafico(String titulo, String[] encabezados,
                                              List<String[]> filas, BufferedImage imagen,
                                              Path destino)
            throws DocumentException, IOException {
        Document documento = new Document(PageSize.A4.rotate());
        try (OutputStream salida = Files.newOutputStream(destino)) {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph parrafoTitulo = new Paragraph(titulo, fuenteTitulo);
            parrafoTitulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(parrafoTitulo);
            documento.add(new Paragraph(" "));

            if (imagen != null) {
                ByteArrayOutputStream memoria = new ByteArrayOutputStream();
                ImageIO.write(imagen, "png", memoria);
                Image grafico = Image.getInstance(memoria.toByteArray());
                grafico.scaleToFit(780, 260);
                grafico.setAlignment(Element.ALIGN_CENTER);
                documento.add(grafico);
                documento.add(new Paragraph(" "));
            }

            PdfPTable tabla = new PdfPTable(encabezados.length);
            tabla.setWidthPercentage(100);
            for (String encabezado : encabezados) {
                PdfPCell celda = new PdfPCell(new Phrase(encabezado,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                celda.setBackgroundColor(new Color(0xE0, 0xE0, 0xE0));
                tabla.addCell(celda);
            }
            for (String[] fila : filas) {
                for (String celda : fila) {
                    tabla.addCell(new PdfPCell(new Phrase(celda)));
                }
            }
            documento.add(tabla);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
    }
}
