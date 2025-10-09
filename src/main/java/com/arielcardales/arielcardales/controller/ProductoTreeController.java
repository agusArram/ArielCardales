package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.InventarioDAO;
import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.DAO.CategoriaDAO;
import com.arielcardales.arielcardales.Entidades.*;
import com.arielcardales.arielcardales.Util.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;

public class ProductoTreeController {

    @FXML private TreeTableView<ItemInventario> tablaInventarioTree;
    @FXML private TextField txtBuscarEtiqueta;
    @FXML private ToggleButton btnNombre;
    @FXML private ToggleButton btnCategoria;
    @FXML private ToggleButton btnEtiqueta;
    @FXML private ToggleGroup grupoBusqueda;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private ObservableList<Producto> listaProductos;
    private Map<String, Long> categoriasNombreId;
    private ObservableList<String> categoriasNombres;

    @FXML
    public void initialize() {
        Platform.runLater(this::configurarVistaCompleta);
    }

    private void configurarVistaCompleta() {
        String[][] columnas = {
                {"Etiqueta", "etiquetaProducto"},
                {"Nombre",   "nombreProducto"},
                {"Color",    "color"},
                {"Talle",    "talle"},
                {"Categoría","categoria"},
                {"Unidad",   "unidad"},
                {"Precio",   "precio"},
                {"Stock",    "stockOnHand"}
        };

        // --- Columnas ---
        tablaInventarioTree.getColumns().clear();
        for (String[] c : columnas) {
            TreeTableColumn<ItemInventario, Object> col = new TreeTableColumn<>(c[0]);
            col.setCellValueFactory(new TreeItemPropertyValueFactory<>(c[1]));
            col.prefWidthProperty().bind(tablaInventarioTree.widthProperty().multiply(0.12));
            tablaInventarioTree.getColumns().add(col);
        }

        // --- Carga inicial ---
        recargarArbol("");

        // --- Búsqueda dinámica ---
        txtBuscarEtiqueta.textProperty().addListener((obs, oldV, newV) -> recargarArbol(newV));
    }

    private void recargarArbol(String filtro) {
        try {
            tablaInventarioTree.setRoot(InventarioDAO.cargarArbol(filtro));
            tablaInventarioTree.setShowRoot(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------
    // ACCIONES
    // -------------------------------------------------------------------

    @FXML
    private void agregarCategoria() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva categoría");
        dialog.setHeaderText("Agregar nueva categoría");
        dialog.setContentText("Nombre de la categoría:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (nombre == null || nombre.trim().isEmpty()) {
                error("El nombre no puede estar vacío");
                return;
            }

            try {
                CategoriaDAO categoriaDAO = new CategoriaDAO();
                Categoria cat = new Categoria();
                cat.setNombre(nombre.trim());
                cat.setParentId(null);

                Long id = categoriaDAO.insert(cat);
                if (categoriasNombreId != null) {
                    categoriasNombreId.put(nombre.trim(), id);
                    categoriasNombres.add(nombre.trim());
                    FXCollections.sort(categoriasNombres);
                }

                ok("Categoría agregada: " + nombre);
            } catch (Exception e) {
                error("No se pudo agregar: " + e.getMessage());
            }
        });
    }

    @FXML
    private void eliminarProducto() {
        Optional<ItemInventario> sel = getSeleccionInventario();
        if (sel.isEmpty()) {
            error("Seleccioná un producto o variante.");
            return;
        }

        ItemInventario item = sel.get();

        if (item.isEsVariante()) {
            error("Eliminar variante aún no implementado (falta VarianteDAO).");
            return;
        }

        // Confirmación
        ButtonType eliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "¿Eliminar el producto?\n\n" + item.getNombreProducto(),
                eliminar, cancelar);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText(null);

        alert.showAndWait().ifPresent(res -> {
            if (res == eliminar) {
                try {
                    productoDAO.deleteById(item.getProductoId());
                    ok("Producto eliminado");
                    recargarArbol(txtBuscarEtiqueta.getText());
                } catch (Exception e) {
                    error("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void abrirAgregarProducto() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/agregarProducto.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Agregar Producto");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            recargarArbol(txtBuscarEtiqueta.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void iniciarVenta() {
        Optional<ItemInventario> sel = getSeleccionInventario();
        if (sel.isEmpty()) {
            error("Seleccioná un producto primero.");
            return;
        }

        ItemInventario item = sel.get();
        Optional<Producto> opt = productoDAO.findById(item.getProductoId());
        if (opt.isEmpty()) {
            error("No se encontró el producto base.");
            return;
        }

        Producto producto = opt.get();
        pedirCantidad(producto);
    }

    private void pedirCantidad(Producto producto) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva venta");
        dialog.setHeaderText("Producto: " + producto.getNombre());
        dialog.setContentText("Cantidad vendida:");

        dialog.showAndWait().ifPresent(valor -> {
            try {
                int cantidad = Integer.parseInt(valor);
                if (cantidad <= 0) throw new NumberFormatException();

                BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
                NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                String totalFormateado = formato.format(total);

                Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
                confirmar.setTitle("Confirmar venta");
                confirmar.setHeaderText("Total: " + totalFormateado);
                confirmar.setContentText(
                        "Producto: " + producto.getNombre() + "\n" +
                                "Cantidad: " + cantidad + "\n" +
                                "Precio unitario: " + formato.format(producto.getPrecio())
                );

                ButtonType ok = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmar.getButtonTypes().setAll(ok, cancel);

                Optional<ButtonType> res = confirmar.showAndWait();
                if (res.isPresent() && res.get() == ok) {
                    procesarVenta(producto, cantidad, total);
                }

            } catch (NumberFormatException e) {
                error("Cantidad inválida.");
            }
        });
    }

    private void procesarVenta(Producto producto, int cantidad, BigDecimal total) {
        boolean actualizado = productoDAO.descontarStock(producto.getId(), cantidad);
        if (!actualizado) {
            error("No hay suficiente stock.");
            return;
        }

        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        String totalFormateado = formato.format(total);

        ok("Venta confirmada: " + totalFormateado);
        recargarArbol(txtBuscarEtiqueta.getText());
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private Optional<ItemInventario> getSeleccionInventario() {
        if (tablaInventarioTree == null) return Optional.empty();
        TreeItem<ItemInventario> ti = tablaInventarioTree.getSelectionModel().getSelectedItem();
        return ti == null ? Optional.empty() : Optional.ofNullable(ti.getValue());
    }

    private void ok(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(2))
                .showConfirm();
    }

    private void error(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(5))
                .showError();
    }
}
