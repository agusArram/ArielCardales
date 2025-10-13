package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Venta;
import com.arielcardales.arielcardales.Entidades.VentaItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VentaDAO {

    public long registrarVenta(Venta venta) throws SQLException {
        String sql = """
            INSERT INTO venta (clienteNombre, medioPago, total)
            VALUES (?, ?, ?)
            RETURNING id
        """;
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, venta.getClienteNombre());
            ps.setString(2, venta.getMedioPago());
            ps.setBigDecimal(3, venta.getTotal());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("id");
            else throw new SQLException("No se pudo registrar la venta");
        }
    }

    public void registrarItem(VentaItem item) throws SQLException {
        String sql = """
            INSERT INTO ventaItem (ventaId, productoId, qty, precioUnit)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getVentaId());
            ps.setLong(2, item.getProductoId());
            ps.setInt(3, item.getCantidad());
            ps.setBigDecimal(4, item.getPrecioUnit());
            ps.executeUpdate();
        }
    }
}

