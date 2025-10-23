package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.service.InventarioService;
import com.arielcardales.arielcardales.service.InventarioService.ResultadoActualizacion;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.TreeTableView;
import javafx.util.StringConverter;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

/**
 * Configurador de edición inline para TreeTableView.
 *
 * RESPONSABILIDAD: Solo configurar cell factories de JavaFX (UI)
 * DELEGACIÓN: Toda la lógica de negocio está en InventarioService
 */
public class EdicionCeldas {

    private final InventarioService inventarioService;
    private final TreeTableView<ItemInventario> tabla;

    public EdicionCeldas(InventarioService service, TreeTableView<ItemInventario> tabla) {
        this.inventarioService = service;
        this.tabla = tabla;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CONFIGURADORES PÚBLICOS - Solo configuran UI
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Configura edición de texto simple
     */
    public void configurarTexto(TreeTableColumn<ItemInventario, String> col, String campo) {
        col.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            String nuevoValor = event.getNewValue();

            // Delegar al Service
            ResultadoActualizacion resultado = inventarioService.actualizarCampo(item, campo, nuevoValor);

            if (resultado.isExitoso()) {
                // Actualizar objeto en memoria
                actualizarObjetoEnMemoria(item, campo, nuevoValor);
                ok(resultado.getMensaje());
            } else {
                error(resultado.getMensaje());
            }

            tabla.refresh();
        });
    }

    /**
     * Configura edición de precios/costos SIN DECIMALES (para Argentina)
     * Los valores se guardan como BigDecimal en BD pero se muestran/editan como enteros
     */
    public void configurarPrecioEntero(TreeTableColumn<ItemInventario, BigDecimal> col, String campo) {
        col.setCellFactory(tc -> new TreeTableCell<ItemInventario, BigDecimal>() {
            private javafx.scene.control.TextField textField;

            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            // Mostrar solo la parte entera al editar
                            textField.setText(String.valueOf(item.intValue()));
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        // Mostrar como "$1,234" sin decimales
                        java.text.NumberFormat formato = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR"));
                        formato.setMaximumFractionDigits(0);
                        formato.setMinimumFractionDigits(0);
                        setText(formato.format(item.intValue()));
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                if (isEmpty()) return;

                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                java.text.NumberFormat formato = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR"));
                formato.setMaximumFractionDigits(0);
                formato.setMinimumFractionDigits(0);
                setText(formato.format(getItem().intValue()));
                setGraphic(null);
            }

            private void createTextField() {
                textField = new javafx.scene.control.TextField(String.valueOf(getItem().intValue()));
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                // Solo permitir números enteros
                textField.textProperty().addListener((obs, oldValue, newValue) -> {
                    if (!newValue.matches("\\d*")) {
                        textField.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                });

                textField.setOnAction(evt -> commitarValor());
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) commitarValor();
                });
            }

            private void commitarValor() {
                try {
                    String input = textField.getText().trim();
                    if (input.isEmpty()) input = "0";

                    // Parsear como entero y convertir a BigDecimal
                    int valorEntero = Integer.parseInt(input);
                    BigDecimal nuevoValor = new BigDecimal(valorEntero);
                    commitEdit(nuevoValor);
                } catch (Exception e) {
                    cancelEdit();
                }
            }
        });

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            BigDecimal nuevoValor = event.getNewValue();

            // Delegar al Service (enviar como entero, sin decimales)
            ResultadoActualizacion resultado = inventarioService.actualizarCampo(item, campo, String.valueOf(nuevoValor.intValue()));

            if (resultado.isExitoso()) {
                // Actualizar objeto en memoria
                if (campo.equalsIgnoreCase("precio")) {
                    item.setPrecio(nuevoValor);
                } else {
                    item.setCosto(nuevoValor);
                }
                ok(resultado.getMensaje());
            } else {
                error(resultado.getMensaje());
            }

            tabla.refresh();
        });
    }

    /**
     * Configura edición de BigDecimal (precio, costo) con formato de moneda CON DECIMALES
     * @deprecated Usar configurarPrecioEntero() para Argentina (sin centavos)
     */
    @Deprecated
    public void configurarDecimal(TreeTableColumn<ItemInventario, BigDecimal> col, String campo) {
        col.setCellFactory(tc -> new TreeTableCell<ItemInventario, BigDecimal>() {
            private javafx.scene.control.TextField textField;
            private final java.text.NumberFormat formato = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR"));

            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            textField.setText(item.toPlainString());
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(formato.format(item));
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                if (isEmpty()) return;

                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(formato.format(getItem()));
                setGraphic(null);
            }

            private void createTextField() {
                textField = new javafx.scene.control.TextField(getItem().toPlainString());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                textField.setOnAction(evt -> commitarValor());
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) commitarValor();
                });
            }

            private void commitarValor() {
                try {
                    String input = textField.getText()
                            .replace("$", "")
                            .replace(" ", "")
                            .replace(".", "")
                            .replace(",", ".")
                            .trim();

                    BigDecimal nuevoValor = new BigDecimal(input);
                    commitEdit(nuevoValor);
                } catch (Exception e) {
                    cancelEdit();
                }
            }
        });

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            BigDecimal nuevoValor = event.getNewValue();

            // Delegar al Service
            ResultadoActualizacion resultado = inventarioService.actualizarCampo(item, campo, nuevoValor.toPlainString());

            if (resultado.isExitoso()) {
                // Actualizar objeto en memoria
                if (campo.equalsIgnoreCase("precio")) {
                    item.setPrecio(nuevoValor);
                } else {
                    item.setCosto(nuevoValor);
                }
                ok(resultado.getMensaje());
            } else {
                error(resultado.getMensaje());
            }

            tabla.refresh();
        });
    }

    /**
     * Configura edición de enteros (stock)
     */
    public void configurarEntero(TreeTableColumn<ItemInventario, Integer> col, String campo, int min, int max) {
        col.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return (value != null) ? value.toString() : "0";
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()) return min;
                try {
                    String limpio = string.replaceAll("[^\\d-]", "").trim();
                    if (limpio.isBlank() || limpio.equals("-")) return min;
                    return Integer.parseInt(limpio);
                } catch (Exception e) {
                    return min;
                }
            }
        }));

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            Integer nuevoValor = event.getNewValue();
            Integer anterior = event.getOldValue();

            // Validación de rango (UI)
            if (nuevoValor < min) {
                error("El valor no puede ser menor a " + min);
                item.setStockOnHand(anterior);
                tabla.refresh();
                return;
            }

            if (nuevoValor > max) {
                error("El valor no puede ser mayor a " + max);
                item.setStockOnHand(anterior);
                tabla.refresh();
                return;
            }

            // Delegar al Service
            ResultadoActualizacion resultado = inventarioService.actualizarCampo(item, campo, nuevoValor.toString());

            if (resultado.isExitoso()) {
                item.setStockOnHand(nuevoValor);
                ok(resultado.getMensaje());
            } else {
                error(resultado.getMensaje());
            }

            tabla.refresh();
        });
    }

    /**
     * Configura edición con ComboBox
     */
    public void configurarCombo(TreeTableColumn<ItemInventario, String> col,
                                String campo,
                                ObservableList<String> opciones) {
        col.setCellFactory(ComboBoxTreeTableCell.forTreeTableColumn(opciones));

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            String nuevoValor = event.getNewValue();

            // Delegar al Service
            ResultadoActualizacion resultado = inventarioService.actualizarCampo(item, campo, nuevoValor);

            if (resultado.isExitoso()) {
                item.setCategoria(nuevoValor);
                ok(resultado.getMensaje());
            } else {
                error(resultado.getMensaje());
            }

            tabla.refresh();
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Actualiza el objeto en memoria después de un guardado exitoso
     */
    private void actualizarObjetoEnMemoria(ItemInventario item, String campo, String valor) {
        switch (campo.toLowerCase()) {
            case "nombre" -> item.setNombreProducto(valor);
            case "color" -> item.setColor(valor);
            case "talle" -> item.setTalle(valor);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // NOTIFICACIONES
    // ════════════════════════════════════════════════════════════════════════════

    private void ok(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(3))
                .showConfirm();
    }

    private void error(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(5))
                .showError();
    }
}
