package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.App;
import com.arielcardales.arielcardales.DAO.InventarioDAO;
import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Updates.UpdateConfig;
import com.arielcardales.arielcardales.Updates.UpdateDialog;
import com.arielcardales.arielcardales.Updates.UpdateManager;
import com.arielcardales.arielcardales.Util.Arboles;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;  // ✅ JavaFX ActionEvent
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.Stage;

import java.net.URI;
import java.sql.SQLException;

public class AppController {

    private ProductoTreeController productoController;

    @FXML
    private VBox contenedorPrincipal;

    private Parent vistaProductos;
    private UpdateManager updateManager;

    @FXML
    public void initialize() {
        // Cargar el dashboard al iniciar la app
        mostrarDashboard();

        // Inicializar update manager
        updateManager = new UpdateManager();
    }

    /** Método genérico: carga rápida de vistas simples (sin Task) **/
    private void cargarVista(String rutaFXML) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent vista = loader.load();
            contenedorPrincipal.getChildren().setAll(vista);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            contenedorPrincipal.getChildren().setAll(vistaProductos);
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
            new Alert(Alert.AlertType.ERROR, "Error cargando inventario: " + e.getMessage()).showAndWait();
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
    private void mostrarDashboard() {
        cargarVista("/fxml/dashboard.fxml");
    }

    @FXML
    private void mostrarVentas() {
        cargarVista("/fxml/ventas.fxml");
    }

    @FXML
    private void mostrarClientes() {
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
}