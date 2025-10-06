package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.Producto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.List;

public class ExportadorExcel {

    public static void exportar(List<Producto> productos, String ruta) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Productos");

            // === Estilos ===
            // Encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);  //negrita
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Moneda
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("$ #,##0.00"));

            // Centrado
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            // === Encabezados ===
            String[] columnas = {"Etiqueta", "Nombre", "Descripción", "Categoría", "Precio", "Stock"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // === Filas ===
            int fila = 1;
            for (Producto prod : productos) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(prod.getEtiqueta());
                row.createCell(1).setCellValue(prod.getNombre());
                row.createCell(2).setCellValue(prod.getDescripcion());
                row.createCell(3).setCellValue(prod.getCategoria());

                // Precio con formato moneda
                Cell precioCell = row.createCell(4);
                precioCell.setCellValue(prod.getPrecio().doubleValue());
                precioCell.setCellStyle(currencyStyle);

                // Stock centrado
                Cell stockCell = row.createCell(5);
                stockCell.setCellValue(prod.getStockOnHand());
                stockCell.setCellStyle(centerStyle);
            }

            // === Ajustar columnas automáticamente ===
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Congelar encabezado
            sheet.createFreezePane(0, 1);

            // === Guardar archivo ===
            try (FileOutputStream out = new FileOutputStream(ruta)) {
                workbook.write(out);
            }
            System.out.println("✅ Exportado a Excel en " + ruta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
