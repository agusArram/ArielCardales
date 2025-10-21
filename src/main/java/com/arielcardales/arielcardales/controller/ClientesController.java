package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ClienteDAO;
import com.arielcardales.arielcardales.Entidades.Cliente;
import com.arielcardales.arielcardales.Entidades.ItemCliente;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.Notifications;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ClientesController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevoCliente;
    @FXML private Button btnEliminarCliente;
    @FXML private Button btnLimpiar;

    @FXML private TreeTableView<ItemCliente> tablaClientes;
    @FXML private Label lblTotalClientes;

    @FXML private VBox vboxLoading;
    @FXML private ProgressIndicator progressIndicator;

    private TreeItem<ItemCliente> rootCompleto;
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private Task<TreeItem<ItemCliente>> cargaTask;
    private javafx.animation.PauseTransition pausaBusqueda = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));

    @FXML
    public void initialize() {
        configurarUI();
        cargarArbolAsync("");
    }

    // ============================================================================
    // CONFIGURACIÓN UI
    // ============================================================================

    private void configurarUI() {
        configurarColumnas();
        configurarPropiedadesTabla();
        configurarBusqueda();
        configurarRowFactory();
    }

    /**
     * Configura las columnas del TreeTableView con doble propósito
     */
    private void configurarColumnas() {
        tablaClientes.getColumns().clear();

        // Columna Nombre / Fecha
        TreeTableColumn<ItemCliente, String> colNombre = new TreeTableColumn<>("Nombre / Fecha");
        colNombre.setPrefWidth(250);
        colNombre.setCellValueFactory(param -> {
            ItemCliente item = param.getValue().getValue();
            if (item == null) return new SimpleStringProperty("");

            if (item.isEsVenta()) {
                // Es venta: mostrar fecha con icono
                if (item.getFecha() != null) {
                    return new SimpleStringProperty(
                        "  📅 " + item.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    );
                }
                return new SimpleStringProperty("  📅 (sin fecha)");
            } else {
                // Es cliente: mostrar nombre
                return item.nombreProperty();
            }
        });

        // Columna DNI / Medio de Pago
        TreeTableColumn<ItemCliente, String> colDniMedio = new TreeTableColumn<>("DNI / Medio Pago");
        colDniMedio.setPrefWidth(130);
        colDniMedio.setCellValueFactory(param -> {
            ItemCliente item = param.getValue().getValue();
            if (item == null) return new SimpleStringProperty("");

            if (item.isEsVenta()) {
                return item.medioPagoProperty();
            } else {
                return item.dniProperty();
            }
        });

        // Columna Teléfono / Total
        TreeTableColumn<ItemCliente, String> colTelTotal = new TreeTableColumn<>("Teléfono / Total");
        colTelTotal.setPrefWidth(140);
        colTelTotal.setCellValueFactory(param -> {
            ItemCliente item = param.getValue().getValue();
            if (item == null) return new SimpleStringProperty("");

            if (item.isEsVenta()) {
                // Mostrar total formateado
                if (item.getTotal() != null) {
                    return new SimpleStringProperty(String.format("$%.2f", item.getTotal()));
                }
                return new SimpleStringProperty("$0.00");
            } else {
                return item.telefonoProperty();
            }
        });
        colTelTotal.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Columna Email
        TreeTableColumn<ItemCliente, String> colEmail = new TreeTableColumn<>("Email");
        colEmail.setPrefWidth(200);
        colEmail.setCellValueFactory(param -> {
            ItemCliente item = param.getValue().getValue();
            if (item == null) return new SimpleStringProperty("");

            if (item.isEsVenta()) {
                return new SimpleStringProperty(""); // Vacío para ventas
            } else {
                return item.emailProperty();
            }
        });

        tablaClientes.getColumns().setAll(colNombre, colDniMedio, colTelTotal, colEmail);
    }

    /**
     * Configura propiedades generales del TreeTableView
     */
    private void configurarPropiedadesTabla() {
        tablaClientes.setShowRoot(false);
        tablaClientes.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        tablaClientes.setStyle("-fx-background-color: white;");

        // Placeholder con spinner
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);
        tablaClientes.setPlaceholder(pi);
    }

    /**
     * Configura el sistema de búsqueda reactiva
     */
    private void configurarBusqueda() {
        // Búsqueda con debounce
        txtBuscar.textProperty().addListener((o, oldValue, newValue) -> {
            pausaBusqueda.stop();
            pausaBusqueda.setOnFinished(e -> cargarArbolAsync(newValue != null ? newValue : ""));
            pausaBusqueda.playFromStart();

            // Fallback instantáneo si se borra todo
            if (newValue == null || newValue.isBlank()) {
                Platform.runLater(() -> cargarArbolAsync(""));
            }
        });
    }

    /**
     * Configura el comportamiento de las filas (estilos)
     */
    private void configurarRowFactory() {
        tablaClientes.setRowFactory(tv -> {
            TreeTableRow<ItemCliente> row = new TreeTableRow<>();

            // Pseudo-clase CSS para filas hijas (ventas)
            row.treeItemProperty().addListener((obs, oldItem, newItem) -> {
                boolean esHijo = newItem != null &&
                        newItem.getParent() != null &&
                        newItem.getParent().getParent() != null;
                row.pseudoClassStateChanged(PseudoClass.getPseudoClass("hijo"), esHijo);
            });

            // Doble clic para editar cliente
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    TreeItem<ItemCliente> item = row.getTreeItem();
                    if (item != null && item.getValue() != null && !item.getValue().isEsVenta()) {
                        // Es un cliente, abrir para editar
                        Cliente cliente = convertirACliente(item.getValue());
                        mostrarDialogoCliente(cliente);
                    }
                }
            });

            return row;
        });
    }

    // ============================================================================
    // CARGA DE DATOS
    // ============================================================================

    /**
     * Carga el árbol de clientes con sus ventas en segundo plano
     */
    private void cargarArbolAsync(String filtro) {
        // Si hay una carga anterior en ejecución, cancelarla
        if (cargaTask != null && cargaTask.isRunning()) {
            cargaTask.cancel();
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        // Spinner de carga
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);
        tablaClientes.setPlaceholder(pi);

        mostrarLoading(true);

        // Task que carga el árbol en background
        cargaTask = new Task<>() {
            @Override
            protected TreeItem<ItemCliente> call() throws Exception {
                return clienteDAO.cargarArbol(filtro);
            }
        };

        cargaTask.setOnSucceeded(e -> {
            TreeItem<ItemCliente> root = cargaTask.getValue();
            if (root == null) {
                tablaClientes.setPlaceholder(new Label("Sin datos disponibles"));
                mostrarLoading(false);
                return;
            }

            rootCompleto = root;
            tablaClientes.setRoot(root);
            tablaClientes.setShowRoot(false);

            actualizarEstadisticas();

            tablaClientes.setPlaceholder(new Label("✅ Clientes cargados correctamente."));
            mostrarLoading(false);
        });

        cargaTask.setOnFailed(e -> {
            tablaClientes.setPlaceholder(new Label("❌ Error al cargar clientes"));
            if (cargaTask.getException() != null) cargaTask.getException().printStackTrace();
            mostrarLoading(false);
        });

        Thread t = new Thread(cargaTask);
        t.setDaemon(true);
        t.start();
    }

    // ============================================================================
    // ACCIONES
    // ============================================================================

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cargarArbolAsync("");
    }

    @FXML
    private void abrirNuevoCliente() {
        mostrarDialogoCliente(null);
    }

    @FXML
    private void eliminarClienteSeleccionado() {
        TreeItem<ItemCliente> selectedItem = tablaClientes.getSelectionModel().getSelectedItem();

        if (selectedItem == null || selectedItem.getValue() == null) {
            mostrarAlerta("Selecciona un cliente de la tabla para eliminar");
            return;
        }

        ItemCliente item = selectedItem.getValue();

        // Verificar que sea un cliente (no una venta)
        if (item.isEsVenta()) {
            mostrarAlerta("No puedes eliminar una venta desde aquí. Selecciona el cliente.");
            return;
        }

        // Confirmación
        Alert confirmacion = new Alert(Alert.AlertType.WARNING);
        confirmacion.setTitle("⚠️ Confirmar eliminación");
        confirmacion.setHeaderText("¿Eliminar cliente?");
        confirmacion.setContentText(
                "Cliente: " + item.getNombre() + "\n" +
                        "DNI: " + (item.getDni() != null ? item.getDni() : "-") + "\n\n" +
                        "⚠️ Esta acción NO se puede deshacer.\n\n" +
                        "¿Deseas continuar?"
        );

        ButtonType btnConfirmar = new ButtonType("Sí, eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(btnConfirmar, btnCancelar);

        if (confirmacion.showAndWait().orElse(btnCancelar) != btnConfirmar) {
            return;
        }

        mostrarLoading(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                clienteDAO.deleteById(item.getClienteId());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            cargarArbolAsync(txtBuscar.getText());
            ok("Cliente eliminado correctamente");
        });

        task.setOnFailed(e -> {
            mostrarError("Error al eliminar cliente", task.getException().getMessage());
            task.getException().printStackTrace();
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    // ============================================================================
    // DIÁLOGO CREAR/EDITAR CLIENTE
    // ============================================================================

    /**
     * Muestra diálogo para crear o editar cliente
     */
    private void mostrarDialogoCliente(Cliente cliente) {
        boolean esNuevo = (cliente == null);
        Cliente clienteEdit = esNuevo ? new Cliente() : cliente;

        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle(esNuevo ? "👤 Nuevo Cliente" : "✏️ Editar Cliente");
        dialog.setHeaderText(null);

        // Botones
        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        // Formulario
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setStyle("-fx-background-color: #d8b075;");

        // Campos con estilo
        TextField txtNombre = new TextField(clienteEdit.getNombre());
        txtNombre.setPromptText("Nombre completo");
        txtNombre.setPrefWidth(300);
        aplicarEstiloCampo(txtNombre);

        TextField txtDni = new TextField(clienteEdit.getDni());
        txtDni.setPromptText("DNI (opcional)");
        txtDni.setPrefWidth(300);
        aplicarEstiloCampo(txtDni);

        TextField txtTelefono = new TextField(clienteEdit.getTelefono());
        txtTelefono.setPromptText("Teléfono");
        txtTelefono.setPrefWidth(300);
        aplicarEstiloCampo(txtTelefono);

        TextField txtEmail = new TextField(clienteEdit.getEmail());
        txtEmail.setPromptText("Email");
        txtEmail.setPrefWidth(300);
        aplicarEstiloCampo(txtEmail);

        TextArea txtNotas = new TextArea(clienteEdit.getNotas());
        txtNotas.setPromptText("Notas adicionales");
        txtNotas.setPrefRowCount(3);
        txtNotas.setPrefWidth(300);
        aplicarEstiloCampo(txtNotas);

        // Labels con estilo
        Label lblNombre = crearLabelFormulario("Nombre:");
        Label lblDni = crearLabelFormulario("DNI:");
        Label lblTelefono = crearLabelFormulario("Teléfono:");
        Label lblEmail = crearLabelFormulario("Email:");
        Label lblNotas = crearLabelFormulario("Notas:");

        grid.add(lblNombre, 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(lblDni, 0, 1);
        grid.add(txtDni, 1, 1);
        grid.add(lblTelefono, 0, 2);
        grid.add(txtTelefono, 1, 2);
        grid.add(lblEmail, 0, 3);
        grid.add(txtEmail, 1, 3);
        grid.add(lblNotas, 0, 4);
        grid.add(txtNotas, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Estilo del DialogPane
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #d8b075; " +
            "-fx-font-family: 'Lora';"
        );

        // Estilizar botones del diálogo
        Platform.runLater(() -> {
            Button btnGuardarNode = (Button) dialog.getDialogPane().lookupButton(btnGuardar);
            Button btnCancelarNode = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

            if (btnGuardarNode != null) {
                btnGuardarNode.setStyle(
                    "-fx-background-color: #6aad6a; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 10 30; " +
                    "-fx-min-width: 120px; " +
                    "-fx-cursor: hand; " +
                    "-fx-background-radius: 6px;"
                );
            }

            if (btnCancelarNode != null) {
                btnCancelarNode.setStyle(
                    "-fx-background-color: #cfa971; " +
                    "-fx-text-fill: #2b2b2b; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 10 30; " +
                    "-fx-min-width: 120px; " +
                    "-fx-cursor: hand; " +
                    "-fx-background-radius: 6px;"
                );
            }

            txtNombre.requestFocus();
        });

        // Validación
        Button btnGuardarNode = (Button) dialog.getDialogPane().lookupButton(btnGuardar);
        btnGuardarNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
                mostrarAlerta("El nombre es obligatorio");
                event.consume();
                return;
            }

            // Validar DNI duplicado
            String dniNuevo = txtDni.getText() != null ? txtDni.getText().trim() : null;
            if (dniNuevo != null && !dniNuevo.isEmpty()) {
                if (clienteDAO.existeDniDuplicado(dniNuevo, clienteEdit.getId())) {
                    mostrarAlerta("Ya existe un cliente con ese DNI");
                    event.consume();
                    return;
                }
            }

            // Validar teléfono duplicado
            String telNuevo = txtTelefono.getText() != null ? txtTelefono.getText().trim() : null;
            if (telNuevo != null && !telNuevo.isEmpty()) {
                if (clienteDAO.existeTelefonoDuplicado(telNuevo, clienteEdit.getId())) {
                    mostrarAlerta("Ya existe un cliente con ese teléfono");
                    event.consume();
                    return;
                }
            }
        });

        // Resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                clienteEdit.setNombre(txtNombre.getText().trim());
                clienteEdit.setDni(txtDni.getText() != null && !txtDni.getText().trim().isEmpty()
                        ? txtDni.getText().trim() : null);
                clienteEdit.setTelefono(txtTelefono.getText() != null && !txtTelefono.getText().trim().isEmpty()
                        ? txtTelefono.getText().trim() : null);
                clienteEdit.setEmail(txtEmail.getText() != null && !txtEmail.getText().trim().isEmpty()
                        ? txtEmail.getText().trim() : null);
                clienteEdit.setNotas(txtNotas.getText() != null && !txtNotas.getText().trim().isEmpty()
                        ? txtNotas.getText().trim() : null);
                return clienteEdit;
            }
            return null;
        });

        Optional<Cliente> resultado = dialog.showAndWait();
        resultado.ifPresent(c -> guardarCliente(c, esNuevo));
    }

    private void guardarCliente(Cliente cliente, boolean esNuevo) {
        mostrarLoading(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (esNuevo) {
                    clienteDAO.insert(cliente);
                } else {
                    clienteDAO.update(cliente);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            cargarArbolAsync(txtBuscar.getText());
            ok(esNuevo ? "Cliente creado correctamente" : "Cliente actualizado correctamente");
        });

        task.setOnFailed(e -> {
            mostrarError("Error al guardar cliente", task.getException().getMessage());
            task.getException().printStackTrace();
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    // ============================================================================
    // HELPERS
    // ============================================================================

    /**
     * Convierte un ItemCliente a Cliente (para edición)
     */
    private Cliente convertirACliente(ItemCliente item) {
        Cliente cliente = new Cliente();
        cliente.setId(item.getClienteId());
        cliente.setNombre(item.getNombre());
        cliente.setDni(item.getDni());
        cliente.setTelefono(item.getTelefono());
        cliente.setEmail(item.getEmail());
        return cliente;
    }

    private void actualizarEstadisticas() {
        if (rootCompleto != null) {
            lblTotalClientes.setText(String.valueOf(rootCompleto.getChildren().size()));
        } else {
            lblTotalClientes.setText("0");
        }
    }

    private void mostrarLoading(boolean mostrar) {
        if (vboxLoading != null && progressIndicator != null) {
            vboxLoading.setVisible(mostrar);
            vboxLoading.setManaged(mostrar);
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void ok(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(3))
                .showConfirm();
    }

    // ============================================================================
    // MÉTODOS DE ESTILO
    // ============================================================================

    private void aplicarEstiloCampo(javafx.scene.control.TextInputControl campo) {
        campo.setStyle(
            "-fx-background-color: #f3d8ad; " +
            "-fx-border-color: #b88a52; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 5px; " +
            "-fx-background-radius: 5px; " +
            "-fx-padding: 6px; " +
            "-fx-font-size: 13px; " +
            "-fx-text-fill: #2b2b2b;"
        );

        // Estilo al hacer foco
        campo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                campo.setStyle(
                    "-fx-background-color: #f3d8ad; " +
                    "-fx-border-color: #cfa971; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-padding: 6px; " +
                    "-fx-font-size: 13px; " +
                    "-fx-text-fill: #2b2b2b; " +
                    "-fx-effect: dropshadow(gaussian, rgba(207, 169, 113, 0.5), 6, 0, 0, 0);"
                );
            } else {
                campo.setStyle(
                    "-fx-background-color: #f3d8ad; " +
                    "-fx-border-color: #b88a52; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-padding: 6px; " +
                    "-fx-font-size: 13px; " +
                    "-fx-text-fill: #2b2b2b;"
                );
            }
        });
    }

    private Label crearLabelFormulario(String texto) {
        Label label = new Label(texto);
        label.setStyle(
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #5D4E37; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 5 10 5 0;"
        );
        return label;
    }
}
