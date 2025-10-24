package SORT_PROYECTS.AppInventario.Util;

import SORT_PROYECTS.AppInventario.Entidades.ItemInventario;
import SORT_PROYECTS.AppInventario.Entidades.Producto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.*;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ExportadorPDF {

    private static final Font FONT_TITLE  = new Font(Font.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY);
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
    private static final Font FONT_CELL   = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Color ZEBRA_1    = new Color(0xFA, 0xF1, 0xE1);
    private static final Color ZEBRA_2    = new Color(0xF1, 0xE1, 0xCA);
    private static final NumberFormat MONEDA_AR = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    /** ========= Exporta lista plana de Producto ========= */
    public static void exportarProductos(List<Producto> productos, String ruta) {
        try (FileOutputStream out = new FileOutputStream(ruta)) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            agregarHeaderFooter(writer);
            doc.open();

            agregarTitulo(doc, "Inventario de Productos");
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8f, 24f, 30f, 16f, 12f, 8f});

            addHeader(table, "Etiqueta");
            addHeader(table, "Nombre");
            addHeader(table, "Descripción");
            addHeader(table, "Categoría");
            addHeader(table, "Precio");
            addHeader(table, "Stock");

            int row = 0;
            for (Producto p : productos) {
                Color bg = (row++ % 2 == 0) ? ZEBRA_1 : ZEBRA_2;
                addCell(table, safe(p.getEtiqueta()), bg, Element.ALIGN_CENTER);
                addCell(table, safe(p.getNombre()),   bg, Element.ALIGN_LEFT);
                addCell(table, safe(p.getDescripcion()), bg, Element.ALIGN_LEFT);
                addCell(table, safe(p.getCategoria()), bg, Element.ALIGN_LEFT);
                addCell(table, p.getPrecio()!=null ? MONEDA_AR.format(p.getPrecio()) : "-", bg, Element.ALIGN_RIGHT);
                addCell(table, String.valueOf(p.getStockOnHand()), bg, Element.ALIGN_CENTER);
            }

            doc.add(table);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error exportando PDF: " + e.getMessage(), e);
        }
    }

    /** ========= Exporta el árbol visible (productos + variantes) ========= */
    public static void exportarInventarioTree(ItemInventario rootValue,
                                              List<ItemInventario> inOrderFlat,
                                              String ruta) {
        try (FileOutputStream out = new FileOutputStream(ruta)) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            agregarHeaderFooter(writer);
            doc.open();

            agregarTitulo(doc, "Inventario (vista actual)");
            Paragraph sub = new Paragraph(
                    "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                            "  •  Moneda: ARS",
                    new Font(Font.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY)
            );
            sub.setSpacingAfter(10f);
            doc.add(sub);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{10f, 28f, 10f, 10f, 18f, 10f, 10f, 6f});

            addHeader(table, "Etiqueta");
            addHeader(table, "Nombre / Variante");
            addHeader(table, "Color");
            addHeader(table, "Talle");
            addHeader(table, "Categoría");
            addHeader(table, "Costo");
            addHeader(table, "Precio");
            addHeader(table, "Stock");

            int row = 0;
            for (ItemInventario it : inOrderFlat) {
                Color bg = (row++ % 2 == 0) ? ZEBRA_1 : ZEBRA_2;

                String etiqueta = safe(it.getEtiquetaProducto());
                String nombre   = safe(it.getNombreProducto());
                String categoria= safe(it.getCategoria());
                String color    = it.isEsVariante() ? safe(it.getColor()) : "—";
                String talle    = it.isEsVariante() ? safe(it.getTalle()) : "—";
                String costo    = it.getCosto()!=null ? MONEDA_AR.format(it.getCosto()) : "—";
                String precio   = it.getPrecio()!=null ? MONEDA_AR.format(it.getPrecio()) : "—";
                String stock    = String.valueOf(it.getStockOnHand());

                String nombreCol = it.isEsVariante() ? "• " + nombre : nombre;

                addCell(table, etiqueta, bg, Element.ALIGN_CENTER);
                addCell(table, nombreCol, bg, Element.ALIGN_LEFT);
                addCell(table, color,    bg, Element.ALIGN_CENTER);
                addCell(table, talle,    bg, Element.ALIGN_CENTER);
                addCell(table, categoria,bg, Element.ALIGN_LEFT);
                addCell(table, costo,    bg, Element.ALIGN_RIGHT);
                addCell(table, precio,   bg, Element.ALIGN_RIGHT);
                addCell(table, stock,    bg, Element.ALIGN_CENTER);
            }

            doc.add(table);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error exportando PDF: " + e.getMessage(), e);
        }
    }

    // ---------------- helpers PDF ----------------
    private static void agregarTitulo(Document doc, String text) throws DocumentException {
        Paragraph titulo = new Paragraph(text, FONT_TITLE);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(12f);
        doc.add(titulo);
    }

    private static void addHeader(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_HEADER));
        c.setBackgroundColor(new Color(0xCF, 0xA9, 0x71)); // tono cuero cabecera
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6f);
        t.addCell(c);
    }

    private static void addCell(PdfPTable t, String text, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_CELL));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        c.setNoWrap(false);
        t.addCell(c);
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static void agregarHeaderFooter(PdfWriter writer) {
        writer.setPageEvent(new PdfPageEventHelper() {
            final Font f = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
            @Override
            public void onEndPage(PdfWriter w, Document d) {
                PdfContentByte cb = w.getDirectContent();
                Phrase left = new Phrase("Inventario Ariel", f);
                Phrase right = new Phrase("Página " + w.getPageNumber(), f);
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,  left,  d.left(),  d.bottom() - 10, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, right, d.right(), d.bottom() - 10, 0);
            }
        });
    }
}
