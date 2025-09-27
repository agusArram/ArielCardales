package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.Mapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoDAO implements CrudDAO<Producto, Long> {

    // ----- Lectura desde la vista vInventario (recomendada para listar/pantalla) -----

    // Espera que Mapper.getProducto(rs) lea estas columnas:
    // id, etiqueta, nombre, categoria (string), unidad (string), precio, costo, stockOnHand, active, updatedAt
    private static final String BASE_SELECT_VISTA = """
    select p.id,
           p.etiqueta,
           p.nombre,
           p.descripcion,   
           p.categoria,
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
        String sql = BASE_SELECT_VISTA + " order by p.nombre asc";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Producto> out = new ArrayList<>();
            while (rs.next()) out.add(Mapper.getProducto(rs));
            return out;

        } catch (SQLException e) {
            throw new DaoException("Error listando productos", e);
        }
    }

    // Búsqueda usando ilike + similarity (pg_trgm)
    public List<Producto> search(String q, int limit) {
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        int lim = (limit <= 0) ? 50 : limit;

        String sql = BASE_SELECT_VISTA + """
            where p.nombre ilike ? or p.etiqueta ilike ? or p.categoria ilike ?
            order by similarity(p.nombre, ?) desc, p.nombre asc
            limit ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, q);
            ps.setInt(5, lim);

            try (ResultSet rs = ps.executeQuery()) {
                List<Producto> out = new ArrayList<>();
                while (rs.next()) out.add(Mapper.getProducto(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new DaoException("Error buscando productos", e);
        }
    }

    // ----- CRUD “real” contra tabla producto (para ABM) -----
    // Asumo que tu Entidad Producto tiene además categoriaId y unidadId.

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
            throw new DaoException("Error buscando producto id=" + id, e);
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
            //ps.setBigDecimal(7, p.getCosto());
            ps.setInt(8, p.getStockOnHand());
            //ps.setBoolean(9, p.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Error insertando producto", e);
        }
    }

    @Override
    public boolean update(Producto p) {
        String sql = """
            update producto
               set etiqueta = ?,
                   nombre = ?,
                   descripcion = ?,
                   categoriaId = ?,
                   unidadId = ?,
                   precio = ?,
                   costo = ?,
                   stockOnHand = ?,
                   active = ?
             where id = ?
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getEtiqueta());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setLong(4, p.getCategoriaId());
            ps.setLong(5, p.getUnidadId());
            ps.setBigDecimal(6, p.getPrecio());
            //ps.setBigDecimal(7, p.getCosto());
            ps.setInt(8, p.getStockOnHand());
            //ps.setBoolean(9, p.isActive());
            ps.setLong(10, p.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Error actualizando producto id=" + p.getId(), e);
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
            throw new DaoException("Error eliminando producto id=" + id, e);
        }
    }
}
