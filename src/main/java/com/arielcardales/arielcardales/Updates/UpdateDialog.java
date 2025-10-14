package com.arielcardales.arielcardales.Updates;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Diálogos JavaFX para el sistema de actualización
 */
public class UpdateDialog {

    /**
     * Muestra notificación de actualización disponible
     */
    public static void showUpdateAvailable(Stage owner, UpdateManager manager) {
        Platform.runLater(() -> {
            UpdateChecker.ReleaseInfo release = manager.getLatestRelease();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(owner);
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle("Actualización disponible");
            dialog.setResizable(false);

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");

            // Título
            Label lblTitle = new Label("🎉 Nueva versión disponible");
            lblTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

            // Info de versiones
            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(8);

            grid.add(createLabel("Versión actual:", true), 0, 0);
            grid.add(createLabel(manager.getCurrentVersion(), false), 1, 0);

            grid.add(createLabel("Nueva versión:", true), 0, 1);
            grid.add(createLabel(release.getTagName(), false), 1, 1);

            grid.add(createLabel("Tamaño:", true), 0, 2);
            grid.add(createLabel(release.getFormattedFileSize(), false), 1, 2);

            grid.add(createLabel("Publicado:", true), 0, 3);
            grid.add(createLabel(release.getFormattedPublishDate(), false), 1, 3);

            // Changelog
            Label lblChangelog = new Label("Novedades:");
            lblChangelog.setFont(Font.font("System", FontWeight.BOLD, 12));

            TextArea txtChangelog = new TextArea(release.getBody());
            txtChangelog.setEditable(false);
            txtChangelog.setWrapText(true);
            txtChangelog.setPrefRowCount(5);
            txtChangelog.setMaxHeight(120);

            // Botones
            HBox buttons = new HBox(10);
            buttons.setAlignment(Pos.CENTER_RIGHT);

            Button btnDownload = new Button("Descargar e Instalar");
            btnDownload.setStyle(
                    "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 8 20;"
            );

            Button btnChangelog = new Button("Ver en GitHub");
            btnChangelog.setStyle("-fx-padding: 8 20;");

            Button btnSkip = new Button("Omitir versión");
            btnSkip.setStyle("-fx-padding: 8 20;");

            Button btnLater = new Button("Más tarde");
            btnLater.setStyle("-fx-padding: 8 20;");

            buttons.getChildren().addAll(btnDownload, btnChangelog, btnSkip, btnLater);

            // Eventos
            btnDownload.setOnAction(e -> {
                dialog.close();
                showUpdateProgress(owner, manager, true);
            });

            btnChangelog.setOnAction(e -> {
                openURL(release.getHtmlUrl());
            });

            btnSkip.setOnAction(e -> {
                manager.skipVersion();
                dialog.close();
                showInfo(owner, "Versión omitida",
                        "No se te volverá a notificar de la versión " + release.getTagName());
            });

            btnLater.setOnAction(e -> dialog.close());

            // Ensamblar
            root.getChildren().addAll(
                    lblTitle,
                    new Separator(),
                    grid,
                    lblChangelog,
                    txtChangelog,
                    new Separator(),
                    buttons
            );

            Scene scene = new Scene(root, 500, 450);
            dialog.setScene(scene);
            dialog.show();
        });
    }

    /**
     * Muestra el progreso de descarga e instalación
     */
    public static void showUpdateProgress(Stage owner, UpdateManager manager, boolean autoRestart) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(owner);
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle("Actualizando Ariel Cardales");
            dialog.setResizable(false);
            dialog.setOnCloseRequest(e -> e.consume()); // No cerrar durante update

            VBox root = new VBox(15);
            root.setPadding(new Insets(25));
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: white;");

            // Título con fase actual
            Label lblPhase = new Label("Preparando actualización...");
            lblPhase.setFont(Font.font("System", FontWeight.BOLD, 14));

            // Barra de progreso
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(400);
            progressBar.setPrefHeight(25);

