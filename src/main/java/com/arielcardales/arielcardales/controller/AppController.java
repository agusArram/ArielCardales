package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.App;
import com.arielcardales.arielcardales.DAO.InventarioDAO;
import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Licencia.Licencia;
import com.arielcardales.arielcardales.session.SessionManager;
import com.arielcardales.arielcardales.session.SessionPersistence;
import com.arielcardales.arielcardales.Updates.UpdateConfig;
import com.arielcardales.arielcardales.Updates.UpdateDialog;
import com.arielcardales.arielcardales.Updates.UpdateManager;
import com.arielcardales.arielcardales.Util.Arboles;
import com.arielcardales.arielcardales.Util.Transiciones;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.net.URI;
import java.sql.SQLException;

public class AppController {

    private ProductoTreeController productoController;

    @FXML
    private VBox contenedorPrincipal;

    @FXML
    private Menu menuAdministracion;

    private Parent vistaProductos;
    private UpdateManager updateManager;

    @FXML
    public void initialize() {
        // 1. Verificar que haya sesión activa (el login ya validó todo)
        if (!SessionManager.getInstance().isAutenticado()) {
            System.err.println("⚠️ ADVERTENCIA: AppController cargado sin sesión activa");
            Platform.exit();
            return;
        }

        // 2. Configurar visibilidad del menú de administración según el plan
        configurarMenuAdministracion();

        // 3. Cargar la vista principal/hub
        mostrarVistaPrincipal();

        // 4. Inicializar update manager
        updateManager = new UpdateManager();

        // 5. Mostrar info de licencia después de cargar
        Platform.runLater(this::mostrarInfoLicencia);
    }

    /**
     * Configura la visibilidad del menú de administración según el plan de licencia
     * Solo el plan DEV tiene acceso a funciones de administración
     */
    private void configurarMenuAdministracion() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔐 CONFIGURANDO MENÚ DE ADMINISTRACIÓN");

        SessionManager session = SessionManager.getInstance();
        Licencia licencia = session.getLicenciaSafe();

        System.out.println("   menuAdministracion: " + (menuAdministracion != null ? "✓ Inyectado" : "✗ NULL"));
        System.out.println("   Sesión autenticada: " + (session.isAutenticado() ? "✓ SÍ" : "✗ NO"));
        System.out.println("   licencia: " + (licencia != null ? "✓ Cargada" : "✗ NULL"));

