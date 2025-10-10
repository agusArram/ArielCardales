package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.Mapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoDAO implements CrudDAO<Producto, Long> {

    // Lectura desde la vista vInventario
    // Espera que Mapper.getProducto(rs) lea estas columnas:
    // id, etiqueta, nombre, categoria (string), unidad (string), precio, costo, stockOnHand, active, updatedAt
    private static final String sqlBase = """
    select p.id,
           p.etiqueta,
           p.nombre,
           p.descripcion, p.categoria,
           p.unidad,
           p.precio,
           p.costo,
           p.stockOnHand,
           p.active,
           p.updatedAt
      from vInventario p
    """;

    @Override
    public List<Producto> findAll() {
        String sql = sqlBase + " order by p.nombre asc";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Producto> resultado = new ArrayList<>();
            while (rs.next()) resultado.add(Mapper.getProducto(rs));
            return resultado;

        }catch (SQLException e) {
            e.printStackTrace(); // 🔥 Esto imprime la causa real en consola
            throw new DaoException("Error insertando producto: " + e.getMessage(), e);
        }
    }

    // Búsqueda usando ilike + similarity (pg_trgm), por ahora sin usos
    public List<Producto> search(String q, int limit) {
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        int lim = (limit <= 0) ? 50 : limit;

        String sql = sqlBase + """
            where p.nombre ilike ? or p.etiqueta ilike ? or p.categoria ilike ?
            order by similarity(p.nombre, ?) desc, p.nombre asc
            limit ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            //podria usar mapper aca, para los set
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, q);
            ps.setInt(5, lim);

            try (ResultSet rs = ps.executeQuery()) {
                List<Producto> resultado = new ArrayList<>();
                while (rs.next()) resultado.add(Mapper.getProducto(rs));
                return resultado;
            }
        }catch (SQLException e) {
            e.printStackTrace(); // Esto imprime la causa real en consola
            throw new DaoException("Error insertando producto: " + e.getMessage(), e);
        }
    }
    // ----- CRUD “real” contra tabla producto (para ABM) -----
    @Override
    public Optional<Producto> findById(Long id) {
        String sql = """
           select p.id, p.etiqueta, p.nombre, p.descripcion,
                  p.categoriaId, p.unidadId, p.precio, p.costo,
                  p.stockOnHand, p.active, p.updatedAt
             from producto p
            where p.id = ?
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(Mapper.getProductoBasico(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Esto imprime la causa real
            throw new DaoException("Error insertando producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Long insert(Producto p) {
        String sql = """
            insert into producto (etiqueta, nombre, descripcion, categoriaId, unidadId, precio, costo, stockOnHand, active)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            returning id
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getEtiqueta());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setLong(4, p.getCategoriaId());
            ps.setLong(5, p.getUnidadId());
            ps.setBigDecimal(6, p.getPrecio());
            ps.setBigDecimal(7, p.getCosto());
            ps.setInt(8, p.getStockOnHand());
            ps.setBoolean(9, p.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Esto imprime la causa real en consola
            throw new DaoException("Error insertando producto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Producto p) {
        String sql = """
        update producto
           set nombre = ?,
               descripcion = ?,
               categoriaId = ?,
               precio = ?,
               stockOnHand = ?
         where id = ?
    """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());

            // Si categoriaId == 0 → dejarlo igual que en DB
            if (p.getCategoriaId() == 0) {
                // Busca el original para conservar la categoría
                Optional<Producto> original = findById(p.getId());
                if (original.isPresent()) {
                    ps.setLong(3, original.get().getCategoriaId());
                } else {
                    throw new DaoException("No se encontró producto id=" + p.getId());
                }
            } else {
                ps.setLong(3, p.getCategoriaId());
            }

            ps.setBigDecimal(4, p.getPrecio());
            ps.setInt(5, p.getStockOnHand());
            ps.setLong(6, p.getId());

            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // unique_violation
                throw new DaoException("Ya existe un producto con la etiqueta: " + p.getEtiqueta(), e);
            }
            throw new DaoException("Error insertando producto: " + e.getMessage(), e);
        }
    }

    public boolean existsByEtiqueta(String etiqueta) {
        String sql = "select 1 from producto where etiqueta = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("Error verificando etiqueta", e);
        }
    }

    public String getUltimaEtiqueta() {
        String sql = "select etiqueta from producto order by id desc limit 1";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getString("etiqueta");
            }
            return null; // no hay productos todavía
        } catch (SQLException e) {
            throw new DaoException("Error obteniendo última etiqueta", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "delete from producto where id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error insertando producto: " + e.getMessage(), e);
        }
    }

    public boolean descontarStock(long idProducto, int cantidadVendida) {
        String sql = "UPDATE producto SET stockOnHand = stockOnHand - ? WHERE id = ? AND stockOnHand >= ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, cantidadVendida);
            ps.setLong(2, idProducto);
            ps.setInt(3, cantidadVendida);

            int filas = ps.executeUpdate();
            return filas == 1; // devuelve true si se actualizó correctamente

        } catch (SQLException e) {
            throw new DaoException("Error al descontar stock del producto ID " + idProducto, e);
        }
    }

    public static List<Producto> getProductosBajoStock() {
        List<Producto> productos = new ArrayList<>();

        String sql = sqlBase + " WHERE p.stockOnHand BETWEEN 0 AND 2 ORDER BY p.stockOnHand ASC";

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                productos.add(Mapper.getProducto(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return productos;
    }

    public boolean updateCampo(long idProducto, String campo, String valor) {
        boolean actualizado = false;

        try (Connection conn = Database.get()) {
            campo = campo.trim().toLowerCase();
            String sql;

            // 🔧 Normaliza nombres de campos según la BD real
            switch (campo) {
                case "categoria", "categoría" ->
                        sql = "UPDATE producto SET categoriaid = (SELECT id FROM categoria WHERE LOWER(nombre) = LOWER(?)) WHERE id = ?";
                case "stock", "stockonhand" ->
                        sql = "UPDATE producto SET stockonhand = ? WHERE id = ?";
                case "precio" ->
                        sql = "UPDATE producto SET precio = ? WHERE id = ?";
                case "costo" ->
                        sql = "UPDATE producto SET costo = ? WHERE id = ?";
                default ->
                        sql = "UPDATE producto SET " + campo + " = ? WHERE id = ?";
            }

            System.out.println("🔧 Ejecutando SQL: " + sql);
            System.out.println("📦 Parámetros → valor='" + valor + "' | id=" + idProducto);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, valor);
                stmt.setLong(2, idProducto);
                int filas = stmt.executeUpdate();
                actualizado = filas > 0;
            }

            System.out.println("📊 Filas afectadas: " + (actualizado ? "1" : "0"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return actualizado;
    }








}
