package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Cliente;
import com.arielcardales.arielcardales.Entidades.ItemCliente;
import com.arielcardales.arielcardales.Entidades.Venta;
import com.arielcardales.arielcardales.Util.Mapper;
import javafx.scene.control.TreeItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Carga el árbol completo de clientes con sus ventas
     * Similar a InventarioDAO.cargarArbol()
     *
     * @param filtro Criterio de búsqueda (nombre o DNI)
     * @return TreeItem raíz con clientes como padres y ventas como hijos
     */
    public TreeItem<ItemCliente> cargarArbol(String filtro) throws SQLException {
        String f = filtro == null ? "" : filtro.trim();
        String like = "%" + f + "%";

        TreeItem<ItemCliente> root = new TreeItem<>();
        Map<Long, TreeItem<ItemCliente>> padres = new LinkedHashMap<>();

        // Consulta que trae clientes con sus ventas (LEFT JOIN para traer clientes sin ventas)
        String sql = """
            SELECT
                c.id as cliente_id,
                c.nombre as cliente_nombre,
                c.dni as cliente_dni,
                c.telefono as cliente_telefono,
                c.email as cliente_email,
                c.notas as cliente_notas,
                v.id as venta_id,
                v.fecha as venta_fecha,
                v.medioPago as venta_medio,
                v.total as venta_total
            FROM cliente c
            LEFT JOIN venta v ON v.clienteId = c.id
            WHERE (? = ''
                OR LOWER(c.nombre) LIKE LOWER(?)
                OR LOWER(COALESCE(c.dni, '')) LIKE LOWER(?))
            ORDER BY c.nombre, v.fecha DESC
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, f);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long clienteId = rs.getLong("cliente_id");
                    Long ventaId = (Long) rs.getObject("venta_id");

                    // Si el cliente no existe en el mapa, crearlo
                    TreeItem<ItemCliente> padre = padres.get(clienteId);
                    if (padre == null) {
                        ItemCliente clienteItem = new ItemCliente();
                        clienteItem.setEsVenta(false);
                        clienteItem.setClienteId(clienteId);
                        clienteItem.setNombre(rs.getString("cliente_nombre"));
                        clienteItem.setDni(rs.getString("cliente_dni"));
                        clienteItem.setTelefono(rs.getString("cliente_telefono"));
                        clienteItem.setEmail(rs.getString("cliente_email"));
                        clienteItem.setNotas(rs.getString("cliente_notas"));

                        padre = new TreeItem<>(clienteItem);
                        padres.put(clienteId, padre);
                        root.getChildren().add(padre);
                    }

                    // Si hay una venta, agregarla como hijo
                    if (ventaId != null) {
                        ItemCliente ventaItem = new ItemCliente();
                        ventaItem.setEsVenta(true);
                        ventaItem.setVentaId(ventaId);

                        Timestamp fechaTs = rs.getTimestamp("venta_fecha");
                        if (fechaTs != null) {
                            ventaItem.setFecha(fechaTs.toLocalDateTime());
                        }

                        ventaItem.setMedioPago(rs.getString("venta_medio"));
                        ventaItem.setTotal(rs.getBigDecimal("venta_total"));

                        padre.getChildren().add(new TreeItem<>(ventaItem));
                    }
                }
            }
        }

        return root;
    }
}
