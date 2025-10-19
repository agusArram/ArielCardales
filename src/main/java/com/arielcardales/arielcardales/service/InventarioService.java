package com.arielcardales.arielcardales.service;

import com.arielcardales.arielcardales.DAO.*;
import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Util.Mapper;
import com.arielcardales.arielcardales.controller.ProductoTreeController;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import java.util.prefs.Preferences;

public class InventarioService {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final ProductoVarianteDAO varianteDAO = new ProductoVarianteDAO();

    // 🔹 Cargar todo el árbol del inventario
    public TreeItem<ItemInventario> cargarArbol(String filtro) throws SQLException {
        TreeItem<ItemInventario> root = inventarioDAO.cargarArbol(filtro);

        limpiarCamposPadres(root);

        // 📦 Leemos preferencia del usuario
        Preferences prefs = Preferences.userNodeForPackage(ProductoTreeController.class);
        boolean expandir = prefs.getBoolean("expandir_nodos_hijos", false);

        if (expandir) {
            expandirNodos(root);
        }

        return root;
    }

    private void limpiarCamposPadres(TreeItem<ItemInventario> nodo) {
        if (nodo == null) return;

        ItemInventario item = nodo.getValue();
        if (item != null && !item.isEsVariante()) {
            boolean tieneHijos = nodo.getChildren() != null && !nodo.getChildren().isEmpty();
            if (tieneHijos) {
                // 🔸 Limpiar todos los campos que no aplican a productos base
                item.setColor(null);
                item.setTalle(null);
                item.setCosto(null);
                item.setPrecio(null);
                item.setStockOnHand(0);
            }
        }

        // 🔁 Recurre en todos los hijos
        if (nodo.getChildren() != null) {
            for (TreeItem<ItemInventario> hijo : nodo.getChildren()) {
                limpiarCamposPadres(hijo);
            }
        }
    }

    // 🔽 Expande automáticamente los productos con hijos
    private void expandirNodos(TreeItem<ItemInventario> nodo) {
        if (nodo == null) return;

        boolean tieneHijos = nodo.getChildren() != null && !nodo.getChildren().isEmpty();
        if (tieneHijos) {
            nodo.setExpanded(true); // 🔓 abre el nodo
            for (TreeItem<ItemInventario> hijo : nodo.getChildren()) {
                expandirNodos(hijo); // 🔁 recursivo
            }
        }
    }

