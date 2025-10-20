package com.arielcardales.arielcardales.DAO;

import java.sql.*;
import java.util.*;

public class RentabilidadDAO {

    public static class ProductoRentabilidad {
        private String nombre;
        private String categoria;
        private double precioVenta;
        private double costo;
        private double margenPorcentaje;
        private double gananciaUnitaria;
        private int cantidadVendida;
        private double gananciaTotal;

        public ProductoRentabilidad(String nombre, String categoria, double precioVenta, double costo, int cantidadVendida) {
            this.nombre = nombre;
            this.categoria = categoria;
            this.precioVenta = precioVenta;
            this.costo = costo;
            this.cantidadVendida = cantidadVendida;
            this.gananciaUnitaria = precioVenta - costo;
            this.gananciaTotal = gananciaUnitaria * cantidadVendida;
            this.margenPorcentaje = costo > 0 ? ((precioVenta - costo) / precioVenta) * 100 : 0;
        }

        public String getNombre() { return nombre; }
        public String getCategoria() { return categoria; }
        public double getPrecioVenta() { return precioVenta; }
        public double getCosto() { return costo; }
        public double getMargenPorcentaje() { return margenPorcentaje; }
        public double getGananciaUnitaria() { return gananciaUnitaria; }
        public int getCantidadVendida() { return cantidadVendida; }
        public double getGananciaTotal() { return gananciaTotal; }
    }

    // 📊 MÉTRICAS GENERALES
    public static Map<String, Object> obtenerMetricasRentabilidad(int dias) {
        Map<String, Object> metricas = new HashMap<>();

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_id,
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) AS costo
                FROM vInventario_variantes v
                WHERE v.active = true
                GROUP BY v.producto_id, v.producto_nombre
            ),
            ventas_periodo AS (
                SELECT
                    pc.producto_nombre,
                    AVG(vi.precioUnit) AS precio_venta,
                    COALESCE(MAX(pc.costo), 0) AS costo,
                    SUM(vi.qty) AS cantidad_vendida
                FROM ventaItem vi
                JOIN venta v2 ON v2.id = vi.ventaId
                JOIN producto p ON p.id = vi.productoId
                LEFT JOIN productos_costo pc ON pc.producto_id = p.id
                WHERE v2.fecha >= NOW() - INTERVAL '%d days'
                GROUP BY pc.producto_nombre
            )
            SELECT
                COALESCE(AVG(CASE WHEN precio_venta > 0 THEN ((precio_venta - costo) / precio_venta) * 100 END), 0) AS margen_promedio,
                COALESCE(SUM((precio_venta - costo) * cantidad_vendida), 0) AS ganancia_total,
                COALESCE(SUM(precio_venta * cantidad_vendida), 0) AS ventas_totales,
                COALESCE(SUM(costo * cantidad_vendida), 0) AS costos_totales
            FROM ventas_periodo
            """.formatted(dias);


        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                metricas.put("margenPromedio", rs.getDouble("margen_promedio"));
                metricas.put("gananciaTotal", rs.getDouble("ganancia_total"));
                metricas.put("ventasTotales", rs.getDouble("ventas_totales"));
                metricas.put("costosTotales", rs.getDouble("costos_totales"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return metricas;
    }

    // 💰 PRODUCTO MÁS RENTABLE
    public static Map<String, Object> obtenerProductoMasRentable(int dias) {
        Map<String, Object> producto = new HashMap<>();

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_id,
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) AS costo
                FROM vInventario_variantes v
                WHERE v.active = true
                GROUP BY v.producto_id, v.producto_nombre
            )
            SELECT
                pc.producto_nombre AS nombre,
                SUM(vi.qty * (vi.precioUnit - COALESCE(pc.costo, 0))) AS ganancia_total
            FROM ventaItem vi
            JOIN venta v2 ON v2.id = vi.ventaId
            JOIN producto p ON p.id = vi.productoId
            LEFT JOIN productos_costo pc ON pc.producto_id = p.id
            WHERE v2.fecha >= NOW() - INTERVAL '%d days'
            GROUP BY pc.producto_nombre
            ORDER BY ganancia_total DESC
            LIMIT 1
            """.formatted(dias);


        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                producto.put("nombre", rs.getString("nombre"));
                producto.put("gananciaTotal", rs.getDouble("ganancia_total"));
            } else {
                producto.put("nombre", "N/A");
                producto.put("gananciaTotal", 0.0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return producto;
    }

    // 🏷️ CATEGORÍA MÁS RENTABLE
    public static Map<String, Object> obtenerCategoriaMasRentable(int dias) {
        Map<String, Object> categoria = new HashMap<>();

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_id,
                    MAX(COALESCE(v.costo, 0)) AS costo,
                    MAX(v.categoria) AS categoria
                FROM vInventario_variantes v
                WHERE v.active = true
                GROUP BY v.producto_id
            )
            SELECT
                pc.categoria,
                SUM(vi.qty * (vi.precioUnit - COALESCE(pc.costo, 0))) AS ganancia_total
            FROM ventaItem vi
            JOIN venta v2 ON v2.id = vi.ventaId
            JOIN producto p ON p.id = vi.productoId
            LEFT JOIN productos_costo pc ON pc.producto_id = p.id
            WHERE v2.fecha >= NOW() - INTERVAL '%d days'
            GROUP BY pc.categoria
            ORDER BY ganancia_total DESC
            LIMIT 1
            """.formatted(dias);

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                categoria.put("nombre", rs.getString("categoria"));
                categoria.put("gananciaTotal", rs.getDouble("ganancia_total"));
            } else {
                categoria.put("nombre", "N/A");
                categoria.put("gananciaTotal", 0.0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categoria;
    }

    // 📋 ANÁLISIS DETALLADO POR PRODUCTO
    public static List<ProductoRentabilidad> obtenerAnalisisProductos(int dias, Integer categoriaId) {
        List<ProductoRentabilidad> productos = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            WITH productos_info AS (
                SELECT
                    v.producto_id,
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) AS costo,
                    MAX(v.categoria) AS categoria
                FROM vInventario_variantes v
                WHERE v.active = true
        """);

        if (categoriaId != null) {
            sql.append(" AND v.categoriaid = ? ");
        }

        sql.append("""
        GROUP BY v.producto_id, v.producto_nombre
            )
            SELECT
                pi.producto_nombre AS nombre,
                pi.categoria,
                AVG(vi.precioUnit) AS precio_venta,
                COALESCE(MAX(pi.costo), 0) AS costo,
                SUM(vi.qty) AS cantidad_vendida
            FROM ventaItem vi
            JOIN venta v2 ON v2.id = vi.ventaId
            JOIN producto p ON p.id = vi.productoId
            LEFT JOIN productos_info pi ON pi.producto_id = p.id
            WHERE v2.fecha >= NOW() - INTERVAL '%d days'
            GROUP BY pi.producto_nombre, pi.categoria
            ORDER BY (AVG(vi.precioUnit) - COALESCE(MAX(pi.costo), 0)) * SUM(vi.qty) DESC
            """.formatted(dias));

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if (categoriaId != null) {
                stmt.setInt(1, categoriaId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(new ProductoRentabilidad(
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getDouble("precio_venta"),
                            rs.getDouble("costo"),
                            rs.getInt("cantidad_vendida")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return productos;
    }
}
