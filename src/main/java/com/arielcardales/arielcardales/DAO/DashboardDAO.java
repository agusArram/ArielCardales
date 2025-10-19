package com.arielcardales.arielcardales.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardDAO {

    /**
     * Obtiene métricas generales del inventario
     * @return Map con claves: totalProductos, stockBajo, sinStock, valorTotal
     */
    public static Map<String, Object> obtenerMetricasInventario() {
        Map<String, Object> metricas = new HashMap<>();

        String sql = """
            SELECT
                COUNT(DISTINCT producto_id) as totalProductos,
                COUNT(DISTINCT CASE WHEN stockOnHand > 0 AND stockOnHand <= 5 THEN variante_id END) as stockBajo,
                COUNT(DISTINCT CASE WHEN stockOnHand = 0 THEN variante_id END) as sinStock,
                COALESCE(SUM(stockOnHand * precio), 0) as valorTotal
            FROM vInventario_variantes
            WHERE active = true
            """;

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                metricas.put("totalProductos", rs.getInt("totalProductos"));
                metricas.put("stockBajo", rs.getInt("stockBajo"));
                metricas.put("sinStock", rs.getInt("sinStock"));
                metricas.put("valorTotal", rs.getDouble("valorTotal"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Valores por defecto en caso de error
            metricas.put("totalProductos", 0);
            metricas.put("stockBajo", 0);
            metricas.put("sinStock", 0);
            metricas.put("valorTotal", 0.0);
        }

        return metricas;
    }

    /**
     * Representa un producto en el ranking de ventas
     */
    public static class ProductoVendido {
        private String etiqueta;
        private String nombre;
        private int cantidadVendida;
        private double ganancia;

        public ProductoVendido(String etiqueta, String nombre, int cantidadVendida, double ganancia) {
            this.etiqueta = etiqueta;
            this.nombre = nombre;
            this.cantidadVendida = cantidadVendida;
            this.ganancia = ganancia;
        }

        public String getEtiqueta() { return etiqueta; }
        public String getNombre() { return nombre; }
        public int getCantidadVendida() { return cantidadVendida; }
        public double getGanancia() { return ganancia; }
    }

    /**
     * Obtiene el top N de productos más vendidos (últimos 30 días)
     */
    public static List<ProductoVendido> obtenerTopProductosVendidos(int limit) {
        List<ProductoVendido> productos = new ArrayList<>();

        String sql = """
            SELECT
                vi.productoNombre as nombre,
                '' as etiqueta,
                SUM(vi.qty) as cantidad_vendida,
                SUM(vi.qty * vi.precioUnit) as total_ventas
            FROM ventaItem vi
            JOIN venta v ON v.id = vi.ventaId
            WHERE v.fecha >= NOW() - INTERVAL '30 days'
            GROUP BY vi.productoNombre
            ORDER BY cantidad_vendida DESC
            LIMIT ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(new ProductoVendido(
                        rs.getString("etiqueta"),
                        rs.getString("nombre"),
                        rs.getInt("cantidad_vendida"),
                        rs.getDouble("total_ventas")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return productos;
    }

    /**
     * Obtiene métricas de ventas (últimos 30 días)
     * @return Map con claves: totalVentas, ventasHoy, promedioVentaDiaria
     */
    public static Map<String, Object> obtenerMetricasVentas() {
        Map<String, Object> metricas = new HashMap<>();

        String sql = """
            SELECT
                COUNT(*) as totalVentas,
                SUM(total) as montoTotal,
                SUM(CASE WHEN fecha::date = CURRENT_DATE THEN total ELSE 0 END) as ventasHoy
            FROM venta
            WHERE fecha >= NOW() - INTERVAL '30 days'
            """;

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int totalVentas = rs.getInt("totalVentas");
                double montoTotal = rs.getDouble("montoTotal");
                double ventasHoy = rs.getDouble("ventasHoy");
                double promedioVentaDiaria = totalVentas > 0 ? montoTotal / 30.0 : 0;

                metricas.put("totalVentas", totalVentas);
                metricas.put("montoTotal", montoTotal);
                metricas.put("ventasHoy", ventasHoy);
                metricas.put("promedioVentaDiaria", promedioVentaDiaria);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            metricas.put("totalVentas", 0);
            metricas.put("montoTotal", 0.0);
            metricas.put("ventasHoy", 0.0);
            metricas.put("promedioVentaDiaria", 0.0);
        }

        return metricas;
    }
}
