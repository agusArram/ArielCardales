package SORT_PROYECTS.AppInventario.Util;

import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Utilidad para crear diálogos estilizados de forma consistente
 */
public class DialogosUtil {

    /**
     * Crea un diálogo de confirmación con estilo consistente
     *
     * @param titulo Título del diálogo
     * @param header Encabezado (texto principal)
     * @param contenido Contenido del mensaje
     * @param textoConfirmar Texto del botón de confirmación
     * @param textoCancelar Texto del botón de cancelar
     * @return Optional<ButtonType> con la respuesta del usuario
     */
    public static Optional<ButtonType> confirmarAccion(
            String titulo,
            String header,
            String contenido,
            String textoConfirmar,
            String textoCancelar) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(contenido);
        alert.initStyle(StageStyle.UTILITY);

        // Crear botones personalizados
        ButtonType btnConfirmar = new ButtonType(textoConfirmar, ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType(textoCancelar, ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnConfirmar, btnCancelar);

        // Aplicar estilos
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: #e4c79a;" +
            "-fx-border-color: #b88a52;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;"
        );

        // Estilo del header
        dialogPane.lookup(".header-panel").setStyle(
            "-fx-background-color: #b88a52;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 15;"
        );

        // Estilo del contenido
        dialogPane.lookup(".content").setStyle(
            "-fx-background-color: #e4c79a;" +
            "-fx-padding: 20;" +
            "-fx-font-size: 14px;"
        );

        // Estilizar botones
        javafx.application.Platform.runLater(() -> {
            Button btnConfirmarNode = (Button) dialogPane.lookupButton(btnConfirmar);
            Button btnCancelarNode = (Button) dialogPane.lookupButton(btnCancelar);

            if (btnConfirmarNode != null) {
                btnConfirmarNode.setStyle(
                    "-fx-background-color: #d32f2f;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 20;" +
                    "-fx-cursor: hand;" +
                    "-fx-background-radius: 6;" +
                    "-fx-min-width: 130px;"
                );
                btnConfirmarNode.setOnMouseEntered(e ->
                    btnConfirmarNode.setStyle(
                        "-fx-background-color: #b71c1c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 130px;"
                    )
                );
                btnConfirmarNode.setOnMouseExited(e ->
                    btnConfirmarNode.setStyle(
                        "-fx-background-color: #d32f2f;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 130px;"
                    )
                );
            }

            if (btnCancelarNode != null) {
                btnCancelarNode.setStyle(
                    "-fx-background-color: #cfa971;" +
                    "-fx-text-fill: #2b2b2b;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 20;" +
                    "-fx-cursor: hand;" +
                    "-fx-background-radius: 6;" +
                    "-fx-min-width: 130px;"
                );
                btnCancelarNode.setOnMouseEntered(e ->
                    btnCancelarNode.setStyle(
                        "-fx-background-color: #b88a52;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 130px;"
                    )
                );
                btnCancelarNode.setOnMouseExited(e ->
                    btnCancelarNode.setStyle(
                        "-fx-background-color: #cfa971;" +
                        "-fx-text-fill: #2b2b2b;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 130px;"
                    )
                );
            }
        });

        // Ajustar tamaño mínimo
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);

        return alert.showAndWait();
    }

    /**
     * Diálogo de confirmación para eliminaciones (versión corta con valores por defecto)
     */
    public static boolean confirmarEliminacion(String titulo, String mensaje) {
        Optional<ButtonType> resultado = confirmarAccion(
            titulo,
            "⚠️ Confirmar eliminación",
            mensaje + "\n\n⚠️ Esta acción NO se puede deshacer.\n\n¿Deseas continuar?",
            "Sí, eliminar",
            "Cancelar"
        );

        return resultado.isPresent() &&
               resultado.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    /**
     * Crea un diálogo de entrada de texto con estilo consistente
     *
     * @param titulo Título del diálogo
     * @param header Encabezado (texto principal)
     * @param contenido Mensaje/instrucción
     * @param valorPorDefecto Valor inicial del campo de texto (puede ser null o vacío)
     * @return Optional<String> con el texto ingresado
     */
    public static Optional<String> solicitarTexto(
            String titulo,
            String header,
            String contenido,
            String valorPorDefecto) {

        TextInputDialog dialog = new TextInputDialog(valorPorDefecto != null ? valorPorDefecto : "");
        dialog.setTitle(titulo);
        dialog.setHeaderText(header);
        dialog.setContentText(contenido);
        dialog.initStyle(StageStyle.UTILITY);

        // Aplicar estilos
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: #e4c79a;" +
            "-fx-border-color: #b88a52;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;"
        );

        // Estilo del header
        javafx.application.Platform.runLater(() -> {
            var header_panel = dialogPane.lookup(".header-panel");
            if (header_panel != null) {
                header_panel.setStyle(
                    "-fx-background-color: #b88a52;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 15px;" +
                    "-fx-padding: 15;"
                );
            }
        });

        // Estilo del contenido
        javafx.application.Platform.runLater(() -> {
            var content = dialogPane.lookup(".content");
            if (content != null) {
                content.setStyle(
                    "-fx-background-color: #e4c79a;" +
                    "-fx-padding: 20;" +
                    "-fx-font-size: 14px;"
                );
            }

            // Estilizar el TextField
            var textField = dialogPane.lookup(".text-field");
            if (textField != null) {
                textField.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-border-color: #b88a52;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-background-radius: 4;" +
                    "-fx-padding: 8;" +
                    "-fx-font-size: 13px;"
                );
            }
        });

        // Estilizar botones
        javafx.application.Platform.runLater(() -> {
            Button btnOk = (Button) dialogPane.lookupButton(ButtonType.OK);
            Button btnCancel = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

            if (btnOk != null) {
                btnOk.setText("Aceptar");
                btnOk.setStyle(
                    "-fx-background-color: #4caf50;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 20;" +
                    "-fx-cursor: hand;" +
                    "-fx-background-radius: 6;" +
                    "-fx-min-width: 100px;"
                );
                btnOk.setOnMouseEntered(e ->
                    btnOk.setStyle(
                        "-fx-background-color: #388e3c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 100px;"
                    )
                );
                btnOk.setOnMouseExited(e ->
                    btnOk.setStyle(
                        "-fx-background-color: #4caf50;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 100px;"
                    )
                );
            }

            if (btnCancel != null) {
                btnCancel.setText("Cancelar");
                btnCancel.setStyle(
                    "-fx-background-color: #cfa971;" +
                    "-fx-text-fill: #2b2b2b;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 20;" +
                    "-fx-cursor: hand;" +
                    "-fx-background-radius: 6;" +
                    "-fx-min-width: 100px;"
                );
                btnCancel.setOnMouseEntered(e ->
                    btnCancel.setStyle(
                        "-fx-background-color: #b88a52;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 100px;"
                    )
                );
                btnCancel.setOnMouseExited(e ->
                    btnCancel.setStyle(
                        "-fx-background-color: #cfa971;" +
                        "-fx-text-fill: #2b2b2b;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-min-width: 100px;"
                    )
                );
            }
        });

        // Ajustar tamaño mínimo
        dialogPane.setMinHeight(220);
        dialogPane.setMinWidth(400);

        return dialog.showAndWait();
    }
}
