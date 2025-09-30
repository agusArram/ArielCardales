package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Categoria;
import com.arielcardales.arielcardales.Util.Mapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CategoriaDAO implements CrudDAO<Categoria, Long> {

    // Lista con self-join para mostrar nombre del padre
    public List<Categoria> findAllWithParent() {
        String sql = """
            select c.id,
                   c.nombre,
                   c.parentId,
                   p.nombre as parentNombre,
                   c.createdAt
              from categoria c
              left join categoria p on p.id = c.parentId
             order by c.nombre asc
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Categoria> out = new ArrayList<>();
            while (rs.next()) out.add(Mapper.getCategoriaConPadre(rs));
            return out;

        } catch (SQLException e) {
            throw new DaoException("Error listando categorías", e);
        }
    }

    @Override
    public List<Categoria> findAll() {
        String sql = "select id, nombre, parentId, createdAt from categoria order by nombre";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Categoria> out = new ArrayList<>();
            while (rs.next()) out.add(Mapper.getCategoria(rs));
            return out;
        } catch (SQLException e) {
            throw new DaoException("Error listando categorías (básico)", e);
        }
    }

    @Override
    public Optional<Categoria> findById(Long id) {
        String sql = "select id, nombre, parentId, createdAt from categoria where id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(Mapper.getCategoria(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Error buscando categoría id=" + id, e);
        }
    }

    @Override
    public Long insert(Categoria cat) {
        String sql = "insert into categoria (nombre, parentId) values (?, ?) returning id";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getNombre());
            if (cat.getParentId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, cat.getParentId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Error insertando categoría", e);
        }
    }



    @Override
    public boolean update(Categoria cat) {
        String sql = "update categoria set nombre = ?, parentId = ? where id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getNombre());
            if (cat.getParentId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, cat.getParentId());
            ps.setLong(3, cat.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Error actualizando categoría id=" + cat.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "delete from categoria where id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            // Si tiene hijos o productos, fallará por FK. Podés capturar el SQLState si querés mensaje más claro.
            throw new DaoException("Error eliminando categoría id=" + id, e);
        }
    }

    public Map<String, Long> mapNombreId() {
        String sql = "select id, nombre from categoria order by nombre";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<String, Long> out = new java.util.LinkedHashMap<>();
            while (rs.next()) out.put(rs.getString("nombre"), rs.getLong("id"));
            return out;
        } catch (SQLException e) {
            throw new DaoException("Error listando categorías", e);
        }
    }

}
