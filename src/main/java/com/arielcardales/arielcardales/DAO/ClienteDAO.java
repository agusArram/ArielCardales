package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Cliente;
import com.arielcardales.arielcardales.Util.Mapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gestión de clientes
 */
public class ClienteDAO implements CrudDAO<Cliente, Long> {

    @Override
    public List<Cliente> findAll() {
        String sql = """
            SELECT id, nombre, dni, telefono, email, notas, createdAt
            FROM cliente
            ORDER BY nombre ASC
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Cliente> clientes = new ArrayList<>();
            while (rs.next()) {
                clientes.add(Mapper.getCliente(rs));
            }
            return clientes;

        } catch (SQLException e) {
            throw new DaoException("Error listando clientes", e);
        }
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        String sql = """
            SELECT id, nombre, dni, telefono, email, notas, createdAt
            FROM cliente
            WHERE id = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Mapper.getCliente(rs));
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Error buscando cliente por ID: " + id, e);
        }

        return Optional.empty();
    }

    @Override
    public Long insert(Cliente cliente) {
        String sql = """
            INSERT INTO cliente (nombre, dni, telefono, email, notas)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDni());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getNotas());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DaoException("No se pudo insertar el cliente");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    cliente.setId(id);
                    return id;
                }
            }

            throw new DaoException("No se pudo obtener el ID generado");

        } catch (SQLException e) {
            throw new DaoException("Error insertando cliente: " + cliente.getNombre(), e);
        }
    }

    @Override
    public boolean update(Cliente cliente) {
        String sql = """
            UPDATE cliente
            SET nombre = ?, dni = ?, telefono = ?, email = ?, notas = ?
            WHERE id = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDni());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getNotas());
            ps.setLong(6, cliente.getId());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DaoException("No se encontró cliente con ID: " + cliente.getId());
            }

            return true;

        } catch (SQLException e) {
            throw new DaoException("Error actualizando cliente ID: " + cliente.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM cliente WHERE id = ?";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DaoException("No se encontró cliente con ID: " + id);
            }

            return true;

        } catch (SQLException e) {
            throw new DaoException("Error eliminando cliente ID: " + id, e);
        }
    }

    /**
     * Busca clientes por nombre (búsqueda parcial, insensible a mayúsculas)
     */
    public List<Cliente> buscarPorNombre(String nombre) {
        String sql = """
            SELECT id, nombre, dni, telefono, email, notas, createdAt
            FROM cliente
            WHERE LOWER(nombre) LIKE LOWER(?)
            ORDER BY nombre ASC
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, "%" + nombre + "%");

            try (ResultSet rs = ps.executeQuery()) {
                List<Cliente> clientes = new ArrayList<>();
                while (rs.next()) {
                    clientes.add(Mapper.getCliente(rs));
                }
                return clientes;
            }

        } catch (SQLException e) {
            throw new DaoException("Error buscando clientes por nombre: " + nombre, e);
        }
    }

    /**
     * Busca un cliente por DNI exacto
     */
    public Optional<Cliente> buscarPorDni(String dni) {
        String sql = """
            SELECT id, nombre, dni, telefono, email, notas, createdAt
            FROM cliente
            WHERE dni = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, dni);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Mapper.getCliente(rs));
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Error buscando cliente por DNI: " + dni, e);
        }

        return Optional.empty();
    }

    /**
     * Busca un cliente por teléfono exacto
     */
    public Optional<Cliente> buscarPorTelefono(String telefono) {
        String sql = """
            SELECT id, nombre, dni, telefono, email, notas, createdAt
            FROM cliente
            WHERE telefono = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, telefono);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Mapper.getCliente(rs));
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Error buscando cliente por teléfono: " + telefono, e);
        }

        return Optional.empty();
    }

    /**
     * Verifica si existe un cliente con el mismo DNI (excluyendo un ID específico, útil para updates)
     */
    public boolean existeDniDuplicado(String dni, Long idExcluido) {
        String sql = """
            SELECT COUNT(*) FROM cliente
            WHERE dni = ? AND id != ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, dni);
            ps.setLong(2, idExcluido != null ? idExcluido : -1L);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Error verificando DNI duplicado", e);
        }

        return false;
    }

    /**
     * Verifica si existe un cliente con el mismo teléfono (excluyendo un ID específico)
     */
    public boolean existeTelefonoDuplicado(String telefono, Long idExcluido) {
        String sql = """
            SELECT COUNT(*) FROM cliente
            WHERE telefono = ? AND id != ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, telefono);
            ps.setLong(2, idExcluido != null ? idExcluido : -1L);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Error verificando teléfono duplicado", e);
        }

        return false;
    }
}
