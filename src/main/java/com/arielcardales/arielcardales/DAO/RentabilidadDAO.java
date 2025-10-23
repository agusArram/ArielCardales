package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.session.SessionManager;

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

        // Obtener cliente_id de la sesión actual
        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando métricas vacías");
            metricas.put("margenPromedio", 0.0);
            metricas.put("gananciaTotal", 0.0);
            metricas.put("ventasTotales", 0.0);
            metricas.put("costosTotales", 0.0);
            return metricas;
        }

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_id,
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) AS costo
                FROM vInventario_variantes v
                WHERE v.active = true
                  AND v.cliente_id = ?
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
                  AND v2.cliente_id = ?
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
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clienteId);
            stmt.setString(2, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    metricas.put("margenPromedio", rs.getDouble("margen_promedio"));
                    metricas.put("gananciaTotal", rs.getDouble("ganancia_total"));
                    metricas.put("ventasTotales", rs.getDouble("ventas_totales"));
                    metricas.put("costosTotales", rs.getDouble("costos_totales"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return metricas;
    }

    // 💰 PRODUCTO MÁS RENTABLE
    public static Map<String, Object> obtenerProductoMasRentable(int dias) {
        Map<String, Object> producto = new HashMap<>();

        // Obtener cliente_id de la sesión actual
        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando producto vacío");
            producto.put("nombre", "N/A");
            producto.put("gananciaTotal", 0.0);
            return producto;
        }

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_id,
                    v.producto_nombre,
                    MAX(COALESCE(v.costo, 0)) AS costo
                FROM vInventario_variantes v
                WHERE v.active = true
                  AND v.cliente_id = ?
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
              AND v2.cliente_id = ?
            GROUP BY pc.producto_nombre
            ORDER BY ganancia_total DESC
            LIMIT 1
            """.formatted(dias);


        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clienteId);
            stmt.setString(2, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    producto.put("nombre", rs.getString("nombre"));
                    producto.put("gananciaTotal", rs.getDouble("ganancia_total"));
                } else {
                    producto.put("nombre", "N/A");
                    producto.put("gananciaTotal", 0.0);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return producto;
    }

    // 🏷️ CATEGORÍA MÁS RENTABLE
    public static Map<String, Object> obtenerCategoriaMasRentable(int dias) {
        Map<String, Object> categoria = new HashMap<>();

        // Obtener cliente_id de la sesión actual
        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando categoría vacía");
            categoria.put("nombre", "N/A");
            categoria.put("gananciaTotal", 0.0);
            return categoria;
        }

        String sql = """
            WITH productos_costo AS (
                SELECT
                    v.producto_id,
                    MAX(COALESCE(v.costo, 0)) AS costo,
                    MAX(v.categoria) AS categoria
                FROM vInventario_variantes v
                WHERE v.active = true
                  AND v.cliente_id = ?
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
              AND v2.cliente_id = ?
            GROUP BY pc.categoria
            ORDER BY ganancia_total DESC
            LIMIT 1
            """.formatted(dias);

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clienteId);
            stmt.setString(2, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    categoria.put("nombre", rs.getString("categoria"));
                    categoria.put("gananciaTotal", rs.getDouble("ganancia_total"));
                } else {
                    categoria.put("nombre", "N/A");
                    categoria.put("gananciaTotal", 0.0);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categoria;
    }

    // 📋 ANÁLISIS DETALLADO POR PRODUCTO
    public static List<ProductoRentabilidad> obtenerAnalisisProductos(int dias, Integer categoriaId) {
        List<ProductoRentabilidad> productos = new ArrayList<>();

        // Obtener cliente_id de la sesión actual
        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando lista vacía");
            return productos;
        }

        // Consulta SIMPLIFICADA: empezar desde VENTAS, no desde inventario
        // Solo muestra productos/variantes que realmente se vendieron
        StringBuilder sql = new StringBuilder("""
            SELECT
                CASE
                    WHEN vi.variante_id IS NOT NULL THEN
                        CONCAT(p.nombre, ' - ', pv.color, '/', pv.talle)
                    ELSE
                        p.nombre
                END AS nombre,
                c.nombre AS categoria,
                AVG(vi.precioUnit) AS precio_venta,
                COALESCE(
                    CASE
                        WHEN vi.variante_id IS NOT NULL THEN MAX(pv.costo)
                        ELSE MAX(p.costo)
                    END,
                    0
                ) AS costo,
                SUM(vi.qty) AS cantidad_vendida
            FROM ventaItem vi
            JOIN venta v ON v.id = vi.ventaId
            JOIN producto p ON p.id = vi.productoId
            JOIN categoria c ON c.id = p.categoriaId
            LEFT JOIN producto_variante pv ON pv.id = vi.variante_id
            WHERE v.fecha >= NOW() - INTERVAL '%d days'
              AND v.cliente_id = ?
              AND p.cliente_id = ?
        """.formatted(dias));

        if (categoriaId != null) {
            sql.append(" AND p.categoriaId = ? ");
        }

        sql.append("""
            GROUP BY
                CASE
                    WHEN vi.variante_id IS NOT NULL THEN
                        CONCAT(p.nombre, ' - ', pv.color, '/', pv.talle)
                    ELSE
                        p.nombre
                END,
                c.nombre,
                vi.variante_id
            ORDER BY (AVG(vi.precioUnit) - COALESCE(
                CASE
                    WHEN vi.variante_id IS NOT NULL THEN MAX(pv.costo)
                    ELSE MAX(p.costo)
                END,
                0
            )) * SUM(vi.qty) DESC
            """);

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, clienteId);  // cliente_id en venta
            stmt.setString(paramIndex++, clienteId);  // cliente_id en producto

            if (categoriaId != null) {
                stmt.setInt(paramIndex++, categoriaId);  // categoriaId opcional
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
