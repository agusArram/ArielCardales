package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.service.InventarioService;
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
 * Helper para configurar edición inline en TreeTableView de ItemInventario.
 * Centraliza toda la lógica de edición de celdas eliminando duplicación.
 */
public class EdicionCeldas {

    private final InventarioService inventarioService;
    private final TreeTableView<ItemInventario> tabla;

    public EdicionCeldas(InventarioService service, TreeTableView<ItemInventario> tabla) {
        this.inventarioService = service;
        this.tabla = tabla;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // CONFIGURADORES PÚBLICOS
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Configura edición de texto simple con validación específica por campo
     */
    public void configurarTexto(TreeTableColumn<ItemInventario, String> col, String campo) {
        col.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());

        // ⭐ Bloquear edición de nombre en variantes
        col.setOnEditStart(event -> {
            ItemInventario item = event.getRowValue().getValue();

            if (item.isEsVariante() && campo.equalsIgnoreCase("nombre")) {
                error("⚠ Las variantes heredan el nombre del producto base");
                event.consume();
            }
        });

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            String nuevoValor = event.getNewValue();

            guardarCambio(item, campo, nuevoValor, (it, val) -> {
                switch (campo.toLowerCase()) {
                    case "nombre" -> it.setNombreProducto(val);
                    case "color" -> it.setColor(val);
                    case "talle" -> it.setTalle(val);
                }
            });
        });
    }

    /**
     * Configura edición de BigDecimal (precio, costo) con formato de moneda
     */
    public void configurarDecimal(TreeTableColumn<ItemInventario, BigDecimal> col, String campo) {
        // Cell factory con formato $ y edición
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

                textField.setOnAction(evt -> {
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
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
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
            }
        });

        col.setOnEditCommit(event -> {
            ItemInventario item = event.getRowValue().getValue();
            BigDecimal nuevoValor = event.getNewValue();

            guardarCambio(item, campo, nuevoValor.toPlainString(), (it, val) -> {
                BigDecimal valor = new BigDecimal(val);
                if (campo.equalsIgnoreCase("precio")) {
                    it.setPrecio(valor);
                } else {
                    it.setCosto(valor);
                }
            });
        });
    }

    /**
     * Configura edición de enteros (stock)
     */
    public void configurarEntero(TreeTableColumn<ItemInventario, Integer> col, String campo) {
        configurarEntero(col, campo, 0, Integer.MAX_VALUE);
    }

    /**
     * Configura edición de enteros con validación de rango
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

            // Validación de rango
            if (nuevoValor < min) {
                error("⚠ El valor no puede ser menor a " + min);
                item.setStockOnHand(anterior);
                tabla.refresh();
                return;
            }

            if (nuevoValor > max) {
                error("⚠ El valor no puede ser mayor a " + max);
                item.setStockOnHand(anterior);
                tabla.refresh();
                return;
            }

            guardarCambio(item, campo, nuevoValor.toString(), (it, val) -> {
                it.setStockOnHand(Integer.parseInt(val));
            });
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

            // ✅ VALIDACIÓN: No permitir editar categoría en variantes
            if (item.isEsVariante() && campo.equalsIgnoreCase("categoria")) {
                error("Las variantes heredan la categoría del producto base");
                tabla.refresh();
                event.consume(); // Cancela el evento
                return;
            }

            guardarCambio(item, campo, nuevoValor, (it, val) -> {
                it.setCategoria(val);
            });
        });
    }

    // ────────────────────────────────────────────────────────────────────────────
    // LÓGICA CENTRAL DE GUARDADO
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Guarda un cambio en BD y actualiza el objeto en memoria
     *
     * @param item ItemInventario a actualizar
     * @param campo Nombre del campo
     * @param valor Nuevo valor como String
     * @param actualizador Lambda que actualiza el objeto en memoria
     */
    private void guardarCambio(ItemInventario item,
                               String campo,
                               String valor,
                               BiConsumer<ItemInventario, String> actualizador) {
        try {
            // Validación: campos que no aplican a productos base
            if (!item.isEsVariante() &&
                    (campo.equalsIgnoreCase("color") || campo.equalsIgnoreCase("talle"))) {
                error("Este producto no tiene variantes, no puede editar " + campo);
                tabla.refresh();
                return;
            }

            // Validación: categoría en variantes
            if (item.isEsVariante() && campo.equalsIgnoreCase("categoria")) {
                error("Las variantes heredan la categoría del producto base");
                tabla.refresh();
                return;
            }

            // Guardar en BD
            boolean exitoso;
            if (item.isEsVariante()) {
                exitoso = inventarioService.actualizarVariante(item.getVarianteId(), campo, valor);

                // ⚠️ Validación especial: duplicado de color/talle
                if (!exitoso && (campo.equalsIgnoreCase("color") || campo.equalsIgnoreCase("talle"))) {
                    error("⚠ Ya existe una variante con esa combinación de color y talle");
                    tabla.refresh();
                    return;
                }
            } else {
                exitoso = inventarioService.actualizarCampo(item.getProductoId(), campo, valor);
            }

            if (exitoso) {
                // Actualizar objeto en memoria
                actualizador.accept(item, valor);
                ok("✓ " + campo + " actualizado correctamente");
            } else {
                error("⚠ No se pudo actualizar " + campo);
            }

        } catch (Exception e) {
            e.printStackTrace();
            error("❌ Error al actualizar " + campo + ": " + e.getMessage());
        } finally {
            tabla.refresh();
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // NOTIFICACIONES
    // ────────────────────────────────────────────────────────────────────────────

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