package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.Producto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ExportadorPDF {

    public static void exportar(List<Producto> productos, String ruta) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(ruta));
            document.open();

            // Título
            Paragraph titulo = new Paragraph("Inventario de Productos",
                    new Font(Font.HELVETICA, 18, Font.BOLD));
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n")); // Espacio

            // Tabla con 6 columnas
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            // Reducimos ID (8f) y damos más a Precio (11f)
            table.setWidths(new float[]{7f, 25f, 31f, 13f, 16f, 7f}); //tamanios con fr ( % ) de las coolumnas

            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD); //tamanio y eso de la letra, abajo nombre de la columna
            addHeaderCell(table, "ID", headerFont);
            addHeaderCell(table, "Nombre", headerFont);
            addHeaderCell(table, "Descripción", headerFont);
            addHeaderCell(table, "Categoría", headerFont);
            addHeaderCell(table, "Precio", headerFont);
            addHeaderCell(table, "#", headerFont);

            // Formateador de moneda
            NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

            // Filas
            for (Producto prod : productos) {
                addBodyCell(table, prod.getEtiqueta());
                addBodyCell(table, prod.getNombre());
                addBodyCell(table, prod.getDescripcion());
                addBodyCell(table, prod.getCategoria());
                addBodyCell(table, formatoMoneda.format(prod.getPrecio())); // <-- formateado
                addBodyCell(table, String.valueOf(prod.getStockOnHand()));
            }
            document.add(table);
            document.close();
            System.out.println("✅ Exportado a PDF en " + ruta); //agregar mensaje dsp para que se vea en el .exe
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Encabezados con fondo gris y padding
    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    // Celdas normales con padding
    private static void addBodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setPadding(4f);
        table.addCell(cell);
    }
}
