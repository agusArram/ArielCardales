package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.*;
import com.arielcardales.arielcardales.Entidades.*;
import com.arielcardales.arielcardales.Util.*;
import com.arielcardales.arielcardales.service.InventarioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import javafx.scene.control.TreeItem;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.prefs.Preferences;

public class ProductoTreeController {
    @FXML private TreeTableView<ItemInventario> tablaInventarioTree;
    @FXML private TextField txtBuscarEtiqueta;
    @FXML private ToggleButton btnNombre;
    @FXML private ToggleButton btnCategoria;
    @FXML private ToggleButton btnEtiqueta;
    @FXML private ToggleGroup grupoBusqueda;
    @FXML private VBox panelLateral;

    private javafx.animation.PauseTransition pausaBusqueda = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
    private TreeItem<ItemInventario> rootCompleto;
    private final InventarioService inventarioService = new InventarioService();
    private final Preferences prefs = Preferences.userNodeForPackage(ProductoTreeController.class);
    private static final String PREF_EXPANDIR_NODOS = "expandir_nodos_hijos";
    private Task<TreeItem<ItemInventario>> cargaTask;
    private volatile boolean primeraCarga = true;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private Map<String, Long> categoriasNombreId;
    private ObservableList<String> categoriasNombres;

    @FXML
    public void initialize() {
        configurarUI();          // columnas, listeners, rowFactory, etc. (sin BD)
        cargarArbolAsync("");    // primera carga en background
    }

