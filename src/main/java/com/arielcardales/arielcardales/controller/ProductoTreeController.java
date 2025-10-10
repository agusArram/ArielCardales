package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.InventarioDAO;
import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.DAO.CategoriaDAO;
import com.arielcardales.arielcardales.DAO.ProductoVarianteDAO;
import com.arielcardales.arielcardales.Entidades.*;
import com.arielcardales.arielcardales.Util.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.TreeItem;
import javafx.util.converter.DefaultStringConverter;
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
    private javafx.animation.PauseTransition pausaBusqueda = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
    private TreeItem<ItemInventario> rootCompleto;

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
                {"Costo",    "costo"},
                {"Precio",   "precio"},
                {"Stock",    "stockOnHand"}
        };

        tablaInventarioTree.getColumns().clear();


        for (String[] c : columnas) {
            TreeTableColumn<ItemInventario, Object> col = new TreeTableColumn<>(c[0]);
            col.setCellValueFactory(new TreeItemPropertyValueFactory<>(c[1]));

            // 🎨 Estilo especial para Color y Talle
            if (c[0].equalsIgnoreCase("Color") || c[0].equalsIgnoreCase("Talle")) {
                col.setCellFactory(tc -> new TreeTableCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                            setStyle("");
                            return;
                        }

                        TreeItem<ItemInventario> treeItem = getTreeTableRow().getTreeItem();
                        if (treeItem == null || treeItem.getValue() == null) {
                            setText(item == null ? "" : item.toString());
                            setStyle("");
                            return;
                        }

                        ItemInventario data = treeItem.getValue();
                        boolean esVariante = data.isEsVariante(); // ✅ usamos tu lógica actual de variantes

                        if (!esVariante) {
                            // 🔸 Producto padre → guion claro y cursiva
                            setText("—");
                            setStyle("-fx-text-fill: #9b8b74; -fx-font-style: italic;");
                        } else {
                            // 🔹 Variante → valor normal
                            setText(item == null ? "" : item.toString());
                            setStyle("-fx-text-fill: #2b2b2b; -fx-font-style: normal;");
                        }
                    }
                });
            }

            tablaInventarioTree.getColumns().add(col);
        }
        ajustarAnchoColumnas(tablaInventarioTree);

        recargarArbol("");

        tablaInventarioTree.setShowRoot(false);
        tablaInventarioTree.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        tablaInventarioTree.setStyle("-fx-background-color: transparent;");

        rootCompleto = tablaInventarioTree.getRoot(); // guarda una copia inicial