        if (menuAdministracion != null && licencia != null) {
            Licencia.PlanLicencia plan = licencia.getPlan();
            System.out.println("   Plan actual: " + plan);
            System.out.println("   Cliente: " + licencia.getNombre() + " (" + licencia.getEmail() + ")");
            System.out.println("   Cliente ID: " + licencia.getClienteId());

            // Solo mostrar el menú de administración si el plan es DEV
            boolean tieneAcceso = plan == Licencia.PlanLicencia.DEV;

            System.out.println("   ¿Es plan DEV?: " + (tieneAcceso ? "SÍ" : "NO"));

            // Ocultar el menú si no tiene acceso, además de deshabilitarlo
            menuAdministracion.setVisible(tieneAcceso);
            menuAdministracion.setDisable(!tieneAcceso);

            if (tieneAcceso) {
                System.out.println("   ✅ RESULTADO: Menú de administración VISIBLE y HABILITADO");
            } else {
                System.out.println("   🔒 RESULTADO: Menú de administración OCULTO y DESHABILITADO");
            }
        } else {
            System.out.println("   ⚠️ ERROR: No se pudo configurar el menú");
            if (menuAdministracion == null) {
                System.out.println("      - menuAdministracion es NULL (problema de fx:id)");
            }
            if (licencia == null) {
                System.out.println("      - licencia es NULL (sin sesión activa)");
            }
        }

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Muestra información de la licencia actual (solo planes DEMO o próximos a vencer)
     */
    private void mostrarInfoLicencia() {
        SessionManager session = SessionManager.getInstance();

        if (!session.isAutenticado()) {
            return;
        }

        Licencia lic = session.getLicencia();
        long diasRestantes = session.getDiasRestantes();

        // Verificar si está por expirar (menos de 7 días)
        if (diasRestantes > 0 && diasRestantes <= 7) {
            Notifications.create()
                .title("⚠ Licencia por expirar")
                .text("Su licencia vence en " + diasRestantes + " días.\n" +
                      "Contacte al administrador para renovar.")
                .position(Pos.TOP_RIGHT)
                .showWarning();
        }

        // Solo mostrar info en planes DEMO o si quedan pocos días
        if (lic.getPlan() == Licencia.PlanLicencia.DEMO || diasRestantes <= 7) {
            String estado = diasRestantes < 0 ? "❌ Expirada" :
                           diasRestantes <= 7 ? "⚠ " + diasRestantes + " días restantes" :
                           "✅ Activa";

            Notifications.create()
                .title("ℹ Información de Licencia")
                .text("Plan: " + lic.getPlan() + "\n" +
                      "Estado: " + estado + "\n" +
                      "Cliente: " + lic.getNombre())
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(10))
                .showInformation();
        }
    }

    /**
     * Carga la vista principal/hub con los módulos
     */
    private void mostrarVistaPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/principal_home.fxml"));
            Parent vista = loader.load();

            // Obtener el controlador e inyectar referencia a AppController
            PrincipalViewController controller = loader.getController();
            controller.setAppController(this);

            // Aplicar transición suave
            Transiciones.cambiarVistaConFade(contenedorPrincipal, vista);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al cargar vista principal", e.getMessage());
        }
    }

    /** Método genérico: carga rápida de vistas simples con transición fade **/
    private void cargarVista(String rutaFXML) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent vista = loader.load();

            // Aplicar transición suave
            Transiciones.cambiarVistaConFade(contenedorPrincipal, vista);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al cargar vista", "No se pudo cargar la vista: " + rutaFXML);
        }
    }

    /**
     * Vuelve a la vista principal/hub (público para menú)
     */
    @FXML
    private void volverInicio() {
        mostrarVistaPrincipal();
    }

    /**
     * Métodos públicos para navegación desde PrincipalViewController
     */
    public void mostrarProductosPublic() {
        mostrarProductos();
    }

    public void mostrarVentasPublic() {
        mostrarVentas();
    }

    public void mostrarClientesPublic() {
        mostrarClientes();
    }

    public void mostrarMetricasPublic() {
        mostrarMetricas();
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
                FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/ProductoTree.fxml"));
                Parent vista = loader.load();
                productoController = loader.getController();
                return vista;
            }
        };

        tareaCarga.setOnSucceeded(e -> {
            vistaProductos = tareaCarga.getValue();
            VBox.setMargin(vistaProductos, new Insets(0));

            // Aplicar transición suave
            Transiciones.cambiarVistaConFade(contenedorPrincipal, vistaProductos);
        });

        tareaCarga.setOnFailed(e -> {
            Label error = new Label("❌ Error al cargar vista de productos.");
            contenedorPrincipal.getChildren().setAll(error);
            tareaCarga.getException().printStackTrace();
        });

        new Thread(tareaCarga).start();
    }

    @FXML
    public void verInventarioTree() {
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

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar (p###, nombre, color, talle)");

        TreeTableView<ItemInventario> tree = Arboles.crearTreeTabla(columnas);
        tree.setShowRoot(false);

        // Carga inicial
        try {
            tree.setRoot(InventarioDAO.cargarArbol(""));
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error cargando inventario", e.getMessage());
        }

        // Filtrar con reconsulta simple
        txtBuscar.textProperty().addListener((obs, oldV, newV) -> {
            try {
                tree.setRoot(InventarioDAO.cargarArbol(newV));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        VBox layout = new VBox(10, txtBuscar, tree);
        layout.setFillWidth(true);

        contenedorPrincipal.getChildren().setAll(layout);
    }

    /** 🔁 Restaurar inventario sin recargar toda la vista **/
    @FXML
    public void restaurarInventarioCompleto() {
        try {
            if (productoController != null) {
                productoController.restaurarInventarioCompleto();
                System.out.println("Inventario restaurado desde AppController (sin recargar FXML).");
            } else {
                mostrarProductos();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("❌ Error al restaurar inventario");
            contenedorPrincipal.getChildren().setAll(error);
        }
    }

    @FXML
    public void mostrarMetricas() {
        cargarVista("/fxml/metricas.fxml");
    }

    @FXML
    public void mostrarVentas() {
        cargarVista("/fxml/ventas.fxml");
    }

    @FXML
    public void mostrarClientes() {
        cargarVista("/fxml/clientes.fxml");
    }

    @FXML
    private void verInventarioTreeExperimental() {
        cargarVista("/fxml/ProductoTree.fxml");
    }

    // ═══════════════════════════════════════════════════════════
    // SISTEMA DE ACTUALIZACIONES
    // ═══════════════════════════════════════════════════════════

    /**
     * Maneja el clic en "Buscar actualizaciones" del menú
     */
    @FXML
    private void onBuscarActualizaciones(ActionEvent event) {
        Stage stage = getStage();

        if (stage == null) {
            System.err.println("⚠️ No se pudo obtener Stage, usando ventana dummy");
            stage = new Stage();
        }

        System.out.println("🔍 Buscando actualizaciones...");

        final Stage finalStage = stage;
        updateManager.checkForUpdatesAsync()
                .thenAccept(hasUpdate -> {
                    Platform.runLater(() -> {
                        if (hasUpdate) {
                            UpdateDialog.showUpdateAvailable(finalStage, updateManager);
                        } else {
                            UpdateDialog.showInfo(
                                    finalStage,
                                    "Sin actualizaciones",
                                    "Ya estás usando la última versión disponible.\n\n" +
                                            "Versión actual: " + updateManager.getCurrentVersion()
                            );
                        }
                    });
                })
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        UpdateDialog.showError(
                                finalStage,
                                "Error de conexión",
                                "No se pudo verificar actualizaciones.\n\n" +
                                        "Verifica tu conexión a Internet e intenta nuevamente.\n\n" +
                                        "Error: " + error.getMessage()
                        );
                    });
                    return null;
                });
    }

    /**
     * Muestra información "Acerca de"
     */
    @FXML
    private void onAcercaDe(ActionEvent event) {
        Stage stage = getStage();

        String mensaje = String.format(
                "App Inventario - Sistema de Gestión\n\n" +
                        "Versión: %s\n" +
                        "Desarrollado con: JavaFX 21 + PostgreSQL\n\n" +
                        "GitHub: github.com/%s/%s",
                updateManager.getCurrentVersion(),
                UpdateConfig.getGithubUser(),
                UpdateConfig.getRepoName()
        );

        UpdateDialog.showInfo(stage, "Acerca de App Inventario", mensaje);
    }

    /**
     * Abre página de issues en GitHub
     */
    @FXML
    private void onReportarProblema(ActionEvent event) {
        try {
            String url = String.format(
                    "https://github.com/%s/%s/issues/new",
                    UpdateConfig.getGithubUser(),
                    UpdateConfig.getRepoName()
            );

            java.awt.Desktop.getDesktop().browse(new URI(url));

        } catch (Exception e) {
            UpdateDialog.showError(
                    getStage(),
                    "Error",
                    "No se pudo abrir el navegador.\n\n" +
                            "Visita manualmente: github.com/" + UpdateConfig.getGithubUser() +
                            "/" + UpdateConfig.getRepoName() + "/issues"
            );
        }
    }

    /**
     * Cierra la sesión actual y vuelve a la pantalla de login
     */
    @FXML
    private void onCerrarSesion(ActionEvent event) {
        // Confirmar con el usuario
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar sesión");
        confirmacion.setHeaderText("¿Está seguro que desea cerrar sesión?");
        confirmacion.setContentText("Deberá iniciar sesión nuevamente para volver a usar la aplicación.");

        ButtonType btnSi = new ButtonType("Sí, cerrar sesión");
        ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(btnSi, btnNo);

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == btnSi) {
                cerrarSesion();
            }
        });
    }

    /**
     * Cierra la sesión y vuelve al login
     */
    private void cerrarSesion() {
        try {
            SessionManager session = SessionManager.getInstance();
            String nombreUsuario = session.getNombreUsuario();

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🚪 CERRANDO SESIÓN");
            System.out.println("   Usuario: " + nombreUsuario);

            // 1. Borrar sesión del disco
            SessionPersistence.borrarSesion();

            // 2. Limpiar SessionManager
            session.logout();

            System.out.println("✅ Sesión cerrada exitosamente");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // 3. Cargar ventana de login
            Stage stage = getStage();
            if (stage != null) {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
                Parent root = loader.load();

                Scene scene = new Scene(root, 500, 600);
                scene.getStylesheets().add(App.class.getResource("/Estilos/Estilos.css").toExternalForm());

                stage.setScene(scene);
                stage.setTitle("Ariel Cardales - Iniciar Sesión");
                stage.setResizable(false);
                stage.centerOnScreen();

                mostrarInfo("Sesión cerrada. Hasta pronto!");
            }

        } catch (Exception e) {
            System.err.println("❌ Error al cerrar sesión: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error", "No se pudo cerrar la sesión correctamente");
        }
    }

    /**
     * Obtiene el Stage actual desde el componente FXML
     */
    private Stage getStage() {
        try {
            // Opción 1: Desde el contenedor principal (más confiable)
            if (contenedorPrincipal != null && contenedorPrincipal.getScene() != null) {
                return (Stage) contenedorPrincipal.getScene().getWindow();
            }

            // Opción 2: Buscar en ventanas abiertas (fallback)
            return javafx.stage.Window.getWindows().stream()
                    .filter(javafx.stage.Window::isShowing)
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            System.err.println("⚠️ No se pudo obtener Stage: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // NOTIFICACIONES
    // ═══════════════════════════════════════════════════════════

    /**
     * Muestra notificación de éxito
     */
    private void mostrarExito(String mensaje) {
        Notifications.create()
                .title("✅ Éxito")
                .text(mensaje)
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(3))
                .showConfirm();
    }

    /**
     * Muestra notificación de error
     */
    private void mostrarError(String titulo, String mensaje) {
        Notifications.create()
                .title("❌ " + titulo)
                .text(mensaje)
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(5))
                .showError();
    }

    /**
     * Muestra notificación informativa
     */
    private void mostrarInfo(String mensaje) {
        Notifications.create()
                .title("ℹ️ Información")
                .text(mensaje)
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(4))
                .showInformation();
    }

    /**
     * Muestra notificación de advertencia
     */
    private void mostrarAdvertencia(String mensaje) {
        Notifications.create()
                .title("⚠️ Advertencia")
                .text(mensaje)
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(4))
                .showWarning();
    }

    // ═══════════════════════════════════════════════════════════
    // ADMINISTRACIÓN DE USUARIOS
    // ═══════════════════════════════════════════════════════════

    /**
     * Muestra diálogo para registrar un nuevo usuario/licencia
     * SOLO ACCESIBLE PARA PLAN DEV
     */
    @FXML
    private void mostrarRegistroUsuario() {
        // PROTECCIÓN: Verificar que el usuario tenga plan DEV
        SessionManager session = SessionManager.getInstance();
        Licencia licencia = session.getLicenciaSafe();

        if (licencia == null || licencia.getPlan() != Licencia.PlanLicencia.DEV) {
            mostrarError("Acceso Denegado",
                "Esta función solo está disponible para administradores del sistema.\n\n" +
                "Plan actual: " + (licencia != null ? licencia.getPlan() : "DESCONOCIDO") + "\n" +
                "Requerido: DEV");
            System.out.println("🚫 Intento de acceso a administración sin plan DEV");
            System.out.println("   Usuario: " + (licencia != null ? licencia.getNombre() : "Sin sesión"));
            System.out.println("   Plan: " + (licencia != null ? licencia.getPlan() : "null"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Administración - Registrar Nuevo Usuario");
        dialog.setHeaderText("Crear una nueva licencia/usuario en el sistema");

        // Botones
        ButtonType btnRegistrar = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnRegistrar, ButtonType.CANCEL);

        // Campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtClienteId = new TextField();
        txtClienteId.setPromptText("ej: tienda_juan, cliente001");

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del negocio/cliente");

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("email@ejemplo.com");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Contraseña (mín. 6 caracteres)");

        PasswordField txtPasswordConfirm = new PasswordField();
        txtPasswordConfirm.setPromptText("Confirmar contraseña");

        ComboBox<String> cbEstado = new ComboBox<>();
        cbEstado.getItems().addAll("ACTIVO", "SUSPENDIDO", "EXPIRADO", "VENCIDO");
        cbEstado.setValue("ACTIVA");

        ComboBox<String> cbPlan = new ComboBox<>();
        cbPlan.getItems().addAll("DEMO", "BASE", "FULL", "DEV");
        cbPlan.setValue("BASE");

        DatePicker dpExpiracion = new DatePicker();
        dpExpiracion.setValue(java.time.LocalDate.now().plusYears(1));

        TextArea txtNotas = new TextArea();
        txtNotas.setPromptText("Notas opcionales");
        txtNotas.setPrefRowCount(2);

        // Layout
        grid.add(new Label("*Cliente ID:"), 0, 0);
        grid.add(txtClienteId, 1, 0);
        grid.add(new Label("*Nombre:"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("*Email:"), 0, 2);
        grid.add(txtEmail, 1, 2);
        grid.add(new Label("*Contraseña:"), 0, 3);
        grid.add(txtPassword, 1, 3);
        grid.add(new Label("*Confirmar:"), 0, 4);
        grid.add(txtPasswordConfirm, 1, 4);
        grid.add(new Label("Estado:"), 0, 5);
        grid.add(cbEstado, 1, 5);
        grid.add(new Label("Plan:"), 0, 6);
        grid.add(cbPlan, 1, 6);
        grid.add(new Label("Fecha Expiración:"), 0, 7);
        grid.add(dpExpiracion, 1, 7);
        grid.add(new Label("Notas:"), 0, 8);
        grid.add(txtNotas, 1, 8);

        dialog.getDialogPane().setContent(grid);

        // Focus en primer campo
        Platform.runLater(() -> txtClienteId.requestFocus());

        // Procesar resultado
        dialog.showAndWait().ifPresent(response -> {
            if (response == btnRegistrar) {
                registrarNuevoUsuario(
                    txtClienteId.getText().trim(),
                    txtNombre.getText().trim(),
                    txtEmail.getText().trim(),
                    txtPassword.getText(),
                    txtPasswordConfirm.getText(),
                    cbEstado.getValue(),
                    cbPlan.getValue(),
                    dpExpiracion.getValue(),
                    txtNotas.getText().trim()
                );
            }
        });
    }

    /**
     * Registra un nuevo usuario en la base de datos
     */
    private void registrarNuevoUsuario(String clienteId, String nombre, String email,
                                       String password, String passwordConfirm,
                                       String estado, String plan,
                                       java.time.LocalDate fechaExpiracion, String notas) {
        // Validaciones
        if (clienteId.isEmpty() || nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            mostrarError("Campos requeridos", "Por favor complete todos los campos marcados con *");
            return;
        }

        if (!password.equals(passwordConfirm)) {
            mostrarError("Error de contraseña", "Las contraseñas no coinciden");
            return;
        }

        if (password.length() < 6) {
            mostrarError("Contraseña débil", "La contraseña debe tener al menos 6 caracteres");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            mostrarError("Email inválido", "Por favor ingrese un email válido");
            return;
        }

        // Validar cliente_id (solo letras, números, guiones bajos)
        if (!clienteId.matches("^[a-zA-Z0-9_]+$")) {
            mostrarError("Cliente ID inválido",
                "El Cliente ID solo puede contener letras, números y guiones bajos.\n" +
                "Ejemplos: tienda_juan, cliente001, negocio123");
            return;
        }

        // Registrar en background
        Task<Boolean> registroTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                com.arielcardales.arielcardales.DAO.AutenticacionDAO dao =
                    new com.arielcardales.arielcardales.DAO.AutenticacionDAO();

                return dao.registrar(
                    clienteId,
                    nombre,
                    email,
                    password,
                    estado,
                    plan,
                    fechaExpiracion,
                    notas.isEmpty() ? null : notas
                );
            }
        };

        registroTask.setOnSucceeded(event -> {
            if (registroTask.getValue()) {
                mostrarExito("Usuario registrado exitosamente!\n\n" +
                    "Cliente ID: " + clienteId + "\n" +
                    "Email: " + email + "\n" +
                    "Plan: " + plan + "\n" +
                    "Expira: " + fechaExpiracion);
            } else {
                mostrarError("Error de registro",
                    "No se pudo registrar el usuario.\n" +
                    "Posibles causas:\n" +
                    "• El email ya está registrado\n" +
                    "• El cliente_id ya existe");
            }
        });

        registroTask.setOnFailed(event -> {
            Throwable ex = registroTask.getException();
            mostrarError("Error al registrar",
                "Error: " + (ex != null ? ex.getMessage() : "Desconocido"));
            ex.printStackTrace();
        });

        new Thread(registroTask).start();
    }
}