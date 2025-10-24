package SORT_PROYECTS.AppInventario.controller;

import SORT_PROYECTS.AppInventario.DAO.CategoriaDAO;
import SORT_PROYECTS.AppInventario.DAO.ProductoDAO;
import SORT_PROYECTS.AppInventario.Entidades.Producto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

/**
 * Encapsula toda la lógica de edición inline de la tabla de productos.
 * Permite mantener ProductoController enfocado en la vista.
 */
public class EditarProductoController {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private Map<String, Long> categoriasNombreId;
    private ObservableList<String> categoriasNombres;

    // === FORMATOS ===
    private final StringConverter<BigDecimal> moneyConv = new StringConverter<>() {
        final DecimalFormat df;
        {
            var sym = new DecimalFormatSymbols(new Locale("es", "AR"));
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
            String limpio = s.replace("$", "").replace(" ", "")
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

    /**
     * Configura todas las columnas editables de la tabla.
     */
    public void configurar(TableView<Producto> tablaProductos) {
        tablaProductos.setEditable(true);

        // === Categorías ===
        categoriasNombreId = new CategoriaDAO().mapNombreId();
        categoriasNombres = FXCollections.observableArrayList(categoriasNombreId.keySet());
        FXCollections.sort(categoriasNombres);

        // === Obtener columnas por ID (asignado en Tablas.crearColumnas) ===
        TableColumn<Producto, String> colNombre = getCol(tablaProductos, "nombre");
        TableColumn<Producto, String> colDesc = getCol(tablaProductos, "descripcion");
        TableColumn<Producto, String> colCat  = getCol(tablaProductos, "categoria");
        TableColumn<Producto, BigDecimal> colPrecio = getCol(tablaProductos, "precio");
        TableColumn<Producto, Integer> colStock = getCol(tablaProductos, "stockOnHand");

        // === NOMBRE ===
        colNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colNombre.setOnEditCommit(e -> {
            Producto p = e.getRowValue();
            p.setNombre(e.getNewValue());
            guardar(p);
        });

        // === DESCRIPCIÓN ===
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(e -> {
            Producto p = e.getRowValue();
            p.setDescripcion(e.getNewValue());
            guardar(p);
        });

        // === CATEGORÍA ===
        colCat.setCellFactory(ComboBoxTableCell.forTableColumn(categoriasNombres));
        colCat.setOnEditCommit(e -> {
            Producto p = e.getRowValue();
            String nombreCat = e.getNewValue();
            Long idCat = categoriasNombreId.get(nombreCat);
            p.setCategoria(nombreCat);
            p.setCategoriaId(idCat);
            guardar(p);
        });

        // === PRECIO ===
        colPrecio.setCellFactory(TextFieldTableCell.forTableColumn(moneyConv));
        colPrecio.setOnEditCommit(e -> {
            Producto p = e.getRowValue();
            try {
                p.setPrecio(e.getNewValue());
                guardar(p);
            } catch (Exception ex) {
                error("Precio inválido");
            }
        });

        // === STOCK ===
        colStock.setCellFactory(TextFieldTableCell.forTableColumn(intConv));
        colStock.setOnEditCommit(e -> {
            Producto p = e.getRowValue();
            try {
                p.setStockOnHand(e.getNewValue());
                guardar(p);
            } catch (Exception ex) {
                error("Stock inválido");
            }
        });
    }

    // === UTILIDADES ===

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Producto, T> getCol(TableView<Producto> tabla, String id) {
        return (TableColumn<Producto, T>) tabla.getColumns().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la columna: " + id));
    }

    private void guardar(Producto p) {
        // Evita bloquear el hilo de JavaFX
        javafx.concurrent.Task<Void> tarea = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                productoDAO.update(p); // llamada a DB
                return null;
            }
        };
        tarea.setOnSucceeded(e -> ok("Producto actualizado"));
        tarea.setOnFailed(e -> error("No se pudo guardar: " + tarea.getException().getMessage()));

        new Thread(tarea).start(); // se ejecuta en background
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


}