// ==============================
// 🔍 NUEVA LÓGICA DE BÚSQUEDA
// ==============================
        grupoBusqueda = new ToggleGroup();
        btnNombre.setToggleGroup(grupoBusqueda);
        btnCategoria.setToggleGroup(grupoBusqueda);
        btnEtiqueta.setToggleGroup(grupoBusqueda);
        grupoBusqueda.selectToggle(btnNombre); // por defecto busca por nombre

        Runnable aplicarFiltro = () -> {
            String filtro = txtBuscarEtiqueta.getText() == null ? "" : txtBuscarEtiqueta.getText().trim().toLowerCase();
            if (filtro.isBlank()) {
                recargarArbol("");
                return;
            }

            Toggle selected = grupoBusqueda.getSelectedToggle();
            if (selected == null) {
                // Si no hay botón seleccionado, forzamos "nombre" por defecto
                grupoBusqueda.selectToggle(btnNombre);
                selected = btnNombre;
            }

            String tipo = ((ToggleButton) selected).getText().toLowerCase();

            switch (tipo) {
                case "nombre" -> recargarArbolPorCampo("nombre", filtro);
                case "categoría", "categoria" -> recargarArbolPorCampo("categoria", filtro);
                case "etiqueta" -> recargarArbolPorCampo("etiqueta", filtro);
                default -> recargarArbol("");
            }

        };


    // 🔁 Escuchar cambios en campo y tipo de búsqueda
    // 🕐 Debounce: evita ejecutar el filtro en cada tecla
        txtBuscarEtiqueta.textProperty().addListener((o, a, b) -> {
            pausaBusqueda.setOnFinished(e -> aplicarFiltro.run());
            pausaBusqueda.playFromStart();
        });

    // Si cambia el tipo de búsqueda (nombre, categoría, etiqueta), aplicar de inmediato
        grupoBusqueda.selectedToggleProperty().addListener((o, a, b) -> aplicarFiltro.run());


        editGeneral();
        aplicarRendererColorTalle();


        tablaInventarioTree.getStylesheets().add(
                getClass().getResource("/Estilos/estilos.css").toExternalForm()
        );

        // --- Doble clic para editar (evita flechita) ---
        tablaInventarioTree.setRowFactory(tv -> {
            TreeTableRow<ItemInventario> row = new TreeTableRow<>();

            // 🌳 Pseudo-clase para diferenciar hijos (nivel > 0)
            row.treeItemProperty().addListener((obs, oldItem, newItem) -> {
                boolean esHijo = newItem != null && newItem.getParent() != null && newItem.getParent().getParent() != null;
                row.pseudoClassStateChanged(PseudoClass.getPseudoClass("hijo"), esHijo);
            });

            // 🖱️ Doble clic para editar (evita la flechita de expansión)
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Node nodoClic = event.getPickResult().getIntersectedNode();
                    while (nodoClic != null && nodoClic != row && !(nodoClic instanceof TreeTableRow)) {
                        if (nodoClic.getStyleClass().contains("tree-disclosure-node")) {
                            return; // si tocó la flecha, no editar
                        }
                        nodoClic = nodoClic.getParent();
                    }
                    int colIndex = tablaInventarioTree.getSelectionModel().getSelectedCells().get(0).getColumn();
                    tablaInventarioTree.edit(row.getIndex(), tablaInventarioTree.getColumns().get(colIndex));
                }
            });

            return row;
        });

    }


    private void editGeneral() {
        tablaInventarioTree.setEditable(true);

        for (TreeTableColumn<ItemInventario, ?> col : tablaInventarioTree.getColumns()) {
            String prop = col.getText().toLowerCase();

            switch (prop) {
                case "nombre" -> configurarEdicionTexto((TreeTableColumn<ItemInventario, String>) col, prop);
                case "precio", "costo" -> configurarEdicionDecimal((TreeTableColumn<ItemInventario, BigDecimal>) col, prop);
                case "stock" -> configurarEdicionEntero((TreeTableColumn<ItemInventario, Integer>) col, prop);
                case "color", "talle" -> configurarEdicionTexto((TreeTableColumn<ItemInventario, String>) col, prop);
                case "categoría", "categoria" -> configurarEdicionCategoria((TreeTableColumn<ItemInventario, String>) col);
            }

        }
    }

    private void guardarEdicion(ItemInventario item, String campo, String valor) {
        boolean okDB = false;

        try {
            // 🚫 Evitar campos que no aplican a productos base
            if (!item.isEsVariante() && (campo.equalsIgnoreCase("color") || campo.equalsIgnoreCase("talle"))) {
                error("Este producto no tiene variantes, por lo que no puede editar " + campo + ".");
                tablaInventarioTree.refresh();
                return;
            }

            // 🚫 Evitar categoría en variantes si no querés actualizar el padre
            if (item.isEsVariante() && campo.equalsIgnoreCase("categoria")) {
                error("Las variantes heredan la categoría del producto base.");
                tablaInventarioTree.refresh();
                return;
            }

            // ✅ Guardar normalmente según tipo
            if (item.isEsVariante()) {
                okDB = InventarioDAO.updateVarianteCampo(item.getVarianteId(), campo, valor);
            } else {
                okDB = productoDAO.updateCampo(item.getProductoId(), campo, valor);
            }

            if (okDB)
                ok("✔ Cambios guardados en " + campo);
            else
                error("⚠ No se pudo actualizar el campo " + campo);

        } catch (Exception e) {
            error("❌ Error al guardar " + campo + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            tablaInventarioTree.refresh();
        }
    }

    private void recargarArbol(String filtro) {
        try {
            tablaInventarioTree.setRoot(InventarioDAO.cargarArbol(filtro));
            tablaInventarioTree.setShowRoot(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void recargarArbolPorCampo(String campo, String valor) {
        try {
            // ⚡ Trae TODO sin filtro desde BD
            TreeItem<ItemInventario> root = clonarArbol(rootCompleto);

            if (root != null && root.getChildren() != null) {
                filtrarRecursivo(root, campo, valor.toLowerCase());
            }

            tablaInventarioTree.setRoot(root);
            tablaInventarioTree.setShowRoot(false);

        } catch (Exception e) {
            e.printStackTrace();
            error("❌ Error al aplicar filtro: " + e.getMessage());
        }
    }

    private boolean filtrarRecursivo(TreeItem<ItemInventario> nodo, String campo, String valor) {
        if (nodo == null || nodo.getChildren() == null) return false;

        Iterator<TreeItem<ItemInventario>> it = nodo.getChildren().iterator();
        boolean algunHijoVisible = false;

        while (it.hasNext()) {
            TreeItem<ItemInventario> hijo = it.next();
            ItemInventario data = hijo.getValue();

            boolean coincide = false;
            if (data != null) {
                switch (campo) {
                    case "nombre" -> coincide = data.getNombreProducto() != null && data.getNombreProducto().toLowerCase().contains(valor);
                    case "categoria" -> coincide = data.getCategoria() != null && data.getCategoria().toLowerCase().contains(valor);
                    case "etiqueta" -> coincide = data.getEtiquetaProducto() != null && data.getEtiquetaProducto().toLowerCase().contains(valor);
                }
            }

            boolean hijosCoinciden = filtrarRecursivo(hijo, campo, valor);
            if (!coincide && !hijosCoinciden) {
                it.remove();
            } else {
                algunHijoVisible = true;
            }
        }

        return algunHijoVisible;
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

        // 🧩 Si es una variante (nodo hijo)
        if (item.isEsVariante()) {
            ButtonType eliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "¿Eliminar la variante seleccionada?\n\nColor: " + item.getColor() +
                            "\nTalle: " + item.getTalle(),
                    eliminar, cancelar);
            alert.setTitle("Confirmar eliminación");
            alert.setHeaderText("Eliminar variante");

            alert.showAndWait().ifPresent(res -> {
                if (res == eliminar) {
                    try {
                        new ProductoVarianteDAO().deleteById(item.getVarianteId());
                        ok("Variante eliminada correctamente");
                        recargarArbol(txtBuscarEtiqueta.getText());
                    } catch (Exception e) {
                        error("Error al eliminar variante: " + e.getMessage());
                    }
                }
            });
            return; // ⚠️ Importante: evitar que siga al bloque de producto base
        }

        // 🧩 Si es un producto normal (base)
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
    private void agregarVariante() {
        Optional<ItemInventario> sel = getSeleccionInventario();
        if (sel.isEmpty()) {
            error("Seleccioná un producto base para agregar una variante.");
            return;
        }

        ItemInventario item = sel.get();

        if (item.isEsVariante()) {
            error("No podés agregar una variante a otra variante.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/agregarVariante.fxml"));
            Parent root = loader.load();

            AgregarVarianteController controller = loader.getController();
            controller.setProductoBaseId(item.getProductoId());

            Stage stage = new Stage();
            stage.setTitle("Agregar Variante");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            recargarArbol(txtBuscarEtiqueta.getText());

        } catch (Exception e) {
            e.printStackTrace();
            error("Error al abrir ventana de variantes: " + e.getMessage());
        }
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

            // 🔄 Si el controlador marcó que hubo cambios, recargar tabla
            Object result = stage.getUserData();
            if (result instanceof Boolean && (Boolean) result) {
                recargarArbol(txtBuscarEtiqueta.getText());
            }

        } catch (Exception e) {
            e.printStackTrace();
            error("Error al abrir ventana de producto: " + e.getMessage());
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

    private void configurarEdicionTexto(TreeTableColumn<ItemInventario, String> col, String campo) {
        col.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            String nuevo = event.getNewValue();
            String anterior = event.getOldValue();

            boolean okDB = false;
            try {
                // 🚫 Bloqueo si el campo no aplica
                if (!item.isEsVariante() && (campo.equalsIgnoreCase("color") || campo.equalsIgnoreCase("talle"))) {
                    error("Este producto no tiene variantes, por lo que no puede editar " + campo + ".");
                    item.setNombreProducto(item.getNombreProducto()); // fuerza refresh visual
                    tablaInventarioTree.refresh();
                    return;
                }

                // ✅ Intentar guardar
                if (item.isEsVariante()) {
                    okDB = InventarioDAO.updateVarianteCampo(item.getVarianteId(), campo, nuevo);
                } else {
                    okDB = productoDAO.updateCampo(item.getProductoId(), campo, nuevo);
                }

                if (okDB) {
                    ok("✔ Cambios guardados en " + campo);
                    switch (campo.toLowerCase()) {
                        case "nombre" -> item.setNombreProducto(nuevo);
                        case "color" -> item.setColor(nuevo);
                        case "talle" -> item.setTalle(nuevo);
                    }
                } else {
                    // ❌ Revertir cambio visual
                    switch (campo.toLowerCase()) {
                        case "nombre" -> item.setNombreProducto(anterior);
                        case "color" -> item.setColor(anterior);
                        case "talle" -> item.setTalle(anterior);
                    }
                    error("⚠ No se pudo actualizar el campo " + campo);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // ❌ Revertir ante error
                switch (campo.toLowerCase()) {
                    case "nombre" -> item.setNombreProducto(anterior);
                    case "color" -> item.setColor(anterior);
                    case "talle" -> item.setTalle(anterior);
                }
                error("❌ Error al guardar " + campo + ": " + e.getMessage());
            } finally {
                tablaInventarioTree.refresh();
            }
        });
    }

    private void configurarEdicionDecimal(TreeTableColumn<ItemInventario, BigDecimal> col, String campo) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        formato.setMaximumFractionDigits(2);
        formato.setMinimumFractionDigits(2);

        col.setCellFactory(column -> new TextFieldTreeTableCell<>(new javafx.util.StringConverter<>() {
            @Override
            public String toString(BigDecimal value) {
                if (value == null) return "";
                try {
                    return formato.format(value);
                } catch (Exception e) {
                    return value.toPlainString();
                }
            }

            @Override
            public BigDecimal fromString(String string) {
                if (string == null || string.isBlank()) return BigDecimal.ZERO;

                try {
                    // 🔹 Elimina TODO lo que no sea número o separador decimal
                    String limpio = string
                            .replaceAll("[^\\d,.-]", "")   // quita $, espacios, letras, etc.
                            .replace(".", "")              // elimina puntos de miles
                            .replace(",", ".")             // cambia coma por punto decimal
                            .trim();

                    // 🔹 Si el usuario deja solo el signo o algo inválido → 0
                    if (limpio.isBlank() || limpio.equals("-") || limpio.equals("."))
                        return BigDecimal.ZERO;

                    return new BigDecimal(limpio);
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            }
        }));

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            BigDecimal nuevo = event.getNewValue();
            BigDecimal anterior = event.getOldValue();

            try {
                boolean okDB;
                if (item.isEsVariante()) {
                    okDB = InventarioDAO.updateVarianteCampo(item.getVarianteId(), campo, nuevo.toPlainString());
                } else {
                    okDB = productoDAO.updateCampo(item.getProductoId(), campo, nuevo.toPlainString());
                }

                if (okDB) {
                    ok("✔ " + campo + " actualizado correctamente");
                    if (campo.equalsIgnoreCase("precio"))
                        item.setPrecio(nuevo);
                    else
                        item.setCosto(nuevo);
                } else {
                    if (campo.equalsIgnoreCase("precio"))
                        item.setPrecio(anterior);
                    else
                        item.setCosto(anterior);
                    error("⚠ No se pudo actualizar el campo " + campo);
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (campo.equalsIgnoreCase("precio"))
                    item.setPrecio(anterior);
                else
                    item.setCosto(anterior);
                error("❌ Error al actualizar " + campo + ": " + e.getMessage());
            } finally {
                tablaInventarioTree.refresh();
            }
        });
    }

    private void configurarEdicionEntero(TreeTableColumn<ItemInventario, Integer> col, String campo) {
        col.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return (value != null) ? value.toString() : "";
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()) return 0;
                try {
                    // 🔹 Elimina espacios, puntos o comas que el usuario pueda poner
                    String limpio = string.replaceAll("[^\\d-]", "").trim();
                    if (limpio.isBlank() || limpio.equals("-")) return 0;
                    return Integer.parseInt(limpio);
                } catch (Exception e) {
                    return 0;
                }
            }
        }));

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            Integer nuevo = event.getNewValue();
            Integer anterior = event.getOldValue();

            // Validación extra: no permitir negativos
            if (nuevo < 0) {
                error("⚠ El stock no puede ser negativo.");
                item.setStockOnHand(anterior);
                tablaInventarioTree.refresh();
                return;
            }

            try {
                boolean okDB;
                if (item.isEsVariante()) {
                    okDB = InventarioDAO.updateVarianteCampo(item.getVarianteId(), campo, nuevo.toString());
                } else {
                    okDB = productoDAO.updateCampo(item.getProductoId(), campo, nuevo.toString());
                }

                if (okDB) {
                    ok("✔ Stock actualizado correctamente");
                    item.setStockOnHand(nuevo);
                } else {
                    item.setStockOnHand(anterior);
                    error("⚠ No se pudo actualizar el campo " + campo);
                }

            } catch (Exception e) {
                e.printStackTrace();
                item.setStockOnHand(anterior);
                error("❌ Error al actualizar " + campo + ": " + e.getMessage());
            } finally {
                tablaInventarioTree.refresh();
            }
        });
    }

    private void configurarEdicionCategoria(TreeTableColumn<ItemInventario, String> col) {
        try {
            // Obtener lista de categorías desde BD
            CategoriaDAO categoriaDAO = new CategoriaDAO();
            List<Categoria> categorias = categoriaDAO.findAll(); // o tu método de obtener todas
            ObservableList<String> opciones = FXCollections.observableArrayList();
            for (Categoria c : categorias) {
                opciones.add(c.getNombre());
            }

            col.setCellFactory(ComboBoxTreeTableCell.forTreeTableColumn(opciones));

            col.setOnEditCommit(event -> {
                ItemInventario item = event.getRowValue().getValue();
                String nuevaCategoria = event.getNewValue();

                item.setCategoria(nuevaCategoria);
                guardarEdicion(item, "categoria", nuevaCategoria);
                tablaInventarioTree.refresh();
            });

        } catch (Exception e) {
            e.printStackTrace();
            error("No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    private void ajustarAnchoColumnas(TreeTableView<ItemInventario> tabla) {
        // Política de ajuste: la última columna se estira para completar el espacio
        tabla.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Escuchar una sola vez el cambio de ancho total del TreeTableView
        tabla.widthProperty().addListener((obs, oldVal, newVal) -> recalcularAnchoColumnas(tabla, newVal.doubleValue()));

        // También ejecutar inmediatamente cuando la tabla ya tenga tamaño real
        if (tabla.getWidth() > 0) {
            recalcularAnchoColumnas(tabla, tabla.getWidth());
        }
    }

    // 🔧 Método auxiliar real que ajusta las proporciones
    private void recalcularAnchoColumnas(TreeTableView<ItemInventario> tabla, double total) {
        if (tabla.getColumns().isEmpty() || total <= 0) return;

        for (TreeTableColumn<ItemInventario, ?> col : tabla.getColumns()) {
            String nombre = col.getText().toLowerCase();

            double ancho = switch (nombre) {
                case "etiqueta" -> total * 0.08;
                case "nombre" -> total * 0.30;
                case "color" -> total * 0.08;
                case "talle" -> total * 0.07;
                case "categoría", "categoria" -> total * 0.13;
                case "costo" -> total * 0.12;
                case "precio" -> total * 0.13;
                case "stock" -> total * 0.06;
                default -> total * 0.10;
            };

            // 🔒 Limitar ancho mínimo y máximo razonables
            col.setMinWidth(60);
            col.setMaxWidth(Math.max(120, ancho * 1.5));

            col.setPrefWidth(ancho);

            // Alineaciones limpias
            if (List.of("costo", "precio", "stock", "etiqueta", "color", "talle", "categoría", "categoria").contains(nombre))
                col.setStyle("-fx-alignment: CENTER;");
            else
                col.setStyle("-fx-alignment: CENTER-LEFT;");
        }
    }


    @SuppressWarnings("unchecked")
    private void aplicarRendererColorTalle() {
        for (TreeTableColumn<ItemInventario, ?> anyCol : tablaInventarioTree.getColumns()) {
            String header = anyCol.getText().toLowerCase();

            // Columnas que solo aplican a variantes o productos con variantes
            if (!List.of("color", "talle", "costo", "precio", "stock").contains(header))
                continue;

            TreeTableColumn<ItemInventario, Object> col = (TreeTableColumn<ItemInventario, Object>) anyCol;

            col.setCellFactory(tc -> new TreeTableCell<>() {

                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    TreeItem<ItemInventario> ti = getTreeTableRow() == null ? null : getTreeTableRow().getTreeItem();
                    if (ti == null || ti.getValue() == null) {
                        setText(item == null ? "" : item.toString());
                        return;
                    }

                    ItemInventario data = ti.getValue();
                    boolean esVariante = data.isEsVariante();
                    boolean tieneHijos = ti.getChildren() != null && !ti.getChildren().isEmpty();

                    // 🔸 Mostrar “—” SOLO si es un padre con hijos (producto con variantes)
                    if (!esVariante && tieneHijos) {
                        setText("—");
                        setStyle("-fx-text-fill: #9b8b74; -fx-font-style: italic;");
                        return;
                    }

                    // 🔹 Mostrar valor normal si es variante o producto sin hijos
                    if (item == null) {
                        setText("");
                    } else if (item instanceof BigDecimal bd) {
                        setText(String.format("$ %,.2f", bd));
                    } else {
                        setText(item.toString());
                    }

                    setStyle("-fx-text-fill: #2b2b2b; -fx-font-style: normal;");
                }

                @Override
                public void startEdit() {
                    // Bloquea edición solo en padres
                    TreeItem<ItemInventario> ti = getTreeTableRow() == null ? null : getTreeTableRow().getTreeItem();
                    ItemInventario data = (ti == null) ? null : ti.getValue();
                    if (data != null && !data.isEsVariante()) {
                        return;
                    }
                    super.startEdit();
                }
            });
        }
    }

    @FXML
    private void buscarPorNombre() { grupoBusqueda.selectToggle(btnNombre); }
    @FXML
    private void buscarPorCategoria() { grupoBusqueda.selectToggle(btnCategoria); }
    @FXML
    private void buscarPorEtiqueta() { grupoBusqueda.selectToggle(btnEtiqueta); }

    private TreeItem<ItemInventario> clonarArbol(TreeItem<ItemInventario> original) {
        if (original == null) return null;
        TreeItem<ItemInventario> copia = new TreeItem<>(original.getValue());
        for (TreeItem<ItemInventario> hijo : original.getChildren()) {
            copia.getChildren().add(clonarArbol(hijo));
        }
        return copia;
    }

}
