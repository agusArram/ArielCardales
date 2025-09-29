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

    public static <T> List<TableColumn<T, ?>> crearColumnas(String[][] columnas) {
        List<TableColumn<T, ?>> lista = new ArrayList<>();

        for (String[] colDef : columnas) {
            String titulo = colDef[0];
            String propiedad = colDef[1];
            double peso = Double.parseDouble(colDef[2]);
            double minWidth = Double.parseDouble(colDef[3]);

            TableColumn<T, Object> col = new TableColumn<>(titulo);

            // 🔑 clave: ID de la columna con el nombre de la propiedad
            col.setId(propiedad);

            col.setCellValueFactory(new PropertyValueFactory<>(propiedad));
            col.setMinWidth(minWidth);
            col.setUserData(peso); // lo leemos luego en el controller

            if (propiedad.equalsIgnoreCase("precio")) {
                col.setCellFactory(formatearMoneda());
            } else if (propiedad.toLowerCase().contains("stock")) {
                col.setCellFactory(formatearEntero());
            }

            // Alineación por CSS
            if (propiedad.equals("descripcion")) {
                col.getStyleClass().add("col-left");
            } else {
                col.getStyleClass().add("col-center");
            }

            lista.add(col);
        }
        return lista;
    }

    private static <T> Callback<TableColumn<T, Object>, javafx.scene.control.TableCell<T, Object>> formatearMoneda() {
        return col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                setText(nf.format((BigDecimal) item));
            }
        };
    }

    private static <T> Callback<TableColumn<T, Object>, javafx.scene.control.TableCell<T, Object>> formatearEntero() {
        return col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%d", item));
            }
        };
    }
}
