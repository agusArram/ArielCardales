package com.arielcardales.arielcardales.DAO.sqlite;

import com.arielcardales.arielcardales.Entidades.Producto;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Producto en SQLite (backup local)
 */
public class SqliteProductoDAO {

    /**
     * Obtiene todos los productos del cliente actual
     */
    public static List<Producto> findAll() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<Producto> productos = new ArrayList<>();

        String sql = """
            SELECT id, etiqueta, nombre, descripcion, categoriaId, unidadId,
                   precio, costo, stockOnHand, active, updatedAt, createdAt
            FROM producto
            WHERE cliente_id = ? AND active = 1
            ORDER BY nombre
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapProducto(rs));
                }
            }
        }

        return productos;
    }

    /**
     * Inserta un nuevo producto
     */
    public static long insert(Producto producto) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            INSERT INTO producto (id, etiqueta, nombre, descripcion, categoriaId, unidadId,
                                  precio, costo, stockOnHand, active, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, producto.getId());
            ps.setString(2, producto.getEtiqueta());
            ps.setString(3, producto.getNombre());
            ps.setString(4, producto.getDescripcion());
            ps.setLong(5, producto.getCategoriaId());
            ps.setLong(6, producto.getUnidadId());
            ps.setDouble(7, producto.getPrecio().doubleValue());
            ps.setDouble(8, producto.getCosto().doubleValue());
            ps.setInt(9, producto.getStockOnHand());
            ps.setInt(10, producto.isActive() ? 1 : 0);
            ps.setString(11, clienteId);

            ps.executeUpdate();
            return producto.getId();
        }
    }

    /**
     * Actualiza un producto existente
     */
    public static void update(Producto producto) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            UPDATE producto SET
                etiqueta = ?,
                nombre = ?,
                descripcion = ?,
                categoriaId = ?,
                unidadId = ?,
                precio = ?,
                costo = ?,
                stockOnHand = ?,
                active = ?,
                updatedAt = datetime('now')
            WHERE id = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, producto.getEtiqueta());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getDescripcion());
            ps.setLong(4, producto.getCategoriaId());
            ps.setLong(5, producto.getUnidadId());
            ps.setDouble(6, producto.getPrecio().doubleValue());
            ps.setDouble(7, producto.getCosto().doubleValue());
            ps.setInt(8, producto.getStockOnHand());
            ps.setInt(9, producto.isActive() ? 1 : 0);
            ps.setLong(10, producto.getId());
            ps.setString(11, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró producto con id=" + producto.getId());
            }
        }
    }

    /**
     * Elimina un producto por ID
     */
    public static void deleteById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "DELETE FROM producto WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró producto con id=" + id);
            }
        }
    }

    /**
     * Obtiene un producto por ID
     */
    public static Producto findById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            SELECT id, etiqueta, nombre, descripcion, categoriaId, unidadId,
                   precio, costo, stockOnHand, active, updatedAt, createdAt
            FROM producto
            WHERE id = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProducto(rs);
                } else {
                    throw new SQLException("No se encontró producto con id=" + id);
                }
            }
        }
    }

    /**
     * Cuenta el total de productos
     */
    public static int count() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        String sql = "SELECT COUNT(*) FROM producto WHERE cliente_id = ?";

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
     * Mapea un ResultSet a un objeto Producto
     */
    private static Producto mapProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setEtiqueta(rs.getString("etiqueta"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoriaId(rs.getLong("categoriaId"));
        p.setUnidadId(rs.getLong("unidadId"));
        p.setPrecio(BigDecimal.valueOf(rs.getDouble("precio")));
        p.setCosto(BigDecimal.valueOf(rs.getDouble("costo")));
        p.setStockOnHand(rs.getInt("stockOnHand"));
        p.setActive(rs.getInt("active") == 1);

        String updatedAtStr = rs.getString("updatedAt");
        if (updatedAtStr != null) {
            p.setUpdatedAt(LocalDateTime.parse(updatedAtStr.replace(" ", "T")));
        }

        return p;
    }
}
