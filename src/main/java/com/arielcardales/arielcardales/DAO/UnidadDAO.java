package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Unidad;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnidadDAO {

    // Devuelve todas las unidades como objetos
    public static List<Unidad> getAll() {
        List<Unidad> unidades = new ArrayList<>();
        String sql = """
            SELECT 
                id,
                nombre,
                abreviatura,
                createdAt
            FROM unidad
            ORDER BY nombre
        """;

        try (Connection conn = Database.get();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Unidad u = new Unidad();
                u.setId(rs.getLong("id"));
                u.setNombre(rs.getString("nombre"));
                u.setAbreviatura(rs.getString("abreviatura"));
                u.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
                unidades.add(u);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return unidades;
    }

    // Igual que CategoriaDAO: devuelve mapa {nombre -> id}
    public Map<String, Long> mapNombreId() {
        Map<String, Long> map = new HashMap<>();
        String sql = "SELECT id, nombre FROM unidad ORDER BY nombre";

        try (Connection conn = Database.get();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                map.put(rs.getString("nombre"), rs.getLong("id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}
