package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.Categoria;
import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Entidades.Producto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Mapper {
// Vista vInventario
    public static Producto getProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setEtiqueta(rs.getString("etiqueta"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoria(rs.getString("categoria")); // string legible
        p.setUnidad(rs.getString("unidad"));       // string legible
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
        it.colorProperty().set(rs.getString("color"));  // vendrá "-" en la vista
        it.talleProperty().set(rs.getString("talle"));  // vendrá "-" en la vista
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
        it.varianteIdProperty().set(rs.getLong("variante_id")); // no null aquí
        it.setEsVariante(true);
        return it;
    }

    // Tabla producto (para ABM)
    public static Producto getProductoBasico(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setEtiqueta(rs.getString("etiqueta"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoriaId(rs.getLong("categoriaId"));
        p.setUnidadId(rs.getLong("unidadId"));
        p.setPrecio(rs.getBigDecimal("precio"));
        //p.setCosto(rs.getBigDecimal("costo"));
        p.setStockOnHand(rs.getInt("stockOnHand"));
        //p.setActive(rs.getBoolean("active"));
        //p.setUpdatedAt(rs.getTimestamp("updatedAt").toInstant());
        return p;
    }

    // Categoría
    public static Categoria getCategoria(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getLong("id"));
        c.setNombre(rs.getString("nombre"));
        long parent = rs.getLong("parentId");
        c.setParentId(rs.wasNull() ? null : parent);
        c.setCreatedAt(LocalDateTime.from(rs.getTimestamp("createdAt").toInstant()));
        return c;
    }

    public static Categoria getCategoriaConPadre(ResultSet rs) throws SQLException {
        Categoria c = getCategoria(rs);
        //c.setParentNombre(rs.getString("parentNombre")); // extra para la UI
        return c;
    }

}
