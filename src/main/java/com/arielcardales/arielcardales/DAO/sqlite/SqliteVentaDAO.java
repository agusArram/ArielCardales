package com.arielcardales.arielcardales.DAO.sqlite;

import com.arielcardales.arielcardales.Entidades.Venta;
import com.arielcardales.arielcardales.Entidades.Venta.VentaItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Venta y VentaItem en SQLite (backup local)
 */
public class SqliteVentaDAO {

    // ===== VENTA =====

    /**
     * Obtiene todas las ventas del cliente actual
     */
    public static List<Venta> findAll() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<Venta> ventas = new ArrayList<>();

        String sql = """
            SELECT id, clienteNombre, fecha, medioPago, total
            FROM venta
            WHERE cliente_id = ?
            ORDER BY fecha DESC
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ventas.add(mapVenta(rs));
                }
            }
        }

        return ventas;
    }

    /**
     * Inserta una nueva venta (sin items)
     */
    public static long insert(Venta venta) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            INSERT INTO venta (id, clienteNombre, fecha, medioPago, total, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, venta.getId());
            ps.setString(2, venta.getClienteNombre());
            ps.setString(3, venta.getFecha() != null ? venta.getFecha().toString() : null);
            ps.setString(4, venta.getMedioPago());
            ps.setDouble(5, venta.getTotal().doubleValue());
            ps.setString(6, clienteId);

            ps.executeUpdate();
            return venta.getId();
        }
    }

    /**
     * Obtiene una venta por ID
     */
    public static Venta findById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            SELECT id, clienteNombre, fecha, medioPago, total
            FROM venta
            WHERE id = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapVenta(rs);
                } else {
                    throw new SQLException("No se encontró venta con id=" + id);
                }
            }
        }
    }

    /**
     * Elimina una venta por ID (también elimina ventaItems por CASCADE)
     */
    public static void deleteById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "DELETE FROM venta WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró venta con id=" + id);
            }
        }
    }

    /**
     * Cuenta el total de ventas
     */
    public static int count() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        String sql = "SELECT COUNT(*) FROM venta WHERE cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    // ===== VENTA ITEM =====

    /**
     * Obtiene los items de una venta
     */
    public static List<VentaItem> findItemsByVentaId(Long ventaId) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<VentaItem> items = new ArrayList<>();

        String sql = """
            SELECT id, ventaId, productoId, variante_id, qty, precioUnit, productoNombre
            FROM ventaItem
            WHERE ventaId = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, ventaId);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapVentaItem(rs));
                }
            }
        }

        return items;
    }

    /**
     * Inserta un nuevo VentaItem
     */
    public static long insertItem(VentaItem item) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            INSERT INTO ventaItem
            (ventaId, productoId, variante_id, qty, precioUnit, productoNombre, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, item.getVentaId());
            ps.setLong(2, item.getProductoId());

            if (item.getVarianteId() != null) {
                ps.setLong(3, item.getVarianteId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setInt(4, item.getQty());
            ps.setDouble(5, item.getPrecioUnit().doubleValue());
            ps.setString(6, item.getProductoNombre());
            ps.setString(7, clienteId);

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Error obteniendo ID generado para ventaItem");
                }
            }
        }
    }

    /**
     * Elimina un VentaItem por ID
     */
    public static void deleteItemById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "DELETE FROM ventaItem WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            ps.executeUpdate();
        }
    }

    // ===== MAPPERS =====

    /**
     * Mapea ResultSet a Venta
     */
    private static Venta mapVenta(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getLong("id"));
        v.setClienteNombre(rs.getString("clienteNombre"));

        String fechaStr = rs.getString("fecha");
        if (fechaStr != null) {
            v.setFecha(LocalDateTime.parse(fechaStr.replace(" ", "T")));
        }

        v.setMedioPago(rs.getString("medioPago"));
        v.setTotal(BigDecimal.valueOf(rs.getDouble("total")));

        return v;
    }

    /**
     * Mapea ResultSet a VentaItem
     */
    private static VentaItem mapVentaItem(ResultSet rs) throws SQLException {
        VentaItem item = new VentaItem();
        item.setId(rs.getLong("id"));
        item.setVentaId(rs.getLong("ventaId"));
        item.setProductoId(rs.getLong("productoId"));

        Long varianteId = rs.getLong("variante_id");
        if (!rs.wasNull()) {
            item.setVarianteId(varianteId);
        }

        item.setQty(rs.getInt("qty"));
        item.setPrecioUnit(BigDecimal.valueOf(rs.getDouble("precioUnit")));
        item.setProductoNombre(rs.getString("productoNombre"));

        // Calcular subtotal
        item.setSubtotal(item.getPrecioUnit().multiply(BigDecimal.valueOf(item.getQty())));

        return item;
    }
}
