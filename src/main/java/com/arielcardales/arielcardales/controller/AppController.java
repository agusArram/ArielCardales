package com.arielcardales.arielcardales.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

public class AppController {

    @FXML
    private VBox contenedorPrincipal;

    private void cargarVista(String rutaFXML) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent vista = loader.load();
            contenedorPrincipal.getChildren().setAll(vista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void mostrarProductos() {
        cargarVista("/fxml/producto.fxml");
    }

    @FXML
    private void mostrarVentas() {
        cargarVista("/fxml/ventas.fxml"); // futura vista
    }

    @FXML
    private void mostrarClientes() {
        cargarVista("/fxml/clientes.fxml"); // futura vista
    }
}
