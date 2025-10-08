package com.arielcardales.arielcardales.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;


public class AppController {

    private ProductoController productoController;

    @FXML
    private VBox contenedorPrincipal;
    private Parent vistaProductos;

    @FXML
    public void initialize() {
        // Cargar el inventario (productos) directamente al iniciar la app
        mostrarProductos();
    }


    /** Método genérico: carga rápida de vistas simples (sin Task) **/
    private void cargarVista(String rutaFXML) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent vista = loader.load();
            contenedorPrincipal.getChildren().setAll(vista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Carga asíncrona de la vista de productos **/
    @FXML
    private void mostrarProductos() {
        contenedorPrincipal.getChildren().clear();

        // Spinner de carga
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(50, 50);
        Label cargando = new Label("Cargando inventario...");
        VBox box = new VBox(10, spinner, cargando);
        box.setStyle("-fx-alignment: center; -fx-padding: 50;");
        contenedorPrincipal.getChildren().add(box);

        Task<Parent> tareaCarga = new Task<>() {
            @Override
            protected Parent call() throws Exception {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/producto.fxml"));
                Parent vista = loader.load();
                productoController = loader.getController(); // 🔗 Guardamos referencia
                return vista;
            }
        };

        tareaCarga.setOnSucceeded(e -> {
            vistaProductos = tareaCarga.getValue();
            VBox.setMargin(vistaProductos, new Insets(0));
            contenedorPrincipal.getChildren().setAll(vistaProductos);
        });

        tareaCarga.setOnFailed(e -> {
            Label error = new Label("❌ Error al cargar vista de productos.");
            contenedorPrincipal.getChildren().setAll(error);
            tareaCarga.getException().printStackTrace();
        });

        new Thread(tareaCarga).start();
    }


    /** 🔁 Restaurar inventario sin recargar toda la vista **/
    @FXML
    public  void restaurarInventarioCompleto() {
        try {
            if (productoController != null) {
                productoController.restaurarInventarioCompleto();
                System.out.println("Inventario restaurado desde AppController (sin recargar FXML).");
            } else {
                mostrarProductos(); // fallback, si todavía no está inicializado
            }
        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("❌ Error al restaurar inventario");
            contenedorPrincipal.getChildren().setAll(error);
        }
    }
    /** Futura vista de ventas **/
    @FXML
    private void mostrarVentas() {
        cargarVista("/fxml/ventas.fxml");
    }

    /** Futura vista de clientes **/
    @FXML
    private void mostrarClientes() {
        cargarVista("/fxml/clientes.fxml");
    }
}
