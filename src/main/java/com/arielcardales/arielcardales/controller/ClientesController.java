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
        // Definición de columnas: {Título, propertyName, ancho%, anchoPx}
        String[][] columnas = {
                {"ID", "id", "0.05", "50"},
                {"Nombre", "nombre", "0.25", "200"},
                {"DNI", "dni", "0.12", "100"},
                {"Teléfono", "telefono", "0.15", "120"},
                {"Email", "email", "0.23", "180"},
                {"Notas", "notas", "0.20", "150"}
        };

        List<TableColumn<Cliente, ?>> cols = com.arielcardales.arielcardales.Util.Tablas.crearColumnas(columnas);
        tablaClientes.getColumns().setAll(cols);

        // Personalizar columna Notas (mostrar tooltip completo)
        @SuppressWarnings("unchecked")
        TableColumn<Cliente, Object> colNotas = (TableColumn<Cliente, Object>)
                cols.stream().filter(c -> c.getId().equals("notas")).findFirst().orElse(null);

        if (colNotas != null) {
            colNotas.setCellFactory(col -> new TableCell<Cliente, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setTooltip(null);
                        return;
                    }

                    Cliente cliente = getTableRow().getItem();
                    String notas = cliente.getNotas();

                    if (notas != null && !notas.trim().isEmpty()) {
                        // Mostrar primeros 30 caracteres
                        String preview = notas.length() > 30 ? notas.substring(0, 30) + "..." : notas;
                        setText(preview);
                        setTooltip(new Tooltip(notas));
                    } else {
                        setText("-");
                        setTooltip(null);
                    }
                }
            });
        }

        tablaClientes.setItems(clientesData);
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Doble clic para editar
        tablaClientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                editarClienteSeleccionado();
            }
        });
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
    private void editarClienteSeleccionado() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Selecciona un cliente de la tabla para editar");
            return;
        }

        mostrarDialogoCliente(seleccionado);
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
