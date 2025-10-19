package com.arielcardales.arielcardales.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RentabilidadDAO {

    /**
     * Clase que representa el análisis de rentabilidad de un producto
     */
    public static class ProductoRentabilidad {
        private String nombre;
        private String categoria;
        private double precioVenta;
        private double costo;
        private double margenPorcentaje;
        private double gananciaUnitaria;
        private int cantidadVendida;
        private double gananciaTotal;

        public ProductoRentabilidad(String nombre, String categoria, double precioVenta, double costo,
                                   int cantidadVendida) {
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

    /**
     * Obtiene métricas generales de rentabilidad para un período
     * @param dias Número de días hacia atrás (7, 30, 90, etc.)
     * @return Map con métricas de rentabilidad
     */
    public static Map<String, Object> obtenerMetricasRentabilidad(int dias) {
        Map<String, Object> metricas = new HashMap<>();

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) as costo
                FROM vInventario_variantes v
                WHERE v.active = true
                GROUP BY v.producto_nombre
            ),
            ventas_periodo AS (
                SELECT
                    vi.productoNombre,
                    AVG(vi.precioUnit) as precio_venta,
                    COALESCE(MAX(pc.costo), 0) as costo,
                    SUM(vi.qty) as cantidad_vendida
                FROM ventaItem vi
                JOIN venta v ON v.id = vi.ventaId
                LEFT JOIN productos_costo pc ON pc.producto_nombre = vi.productoNombre
                WHERE v.fecha >= NOW() - INTERVAL '%d days'
                GROUP BY vi.productoNombre
            )
            SELECT
                COALESCE(AVG(CASE WHEN precio_venta > 0 THEN ((precio_venta - costo) / precio_venta) * 100 END), 0) as margen_promedio,
                COALESCE(SUM((precio_venta - costo) * cantidad_vendida), 0) as ganancia_total,
                COALESCE(SUM(precio_venta * cantidad_vendida), 0) as ventas_totales,
                COALESCE(SUM(costo * cantidad_vendida), 0) as costos_totales
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
            metricas.put("margenPromedio", 0.0);
            metricas.put("gananciaTotal", 0.0);
            metricas.put("ventasTotales", 0.0);
            metricas.put("costosTotales", 0.0);
        }

        return metricas;
    }

    /**
     * Obtiene el producto más rentable del período
     * @param dias Número de días hacia atrás
     * @return Map con datos del producto más rentable
     */
    public static Map<String, Object> obtenerProductoMasRentable(int dias) {
        Map<String, Object> producto = new HashMap<>();

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) as costo
                FROM vInventario_variantes v
                WHERE v.active = true
                GROUP BY v.producto_nombre
            )
            SELECT
                vi.productoNombre as nombre,
                SUM(vi.qty * (vi.precioUnit - COALESCE(pc.costo, 0))) as ganancia_total
            FROM ventaItem vi
            JOIN venta v ON v.id = vi.ventaId
            LEFT JOIN productos_costo pc ON pc.producto_nombre = vi.productoNombre
            WHERE v.fecha >= NOW() - INTERVAL '%d days'
            GROUP BY vi.productoNombre
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
            producto.put("nombre", "N/A");
            producto.put("gananciaTotal", 0.0);
        }

        return producto;
    }

    /**
     * Obtiene la categoría más rentable del período
     * @param dias Número de días hacia atrás
     * @return Map con datos de la categoría más rentable
     */
    public static Map<String, Object> obtenerCategoriaMasRentable(int dias) {
        Map<String, Object> categoria = new HashMap<>();

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) as costo,
                    MAX(COALESCE(c.nombre, 'Sin categoría')) as categoria
                FROM vInventario_variantes v
                LEFT JOIN producto p ON p.id = v.producto_id
                LEFT JOIN categoria c ON c.id = p.categoriaid
                WHERE v.active = true
                GROUP BY v.producto_nombre
            )
            SELECT
                pc.categoria,
                SUM(vi.qty * (vi.precioUnit - COALESCE(pc.costo, 0))) as ganancia_total
            FROM ventaItem vi
            JOIN venta v ON v.id = vi.ventaId
            LEFT JOIN productos_costo pc ON pc.producto_nombre = vi.productoNombre
            WHERE v.fecha >= NOW() - INTERVAL '%d days'
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
            categoria.put("nombre", "N/A");
            categoria.put("gananciaTotal", 0.0);
        }

        return categoria;
    }

    /**
     * Obtiene análisis detallado de rentabilidad por producto
     * @param dias Número de días hacia atrás
     * @param categoriaId ID de categoría para filtrar (null para todas)
     * @return Lista de productos con análisis de rentabilidad
     */
    public static List<ProductoRentabilidad> obtenerAnalisisProductos(int dias, Integer categoriaId) {
        List<ProductoRentabilidad> productos = new ArrayList<>();

        // Primero construyo el CTE base
        StringBuilder sql = new StringBuilder("""
            WITH productos_info AS (
                SELECT
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) as costo,
                    MAX(COALESCE(c.nombre, 'Sin categoría')) as categoria
                FROM vInventario_variantes v
                LEFT JOIN producto p ON p.id = v.producto_id
                LEFT JOIN categoria c ON c.id = p.categoriaid
                WHERE v.active = true
            """);

        // Si hay filtro de categoría, lo agregamos al CTE
        if (categoriaId != null) {
            sql.append(" AND c.id = ?");
        }

        sql.append("""
                GROUP BY v.producto_nombre
            )
            SELECT
                vi.productoNombre as nombre,
                pi.categoria,
                AVG(vi.precioUnit) as precio_venta,
                COALESCE(MAX(pi.costo), 0) as costo,
                SUM(vi.qty) as cantidad_vendida
            FROM ventaItem vi
            JOIN venta v ON v.id = vi.ventaId
            LEFT JOIN productos_info pi ON pi.producto_nombre = vi.productoNombre
            WHERE v.fecha >= NOW() - INTERVAL '%d days'
            GROUP BY vi.productoNombre, pi.categoria
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
