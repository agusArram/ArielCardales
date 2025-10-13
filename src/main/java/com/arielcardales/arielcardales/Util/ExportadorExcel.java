package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Entidades.Producto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ExportadorExcel {

    private static final NumberFormat MONEDA_AR = NumberFormat.getCurrencyInstance(new Locale("es","AR"));

    /** Lista plana (compatibilidad) */
    public static void exportarProductos(List<Producto> productos, String ruta) {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(ruta)) {
            Sheet sh = wb.createSheet("Productos");
            Styles st = new Styles(wb);

            // headers
            String[] cols = {"Etiqueta","Nombre","Descripción","Categoría","Precio","Stock"};
            Row rh = sh.createRow(0);
            for (int i=0;i<cols.length;i++){
                Cell c = rh.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(st.header);
            }

            int r=1;
            for (Producto p: productos) {
                Row row = sh.createRow(r++);
                set(row,0,p.getEtiqueta(), st.text);
                set(row,1,p.getNombre(),   st.text);
                set(row,2,p.getDescripcion(), st.wrap);
                set(row,3,p.getCategoria(), st.text);
                set(row,4,formatMoney(p.getPrecio()), st.money);
                set(row,5,String.valueOf(p.getStockOnHand()), st.center);
            }

            for (int i=0;i<cols.length;i++) sh.autoSizeColumn(i);
            wb.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Error exportando Excel: " + e.getMessage(), e);
        }
    }

    /** Árbol a Excel (lo visible) */
    public static void exportarInventarioTree(List<ItemInventario> flat, String ruta) {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(ruta)) {
            Sheet sh = wb.createSheet("Inventario");
            Styles st = new Styles(wb);

            String[] cols = {"Etiqueta","Nombre / Variante","Color","Talle","Categoría","Costo","Precio","Stock"};
            Row rh = sh.createRow(0);
            for (int i=0;i<cols.length;i++){ Cell c=rh.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(st.header); }

            int r=1;
            for (ItemInventario it: flat) {
                Row row = sh.createRow(r++);
                String nombre = it.isEsVariante() ? "• " + nullSafe(it.getNombreProducto()) : nullSafe(it.getNombreProducto());
                set(row,0, nullSafe(it.getEtiquetaProducto()), st.center);
                set(row,1, nombre, st.wrap);
                set(row,2, it.isEsVariante()? nullSafe(it.getColor()) : "—", st.center);
                set(row,3, it.isEsVariante()? nullSafe(it.getTalle()) : "—", st.center);
                set(row,4, nullSafe(it.getCategoria()), st.text);
                set(row,5, formatMoney(it.getCosto()), st.money);
                set(row,6, formatMoney(it.getPrecio()), st.money);
                set(row,7, String.valueOf(it.getStockOnHand()), st.center);
            }

            for (int i=0;i<cols.length;i++) sh.autoSizeColumn(i);
            wb.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Error exportando Excel: " + e.getMessage(), e);
        }
    }

    // -------- helpers --------
    private static String nullSafe(String s){ return (s==null||s.isBlank())? "—" : s; }
    private static String formatMoney(BigDecimal b){ return b==null? "—" : MONEDA_AR.format(b); }

    private static void set(Row r, int col, String v, CellStyle st){
        Cell c = r.createCell(col);
        c.setCellValue(v);
        c.setCellStyle(st);
    }

    private static class Styles {
        final CellStyle header, text, wrap, center, money;
        Styles(Workbook wb){
            // header
            Font fh = wb.createFont(); fh.setBold(true);
            header = wb.createCellStyle();
            header.setFillForegroundColor(IndexedColors.TAN.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setBorderBottom(BorderStyle.THIN);
            header.setFont(fh);

            text = wb.createCellStyle();
            text.setVerticalAlignment(VerticalAlignment.CENTER);

            wrap = wb.createCellStyle();
            wrap.cloneStyleFrom(text);
            wrap.setWrapText(true);

            center = wb.createCellStyle();
            center.cloneStyleFrom(text);
            center.setAlignment(HorizontalAlignment.CENTER);

            money = wb.createCellStyle();
            money.cloneStyleFrom(text);
            DataFormat df = wb.createDataFormat();
            money.setDataFormat(df.getFormat("\"$\" #,##0.00")); // visual
            money.setAlignment(HorizontalAlignment.RIGHT);
        }
    }
}
