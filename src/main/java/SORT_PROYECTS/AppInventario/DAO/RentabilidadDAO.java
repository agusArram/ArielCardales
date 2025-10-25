package SORT_PROYECTS.AppInventario.DAO;

import SORT_PROYECTS.AppInventario.session.SessionManager;

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

        // SQL CORREGIDO:
        // Se calcula el costo por CADA item vendido (uniendo a producto_variante o producto)
        // en lugar de promediar el costo del producto padre.
        String sql = """
            WITH ventas_con_costo AS (
                SELECT
                    vi.precioUnit AS precio_venta,
                    COALESCE(
                        CASE
                            WHEN vi.variante_id IS NOT NULL THEN pv.costo
                            ELSE p.costo
                        END,
                        0
                    ) AS costo,
                    vi.qty AS cantidad_vendida
                FROM ventaItem vi
                JOIN venta v2 ON v2.id = vi.ventaId
                JOIN producto p ON p.id = vi.productoId
                LEFT JOIN producto_variante pv ON pv.id = vi.variante_id AND pv.producto_id = p.id
                WHERE v2.fecha >= NOW() - INTERVAL '%d days'
                  AND v2.cliente_id = ?
                  AND p.cliente_id = ?
                  -- Asegurarse que la variante (si existe) también pertenezca al cliente
                  AND (pv.cliente_id = ? OR pv.cliente_id IS NULL)
            )
            SELECT
                COALESCE(AVG(CASE WHEN precio_venta > 0 AND (precio_venta - costo) != 0 THEN ((precio_venta - costo) / precio_venta) * 100 END), 0) AS margen_promedio,
                COALESCE(SUM((precio_venta - costo) * cantidad_vendida), 0) AS ganancia_total,
                COALESCE(SUM(precio_venta * cantidad_vendida), 0) AS ventas_totales,
                COALESCE(SUM(costo * cantidad_vendida), 0) AS costos_totales
            FROM ventas_con_costo
            """.formatted(dias);


        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // CORRECCIÓN: Se necesitan 3 parámetros de clienteId
            stmt.setString(1, clienteId); // v2.cliente_id
            stmt.setString(2, clienteId); // p.cliente_id
            stmt.setString(3, clienteId); // pv.cliente_id

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
            // Mantener valores por defecto en caso de error
            metricas.put("margenPromedio", 0.0);
            metricas.put("gananciaTotal", 0.0);
            metricas.put("ventasTotales", 0.0);
            metricas.put("costosTotales", 0.0);
        }

        return metricas;
    }
    // 💰 PRODUCTO MÁS RENTABLE
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

        // SQL CORREGIDO:
        // Se elimina el CTE y se une directamente para obtener el costo real
        // de la variante (pv.costo) o del producto (p.costo).
        String sql = """
            SELECT
                p.nombre AS nombre,
                SUM(vi.qty * (vi.precioUnit - COALESCE(
                    CASE
                        WHEN vi.variante_id IS NOT NULL THEN pv.costo
                        ELSE p.costo
                    END,
                    0
                ))) AS ganancia_total
            FROM ventaItem vi
            JOIN venta v2 ON v2.id = vi.ventaId
            JOIN producto p ON p.id = vi.productoId
            LEFT JOIN producto_variante pv ON pv.id = vi.variante_id AND pv.producto_id = p.id
            WHERE v2.fecha >= NOW() - INTERVAL '%d days'
              AND v2.cliente_id = ?
              AND p.cliente_id = ?
              AND (pv.cliente_id = ? OR pv.cliente_id IS NULL)
            GROUP BY p.nombre
            ORDER BY ganancia_total DESC
            LIMIT 1
            """.formatted(dias);


        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // CORRECCIÓN: Se necesitan 3 parámetros de clienteId
            stmt.setString(1, clienteId); // v2.cliente_id
            stmt.setString(2, clienteId); // p.cliente_id
            stmt.setString(3, clienteId); // pv.cliente_id

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
            producto.put("nombre", "Error");
            producto.put("gananciaTotal", 0.0);
        }

        return producto;
    }

    // 🏷️ CATEGORÍA MÁS RENTABLE
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

        // SQL CORREGIDO:
        // Se elimina el CTE y se une con categoria, producto y producto_variante
        // para obtener la ganancia real por item.
        String sql = """
            SELECT
                COALESCE(c.nombre, 'Sin categoría') as categoria,
                SUM(vi.qty * (vi.precioUnit - COALESCE(
                    CASE
                        WHEN vi.variante_id IS NOT NULL THEN pv.costo
                        ELSE p.costo
                    END,
                    0
                ))) AS ganancia_total
            FROM ventaItem vi
            JOIN venta v2 ON v2.id = vi.ventaId
            JOIN producto p ON p.id = vi.productoId
            LEFT JOIN categoria c ON c.id = p.categoriaId
            LEFT JOIN producto_variante pv ON pv.id = vi.variante_id AND pv.producto_id = p.id
            WHERE v2.fecha >= NOW() - INTERVAL '%d days'
              AND v2.cliente_id = ?
              AND p.cliente_id = ?
              AND (pv.cliente_id = ? OR pv.cliente_id IS NULL)
              AND (c.cliente_id = ? OR c.cliente_id IS NULL)
            GROUP BY c.nombre
            ORDER BY ganancia_total DESC
            LIMIT 1
            """.formatted(dias);

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // CORRECCIÓN: Se necesitan 4 parámetros de clienteId
            stmt.setString(1, clienteId); // v2.cliente_id
            stmt.setString(2, clienteId); // p.cliente_id
            stmt.setString(3, clienteId); // pv.cliente_id
            stmt.setString(4, clienteId); // c.cliente_id

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
            categoria.put("nombre", "Error");
            categoria.put("gananciaTotal", 0.0);
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
