package SORT_PROYECTS.AppInventario.controller;

import SORT_PROYECTS.AppInventario.DAO.CategoriaDAO;
import SORT_PROYECTS.AppInventario.DAO.RentabilidadDAO;
import SORT_PROYECTS.AppInventario.Entidades.Categoria;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.controlsfx.control.Notifications;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RentabilidadController {

    @FXML private ComboBox<PeriodoItem> comboPeriodo;
    @FXML private ComboBox<Categoria> comboCategoria;
    @FXML private Button btnAnalizar;

    // Labels de métricas
    @FXML private Label lblMargenPromedio;
    @FXML private Label lblGananciaTotal;
    @FXML private Label lblProductoMasRentable;
    @FXML private Label lblCategoriaMasRentable;

    // Tabla de análisis
    @FXML private TableView<ProductoRentabilidadRow> tablaRentabilidad;
    @FXML private TableColumn<ProductoRentabilidadRow, String> colProducto;
    @FXML private TableColumn<ProductoRentabilidadRow, String> colCategoria;
    @FXML private TableColumn<ProductoRentabilidadRow, String> colPrecio;
    @FXML private TableColumn<ProductoRentabilidadRow, String> colCosto;
    @FXML private TableColumn<ProductoRentabilidadRow, String> colMargen;
    @FXML private TableColumn<ProductoRentabilidadRow, String> colGananciaUnit;
    @FXML private TableColumn<ProductoRentabilidadRow, Integer> colCantidad;
    @FXML private TableColumn<ProductoRentabilidadRow, String> colGananciaTotal;

    @FXML private VBox vboxLoading;

    private final ObservableList<ProductoRentabilidadRow> rentabilidadData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance(new Locale("es", "AR"));

    /**
     * Clase para items de período en el combo
     */
    public static class PeriodoItem {
        private final String nombre;
        private final int dias;

        public PeriodoItem(String nombre, int dias) {
            this.nombre = nombre;
            this.dias = dias;
        }

        public int getDias() { return dias; }

        @Override
        public String toString() { return nombre; }
    }

    /**
     * Clase para representar una fila en la tabla de rentabilidad
     */
    public static class ProductoRentabilidadRow {
        private final SimpleStringProperty producto;
        private final SimpleStringProperty categoria;
        private final SimpleStringProperty precio;
        private final SimpleStringProperty costo;
        private final SimpleStringProperty margen;
        private final SimpleStringProperty gananciaUnit;
        private final SimpleIntegerProperty cantidad;
        private final SimpleStringProperty gananciaTotal;

        public ProductoRentabilidadRow(String producto, String categoria, double precio, double costo,
                                      double margenPct, double gananciaUnit, int cantidad, double gananciaTotal) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

            this.producto = new SimpleStringProperty(producto);
            this.categoria = new SimpleStringProperty(categoria);
            this.precio = new SimpleStringProperty(currencyFormat.format(precio));
            this.costo = new SimpleStringProperty(currencyFormat.format(costo));
            this.margen = new SimpleStringProperty(String.format("%.1f%%", margenPct));
            this.gananciaUnit = new SimpleStringProperty(currencyFormat.format(gananciaUnit));
            this.cantidad = new SimpleIntegerProperty(cantidad);
            this.gananciaTotal = new SimpleStringProperty(currencyFormat.format(gananciaTotal));
        }

        public String getProducto() { return producto.get(); }
        public String getCategoria() { return categoria.get(); }
        public String getPrecio() { return precio.get(); }
        public String getCosto() { return costo.get(); }
        public String getMargen() { return margen.get(); }
        public String getGananciaUnit() { return gananciaUnit.get(); }
        public int getCantidad() { return cantidad.get(); }
        public String getGananciaTotal() { return gananciaTotal.get(); }
    }

    @FXML
    public void initialize() {
        percentFormat.setMinimumFractionDigits(1);
        percentFormat.setMaximumFractionDigits(1);

        configurarCombos();
        configurarTabla();
        cargarAnalisisAsync();
    }

    /**
     * Configura los combos de filtros
     */
    private void configurarCombos() {
        // Combo de períodos
        comboPeriodo.setItems(FXCollections.observableArrayList(
            new PeriodoItem("Últimos 7 días", 7),
            new PeriodoItem("Últimos 30 días", 30),
            new PeriodoItem("Últimos 90 días", 90),
            new PeriodoItem("Último año", 365)
        ));
        comboPeriodo.getSelectionModel().select(1); // Por defecto 30 días

        // Combo de categorías
        Task<List<Categoria>> task = new Task<>() {
            @Override
            protected List<Categoria> call() {
                CategoriaDAO dao = new CategoriaDAO();
                return dao.findAll();
            }

            @Override
            protected void succeeded() {
                List<Categoria> categorias = getValue();
                ObservableList<Categoria> items = FXCollections.observableArrayList(categorias);
                comboCategoria.setItems(items);

                // Agregar opción "Todas las categorías" al inicio
                Categoria todasCategorias = new Categoria();
                todasCategorias.setId(-1); // -1 para indicar "todas"
                todasCategorias.setNombre("Todas las categorías");
                comboCategoria.getItems().add(0, todasCategorias);
                comboCategoria.getSelectionModel().select(0);
            }
        };
        new Thread(task).start();
    }

    /**
     * Configura las columnas de la tabla
     */
    private void configurarTabla() {
        colProducto.setCellValueFactory(cellData -> cellData.getValue().producto);
        colCategoria.setCellValueFactory(cellData -> cellData.getValue().categoria);
        colPrecio.setCellValueFactory(cellData -> cellData.getValue().precio);
        colCosto.setCellValueFactory(cellData -> cellData.getValue().costo);
        colMargen.setCellValueFactory(cellData -> cellData.getValue().margen);
        colGananciaUnit.setCellValueFactory(cellData -> cellData.getValue().gananciaUnit);
        colCantidad.setCellValueFactory(cellData -> cellData.getValue().cantidad.asObject());
        colGananciaTotal.setCellValueFactory(cellData -> cellData.getValue().gananciaTotal);

        tablaRentabilidad.setItems(rentabilidadData);
        tablaRentabilidad.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Colorear margen según valor
        colMargen.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String margen, boolean empty) {
                super.updateItem(margen, empty);
                if (empty || margen == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(margen);
                    try {
                        double valor = Double.parseDouble(margen.replace("%", "").replace(",", "."));
                        if (valor >= 50) {
                            setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;"); // Verde
                        } else if (valor >= 30) {
                            setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;"); // Naranja
                        } else {
                            setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;"); // Rojo
                        }
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });
    }

    /**
     * Carga el análisis de rentabilidad en segundo plano
     */
    private void cargarAnalisisAsync() {
        PeriodoItem periodo = comboPeriodo.getValue();
        Categoria categoria = comboCategoria.getValue();

        if (periodo == null) return;

        int dias = periodo.getDias();
        Integer categoriaId = (categoria != null && categoria.getId() != -1) ? Integer.valueOf((int) categoria.getId()) : null;

        Task<Void> task = new Task<>() {
            private Map<String, Object> metricas;
            private Map<String, Object> productoMasRentable;
            private Map<String, Object> categoriaMasRentable;
            private List<RentabilidadDAO.ProductoRentabilidad> productos;

            @Override
            protected Void call() {
                metricas = RentabilidadDAO.obtenerMetricasRentabilidad(dias);
                productoMasRentable = RentabilidadDAO.obtenerProductoMasRentable(dias);
                categoriaMasRentable = RentabilidadDAO.obtenerCategoriaMasRentable(dias);
                productos = RentabilidadDAO.obtenerAnalisisProductos(dias, categoriaId);
                return null;
            }

            @Override
            protected void succeeded() {
                // Actualizar métricas
                double margenPromedio = (double) metricas.get("margenPromedio");
                lblMargenPromedio.setText(String.format("%.1f%%", margenPromedio));
                lblGananciaTotal.setText(currencyFormat.format(metricas.get("gananciaTotal")));

                String productoNombre = (String) productoMasRentable.get("nombre");
                double productoGanancia = (double) productoMasRentable.get("gananciaTotal");
                lblProductoMasRentable.setText(productoNombre + " (" + currencyFormat.format(productoGanancia) + ")");

                String categoriaNombre = (String) categoriaMasRentable.get("nombre");
                double categoriaGanancia = (double) categoriaMasRentable.get("gananciaTotal");
                lblCategoriaMasRentable.setText(categoriaNombre + " (" + currencyFormat.format(categoriaGanancia) + ")");

                // Actualizar tabla
                rentabilidadData.clear();
                for (RentabilidadDAO.ProductoRentabilidad p : productos) {
                    rentabilidadData.add(new ProductoRentabilidadRow(
                        p.getNombre(),
                        p.getCategoria(),
                        p.getPrecioVenta(),
                        p.getCosto(),
                        p.getMargenPorcentaje(),
                        p.getGananciaUnitaria(),
                        p.getCantidadVendida(),
                        p.getGananciaTotal()
                    ));
                }

                ocultarLoading();
            }

            @Override
            protected void failed() {
                ocultarLoading();
                Notifications.create()
                    .title("Error")
                    .text("No se pudo cargar el análisis de rentabilidad")
                    .showError();
                getException().printStackTrace();
            }
        };

        mostrarLoading();
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Maneja el evento de analizar con los filtros seleccionados
     */
    @FXML
    private void analizarRentabilidad() {
        cargarAnalisisAsync();
    }

    /**
     * Muestra el indicador de carga
     */
    private void mostrarLoading() {
        Platform.runLater(() -> {
            vboxLoading.setVisible(true);
            vboxLoading.setManaged(true);
            vboxLoading.toFront();
        });
    }

    /**
     * Oculta el indicador de carga
     */
    private void ocultarLoading() {
        Platform.runLater(() -> {
            vboxLoading.setVisible(false);
            vboxLoading.setManaged(false);
        });
    }
}
