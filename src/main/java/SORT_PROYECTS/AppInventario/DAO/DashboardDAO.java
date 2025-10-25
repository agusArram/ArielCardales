package SORT_PROYECTS.AppInventario.DAO;

import SORT_PROYECTS.AppInventario.session.SessionManager;

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

        // Obtener cliente_id de la sesión actual
        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando métricas vacías");
            metricas.put("totalProductos", 0);
            metricas.put("stockBajo", 0);
            metricas.put("sinStock", 0);
            metricas.put("valorTotal", 0.0);
            return metricas;
        }

        String sql = """
            SELECT
                COUNT(DISTINCT producto_id) as totalProductos,
                COUNT(DISTINCT CASE
                    WHEN stockOnHand > 0 AND stockOnHand <= 5 THEN
                        COALESCE(variante_id, producto_id)
                END) as stockBajo,
                COUNT(DISTINCT CASE
                    WHEN stockOnHand = 0 THEN
                        COALESCE(variante_id, producto_id)
                END) as sinStock,
                COALESCE(SUM(stockOnHand * precio), 0) as valorTotal
            FROM vInventario_variantes
            WHERE active = true
              AND cliente_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    metricas.put("totalProductos", rs.getInt("totalProductos"));
                    metricas.put("stockBajo", rs.getInt("stockBajo"));
                    metricas.put("sinStock", rs.getInt("sinStock"));
                    metricas.put("valorTotal", rs.getDouble("valorTotal"));
                }
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
    /**
     * Obtiene el top N de productos más vendidos (según período en días)
     */
    public static List<ProductoVendido> obtenerTopProductosVendidos(int limit, int dias) {
        List<ProductoVendido> productos = new ArrayList<>();

        // Obtener cliente_id de la sesión actual
        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando lista vacía");
            return productos;
        }

        // --- INICIO DEL CAMBIO ---
        String sql = """
            SELECT
                vi.productoNombre as nombre,
                '' as etiqueta,
                SUM(vi.qty) as cantidad_vendida,
                SUM(vi.qty * vi.precioUnit) as total_ventas
            FROM ventaItem vi
            JOIN venta v ON v.id = vi.ventaId
            WHERE v.fecha >= NOW() - (INTERVAL '1 day' * ?) -- CAMBIO CLAVE
              AND v.cliente_id = ?
            GROUP BY vi.productoNombre
            ORDER BY cantidad_vendida DESC
            LIMIT ?
            """; // Se eliminó .formatted(dias)

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Se añaden los parámetros en orden
            stmt.setInt(1, dias);       // Parámetro para el INTERVAL
            stmt.setString(2, clienteId); // Parámetro para v.cliente_id
            stmt.setInt(3, limit);      // Parámetro para LIMIT

            try (ResultSet rs = stmt.executeQuery()) {
                // --- FIN DEL CAMBIO ---
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
    public static Map<String, Object> obtenerMetricasVentas(int dias) {
        Map<String, Object> metricas = new HashMap<>();

        // Obtener cliente_id de la sesión actual
        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando métricas vacías");
            metricas.put("totalVentas", 0);
            metricas.put("montoTotal", 0.0);
            metricas.put("ventasHoy", 0.0);
            metricas.put("promedioVentaDiaria", 0.0);
            return metricas;
        }

        // --- INICIO DEL CAMBIO ---
        // 1. Se elimina .formatted()
        // 2. Se reemplaza '%d days' por '1 day' * ?
        //    (Esta es la forma segura de pasar un intervalo como parámetro en SQL)
        String sql = """
            SELECT
                COUNT(*) as totalVentas,
                SUM(total) as montoTotal,
                SUM(CASE WHEN fecha::date = CURRENT_DATE THEN total ELSE 0 END) as ventasHoy
            FROM venta
            WHERE fecha >= NOW() - (INTERVAL '1 day' * ?) -- CAMBIO CLAVE
              AND cliente_id = ?
            """; // Se eliminó .formatted(dias)

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 3. Se añaden los parámetros en orden
            stmt.setInt(1, dias);       // Parámetro para el INTERVAL
            stmt.setString(2, clienteId); // Parámetro para el cliente_id

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalVentas = rs.getInt("totalVentas");
                    double montoTotal = rs.getDouble("montoTotal");
                    double ventasHoy = rs.getDouble("ventasHoy");

                    // Cálculo del promedio (esto ya estaba bien)
                    double promedioVentaDiaria = (totalVentas > 0 && dias > 0) ? montoTotal / (double)dias : 0;
                    metricas.put("totalVentas", totalVentas);
                    metricas.put("montoTotal", montoTotal);
                    metricas.put("ventasHoy", ventasHoy);
                    metricas.put("promedioVentaDiaria", promedioVentaDiaria);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            metricas.put("totalVentas", 0);
            metricas.put("montoTotal", 0.0);
            metricas.put("ventasHoy", 0.0);
            metricas.put("promedioVentaDiaria", 0.0);
        }
        // --- FIN DEL CAMBIO ---

        return metricas;
    }

    /**
     * Clase auxiliar para ventas mensuales
     */
    public static class VentaMensual {
        private String mes;
        private double monto;
        private int cantidad;

        public VentaMensual(String mes, double monto, int cantidad) {
            this.mes = mes;
            this.monto = monto;
            this.cantidad = cantidad;
        }

        public String getMes() { return mes; }
        public double getMonto() { return monto; }
        public int getCantidad() { return cantidad; }
    }

    /**
     * Obtiene ventas agrupadas por período (días o meses según duración)
     * @param dias Número de días a consultar (ej: 7, 15, 30, 180, 365)
     * @return Lista de ventas agrupadas
     */
    public static List<VentaMensual> obtenerVentasPorPeriodo(int dias) {
        List<VentaMensual> ventas = new ArrayList<>();

        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando lista vacía");
            return ventas;
        }

        // Si el período es <= 30 días, agrupar por día. Si no, agrupar por mes
        String sql;
        if (dias <= 30) {
            // Agrupar por día
            sql = """
                SELECT
                    TO_CHAR(fecha::date, 'DD Mon') as mes,
                    TO_CHAR(fecha::date, 'YYYY-MM-DD') as mes_orden,
                    SUM(total) as monto_total,
                    COUNT(*) as cantidad_ventas
                FROM venta
                WHERE fecha >= CURRENT_DATE - INTERVAL '%d days'
                  AND cliente_id = ?
                GROUP BY fecha::date, mes_orden
                ORDER BY mes_orden ASC
                """.formatted(dias);
        } else {
            // Agrupar por mes
            sql = """
                SELECT
                    TO_CHAR(DATE_TRUNC('month', fecha), 'Mon YYYY') as mes,
                    TO_CHAR(DATE_TRUNC('month', fecha), 'YYYY-MM') as mes_orden,
                    SUM(total) as monto_total,
                    COUNT(*) as cantidad_ventas
                FROM venta
                WHERE fecha >= CURRENT_DATE - INTERVAL '%d days'
                  AND cliente_id = ?
                GROUP BY DATE_TRUNC('month', fecha), mes_orden
                ORDER BY mes_orden ASC
                """.formatted(dias);
        }

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ventas.add(new VentaMensual(
                        rs.getString("mes"),
                        rs.getDouble("monto_total"),
                        rs.getInt("cantidad_ventas")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ventas;
    }

    /**
     * Clase auxiliar para ventas por categoría
     */
    public static class VentaCategoria {
        private String categoria;
        private double monto;
        private int cantidad;

        public VentaCategoria(String categoria, double monto, int cantidad) {
            this.categoria = categoria;
            this.monto = monto;
            this.cantidad = cantidad;
        }

        public String getCategoria() { return categoria; }
        public double getMonto() { return monto; }
        public int getCantidad() { return cantidad; }
    }

    /**
     * Obtiene ventas agrupadas por categoría
     * @param dias Número de días a consultar
     * @return Lista de ventas por categoría
     */
    public static List<VentaCategoria> obtenerVentasPorCategoria(int dias) {
        List<VentaCategoria> ventas = new ArrayList<>();

        String clienteId = SessionManager.getInstance().getClienteId();
        if (clienteId == null) {
            System.err.println("⚠️ No hay sesión activa - retornando lista vacía");
            return ventas;
        }

        String sql = """
            SELECT
                COALESCE(p.categoria, 'Sin categoría') as categoria,
                SUM(vi.qty * vi.precioUnit) as monto_total,
                SUM(vi.qty) as cantidad_vendida
            FROM ventaItem vi
            JOIN venta v ON v.id = vi.ventaId
            LEFT JOIN (
                SELECT DISTINCT producto_id, categoria
                FROM vInventario_variantes
                WHERE cliente_id = ?
            ) p ON p.producto_id = vi.productoId
            WHERE v.fecha >= CURRENT_DATE - INTERVAL '%d days'
              AND v.cliente_id = ?
            GROUP BY categoria
            ORDER BY monto_total DESC
            """.formatted(dias);

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clienteId);
            stmt.setString(2, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ventas.add(new VentaCategoria(
                        rs.getString("categoria"),
                        rs.getDouble("monto_total"),
                        rs.getInt("cantidad_vendida")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ventas;
    }
}
