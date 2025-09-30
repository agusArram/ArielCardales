package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.*;
import com.arielcardales.arielcardales.Entidades.Categoria;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Entidades.Unidad;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class AgregarProductoController {

    @FXML private TextField txtEtiqueta;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtCosto;
    @FXML private TextField txtStock;
    @FXML private CheckBox chkActivo;

    private Map<String, Long> categorias;
    private Map<String, Long> unidades;
    private final ProductoDAO productoDAO = new ProductoDAO();

    @FXML
    public void initialize() {
        categorias = new CategoriaDAO().mapNombreId();
        unidades = new UnidadDAO().mapNombreId();

        cmbCategoria.setItems(FXCollections.observableArrayList(categorias.keySet()));
        cmbUnidad.setItems(FXCollections.observableArrayList(unidades.keySet()));

        // 👇 Nueva lógica para sugerir etiqueta
        String ultima = productoDAO.getUltimaEtiqueta();
        if (ultima != null && ultima.matches("p\\d+")) {
            int num = Integer.parseInt(ultima.substring(1)); // saca el número
            String sugerida = "p" + String.format("%03d", num + 1); // ej: p043
            txtEtiqueta.setPromptText(sugerida);
        } else {
            txtEtiqueta.setPromptText("p001"); // si no hay productos aún
        }
    }





    @FXML
    private void guardar() {
        try {
            Producto p = new Producto();
            p.setEtiqueta(txtEtiqueta.getText());
            p.setNombre(txtNombre.getText());
            p.setDescripcion(txtDescripcion.getText());

            if (productoDAO.existsByEtiqueta(p.getEtiqueta())) {
                new Alert(Alert.AlertType.ERROR, "La etiqueta ya existe: " + p.getEtiqueta()).showAndWait();
                return;
            }


            // IDs seleccionados
            p.setCategoriaId(categorias.get(cmbCategoria.getValue()));
            p.setUnidadId(unidades.get(cmbUnidad.getValue()));

            p.setPrecio(new BigDecimal(txtPrecio.getText()));
            p.setCosto(new BigDecimal(txtCosto.getText()));
            p.setStockOnHand(Integer.parseInt(txtStock.getText()));
            p.setActive(chkActivo.isSelected());

            String etiqueta = txtEtiqueta.getText();
            if (etiqueta == null || etiqueta.isBlank()) {
                etiqueta = txtEtiqueta.getPromptText(); // usa sugerida
            }
            p.setEtiqueta(etiqueta);


            Long id = productoDAO.insert(p);
            System.out.println("✅ Producto agregado con ID: " + id);

            cerrar();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }
}
