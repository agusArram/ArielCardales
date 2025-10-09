
package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.CategoriaDAO;
import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.Entidades.Categoria;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import javafx.concurrent.Task;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.text.NumberFormat;

public class ProductoController {
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TextField txtBuscarEtiqueta;
    @FXML private ToggleButton btnNombre;
    @FXML private ToggleButton btnCategoria;
    @FXML private ToggleButton btnEtiqueta;
    @FXML private ToggleGroup grupoBusqueda;
    @FXML private Button btnNuevaVenta;

    private ObservableList<Producto> listaOriginal;
    private ObservableList<Producto> listaProductos;
    private final ProductoDAO productoDAO = new ProductoDAO();

    // Cache categorías (nombre -> id) para ComboBox
    private Map<String, Long> categoriasNombreId;
    private ObservableList<String> categoriasNombres;

    @FXML
    public void initialize() {
        listaProductos = FXCollections.observableArrayList();
        tablaProductos.setItems(listaProductos);

        // Spinner central mientras se prepara la vista
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(45, 45);
        tablaProductos.setPlaceholder(spinner);

        // Ejecuta el setup completo después de que JavaFX pinte la escena
        Platform.runLater(() -> inicializarEstructura());

        Platform.runLater(() -> {
            if (tablaProductos != null) {
                System.out.println("✅ Tabla Productos cargada correctamente.");
            } else {
                System.err.println("⚠️ Tabla Productos es null.");
            }
        });

    }

    private void inicializarEstructura() {
        // Crea el ToggleGroup sin columnas todavía
        grupoBusqueda = new ToggleGroup();
        btnNombre.setToggleGroup(grupoBusqueda);
        btnCategoria.setToggleGroup(grupoBusqueda);
        btnEtiqueta.setToggleGroup(grupoBusqueda);
        grupoBusqueda.selectToggle(btnNombre);

        // ⚡ Cargar configuración completa en el FX Thread (sin hilos externos innecesarios)
        Platform.runLater(this::configurarVistaCompleta);
    }

    private void configurarVistaCompleta() {
        long inicio = System.currentTimeMillis();

        String[][] columnas = {
                {"Etiqueta", "etiqueta", "0.08", "60"},
                {"Nombre", "nombre", "0.20", "140"},
                {"Descripción", "descripcion", "0.35", "280"},
                {"Categoría", "categoria", "0.12", "100"},
                {"Precio", "precio", "0.15", "100"},
                {"Stock", "stockOnHand", "0.10", "70"}
        };

        tablaProductos.getColumns().clear(); // limpia las columnas básicas
        List<TableColumn<Producto, ?>> cols = Tablas.crearColumnas(columnas);
        cols.forEach(col -> {
            Object ud = col.getUserData();
            if (ud instanceof Double peso) {
                col.prefWidthProperty().bind(tablaProductos.widthProperty().multiply(peso));
            }
            tablaProductos.getColumns().add(col);
        });

        FilteredList<Producto> filtrados = new FilteredList<>(listaProductos, p -> true);
        Runnable aplicarFiltro = () -> {
            String filtro = txtBuscarEtiqueta.getText() == null ? "" : txtBuscarEtiqueta.getText().trim().toLowerCase();
            filtrados.setPredicate(prod -> {
                if (filtro.isBlank()) return true;
                boolean pareceEtiqueta = filtro.matches("p\\d+");
                if (pareceEtiqueta)
                    return prod.getEtiqueta() != null && prod.getEtiqueta().toLowerCase().contains(filtro);
                String tipo = ((ToggleButton) grupoBusqueda.getSelectedToggle()).getText().toLowerCase();
                //busqueda con switch, si quiero agregar algo meto un case mas y sale rapido
                return switch (tipo) {
                    case "nombre" -> prod.getNombre() != null && prod.getNombre().toLowerCase().contains(filtro);
                    case "categoría", "categoria" -> prod.getCategoria() != null && prod.getCategoria().toLowerCase().contains(filtro);
                    case "etiqueta" -> prod.getEtiqueta() != null && prod.getEtiqueta().toLowerCase().contains(filtro);
                    default -> true;
                };
            });
        };

        txtBuscarEtiqueta.textProperty().addListener((o, a, b) -> aplicarFiltro.run());
        grupoBusqueda.selectedToggleProperty().addListener((o, a, b) -> aplicarFiltro.run());

        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(ordenados);

        new EditarProductoController().configurar(tablaProductos);
        btnNuevaVenta.setOnAction(e -> iniciarVenta());

        cargarProductosAsync();

        long fin = System.currentTimeMillis();
        System.out.println("Vista configurada en " + (fin - inicio) + " ms");
    }


