package com.arielcardales.arielcardales.Util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import java.text.NumberFormat;
import java.util.Locale;

public class Tablas {
    public static <T> TableView<T> crearTabla(Class<T> clase, String[][] columnas) {
        TableView<T> tabla = new TableView<>();
        for (String[] colDef : columnas) {
            TableColumn<T, Object> col = new TableColumn<>(colDef[0]);
            col.setCellValueFactory(new PropertyValueFactory<>(colDef[1]));
            tabla.getColumns().add(col);
        }
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return tabla;
    }

    public static <T> void formatearMoneda(TableView<T> tabla, String header, Locale locale) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        tabla.getColumns().stream()
                .filter(c -> header.equals(c.getText()))
                .findFirst()
                .ifPresent(c -> ((TableColumn<T, Object>) c).setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Object val, boolean empty) {
                        super.updateItem(val, empty);
                        if (empty || val == null) {
                            setText(null);
                        } else {
                            setText(nf.format(val));
                        }
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                }));
    }
}

