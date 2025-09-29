package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.ExportadorExcel;
import com.arielcardales.arielcardales.Util.ExportadorPDF;
import com.arielcardales.arielcardales.Util.Tablas;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class AppController {

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TextField txtBuscarEtiqueta;
    @FXML private ToggleButton btnNombre;
    @FXML private ToggleButton btnCategoria;
    @FXML private ToggleButton btnEtiqueta;

    @FXML private ToggleGroup grupoBusqueda;
    private ObservableList<Producto> listaProductos;

    @FXML

    /*
    Qué hace esto
        Carga de columnas → como ya tenías con Tablas.crearColumnas.
        listaProductos → productos desde ProductoDAO.
        FilteredList → aplica filtro en base al TextField y el criterio del ComboBox.
        SortedList → mantiene orden de columnas.
        Stock bajo → sigue pintando filas con low-stock.
        comboBuscarPor → arranca en "Nombre" por defecto.
     */

    public void initialize() {
        String[][] columnas = {
                {"Etiqueta", "etiqueta", "0.08", "60"},
                {"Nombre", "nombre", "0.20", "140"},
                {"Descripción", "descripcion", "0.35", "280"},
                {"Categoría", "categoria", "0.12", "100"},
                {"Precio", "precio", "0.15", "100"},
                {"Stock", "stockOnHand", "0.10", "70"}
        };

        // Inicializar ToggleGroup
        grupoBusqueda = new ToggleGroup();
        btnNombre.setToggleGroup(grupoBusqueda);
        btnCategoria.setToggleGroup(grupoBusqueda);
        btnEtiqueta.setToggleGroup(grupoBusqueda);
        grupoBusqueda.selectToggle(btnNombre);

        // Columnas
        List<TableColumn<Producto, ?>> cols = Tablas.crearColumnas(columnas);
        tablaProductos.getColumns().setAll(cols);
        for (TableColumn<Producto, ?> col : tablaProductos.getColumns()) {
            Object ud = col.getUserData();
            if (ud instanceof Double peso) {
                col.prefWidthProperty().bind(tablaProductos.widthProperty().multiply(peso));
            }
        }

        // Datos
        listaProductos = FXCollections.observableArrayList(new ProductoDAO().findAll());
        FilteredList<Producto> filtrados = new FilteredList<>(listaProductos, p -> true);

        // 🔑 función de filtrado centralizada
        Runnable aplicarFiltro = () -> {
            String filtro = txtBuscarEtiqueta.getText() == null ? "" : txtBuscarEtiqueta.getText().trim().toLowerCase();

            filtrados.setPredicate(prod -> {
                if (filtro.isBlank()) return true;

                // 🔎 Detectar si el filtro parece una etiqueta: empieza con "p" y números
                boolean pareceEtiqueta = filtro.matches("p\\d+");

                if (pareceEtiqueta) {
                    return prod.getEtiqueta() != null && prod.getEtiqueta().toLowerCase().contains(filtro);
                }

                if (btnNombre.isSelected()) {
                    return prod.getNombre() != null && prod.getNombre().toLowerCase().contains(filtro);
                } else if (btnCategoria.isSelected()) {
                    return prod.getCategoria() != null && prod.getCategoria().toLowerCase().contains(filtro);
                } else if (btnEtiqueta.isSelected()) {
                    return prod.getEtiqueta() != null && prod.getEtiqueta().toLowerCase().contains(filtro);
                } else {
                    return true;
                }
            });
        };


        // Listener en el texto
        txtBuscarEtiqueta.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltro.run());

        // 🔑 Listener en el grupo de botones
        grupoBusqueda.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> aplicarFiltro.run());

        // SortedList
        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(ordenados);

        // Resaltar stock bajo
        tablaProductos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("low-stock");
                if (!empty && item != null && item.getStockOnHand() <= 3) {
                    getStyleClass().add("low-stock");
                }
            }
        });
    }

    //EXPORTAR COSAS
    @FXML
    private void exportarExcel() {
        ExportadorExcel.exportar(tablaProductos.getItems(), "productos.xlsx");
    }

    @FXML
    private void exportarPDF() {
        ExportadorPDF.exportar(tablaProductos.getItems(), "productos.pdf");
    }

}
