package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.*;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Entidades.ProductoVariante;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

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
    @FXML private CheckBox chkTieneVariantes;



    private Map<String, Long> categorias;
    private Map<String, Long> unidades;
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProductoVarianteDAO varianteDAO = new ProductoVarianteDAO();

    private Producto productoBase; // producto padre

    @FXML
    public void initialize() {
        categorias = new CategoriaDAO().mapNombreId();
        unidades = new UnidadDAO().mapNombreId();

        cmbCategoria.setItems(FXCollections.observableArrayList(categorias.keySet()));
        cmbUnidad.setItems(FXCollections.observableArrayList(unidades.keySet()));

        // 🔹 Generar automáticamente la etiqueta sugerida
        String ultima = productoDAO.getUltimaEtiqueta();
        if (ultima != null && ultima.matches("p\\d+")) {
            int num = Integer.parseInt(ultima.substring(1)); // saca el número
            String sugerida = "p" + String.format("%03d", num + 1); // ej: p043
            txtEtiqueta.setText(sugerida); // ✅ ahora se muestra directamente
        } else {
            txtEtiqueta.setText("p001");
        }
    }



    // Guarda producto base (padre)
    @FXML
    private void guardar() {
        try {
            Producto p = new Producto();

            // Etiqueta
            String etiqueta = txtEtiqueta.getText();
            if (etiqueta == null || etiqueta.isBlank()) {
                etiqueta = txtEtiqueta.getPromptText();
            }
            p.setEtiqueta(etiqueta);

            // Validar duplicado
            if (productoDAO.existsByEtiqueta(etiqueta)) {
                mostrarError("La etiqueta ya existe: " + etiqueta);
                return;
            }

            // Datos del producto
            p.setNombre(txtNombre.getText());
            p.setDescripcion(txtDescripcion.getText());
            p.setCategoriaId(categorias.get(cmbCategoria.getValue()));
            p.setUnidadId(unidades.get(cmbUnidad.getValue()));
            p.setPrecio(new BigDecimal(txtPrecio.getText()));
            p.setCosto(new BigDecimal(txtCosto.getText()));
            p.setStockOnHand(Integer.parseInt(txtStock.getText()));
            p.setActive(chkActivo.isSelected());

            productoDAO.insert(p);

            mostrarOk("✅ Producto agregado correctamente.");

            // 🔄 Notificar al inventario que se recargue
            // Se ejecuta al cerrar la ventana, así la lista se actualiza automáticamente
            Stage stage = (Stage) txtNombre.getScene().getWindow();
            stage.setUserData(true); // usamos un flag simple
            stage.close();

        } catch (Exception e) {
            mostrarError("Error al guardar producto: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void generarEtiqueta() {
        // Simula la lógica que tengas (podés reemplazar por la de la BD o DAO)
        String proximoCodigo = "p" + String.format("%03d", obtenerSiguienteNumero());
        txtEtiqueta.setText(proximoCodigo);
    }

    private int obtenerSiguienteNumero() {
        // Ejemplo fijo (cambiar por el método real que consulte tu BD o DAO)
        return 52;
    }

    // Finalizar carga (cerrar ventana)
    @FXML
    private void finalizar() {
        cerrar();
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    // --- Métodos auxiliares de alertas ---
    private void mostrarOk(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void mostrarError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private void mostrarInfo(String msg) {
        new Alert(Alert.AlertType.CONFIRMATION, msg).showAndWait();
    }
}
