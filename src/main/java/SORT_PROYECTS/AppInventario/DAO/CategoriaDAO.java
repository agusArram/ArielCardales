package SORT_PROYECTS.AppInventario.DAO;

import SORT_PROYECTS.AppInventario.Entidades.Categoria;
import SORT_PROYECTS.AppInventario.Util.Mapper;
import SORT_PROYECTS.AppInventario.session.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CategoriaDAO implements CrudDAO<Categoria, Long> {

    // Lista con self-join para mostrar nombre del padre (filtrado por cliente_id)
    public List<Categoria> findAllWithParent() {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = """
            select c.id,
                   c.nombre,
                   c.parentId,
                   p.nombre as parentNombre,
                   c.createdAt
              from categoria c
              left join categoria p on p.id = c.parentId AND p.cliente_id = ?
             where c.cliente_id = ?
             order by c.nombre asc
        """;
        try (Connection c = Database.getWithFallback();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, clienteId);
            ps.setString(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Categoria> out = new ArrayList<>();
                while (rs.next()) out.add(Mapper.getCategoriaConPadre(rs));
                return out;
            }

        } catch (SQLException e) {
            throw new DaoException("Error listando categorías", e);
        }
    }

    @Override
    public List<Categoria> findAll() {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = "select id, nombre, parentId, createdAt from categoria where cliente_id = ? order by nombre";
        try (Connection c = Database.getWithFallback();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Categoria> out = new ArrayList<>();
                while (rs.next()) out.add(Mapper.getCategoria(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new DaoException("Error listando categorías (básico)", e);
        }
    }

    @Override
    public Optional<Categoria> findById(Long id) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = "select id, nombre, parentId, createdAt from categoria where id = ? AND cliente_id = ?";
        try (Connection c = Database.getWithFallback();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, clienteId);
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
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = "insert into categoria (nombre, parentId, cliente_id) values (?, ?, ?) returning id";
        try (Connection c = Database.getWithFallback();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getNombre());
            if (cat.getParentId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, cat.getParentId());
            ps.setString(3, clienteId);
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
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = "update categoria set nombre = ?, parentId = ? where id = ? AND cliente_id = ?";
        try (Connection c = Database.getWithFallback();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getNombre());
            if (cat.getParentId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, cat.getParentId());
            ps.setLong(3, cat.getId());
            ps.setString(4, clienteId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Error actualizando categoría id=" + cat.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = "delete from categoria where id = ? AND cliente_id = ?";
        try (Connection c = Database.getWithFallback();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, clienteId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            // Si tiene hijos o productos, fallará por FK. Podés capturar el SQLState si querés mensaje más claro.
            throw new DaoException("Error eliminando categoría id=" + id, e);
        }
    }

    public Map<String, Long> mapNombreId() {
        String clienteId = SessionManager.getInstance().getClienteId();
        String sql = "select id, nombre from categoria where cliente_id = ? order by nombre";
        try (Connection c = Database.getWithFallback();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Long> out = new java.util.LinkedHashMap<>();
                while (rs.next()) out.put(rs.getString("nombre"), rs.getLong("id"));
                return out;
            }
        } catch (SQLException e) {
            throw new DaoException("Error listando categorías", e);
        }
    }

}
