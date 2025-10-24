package SORT_PROYECTS.AppInventario.controller;

import SORT_PROYECTS.AppInventario.DAO.DashboardDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.controlsfx.control.Notifications;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    @FXML private Button btnRefrescar;
    @FXML private ComboBox<Periodo> cbPeriodo;

    // Labels de métricas de inventario
    @FXML private Label lblTotalProductos;
    @FXML private Label lblStockBajo;
    @FXML private Label lblSinStock;
    @FXML private Label lblValorTotal;

    // Labels de métricas de ventas
    @FXML private Label lblVentasHoy;
    @FXML private Label lblTotalVentas30;
    @FXML private Label lblPromedioDiario;

    // Tabla de top productos
    @FXML private TableView<ProductoRanking> tablaTopProductos;
    @FXML private TableColumn<ProductoRanking, Integer> colRank;
    @FXML private TableColumn<ProductoRanking, String> colNombre;
    @FXML private TableColumn<ProductoRanking, Integer> colCantidad;
    @FXML private TableColumn<ProductoRanking, String> colGanancia;

    // Gráficos
    @FXML private BarChart<String, Number> chartVentasMensuales;
    @FXML private CategoryAxis xAxisMeses;
    @FXML private NumberAxis yAxisMonto;
    @FXML private PieChart chartVentasCategorias;
    @FXML private Label lblTituloBarChart;
    @FXML private Label lblTituloCategoria;

    @FXML private VBox vboxLoading;

    private final ObservableList<ProductoRanking> topProductosData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    /**
     * Enum para períodos de análisis
     */
    public enum Periodo {
        SIETE_DIAS(7, "7 días"),
        QUINCE_DIAS(15, "15 días"),
        UN_MES(30, "1 mes"),
        SEIS_MESES(180, "6 meses"),
        UN_ANIO(365, "1 año");

        private final int dias;
        private final String texto;

        Periodo(int dias, String texto) {
            this.dias = dias;
            this.texto = texto;
        }

        public int getDias() { return dias; }
        public String getTexto() { return texto; }

        @Override
        public String toString() { return texto; }
    }

    /**
     * Clase auxiliar para representar un producto en el ranking
     */
    public static class ProductoRanking {
        private final SimpleIntegerProperty rank;
        private final SimpleStringProperty etiqueta;
        private final SimpleStringProperty nombre;
        private final SimpleIntegerProperty cantidad;
        private final SimpleStringProperty ganancia;

        public ProductoRanking(int rank, String etiqueta, String nombre, int cantidad, double ganancia) {
            this.rank = new SimpleIntegerProperty(rank);
            this.etiqueta = new SimpleStringProperty(etiqueta);
            this.nombre = new SimpleStringProperty(nombre);
            this.cantidad = new SimpleIntegerProperty(cantidad);
            this.ganancia = new SimpleStringProperty(
                NumberFormat.getCurrencyInstance(new Locale("es", "AR")).format(ganancia)
            );
        }

        public int getRank() { return rank.get(); }
        public String getEtiqueta() { return etiqueta.get(); }
        public String getNombre() { return nombre.get(); }
        public int getCantidad() { return cantidad.get(); }
        public String getGanancia() { return ganancia.get(); }
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarGraficos();
        configurarPeriodo();
        cargarDashboardAsync();
    }

    /**
     * Configura el ComboBox de período y su listener
     */
    private void configurarPeriodo() {
        // Llenar ComboBox con períodos
        cbPeriodo.setItems(FXCollections.observableArrayList(Periodo.values()));

        // Seleccionar "1 mes" por defecto
        cbPeriodo.setValue(Periodo.UN_MES);

        // Listener para cambio de período
        cbPeriodo.valueProperty().addListener((obs, oldPeriodo, newPeriodo) -> {
            if (newPeriodo != null) {
                cargarDashboardAsync();
            }
        });
    }

    /**
     * Configura los gráficos (formato de ejes, colores, etc)
     */
    private void configurarGraficos() {
        // Configurar formato de moneda en eje Y del BarChart
        yAxisMonto.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number number) {
                return currencyFormat.format(number.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Configura las columnas de la tabla de top productos
     */
    private void configurarTabla() {
        colRank.setCellValueFactory(cellData -> cellData.getValue().rank.asObject());
        colNombre.setCellValueFactory(cellData -> cellData.getValue().nombre);
        colCantidad.setCellValueFactory(cellData -> cellData.getValue().cantidad.asObject());
        colGanancia.setCellValueFactory(cellData -> cellData.getValue().ganancia);

        tablaTopProductos.setItems(topProductosData);

        // Hacer que la tabla se adapte al ancho disponible
        tablaTopProductos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Estilo para la columna de ranking
        colRank.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer rank, boolean empty) {
                super.updateItem(rank, empty);
                if (empty || rank == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(rank));
                    // Destacar top 3
                    if (rank == 1) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #FFD700;"); // Oro
                    } else if (rank == 2) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #C0C0C0;"); // Plata
                    } else if (rank == 3) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #CD7F32;"); // Bronce
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    /**
     * Carga todas las métricas del dashboard en segundo plano
     */
    private void cargarDashboardAsync() {
        // Obtener período seleccionado
        Periodo periodo = cbPeriodo.getValue();
        if (periodo == null) {
            periodo = Periodo.UN_MES; // Por defecto
        }
        final int diasPeriodo = periodo.getDias();
        final String textoPeriodo = periodo.getTexto();

        Task<Void> task = new Task<>() {
            private Map<String, Object> metricasInventario;
            private Map<String, Object> metricasVentas;
            private List<DashboardDAO.ProductoVendido> topProductos;
            private List<DashboardDAO.VentaMensual> ventasMensuales;
            private List<DashboardDAO.VentaCategoria> ventasCategorias;

            @Override
            protected Void call() throws Exception {
                // Cargar métricas de inventario
                metricasInventario = DashboardDAO.obtenerMetricasInventario();

                // Cargar métricas de ventas
                metricasVentas = DashboardDAO.obtenerMetricasVentas();

                // Cargar top 5 productos más vendidos
                topProductos = DashboardDAO.obtenerTopProductosVendidos(5);

                // Cargar datos de gráficos con el período seleccionado
                ventasMensuales = DashboardDAO.obtenerVentasPorPeriodo(diasPeriodo);
                ventasCategorias = DashboardDAO.obtenerVentasPorCategoria(diasPeriodo);

                return null;
            }

            @Override
            protected void succeeded() {
                // Actualizar UI con métricas de inventario
                lblTotalProductos.setText(String.valueOf(metricasInventario.get("totalProductos")));
                lblStockBajo.setText(String.valueOf(metricasInventario.get("stockBajo")));
                lblSinStock.setText(String.valueOf(metricasInventario.get("sinStock")));
                lblValorTotal.setText(currencyFormat.format(metricasInventario.get("valorTotal")));

                // Actualizar UI con métricas de ventas
                lblVentasHoy.setText(currencyFormat.format(metricasVentas.get("ventasHoy")));
                lblTotalVentas30.setText(currencyFormat.format(metricasVentas.get("montoTotal")));
                lblPromedioDiario.setText(currencyFormat.format(metricasVentas.get("promedioVentaDiaria")));

                // Actualizar tabla de top productos
                topProductosData.clear();
                int rank = 1;
                for (DashboardDAO.ProductoVendido p : topProductos) {
                    topProductosData.add(new ProductoRanking(
                        rank++,
                        p.getEtiqueta(),
                        p.getNombre(),
                        p.getCantidadVendida(),
                        p.getGanancia()
                    ));
                }

                // Actualizar títulos dinámicos según el período
                String tituloComparacion = diasPeriodo <= 30
                    ? "💰 Comparación diaria - últimos " + textoPeriodo
                    : "💰 Comparación mensual - últimos " + textoPeriodo;
                lblTituloBarChart.setText(tituloComparacion);
                lblTituloCategoria.setText("🏷 Ventas por categoría (últimos " + textoPeriodo + ")");

                // Actualizar label del eje X
                xAxisMeses.setLabel(diasPeriodo <= 30 ? "Día" : "Mes");

                // Actualizar gráficos
                actualizarGraficoVentasMensuales(ventasMensuales);
                actualizarGraficoVentasCategorias(ventasCategorias);

                ocultarLoading();
            }

            @Override
            protected void failed() {
                ocultarLoading();
                Notifications.create()
                    .title("Error")
                    .text("No se pudieron cargar las métricas del dashboard")
                    .showError();
                getException().printStackTrace();
            }
        };

        mostrarLoading();
        new Thread(task).start();
    }

    /**
     * Actualiza el gráfico de barras con ventas mensuales
     */
    private void actualizarGraficoVentasMensuales(List<DashboardDAO.VentaMensual> ventas) {
        chartVentasMensuales.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ventas");

        for (DashboardDAO.VentaMensual venta : ventas) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(venta.getMes(), venta.getMonto());
            series.getData().add(data);

            // Agregar tooltip a cada barra
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip tooltip = new Tooltip(
                        venta.getMes() + "\n" +
                        "Ventas: " + venta.getCantidad() + "\n" +
                        "Total: " + currencyFormat.format(venta.getMonto())
                    );
                    Tooltip.install(newNode, tooltip);
                }
            });
        }

        chartVentasMensuales.getData().add(series);
    }

    /**
     * Actualiza el gráfico circular con ventas por categoría
     */
    private void actualizarGraficoVentasCategorias(List<DashboardDAO.VentaCategoria> ventas) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (DashboardDAO.VentaCategoria venta : ventas) {
            pieData.add(new PieChart.Data(venta.getCategoria(), venta.getMonto()));
        }

        chartVentasCategorias.setData(pieData);
    }

    /**
     * Refresca todas las métricas del dashboard
     */
    @FXML
    private void refrescarDashboard() {
        cargarDashboardAsync();
        Notifications.create()
            .text("Refrescando dashboard...")
            .showInformation();
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
