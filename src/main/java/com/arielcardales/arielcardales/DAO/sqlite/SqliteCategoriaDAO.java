package com.arielcardales.arielcardales.DAO.sqlite;

import com.arielcardales.arielcardales.Entidades.Categoria;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Categoria en SQLite (backup local)
 */
public class SqliteCategoriaDAO {

    /**
     * Obtiene todas las categorías del cliente actual
     */
    public static List<Categoria> findAll() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<Categoria> categorias = new ArrayList<>();

        String sql = "SELECT id, nombre, parentId, createdAt FROM categoria WHERE cliente_id = ? ORDER BY nombre";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Categoria c = new Categoria();
                    c.setId(rs.getLong("id"));
                    c.setNombre(rs.getString("nombre"));

                    // parentId puede ser NULL
                    Long parentId = rs.getLong("parentId");
                    if (!rs.wasNull()) {
                        c.setParentId(parentId);
                    }

                    String createdAtStr = rs.getString("createdAt");
                    if (createdAtStr != null) {
                        c.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                    }

                    categorias.add(c);
                }
            }
        }

        return categorias;
    }

    /**
     * Inserta una nueva categoría
     */
    public static long insert(Categoria categoria) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "INSERT INTO categoria (id, nombre, parentId, cliente_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, categoria.getId());
            ps.setString(2, categoria.getNombre());

            if (categoria.getParentId() != null) {
                ps.setLong(3, categoria.getParentId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, clienteId);

            ps.executeUpdate();
            return categoria.getId();
        }
    }

    /**
     * Actualiza una categoría existente
     */
    public static void update(Categoria categoria) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "UPDATE categoria SET nombre = ?, parentId = ? WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, categoria.getNombre());

            if (categoria.getParentId() != null) {
                ps.setLong(2, categoria.getParentId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setLong(3, categoria.getId());
            ps.setString(4, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró categoría con id=" + categoria.getId());
            }
        }
    }

    /**
     * Elimina una categoría por ID
     */
    public static void deleteById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "DELETE FROM categoria WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró categoría con id=" + id);
            }
        }
    }

    /**
     * Obtiene una categoría por ID
     */
    public static Categoria findById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "SELECT id, nombre, parentId, createdAt FROM categoria WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Categoria c = new Categoria();
                    c.setId(rs.getLong("id"));
                    c.setNombre(rs.getString("nombre"));

                    Long parentId = rs.getLong("parentId");
                    if (!rs.wasNull()) {
                        c.setParentId(parentId);
                    }

                    String createdAtStr = rs.getString("createdAt");
                    if (createdAtStr != null) {
                        c.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                    }

                    return c;
                } else {
                    throw new SQLException("No se encontró categoría con id=" + id);
                }
            }
        }
    }

    /**
     * Cuenta el total de categorías
     */
    public static int count() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        String sql = "SELECT COUNT(*) FROM categoria WHERE cliente_id = ?";

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
}
