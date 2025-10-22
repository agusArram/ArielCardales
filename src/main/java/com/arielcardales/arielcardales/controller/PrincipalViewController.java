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
    @FXML private VBox cardProductos;
    @FXML private VBox cardVentas;
    @FXML private VBox cardClientes;
    @FXML private VBox cardMetricas;

    // Referencia al AppController para navegación
    private AppController appController;

    @FXML
    public void initialize() {
        // Cargar versión actual
        if (lblVersion != null) {
            lblVersion.setText("v1.0.0");
        }

        // Configurar efectos hover sutiles en los cards
        configurarHover(cardProductos);
        configurarHover(cardVentas);
        configurarHover(cardClientes);
        configurarHover(cardMetricas);
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
        if (appController != null) {
            appController.mostrarProductosPublic();
        }
    }

    @FXML
    private void abrirVentas(MouseEvent event) {
        if (appController != null) {
            appController.mostrarVentasPublic();
        }
    }

    @FXML
    private void abrirClientes(MouseEvent event) {
        if (appController != null) {
            appController.mostrarClientesPublic();
        }
    }

    @FXML
    private void abrirMetricas(MouseEvent event) {
        if (appController != null) {
            appController.mostrarMetricasPublic();
        }
    }

    // ============================================================================
    // EFECTOS HOVER
    // ============================================================================

    /**
     * Configura efecto hover sutil (solo escala) para un card
     */
    private void configurarHover(VBox card) {
        if (card == null) return;

        card.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.03);
            scale.setToY(1.03);
            scale.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
    }
}
