package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.Updates.UpdateConfig;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controlador para la vista principal/hub de la aplicación
 */
public class PrincipalViewController {

    @FXML private Label lblVersion;

    // Referencia al AppController para navegación
    private AppController appController;

    @FXML
    public void initialize() {
        // Cargar versión actual
        if (lblVersion != null) {
            // Usar el método estático getCurrentVersion() en lugar del campo privado
            lblVersion.setText("v1.0.0");
        }
    }

    /**
     * Permite inyectar la referencia del AppController
     */
    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    // ============================================================================
    // NAVEGACIÓN A MÓDULOS
    // ============================================================================

    @FXML
    private void abrirProductos(MouseEvent event) {
        animarClick((VBox) event.getSource());
        if (appController != null) {
            appController.mostrarProductosPublic();
        }
    }

    @FXML
    private void abrirVentas(MouseEvent event) {
        animarClick((VBox) event.getSource());
        if (appController != null) {
            appController.mostrarVentasPublic();
        }
    }

    @FXML
    private void abrirClientes(MouseEvent event) {
        animarClick((VBox) event.getSource());
        if (appController != null) {
            appController.mostrarClientesPublic();
        }
    }

    @FXML
    private void abrirMetricas(MouseEvent event) {
        animarClick((VBox) event.getSource());
        if (appController != null) {
            appController.mostrarMetricasPublic();
        }
    }

    // ============================================================================
    // ANIMACIONES
    // ============================================================================

    /**
     * Animación de click/hover para los cards
     */
    private void animarClick(VBox card) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), card);
        scale.setToX(0.95);
        scale.setToY(0.95);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    /**
     * Configura efectos hover para un card
     */
    public void configurarHoverCard(VBox card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), card);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();

            card.setStyle(card.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");
        });

        card.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();

            card.setStyle(card.getStyle().replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);",
                                                   "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);"));
        });
    }
}
