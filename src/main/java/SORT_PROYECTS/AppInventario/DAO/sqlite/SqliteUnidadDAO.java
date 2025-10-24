package SORT_PROYECTS.AppInventario.DAO.sqlite;

import SORT_PROYECTS.AppInventario.Entidades.Unidad;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Unidad en SQLite (backup local)
 */
public class SqliteUnidadDAO {

    /**
     * Obtiene todas las unidades del cliente actual
     */
    public static List<Unidad> findAll() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<Unidad> unidades = new ArrayList<>();

        String sql = "SELECT id, nombre, abreviatura, createdAt FROM unidad WHERE cliente_id = ? ORDER BY nombre";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Unidad u = new Unidad();
                    u.setId(rs.getLong("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setAbreviatura(rs.getString("abreviatura"));
                    // SQLite guarda fechas como TEXT, parsear a LocalDateTime
                    String createdAtStr = rs.getString("createdAt");
                    if (createdAtStr != null) {
                        u.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                    }
                    unidades.add(u);
                }
            }
        }

        return unidades;
    }

    /**
     * Inserta una nueva unidad
     */
    public static long insert(Unidad unidad) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "INSERT INTO unidad (id, nombre, abreviatura, cliente_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, unidad.getId());
            ps.setString(2, unidad.getNombre());
            ps.setString(3, unidad.getAbreviatura());
            ps.setString(4, clienteId);

            ps.executeUpdate();
            return unidad.getId();
        }
    }

    /**
     * Actualiza una unidad existente
     */
    public static void update(Unidad unidad) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "UPDATE unidad SET nombre = ?, abreviatura = ? WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, unidad.getNombre());
            ps.setString(2, unidad.getAbreviatura());
            ps.setLong(3, unidad.getId());
            ps.setString(4, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró unidad con id=" + unidad.getId());
            }
        }
    }

    /**
     * Elimina una unidad por ID
     */
    public static void deleteById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "DELETE FROM unidad WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró unidad con id=" + id);
            }
        }
    }

    /**
     * Obtiene una unidad por ID
     */
    public static Unidad findById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "SELECT id, nombre, abreviatura, createdAt FROM unidad WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Unidad u = new Unidad();
                    u.setId(rs.getLong("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setAbreviatura(rs.getString("abreviatura"));
                    String createdAtStr = rs.getString("createdAt");
                    if (createdAtStr != null) {
                        u.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                    }
                    return u;
                } else {
                    throw new SQLException("No se encontró unidad con id=" + id);
                }
            }
        }
    }

    /**
     * Cuenta el total de unidades
     */
    public static int count() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        String sql = "SELECT COUNT(*) FROM unidad WHERE cliente_id = ?";

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
