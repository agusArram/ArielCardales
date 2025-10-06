package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.CategoriaDAO;
import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.Entidades.Categoria;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javax.swing.filechooser.FileSystemView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.controlsfx.control.Notifications;
import javafx.concurrent.Task;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.text.NumberFormat;
import java.util.Locale;


public class AppController {
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TextField txtBuscarEtiqueta;
    @FXML private ToggleButton btnNombre;
    @FXML private ToggleButton btnCategoria;
    @FXML private ToggleButton btnEtiqueta;
    @FXML private ToggleGroup grupoBusqueda;
    @FXML private Button btnNuevaVenta;

    private ObservableList<Producto> listaProductos;
    private final ProductoDAO productoDAO = new ProductoDAO();

    // Cache categorías (nombre -> id) para ComboBox
    private Map<String, Long> categoriasNombreId;
    private ObservableList<String> categoriasNombres;

    // --- Converters para edición ---
    private final StringConverter<BigDecimal> moneyConv = new StringConverter<>() {
        final DecimalFormat df;
        {
            var sym = new DecimalFormatSymbols(new Locale("es","AR"));
            sym.setDecimalSeparator(',');
            sym.setGroupingSeparator('.');
            df = new DecimalFormat("#,##0.00", sym);
        }
        @Override public String toString(BigDecimal val) {
            if (val == null) return "";
            return "$ " + df.format(val);
        }
        @Override public BigDecimal fromString(String s) {
            if (s == null || s.isBlank()) return BigDecimal.ZERO;
            // Acepta: "$ 12.345,67", "12345,67", "12.345" etc.
            String limpio = s.replace("$","").replace(" ", "")
                    .replace(".", "").replace(",", ".");
            return new BigDecimal(limpio);
        }
    };
    private final StringConverter<Integer> intConv = new StringConverter<>() {
        @Override public String toString(Integer v) { return v == null ? "" : String.valueOf(v); }
        @Override public Integer fromString(String s) {
            if (s == null || s.isBlank()) return 0;
            int n = Integer.parseInt(s.trim());
            if (n < 0) throw new NumberFormatException("El stock no puede ser negativo");
            return n;
        }
    };

