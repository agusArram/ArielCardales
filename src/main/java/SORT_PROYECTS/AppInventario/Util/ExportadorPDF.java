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

    /** ========= Exporta estadísticas del Dashboard ========= */
    public static void exportarDashboard(
            java.util.Map<String, Object> metricasInventario,
            java.util.Map<String, Object> metricasVentas,
            List<String[]> topProductos,
            String periodo,
            String ruta) {
        try (FileOutputStream out = new FileOutputStream(ruta)) {
            Document doc = new Document(PageSize.A4, 36, 36, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            agregarHeaderFooter(writer);
            doc.open();

            // Título principal
            agregarTitulo(doc, "Dashboard - Estadísticas del Negocio");

            // Subtítulo con fecha y período
            Paragraph sub = new Paragraph(
                "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                "  •  Período: " + periodo,
                new Font(Font.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY)
            );
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20f);
            doc.add(sub);

            // === MÉTRICAS DE INVENTARIO ===
            Paragraph tituloInv = new Paragraph("📦 Métricas de Inventario",
                new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0x2E, 0x7D, 0x32)));
            tituloInv.setSpacingAfter(10f);
            doc.add(tituloInv);

            PdfPTable tableInv = new PdfPTable(4);
            tableInv.setWidthPercentage(100);
            tableInv.setWidths(new float[]{25f, 25f, 25f, 25f});

            // Headers inventario
            addMetricHeader(tableInv, "Total Productos");
            addMetricHeader(tableInv, "Stock Bajo");
            addMetricHeader(tableInv, "Sin Stock");
            addMetricHeader(tableInv, "Valor Total");

            // Valores inventario
            addMetricValue(tableInv, String.valueOf(metricasInventario.get("totalProductos")));
            addMetricValue(tableInv, String.valueOf(metricasInventario.get("stockBajo")));
            addMetricValue(tableInv, String.valueOf(metricasInventario.get("sinStock")));
            addMetricValue(tableInv, MONEDA_AR.format(metricasInventario.get("valorTotal")));

            doc.add(tableInv);
            doc.add(new Paragraph(" ")); // Espacio

            // === MÉTRICAS DE VENTAS ===
            Paragraph tituloVentas = new Paragraph("💰 Métricas de Ventas (últimos 30 días)",
                new Font(Font.HELVETICA, 14, Font.BOLD, new Color(25, 118, 210)));
            tituloVentas.setSpacingAfter(10f);
            doc.add(tituloVentas);

            PdfPTable tableVentas = new PdfPTable(3);
            tableVentas.setWidthPercentage(100);
            tableVentas.setWidths(new float[]{33f, 34f, 33f});

            // Headers ventas
            addMetricHeader(tableVentas, "Ventas Hoy");
            addMetricHeader(tableVentas, "Total 30 días");
            addMetricHeader(tableVentas, "Promedio Diario");

            // Valores ventas
            addMetricValue(tableVentas, MONEDA_AR.format(metricasVentas.get("ventasHoy")));
            addMetricValue(tableVentas, MONEDA_AR.format(metricasVentas.get("montoTotal")));
            addMetricValue(tableVentas, MONEDA_AR.format(metricasVentas.get("promedioVentaDiaria")));

            doc.add(tableVentas);
            doc.add(new Paragraph(" ")); // Espacio

            // === TOP 5 PRODUCTOS ===
            Paragraph tituloTop = new Paragraph("🏆 Top 5 Productos Más Vendidos (últimos 30 días)",
                new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0xF5, 0x7C, 0x00)));
            tituloTop.setSpacingAfter(10f);
            doc.add(tituloTop);

            PdfPTable tableTop = new PdfPTable(4);
            tableTop.setWidthPercentage(100);
            tableTop.setWidths(new float[]{10f, 50f, 20f, 20f});

            addHeader(tableTop, "#");
            addHeader(tableTop, "Producto");
            addHeader(tableTop, "Cantidad");
            addHeader(tableTop, "Total Ventas");

            int row = 0;
            for (String[] producto : topProductos) {
                Color bg = (row++ % 2 == 0) ? ZEBRA_1 : ZEBRA_2;
                addCell(tableTop, producto[0], bg, Element.ALIGN_CENTER); // Rank
                addCell(tableTop, producto[1], bg, Element.ALIGN_LEFT);   // Nombre
                addCell(tableTop, producto[2], bg, Element.ALIGN_CENTER); // Cantidad
                addCell(tableTop, producto[3], bg, Element.ALIGN_RIGHT);  // Total
            }

            doc.add(tableTop);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error exportando Dashboard a PDF: " + e.getMessage(), e);
        }
    }

    private static void addMetricHeader(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(new Color(0x5D, 0x4E, 0x37)); // Marrón oscuro
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(8f);
        t.addCell(c);
    }

    private static void addMetricValue(PdfPTable t, String value) {
        PdfPCell c = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK)));
        c.setBackgroundColor(new Color(0xF5, 0xF5, 0xF5)); // Gris claro
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(10f);
        t.addCell(c);
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

    /** ========= Exporta listado de ventas ========= */
    public static void exportarVentas(
            List<String[]> ventasData,
            String periodoTexto,
            int totalVentas,
            String montoTotal,
            String ruta) {
        try (FileOutputStream out = new FileOutputStream(ruta)) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            agregarHeaderFooter(writer);
            doc.open();

            // Título principal
            agregarTitulo(doc, "Listado de Ventas");

            // Subtítulo con fecha y período
            Paragraph sub = new Paragraph(
                "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                "  •  " + periodoTexto,
                new Font(Font.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY)
            );
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15f);
            doc.add(sub);

            // === RESUMEN ===
            PdfPTable tableResumen = new PdfPTable(2);
            tableResumen.setWidthPercentage(40);
            tableResumen.setHorizontalAlignment(Element.ALIGN_LEFT);
            tableResumen.setWidths(new float[]{50f, 50f});

            addMetricHeader(tableResumen, "Total Ventas");
            addMetricHeader(tableResumen, "Monto Total");

            addMetricValue(tableResumen, String.valueOf(totalVentas));
            addMetricValue(tableResumen, montoTotal);

            doc.add(tableResumen);
            doc.add(new Paragraph(" ")); // Espacio

            // === TABLA DE VENTAS ===
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{6f, 14f, 16f, 12f, 12f, 24f, 8f, 12f});

            addHeader(table, "ID");
            addHeader(table, "Fecha");
            addHeader(table, "Cliente");
            addHeader(table, "Medio Pago");
            addHeader(table, "Etiqueta");
            addHeader(table, "Producto");
            addHeader(table, "Cant.");
            addHeader(table, "Total");

            int row = 0;
            for (String[] venta : ventasData) {
                Color bg = (row++ % 2 == 0) ? ZEBRA_1 : ZEBRA_2;

                for (int i = 0; i < venta.length; i++) {
                    int align = (i == 0 || i == 6) ? Element.ALIGN_CENTER : // ID y Cantidad
                               (i == 7) ? Element.ALIGN_RIGHT :  // Total
                               Element.ALIGN_LEFT;              // Resto
                    addCell(table, safe(venta[i]), bg, align);
                }
            }

            doc.add(table);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error exportando ventas a PDF: " + e.getMessage(), e);
        }
    }

    private static void agregarHeaderFooter(PdfWriter writer) {
        writer.setPageEvent(new PdfPageEventHelper() {
            final Font f = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
            @Override
            public void onEndPage(PdfWriter w, Document d) {
                PdfContentByte cb = w.getDirectContent();
                Phrase left = new Phrase("SORT_PROYECTS - AppInventario", f);
                Phrase right = new Phrase("Página " + w.getPageNumber(), f);
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,  left,  d.left(),  d.bottom() - 10, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, right, d.right(), d.bottom() - 10, 0);
            }
        });
    }
}
