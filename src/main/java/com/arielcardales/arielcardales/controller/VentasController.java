package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.VentaDAO;
import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.DAO.ProductoVarianteDAO;
import com.arielcardales.arielcardales.Entidades.Venta;
import com.arielcardales.arielcardales.Entidades.Venta.VentaItem;
import com.arielcardales.arielcardales.Entidades.Producto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.text.NumberFormat;

public class VentasController {

    @FXML private TextField txtBuscarCliente;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private Button btnFiltrar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnNuevaVenta;
    @FXML private ToggleButton btnMasVendidos; // ⭐ NUEVO

    @FXML private TableView<Venta> tablaVentas;

    @FXML private StackPane contenedorPrincipal;
    @FXML private VBox contenedorTabla; // ⭐ Contenedor de la tabla
    @FXML private VBox panelLateralMasVendidos;
    @FXML private TableView<VentaDAO.ProductoVendido> tablaMasVendidos;
    @FXML private Label lblPeriodoPanel;

    @FXML private Label lblTotalVentas;
    @FXML private Label lblTotalMonto;
    @FXML private Label lblPromedioVenta;
    @FXML private Label lblVentaMayor;
    @FXML private Label lblVentaMenor;

    @FXML private VBox vboxLoading;
    @FXML private ProgressIndicator progressIndicator;

    private ObservableList<Venta> ventasData;
    private ObservableList<VentaDAO.ProductoVendido> masVendidosData;
    private DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private ProductoDAO productoDAO = new ProductoDAO();
    private ProductoVarianteDAO varianteDAO = new ProductoVarianteDAO();
    private static final int PAGE_SIZE = 10;
    private int currentOffset = 0;
    private int totalVentas = 0;


    private boolean panelVisible = false;

    @FXML
    public void initialize() {
        configurarTablaVentasUnificada();
        configurarTablaMasVendidos();
        configurarFiltros();
        cargarVentas();

        // ⭐ Panel oculto inicialmente (sin animación, solo hidden)
        panelLateralMasVendidos.setVisible(false);
        panelLateralMasVendidos.setManaged(false);
    }

    // ========== MÉTODOS ORIGINALES (sin cambios) ==========

