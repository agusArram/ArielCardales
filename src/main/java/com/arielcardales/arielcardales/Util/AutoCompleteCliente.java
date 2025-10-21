package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.DAO.ClienteDAO;
import com.arielcardales.arielcardales.Entidades.Cliente;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * TextField con autocompletado de clientes
 * Muestra sugerencias mientras el usuario escribe
 */
public class AutoCompleteCliente extends TextField {

    private final ContextMenu sugerencias = new ContextMenu();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private Cliente clienteSeleccionado = null;

    public AutoCompleteCliente() {
        super();
        configurarAutocompletado();
    }

    private void configurarAutocompletado() {
        // Listener para mostrar sugerencias mientras escribe
        textProperty().addListener((obs, oldValue, newValue) -> {
            clienteSeleccionado = null; // Reset al escribir

            if (newValue == null || newValue.trim().length() < 2) {
                sugerencias.hide();
                return;
            }

            String filtro = newValue.trim();

            // Buscar clientes que coincidan
            try {
                List<Cliente> clientes = clienteDAO.buscarPorNombre(filtro);

                // Si no hay resultados, ocultar
                if (clientes.isEmpty()) {
                    sugerencias.hide();
                    return;
                }

                // Limpiar sugerencias anteriores
                sugerencias.getItems().clear();

                // Limitar a 5 sugerencias
                int limite = Math.min(5, clientes.size());
                for (int i = 0; i < limite; i++) {
                    Cliente cliente = clientes.get(i);

                    // Crear label con info del cliente
                    String detalles = cliente.getNombre();
                    if (cliente.getDni() != null && !cliente.getDni().isEmpty()) {
                        detalles += " - DNI: " + cliente.getDni();
                    } else if (cliente.getTelefono() != null && !cliente.getTelefono().isEmpty()) {
                        detalles += " - Tel: " + cliente.getTelefono();
                    }

                    Label label = new Label(detalles);
                    label.setStyle("-fx-padding: 5 10; -fx-cursor: hand;");

                    CustomMenuItem item = new CustomMenuItem(label, true);
                    item.setOnAction(e -> {
                        // Al seleccionar, poner el nombre en el campo
                        setText(cliente.getNombre());
                        clienteSeleccionado = cliente;
                        sugerencias.hide();
                    });

                    sugerencias.getItems().add(item);
                }

                // Mostrar sugerencias debajo del TextField
                if (!sugerencias.isShowing()) {
                    sugerencias.show(this, Side.BOTTOM, 0, 0);
                }

            } catch (Exception e) {
                e.printStackTrace();
                sugerencias.hide();
            }
        });

        // Ocultar sugerencias al perder foco
        focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                // Delay para permitir click en sugerencia
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {}
                    sugerencias.hide();
                });
            }
        });
    }

    /**
     * Obtiene el cliente seleccionado del autocompletado
     * @return Cliente seleccionado o null si es un nombre nuevo
     */
    public Cliente getClienteSeleccionado() {
        return clienteSeleccionado;
    }

    /**
     * Obtiene el ID del cliente seleccionado
     * @return ID del cliente o null si no hay selección
     */
    public Long getClienteId() {
        return clienteSeleccionado != null ? clienteSeleccionado.getId() : null;
    }

    /**
     * Reinicia el campo y la selección
     */
    public void reset() {
        setText("");
        clienteSeleccionado = null;
        sugerencias.hide();
    }
}
