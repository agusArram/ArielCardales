package com.arielcardales.arielcardales;

import com.arielcardales.arielcardales.Licencia.Licencia;
import com.arielcardales.arielcardales.Updates.UpdateDialog;
import com.arielcardales.arielcardales.Updates.UpdateManager;
import com.arielcardales.arielcardales.session.LicenseMonitor;
import com.arielcardales.arielcardales.session.SessionManager;
import com.arielcardales.arielcardales.session.SessionPersistence;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Optional;

public class App extends Application {

    private UpdateManager updateManager;

    @Override
    public void start(Stage stage) throws Exception {
        // 🔹 Registrar las fuentes manualmente (rápido - ~50ms)
        Font.loadFont(getClass().getResourceAsStream("/Fuentes/static/Lora-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/Fuentes/static/Lora-Bold.ttf"), 14);

        // 🚀 Mostrar pantalla de carga inmediatamente
        mostrarPantallaCarga(stage);

        // ⚡ Validar sesión en background (no bloquea la UI)
        javafx.concurrent.Task<Optional<Licencia>> validarTask = new javafx.concurrent.Task<>() {
            @Override
            protected Optional<Licencia> call() {
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("🔐 VERIFICANDO SESIÓN PERSISTENTE (async)");
                return SessionPersistence.cargarSesion();
            }
        };

        validarTask.setOnSucceeded(event -> {
            Optional<Licencia> sesionGuardada = validarTask.getValue();

            if (sesionGuardada.isPresent()) {
                // Hay sesión guardada válida - cargar app directamente
                Licencia licencia = sesionGuardada.get();
                SessionManager.getInstance().login(licencia);

                System.out.println("✅ Sesión restaurada - cargando aplicación");
                System.out.println("   Usuario: " + licencia.getNombre());
                System.out.println("   Plan: " + licencia.getPlan());
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                try {
                    cargarVentanaPrincipal(stage);
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarErrorYCerrar(stage, "Error al cargar la aplicación");
                }

            } else {
                // No hay sesión - mostrar login
                System.out.println("ℹ️ No hay sesión válida - mostrando login");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                try {
                    cargarVentanaLogin(stage);
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarErrorYCerrar(stage, "Error al cargar el login");
                }
            }
        });

        validarTask.setOnFailed(event -> {
            System.err.println("❌ Error validando sesión: " + validarTask.getException().getMessage());
            validarTask.getException().printStackTrace();

            // Mostrar login en caso de error
            try {
                cargarVentanaLogin(stage);
            } catch (Exception e) {
                e.printStackTrace();
                mostrarErrorYCerrar(stage, "Error crítico al iniciar");
            }
        });

        // Ejecutar validación en thread separado
        new Thread(validarTask).start();
    }

    /**
     * Carga la ventana de login
     */
    private void cargarVentanaLogin(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 500, 600);
        scene.getStylesheets().add(App.class.getResource("/Estilos/Estilos.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Ariel Cardales - Iniciar Sesión");
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Carga la ventana principal de la aplicación
     */
    private void cargarVentanaPrincipal(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/principal.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 1200, 800); // Tamaño inicial: 1200x800
        scene.getStylesheets().add(App.class.getResource("/Estilos/Estilos.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Ariel Cardales - " + SessionManager.getInstance().getNombreUsuario());
        stage.setResizable(true); // Permitir que el usuario redimensione
        stage.centerOnScreen(); // Centrar en pantalla
        stage.show();

        // 🔒 Iniciar monitor de licencias en background
        LicenseMonitor.getInstance().iniciar();
    }

    /**
     * Inicializa y configura el sistema de actualizaciones
     */
    private void initUpdateSystem(Stage stage) {
        updateManager = new UpdateManager();

        // Verificar actualizaciones en segundo plano
        // Esperar 3 segundos para que la UI esté completamente cargada
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                checkForUpdates(stage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Verifica si hay actualizaciones disponibles
     */
    private void checkForUpdates(Stage stage) {
        // Solo verificar si pasó el intervalo configurado
        if (!updateManager.shouldCheckForUpdates()) {
            System.out.println("⏭️ No es momento de verificar actualizaciones");
            return;
        }

        System.out.println("🔍 Verificando actualizaciones en GitHub...");

        updateManager.checkForUpdatesAsync()
                .thenAccept(hasUpdate -> {
                    if (hasUpdate) {
                        System.out.println("✅ Nueva versión disponible: " +
                                updateManager.getLatestRelease().getTagName());

                        // Mostrar diálogo en UI thread
                        Platform.runLater(() ->
                                UpdateDialog.showUpdateAvailable(stage, updateManager)
                        );
                    } else {
                        System.out.println("✅ La aplicación está actualizada (v" +
                                updateManager.getCurrentVersion() + ")");
                    }
                })
                .exceptionally(error -> {
                    System.err.println("❌ Error al verificar actualizaciones: " +
                            error.getMessage());
                    return null;
                });
    }

    /**
     * Menú manual para verificar actualizaciones
     * (Llamar desde tu AppController.java cuando el usuario hace clic en "Buscar actualizaciones")
     */
    public void manualUpdateCheck(Stage stage) {
        System.out.println("🔍 Verificación manual de actualizaciones...");

        updateManager.checkForUpdatesAsync()
                .thenAccept(hasUpdate -> {
                    if (hasUpdate) {
                        Platform.runLater(() ->
                                UpdateDialog.showUpdateAvailable(stage, updateManager)
                        );
                    } else {
                        Platform.runLater(() ->
                                UpdateDialog.showInfo(
                                        stage,
                                        "Sin actualizaciones",
                                        "Ya estás usando la última versión (" +
                                                updateManager.getCurrentVersion() + ")"
                                )
                        );
                    }
                })
                .exceptionally(error -> {
                    Platform.runLater(() ->
                            UpdateDialog.showError(
                                    stage,
                                    "Error de conexión",
                                    "No se pudo verificar actualizaciones. Verifica tu conexión a Internet."
                            )
                    );
                    return null;
                });
    }

    /**
     * Muestra la pantalla de carga mientras se valida la sesión
     */
    private void mostrarPantallaCarga(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/loading.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 500, 400);
        scene.getStylesheets().add(App.class.getResource("/Estilos/Estilos.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Ariel Cardales - Cargando...");
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Muestra un error y cierra la aplicación
     */
    private void mostrarErrorYCerrar(Stage stage, String mensaje) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Error");
        alert.setHeaderText("Error al iniciar la aplicación");
        alert.setContentText(mensaje);
        alert.showAndWait();
        Platform.exit();
    }

    @Override
    public void stop() throws Exception {
        // Detener monitor de licencias
        LicenseMonitor.getInstance().detener();

        super.stop();
        // Cleanup si es necesario
    }

    public static void main(String[] args) {
        launch();
    }
}
