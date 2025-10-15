package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Venta;
import com.arielcardales.arielcardales.Entidades.Venta.VentaItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    public static List<Venta> obtenerTodasLasVentas() throws SQLException {
        String sql = """
            SELECT 
                id, clienteNombre, fecha, medioPago, total,
                totalItems, cantidadProductos
            FROM vVentasResumen
            ORDER BY fecha DESC
        """;

        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Venta venta = new Venta(
                        rs.getLong("id"),
                        rs.getString("clienteNombre"),
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getString("medioPago"),
                        rs.getBigDecimal("total")
                );
                ventas.add(venta);
            }
        }

        return ventas;
    }

    public static List<Venta> obtenerVentasPorFecha(LocalDate fechaInicio, LocalDate fechaFin)
            throws SQLException {
        String sql = """
            SELECT id, clienteNombre, fecha, medioPago, total
            FROM vVentasResumen
            WHERE DATE(fecha) BETWEEN ? AND ?
            ORDER BY fecha DESC
        """;

        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venta venta = new Venta(
                            rs.getLong("id"),
                            rs.getString("clienteNombre"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getString("medioPago"),
                            rs.getBigDecimal("total")
                    );
                    ventas.add(venta);
                }
            }
        }

        return ventas;
    }

    public static List<VentaItem> obtenerItemsDeVenta(Long ventaId) throws SQLException {
        String sql = """
            SELECT 
                itemId as id,
                ventaId,
                productoId,
                productoNombre,
                productoEtiqueta,
                qty,
                precioUnit,
                subtotal
            FROM vVentasCompletas
            WHERE ventaId = ?
            ORDER BY itemId
        """;

        List<VentaItem> items = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, ventaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VentaItem item = new VentaItem(
                            rs.getLong("id"),
                            rs.getLong("ventaId"),
                            rs.getLong("productoId"),
                            rs.getString("productoNombre"),
                            rs.getString("productoEtiqueta"),
                            rs.getInt("qty"),
                            rs.getBigDecimal("precioUnit"),
                            rs.getBigDecimal("subtotal")
                    );
                    items.add(item);
                }
            }
        }

        return items;
    }

    public static List<Venta> buscarVentasPorCliente(String nombreCliente) throws SQLException {
        String sql = """
            SELECT v.id, v.clienteNombre, v.fecha, v.medioPago, v.total
            FROM venta v
            WHERE LOWER(v.clienteNombre) LIKE LOWER(?)
            ORDER BY v.fecha DESC
        """;

        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nombreCliente + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venta venta = new Venta(
                            rs.getLong("id"),
                            rs.getString("clienteNombre"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getString("medioPago"),
                            rs.getBigDecimal("total")
                    );
                    ventas.add(venta);
                }
            }
        }

        return ventas;
    }

    public static VentaEstadisticas obtenerEstadisticas(LocalDate fechaInicio, LocalDate fechaFin)
            throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as totalVentas,
                COALESCE(SUM(total), 0) as totalMonto,
                COALESCE(AVG(total), 0) as promedioVenta,
                COALESCE(MAX(total), 0) as ventaMayor,
                COALESCE(MIN(total), 0) as ventaMenor
            FROM venta
            WHERE DATE(fecha) BETWEEN ? AND ?
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new VentaEstadisticas(
                            rs.getInt("totalVentas"),
                            rs.getBigDecimal("totalMonto"),
                            rs.getBigDecimal("promedioVenta"),
                            rs.getBigDecimal("ventaMayor"),
                            rs.getBigDecimal("ventaMenor")
                    );
                }
            }
        }

        return new VentaEstadisticas(0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Registra una venta completa con sus items en una transacción
     */
    public static Long registrarVentaCompleta(Venta venta) throws SQLException {
        Connection conn = null;
        try {
            conn = Database.get();
            conn.setAutoCommit(false);

            // 1. Insertar venta
            String sqlVenta = """
                INSERT INTO venta (clienteNombre, fecha, medioPago, total)
                VALUES (?, ?, ?, ?)
            """;

            long ventaId;
            try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, venta.getClienteNombre());
                ps.setTimestamp(2, Timestamp.valueOf(venta.getFecha()));
                ps.setString(3, venta.getMedioPago());
                ps.setBigDecimal(4, venta.getTotal());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        ventaId = rs.getLong(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID de la venta");
                    }
                }
            }

            // 2. Insertar items
            String sqlItem = """
                INSERT INTO ventaItem (ventaId, productoId, qty, precioUnit)
                VALUES (?, ?, ?, ?)
            """;

            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                for (VentaItem item : venta.getItems()) {
                    ps.setLong(1, ventaId);
                    ps.setLong(2, item.getProductoId());
                    ps.setInt(3, item.getQty());
                    ps.setBigDecimal(4, item.getPrecioUnit());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return ventaId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class VentaEstadisticas {
        private int totalVentas;
        private BigDecimal totalMonto;
        private BigDecimal promedioVenta;
        private BigDecimal ventaMayor;
        private BigDecimal ventaMenor;

        public VentaEstadisticas(int totalVentas, BigDecimal totalMonto,
                                 BigDecimal promedioVenta, BigDecimal ventaMayor,
                                 BigDecimal ventaMenor) {
            this.totalVentas = totalVentas;
            this.totalMonto = totalMonto;
            this.promedioVenta = promedioVenta;
            this.ventaMayor = ventaMayor;
            this.ventaMenor = ventaMenor;
        }

        public int getTotalVentas() { return totalVentas; }
        public BigDecimal getTotalMonto() { return totalMonto; }
        public BigDecimal getPromedioVenta() { return promedioVenta; }
        public BigDecimal getVentaMayor() { return ventaMayor; }
        public BigDecimal getVentaMenor() { return ventaMenor; }
    }

    // Agregar estos métodos al VentaDAO existente

    /**
     * Obtiene los productos más vendidos en un período
     */
    public static List<ProductoVendido> obtenerProductosMasVendidos(
            LocalDate fechaInicio, LocalDate fechaFin, int limite) throws SQLException {

        String sql = """
        SELECT 
            p.etiqueta,
            p.nombre,
            SUM(vi.qty) as totalVendido,
            SUM(vi.subtotal) as totalIngresos
        FROM ventaItem vi
        JOIN producto p ON p.id = vi.productoId
        JOIN venta v ON v.id = vi.ventaId
        WHERE DATE(v.fecha) BETWEEN ? AND ?
        GROUP BY p.id, p.etiqueta, p.nombre
        ORDER BY totalVendido DESC
        LIMIT ?
    """;

        List<ProductoVendido> productos = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));
            ps.setInt(3, limite);

            try (ResultSet rs = ps.executeQuery()) {
                int posicion = 1;
                while (rs.next()) {
                    ProductoVendido pv = new ProductoVendido(
                            posicion++,
                            rs.getString("etiqueta"),
                            rs.getString("nombre"),
                            rs.getInt("totalVendido"),
                            rs.getBigDecimal("totalIngresos")
                    );
                    productos.add(pv);
                }
            }
        }

        return productos;
    }

    /**
     * Clase para representar productos más vendidos
     */
    public static class ProductoVendido {
        private int posicion;
        private String etiqueta;
        private String nombre;
        private int totalVendido;
        private BigDecimal totalIngresos;

        public ProductoVendido(int posicion, String etiqueta, String nombre,
                               int totalVendido, BigDecimal totalIngresos) {
            this.posicion = posicion;
            this.etiqueta = etiqueta;
            this.nombre = nombre;
            this.totalVendido = totalVendido;
            this.totalIngresos = totalIngresos;
        }

        public int getPosicion() { return posicion; }
        public String getEtiqueta() { return etiqueta; }
        public String getNombre() { return nombre; }
        public int getTotalVendido() { return totalVendido; }
        public BigDecimal getTotalIngresos() { return totalIngresos; }
    }
}