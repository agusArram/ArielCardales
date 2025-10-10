package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.ProductoVariante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ProductoVarianteDAO {
    public void insert(ProductoVariante v) throws SQLException {
        String sql = """
            INSERT INTO producto_variante (producto_id, color, talle, precio, costo, stock, active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
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

            ps.executeUpdate();
        }
    }

    public void deleteById(long id) throws SQLException {
        String sql = "DELETE FROM producto_variante WHERE id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}