    private void cargarProductosAsync() {
        tablaProductos.setPlaceholder(new Label("🔄 Cargando productos..."));

        Task<List<Producto>> task = new Task<>() {
            @Override protected List<Producto> call() { return productoDAO.findAll(); }
        };

        task.setOnSucceeded(e -> {
            listaProductos.setAll(task.getValue());
            // ✅ Guardar copia original
            listaOriginal = FXCollections.observableArrayList(task.getValue());

            if (listaProductos.isEmpty())
                tablaProductos.setPlaceholder(new Label("⚠ No hay productos cargados"));
        });


        task.setOnFailed(e -> {
            tablaProductos.setPlaceholder(new Label("❌ Error al cargar productos"));
            task.getException().printStackTrace();
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
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

    @FXML
    private void eliminarProducto() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            error("Selecciona un producto para eliminar.");
            return;
        }

        // Botones "oficiales" de Alert
        ButtonType btnEliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.WARNING,
                "¿Estás seguro de eliminar el producto?\n\n" + seleccionado.getNombre() + " ?",
                btnEliminar, btnCancelar);

        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText(null); // sacamos el encabezado feo
        alert.getDialogPane().setMinWidth(400);

        // --- Estilo personalizado SOLO al botón Eliminar ---
        Button eliminarButton = (Button) alert.getDialogPane().lookupButton(btnEliminar);
        eliminarButton.setStyle(
                "-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 120px;"
        );

        // Hover
        eliminarButton.setOnMouseEntered(e -> eliminarButton.setStyle(
                "-fx-background-color: darkred; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 120px;"
        ));
        eliminarButton.setOnMouseExited(e -> eliminarButton.setStyle(
                "-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 120px;"
        ));

        // --- Mostrar y actuar ---
        alert.showAndWait().ifPresent(res -> {
            if (res == btnEliminar) {
                try {
                    productoDAO.deleteById(seleccionado.getId());
                    listaProductos.remove(seleccionado);
                    ok("Producto eliminado");
                } catch (Exception e) {
                    error("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

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
                cat.setParentId(null); // si querés jerarquía en el futuro

                Long id = categoriaDAO.insert(cat);

                // actualizar cache
                categoriasNombreId.put(nombre.trim(), id);
                categoriasNombres.add(nombre.trim());
                FXCollections.sort(categoriasNombres);

                ok("Categoría agregada: " + nombre);
            } catch (Exception e) {
                error("No se pudo agregar: " + e.getMessage());
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

            // refrescar la tabla después de cerrar
            cargarProductosAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pedirCantidad(Producto producto) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva venta");
        dialog.setHeaderText("Producto: " + producto.getNombre());
        dialog.setContentText("Cantidad vendida:");

        Optional<String> resultado = dialog.showAndWait();

        resultado.ifPresent(valor -> {
            try {
                int cantidad = Integer.parseInt(valor);
                if (cantidad <= 0) throw new NumberFormatException();

                BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
                NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                String totalFormateado = formato.format(total);


                // Mostrar confirmación antes de procesar
                Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
                confirmar.setTitle("Confirmar venta");
                confirmar.setHeaderText("Total: " + totalFormateado);
                confirmar.setContentText(
                        "Producto: " + producto.getNombre() + "\n" +
                                "Cantidad: " + cantidad + "\n" +
                                "Precio unitario: " + formato.format(producto.getPrecio()) + "\n\n" +
                                "¿Desea confirmar la venta?"
                );


                // Botones personalizados
                ButtonType btnConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmar.getButtonTypes().setAll(btnConfirmar, btnCancelar);

                Optional<ButtonType> decision = confirmar.showAndWait();

                if (decision.isPresent() && decision.get() == btnConfirmar) {
                    procesarVenta(producto, cantidad, total);
                }

            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Cantidad inválida.").showAndWait();
            }
        });
    }


    private void iniciarVenta() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setHeaderText(null);
            alerta.setContentText("Seleccioná un producto primero.");
            alerta.showAndWait();
            return;
        }

        long idProducto = seleccionado.getId();
        System.out.println("Producto seleccionado ID: " + idProducto);

        // Ahora usamos el DAO para traer los datos completos
        Optional<Producto> prodOpt = productoDAO.findById(idProducto);

        if (prodOpt.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "No se encontró el producto en base de datos.").showAndWait();
            return;
        }
        Producto producto = prodOpt.get();
        pedirCantidad(producto);
    }

    private void procesarVenta(Producto producto, int cantidad, BigDecimal total) {
        boolean actualizado = productoDAO.descontarStock(producto.getId(), cantidad);

        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        String totalFormateado = formato.format(total);

        if (actualizado) {
            // Refrescar tabla para ver el nuevo stock
            tablaProductos.setItems(FXCollections.observableArrayList(productoDAO.findAll()));

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Venta confirmada");
            alerta.setHeaderText("Venta confirmada " + totalFormateado);
            alerta.setContentText(
                            "Producto: " + producto.getNombre() + "\n" +
                            "Cantidad: " + cantidad + "\n" +
                            "Etiqueta: " +  producto.getEtiqueta()
            );
            alerta.showAndWait();

        } else {
            new Alert(Alert.AlertType.WARNING,
                    "No hay suficiente stock para completar la venta.")
                    .showAndWait();
        }
    }

    @FXML
    private void mostrarBajoStock() {
        try {
            var productos = ProductoDAO.getProductosBajoStock();
            if (productos.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Stock bajo");
                alert.setHeaderText(null);
                alert.setContentText("No hay productos con stock menor o igual a 2.");
                alert.showAndWait();
                return;
            }

            // ✅ Reemplaza el contenido sin romper el binding
            listaProductos.setAll(productos);

            // ⚡ Refresca la vista
            tablaProductos.refresh();
            System.out.println("Mostrando productos con bajo stock (0 a 2).");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al cargar bajo stock: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    void restaurarInventarioCompleto() {
        if (listaOriginal == null || listaOriginal.isEmpty()) {
            cargarProductosAsync();
            return;
        }

        listaProductos.setAll(listaOriginal); // 🔥 mismo concepto
        tablaProductos.refresh();
        System.out.println("Inventario completo restaurado.");
    }



    // Exportar
    @FXML
    private void exportarPDF() {
        ExportarController.exportarPDF(tablaProductos.getItems());
    }

    @FXML
    private void exportarExcel() {
        ExportarController.exportarExcel(tablaProductos.getItems());
    }

}


