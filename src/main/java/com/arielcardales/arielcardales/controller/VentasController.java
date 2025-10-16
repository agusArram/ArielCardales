package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.VentaDAO;
import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.DAO.ProductoVarianteDAO;
import com.arielcardales.arielcardales.Entidades.Venta;
import com.arielcardales.arielcardales.Entidades.Venta.VentaItem;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.stage.Screen;
import org.controlsfx.control.Notifications;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.*;
import java.text.NumberFormat;

public class VentasController {

    @FXML private TextField txtBuscarCliente;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private Button btnFiltrar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnNuevaVenta;
    @FXML private ToggleButton btnMasVendidos;
    @FXML private Label lblCargando; // Puede ser null si no está en el FXML

    @FXML private TableView<Venta> tablaVentas;

    @FXML private StackPane contenedorPrincipal;
    @FXML private VBox contenedorTabla; //  Contenedor de la tabla
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
    private int PAGE_SIZE; // Ya no es final ni static
    private int currentOffset = 0;
    private int totalVentas = 0;
    private boolean scrollListenerInstalado = false;
    private boolean cargandoPagina = false;
    private boolean cargaAutomaticaCompletada = false; // ✅ NUEVO

    // ✅ CAMBIAR estas constantes
    private static final int CARGA_INICIAL = 10;      // Primera carga (inmediata)
    private static final int CARGA_INCREMENTAL = 10;  // Cargas automáticas
    private static final int MINIMO_PARA_SCROLL = 35; // Límite antes de activar scroll
    private static final int PAGE_SIZE_SCROLL = 15;   // Tamaño al hacer scroll manual
    private boolean panelVisible = false;

    @FXML
    public void initialize() {
        System.out.println("🚀 Iniciando VentasController...");

        // Debug de elementos FXML
        System.out.println("   - tablaVentas: " + (tablaVentas != null ? "OK" : "NULL"));
        System.out.println("   - vboxLoading: " + (vboxLoading != null ? "OK" : "NULL"));
        System.out.println("   - progressIndicator: " + (progressIndicator != null ? "OK" : "NULL"));
        System.out.println("   - lblCargando: " + (lblCargando != null ? "OK" : "NULL"));

        ventasData = FXCollections.observableArrayList();

        // Placeholders para stats
        lblTotalVentas.setText("...");
        lblTotalMonto.setText("...");
        lblPromedioVenta.setText("...");
        lblVentaMayor.setText("...");
        lblVentaMenor.setText("...");

        configurarTablaVentasUnificada();
        configurarTablaMasVendidos();
        configurarFiltros();
        cargarVentasInicial();

        panelLateralMasVendidos.setVisible(false);
        panelLateralMasVendidos.setManaged(false);

        System.out.println("✅ VentasController inicializado");
    }

    /**
     * Calcula el PAGE_SIZE óptimo según resolución de pantalla
     * Objetivo: Llenar la pantalla + tener suficientes para scroll
     */
    private int calcularPageSizeOptimo() {
        try {
            // Obtener altura de la pantalla principal
            double altoPantalla = Screen.getPrimary().getBounds().getHeight();

            // Estimar filas visibles (considerando headers, toolbars, etc)
            // Altura promedio por fila: ~35-40px
            // Espacio disponible para tabla: ~70-80% de la pantalla
            double espacioDisponible = altoPantalla * 0.75; // 75% de la pantalla
            int filasVisibles = (int)(espacioDisponible / 38); // ~38px por fila

            // Cargar el doble de lo visible para asegurar scroll
            int pageSize = filasVisibles * 2;

            // Límites: mínimo 30, máximo 100
            pageSize = Math.max(30, Math.min(pageSize, 100));

            System.out.println("🖥️ Resolución pantalla: " + altoPantalla + "px");
            System.out.println("📊 Filas visibles estimadas: " + filasVisibles);
            System.out.println("📦 PAGE_SIZE ajustado: " + pageSize);

            return pageSize;

        } catch (Exception e) {
            System.err.println("⚠️ Error calculando PAGE_SIZE, usando default 50");
            e.printStackTrace();
            return 50; // Fallback seguro
        }
    }

