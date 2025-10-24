package SORT_PROYECTS.AppInventario.controller;

import SORT_PROYECTS.AppInventario.DAO.ProductoVarianteDAO;
import SORT_PROYECTS.AppInventario.Entidades.ProductoVariante;
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
            mostrarOk("Variante agregada correctamente.");
            limpiarCampos();
        } catch (Exception e) {
            mostrarError("Error al guardar variante: " + e.getMessage());
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

    // --- Métodos auxiliares de notificaciones ---
    private void mostrarOk(String msg) {
        org.controlsfx.control.Notifications.create()
                .title("✅ Éxito")
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(3))
                .showConfirm();
    }

    private void mostrarError(String msg) {
        org.controlsfx.control.Notifications.create()
                .title("❌ Error")
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(5))
                .showError();
    }
}
