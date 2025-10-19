package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.DashboardDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.Notifications;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    @FXML private Button btnRefrescar;

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

    @FXML private StackPane stackLoading;

    private final ObservableList<ProductoRanking> topProductosData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

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
        cargarDashboardAsync();
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
        Task<Void> task = new Task<>() {
            private Map<String, Object> metricasInventario;
            private Map<String, Object> metricasVentas;
            private List<DashboardDAO.ProductoVendido> topProductos;

            @Override
            protected Void call() throws Exception {
                // Cargar métricas de inventario
                metricasInventario = DashboardDAO.obtenerMetricasInventario();

                // Cargar métricas de ventas
                metricasVentas = DashboardDAO.obtenerMetricasVentas();

                // Cargar top 5 productos más vendidos
                topProductos = DashboardDAO.obtenerTopProductosVendidos(5);

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
            stackLoading.setVisible(true);
            stackLoading.setManaged(true);
            stackLoading.toFront();
        });
    }

    /**
     * Oculta el indicador de carga
     */
    private void ocultarLoading() {
        Platform.runLater(() -> {
            stackLoading.setVisible(false);
            stackLoading.setManaged(false);
        });
    }
}
