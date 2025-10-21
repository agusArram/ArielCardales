package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ClienteDAO;
import com.arielcardales.arielcardales.DAO.VentaDAO;
import com.arielcardales.arielcardales.Entidades.Cliente;
import com.arielcardales.arielcardales.Entidades.Venta;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.Notifications;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ClientesController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevoCliente;
    @FXML private Button btnEditarCliente;
    @FXML private Button btnEliminarCliente;
    @FXML private Button btnLimpiar;

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private Label lblTotalClientes;

    @FXML private VBox vboxLoading;
    @FXML private ProgressIndicator progressIndicator;

    private ObservableList<Cliente> clientesData;
    private ClienteDAO clienteDAO;

    @FXML
    public void initialize() {
        clienteDAO = new ClienteDAO();
        clientesData = FXCollections.observableArrayList();

        configurarTablaClientes();
        configurarFiltros();
        cargarClientes();
    }

    private void configurarTablaClientes() {
        // ✅ HACER LA TABLA EDITABLE
        tablaClientes.setEditable(true);

        // Columna ID (no editable)
        TableColumn<Cliente, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cd -> cd.getValue().idProperty().asObject());
        colId.setPrefWidth(50);
        colId.setStyle("-fx-alignment: CENTER;");
        colId.setEditable(false);

        // Columna Nombre (editable)
        TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(cd -> cd.getValue().nombreProperty());
        colNombre.setPrefWidth(200);
        colNombre.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colNombre.setOnEditCommit(event -> {
            Cliente cliente = event.getRowValue();
            String nuevoNombre = event.getNewValue();

            if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                mostrarAlerta("El nombre no puede estar vacío");
                tablaClientes.refresh();
                return;
            }

            cliente.setNombre(nuevoNombre.trim());
            guardarClienteInline(cliente);
        });

        // Columna DNI (editable)
        TableColumn<Cliente, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(cd -> cd.getValue().dniProperty());
        colDni.setPrefWidth(100);
        colDni.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colDni.setOnEditCommit(event -> {
            Cliente cliente = event.getRowValue();
            String nuevoDni = event.getNewValue();

            // Validar duplicado
            if (nuevoDni != null && !nuevoDni.trim().isEmpty()) {
                if (clienteDAO.existeDniDuplicado(nuevoDni.trim(), cliente.getId())) {
                    mostrarAlerta("Ya existe un cliente con ese DNI");
                    tablaClientes.refresh();
                    return;
                }
            }

            cliente.setDni(nuevoDni != null && !nuevoDni.trim().isEmpty() ? nuevoDni.trim() : null);
            guardarClienteInline(cliente);
        });

        // Columna Teléfono (editable)
        TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(cd -> cd.getValue().telefonoProperty());
        colTelefono.setPrefWidth(120);
        colTelefono.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colTelefono.setOnEditCommit(event -> {
            Cliente cliente = event.getRowValue();
            String nuevoTel = event.getNewValue();

            // Validar duplicado
            if (nuevoTel != null && !nuevoTel.trim().isEmpty()) {
                if (clienteDAO.existeTelefonoDuplicado(nuevoTel.trim(), cliente.getId())) {
                    mostrarAlerta("Ya existe un cliente con ese teléfono");
                    tablaClientes.refresh();
                    return;
                }
            }

            cliente.setTelefono(nuevoTel != null && !nuevoTel.trim().isEmpty() ? nuevoTel.trim() : null);
            guardarClienteInline(cliente);
        });

        // Columna Email (editable)
        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(cd -> cd.getValue().emailProperty());
        colEmail.setPrefWidth(180);
        colEmail.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(event -> {
            Cliente cliente = event.getRowValue();
            String nuevoEmail = event.getNewValue();
            cliente.setEmail(nuevoEmail != null && !nuevoEmail.trim().isEmpty() ? nuevoEmail.trim() : null);
            guardarClienteInline(cliente);
        });

        // Columna Notas (editable)
        TableColumn<Cliente, String> colNotas = new TableColumn<>("Notas");
        colNotas.setCellValueFactory(cd -> cd.getValue().notasProperty());
        colNotas.setPrefWidth(150);
        colNotas.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colNotas.setOnEditCommit(event -> {
            Cliente cliente = event.getRowValue();
            String nuevasNotas = event.getNewValue();
            cliente.setNotas(nuevasNotas != null && !nuevasNotas.trim().isEmpty() ? nuevasNotas.trim() : null);
            guardarClienteInline(cliente);
        });

        tablaClientes.getColumns().setAll(colId, colNombre, colDni, colTelefono, colEmail, colNotas);

        tablaClientes.setItems(clientesData);
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configurarFiltros() {
        // Búsqueda en tiempo real
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 2) {
                buscarClientes(newVal);
            } else if (newVal == null || newVal.isEmpty()) {
                cargarClientes();
            }
        });
    }

    private void cargarClientes() {
        mostrarLoading(true);

        Task<List<Cliente>> task = new Task<>() {
            @Override
            protected List<Cliente> call() throws Exception {
                return clienteDAO.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            clientesData.setAll(task.getValue());
            actualizarEstadisticas();
            mostrarLoading(false);
        });

        task.setOnFailed(e -> {
            mostrarError("Error al cargar clientes", task.getException().getMessage());
            task.getException().printStackTrace();
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    private void buscarClientes(String criterio) {
        mostrarLoading(true);

        Task<List<Cliente>> task = new Task<>() {
            @Override
            protected List<Cliente> call() throws Exception {
                // Buscar por nombre o DNI
                List<Cliente> resultados = clienteDAO.buscarPorNombre(criterio);

                // Si no hay resultados y el criterio parece un DNI, buscar por DNI exacto
                if (resultados.isEmpty() && criterio.matches("\\d+")) {
                    Optional<Cliente> porDni = clienteDAO.buscarPorDni(criterio);
                    porDni.ifPresent(resultados::add);
                }

                return resultados;
            }
        };

        task.setOnSucceeded(e -> {
            clientesData.setAll(task.getValue());
            actualizarEstadisticas();
            mostrarLoading(false);
        });

        task.setOnFailed(e -> {
            mostrarError("Error en la búsqueda", task.getException().getMessage());
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cargarClientes();
    }

    @FXML
    private void abrirNuevoCliente() {
        mostrarDialogoCliente(null);
    }

    @FXML
    private void eliminarClienteSeleccionado() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Selecciona un cliente de la tabla para eliminar");
            return;
        }

        // Confirmación
        Alert confirmacion = new Alert(Alert.AlertType.WARNING);
        confirmacion.setTitle("⚠️ Confirmar eliminación");
        confirmacion.setHeaderText("¿Eliminar cliente?");
        confirmacion.setContentText(
                "Cliente: " + seleccionado.getNombre() + "\n" +
                        "DNI: " + (seleccionado.getDni() != null ? seleccionado.getDni() : "-") + "\n\n" +
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
                clienteDAO.deleteById(seleccionado.getId());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            cargarClientes();
            ok("Cliente eliminado correctamente");
        });

        task.setOnFailed(e -> {
            mostrarError("Error al eliminar cliente", task.getException().getMessage());
            task.getException().printStackTrace();
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    /**
     * Muestra diálogo para crear o editar cliente
     */
    private void mostrarDialogoCliente(Cliente cliente) {
        boolean esNuevo = (cliente == null);
        Cliente clienteEdit = esNuevo ? new Cliente() : cliente;

        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle(esNuevo ? "Nuevo Cliente" : "Editar Cliente");
        dialog.setHeaderText(esNuevo ? "Ingrese los datos del nuevo cliente" : "Edite los datos del cliente");

        // Botones
        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        // Formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtNombre = new TextField(clienteEdit.getNombre());
        txtNombre.setPromptText("Nombre completo");

        TextField txtDni = new TextField(clienteEdit.getDni());
        txtDni.setPromptText("DNI (opcional)");

        TextField txtTelefono = new TextField(clienteEdit.getTelefono());
        txtTelefono.setPromptText("Teléfono");

        TextField txtEmail = new TextField(clienteEdit.getEmail());
        txtEmail.setPromptText("Email");

        TextArea txtNotas = new TextArea(clienteEdit.getNotas());
        txtNotas.setPromptText("Notas adicionales");
        txtNotas.setPrefRowCount(3);

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("DNI:"), 0, 1);
        grid.add(txtDni, 1, 1);
        grid.add(new Label("Teléfono:"), 0, 2);
        grid.add(txtTelefono, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(txtEmail, 1, 3);
        grid.add(new Label("Notas:"), 0, 4);
        grid.add(txtNotas, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Foco inicial en nombre
        Platform.runLater(() -> txtNombre.requestFocus());

        // Validación
        Button btnGuardarNode = (Button) dialog.getDialogPane().lookupButton(btnGuardar);
        btnGuardarNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
                mostrarAlerta("El nombre es obligatorio");
                event.consume();
                return;
            }

            // Validar DNI duplicado (solo si se ingresó un DNI)
            String dniNuevo = txtDni.getText() != null ? txtDni.getText().trim() : null;
            if (dniNuevo != null && !dniNuevo.isEmpty()) {
                if (clienteDAO.existeDniDuplicado(dniNuevo, clienteEdit.getId())) {
                    mostrarAlerta("Ya existe un cliente con ese DNI");
                    event.consume();
                    return;
                }
            }

            // Validar teléfono duplicado (solo si se ingresó un teléfono)
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

    /**
     * Guarda cambios de edición inline (sin recargar toda la tabla)
     */
    private void guardarClienteInline(Cliente cliente) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                clienteDAO.update(cliente);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            ok("✓ Cambio guardado");
            tablaClientes.refresh();
        });

        task.setOnFailed(e -> {
            mostrarError("Error al guardar", task.getException().getMessage());
            task.getException().printStackTrace();
            cargarClientes(); // Recargar para deshacer cambio
        });

        new Thread(task).start();
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
            cargarClientes();
            ok(esNuevo ? "Cliente creado correctamente" : "Cliente actualizado correctamente");
        });

        task.setOnFailed(e -> {
            mostrarError("Error al guardar cliente", task.getException().getMessage());
            task.getException().printStackTrace();
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    private void actualizarEstadisticas() {
        lblTotalClientes.setText(String.valueOf(clientesData.size()));
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
}
