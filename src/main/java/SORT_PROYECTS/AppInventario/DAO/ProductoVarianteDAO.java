package SORT_PROYECTS.AppInventario.DAO;

import SORT_PROYECTS.AppInventario.Entidades.ProductoVariante;
import SORT_PROYECTS.AppInventario.Util.Mapper;
import SORT_PROYECTS.AppInventario.session.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoVarianteDAO implements CrudDAO<ProductoVariante, Long> {

    // ========================================
    // CRUD BÁSICO
    // ========================================

    @Override
    public List<ProductoVariante> findAll() {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = """
            SELECT pv.id, pv.producto_id, pv.color, pv.talle, pv.precio, pv.costo, pv.stock,
                   pv.etiqueta, pv.active, pv.createdAt, pv.updatedAt
            FROM producto_variante pv
            JOIN producto p ON p.id = pv.producto_id
            WHERE pv.active = true
              AND p.cliente_id = ?
            ORDER BY pv.producto_id, pv.color, pv.talle
        """;

        try (Connection conn = Database.getWithFallback();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                List<ProductoVariante> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(Mapper.getProductoVariante(rs));
                }
                return resultado;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error obteniendo todas las variantes: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ProductoVariante> findById(Long id) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = """
            SELECT pv.id, pv.producto_id, pv.color, pv.talle, pv.precio, pv.costo, pv.stock,
                   pv.etiqueta, pv.active, pv.createdAt, pv.updatedAt
            FROM producto_variante pv
            JOIN producto p ON p.id = pv.producto_id
            WHERE pv.id = ?
              AND p.cliente_id = ?
        """;

        try (Connection conn = Database.getWithFallback();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Mapper.getProductoVariante(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error buscando variante por ID: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public Long insert(ProductoVariante v) {
        String clienteId = SessionManager.getInstance().getClienteId();

        // Verificar que el producto pertenece al cliente
        String sqlCheck = "SELECT 1 FROM producto WHERE id = ? AND cliente_id = ?";
        String sql = """
            INSERT INTO producto_variante (producto_id, color, talle, precio, costo, stock, active, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;


        try (Connection conn = Database.getWithFallback()) {
            // Verificar propiedad del producto
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setLong(1, v.getProductoId());
                psCheck.setString(2, clienteId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        throw new DaoException("El producto no pertenece al cliente actual");
                    }
                }
            }

            // Insertar variante
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, v.getProductoId());
                ps.setString(2, v.getColor());
                ps.setString(3, v.getTalle());
                ps.setBigDecimal(4, v.getPrecio());
                ps.setBigDecimal(5, v.getCosto());
                ps.setInt(6, v.getStock());
                ps.setBoolean(7, v.isActive());
                ps.setString(8, clienteId); // 🔹 Agregado aquí

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    throw new DaoException("No se pudo obtener el ID de la variante insertada");
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getSQLState().equals("23505")) { // unique_violation
                throw new DaoException("Ya existe una variante con ese color y talle para este producto", e);
            }
            throw new DaoException("Error insertando variante: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(ProductoVariante v) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = """
            UPDATE producto_variante pv
            SET color = ?, talle = ?, precio = ?, costo = ?, stock = ?, active = ?
            FROM producto p
            WHERE pv.id = ?
              AND pv.producto_id = p.id
              AND p.cliente_id = ?
        """;

        try (Connection conn = Database.getWithFallback();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, v.getColor());
            ps.setString(2, v.getTalle());
            ps.setBigDecimal(3, v.getPrecio());
            ps.setBigDecimal(4, v.getCosto());
            ps.setInt(5, v.getStock());
            ps.setBoolean(6, v.isActive());
            ps.setLong(7, v.getId());
            ps.setString(8, clienteId);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error actualizando variante: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String clienteId = SessionManager.getInstance().getClienteId();
        // Soft delete: marcar como inactivo en lugar de eliminar físicamente
        String sql = """
            UPDATE producto_variante pv
            SET active = false
            FROM producto p
            WHERE pv.id = ?
              AND pv.producto_id = p.id
              AND p.cliente_id = ?
        """;

        try (Connection conn = Database.getWithFallback();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, clienteId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error deshabilitando variante: " + e.getMessage(), e);
        }
    }

    // ========================================
    // MÉTODOS ESPECÍFICOS
    // ========================================

    /**
     * Obtiene todas las variantes de un producto específico
     */
    public List<ProductoVariante> findByProductoId(Long productoId) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = """
            SELECT pv.id, pv.producto_id, pv.color, pv.talle, pv.precio, pv.costo, pv.stock,
                   pv.etiqueta, pv.active, pv.createdAt, pv.updatedAt
            FROM producto_variante pv
            JOIN producto p ON p.id = pv.producto_id
            WHERE pv.producto_id = ?
              AND pv.active = true
              AND p.cliente_id = ?
            ORDER BY pv.color, pv.talle
        """;

        try (Connection conn = Database.getWithFallback();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productoId);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                List<ProductoVariante> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(Mapper.getProductoVariante(rs));
                }
                return resultado;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error obteniendo variantes del producto: " + e.getMessage(), e);
        }
    }

    /**
     * Descuenta stock de una variante (para ventas)
     * Retorna true solo si había suficiente stock
     */
    public boolean descontarStock(long idVariante, int cantidad) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = """
            UPDATE producto_variante pv
            SET stock = stock - ?
            FROM producto p
            WHERE pv.id = ?
              AND pv.stock >= ?
              AND pv.producto_id = p.id
              AND p.cliente_id = ?
        """;

        try (Connection conn = Database.getWithFallback();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cantidad);
            ps.setLong(2, idVariante);
            ps.setInt(3, cantidad);
            ps.setString(4, clienteId);

            int filas = ps.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error descontando stock de variante ID " + idVariante, e);
        }
    }

    /**
     * Actualiza un campo específico de una variante
     * INCLUYE validación de duplicados para color/talle
     *
     * @param idVariante ID de la variante a actualizar
     * @param campo Nombre del campo (stock, precio, costo, color, talle)
     * @param valor Nuevo valor como String
     * @return true si la actualización fue exitosa, false si falló o hay duplicado
     */
    public boolean updateCampo(Long idVariante, String campo, String valor) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String columna = campo.trim().toLowerCase();

        // Mapear nombres del modelo Java a columnas reales de la BD
        if (columna.equalsIgnoreCase("stockOnHand")) columna = "stock";

        try (Connection conn = Database.getWithFallback()) {
            // ✅ VALIDACIÓN: Si edita color o talle, verificar duplicados
            if (columna.equalsIgnoreCase("color") || columna.equalsIgnoreCase("talle")) {
                // 1. Obtener datos actuales de la variante (con cliente_id check)
                String sqlActual = """
                    SELECT pv.producto_id, pv.color, pv.talle
                    FROM producto_variante pv
                    JOIN producto p ON p.id = pv.producto_id
                    WHERE pv.id = ? AND p.cliente_id = ?
                """;
                Long productoId;
                String colorActual, talleActual;

                try (PreparedStatement ps = conn.prepareStatement(sqlActual)) {
                    ps.setLong(1, idVariante);
                    ps.setString(2, clienteId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return false;
                        productoId = rs.getLong("producto_id");
                        colorActual = rs.getString("color");
                        talleActual = rs.getString("talle");
                    }
                }

                // 2. Determinar valores nuevos
                String nuevoColor = columna.equalsIgnoreCase("color") ? valor : colorActual;
                String nuevoTalle = columna.equalsIgnoreCase("talle") ? valor : talleActual;

                // 3. Verificar si ya existe otra variante con esa combinación
                String sqlCheck = "SELECT id FROM producto_variante " +
                        "WHERE producto_id = ? AND color = ? AND talle = ? AND id != ?";

                try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                    ps.setLong(1, productoId);
                    ps.setString(2, nuevoColor);
                    ps.setString(3, nuevoTalle);
                    ps.setLong(4, idVariante);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // ❌ Ya existe una variante con esa combinación
                            System.err.println("⚠️ Duplicado: Ya existe variante con color=" + nuevoColor + " y talle=" + nuevoTalle);
                            return false;
                        }
                    }
                }
            }

            // 4. Si pasó la validación, hacer el UPDATE
            String sql = """
                UPDATE producto_variante pv
                SET %s = ?
                FROM producto p
                WHERE pv.id = ?
                  AND pv.producto_id = p.id
                  AND p.cliente_id = ?
            """.formatted(columna);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, valor);
                stmt.setLong(2, idVariante);
                stmt.setString(3, clienteId);
                int filas = stmt.executeUpdate();
                return filas > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si existe una variante con el mismo color y talle para un producto
     */
    public boolean existeVariante(Long productoId, String color, String talle) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = """
            SELECT 1 FROM producto_variante pv
            JOIN producto p ON p.id = pv.producto_id
            WHERE pv.producto_id = ?
              AND LOWER(pv.color) = LOWER(?)
              AND LOWER(pv.talle) = LOWER(?)
              AND p.cliente_id = ?
        """;

        try (Connection conn = Database.getWithFallback();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productoId);
            ps.setString(2, color);
            ps.setString(3, talle);
            ps.setString(4, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error verificando existencia de variante", e);
        }
    }
}