    /**
     * Método unificado para actualizar un campo de un ItemInventario
     * Centraliza toda la lógica de validación y delegación a los DAOs correctos
     *
     * @param item ItemInventario a actualizar
     * @param campo Nombre del campo
     * @param valor Nuevo valor como String
     * @return ResultadoActualizacion con éxito/error y mensaje
     */
    public ResultadoActualizacion actualizarCampo(ItemInventario item, String campo, String valor) {
        try {
            // ════════════════════════════════════════════════════════════
            // VALIDACIONES DE NEGOCIO
            // ════════════════════════════════════════════════════════════

            // 1. Validar que no se editen campos de productos base que no aplican
            if (!item.isEsVariante()) {
                if (campo.equalsIgnoreCase("color") || campo.equalsIgnoreCase("talle")) {
                    return ResultadoActualizacion.error(
                        "Los productos base no tienen color/talle. Solo las variantes."
                    );
                }
            }

            // 2. Validar que las variantes no editen campos heredados del padre
            if (item.isEsVariante()) {
                if (campo.equalsIgnoreCase("categoria")) {
                    return ResultadoActualizacion.error(
                        "Las variantes heredan la categoría del producto base"
                    );
                }
                if (campo.equalsIgnoreCase("nombre")) {
                    return ResultadoActualizacion.error(
                        "Las variantes heredan el nombre del producto base"
                    );
                }
            }

            // ════════════════════════════════════════════════════════════
            // DELEGACIÓN A DAOs
            // ════════════════════════════════════════════════════════════

            boolean exitoso;

            if (item.isEsVariante()) {
                // Delegar a ProductoVarianteDAO
                exitoso = varianteDAO.updateCampo(item.getVarianteId(), campo, valor);

                // Validación especial: detectar duplicado de color/talle
                if (!exitoso && (campo.equalsIgnoreCase("color") || campo.equalsIgnoreCase("talle"))) {
                    return ResultadoActualizacion.error(
                        "Ya existe una variante con esa combinación de color y talle"
                    );
                }
            } else {
                // Delegar a ProductoDAO
                exitoso = productoDAO.updateCampo(item.getProductoId(), campo, valor);
            }

            if (exitoso) {
                return ResultadoActualizacion.exito("Campo actualizado correctamente");
            } else {
                return ResultadoActualizacion.error("No se pudo actualizar el campo");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResultadoActualizacion.error("Error: " + e.getMessage());
        }
    }

    /**
     * Clase para encapsular el resultado de una actualización
     */
    public static class ResultadoActualizacion {
        private final boolean exitoso;
        private final String mensaje;

        private ResultadoActualizacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }

        public static ResultadoActualizacion exito(String mensaje) {
            return new ResultadoActualizacion(true, mensaje);
        }

        public static ResultadoActualizacion error(String mensaje) {
            return new ResultadoActualizacion(false, mensaje);
        }

        public boolean isExitoso() {
            return exitoso;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    // 🔹 Eliminar un producto base
    public boolean eliminarProducto(long productoId) {
        try {
            productoDAO.deleteById(productoId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Eliminar una variante
    public boolean eliminarVariante(long varianteId) {
        try {
            varianteDAO.deleteById(varianteId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Descontar stock después de una venta
    public boolean descontarStock(long productoId, int cantidad) {
        try {
            return productoDAO.descontarStock(productoId, cantidad);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Muestra ventana modal para seleccionar productos del inventario
     *
     * @param owner Ventana padre
     * @return Optional con el ItemInventario seleccionado
     */
    public Optional<ItemInventario> mostrarSelectorProductos(Stage owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Seleccionar Producto");

        // Usar helper Arboles para crear TreeTableView
        String[][] columnas = {
                {"Etiqueta", "etiquetaProducto"},
                {"Nombre", "nombreProducto"},
                {"Color", "color"},
                {"Talle", "talle"},
                {"Stock", "stockOnHand"},
                {"Precio", "precio"}
        };

        TreeTableView<ItemInventario> tabla = com.arielcardales.arielcardales.Util.Arboles.crearTreeTabla(columnas);
        tabla.setPrefHeight(400);

        // Campo de búsqueda
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("🔍 Buscar producto...");
        txtBuscar.setPrefWidth(680);

        // Cargar datos iniciales
        try {
            TreeItem<ItemInventario> root = cargarArbol("");
            tabla.setRoot(root);
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("No se pudo cargar el inventario: " + e.getMessage());
            alert.showAndWait();
            return Optional.empty();
        }

        // Búsqueda reactiva
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                TreeItem<ItemInventario> root = cargarArbol(newVal != null ? newVal : "");
                tabla.setRoot(root);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // Variable para guardar selección
        final ItemInventario[] seleccion = {null};

        // Botones
        Button btnSeleccionar = new Button("✓ Seleccionar");
        btnSeleccionar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        btnSeleccionar.setDefaultButton(true);

        Button btnCancelar = new Button("✗ Cancelar");
        btnCancelar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 8 20;");
        btnCancelar.setCancelButton(true);

        btnSeleccionar.setOnAction(e -> {
            TreeItem<ItemInventario> item = tabla.getSelectionModel().getSelectedItem();
            if (item != null && item.getValue() != null) {
                ItemInventario selected = item.getValue();

                // Validar stock
                if (selected.getStockOnHand() <= 0) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Sin stock");
                    alert.setHeaderText(null);
                    alert.setContentText("El producto seleccionado no tiene stock disponible.");
                    alert.showAndWait();
                    return;
                }

                seleccion[0] = selected;
                stage.close();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Selección requerida");
                alert.setHeaderText(null);
                alert.setContentText("Por favor selecciona un producto de la lista.");
                alert.showAndWait();
            }
        });

        btnCancelar.setOnAction(e -> stage.close());

        // Doble clic para seleccionar
        tabla.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                btnSeleccionar.fire();
            }
        });

        // Layout
        HBox botonesBox = new HBox(10, btnSeleccionar, btnCancelar);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(10, 0, 0, 0));

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
                new Label("Selecciona un producto o variante para agregar a la venta:"),
                txtBuscar,
                tabla,
                botonesBox
        );

        Scene scene = new Scene(layout, 720, 550);

        // Aplicar CSS
        try {
            var cssUrl = InventarioService.class.getResource("/Estilos/Estilos.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            // CSS opcional
        }

        stage.setScene(scene);
        stage.showAndWait();

        return Optional.ofNullable(seleccion[0]);
    }
}