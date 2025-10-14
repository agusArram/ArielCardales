package com.arielcardales.arielcardales.Updates;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Aplicación de prueba para el sistema de actualizaciones
 *
 * IMPORTANTE: Cambiar CURRENT_VERSION en UpdateConfig.java a una versión anterior
 * (ej: v1.0.0) para simular que hay actualización disponible
 */
public class UpdateTester extends Application {

    private UpdateManager updateManager;
    private Label lblStatus;

    @Override
    public void start(Stage stage) {
        updateManager = new UpdateManager();

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        Label lblTitle = new Label("🧪 Probador de Sistema de Actualizaciones");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        lblStatus = new Label("Listo para probar");
        lblStatus.setStyle("-fx-font-size: 12px;");
        lblStatus.setWrapText(true);
        lblStatus.setMaxWidth(400);

        Label lblVersion = new Label("Versión actual: " + updateManager.getCurrentVersion());
        lblVersion.setStyle("-fx-font-style: italic;");

        // Botones de prueba
        Button btnCheck = createButton("1️⃣ Verificar actualizaciones");
        btnCheck.setOnAction(e -> testCheckForUpdates(stage));

        Button btnShow = createButton("2️⃣ Mostrar diálogo de actualización");
        btnShow.setOnAction(e -> testShowDialog(stage));

        Button btnDownload = createButton("3️⃣ Descargar actualización");
        btnDownload.setOnAction(e -> testDownload(stage));

        Button btnInstall = createButton("4️⃣ Descargar e Instalar");
        btnInstall.setOnAction(e -> testInstall(stage));

        Button btnConfig = createButton("⚙️ Ver configuración");
        btnConfig.setOnAction(e -> testConfig());

        Button btnClearSkip = createButton("🗑️ Limpiar versión omitida");
        btnClearSkip.setOnAction(e -> {
            updateManager.clearSkippedVersion();
            updateStatus("✅ Versión omitida limpiada");
        });

        root.getChildren().addAll(
                lblTitle,
                lblVersion,
                lblStatus,
                btnCheck,
                btnShow,
                btnDownload,
                btnInstall,
                btnConfig,
                btnClearSkip
        );

        Scene scene = new Scene(root, 500, 600);
        stage.setScene(scene);
        stage.setTitle("Update System Tester");
        stage.show();
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-padding: 10px;");
        return btn;
    }

    private void testCheckForUpdates(Stage stage) {
        updateStatus("🔍 Verificando actualizaciones en GitHub...");

        updateManager.checkForUpdatesAsync()
                .thenAccept(hasUpdate -> {
                    if (hasUpdate) {
                        UpdateChecker.ReleaseInfo release = updateManager.getLatestRelease();
                        updateStatus(String.format(
                                "✅ Actualización disponible!\n" +
                                        "Nueva versión: %s\n" +
                                        "Tamaño: %s\n" +
                                        "Publicado: %s",
                                release.getTagName(),
                                release.getFormattedFileSize(),
                                release.getFormattedPublishDate()
                        ));
                    } else {
                        updateStatus("✅ Ya estás usando la última versión");
                    }
                })
                .exceptionally(error -> {
                    updateStatus("❌ Error: " + error.getMessage());
                    return null;
                });
    }

    private void testShowDialog(Stage stage) {
        updateStatus("🔍 Verificando y mostrando diálogo...");

        updateManager.checkForUpdatesAsync()
                .thenAccept(hasUpdate -> {
                    if (hasUpdate) {
                        javafx.application.Platform.runLater(() -> {
                            UpdateDialog.showUpdateAvailable(stage, updateManager);
                            updateStatus("✅ Diálogo mostrado");
                        });
                    } else {
                        updateStatus("⚠️ No hay actualizaciones para mostrar");
                    }
                })
                .exceptionally(error -> {
                    updateStatus("❌ Error: " + error.getMessage());
                    return null;
                });
    }

