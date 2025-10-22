package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.App;
import com.arielcardales.arielcardales.DAO.AutenticacionDAO;
import com.arielcardales.arielcardales.Licencia.Licencia;
import com.arielcardales.arielcardales.session.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador para la pantalla de login
 */
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button cancelButton;

    @FXML
    private HBox loadingBox;

    private final AutenticacionDAO autenticacionDAO = new AutenticacionDAO();

    // ============================================================================
    // INICIALIZACIÓN
    // ============================================================================

    @FXML
    public void initialize() {
        // Focus en el campo email al cargar
        Platform.runLater(() -> emailField.requestFocus());

        // Validación simple en tiempo real
        emailField.textProperty().addListener((obs, old, newVal) -> {
            if (errorLabel.isVisible()) {
                hideError();
            }
        });

        passwordField.textProperty().addListener((obs, old, newVal) -> {
            if (errorLabel.isVisible()) {
                hideError();
            }
        });
    }

    // ============================================================================
    // ACCIONES
    // ============================================================================

    /**
     * Maneja el intento de login
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validaciones básicas
        if (email.isEmpty()) {
            showError("Por favor ingrese su email");
            emailField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Por favor ingrese su contraseña");
            passwordField.requestFocus();
            return;
        }

        // Validación simple de formato de email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showError("Email inválido");
            emailField.requestFocus();
            return;
        }

        // Ejecutar login en background thread
        autenticarAsync(email, password);
    }

    /**
     * Maneja el botón cancelar/salir
     */
    @FXML
    private void handleCancel() {
        Platform.exit();
    }

    // ============================================================================
    // AUTENTICACIÓN ASÍNCRONA
    // ============================================================================

    /**
     * Autentica al usuario de forma asíncrona (no bloquea la UI)
     */
    private void autenticarAsync(String email, String password) {
        // Deshabilitar botones y mostrar loading
        setUIEnabled(false);
        showLoading();

        // Crear task de autenticación
        Task<Optional<Licencia>> loginTask = new Task<>() {
            @Override
            protected Optional<Licencia> call() {
                try {
                    // Pequeña pausa para UX (opcional)
                    Thread.sleep(300);

                    // Autenticar
                    return autenticacionDAO.login(email, password);

                } catch (Exception e) {
                    System.err.println("Error en autenticación: " + e.getMessage());
                    e.printStackTrace();
                    return Optional.empty();
                }
            }
        };

        // Manejar resultado exitoso
        loginTask.setOnSucceeded(event -> {
            Optional<Licencia> licenciaOpt = loginTask.getValue();

            if (licenciaOpt.isPresent()) {
                // Login exitoso
                Licencia licencia = licenciaOpt.get();

                // Iniciar sesión en SessionManager
                SessionManager.getInstance().login(licencia);

                // Cargar ventana principal
                cargarVentanaPrincipal();

            } else {
                // Credenciales incorrectas
                hideLoading();
                setUIEnabled(true);
                showError("Email o contraseña incorrectos");
                passwordField.clear();
                passwordField.requestFocus();
            }
        });

        // Manejar error
        loginTask.setOnFailed(event -> {
            hideLoading();
            setUIEnabled(true);
            Throwable exception = loginTask.getException();
            showError("Error al autenticar: " + exception.getMessage());
            System.err.println("Error en task de login:");
            exception.printStackTrace();
        });

        // Ejecutar en thread separado
        new Thread(loginTask).start();
    }

    // ============================================================================
    // NAVEGACIÓN
    // ============================================================================

    /**
     * Carga la ventana principal después del login exitoso
     */
    private void cargarVentanaPrincipal() {
        try {
            // Cargar FXML de la ventana principal
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/principal.fxml"));
            Parent root = loader.load();

            // Obtener el stage actual (ventana de login)
            Stage stage = (Stage) loginButton.getScene().getWindow();

            // Configurar la nueva escena
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Ariel Cardales - " + SessionManager.getInstance().getNombreUsuario());
            stage.setMaximized(true);
            stage.show();

            System.out.println("✅ Ventana principal cargada exitosamente");

        } catch (IOException e) {
            System.err.println("❌ Error cargando ventana principal: " + e.getMessage());
            e.printStackTrace();
            showError("Error al cargar la aplicación");
        }
    }

    // ============================================================================
    // UI HELPERS
    // ============================================================================

    /**
     * Muestra un mensaje de error
     */
    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Oculta el mensaje de error
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Muestra el indicador de carga
     */
    private void showLoading() {
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
    }

    /**
     * Oculta el indicador de carga
     */
    private void hideLoading() {
        loadingBox.setVisible(false);
        loadingBox.setManaged(false);
    }

    /**
     * Habilita/deshabilita la UI durante el login
     */
    private void setUIEnabled(boolean enabled) {
        emailField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        loginButton.setDisable(!enabled);
        cancelButton.setDisable(!enabled);
    }
}
