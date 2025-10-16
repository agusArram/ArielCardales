package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.Categoria;
import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Entidades.ProductoVariante;
import com.arielcardales.arielcardales.Entidades.Venta;
import com.arielcardales.arielcardales.Entidades.Venta.VentaItem;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Mapper {

    // ========================================
    // INVENTARIO
    // ========================================

    public static Producto getProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setEtiqueta(rs.getString("etiqueta"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoria(rs.getString("categoria"));
        p.setUnidad(rs.getString("unidad"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setStockOnHand(rs.getInt("stockOnHand"));
        return p;
    }

    public static ItemInventario getItemInventarioBase(ResultSet rs) throws SQLException {
        ItemInventario it = new ItemInventario();
        it.productoIdProperty().set(rs.getLong("producto_id"));
        it.varianteIdProperty().set(null);
        it.etiquetaProductoProperty().set(rs.getString("producto_etiqueta"));
        it.nombreProductoProperty().set(rs.getString("producto_nombre"));
        it.categoriaProperty().set(rs.getString("categoria"));
        it.unidadProperty().set(rs.getString("unidad"));
        it.colorProperty().set(rs.getString("color"));
        it.talleProperty().set(rs.getString("talle"));
        it.precioProperty().set(rs.getBigDecimal("precio"));
        it.costoProperty().set(rs.getBigDecimal("costo"));
        it.stockOnHandProperty().set(rs.getInt("stockOnHand"));
        it.activeProperty().set(rs.getBoolean("active"));
        it.updatedAtProperty().set(rs.getTimestamp("updatedAt").toLocalDateTime());
        it.setEsVariante(false);
        return it;
    }

    public static ItemInventario getItemInventarioVariante(ResultSet rs) throws SQLException {
        ItemInventario it = getItemInventarioBase(rs);
        it.varianteIdProperty().set(rs.getLong("variante_id"));
        it.setEsVariante(true);
        return it;
    }

    public static Producto getProductoBasico(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setEtiqueta(rs.getString("etiqueta"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoriaId(rs.getLong("categoriaId"));
        p.setUnidadId(rs.getLong("unidadId"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setStockOnHand(rs.getInt("stockOnHand"));
        return p;
    }

    // ========================================
    // VARIANTES
    // ========================================

    /**
     * Mapea una ProductoVariante desde ResultSet
     * Espera columnas: id, producto_id, color, talle, precio, costo, stock,
     *                  etiqueta, active, createdAt, updatedAt
     */
    public static ProductoVariante getProductoVariante(ResultSet rs) throws SQLException {
        ProductoVariante v = new ProductoVariante();
        v.setId(rs.getLong("id"));
        v.setProductoId(rs.getLong("producto_id"));
        v.setColor(rs.getString("color"));
        v.setTalle(rs.getString("talle"));
        v.setPrecio(rs.getBigDecimal("precio"));
        v.setCosto(rs.getBigDecimal("costo"));
        v.setStock(rs.getInt("stock"));
        v.setEtiqueta(rs.getString("etiqueta"));
        v.setActive(rs.getBoolean("active"));
        v.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        v.setUpdatedAt(rs.getTimestamp("updatedAt").toLocalDateTime());
        return v;
    }

    // ========================================
    // CATEGORÍAS
    // ========================================

    public static Categoria getCategoria(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getLong("id"));
        c.setNombre(rs.getString("nombre"));
        long parent = rs.getLong("parentId");
        c.setParentId(rs.wasNull() ? null : parent);
        return c;
    }

    public static Categoria getCategoriaConPadre(ResultSet rs) throws SQLException {
        return getCategoria(rs);
    }

    // ========================================
    // VENTAS
    // ========================================

    /**
     * Mapea una venta desde ResultSet
     */
    public static Venta getVenta(ResultSet rs) throws SQLException {
        return new Venta(
                rs.getLong("id"),
                rs.getString("clienteNombre"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getString("medioPago"),
                rs.getBigDecimal("total")
        );
    }

    /**
     * Mapea un item de venta desde ResultSet
     */
    public static VentaItem getVentaItem(ResultSet rs) throws SQLException {
        return new VentaItem(
                rs.getLong("id"),
                rs.getLong("ventaId"),
                rs.getLong("productoId"),
                rs.getString("productoNombre"),
                rs.getString("productoEtiqueta"),
                rs.getInt("qty"),
                rs.getBigDecimal("precioUnit"),
                rs.getBigDecimal("subtotal")
        );
    }
}