    // ========== MÉTODOS ORIGINALES (sin cambios) ==========

    // ===============================================
// REEMPLAZAR configurarTablaVentasUnificada()
// Versión basada en tu código original que funcionaba
// ===============================================
    private void configurarTablaVentasUnificada() {
        String[][] columnas = {
                {"ID", "id", "0.04", "50"},
                {"Fecha", "fechaFormateada", "0.11", "110"},
                {"Cliente", "clienteNombre", "0.11", "100"},
                {"Medio Pago", "medioPago", "0.10", "90"},
                {"Etiqueta", "productosEtiquetas", "0.15", "130"},
                {"Nombre Producto", "productosNombres", "0.35", "280"},
                {"Cant.", "cantidadTotal", "0.05", "50"}, // ✅ CAMBIADO: cantidadProductos → cantidadTotal
                {"Total", "total", "0.09", "90"}
        };

        List<TableColumn<Venta, ?>> cols = com.arielcardales.arielcardales.Util.Tablas.crearColumnas(columnas);
        tablaVentas.getColumns().setAll(cols);

        // ✅ Configurar ETIQUETAS (solo setCellFactory, sin setCellValueFactory)
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

        // ✅ Configurar NOMBRES (solo setCellFactory, sin setCellValueFactory)
        @SuppressWarnings("unchecked")
        TableColumn<Venta, Object> colNombres = (TableColumn<Venta, Object>)
                cols.stream().filter(c -> c.getId().equals("productosNombres")).findFirst().orElse(null);

        if (colNombres != null) {
            colNombres.setCellFactory(col -> new TableCell<Venta, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setTooltip(null);
                        return;
                    }

                    Venta venta = getTableRow().getItem();

                    if (venta.getItems() == null || venta.getItems().isEmpty()) {
                        setText("-");
                        setTooltip(null);
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < venta.getItems().size(); i++) {
                        VentaItem vi = venta.getItems().get(i);
                        if (i > 0) sb.append(", ");
                        sb.append(vi.getProductoNombre());
                    }
                    setText(sb.toString());

                    StringBuilder tooltip = new StringBuilder();
                    NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

                    for (VentaItem vi : venta.getItems()) {
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
                cargarVentasInicial();
            }
        });
    }

    private void cargarVentasCompleta() {
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


    private void cargarVentasInicial() {
        ventasData.clear();
        currentOffset = 0;
        scrollListenerInstalado = false;
        cargaAutomaticaCompletada = false; // ✅ Reset
        mostrarLoading(true);

        Task<List<Venta>> task = new Task<>() {
            @Override
            protected List<Venta> call() throws Exception {
                totalVentas = VentaDAO.contarVentas();
                // ✅ Primera carga: solo 10 ventas
                List<Venta> ventas = VentaDAO.obtenerVentasPaginadas(currentOffset, CARGA_INICIAL);

                for (Venta v : ventas) {
                    try {
                        v.setItems(VentaDAO.obtenerItemsDeVenta(v.getId()));
                    } catch (Exception ex) {
                        System.err.println("Error cargando items de venta " + v.getId() + ": " + ex.getMessage());
                        v.setItems(new java.util.ArrayList<>());
                    }
                }

                return ventas;
            }
        };

        task.setOnSucceeded(e -> {
            List<Venta> primeras = task.getValue();
            ventasData.addAll(primeras);
            currentOffset = CARGA_INICIAL;
            mostrarLoading(false);

            System.out.println("⚡ Carga inicial: " + primeras.size() + " ventas (total: " + totalVentas + ")");

            actualizarIndicadorCarga(); // ✅ AGREGAR AQUÍ

            Platform.runLater(this::cargarEstadisticasEnSegundoPlano);
            Platform.runLater(this::cargarProgresivoAutomatico);
        });


        task.setOnFailed(e -> {
            mostrarError("Error al cargar ventas", task.getException().getMessage());
            task.getException().printStackTrace();
            mostrarLoading(false);
        });

        new Thread(task).start();
    }

    /**
     * Carga ventas de forma progresiva hasta alcanzar MINIMO_PARA_SCROLL
     * Luego instala el scroll listener para carga manual
     */
    private void cargarProgresivoAutomatico() {
        // Si ya tenemos suficientes ventas, instalar scroll y terminar
        if (ventasData.size() >= MINIMO_PARA_SCROLL || ventasData.size() >= totalVentas) {
            System.out.println("✅ Carga automática completada: " + ventasData.size() + " ventas");

            actualizarIndicadorCarga(); // (oculta el label)


            cargaAutomaticaCompletada = true;
            Platform.runLater(this::instalarScrollListener);
            return;
        }

        // Si ya hay una carga en progreso, esperar
        if (cargandoPagina) {
            Platform.runLater(() -> {
                try {
                    Thread.sleep(100);
                    cargarProgresivoAutomatico();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            });
            return;
        }

        cargandoPagina = true;

        Task<List<Venta>> task = new Task<>() {
            @Override
            protected List<Venta> call() throws Exception {
                // Cargar siguiente lote
                int cantidadACargar = Math.min(CARGA_INCREMENTAL, totalVentas - currentOffset);
                List<Venta> ventas = VentaDAO.obtenerVentasPaginadas(currentOffset, cantidadACargar);

                for (Venta v : ventas) {
                    try {
                        v.setItems(VentaDAO.obtenerItemsDeVenta(v.getId()));
                    } catch (Exception ex) {
                        System.err.println("Error cargando items de venta " + v.getId() + ": " + ex.getMessage());
                        v.setItems(new java.util.ArrayList<>());
                    }
                }

                return ventas;
            }
        };

        task.setOnSucceeded(e -> {
            List<Venta> nuevas = task.getValue();

            if (!nuevas.isEmpty()) {
                ventasData.addAll(nuevas);
                currentOffset += nuevas.size();
                System.out.println("📦 Carga progresiva: +" + nuevas.size() + " ventas (total: " + ventasData.size() + "/" + totalVentas + ")");

                actualizarIndicadorCarga(); // ✅ AGREGAR AQUÍ

            }

            cargandoPagina = false;

            // Continuar cargando automáticamente con un pequeño delay
            Platform.runLater(() -> {
                try {
                    Thread.sleep(50); // 50ms de pausa entre cargas (imperceptible)
                    cargarProgresivoAutomatico();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            });
        });

        task.setOnFailed(e -> {
            System.err.println("❌ Error en carga progresiva:");
            task.getException().printStackTrace();
            cargandoPagina = false;
            cargaAutomaticaCompletada = true;
            Platform.runLater(this::instalarScrollListener);
        });

        new Thread(task).start();


    }



    private void cargarEstadisticasEnSegundoPlano() {
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

            System.out.println("✅ Estadísticas cargadas");
        });

        task.setOnFailed(e -> {
            System.err.println("⚠️ Error al cargar estadísticas (no crítico)");
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    /**
     * Carga la siguiente página (si hay más) — evita llamadas simultáneas con cargandoPagina.
     */
    private void cargarMasVentas() {
        if (!cargaAutomaticaCompletada) {
            System.out.println("⏸️ Carga automática aún en progreso...");
            return;
        }

        if (cargandoPagina) {
            System.out.println("⏳ Ya hay una carga en progreso...");
            return;
        }

        if (ventasData.size() >= totalVentas) {
            System.out.println("✅ Todas las ventas cargadas (" + ventasData.size() + "/" + totalVentas + ")");
            return;
        }

        cargandoPagina = true;

        // ✅ Actualizar label durante carga manual
        if (lblCargando != null) {
            lblCargando.setText("⏳ Cargando más...");
        }

        Task<List<Venta>> task = new Task<>() {
            @Override
            protected List<Venta> call() throws Exception {
                int cantidadACargar = Math.min(PAGE_SIZE_SCROLL, totalVentas - currentOffset);
                List<Venta> ventas = VentaDAO.obtenerVentasPaginadas(currentOffset, cantidadACargar);

                for (Venta v : ventas) {
                    try {
                        v.setItems(VentaDAO.obtenerItemsDeVenta(v.getId()));
                    } catch (Exception ex) {
                        System.err.println("Error cargando items de venta " + v.getId() + ": " + ex.getMessage());
                        v.setItems(new java.util.ArrayList<>());
                    }
                }

                return ventas;
            }
        };

        task.setOnSucceeded(e -> {
            List<Venta> nuevas = task.getValue();

            if (!nuevas.isEmpty()) {
                ventasData.addAll(nuevas);
                currentOffset += nuevas.size();
                System.out.println("🔄 Scroll manual: +" + nuevas.size() + " ventas (total: " + ventasData.size() + "/" + totalVentas + ")");
            }

            cargandoPagina = false;
            actualizarIndicadorCarga(); // ✅ Actualizar después de cargar
        });

        task.setOnFailed(e -> {
            System.err.println("❌ Error al cargar más ventas:");
            task.getException().printStackTrace();
            cargandoPagina = false;
            actualizarIndicadorCarga();
        });

        new Thread(task).start();
    }



    /**
     * Opcional: actualizar label o pie inferior con estado: "Mostrando X de Y ventas"
     */
    private void actualizarEstadoPie() {
        // Si tenés un Label para estado, actualizalo. Si no, podés crear uno.
        // Ejemplo (si agregás Label lblEstado en FXML):
        // lblEstado.setText(String.format("Mostrando %d de %d ventas", ventasData.size(), totalVentas));
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
        cargarVentasInicial();
    }

    // Ya no se llama desde cargarVentasInicial
// ===============================================
    private void actualizarEstadisticas() {
        // ✅ Ahora solo delega al método de segundo plano
        cargarEstadisticasEnSegundoPlano();
    }

    @FXML
    private void abrirNuevaVenta() {
        mostrarAlerta("Usa la vista de Productos para registrar ventas más rápido.\n\nSelecciona un producto y presiona 'Nueva venta'.");
    }

    private void mostrarLoading(boolean mostrar) {
        System.out.println((mostrar ? "🔄 Mostrando" : "✅ Ocultando") + " spinner de carga");

        if (vboxLoading != null && progressIndicator != null) {
            vboxLoading.setVisible(mostrar);
            vboxLoading.setManaged(mostrar);

            System.out.println("   - vboxLoading visible: " + vboxLoading.isVisible());
            System.out.println("   - vboxLoading managed: " + vboxLoading.isManaged());
        } else {
            System.err.println("⚠️ vboxLoading o progressIndicator es NULL");
            if (vboxLoading == null) System.err.println("   - vboxLoading es null");
            if (progressIndicator == null) System.err.println("   - progressIndicator es null");
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


    private void configurarTablaMasVendidos() {
        // Columna posición
        TableColumn<VentaDAO.ProductoVendido, Integer> colPos = new TableColumn<>("#");
        colPos.setCellValueFactory(cd -> new SimpleIntegerProperty(
                cd.getValue().getPosicion()).asObject());
        colPos.setPrefWidth(35);
        colPos.setStyle("-fx-alignment: CENTER;");

        // Columna etiqueta
        TableColumn<VentaDAO.ProductoVendido, String> colEtiqueta = new TableColumn<>("Etiq.");
        colEtiqueta.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEtiqueta()));
        colEtiqueta.setPrefWidth(60);
        colEtiqueta.setStyle("-fx-alignment: CENTER;");

        // Columna nombre
        TableColumn<VentaDAO.ProductoVendido, String> colNombre = new TableColumn<>("Producto");
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombre()));
        colNombre.setPrefWidth(150);

        // Columna cantidad con ProgressBar
        TableColumn<VentaDAO.ProductoVendido, Integer> colCantidad = new TableColumn<>("Cant.");
        colCantidad.setCellValueFactory(cd -> new SimpleIntegerProperty(
                cd.getValue().getTotalVendido()).asObject());
        colCantidad.setPrefWidth(80);
        colCantidad.setStyle("-fx-alignment: CENTER;");

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
                bar.setStyle("-fx-accent: #b88a52;");

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
        cargarVentasInicial(); // Restaurar ventas completas
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

    // ===============================================
// REEMPLAZAR instalarScrollListener() POR ESTA VERSIÓN ÚNICA
// Borra cualquier otro método con este nombre
// ===============================================

    private void instalarScrollListener() {
        if (scrollListenerInstalado) {
            System.out.println("⚠️ Scroll listener ya instalado, omitiendo...");
            return;
        }

        if (tablaVentas.getSkin() == null) {
            System.out.println("⏳ Skin no está listo, reintentando...");
            Platform.runLater(this::instalarScrollListener);
            return;
        }

        ScrollBar vBar = null;
        for (Node n : tablaVentas.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.VERTICAL) {
                vBar = sb;
                break;
            }
        }

        if (vBar != null) {
            ScrollBar finalVBar = vBar;

            finalVBar.valueProperty().addListener((o, oldV, newV) -> {
                // Detectar 80% en vez de 90% (más sensible)
                if (newV.doubleValue() >= 0.8) {
                    if (!cargandoPagina && ventasData.size() < totalVentas) {
                        System.out.println("📥 Scroll detectado al " + (int)(newV.doubleValue() * 100) + "% - Cargando más...");
                        cargarMasVentas();
                    }
                }
            });

            scrollListenerInstalado = true;
            System.out.println("✅ Scroll listener instalado (ventas actuales: " + ventasData.size() + "/" + totalVentas + ")");

            // Si después de cargar las primeras NO hay scroll, cargar más automáticamente
            Platform.runLater(() -> {
                if (finalVBar.getMax() == 0.0 && ventasData.size() < totalVentas) {
                    System.out.println("⚠️ No hay scroll visible - Cargando más ventas automáticamente...");
                    cargarMasVentas();
                }
            });

        } else {
            System.out.println("⚠️ ScrollBar no encontrada, reintentando...");
            Platform.runLater(this::instalarScrollListener);
        }
    }

    private void actualizarIndicadorCarga() {
        if (lblCargando == null) return;

        if (!cargaAutomaticaCompletada && ventasData.size() < MINIMO_PARA_SCROLL) {
            // Durante la carga progresiva
            lblCargando.setText("⏳ Cargando " + ventasData.size() + "/" + Math.min(MINIMO_PARA_SCROLL, totalVentas) + " ventas...");
            lblCargando.setStyle("-fx-font-size: 13px; -fx-text-fill: #8B7355; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-color: rgba(139, 115, 85, 0.15); -fx-background-radius: 5;");
        } else if (cargaAutomaticaCompletada && ventasData.size() < totalVentas) {
            // Después de la carga automática, esperando scroll
            lblCargando.setText("📊 " + ventasData.size() + "/" + totalVentas + " ventas cargadas");
            lblCargando.setStyle("-fx-font-size: 12px; -fx-text-fill: #A67C52; -fx-font-style: italic; -fx-padding: 5 10;");
        } else {
            // Todo cargado
            lblCargando.setText("✅ " + totalVentas + " ventas");
            lblCargando.setStyle("-fx-font-size: 12px; -fx-text-fill: #5D9C5D; -fx-font-weight: bold; -fx-padding: 5 10;");
        }
    }
}