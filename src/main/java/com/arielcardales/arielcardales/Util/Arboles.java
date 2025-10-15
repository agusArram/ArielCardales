package com.arielcardales.arielcardales.Util;

import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class Arboles {

    /**
     * Crea un TreeTableView con columnas dinámicas
     * Matriz: {título, propiedad}
     */
    public static <T> TreeTableView<T> crearTreeTabla(String[][] columnas) {
        TreeTableView<T> tree = new TreeTableView<>();
        tree.setShowRoot(false);

        for (String[] colDef : columnas) {
            String titulo = colDef[0];
            String propiedad = colDef[1];

            TreeTableColumn<T, Object> col = new TreeTableColumn<>(titulo);
            col.setId(propiedad);
            col.setCellValueFactory(new TreeItemPropertyValueFactory<>(propiedad));

            // Aplicar formato según tipo
            aplicarFormato(col, propiedad);

            // Alineación
            if (propiedad.contains("nombre") || propiedad.contains("descripcion")) {
                col.getStyleClass().add("col-left");
            } else {
                col.getStyleClass().add("col-center");
            }

            tree.getColumns().add(col);
        }

        return tree;
    }

    /**
     * Aplica formato automático según el nombre de la propiedad
     */
    private static <T> void aplicarFormato(TreeTableColumn<T, Object> col, String propiedad) {
        String propLower = propiedad.toLowerCase();

        // Formato de moneda
        if (propLower.contains("precio") || propLower.contains("total") ||
                propLower.contains("costo") || propLower.contains("subtotal")) {
            col.setCellFactory(tc -> new javafx.scene.control.TreeTableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                        if (item instanceof BigDecimal) {
                            setText(formato.format((BigDecimal) item));
                        } else if (item instanceof Number) {
                            setText(formato.format(((Number) item).doubleValue()));
                        } else {
                            setText(item.toString());
                        }
                    }
                }
            });
        }
        // Formato de enteros
        else if (propLower.contains("stock") || propLower.equals("qty") || propLower.equals("cantidad")) {
            col.setCellFactory(tc -> new javafx.scene.control.TreeTableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else if (item instanceof Number) {
                        setText(String.format("%d", ((Number) item).intValue()));
                    } else {
                        setText(item.toString());
                    }
                }
            });
        }
    }
}