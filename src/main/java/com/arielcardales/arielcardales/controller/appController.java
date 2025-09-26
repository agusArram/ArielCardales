package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.Tablas;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class appController {
    @FXML
    private TableView<Producto> tablaProductos;

    public void initialize() {
        String[][] columnas = {
                {"ID", "idProducto"},
                {"Nombre", "nombre"},
                {"Precio", "precio"},
                {"Stock", "stock"}
        };

        TableView<Producto> tabla = Tablas.crearTabla(Producto.class, columnas);
        tabla.setItems(FXCollections.observableArrayList(ProductoDAO.getAll()));

        // Reemplaza la tabla del FXML por la generada dinámicamente
        tablaProductos.getColumns().setAll(tabla.getColumns());
        tablaProductos.setItems(tabla.getItems());
    }
}
