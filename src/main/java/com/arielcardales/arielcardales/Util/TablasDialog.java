package com.arielcardales.arielcardales.Util;

import javafx.beans.property.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Function;

/**
 * Helper para crear TableViews en diálogos sin usar reflexión.
 * Usa lambdas para acceder a propiedades directamente.
 */
public class TablasDialog {

    /**
     * Crea una columna de texto
     */
    public static <T> TableColumn<T, String> crearColumnaTexto(
            String titulo,
            Function<T, String> getter,
            double ancho) {

        TableColumn<T, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(cellData ->
                new SimpleStringProperty(getter.apply(cellData.getValue())));
        col.setPrefWidth(ancho);
        col.setStyle("-fx-alignment: CENTER-LEFT;");
        return col;
    }

    /**
     * Crea una columna de enteros
     */
    public static <T> TableColumn<T, Integer> crearColumnaEntero(
            String titulo,
            Function<T, Integer> getter,
            double ancho) {

        TableColumn<T, Integer> col = new TableColumn<>(titulo);
        col.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(getter.apply(cellData.getValue())).asObject());
        col.setPrefWidth(ancho);
        col.setStyle("-fx-alignment: CENTER;");
        return col;
    }

    /**
     * Crea una columna de moneda (BigDecimal con formato $)
     */
    public static <T> TableColumn<T, BigDecimal> crearColumnaMoneda(
            String titulo,
            Function<T, BigDecimal> getter,
            double ancho) {

        TableColumn<T, BigDecimal> col = new TableColumn<>(titulo);
        col.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(getter.apply(cellData.getValue())));
        col.setPrefWidth(ancho);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Aplicar formato de moneda
        col.setCellFactory(tc -> new TableCell<T, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                    setText(formato.format(item));
                }
            }
        });

        return col;
    }

    /**
     * Crea una columna numérica simple (sin formato especial)
     */
    public static <T> TableColumn<T, Number> crearColumnaNumero(
            String titulo,
            Function<T, Number> getter,
            double ancho) {

        TableColumn<T, Number> col = new TableColumn<>(titulo);
        col.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(getter.apply(cellData.getValue())));
        col.setPrefWidth(ancho);
        col.setStyle("-fx-alignment: CENTER;");
        return col;
    }

    /**
     * Crea una TableView completa con estilo consistente
     */
    public static <T> TableView<T> crearTabla(double prefHeight, double prefWidth) {
        TableView<T> tabla = new TableView<>();
        tabla.setPrefHeight(prefHeight);
        tabla.setPrefWidth(prefWidth);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return tabla;
    }
}