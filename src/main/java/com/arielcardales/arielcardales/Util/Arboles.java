package com.arielcardales.arielcardales.Util;

import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;

public final class Arboles {
    private Arboles() {}

    public static <T> TreeTableView<T> crearTreeTabla(String[][] columnas) {
        TreeTableView<T> tree = new TreeTableView<>();
        for (String[] def : columnas) {
            TreeTableColumn<T, Object> col = new TreeTableColumn<>(def[0]); // visible
            col.setCellValueFactory(new TreeItemPropertyValueFactory<>(def[1])); // nombreProperty
            col.setPrefWidth(120);
            tree.getColumns().add(col);
        }
        tree.setShowRoot(false);


        return tree;
    }


}