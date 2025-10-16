package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.ProductoVariante;
import com.arielcardales.arielcardales.Util.Mapper;

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
        String sql = """
            SELECT id, producto_id, color, talle, precio, costo, stock, 
                   etiqueta, active, createdAt, updatedAt
            FROM producto_variante
            ORDER BY producto_id, color, talle
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<ProductoVariante> resultado = new ArrayList<>();
            while (rs.next()) {
                resultado.add(Mapper.getProductoVariante(rs));
            }
            return resultado;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error obteniendo todas las variantes: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ProductoVariante> findById(Long id) {
        String sql = """
            SELECT id, producto_id, color, talle, precio, costo, stock,
                   etiqueta, active, createdAt, updatedAt
            FROM producto_variante
            WHERE id = ?
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

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
        String sql = """
            INSERT INTO producto_variante (producto_id, color, talle, precio, costo, stock, active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, v.getProductoId());
            ps.setString(2, v.getColor());
            ps.setString(3, v.getTalle());
            ps.setBigDecimal(4, v.getPrecio());
            ps.setBigDecimal(5, v.getCosto());
            ps.setInt(6, v.getStock());
            ps.setBoolean(7, v.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new DaoException("No se pudo obtener el ID de la variante insertada");
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
        String sql = """
            UPDATE producto_variante
            SET color = ?, talle = ?, precio = ?, costo = ?, stock = ?, active = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, v.getColor());
            ps.setString(2, v.getTalle());
            ps.setBigDecimal(3, v.getPrecio());
            ps.setBigDecimal(4, v.getCosto());
            ps.setInt(5, v.getStock());
            ps.setBoolean(6, v.isActive());
            ps.setLong(7, v.getId());

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error actualizando variante: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM producto_variante WHERE id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error eliminando variante: " + e.getMessage(), e);
        }
    }

    // ========================================
    // MÉTODOS ESPECÍFICOS
    // ========================================

    /**
     * Obtiene todas las variantes de un producto específico
     */
    public List<ProductoVariante> findByProductoId(Long productoId) {
        String sql = """
            SELECT id, producto_id, color, talle, precio, costo, stock,
                   etiqueta, active, createdAt, updatedAt
            FROM producto_variante
            WHERE producto_id = ?
            ORDER BY color, talle
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productoId);

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
        String sql = """
            UPDATE producto_variante
            SET stock = stock - ?
            WHERE id = ? AND stock >= ?
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cantidad);
            ps.setLong(2, idVariante);
            ps.setInt(3, cantidad);

            int filas = ps.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error descontando stock de variante ID " + idVariante, e);
        }
    }

    /**
     * Actualiza un campo específico de una variante (usado en edición inline)
     */
    public boolean updateCampo(long idVariante, String campo, String valor) {
        boolean actualizado = false;

        try (Connection conn = Database.get()) {
            campo = campo.trim().toLowerCase();
            String sql;

            // 🔧 Normaliza nombres de campos según la BD real
            switch (campo) {
                case "stock" ->
                        sql = "UPDATE producto_variante SET stock = ? WHERE id = ?";
                case "precio" ->
                        sql = "UPDATE producto_variante SET precio = ? WHERE id = ?";
                case "costo" ->
                        sql = "UPDATE producto_variante SET costo = ? WHERE id = ?";
                case "color" ->
                        sql = "UPDATE producto_variante SET color = ? WHERE id = ?";
                case "talle" ->
                        sql = "UPDATE producto_variante SET talle = ? WHERE id = ?";
                default ->
                        sql = "UPDATE producto_variante SET " + campo + " = ? WHERE id = ?";
            }

            System.out.println("🔧 Ejecutando SQL (variante): " + sql);
            System.out.println("📦 Parámetros → valor='" + valor + "' | id=" + idVariante);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, valor);
                stmt.setLong(2, idVariante);
                int filas = stmt.executeUpdate();
                actualizado = filas > 0;
            }

            System.out.println("📊 Filas afectadas: " + (actualizado ? "1" : "0"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return actualizado;
    }

    /**
     * Verifica si existe una variante con el mismo color y talle para un producto
     */
    public boolean existeVariante(Long productoId, String color, String talle) {
        String sql = """
            SELECT 1 FROM producto_variante
            WHERE producto_id = ? AND LOWER(color) = LOWER(?) AND LOWER(talle) = LOWER(?)
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productoId);
            ps.setString(2, color);
            ps.setString(3, talle);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error verificando existencia de variante", e);
        }
    }
}