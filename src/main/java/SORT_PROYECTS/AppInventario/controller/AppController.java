package SORT_PROYECTS.AppInventario.controller;

import SORT_PROYECTS.AppInventario.DAO.AutenticacionDAO;
import SORT_PROYECTS.AppInventario.App;
import SORT_PROYECTS.AppInventario.DAO.InventarioDAO;
import SORT_PROYECTS.AppInventario.Entidades.ItemInventario;
import SORT_PROYECTS.AppInventario.Licencia.Licencia;
import SORT_PROYECTS.AppInventario.session.SessionManager;
import SORT_PROYECTS.AppInventario.session.SessionPersistence;
import SORT_PROYECTS.AppInventario.Updates.UpdateConfig;
import SORT_PROYECTS.AppInventario.Updates.UpdateDialog;
import SORT_PROYECTS.AppInventario.Updates.UpdateManager;
import SORT_PROYECTS.AppInventario.Updates.SyncDialog;
import SORT_PROYECTS.AppInventario.service.sync.SyncService;
import SORT_PROYECTS.AppInventario.service.sync.SyncResult;
// --- IMPORT NUEVO ---
import SORT_PROYECTS.AppInventario.Util.Permisos;
import SORT_PROYECTS.AppInventario.Util.Arboles;
import SORT_PROYECTS.AppInventario.Util.Transiciones;
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
import javafx.scene.layout.StackPane;
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

    @FXML
    private Label labelEstadoConexion;

    // --- NUEVO FXML (Ejemplo) ---
    // Debes agregar estos @FXML si quieres controlar más menús
    @FXML
    private MenuItem menuSincronizarBackup;

    @FXML
    private Menu menuExportar; // Suponiendo que tienes un menú "Exportar"

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

        // 2. Configurar visibilidad de menús según el plan
        // ESTA FUNCIÓN AHORA HACE MÁS QUE SOLO EL MENÚ ADMIN
        configurarPermisosUI();

        // 3. Cargar la vista principal/hub
        mostrarVistaPrincipal();

        // 4. Inicializar update manager
        updateManager = new UpdateManager();

        // 5. Inicializar indicador de estado de conexión
        inicializarIndicadorConexion();

        // 6. Mostrar info de licencia después de cargar
        Platform.runLater(this::mostrarInfoLicencia);
    }

    /**
     * Configura la visibilidad de todos los elementos de la UI
     * controlados por permisos (Admin, Exportar, etc.)
     */
    private void configurarPermisosUI() {
        SessionManager session = SessionManager.getInstance();

        // --- 1. CONFIGURAR MENÚ ADMINISTRACIÓN ---
        // Usamos el helper centralizado en lugar de lógica local
        boolean tieneAccesoAdmin = session.canAccess(Permisos.ADMIN_MENU);

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔐 CONFIGURANDO PERMISOS DE UI");
        System.out.println("   Plan actual: " + session.getPlan());
        System.out.println("   ¿Tiene acceso admin?: " + (tieneAccesoAdmin ? "SÍ" : "NO"));

        if (menuAdministracion != null) {
            menuAdministracion.setVisible(tieneAccesoAdmin);
            menuAdministracion.setDisable(!tieneAccesoAdmin);
        } else {
            System.out.println("   ⚠️ ERROR: menuAdministracion es NULL (problema de fx:id)");
        }

        // --- 2. CONFIGURAR OTROS PERMISOS (EJEMPLO) ---
        // (Descomenta esto si tienes un @FXML para un menú "Exportar")
        /*
        boolean puedeExportar = session.canAccess(Permisos.EXPORTAR_PDF) || session.canAccess(Permisos.EXPORTAR_EXCEL);
        System.out.println("   ¿Puede exportar?: " + (puedeExportar ? "SÍ" : "NO"));
        if (menuExportar != null) {
            menuExportar.setVisible(puedeExportar);
            menuExportar.setDisable(!puedeExportar);
        }
        */

        // (Descomenta si tienes @FXML para métricas avanzadas)
        /*
        boolean puedeVerMetricas = session.canAccess(Permisos.METRICAS_AVANZADAS);
        System.out.println("   ¿Puede ver métricas avanzadas?: " + (puedeVerMetricas ? "SÍ" : "NO"));
        // ... (lógica para deshabilitar el botón/menú de métricas) ...
        */

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Configura la visibilidad del menú de administración según el plan de licencia
     * SOLO ACCESIBLE PARA PLAN DEV
     * * @deprecated Reemplazado por {@link #configurarPermisosUI()}
     */
    private void configurarMenuAdministracion() {
        // Esta lógica ahora está dentro de configurarPermisosUI()
        configurarPermisosUI();
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

            // --- VALIDACIÓN DE PERMISOS (EJEMPLO) ---
            // Protegemos la vista de métricas
            if (rutaFXML.contains("metricas.fxml")) {
                if (!SessionManager.getInstance().canAccess(Permisos.METRICAS_AVANZADAS)) {
                    mostrarError("Acceso Denegado", "Su plan actual no incluye métricas avanzadas.");
                    return; // No carga la vista
                }
            }

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
        // Ahora, esta llamada está protegida por la lógica dentro de cargarVista()
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
     * Sincroniza datos con el backup local (Supabase → SQLite)
     */
    @FXML
    private void onSincronizarBackup(ActionEvent event) {
        Stage stage = getStage();

        if (stage == null) {
            System.err.println("⚠️ No se pudo obtener Stage para sincronización");
            mostrarError("Error", "No se pudo iniciar la sincronización");
            return;
        }

        // 1. Mostrar confirmación SIEMPRE
        boolean confirmado = SyncDialog.showSyncConfirmation(stage);
        if (!confirmado) {
            System.out.println("ℹ️ Sincronización cancelada por el usuario");
            return;
        }

        // 2. Mostrar diálogo de progreso
        Stage progressDialog = SyncDialog.showSyncProgress(stage);

        // 3. Ejecutar sincronización en background
        Task<SyncResult> syncTask = new Task<>() {
            @Override
            protected SyncResult call() {
                System.out.println("🔄 Iniciando sincronización desde UI...");
                SyncService syncService = new SyncService();
                return syncService.syncFromCloud();
            }
        };

        syncTask.setOnSucceeded(e -> {
            // Cerrar diálogo de progreso
            Platform.runLater(progressDialog::close);

            // Obtener resultado
            SyncResult result = syncTask.getValue();

            // Mostrar resultados
            SyncDialog.showSyncResults(stage, result);

            // Mensaje en consola
            if (result.isSuccess()) {
                System.out.println("✅ Sincronización completada desde UI");
                mostrarExito("Backup local sincronizado exitosamente");
            } else {
                System.err.println("❌ Sincronización falló: " + result.getMessage());
            }
        });

        syncTask.setOnFailed(e -> {
            // Cerrar diálogo de progreso
            Platform.runLater(progressDialog::close);

            // Mostrar error
            Throwable ex = syncTask.getException();
            String errorMsg = ex != null ? ex.getMessage() : "Error desconocido";

            System.err.println("❌ Error en sincronización: " + errorMsg);
            ex.printStackTrace();

            SyncDialog.showError(
                    stage,
                    "Error de Sincronización",
                    "No se pudo completar la sincronización.\n\n" +
                            "Error: " + errorMsg + "\n\n" +
                            "Verifique su conexión a Internet e intente nuevamente."
            );
        });

        // Iniciar tarea en background
        new Thread(syncTask).start();
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
        // --- LÓGICA DE PERMISOS ACTUALIZADA ---
        SessionManager session = SessionManager.getInstance();

        if (!session.canAccess(Permisos.ADMIN_MENU)) {
            mostrarError("Acceso Denegado",
                    "Esta función solo está disponible para administradores del sistema.\n\n" +
                            "Plan actual: " + session.getPlan() + "\n" +
                            "Requerido: DEV");
            System.out.println("🚫 Intento de acceso a administración sin plan DEV");
            System.out.println("   Usuario: " + session.getNombreUsuario());
            System.out.println("   Plan: " + session.getPlan());
            return;
        }

        // El resto de tu código de diálogo va aquí...
        // (Es largo, así que lo omito, pero está perfecto como lo tenías)
        // ...

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
        txtPassword.setPromptText("Mín. 6 caracteres, al menos 1 letra y 1 número");
        TextField txtPasswordVisible = new TextField();
        txtPasswordVisible.setPromptText("Mín. 6 caracteres, al menos 1 letra y 1 número");
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        PasswordField txtPasswordConfirm = new PasswordField();
        txtPasswordConfirm.setPromptText("Confirmar contraseña");
        TextField txtPasswordConfirmVisible = new TextField();
        txtPasswordConfirmVisible.setPromptText("Confirmar contraseña");
        txtPasswordConfirmVisible.setVisible(false);
        txtPasswordConfirmVisible.setManaged(false);

        // Label para mostrar errores de validación
        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-wrap-text: true;");
        lblError.setMaxWidth(400);
        lblError.setVisible(false);

        // Sincronizar campos de contraseña
        txtPassword.textProperty().addListener((obs, old, newVal) -> {
            if (!txtPasswordVisible.getText().equals(newVal)) {
                txtPasswordVisible.setText(newVal);
            }
        });
        txtPasswordVisible.textProperty().addListener((obs, old, newVal) -> {
            if (!txtPassword.getText().equals(newVal)) {
                txtPassword.setText(newVal);
            }
        });

        txtPasswordConfirm.textProperty().addListener((obs, old, newVal) -> {
            if (!txtPasswordConfirmVisible.getText().equals(newVal)) {
                txtPasswordConfirmVisible.setText(newVal);
            }
        });
        txtPasswordConfirmVisible.textProperty().addListener((obs, old, newVal) -> {
            if (!txtPasswordConfirm.getText().equals(newVal)) {
                txtPasswordConfirm.setText(newVal);
            }
        });

        // Crear StackPanes con botones de toggle
        StackPane stackPassword = new StackPane();
        Button btnTogglePassword = new Button("👁");
        btnTogglePassword.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 5;");
        StackPane.setAlignment(btnTogglePassword, javafx.geometry.Pos.CENTER_RIGHT);
        StackPane.setMargin(btnTogglePassword, new Insets(0, 5, 0, 0));
        stackPassword.getChildren().addAll(txtPassword, txtPasswordVisible, btnTogglePassword);

        StackPane stackPasswordConfirm = new StackPane();
        Button btnTogglePasswordConfirm = new Button("👁");
        btnTogglePasswordConfirm.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 5;");
        StackPane.setAlignment(btnTogglePasswordConfirm, javafx.geometry.Pos.CENTER_RIGHT);
        StackPane.setMargin(btnTogglePasswordConfirm, new Insets(0, 5, 0, 0));
        stackPasswordConfirm.getChildren().addAll(txtPasswordConfirm, txtPasswordConfirmVisible, btnTogglePasswordConfirm);

        // Handlers para los botones de toggle
        final boolean[] passwordVisible = {false};
        btnTogglePassword.setOnAction(e -> {
            passwordVisible[0] = !passwordVisible[0];
            if (passwordVisible[0]) {
                txtPasswordVisible.setVisible(true);
                txtPasswordVisible.setManaged(true);
                txtPassword.setVisible(false);
                txtPassword.setManaged(false);
                btnTogglePassword.setText("🙈");
                txtPasswordVisible.requestFocus();
                txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
            } else {
                txtPassword.setVisible(true);
                txtPassword.setManaged(true);
                txtPasswordVisible.setVisible(false);
                txtPasswordVisible.setManaged(false);
                btnTogglePassword.setText("👁");
                txtPassword.requestFocus();
                txtPassword.positionCaret(txtPassword.getText().length());
            }
        });

        final boolean[] passwordConfirmVisible = {false};
        btnTogglePasswordConfirm.setOnAction(e -> {
            passwordConfirmVisible[0] = !passwordConfirmVisible[0];
            if (passwordConfirmVisible[0]) {
                txtPasswordConfirmVisible.setVisible(true);
                txtPasswordConfirmVisible.setManaged(true);
                txtPasswordConfirm.setVisible(false);
                txtPasswordConfirm.setManaged(false);
                btnTogglePasswordConfirm.setText("🙈");
                txtPasswordConfirmVisible.requestFocus();
                txtPasswordConfirmVisible.positionCaret(txtPasswordConfirmVisible.getText().length());
            } else {
                txtPasswordConfirm.setVisible(true);
                txtPasswordConfirm.setManaged(true);
                txtPasswordConfirmVisible.setVisible(false);
                txtPasswordConfirmVisible.setManaged(false);
                btnTogglePasswordConfirm.setText("👁");
                txtPasswordConfirm.requestFocus();
                txtPasswordConfirm.positionCaret(txtPasswordConfirm.getText().length());
            }
        });

        ComboBox<String> cbEstado = new ComboBox<>();
        cbEstado.getItems().addAll("ACTIVO", "SUSPENDIDO", "EXPIRADO", "VENCIDO");
        cbEstado.setValue("ACTIVO"); // Corregido de "ACTIVA"

        ComboBox<String> cbPlan = new ComboBox<>();
        cbPlan.getItems().addAll("DEMO", "BASE", "FULL", "DEV");
        cbPlan.setValue("BASE");

        DatePicker dpExpiracion = new DatePicker();
        dpExpiracion.setValue(java.time.LocalDate.now().plusYears(1));

        TextArea txtNotas = new TextArea();
        txtNotas.setPromptText("Notas opcionales");
        txtNotas.setPrefRowCount(2);

        // Layout
        grid.add(lblError, 0, 0, 2, 1); // Error label spans 2 columns
        grid.add(new Label("*Cliente ID:"), 0, 1);
        grid.add(txtClienteId, 1, 1);
        grid.add(new Label("*Nombre:"), 0, 2);
        grid.add(txtNombre, 1, 2);
        grid.add(new Label("*Email:"), 0, 3);
        grid.add(txtEmail, 1, 3);
        grid.add(new Label("*Contraseña:"), 0, 4);
        grid.add(stackPassword, 1, 4);
        grid.add(new Label("*Confirmar:"), 0, 5);
        grid.add(stackPasswordConfirm, 1, 5);
        grid.add(new Label("Estado:"), 0, 6);
        grid.add(cbEstado, 1, 6);
        grid.add(new Label("Plan:"), 0, 7);
        grid.add(cbPlan, 1, 7);
        grid.add(new Label("Fecha Expiración:"), 0, 8);
        grid.add(dpExpiracion, 1, 8);
        grid.add(new Label("Notas:"), 0, 9);
        grid.add(txtNotas, 1, 9);

        dialog.getDialogPane().setContent(grid);

        // Focus en primer campo
        Platform.runLater(() -> txtClienteId.requestFocus());

        // VALIDACIÓN ANTES DE CERRAR EL DIÁLOGO
        final javafx.scene.control.Button registrarButton =
                (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btnRegistrar);

        registrarButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Obtener valores
            String clienteId = txtClienteId.getText().trim();
            String nombre = txtNombre.getText().trim();
            String email = txtEmail.getText().trim();
            String password = txtPassword.getText();
            String passwordConfirm = txtPasswordConfirm.getText();

            // Validar campos requeridos
            if (clienteId.isEmpty() || nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
                lblError.setText("Por favor complete todos los campos marcados con *");
                lblError.setVisible(true);
                event.consume(); // Evitar que se cierre el diálogo
                return;
            }

            // Validar cliente_id (solo letras, números, guiones bajos)
            if (!clienteId.matches("^[a-zA-Z0-9_]+$")) {
                lblError.setText("Cliente ID inválido. Solo letras, números y guiones bajos.\nEjemplos: tienda_juan, cliente001");
                lblError.setVisible(true);
                event.consume();
                return;
            }

            // Validar email
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                lblError.setText("Email inválido. Ingrese un email válido");
                lblError.setVisible(true);
                event.consume();
                return;
            }

            // Validar contraseñas coincidan
            if (!password.equals(passwordConfirm)) {
                lblError.setText("Las contraseñas no coinciden");
                lblError.setVisible(true);
                event.consume();
                return;
            }

            // Validar contraseña con PasswordUtil (incluye: mín 6 chars, letra, número)
            if (!SORT_PROYECTS.AppInventario.Util.PasswordUtil.validarPassword(password)) {
                String error = SORT_PROYECTS.AppInventario.Util.PasswordUtil.getPasswordError(password);
                lblError.setText("Contraseña inválida:\n" + error);
                lblError.setVisible(true);
                event.consume();
                return;
            }

            // Si pasa todas las validaciones, ocultar el error y permitir cerrar
            lblError.setVisible(false);
        });

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
     * NOTA: Las validaciones se realizan en el diálogo antes de llamar este método
     */
    private void registrarNuevoUsuario(String clienteId, String nombre, String email,
                                       String password, String passwordConfirm,
                                       String estado, String plan,
                                       java.time.LocalDate fechaExpiracion, String notas) {

        // Registrar en background
        Task<Boolean> registroTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                AutenticacionDAO dao = new AutenticacionDAO();

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

    // ========================================
    // INDICADOR DE ESTADO DE CONEXIÓN
    // ========================================

    /**
     * Inicializa el indicador de estado de conexión
     * Configura un timer que actualiza el estado cada 30 segundos
     */
    private void inicializarIndicadorConexion() {
        // Actualizar estado inicial
        actualizarEstadoConexion();

        // Timer para actualizar cada 30 segundos
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(30),
                        event -> actualizarEstadoConexion()
                )
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Actualiza el indicador visual de estado de conexión
     */
    private void actualizarEstadoConexion() {
        if (labelEstadoConexion == null) {
            return;
        }

        Platform.runLater(() -> {
            String mensaje = SORT_PROYECTS.AppInventario.DAO.Database.getStatusMessageSafe();
            labelEstadoConexion.setText(mensaje);

            // Cambiar estilo según estado
            if (SORT_PROYECTS.AppInventario.DAO.Database.isOnline()) {
                labelEstadoConexion.setStyle(
                        "-fx-background-color: rgba(0, 128, 0, 0.3); " +
                                "-fx-text-fill: #90EE90; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 8 15; " +
                                "-fx-background-radius: 5; " +
                                "-fx-cursor: hand;"
                );
            } else {
                labelEstadoConexion.setStyle(
                        "-fx-background-color: rgba(255, 0, 0, 0.3); " +
                                "-fx-text-fill: #FFB6C1; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 8 15; " +
                                "-fx-background-radius: 5; " +
                                "-fx-cursor: hand;"
                );
            }
        });
    }

    /**
     * Maneja el click en el indicador de estado
     * Muestra un diálogo con información detallada y opciones
     */
    @FXML
    private void onClickEstadoConexion() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Estado de Conexión");
        alert.setHeaderText(SORT_PROYECTS.AppInventario.DAO.Database.getStatusMessage());

        String contenido;
        if (SORT_PROYECTS.AppInventario.DAO.Database.isOnline()) {
            contenido = """
                ✓ Conectado a Supabase (PostgreSQL)

                Todas las operaciones se guardan en la nube automáticamente.
                El backup local se actualiza al sincronizar desde el menú Ayuda.
                """;
        } else {
            contenido = """
                ⚠ Sin conexión a Supabase

                Usando backup local (SQLite) como respaldo.
                Los cambios se guardan localmente.

                Cuando recuperes la conexión, sincroniza desde:
                Menú Ayuda → Sincronizar Backup Local
                """;

            // Agregar botón "Intentar Reconectar"
            ButtonType btnReconectar = new ButtonType("Intentar Reconectar");
            alert.getButtonTypes().add(0, btnReconectar);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnReconectar) {
                    intentarReconexion();
                }
            });
            return;
        }

        alert.setContentText(contenido);
        alert.showAndWait();
    }

    /**
     * Intenta reconectar a Supabase
     */
    private void intentarReconexion() {
        System.out.println("🔄 Intentando reconectar a Supabase...");

        Task<Boolean> reconectTask = new Task<>() {
            @Override
            protected Boolean call() {
                return SORT_PROYECTS.AppInventario.DAO.Database.tryReconnect();
            }
        };

        reconectTask.setOnSucceeded(event -> {
            boolean exito = reconectTask.getValue();
            actualizarEstadoConexion();

            if (exito) {
                Notifications.create()
                        .title("Reconexión Exitosa")
                        .text("Se restableció la conexión a Supabase")
                        .showInformation();
            } else {
                Notifications.create()
                        .title("Reconexión Fallida")
                        .text("No se pudo conectar a Supabase. Seguirás trabajando offline.")
                        .showWarning();
            }
        });

        reconectTask.setOnFailed(event -> {
            actualizarEstadoConexion();
            Notifications.create()
                    .title("Error de Reconexión")
                    .text("Error al intentar reconectar")
                    .showError();
        });

        new Thread(reconectTask).start();
    }
}
