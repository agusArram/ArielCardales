package SORT_PROYECTS.AppInventario.Updates;

import SORT_PROYECTS.AppInventario.service.sync.SyncResult;
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
 * Diálogos JavaFX para el sistema de sincronización de backup
 */
public class SyncDialog {

    /**
     * Muestra diálogo de confirmación antes de sincronizar
     *
     * @param owner Ventana padre
     * @return true si el usuario confirma, false si cancela
     */
    public static boolean showSyncConfirmation(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Sincronizar Backup Local");
        alert.setHeaderText("¿Desea sincronizar los datos con el backup local?");
        alert.setContentText(
            "Esta operación descargará todos los datos desde la nube (Supabase)\n" +
            "y los guardará en el backup local (SQLite).\n\n" +
            "• Dirección: Supabase → SQLite\n" +
            "• Los datos locales se actualizarán con los datos de la nube\n" +
            "• Esta operación puede tardar unos segundos\n\n" +
            "¿Desea continuar?"
        );

        ButtonType btnSi = new ButtonType("Sí, sincronizar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnSi, btnNo);

        return alert.showAndWait()
                .filter(response -> response == btnSi)
                .isPresent();
    }

    /**
     * Muestra diálogo de progreso durante la sincronización
     *
     * @param owner Ventana padre
     * @return Stage del diálogo (para cerrarlo externamente)
     */
    public static Stage showSyncProgress(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Sincronizando...");
        dialog.setResizable(false);
        dialog.setOnCloseRequest(e -> e.consume()); // No cerrar durante sync

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white;");

        // Título
        Label lblTitle = new Label("💾 Sincronizando datos...");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Barra de progreso indeterminada
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(60, 60);

        // Mensaje
        Label lblMessage = new Label("Descargando datos desde la nube...\nPor favor espere.");
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(350);
        lblMessage.setAlignment(Pos.CENTER);
        lblMessage.setStyle("-fx-text-fill: #555;");

        root.getChildren().addAll(lblTitle, progress, lblMessage);

        Scene scene = new Scene(root, 400, 250);
        dialog.setScene(scene);

        Platform.runLater(dialog::show);

        return dialog;
    }

    /**
     * Muestra diálogo con los resultados de la sincronización
     *
     * @param owner Ventana padre
     * @param result Resultado de la sincronización
     */
    public static void showSyncResults(Stage owner, SyncResult result) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(owner);
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle(result.isSuccess() ? "Sincronización Completada" : "Error de Sincronización");
            dialog.setResizable(false);

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");

            // Título
            String emoji = result.isSuccess() ? "✅" : "❌";
            Label lblTitle = new Label(emoji + " " + result.getMessage());
            lblTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
            lblTitle.setWrapText(true);

            if (result.isSuccess()) {
                // Estadísticas de sincronización
                GridPane grid = new GridPane();
                grid.setHgap(15);
                grid.setVgap(8);
                grid.setPadding(new Insets(10, 0, 0, 0));

                SyncResult.SyncStats stats = result.getStats();

                int row = 0;

                // Unidades
                if (stats.getUnidadesInsertadas() > 0 || stats.getUnidadesActualizadas() > 0) {
                    grid.add(createLabel("Unidades:", true), 0, row);
                    grid.add(createLabel(
                        stats.getUnidadesInsertadas() + " nuevas, " +
                        stats.getUnidadesActualizadas() + " actualizadas", false), 1, row++);
                }

                // Categorías
                if (stats.getCategoriasInsertadas() > 0 || stats.getCategoriasActualizadas() > 0) {
                    grid.add(createLabel("Categorías:", true), 0, row);
                    grid.add(createLabel(
                        stats.getCategoriasInsertadas() + " nuevas, " +
                        stats.getCategoriasActualizadas() + " actualizadas", false), 1, row++);
                }

                // Productos
                if (stats.getProductosInsertados() > 0 || stats.getProductosActualizados() > 0) {
                    grid.add(createLabel("Productos:", true), 0, row);
                    grid.add(createLabel(
                        stats.getProductosInsertados() + " nuevos, " +
                        stats.getProductosActualizados() + " actualizados", false), 1, row++);
                }

                // Variantes
                if (stats.getVariantesInsertadas() > 0 || stats.getVariantesActualizadas() > 0) {
                    grid.add(createLabel("Variantes:", true), 0, row);
                    grid.add(createLabel(
                        stats.getVariantesInsertadas() + " nuevas, " +
                        stats.getVariantesActualizadas() + " actualizadas", false), 1, row++);
                }

                // Ventas
                if (stats.getVentasInsertadas() > 0) {
                    grid.add(createLabel("Ventas:", true), 0, row);
                    grid.add(createLabel(stats.getVentasInsertadas() + " nuevas", false), 1, row++);
                }

                // Conflictos resueltos
                if (stats.getConflictosResueltos() > 0) {
                    grid.add(createLabel("Conflictos resueltos:", true), 0, row);
                    grid.add(createLabel(String.valueOf(stats.getConflictosResueltos()), false), 1, row++);
                }

                // Total
                Separator sep = new Separator();
                GridPane.setColumnSpan(sep, 2);
                grid.add(sep, 0, row++);

                grid.add(createLabel("Total de operaciones:", true), 0, row);
                grid.add(createLabel(String.valueOf(stats.getTotalOperaciones()), false), 1, row);

                root.getChildren().addAll(lblTitle, new Separator(), grid);

            } else {
                // Mostrar errores
                Label lblErrorTitle = new Label("Se encontraron los siguientes errores:");
                lblErrorTitle.setFont(Font.font("System", FontWeight.BOLD, 12));

                TextArea txtErrors = new TextArea();
                txtErrors.setEditable(false);
                txtErrors.setWrapText(true);
                txtErrors.setPrefRowCount(5);
                txtErrors.setMaxHeight(150);

                StringBuilder errorText = new StringBuilder();
                for (String error : result.getErrors()) {
                    errorText.append("• ").append(error).append("\n");
                }
                txtErrors.setText(errorText.toString());

                root.getChildren().addAll(lblTitle, new Separator(), lblErrorTitle, txtErrors);
            }

            // Botón OK
            Button btnOk = new Button("Aceptar");
            btnOk.setStyle(
                "-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 8 30;"
            );
            btnOk.setOnAction(e -> dialog.close());

            HBox buttonBox = new HBox(btnOk);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setPadding(new Insets(10, 0, 0, 0));

            root.getChildren().addAll(new Separator(), buttonBox);

            Scene scene = new Scene(root, 450, result.isSuccess() ? 400 : 350);
            dialog.setScene(scene);
            dialog.show();
        });
    }

    /**
     * Muestra diálogo de error simple
     */
    public static void showError(Stage owner, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
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
