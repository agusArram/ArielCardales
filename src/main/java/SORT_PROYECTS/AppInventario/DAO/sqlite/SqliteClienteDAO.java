package SORT_PROYECTS.AppInventario.DAO.sqlite;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Cliente (clientes del negocio) en SQLite
 * Nota: NO confundir con cliente_id (tenant). Esta tabla almacena los clientes del negocio.
 */
public class SqliteClienteDAO {

    /**
     * Clase Cliente simple para SQLite (solo campos básicos necesarios para sync)
     */
    public static class Cliente {
        private Long id;
        private String nombre;
        private String dni;
        private String telefono;
        private String email;
        private String notas;
        private LocalDateTime createdAt;

        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getDni() { return dni; }
        public void setDni(String dni) { this.dni = dni; }

        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getNotas() { return notas; }
        public void setNotas(String notas) { this.notas = notas; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Obtiene todos los clientes del negocio
     */
    public static List<Cliente> findAll() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        List<Cliente> clientes = new ArrayList<>();

        String sql = """
            SELECT id, nombre, dni, telefono, email, notas, createdAt
            FROM cliente
            WHERE cliente_id = ?
            ORDER BY nombre
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapCliente(rs));
                }
            }
        }

        return clientes;
    }

    /**
     * Inserta un nuevo cliente
     */
    public static long insert(Cliente cliente) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            INSERT INTO cliente (nombre, dni, telefono, email, notas, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDni());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getNotas());
            ps.setString(6, clienteId);

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Error obteniendo ID generado para cliente");
                }
            }
        }
    }

    /**
     * Actualiza un cliente existente
     */
    public static void update(Cliente cliente) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            UPDATE cliente SET
                nombre = ?,
                dni = ?,
                telefono = ?,
                email = ?,
                notas = ?
            WHERE id = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDni());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getNotas());
            ps.setLong(6, cliente.getId());
            ps.setString(7, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró cliente con id=" + cliente.getId());
            }
        }
    }

    /**
     * Elimina un cliente por ID
     */
    public static void deleteById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = "DELETE FROM cliente WHERE id = ? AND cliente_id = ?";

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró cliente con id=" + id);
            }
        }
    }

    /**
     * Obtiene un cliente por ID
     */
    public static Cliente findById(Long id) throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();

        String sql = """
            SELECT id, nombre, dni, telefono, email, notas, createdAt
            FROM cliente
            WHERE id = ? AND cliente_id = ?
        """;

        try (Connection conn = SqliteDatabase.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCliente(rs);
                } else {
                    throw new SQLException("No se encontró cliente con id=" + id);
                }
            }
        }
    }

    /**
     * Cuenta el total de clientes
     */
    public static int count() throws SQLException {
        String clienteId = SqliteDatabase.getClienteId();
        String sql = "SELECT COUNT(*) FROM cliente WHERE cliente_id = ?";

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
     * Mapea ResultSet a Cliente
     */
    private static Cliente mapCliente(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getLong("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDni(rs.getString("dni"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setNotas(rs.getString("notas"));

        String createdAtStr = rs.getString("createdAt");
        if (createdAtStr != null) {
            c.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }

        return c;
    }
}
