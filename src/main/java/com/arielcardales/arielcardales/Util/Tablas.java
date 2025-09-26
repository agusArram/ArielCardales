package com.arielcardales.arielcardales.Util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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
}

