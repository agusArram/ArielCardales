package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.Tablas;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.Locale;

public class AppController {

    @FXML
    private TableView<Producto> tablaProductos;

    public void initialize() {
        String[][] columnas = {
                {"Etiqueta", "etiqueta"},
                {"Nombre", "nombre"},
                {"Descripción", "descripcion"},
                {"Categoría", "categoria"},
                {"Precio", "precio"},
                {"Stock", "stockOnHand"}
        };

        TableView<Producto> tabla = Tablas.crearTabla(Producto.class, columnas);
        Tablas.formatearMoneda(tabla, "Precio", Locale.forLanguageTag("es-AR"));

        // CORREGIDO: uso de new ProductoDAO()
        tabla.setItems(FXCollections.observableArrayList(new ProductoDAO().findAll()));

        tablaProductos.getColumns().setAll(tabla.getColumns());
        tablaProductos.setItems(tabla.getItems());
    }
}
