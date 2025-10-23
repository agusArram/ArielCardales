package com.arielcardales.arielcardales.DAO.sqlite;

import com.arielcardales.arielcardales.Entidades.ProductoVariante;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de ProductoVariante en SQLite (backup local)
 */
public class SqliteProductoVarianteDAO {

    /**
     * Obtiene todas las variantes del cliente actual
     */
    public static List<ProductoVariante> findAll() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<ProductoVariante> variantes = new ArrayList<>();

        String sql = """
            SELECT id, producto_id, color, talle, precio, costo, stock,
                   etiqueta, active, createdAt, updatedAt
            FROM producto_variante
            WHERE cliente_id = ?
            ORDER BY producto_id, color, talle
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    variantes.add(mapVariante(rs));
                }
            }
        }

        return variantes;
    }

    /**
     * Obtiene variantes por producto_id
     */
    public static List<ProductoVariante> findByProductoId(Long productoId) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<ProductoVariante> variantes = new ArrayList<>();

        String sql = """
            SELECT id, producto_id, color, talle, precio, costo, stock,
                   etiqueta, active, createdAt, updatedAt
            FROM producto_variante
            WHERE producto_id = ? AND cliente_id = ?
            ORDER BY color, talle
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productoId);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    variantes.add(mapVariante(rs));
                }
            }
        }

        return variantes;
    }

    /**
     * Inserta una nueva variante
     */
    public static long insert(ProductoVariante variante) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            INSERT INTO producto_variante
            (producto_id, color, talle, precio, costo, stock, etiqueta, active, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, variante.getProductoId());
            ps.setString(2, variante.getColor());
            ps.setString(3, variante.getTalle());
            ps.setDouble(4, variante.getPrecio().doubleValue());
            ps.setDouble(5, variante.getCosto().doubleValue());
            ps.setInt(6, variante.getStock());
            ps.setString(7, variante.getEtiqueta());
            ps.setInt(8, variante.isActive() ? 1 : 0);
            ps.setString(9, clienteId);

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Error obteniendo ID generado para variante");
                }
            }
        }
    }

    /**
     * Actualiza una variante existente
     */
    public static void update(ProductoVariante variante) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            UPDATE producto_variante SET
                producto_id = ?,
                color = ?,
                talle = ?,
                precio = ?,
                costo = ?,
                stock = ?,
                etiqueta = ?,
                active = ?,
                updatedAt = datetime('now')
            WHERE id = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, variante.getProductoId());
            ps.setString(2, variante.getColor());
            ps.setString(3, variante.getTalle());
            ps.setDouble(4, variante.getPrecio().doubleValue());
            ps.setDouble(5, variante.getCosto().doubleValue());
            ps.setInt(6, variante.getStock());
            ps.setString(7, variante.getEtiqueta());
            ps.setInt(8, variante.isActive() ? 1 : 0);
            ps.setLong(9, variante.getId());
            ps.setString(10, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró variante con id=" + variante.getId());
            }
        }
    }

    /**
     * Elimina una variante por ID
     */
    public static void deleteById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "DELETE FROM producto_variante WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró variante con id=" + id);
            }
        }
    }

    /**
     * Obtiene una variante por ID
     */
    public static ProductoVariante findById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            SELECT id, producto_id, color, talle, precio, costo, stock,
                   etiqueta, active, createdAt, updatedAt
            FROM producto_variante
            WHERE id = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapVariante(rs);
                } else {
                    throw new SQLException("No se encontró variante con id=" + id);
                }
            }
        }
    }

    /**
     * Cuenta el total de variantes
     */
    public static int count() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        String sql = "SELECT COUNT(*) FROM producto_variante WHERE cliente_id = ?";

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

    /**
     * Mapea un ResultSet a ProductoVariante
     */
    private static ProductoVariante mapVariante(ResultSet rs) throws SQLException {
        ProductoVariante v = new ProductoVariante();
        v.setId(rs.getLong("id"));
        v.setProductoId(rs.getLong("producto_id"));
        v.setColor(rs.getString("color"));
        v.setTalle(rs.getString("talle"));
        v.setPrecio(BigDecimal.valueOf(rs.getDouble("precio")));
        v.setCosto(BigDecimal.valueOf(rs.getDouble("costo")));
        v.setStock(rs.getInt("stock"));
        v.setEtiqueta(rs.getString("etiqueta"));
        v.setActive(rs.getInt("active") == 1);

        String createdAtStr = rs.getString("createdAt");
        if (createdAtStr != null) {
            v.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }

        String updatedAtStr = rs.getString("updatedAt");
        if (updatedAtStr != null) {
            v.setUpdatedAt(LocalDateTime.parse(updatedAtStr.replace(" ", "T")));
        }

        return v;
    }
}
