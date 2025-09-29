package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.CategoriaDAO;
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
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppController {

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TextField txtBuscarEtiqueta;
    @FXML private ToggleButton btnNombre;
    @FXML private ToggleButton btnCategoria;
    @FXML private ToggleButton btnEtiqueta;
    @FXML private ToggleGroup grupoBusqueda;

    private ObservableList<Producto> listaProductos;
    private final ProductoDAO productoDAO = new ProductoDAO();

    // Cache categorías (nombre -> id) para ComboBox
    private Map<String, Long> categoriasNombreId;
    private ObservableList<String> categoriasNombres;

    // --- Converters para edición ---
    private final StringConverter<BigDecimal> moneyConv = new StringConverter<>() {
        final DecimalFormat df;
        {
            var sym = new DecimalFormatSymbols(new Locale("es","AR"));
            sym.setDecimalSeparator(',');
            sym.setGroupingSeparator('.');
            df = new DecimalFormat("#,##0.00", sym);
        }
        @Override public String toString(BigDecimal val) {
            if (val == null) return "";
            return "$ " + df.format(val);
        }
        @Override public BigDecimal fromString(String s) {
            if (s == null || s.isBlank()) return BigDecimal.ZERO;
            // Acepta: "$ 12.345,67", "12345,67", "12.345" etc.
            String limpio = s.replace("$","").replace(" ", "")
                    .replace(".", "").replace(",", ".");
            return new BigDecimal(limpio);
        }
    };
    private final StringConverter<Integer> intConv = new StringConverter<>() {
        @Override public String toString(Integer v) { return v == null ? "" : String.valueOf(v); }
        @Override public Integer fromString(String s) {
            if (s == null || s.isBlank()) return 0;
            int n = Integer.parseInt(s.trim());
            if (n < 0) throw new NumberFormatException("El stock no puede ser negativo");
            return n;
        }
    };

    @FXML
    public void initialize() {
        // 1) Columnas dinámicas
        String[][] columnas = {
                {"Etiqueta", "etiqueta", "0.08", "60"},
                {"Nombre", "nombre", "0.20", "140"},
                {"Descripción", "descripcion", "0.35", "280"},
                {"Categoría", "categoria", "0.12", "100"},
                {"Precio", "precio", "0.15", "100"},
                {"Stock", "stockOnHand", "0.10", "70"}
        };
        List<TableColumn<Producto, ?>> cols = Tablas.crearColumnas(columnas);
        // IMPORTANTE: que Tablas.crearColumnas establezca col.setId(propiedad)
        // (etiqueta/nombre/descripcion/categoria/precio/stockOnHand) para poder buscarlas por id:
        cols.forEach(col -> {
            Object ud = col.getUserData();
            if (ud instanceof Double peso) {
                col.prefWidthProperty().bind(tablaProductos.widthProperty().multiply(peso));
            }
            tablaProductos.getColumns().add(col);
        });

        // 2) Datos
        listaProductos = FXCollections.observableArrayList(productoDAO.findAll());
        FilteredList<Producto> filtrados = new FilteredList<>(listaProductos, p -> true);

        // 3) Búsqueda con heurística (p### = etiqueta)
        Runnable aplicarFiltro = () -> {
            String filtro = txtBuscarEtiqueta.getText() == null ? "" : txtBuscarEtiqueta.getText().trim().toLowerCase();
            filtrados.setPredicate(prod -> {
                if (filtro.isBlank()) return true;
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

        grupoBusqueda = new ToggleGroup();
        btnNombre.setToggleGroup(grupoBusqueda);
        btnCategoria.setToggleGroup(grupoBusqueda);
        btnEtiqueta.setToggleGroup(grupoBusqueda);


        txtBuscarEtiqueta.textProperty().addListener((o, a, b) -> aplicarFiltro.run());
        grupoBusqueda.selectToggle(btnNombre);
        grupoBusqueda.selectedToggleProperty().addListener((o, a, b) -> aplicarFiltro.run());

        // 4) Orden + setItems
        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(ordenados);

        // 5) Edición inline
        configurarEdicionInline();

        // 6) RowFactory unificado: doble-click inicia edición + marca low stock
        tablaProductos.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>() {
                @Override protected void updateItem(Producto item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("low-stock");
                    if (!empty && item != null && item.getStockOnHand() <= 3) {
                        if (!getStyleClass().contains("low-stock"))
                            getStyleClass().add("low-stock");
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    // Inicia edición en la columna actualmente seleccionada
                    if (!tablaProductos.getSelectionModel().getSelectedCells().isEmpty()) {
                        TablePosition<Producto, ?> pos = tablaProductos.getSelectionModel().getSelectedCells().get(0);
                        @SuppressWarnings("unchecked")
                        TableColumn<Producto, ?> col = (TableColumn<Producto, ?>) pos.getTableColumn();
                        tablaProductos.edit(pos.getRow(), col);
                    }

                    Notifications.create()
                            .text("Modo edición: escribí y presioná Enter para guardar (Esc: cancelar).")
                            .hideAfter(javafx.util.Duration.seconds(5))
                            .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                            .showInformation();
                }
            });
            return row;
        });
    }

    private void configurarEdicionInline() {
        tablaProductos.setEditable(true);

        // --- obtener referencias a las columnas por id (asignadas en Tablas.crearColumnas) ---
        @SuppressWarnings("unchecked")
        TableColumn<Producto, String> colNombre =
                (TableColumn<Producto, String>) tablaProductos.getColumns().stream()
                        .filter(c -> "nombre".equals(c.getId())).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        TableColumn<Producto, String> colDesc =
                (TableColumn<Producto, String>) tablaProductos.getColumns().stream()
                        .filter(c -> "descripcion".equals(c.getId())).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        TableColumn<Producto, String> colCat =
                (TableColumn<Producto, String>) tablaProductos.getColumns().stream()
                        .filter(c -> "categoria".equals(c.getId()))
                        .findFirst().orElseThrow();


        @SuppressWarnings("unchecked")
        TableColumn<Producto, BigDecimal> colPrecio =
                (TableColumn<Producto, BigDecimal>) tablaProductos.getColumns().stream()
                        .filter(c -> "precio".equals(c.getId())).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        TableColumn<Producto, Integer> colStock =
                (TableColumn<Producto, Integer>) tablaProductos.getColumns().stream()
                        .filter(c -> "stockOnHand".equals(c.getId())).findFirst().orElseThrow();

        // --- Nombre / Descripción: TextFieldTableCell ---
        colNombre.setEditable(true);
        colNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colNombre.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            String nuevo = ev.getNewValue();
            if (nuevo != null && !nuevo.equals(ev.getOldValue())) {
                p.setNombre(nuevo);
                guardarBasico(p);
            } else {
                tablaProductos.refresh();
            }
        });

        colDesc.setEditable(true);
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            String nuevo = ev.getNewValue();
            if (nuevo != null && !nuevo.equals(ev.getOldValue())) {
                p.setDescripcion(nuevo);
                guardarBasico(p);
            } else {
                tablaProductos.refresh();
            }
        });

        // --- Categoría: ComboBoxTableCell con nombres ---
        categoriasNombreId = new CategoriaDAO().mapNombreId();  // << añade esto en CategoriaDAO (abajo)
        categoriasNombres = FXCollections.observableArrayList(categoriasNombreId.keySet());
        FXCollections.sort(categoriasNombres);

        colCat.setEditable(true);
        colCat.setCellFactory(ComboBoxTableCell.forTableColumn(
                new javafx.util.StringConverter<String>() {
                    @Override public String toString(String s) { return s; }
                    @Override public String fromString(String s) { return s; }
                },
                categoriasNombres
        ));

        colCat.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            String nombreCat = ev.getNewValue();
            if (nombreCat != null && !nombreCat.equals(ev.getOldValue())) {
                Long idCat = categoriasNombreId.get(nombreCat);
                p.setCategoria(nombreCat);     // mostrar
                p.setCategoriaId(idCat);       // persistir
                guardarBasico(p);
            } else {
                tablaProductos.refresh();
            }
        });


        // --- Precio: TextFieldTableCell con formateo $ ---
        colPrecio.setEditable(true);
        colPrecio.setCellFactory(TextFieldTableCell.forTableColumn(moneyConv));
        colPrecio.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            try {
                BigDecimal nuevo = ev.getNewValue();
                if (nuevo != null && (ev.getOldValue() == null || nuevo.compareTo(ev.getOldValue()) != 0)) {
                    p.setPrecio(nuevo);
                    guardarBasico(p);
                } else {
                    tablaProductos.refresh();
                }
            } catch (Exception ex) {
                error("Precio inválido");
                tablaProductos.refresh();
            }
        });

        // --- Stock: TextFieldTableCell Integer ---
        colStock.setEditable(true);
        colStock.setCellFactory(TextFieldTableCell.forTableColumn(intConv));
        colStock.setOnEditCommit(ev -> {
            Producto p = ev.getRowValue();
            try {
                Integer nuevo = ev.getNewValue();
                if (nuevo != null && !nuevo.equals(ev.getOldValue())) {
                    p.setStockOnHand(nuevo);
                    guardarBasico(p);
                } else {
                    tablaProductos.refresh();
                }
            } catch (Exception ex) {
                error("Stock inválido");
                tablaProductos.refresh();
            }
        });

        // Enter = commit, Esc = cancel (extra, por si acaso)
        tablaProductos.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE && tablaProductos.getEditingCell() != null) {
                tablaProductos.edit(-1, null);
            }
        });
    }

    // Guarda en DB y muestra toast
    private void guardarBasico(Producto p) {
        try {
            // update() tuyo requiere categoriaId/unidadId: unidadId no la editamos, dejamos el valor actual del producto.
            productoDAO.update(p);
            ok("Producto actualizado");
        } catch (Exception e) {
            error("No se pudo guardar: " + e.getMessage());
        }
    }

    private void ok(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(2))
                .showConfirm();
    }

    private void error(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(5))
                .showError();
    }

    // Exportar
    @FXML private void exportarExcel() { ExportadorExcel.exportar(tablaProductos.getItems(), "productos.xlsx"); }
    @FXML private void exportarPDF()   { ExportadorPDF.exportar(tablaProductos.getItems(), "productos.pdf"); }
}
