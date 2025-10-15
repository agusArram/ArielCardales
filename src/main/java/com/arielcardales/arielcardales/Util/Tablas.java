package com.arielcardales.arielcardales.Util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Tablas {

    // <t> significa que cualquier entidad puede usar esta tabla, recibe matriz de tipo:
    //título, propiedad, peso, ancho
    public static <T> List<TableColumn<T, ?>> crearColumnas(String[][] columnas) {
        List<TableColumn<T, ?>> lista = new ArrayList<>();

        for (String[] colDef : columnas) {
            //aca abajo recorre la matriz
            String titulo = colDef[0];
            String propiedad = colDef[1]; //nombre del getter

            //antes era asi :
            //col.setCellValueFactory(new PropertyValueFactory<>(propiedad));
            //con lo nuevo es dinamico para cualquier entidad

            double peso = Double.parseDouble(colDef[2]);
            double minWidth = Double.parseDouble(colDef[3]);

            TableColumn<T, Object> col = new TableColumn<>(titulo);

            // 🔑 clave: ID de la columna con el nombre de la propiedad
            col.setId(propiedad);

            col.setCellValueFactory(new PropertyValueFactory<>(propiedad));
            col.setMinWidth(minWidth);
            col.setUserData(peso); // lo leemos luego en el controller

            // 🎨 Aplicar formato según el tipo de columna
            aplicarFormato(col, propiedad);

            // Alineación por CSS
            if (propiedad.equals("descripcion")) col.getStyleClass().add("col-left");
            else col.getStyleClass().add("col-center");

            lista.add(col);
        }
        return lista;
    }

    // ==========================
    // FORMATO DE MONEDA ($)
    // ==========================

    /**
     * Aplica el formato correcto según el nombre de la propiedad
     */
    private static <T> void aplicarFormato(TableColumn<T, Object> col, String propiedad) {
        String propLower = propiedad.toLowerCase();

        // Detectar columnas de precio/dinero
        if (propLower.contains("precio") || propLower.contains("total") ||
                propLower.contains("costo") || propLower.contains("subtotal") ||
                propLower.contains("monto")) {
            col.setCellFactory(formatearMoneda());
        }
        // Detectar columnas de stock/cantidad entera
        else if (propLower.contains("stock") || propLower.equals("qty") ||
                propLower.equals("cantidad")) {
            col.setCellFactory(formatearEntero());
        }
    }

    private static <T> Callback<TableColumn<T, Object>, javafx.scene.control.TableCell<T, Object>> formatearMoneda() {

        // Devuelve una "fábrica" de celdas personalizadas
        return columna -> new javafx.scene.control.TableCell<>() {

            // Este método se llama cada vez que una celda cambia su valor
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                // Si la celda está vacía , queda vacia
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                // Formatea el número como moneda argentina
                NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

                // Manejar tanto BigDecimal como otros tipos numéricos
                if (item instanceof BigDecimal) {
                    setText(formato.format((BigDecimal) item));
                } else if (item instanceof Number) {
                    setText(formato.format(((Number) item).doubleValue()));
                } else {
                    setText(item.toString());
                }
            }
        };
    }
    
    // ==========================
    // FORMATO DE ENTEROS (STOCK)
    // ==========================
    private static <T> Callback<TableColumn<T, Object>, javafx.scene.control.TableCell<T, Object>> formatearEntero() {

        return columna -> new javafx.scene.control.TableCell<>() {

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof Number) {
                    // Convierte el número a entero sin decimales
                    setText(String.format("%d", ((Number) item).intValue()));
                } else {
                    setText(item.toString());
                }
            }
        };
    }

}
