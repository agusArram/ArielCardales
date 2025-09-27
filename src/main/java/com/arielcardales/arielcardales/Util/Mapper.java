package com.arielcardales.arielcardales.Util;

import com.arielcardales.arielcardales.Entidades.Categoria;
import com.arielcardales.arielcardales.Entidades.Producto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Mapper {
    // Vista vInventario
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