    private void configurarTablaVentasUnificada() {
        String[][] columnas = {
                {"ID", "id", "0.04", "50"},
                {"Fecha", "fechaFormateada", "0.11", "110"},
                {"Cliente", "clienteNombre", "0.11", "100"},
                {"Medio Pago", "medioPago", "0.10", "90"},
                {"Etiqueta", "productosEtiquetas", "0.15", "130"},
                {"Nombre Producto", "productosNombres", "0.35", "280"},
                {"Cant.", "cantidadProductos", "0.05", "50"},
                {"Total", "total", "0.09", "90"}
        };

        List<TableColumn<Venta, ?>> cols = com.arielcardales.arielcardales.Util.Tablas.crearColumnas(columnas);
        tablaVentas.getColumns().setAll(cols);

        // Renderizar columnas personalizadas (código original)
        @SuppressWarnings("unchecked")
        TableColumn<Venta, Object> colEtiquetas = (TableColumn<Venta, Object>)
                cols.stream().filter(c -> c.getId().equals("productosEtiquetas")).findFirst().orElse(null);

        if (colEtiquetas != null) {
            colEtiquetas.setCellFactory(col -> new TableCell<Venta, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        return;
                    }

                    Venta venta = getTableRow().getItem();
                    if (venta.getItems() != null && !venta.getItems().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < venta.getItems().size(); i++) {
                            VentaItem vi = venta.getItems().get(i);
                            if (i > 0) sb.append(", ");
                            sb.append(vi.getProductoEtiqueta()).append(" x").append(vi.getQty());
                        }
                        setText(sb.toString());
                    } else {
                        setText("-");
                    }
                }
            });
        }

        @SuppressWarnings("unchecked")
        TableColumn<Venta, Object> colNombres = (TableColumn<Venta, Object>)
                cols.stream().filter(c -> c.getId().equals("productosNombres")).findFirst().orElse(null);

        if (colNombres != null) {
            colNombres.setCellFactory(col -> new TableCell<Venta, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    // ✅ Validación robusta para evitar errores de formato
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setTooltip(null);
                        return;
                    }

                    Venta venta = getTableRow().getItem();

                    // ✅ Verificar que la venta tiene items cargados
                    if (venta.getItems() == null || venta.getItems().isEmpty()) {
                        setText("-");
                        setTooltip(null);
                        return;
                    }

                    // ✅ Construir texto de productos
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < venta.getItems().size(); i++) {
                        VentaItem vi = venta.getItems().get(i);
                        if (i > 0) sb.append(", ");
                        sb.append(vi.getProductoNombre());
                    }
                    setText(sb.toString());

                    // ✅ Construir tooltip con detalles
                    StringBuilder tooltip = new StringBuilder();
                    NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

                    for (VentaItem vi : venta.getItems()) {
                        // 🔥 Fix crítico: validar que subtotal no sea null antes de formatear
                        BigDecimal subtotal = vi.getSubtotal();
                        String subtotalStr = (subtotal != null)
                                ? formato.format(subtotal)
                                : "$0,00";

                        tooltip.append(String.format("%s - %s (x%d) = %s\n",
                                vi.getProductoEtiqueta(),
                                vi.getProductoNombre(),
                                vi.getQty(),
                                subtotalStr));
                    }

                    setTooltip(new Tooltip(tooltip.toString()));
                }
            });
        }

        ventasData = FXCollections.observableArrayList();
        tablaVentas.setItems(ventasData);
        tablaVentas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configurarFiltros() {
        dpFechaFin.setValue(LocalDate.now());
        dpFechaInicio.setValue(LocalDate.now().minusMonths(1));

        txtBuscarCliente.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 2) {
                buscarPorCliente(newVal);
            } else if (newVal == null || newVal.isEmpty()) {
                cargarVentas();
            }
        });
    }

    private void cargarVentas() {
        mostrarLoading(true);

        Task<List<Venta>> task = new Task<>() {
            @Override
            protected List<Venta> call() throws Exception {
                List<Venta> ventas = VentaDAO.obtenerTodasLasVentas();
                for (Venta v : ventas) {
                    v.setItems(VentaDAO.obtenerItemsDeVenta(v.getId()));
                }
                return ventas;
            }
        };

        task.setOnSucceeded(e -> {
            ventasData.setAll(task.getValue());
            actualizarEstadisticas();

            // 🔄 Actualizar panel si está visible
            if (panelVisible) {
                cargarProductosMasVendidos();
            }

            mostrarLoading(false);
        });

        task.setOnFailed(e -> {
            mostrarError("Error al cargar ventas", task.getException().getMessage());
            task.getException().printStackTrace();
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void filtrarPorFechas() {
        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue();

        if (inicio == null || fin == null) {
            mostrarAlerta("Selecciona un rango de fechas válido");
            return;
        }

        if (inicio.isAfter(fin)) {
            mostrarAlerta("La fecha de inicio debe ser anterior a la fecha de fin");
            return;
        }

        mostrarLoading(true);

        Task<List<Venta>> task = new Task<>() {
            @Override
            protected List<Venta> call() throws Exception {
                List<Venta> ventas = VentaDAO.obtenerVentasPorFecha(inicio, fin);
                for (Venta v : ventas) {
                    v.setItems(VentaDAO.obtenerItemsDeVenta(v.getId()));
                }
                return ventas;
            }
        };

        task.setOnSucceeded(e -> {
            ventasData.setAll(task.getValue());
            actualizarEstadisticas();

            // 🔄 Actualizar panel si está visible
            if (panelVisible) {
                cargarProductosMasVendidos();
            }

            mostrarLoading(false);
        });

        task.setOnFailed(e -> {
            mostrarError("Error al filtrar ventas", task.getException().getMessage());
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    private void buscarPorCliente(String nombreCliente) {
        mostrarLoading(true);

        Task<List<Venta>> task = new Task<>() {
            @Override
            protected List<Venta> call() throws Exception {
                return VentaDAO.buscarVentasPorCliente(nombreCliente);
            }
        };

        task.setOnSucceeded(e -> {
            ventasData.setAll(task.getValue());
            actualizarEstadisticas();
            mostrarLoading(false);
        });

        task.setOnFailed(e -> {
            mostrarError("Error al buscar ventas", task.getException().getMessage());
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscarCliente.clear();
        dpFechaInicio.setValue(LocalDate.now().minusMonths(1));
        dpFechaFin.setValue(LocalDate.now());
        cargarVentas();
    }

    private void actualizarEstadisticas() {
        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue();

        if (inicio == null || fin == null) return;

        Task<VentaDAO.VentaEstadisticas> task = new Task<>() {
            @Override
            protected VentaDAO.VentaEstadisticas call() throws Exception {
                return VentaDAO.obtenerEstadisticas(inicio, fin);
            }
        };

        task.setOnSucceeded(e -> {
            VentaDAO.VentaEstadisticas stats = task.getValue();
            NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

            lblTotalVentas.setText(String.valueOf(stats.getTotalVentas()));
            lblTotalMonto.setText(formato.format(stats.getTotalMonto()));
            lblPromedioVenta.setText(formato.format(stats.getPromedioVenta()));
            lblVentaMayor.setText(formato.format(stats.getVentaMayor()));
            lblVentaMenor.setText(formato.format(stats.getVentaMenor()));
        });

        new Thread(task).start();
    }

    @FXML
    private void abrirNuevaVenta() {
        mostrarAlerta("Usa la vista de Productos para registrar ventas más rápido.\n\nSelecciona un producto y presiona 'Nueva venta'.");
    }

    private void mostrarLoading(boolean mostrar) {
        if (vboxLoading != null && progressIndicator != null) {
            vboxLoading.setVisible(mostrar);
            vboxLoading.setManaged(mostrar);
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void ok(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(3))
                .showConfirm();
    }

    private void error(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(5))
                .showError();
    }


    // ⭐ NUEVA TABLA DE PRODUCTOS MÁS VENDIDOS
    private void configurarTablaMasVendidos() {
        // Columnas optimizadas para panel lateral
        TableColumn<VentaDAO.ProductoVendido, Integer> colPos = new TableColumn<>("#");
        colPos.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(
                cd.getValue().getPosicion()).asObject());
        colPos.setPrefWidth(35);
        colPos.setStyle("-fx-alignment: CENTER;");

        TableColumn<VentaDAO.ProductoVendido, String> colEtiqueta = new TableColumn<>("Etiq.");
        colEtiqueta.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEtiqueta()));
        colEtiqueta.setPrefWidth(60);
        colEtiqueta.setStyle("-fx-alignment: CENTER;");

        TableColumn<VentaDAO.ProductoVendido, String> colNombre = new TableColumn<>("Producto");
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombre()));
        colNombre.setPrefWidth(150);

        TableColumn<VentaDAO.ProductoVendido, Integer> colCantidad = new TableColumn<>("Cant.");
        colCantidad.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(
                cd.getValue().getTotalVendido()).asObject());
        colCantidad.setPrefWidth(50);
        colCantidad.setStyle("-fx-alignment: CENTER;");

        // 📊 Barra visual inline (con colores de tu paleta)
        colCantidad.setCellFactory(col -> new TableCell<VentaDAO.ProductoVendido, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                int max = masVendidosData.isEmpty() ? 1 :
                        masVendidosData.stream()
                                .mapToInt(VentaDAO.ProductoVendido::getTotalVendido)
                                .max().orElse(1);

                double porcentaje = (double) item / max;

                ProgressBar bar = new ProgressBar(porcentaje);
                bar.setPrefWidth(40);
                bar.setMaxHeight(8);
                bar.setStyle("-fx-accent: #b88a52;"); // ⭐ Tu color

                Label lbl = new Label(String.valueOf(item));
                lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #2b2b2b;");

                HBox box = new HBox(5, bar, lbl);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        tablaMasVendidos.getColumns().addAll(colPos, colEtiqueta, colNombre, colCantidad);
        tablaMasVendidos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        masVendidosData = FXCollections.observableArrayList();
        tablaMasVendidos.setItems(masVendidosData);

        // 🖱️ Click en producto → filtrar ventas
        tablaMasVendidos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                VentaDAO.ProductoVendido selected = tablaMasVendidos.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    filtrarVentasPorProducto(selected.getEtiqueta());
                }
            }
        });
    }

    // ⭐ TOGGLE DEL PANEL LATERAL (sin animación, instantáneo)
    @FXML
    private void toggleMasVendidos() {
        if (!panelVisible) {
            mostrarPanelMasVendidos();
        } else {
            ocultarPanelMasVendidos();
        }
    }

    private void mostrarPanelMasVendidos() {
        panelVisible = true;
        panelLateralMasVendidos.setVisible(true);
        panelLateralMasVendidos.setManaged(true); // ⭐ La tabla se achica automáticamente
        btnMasVendidos.setSelected(true);
        cargarProductosMasVendidos();
    }

    private void ocultarPanelMasVendidos() {
        panelVisible = false;
        panelLateralMasVendidos.setVisible(false);
        panelLateralMasVendidos.setManaged(false); // ⭐ La tabla recupera espacio
        btnMasVendidos.setSelected(false);
        cargarVentas(); // Restaurar ventas completas
    }

    // ⭐ CARGA ASÍNCRONA DE PRODUCTOS MÁS VENDIDOS
    private void cargarProductosMasVendidos() {
        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue();

        if (inicio == null || fin == null) {
            inicio = LocalDate.now().minusMonths(1);
            fin = LocalDate.now();
        }

        // ⭐ Actualizar label del período
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblPeriodoPanel.setText(String.format("Período: %s a %s",
                inicio.format(fmt), fin.format(fmt)));

        LocalDate finalInicio = inicio;
        LocalDate finalFin = fin;

        Task<List<VentaDAO.ProductoVendido>> task = new Task<>() {
            @Override
            protected List<VentaDAO.ProductoVendido> call() throws Exception {
                return VentaDAO.obtenerProductosMasVendidos(finalInicio, finalFin, 10);
            }
        };

        task.setOnSucceeded(e -> {
            List<VentaDAO.ProductoVendido> productos = task.getValue();
            masVendidosData.setAll(productos);

            if (productos.isEmpty()) {
                tablaMasVendidos.setPlaceholder(new Label("Sin ventas en el período"));
            }
        });

        task.setOnFailed(e -> {
            error("Error al cargar productos más vendidos");
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    // ⭐ FILTRAR VENTAS POR PRODUCTO SELECCIONADO
    private void filtrarVentasPorProducto(String etiqueta) {
        Task<List<Venta>> task = new Task<>() {
            @Override
            protected List<Venta> call() throws Exception {
                LocalDate inicio = dpFechaInicio.getValue();
                LocalDate fin = dpFechaFin.getValue();
                List<Venta> ventas = VentaDAO.obtenerVentasPorFecha(inicio, fin);

                // Filtrar solo ventas que contengan este producto
                return ventas.stream()
                        .peek(v -> {
                            try {
                                v.setItems(VentaDAO.obtenerItemsDeVenta(v.getId()));
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        })
                        .filter(v -> v.getItems().stream()
                                .anyMatch(item -> item.getProductoEtiqueta().equals(etiqueta)))
                        .toList();
            }
        };

        task.setOnSucceeded(e -> {
            ventasData.setAll(task.getValue());
            ok("📌 Mostrando ventas de: " + etiqueta);
        });

        task.setOnFailed(e -> error("Error al filtrar ventas"));

        new Thread(task).start();
    }

}