    /**
     * Configura toda la interfaz de usuario del TreeTableView:
     * - Crea columnas dinámicas.
     * - Aplica estilos y renderizadores.
     * - Configura la búsqueda reactiva.
     * - Vincula edición directa en celdas.
     * - Inicializa el control de expansión automática.
     */
    private void configurarUI() {

        // 🧹 Limpia columnas previas antes de volver a construirlas
        tablaInventarioTree.getColumns().clear();

        // === Definición de columnas visibles y su propiedad asociada ===
        // 1° valor → título visible en la UI
        // 2° valor → nombre del atributo en la clase ItemInventario
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

        // 🔄 Recorre la definición y crea dinámicamente cada columna
        for (String[] c : columnas) {
            TreeTableColumn<ItemInventario, ?> col;

            // 🧩 Determina el tipo genérico de columna según su contenido
            switch (c[0].toLowerCase()) {
                case "stock" -> col = new TreeTableColumn<ItemInventario, Integer>(c[0]);
                case "precio", "costo" -> col = new TreeTableColumn<ItemInventario, BigDecimal>(c[0]);
                default -> col = new TreeTableColumn<ItemInventario, String>(c[0]);
            }

            // 🔗 Vincula la columna con la propiedad correspondiente del modelo
            col.setCellValueFactory(new TreeItemPropertyValueFactory<>(c[1]));

            // 🎨 Personaliza el renderizado de las columnas Color y Talle
            if (c[0].equalsIgnoreCase("Color") || c[0].equalsIgnoreCase("Talle")) {
                ((TreeTableColumn<ItemInventario, String>) col).setCellFactory(tc -> new TreeTableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setText(null); setStyle(""); return; }

                        // Obtiene el TreeItem asociado a la fila actual
                        TreeItem<ItemInventario> treeItem = getTreeTableRow().getTreeItem();
                        if (treeItem == null || treeItem.getValue() == null) {
                            setText(item);
                            return;
                        }

                        ItemInventario data = treeItem.getValue();
                        boolean esVariante = data.isEsVariante();

                        // 🔸 Producto base → muestra un guion y color gris
                        if (!esVariante) {
                            setText("—");
                            setStyle("-fx-text-fill: #9b8b74; -fx-font-style: italic;");
                        }
                        // 🔹 Variante → texto normal
                        else {
                            setText(item == null ? "" : item);
                            setStyle("-fx-text-fill: #2b2b2b; -fx-font-style: normal;");
                        }
                    }
                });
            }

            // Agrega la columna construida al TreeTableView
            tablaInventarioTree.getColumns().add(col);
        }

        // === Propiedades generales del TreeTableView ===
        tablaInventarioTree.setShowRoot(false);                         // Oculta el nodo raíz “invisible”
        tablaInventarioTree.setEditable(true);                          // Permite edición de celdas
        tablaInventarioTree.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY); // Ajusta ancho
        tablaInventarioTree.setStyle("-fx-background-color: transparent;");
        URL cssUrl = getClass().getResource("/Estilos/Estilos.css");
        if (cssUrl != null) {
            tablaInventarioTree.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("⚠️ Advertencia: No se encontró estilos.css, usando estilos por defecto");
        }

        // 🌀 Placeholder inicial mientras carga datos
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);
        tablaInventarioTree.setPlaceholder(pi);

        // === Configuración de búsqueda reactiva ===
        grupoBusqueda = new ToggleGroup();
        btnNombre.setToggleGroup(grupoBusqueda);
        btnCategoria.setToggleGroup(grupoBusqueda);
        btnEtiqueta.setToggleGroup(grupoBusqueda);
        grupoBusqueda.selectToggle(btnNombre); // búsqueda por nombre por defecto

        // 🔍 Lógica principal del filtro dinámico
        Runnable aplicarFiltro = () -> {
            String filtro = Optional.ofNullable(txtBuscarEtiqueta.getText()).orElse("").trim().toLowerCase();

            // Si el campo de búsqueda está vacío → restaurar inventario completo
            if (filtro.isBlank()) {
                if (rootCompleto != null) {
                    TreeItem<ItemInventario> copia = clonarArbol(rootCompleto);
                    tablaInventarioTree.setRoot(copia);

                    // 🔧 Si está activa la preferencia “Expandir auto”, expandir nodos
                    boolean expandir = prefs.getBoolean(PREF_EXPANDIR_NODOS, false);
                    if (expandir) expandirTodo(copia);
                } else {
                    // Si aún no hay cache, recargar desde BD
                    cargarArbolAsync("");
                }
                return;
            }

            // Si aún no terminó de cargar el árbol → abortar filtro
            if (rootCompleto == null) return;

            // Determina el tipo de filtro (Nombre, Categoría o Etiqueta)
            String tipo = ((ToggleButton) Optional.ofNullable(grupoBusqueda.getSelectedToggle())
                    .orElse(btnNombre)).getText().toLowerCase();

            // Crea una copia del árbol original en memoria
            TreeItem<ItemInventario> copia = clonarArbol(rootCompleto);

            // Aplica el filtrado recursivo según el campo seleccionado
            filtrarRecursivo(copia, switch (tipo) {
                case "categoría", "categoria" -> "categoria";
                case "etiqueta" -> "etiqueta";
                default -> "nombre";
            }, filtro);

            // Reemplaza el árbol en pantalla por la versión filtrada
            tablaInventarioTree.setRoot(copia);

            // Si está activa la preferencia, expande los nodos filtrados
            boolean expandir = prefs.getBoolean(PREF_EXPANDIR_NODOS, false);
            if (expandir) expandirTodo(copia);
        };

        // === Búsqueda reactiva con debounce y recuperación segura ===
        txtBuscarEtiqueta.textProperty().addListener((o, oldValue, newValue) -> {
            pausaBusqueda.stop();
            pausaBusqueda.setOnFinished(e -> aplicarFiltro.run());
            pausaBusqueda.playFromStart();

            // 🩹 Fallback instantáneo si el texto queda vacío (borra todo de golpe)
            if (newValue == null || newValue.isBlank()) {
                Platform.runLater(aplicarFiltro); // fuerza ejecución inmediata
            }
        });


        // Escucha cambios en el tipo de búsqueda (Nombre / Categoría / Etiqueta)
        grupoBusqueda.selectedToggleProperty().addListener((o, a, b) -> aplicarFiltro.run());

        // === Configura edición y renderizados ===
        aplicarRendererColorTalle();  // Renderizado especial para color/talle
        editGeneral();                // Asigna celdas editables
        ajustarAnchoColumnas(tablaInventarioTree); // Ajusta proporciones iniciales

        // === Checkbox de expansión automática persistente ===
        CheckBox chkExpandir = new CheckBox("Expandir auto");
        chkExpandir.setSelected(prefs.getBoolean(PREF_EXPANDIR_NODOS, false));
        chkExpandir.setOnAction(e -> {
            // Guarda la preferencia del usuario y recarga el árbol
            prefs.putBoolean(PREF_EXPANDIR_NODOS, chkExpandir.isSelected());
            recargarArbol(txtBuscarEtiqueta.getText());
        });
        chkExpandir.setStyle("-fx-padding: 10 0 0 4; -fx-font-size: 13px;");

        if (panelLateral != null) panelLateral.getChildren().add(chkExpandir);

        // === RowFactory con doble clic para editar celdas ===
        tablaInventarioTree.setRowFactory(tv -> {
            TreeTableRow<ItemInventario> row = new TreeTableRow<>();

            // Pseudo-clase CSS “.hijo” → permite colorear filas hijas
            row.treeItemProperty().addListener((obs, oldItem, newItem) -> {
                boolean esHijo = newItem != null && newItem.getParent() != null && newItem.getParent().getParent() != null;
                row.pseudoClassStateChanged(PseudoClass.getPseudoClass("hijo"), esHijo);
            });

            // Doble clic para activar edición directa
            row.setOnMouseClicked(event -> {
                // 🖱 Doble clic en una fila no vacía
                if (event.getClickCount() == 2 && !row.isEmpty()) {

                    Node nodo = event.getPickResult().getIntersectedNode();

                    // ❌ Si tocó la flechita de expansión → no editar
                    while (nodo != null && nodo != row && !(nodo instanceof TreeTableRow)) {
                        if (nodo.getStyleClass().contains("tree-disclosure-node")) {
                            return;
                        }
                        nodo = nodo.getParent();
                    }

                    // ✅ Evita que el doble clic se propague y cause expansión
                    event.consume();

                    // 🔎 Determina columna seleccionada
                    int colIndex = tablaInventarioTree.getSelectionModel().getSelectedCells().isEmpty()
                            ? 0
                            : tablaInventarioTree.getSelectionModel().getSelectedCells().get(0).getColumn();

                    // 🧭 Asegura foco en la tabla antes de editar
                    tablaInventarioTree.requestFocus();

                    // ✏ Inicia edición sobre la celda clickeada
                    tablaInventarioTree.edit(row.getIndex(), tablaInventarioTree.getColumns().get(colIndex));
                }
            });
            // 🧩 Bloquea la expansión/colapso del padre cuando se hace doble clic fuera de la flechita
            tablaInventarioTree.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getClickCount() == 2) {
                    Node nodo = event.getPickResult().getIntersectedNode();

                    // ✅ Si el clic fue sobre la flechita, permitir comportamiento normal
                    while (nodo != null && !(nodo instanceof TreeTableRow)) {
                        if (nodo.getStyleClass().contains("tree-disclosure-node")) {
                            return; // no bloqueamos expansión si tocó la flecha
                        }
                        nodo = nodo.getParent();
                    }

                    // ❌ Si el doble clic fue sobre cualquier otra parte de la fila, no expandir
                    event.consume();
                }
            });

            return row;
        });
    }


    /**
     * Carga el árbol de inventario en segundo plano (Thread separado del FX UI Thread)
     * para evitar bloqueos en la interfaz.
     *
     * - Ejecuta inventarioService.cargarArbol(filtro).
     * - Muestra un spinner mientras carga.
     * - Actualiza el TreeTableView una vez finalizado.
     */
    private void cargarArbolAsync(String filtro) {

        // Si hay una carga anterior en ejecución, cancelarla para evitar colisiones
        if (cargaTask != null && cargaTask.isRunning()) {
            cargaTask.cancel();
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        // Spinner de carga durante la ejecución del Task
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);
        tablaInventarioTree.setPlaceholder(pi);

        // Task que se ejecuta en background y retorna un TreeItem<ItemInventario>
        cargaTask = new Task<>() {
            @Override
            protected TreeItem<ItemInventario> call() throws Exception {
                return inventarioService.cargarArbol(filtro);
            }
        };

        // ✅ Al finalizar correctamente → mostrar los datos en la tabla
        cargaTask.setOnSucceeded(e -> {
            TreeItem<ItemInventario> root = cargaTask.getValue();
            if (root == null) {
                tablaInventarioTree.setPlaceholder(new Label("Sin datos disponibles"));
                return;
            }

            rootCompleto = root;                     // Cachea la versión completa del árbol
            tablaInventarioTree.setRoot(root);
            tablaInventarioTree.setShowRoot(false);  // Oculta el nodo raíz técnico

            // Si el usuario tiene activa la opción “Expandir auto”, expandir todo el árbol
            boolean expandir = prefs.getBoolean(PREF_EXPANDIR_NODOS, false);
            if (expandir) expandirTodo(root);

            // Mensaje de estado visual en la tabla
            tablaInventarioTree.setPlaceholder(new Label(
                    primeraCarga ? "✅ Inventario cargado correctamente." : "🔄 Inventario actualizado."
            ));
            primeraCarga = false;
        });

        // ❌ Si ocurre un error, mostrar mensaje y trazar excepción
        cargaTask.setOnFailed(e -> {
            tablaInventarioTree.setPlaceholder(new Label("❌ Error al cargar inventario"));
            if (cargaTask.getException() != null) cargaTask.getException().printStackTrace();
        });

        // Lanza el Task en un hilo daemon separado
        Thread t = new Thread(cargaTask);
        t.setDaemon(true);
        t.start();
    }

    private void expandirTodo(TreeItem<?> nodo) {
        if (nodo == null) return;
        nodo.setExpanded(true);
        for (TreeItem<?> hijo : nodo.getChildren()) expandirTodo(hijo);
    }

    private void recargarArbol(String filtro) {
        cargarArbolAsync(filtro);
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
                okDB = inventarioService.actualizarCampo(item.getProductoId(), campo, valor);
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
                    inventarioService.eliminarProducto(item.getProductoId());
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
            error("Seleccioná un producto o variante primero.");
            return;
        }

        ItemInventario item = sel.get();

        // 🧩 Si es variante → traer info desde ProductoVarianteDAO
        if (item.isEsVariante()) {
            Optional<Producto> baseOpt = productoDAO.findById(item.getProductoId());
            if (baseOpt.isEmpty()) {
                error("No se encontró el producto base.");
                return;
            }

            Producto base = baseOpt.get();
            Producto producto = new Producto();
            producto.setId(base.getId());
            producto.setEtiqueta(base.getEtiqueta());
            producto.setNombre(base.getNombre() + " (" + item.getColor() + " " + item.getTalle() + ")");
            producto.setPrecio(item.getPrecio());
            producto.setStockOnHand(item.getStockOnHand());

            pedirCantidad(producto, item.getVarianteId());
            return;
        }

        // 🧩 Si es producto base
        Optional<Producto> opt = productoDAO.findById(item.getProductoId());
        if (opt.isEmpty()) {
            error("No se encontró el producto en base de datos.");
            return;
        }

        Producto producto = opt.get();
        pedirCantidad(producto, null);
    }

    private void pedirCantidad(Producto producto, Long idVariante) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva venta");
        dialog.setHeaderText("Producto: " + producto.getNombre());
        dialog.setContentText("Cantidad:");

        DialogPane pane1 = dialog.getDialogPane();
        pane1.getStylesheets().add(getClass().getResource("/Estilos/Estilos.css").toExternalForm());
        pane1.getStyleClass().add("dialog-cuero");

        dialog.showAndWait().ifPresent(valor -> {
            try {
                int cantidad = Integer.parseInt(valor);
                if (cantidad <= 0) throw new NumberFormatException();

                BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
                NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                String totalFormateado = formato.format(total);

                // === Diálogo de confirmación ===
                Dialog<ButtonType> confirmar = new Dialog<>();
                confirmar.setTitle("Confirmar venta");

                DialogPane pane2 = confirmar.getDialogPane();
                pane2.getStylesheets().add(getClass().getResource("/Estilos/Estilos.css").toExternalForm());
                pane2.getStyleClass().add("dialog-cuero");

                Label header = new Label("💰 Total: " + totalFormateado);
                header.getStyleClass().add("dialog-header");
                pane2.setHeader(header);

                ButtonType ok = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                pane2.getButtonTypes().addAll(ok, cancel);

                Label lblProducto = new Label("Producto: " + producto.getNombre());
                Label lblCantidad = new Label("Cantidad: " + cantidad);
                Label lblPrecio = new Label("Precio unitario: " + formato.format(producto.getPrecio()));
                Label lblMedio = new Label("Medio de pago:");

                ComboBox<String> comboPago = new ComboBox<>();
                comboPago.getItems().addAll("Efectivo", "Tarjeta", "Transferencia", "MercadoPago");
                comboPago.setValue("Efectivo");

                VBox content = new VBox(lblProducto, lblCantidad, lblPrecio, lblMedio, comboPago);
                content.getStyleClass().add("dialog-content");
                pane2.setContent(content);

                Optional<ButtonType> res = confirmar.showAndWait();
                if (res.isPresent() && res.get() == ok) {
                    String medioPago = comboPago.getValue();
                    procesarVenta(producto, cantidad, total, medioPago, idVariante);
                }

            } catch (NumberFormatException e) {
                error("Cantidad inválida.");
            }
        });
    }

    private void procesarVenta(Producto producto, int cantidad, BigDecimal total, String medioPago, Long idVariante) {
        try {
            registrarVentaEnBD(producto, cantidad, total, medioPago, idVariante);
        } catch (Exception e) {
            e.printStackTrace();
            error("❌ Error al procesar la venta: " + e.getMessage());
        }
    }

    private void registrarVentaEnBD(Producto producto, int cantidad, BigDecimal total, String medioPago, Long idVariante) {
        try {
            Venta venta = new Venta();
            venta.setClienteNombre(null);
            venta.setMedioPago(medioPago);
            venta.setFecha(LocalDateTime.now());

            Venta.VentaItem item = new Venta.VentaItem();
            item.setProductoId(producto.getId());
            item.setProductoNombre(producto.getNombre());
            item.setProductoEtiqueta(producto.getEtiqueta());
            item.setQty(cantidad);
            item.setPrecioUnit(producto.getPrecio());

            venta.addItem(item);
            venta.calcularTotal();

            VentaDAO.registrarVentaCompleta(venta);

            boolean actualizado;
            if (idVariante != null) {
                actualizado = new ProductoVarianteDAO().descontarStock(idVariante, cantidad);
            } else {
                actualizado = productoDAO.descontarStock(producto.getId(), cantidad);
            }

            if (!actualizado) {
                error("⚠ No hay suficiente stock.");
                return;
            }

            recargarArbol(txtBuscarEtiqueta.getText());
            NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
            ok("✅ Venta registrada. Total: " + formato.format(total));

        } catch (Exception e) {
            e.printStackTrace();
            error("❌ Error al registrar venta: " + e.getMessage());
        }
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
                .hideAfter(javafx.util.Duration.seconds(3))
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

            try {
                boolean okDB;
                if (item.isEsVariante()) {
                    okDB = inventarioService.actualizarVariante(item.getVarianteId(), campo, nuevo);
                } else {
                    okDB = inventarioService.actualizarCampo(item.getProductoId(), campo, nuevo);
                }

                if (okDB) {
                    ok("✔ Cambios guardados en " + campo);
                    switch (campo.toLowerCase()) {
                        case "nombre" -> item.setNombreProducto(nuevo);
                        case "color" -> item.setColor(nuevo);
                        case "talle" -> item.setTalle(nuevo);
                    }
                } else {
                    error("⚠ No se pudo actualizar el campo " + campo);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                    okDB = inventarioService.actualizarVariante(item.getVarianteId(), campo, nuevo.toPlainString());
                } else {
                    okDB = inventarioService.actualizarCampo(item.getProductoId(), campo, nuevo.toPlainString());
                }

                if (okDB) {
                    ok("✔ " + campo + " actualizado correctamente");
                    if (campo.equalsIgnoreCase("precio"))
                        item.setPrecio(nuevo);
                    else
                        item.setCosto(nuevo);
                } else {
                    error("⚠ No se pudo actualizar el campo " + campo);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                    okDB = inventarioService.actualizarVariante(item.getVarianteId(), campo, nuevo.toString());
                } else {
                    okDB = inventarioService.actualizarCampo(item.getProductoId(), campo, nuevo.toString());
                }

                if (okDB) {
                    ok("✔ Stock actualizado correctamente");
                    item.setStockOnHand(nuevo);
                } else {
                    error("⚠ No se pudo actualizar el campo " + campo);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                case "etiqueta" -> total * 0.07;
                case "nombre" -> total * 0.25;
                case "color" -> total * 0.08;
                case "talle" -> total * 0.07;
                case "categoría", "categoria" -> total * 0.2;
                case "costo" -> total * 0.12;
                case "precio" -> total * 0.12;
                case "stock" -> total * 0.06;
                default -> total * 0.10;
            };

            // 🔒 Limitar ancho mínimo y máximo razonables
            col.setMinWidth(65);
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

            // ✅ Solo aplica a "color" y "talle"
            if (!List.of("color", "talle").contains(header))
                continue;

            TreeTableColumn<ItemInventario, Object> col = (TreeTableColumn<ItemInventario, Object>) anyCol;

            col.setCellFactory(tc -> new TextFieldTreeTableCell<>(new javafx.util.StringConverter<Object>() {
                @Override
                public String toString(Object value) {
                    if (value == null) return "";
                    return value.toString();
                }

                @Override
                public Object fromString(String s) {
                    return (s == null || s.isBlank()) ? null : s.trim();
                }
            }) {
                @Override
                public void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    TreeItem<ItemInventario> ti = getTreeTableRow() == null ? null : getTreeTableRow().getTreeItem();
                    if (ti == null || ti.getValue() == null) return;

                    ItemInventario data = ti.getValue();
                    boolean esVariante = data.isEsVariante();

                    // 🔸 Producto base → muestra guion gris
                    if (!esVariante) {
                        setText("—");
                        setStyle("-fx-text-fill: #9b8b74; -fx-font-style: italic;");
                    } else {
                        setStyle("-fx-text-fill: #2b2b2b; -fx-font-style: normal;");
                    }
                }

                @Override
                public void startEdit() {
                    TreeItem<ItemInventario> ti = getTreeTableRow() == null ? null : getTreeTableRow().getTreeItem();
                    if (ti != null && ti.getValue() != null && !ti.getValue().isEsVariante()) {
                        // 🔒 No permitir editar color/talle en producto base
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
    @FXML
    public void restaurarInventarioCompleto() {
        try {
            recargarArbol(txtBuscarEtiqueta.getText());
            ok("Inventario actualizado.");
        } catch (Exception e) {
            e.printStackTrace();
            error("❌ Error al restaurar inventario: " + e.getMessage());
        }
    }


    /**
     * Muestra solo los productos con stock entre 0 y 2.
     * Si el checkbox "Expandir auto" está activado, expande los nodos visibles.
     */
    @FXML
    private void mostrarBajoStock() {
        try {
            // ⚙️ Cargar todo el árbol desde la BD
            TreeItem<ItemInventario> root = inventarioService.cargarArbol("");

            if (root == null || root.getChildren().isEmpty()) {
                error("No hay productos cargados en el inventario.");
                return;
            }

            // 📦 Crear copia para filtrar
            TreeItem<ItemInventario> copia = clonarArbol(root);

            // 🔍 Filtrar recursivamente por stock <= 2
            filtrarPorStockBajo(copia);

            // 🚫 Si no hay resultados, informar al usuario
            if (copia.getChildren().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Stock bajo");
                alert.setHeaderText(null);
                alert.setContentText("No hay productos con stock menor o igual a 2.");
                alert.showAndWait();
                return;
            }

            // ✅ Mostrar la copia filtrada
            tablaInventarioTree.setRoot(copia);
            tablaInventarioTree.setShowRoot(false);

            // ⚙️ Si el usuario activó “Expandir auto”, expandir todos los nodos visibles
            boolean expandir = prefs.getBoolean(PREF_EXPANDIR_NODOS, false);
            if (expandir) expandirTodo(copia);

            ok("📦 Mostrando productos con stock entre 0 y 2.");

        } catch (Exception e) {
            e.printStackTrace();
            error("❌ Error al mostrar productos con stock bajo: " + e.getMessage());
        }
    }

    /**
     * Elimina de forma recursiva los nodos cuyo stock > 2.
     */
    private boolean filtrarPorStockBajo(TreeItem<ItemInventario> nodo) {
        if (nodo == null || nodo.getChildren() == null) return false;

        Iterator<TreeItem<ItemInventario>> it = nodo.getChildren().iterator();
        boolean algunHijoVisible = false;

        while (it.hasNext()) {
            TreeItem<ItemInventario> hijo = it.next();
            ItemInventario data = hijo.getValue();

            boolean coincide = false;
            if (data != null && data.getStockOnHand() <= 2) {
                coincide = true;
            }

            boolean hijosCoinciden = filtrarPorStockBajo(hijo);
            if (!coincide && !hijosCoinciden) {
                it.remove();
            } else {
                algunHijoVisible = true;
            }
        }

        return algunHijoVisible;
    }
    // === dentro de ProductoTreeController ===

    // Convierte el TreeTableView visible en una lista plana en orden
    private List<ItemInventario> flattenVisible() {
        List<ItemInventario> out = new ArrayList<>();
        TreeItem<ItemInventario> root = tablaInventarioTree.getRoot();
        if (root == null) return out;

        Deque<TreeItem<ItemInventario>> stack = new ArrayDeque<>(root.getChildren());

        while (!stack.isEmpty()) {
            TreeItem<ItemInventario> it = stack.removeFirst();
            if (it.getValue() != null) out.add(it.getValue());

            // mantener orden de la UI (preorden)
            if (!it.getChildren().isEmpty()) {
                List<TreeItem<ItemInventario>> hijos = new ArrayList<>(it.getChildren());
                Collections.reverse(hijos); // para que mantenga el orden
                for (TreeItem<ItemInventario> h : hijos) {
                    stack.addFirst(h);
                }
            }
        }
        return out;
    }


    @FXML
    private void exportarVistaPDF() {
        try {
            List<ItemInventario> flat = flattenVisible();
            if (flat.isEmpty()) {
                error("No hay datos para exportar.");
                return;
            }
            ExportarController.exportarInventarioTreePDF(flat, tablaInventarioTree.getScene().getWindow());
        } catch (Exception e) {
            e.printStackTrace();
            error("Error exportando PDF: " + e.getMessage());
        }
    }

    @FXML
    private void exportarVistaExcel() {
        try {
            List<ItemInventario> flat = flattenVisible();
            if (flat.isEmpty()) {
                error("No hay datos para exportar.");
                return;
            }
            ExportarController.exportarInventarioTreeExcel(flat, tablaInventarioTree.getScene().getWindow());
        } catch (Exception e) {
            e.printStackTrace();
            error("Error exportando Excel: " + e.getMessage());
        }
    }

}