    private void testDownload(Stage stage) {
        updateStatus("🔍 Iniciando prueba de descarga...");

        updateManager.checkForUpdatesAsync()
                .thenCompose(hasUpdate -> {
                    if (!hasUpdate) {
                        updateStatus("⚠️ No hay actualizaciones disponibles");
                        return java.util.concurrent.CompletableFuture.completedFuture(false);
                    }

                    updateStatus("📥 Descargando actualización...");

                    return updateManager.downloadUpdateAsync(progress -> {
                        javafx.application.Platform.runLater(() -> {
                            updateStatus(String.format(
                                    "[%s] %d%% - %s",
                                    progress.getPhase().getDisplayName(),
                                    progress.getPercentage(),
                                    progress.getMessage()
                            ));
                        });
                    });
                })
                .thenAccept(success -> {
                    if (success) {
                        updateStatus("✅ Descarga completada exitosamente!");
                    } else {
                        updateStatus("❌ Falló la descarga");
                    }
                })
                .exceptionally(error -> {
                    updateStatus("❌ Error: " + error.getMessage());
                    return null;
                });
    }

    private void testInstall(Stage stage) {
        updateStatus("🚀 Iniciando proceso completo (sin reinicio automático)...");

        updateManager.checkForUpdatesAsync()
                .thenCompose(hasUpdate -> {
                    if (!hasUpdate) {
                        updateStatus("⚠️ No hay actualizaciones disponibles");
                        return java.util.concurrent.CompletableFuture.completedFuture(false);
                    }

                    // Mostrar diálogo de progreso
                    javafx.application.Platform.runLater(() -> {
                        UpdateDialog.showUpdateProgress(stage, updateManager, false);
                    });

                    return java.util.concurrent.CompletableFuture.completedFuture(true);
                })
                .exceptionally(error -> {
                    updateStatus("❌ Error: " + error.getMessage());
                    return null;
                });
    }

    private void testConfig() {
        UpdateConfig config = updateManager.getConfig();

        String info = String.format(
                "⚙️ CONFIGURACIÓN ACTUAL:\n\n" +
                        "Auto-check: %s\n" +
                        "Intervalo: %d horas\n" +
                        "Auto-download: %s\n" +
                        "Mantener backups: %s\n" +
                        "Max backups: %d\n" +
                        "Última verificación: %s\n" +
                        "Versión omitida: %s\n\n" +
                        "Directorio backups: %s\n" +
                        "Directorio descargas: %s",
                config.isAutoCheckEnabled() ? "Sí" : "No",
                config.getCheckIntervalHours(),
                config.isAutoDownloadEnabled() ? "Sí" : "No",
                config.isKeepBackupsEnabled() ? "Sí" : "No",
                config.getMaxBackups(),
                config.getLastCheckTimestamp() > 0 ?
                        new java.util.Date(config.getLastCheckTimestamp()).toString() : "Nunca",
                config.getSkippedVersion().isEmpty() ? "Ninguna" : config.getSkippedVersion(),
                UpdateConfig.getBackupDir(),
                UpdateConfig.getDownloadDir()
        );

        updateStatus(info);
    }

    private void updateStatus(String message) {
        javafx.application.Platform.runLater(() -> {
            lblStatus.setText(message);
            System.out.println(message);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void testVersionComparison() {
        UpdateChecker checker = new UpdateChecker(new UpdateConfig());

        // Test cases
        System.out.println("\n🧪 Testing version comparison:");

        // Debería ser true (2.1.0 > 2.0.0)
        boolean test1 = testVersions("v2.0.0", "v2.1.0");
        System.out.println("v2.0.0 < v2.1.0: " + test1 + " " + (test1 ? "✅" : "❌"));

        // Debería ser false (2.1.0 == 2.1.0)
        boolean test2 = testVersions("v2.1.0", "v2.1.0");
        System.out.println("v2.1.0 < v2.1.0: " + test2 + " " + (!test2 ? "✅" : "❌"));

        // Debería ser true (2.0.9 < 2.1.0)
        boolean test3 = testVersions("v2.0.9", "v2.1.0");
        System.out.println("v2.0.9 < v2.1.0: " + test3 + " " + (test3 ? "✅" : "❌"));
    }

    // Método helper (copia el código de UpdateChecker)
    private boolean testVersions(String current, String latest) {
        // Copia exacta del método isNewerVersion de UpdateChecker
        String currentClean = current.toLowerCase().replace("v", "");
        String latestClean = latest.toLowerCase().replace("v", "");

        if (currentClean.equals(latestClean)) {
            return false;
        }

        String[] currentParts = currentClean.split("\\.");
        String[] latestParts = latestClean.split("\\.");

        int maxLength = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentPart = i < currentParts.length ?
                    Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ?
                    Integer.parseInt(latestParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }

        return false;
    }
}