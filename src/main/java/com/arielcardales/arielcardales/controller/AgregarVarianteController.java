package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ProductoVarianteDAO;
import com.arielcardales.arielcardales.Entidades.ProductoVariante;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.math.BigDecimal;

public class AgregarVarianteController {

    @FXML private TextField txtColor, txtTalle, txtPrecio, txtCosto, txtStock;
    private long productoBaseId;

    public void setProductoBaseId(long id) {
        this.productoBaseId = id;
    }

    @FXML
    private void guardarVariante() {
        try {
            ProductoVariante v = new ProductoVariante();
            v.setProductoId(productoBaseId);
            v.setColor(txtColor.getText());
            v.setTalle(txtTalle.getText());
            v.setPrecio(new BigDecimal(txtPrecio.getText()));
            v.setCosto(new BigDecimal(txtCosto.getText()));
            v.setStock(Integer.parseInt(txtStock.getText()));
            v.setActive(true);

            new ProductoVarianteDAO().insert(v);
            new Alert(Alert.AlertType.INFORMATION, "Variante agregada correctamente.").showAndWait();
            limpiarCampos();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al guardar variante: " + e.getMessage()).showAndWait();
        }
    }

    private void limpiarCampos() {
        txtColor.clear();
        txtTalle.clear();
        txtPrecio.clear();
        txtCosto.clear();
        txtStock.clear();
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) txtColor.getScene().getWindow();
        stage.close();
    }
}
