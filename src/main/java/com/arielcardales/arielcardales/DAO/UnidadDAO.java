package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Unidad;
import com.arielcardales.arielcardales.session.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnidadDAO {

    // Devuelve todas las unidades como objetos (filtrado por cliente_id)
    public static List<Unidad> getAll() {
        String clienteId = SessionManager.getInstance().getClienteId();
        List<Unidad> unidades = new ArrayList<>();
        String sql = """
            SELECT
                id,
                nombre,
                abreviatura,
                createdAt
            FROM unidad
            WHERE cliente_id = ?
            ORDER BY nombre
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Unidad u = new Unidad();
                    u.setId(rs.getLong("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setAbreviatura(rs.getString("abreviatura"));
                    u.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
                    unidades.add(u);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return unidades;
    }

    // Igual que CategoriaDAO: devuelve mapa {nombre -> id} (filtrado por cliente_id)
    public Map<String, Long> mapNombreId() {
        String clienteId = SessionManager.getInstance().getClienteId();
        Map<String, Long> map = new HashMap<>();
        String sql = "SELECT id, nombre FROM unidad WHERE cliente_id = ? ORDER BY nombre";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("nombre"), rs.getLong("id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}