    @FXML
    public void initialize() {
        // 1) Columnas dinámicas
        String[][] columnas = {
                {"Etiqueta", "etiqueta", "0.08", "60"},
                {"Nombre", "nombre", "0.20", "140"},
                {"Descripción", "descripcion", "0.35", "280"},
                {"Categoría", "categoria", "0.12", "100"},
                {"Precio", "precio", "0.15", "100"},
                {"Stock", "stockOnHand", "0.10", "70"}
        };
        List<TableColumn<Producto, ?>> cols = Tablas.crearColumnas(columnas);
        // IMPORTANTE: que Tablas.crearColumnas establezca col.setId(propiedad)
        // (etiqueta/nombre/descripcion/categoria/precio/stockOnHand) para poder buscarlas por id:
        cols.forEach(col -> {
            Object ud = col.getUserData();
            if (ud instanceof Double peso) {
                col.prefWidthProperty().bind(tablaProductos.widthProperty().multiply(peso));
            }
            tablaProductos.getColumns().add(col);
        });

        // 2) Datos (arranca vacío, con spinner)
        listaProductos = FXCollections.observableArrayList();

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(40, 40); // tamaño
        tablaProductos.setPlaceholder(spinner);

        FilteredList<Producto> filtrados = new FilteredList<>(listaProductos, p -> true);


    // 3) Búsqueda con heurística (p### = etiqueta)
        Runnable aplicarFiltro = () -> {
            String filtro = txtBuscarEtiqueta.getText() == null ? "" : txtBuscarEtiqueta.getText().trim().toLowerCase();
            filtrados.setPredicate(prod -> {
                if (filtro.isBlank()) return true;

                boolean pareceEtiqueta = filtro.matches("p\\d+");
                if (pareceEtiqueta) {
                    return prod.getEtiqueta() != null && prod.getEtiqueta().toLowerCase().contains(filtro);
                }
                // Detectar el tipo de búsqueda actual
                String tipo = ((ToggleButton) grupoBusqueda.getSelectedToggle()).getText().toLowerCase(); //te da "Nombre", "Categoría" o "Etiqueta".
                switch (tipo) {
                    case "nombre" -> {
                        return prod.getNombre() != null && prod.getNombre().toLowerCase().contains(filtro);
                    }
                    case "categoría", "categoria" -> {
                        return prod.getCategoria() != null && prod.getCategoria().toLowerCase().contains(filtro);
                    }
                    case "etiqueta" -> {
                        return prod.getEtiqueta() != null && prod.getEtiqueta().toLowerCase().contains(filtro);
                    } //mejor este switch con lambda x si quiero agregar algo es solo sumar un case y listo
                    default -> {
                        return true;
                    }
                }
            });
        };


        grupoBusqueda = new ToggleGroup();
        btnNombre.setToggleGroup(grupoBusqueda);
        btnCategoria.setToggleGroup(grupoBusqueda);
        btnEtiqueta.setToggleGroup(grupoBusqueda);


        txtBuscarEtiqueta.textProperty().addListener((o, a, b) -> aplicarFiltro.run());
        grupoBusqueda.selectToggle(btnNombre);
        grupoBusqueda.selectedToggleProperty().addListener((o, a, b) -> aplicarFiltro.run());

// 4) Orden + setItems
        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(ordenados);

// veo cambios en la lista filtrada
        filtrados.addListener((ListChangeListener<Producto>) c -> {
            if (listaProductos.isEmpty()) {
                tablaProductos.setPlaceholder(new Label("⚠ No hay productos cargados"));
            } else if (filtrados.isEmpty()) {
                tablaProductos.setPlaceholder(new Label("No se encontraron resultados"));
            }
        });

        // 5) Edición inline
        configurarEdicionInline();

        //venta
        btnNuevaVenta.setOnAction(e -> iniciarVenta());

        // 6) RowFactory unificado: doble-click inicia edición + marca low stock (no esta andando stock)
        tablaProductos.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>() {
                @Override protected void updateItem(Producto item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("low-stock");
                    if (!empty && item != null && item.getStockOnHand() <= 3) {
                        if (!getStyleClass().contains("low-stock"))
                            getStyleClass().add("low-stock");
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    // Inicia edición en la columna actualmente seleccionada
                    if (!tablaProductos.getSelectionModel().getSelectedCells().isEmpty()) {
                        TablePosition<Producto, ?> pos = tablaProductos.getSelectionModel().getSelectedCells().get(0);
                        @SuppressWarnings("unchecked")
                        TableColumn<Producto, ?> col = (TableColumn<Producto, ?>) pos.getTableColumn();
                        tablaProductos.edit(pos.getRow(), col);
                    }
/* codigo que notifica que entro en modo edicion, resulta molesto
                    Notifications.create()
                            .text("Modo edición: escribí y presioná Enter para guardar (Esc: cancelar).")
                            .hideAfter(javafx.util.Duration.seconds(5))
                            .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                            .showInformation();
 */
                }
            });
            //  Cargar productos sin bloquear la UI
            return row;
        });
        cargarProductosAsync();
    }

    private void cargarProductosAsync() {
        Task<List<Producto>> task = new Task<>() {
            @Override
            protected List<Producto> call() {
                return productoDAO.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            listaProductos.setAll(task.getValue());

            if (listaProductos.isEmpty()) {
                tablaProductos.setPlaceholder(new Label("⚠ No hay productos cargados"));
            } else {
                tablaProductos.setPlaceholder(new Label("")); // vacío, se maneja con listener
            }
        });

        task.setOnFailed(e -> {
            tablaProductos.setPlaceholder(new Label("❌ Error al cargar productos"));
            task.getException().printStackTrace();
        });
        new Thread(task).start();
    }

    private void configurarEdicionInline() {
        tablaProductos.setEditable(true); //hacemos editables las columnas

        // --- obtener referencias a las columnas por id (asignadas en Tablas.crearColumnas) ---
        @SuppressWarnings("unchecked")
        TableColumn<Producto, String> colNombre = //muestras tring, busca tipo de dato nombre
                (TableColumn<Producto, String>) tablaProductos.getColumns().stream()
                        .filter(c -> "nombre".equals(c.getId())).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        TableColumn<Producto, String> colDesc =
                (TableColumn<Producto, String>) tablaProductos.getColumns().stream()
                        .filter(c -> "descripcion".equals(c.getId())).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        TableColumn<Producto, String> colCat =
                (TableColumn<Producto, String>) tablaProductos.getColumns().stream()
                        .filter(c -> "categoria".equals(c.getId()))
                        .findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        TableColumn<Producto, BigDecimal> colPrecio =
                (TableColumn<Producto, BigDecimal>) tablaProductos.getColumns().stream()
                        .filter(c -> "precio".equals(c.getId())).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        TableColumn<Producto, Integer> colStock =
                (TableColumn<Producto, Integer>) tablaProductos.getColumns().stream()
                        .filter(c -> "stockOnHand".equals(c.getId())).findFirst().orElseThrow();

        // --- Nombre / Descripción: TextFieldTableCell ---
        colNombre.setEditable(true);
        colNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colNombre.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            String nuevo = ev.getNewValue();
            if (nuevo != null && !nuevo.equals(ev.getOldValue())) {
                p.setNombre(nuevo);
                guardarBasico(p);
            } else {
                tablaProductos.refresh();
            }
        });

        colDesc.setEditable(true);
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            String nuevo = ev.getNewValue();
            if (nuevo != null && !nuevo.equals(ev.getOldValue())) {
                p.setDescripcion(nuevo);
                guardarBasico(p);
            } else {
                tablaProductos.refresh();
            }
        });

        // --- Categoría: ComboBoxTableCell con nombres ---
        categoriasNombreId = new CategoriaDAO().mapNombreId();  // << añade esto en CategoriaDAO (abajo)
        categoriasNombres = FXCollections.observableArrayList(categoriasNombreId.keySet());
        FXCollections.sort(categoriasNombres);

        colCat.setEditable(true);
        colCat.setCellFactory(ComboBoxTableCell.forTableColumn(
                new javafx.util.StringConverter<String>() {
                    @Override public String toString(String s) { return s; }
                    @Override public String fromString(String s) { return s; }
                },
                categoriasNombres
        ));

        colCat.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            String nombreCat = ev.getNewValue();
            if (nombreCat != null && !nombreCat.equals(ev.getOldValue())) {
                Long idCat = categoriasNombreId.get(nombreCat);
                p.setCategoria(nombreCat);     // mostrar
                p.setCategoriaId(idCat);       // persistir
                guardarBasico(p);
            } else {
                tablaProductos.refresh();
            }
        });

        // --- Precio: TextFieldTableCell con formateo $ ---
        colPrecio.setEditable(true);
        colPrecio.setCellFactory(TextFieldTableCell.forTableColumn(moneyConv));
        colPrecio.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            try {
                BigDecimal nuevo = ev.getNewValue();
                if (nuevo != null && (ev.getOldValue() == null || nuevo.compareTo(ev.getOldValue()) != 0)) {
                    p.setPrecio(nuevo);
                    guardarBasico(p);
                } else {
                    tablaProductos.refresh();
                }
            } catch (Exception ex) {
                error("Precio inválido");
                tablaProductos.refresh();
            }
        });

        // --- Stock: TextFieldTableCell Integer ---
        colStock.setEditable(true);
        colStock.setCellFactory(TextFieldTableCell.forTableColumn(intConv));
        colStock.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            try {
                Integer nuevo = ev.getNewValue();
                if (nuevo != null && !nuevo.equals(ev.getOldValue())) {
                    p.setStockOnHand(nuevo);
                    guardarBasico(p);
                } else {
                    tablaProductos.refresh();
                }
            } catch (Exception ex) {
                error("Stock inválido");
                tablaProductos.refresh();
            }
        });

        // Enter = commit, Esc = cancel (extra, por si acaso)
        tablaProductos.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE && tablaProductos.getEditingCell() != null) {
                tablaProductos.edit(-1, null);
            }
        });
    }

    // Guarda en DB y muestra toast
    private void guardarBasico(Producto p) {
        try {
            // update()  requiere categoriaId/unidadId: unidadId no la edito, dejamos el valor actual del producto.
            productoDAO.update(p);
            ok("Producto actualizado");
        } catch (Exception e) {
            error("No se pudo guardar: " + e.getMessage());
        }
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

    // Exportar
    private String getRutaEscritorio(String nombreArchivo) {
        // Detecta la carpeta de Escritorio real (incluido OneDrive)
        java.io.File escritorio = FileSystemView.getFileSystemView().getHomeDirectory();
        return new java.io.File(escritorio, nombreArchivo).getAbsolutePath();
    }

    @FXML
    private void exportarPDF() {
        String ruta = getRutaEscritorio("productos.pdf");
        ExportadorPDF.exportar(tablaProductos.getItems(), ruta);
        ok("PDF exportado en: " + ruta);
    }

    @FXML
    private void exportarExcel() {
        String ruta = getRutaEscritorio("productos.xlsx");
        ExportadorExcel.exportar(tablaProductos.getItems(), ruta);
        ok("Excel exportado en: " + ruta);
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
        ProductoDAO dao = new ProductoDAO();
        Optional<Producto> prodOpt = dao.findById(idProducto);

        if (prodOpt.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "No se encontró el producto en base de datos.").showAndWait();
            return;
        }
        Producto producto = prodOpt.get();
        pedirCantidad(producto);
    }

    private void procesarVenta(Producto producto, int cantidad, BigDecimal total) {
        ProductoDAO dao = new ProductoDAO();
        boolean actualizado = dao.descontarStock(producto.getId(), cantidad);

        // 🔹 Formatear total acá también
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        String totalFormateado = formato.format(total);

        if (actualizado) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Venta confirmada.\n" +
                            "Producto: " + producto.getNombre() + "\n" +
                            "Cantidad: " + cantidad + "\n" +
                            "Total: " + totalFormateado)
                    .showAndWait();

            // Refrescar tabla para ver el nuevo stock
            tablaProductos.setItems(FXCollections.observableArrayList(dao.findAll()));

        } else {
            new Alert(Alert.AlertType.WARNING,
                    "No hay suficiente stock para completar la venta.")
                    .showAndWait();
        }
    }

}


