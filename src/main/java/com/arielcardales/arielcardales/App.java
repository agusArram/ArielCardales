package com.arielcardales.arielcardales;

import com.arielcardales.arielcardales.Updates.UpdateDialog;
import com.arielcardales.arielcardales.Updates.UpdateManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class App extends Application {

    private UpdateManager updateManager;

    @Override
    public void start(Stage stage) throws Exception {
        // 🔹 Registrar las fuentes manualmente
        Font.loadFont(getClass().getResourceAsStream("/Fuentes/static/Lora-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/Fuentes/static/Lora-Bold.ttf"), 14);

        // 🔹 Cargar pantalla de LOGIN (multi-tenant)
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 500, 600); // Ventana de login más pequeña

        // 🔹 Aplicar tu CSS
        scene.getStylesheets().add(App.class.getResource("/Estilos/Estilos.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Ariel Cardales - Iniciar Sesión");
        stage.setResizable(false); // Ventana de login no redimensionable
        stage.centerOnScreen();
        stage.show();

        // ⭐ Sistema de actualizaciones se inicializa después del login
        // (Ver AppController.initialize())
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

    @Override
    public void stop() throws Exception {
        super.stop();
        // Cleanup si es necesario
    }

    public static void main(String[] args) {
        launch();
    }
}
