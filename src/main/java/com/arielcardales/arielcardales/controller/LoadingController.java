package com.arielcardales.arielcardales.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

/**
 * Controlador para la pantalla de carga
 * Muestra un indicador mientras se valida la sesión en segundo plano
 */
public class LoadingController {

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label loadingLabel;

    @FXML
    public void initialize() {
        // El ProgressIndicator está en modo indeterminado por defecto (-1)
        progressIndicator.setProgress(-1);
    }

    /**
     * Actualiza el mensaje de carga
     */
    public void setMensaje(String mensaje) {
        if (loadingLabel != null) {
            loadingLabel.setText(mensaje);
        }
    }
}
