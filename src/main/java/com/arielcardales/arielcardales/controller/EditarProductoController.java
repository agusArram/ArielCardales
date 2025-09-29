package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.Entidades.Producto;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class EditarProductoController {

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtCategoria;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;

    private Producto producto;

    public void setProducto(Producto producto) {
        this.producto = producto;
        txtNombre.setText(producto.getNombre());
        txtDescripcion.setText(producto.getDescripcion());
        txtCategoria.setText(producto.getCategoria());
        txtPrecio.setText(String.valueOf(producto.getPrecio()));
        txtStock.setText(String.valueOf(producto.getStockOnHand()));
    }

    @FXML
    private void guardarCambios() {
        producto.setNombre(txtNombre.getText());
        producto.setDescripcion(txtDescripcion.getText());
        producto.setCategoria(txtCategoria.getText());

        // Usar BigDecimal en lugar de Double
        try {
            producto.setPrecio(new BigDecimal(txtPrecio.getText()));
        } catch (NumberFormatException e) {
            System.err.println("⚠ Error: precio inválido");
            return;
        }

        producto.setStockOnHand(Integer.parseInt(txtStock.getText()));

        new ProductoDAO().update(producto); // Guarda en DB

        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

}