            // Mensaje detallado
            Label lblMessage = new Label("");
            lblMessage.setWrapText(true);
            lblMessage.setMaxWidth(400);
            lblMessage.setAlignment(Pos.CENTER);

            // Log de actividad (oculto por defecto)
            TextArea txtLog = new TextArea();
            txtLog.setEditable(false);
            txtLog.setPrefRowCount(8);
            txtLog.setMaxHeight(150);
            txtLog.setVisible(false);
            txtLog.setManaged(false);

            CheckBox chkShowLog = new CheckBox("Mostrar detalles técnicos");
            chkShowLog.selectedProperty().addListener((obs, old, val) -> {
                txtLog.setVisible(val);
                txtLog.setManaged(val);
            });

            // Botón de cancelar (solo durante descarga)
            Button btnCancel = new Button("Cancelar");
            btnCancel.setVisible(false);
            btnCancel.setOnAction(e -> {
                if (showConfirm(owner, "¿Cancelar actualización?",
                        "La descarga se interrumpirá. ¿Estás seguro?")) {
                    dialog.close();
                }
            });

            root.getChildren().addAll(
                    lblPhase,
                    progressBar,
                    lblMessage,
                    chkShowLog,
                    txtLog,
                    btnCancel
            );

            Scene scene = new Scene(root, 500, 200);
            dialog.setScene(scene);
            dialog.show();

            // Iniciar proceso de actualización
            manager.downloadAndInstallAsync(
                    progress -> Platform.runLater(() -> {
                        lblPhase.setText(progress.getPhase().getDisplayName());
                        lblMessage.setText(progress.getMessage());
                        progressBar.setProgress(progress.getPercentage() / 100.0);

                        txtLog.appendText(progress.toString() + "\n");
                        txtLog.setScrollTop(Double.MAX_VALUE);

                        // Mostrar/ocultar botón cancelar
                        btnCancel.setVisible(
                                progress.getPhase() == UpdateManager.UpdatePhase.DOWNLOADING
                        );

                        // Si completó o hubo error, cerrar después de un momento
                        if (progress.getPhase() == UpdateManager.UpdatePhase.COMPLETED) {
                            if (autoRestart) {
                                lblMessage.setText("Reiniciando aplicación...");
                            } else {
                                lblMessage.setText("¡Listo! Reinicia la aplicación para usar la nueva versión.");
                                // Cerrar automáticamente después de 3 segundos
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(3000);
                                        Platform.runLater(dialog::close);
                                    } catch (InterruptedException ignored) {}
                                }).start();
                            }
                        } else if (progress.getPhase() == UpdateManager.UpdatePhase.ERROR ||
                                progress.getPhase() == UpdateManager.UpdatePhase.ROLLED_BACK) {
                            showError(owner, "Error de actualización", progress.getMessage());
                            dialog.close();
                        }
                    }),
                    autoRestart
            );
        });
    }

    /**
     * Diálogo de confirmación simple
     */
    public static boolean showConfirm(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }

    /**
     * Diálogo de información
     */
    public static void showInfo(Stage owner, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(owner);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Diálogo de error
     */
    public static void showError(Stage owner, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Agregar botón para reportar bug
            ButtonType btnReport = new ButtonType("Reportar problema", ButtonBar.ButtonData.HELP);
            alert.getButtonTypes().add(btnReport);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnReport) {
                    openURL("https://github.com/" + UpdateConfig.getGithubUser() +
                            "/" + UpdateConfig.getRepoName() + "/issues/new");
                }
            });
        });
    }

    /**
     * Abre URL en navegador
     */
    private static void openURL(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            System.err.println("No se pudo abrir URL: " + e.getMessage());
        }
    }

    /**
     * Crea label con estilo personalizado
     */
    private static Label createLabel(String text, boolean bold) {
        Label label = new Label(text);
        if (bold) {
            label.setFont(Font.font("System", FontWeight.BOLD, 12));
        }
        return label;
    }
}