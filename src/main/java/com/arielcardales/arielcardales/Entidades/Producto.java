package com.arielcardales.arielcardales.Entidades;

import java.math.BigDecimal;

public class Producto {
    private long id;
    private String etiqueta;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private int stockOnHand;
    private long categoriaId;
    private long unidadId;

    // Campos adicionales solo para mostrar en la tabla
    private String categoria;  // nombre legible de categoría
    private String unidad;     // abreviatura de unidad

    // Getters y setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getStockOnHand() { return stockOnHand; }
    public void setStockOnHand(int stockOnHand) { this.stockOnHand = stockOnHand; }

    public long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(long categoriaId) { this.categoriaId = categoriaId; }

    public long getUnidadId() { return unidadId; }
    public void setUnidadId(long unidadId) { this.unidadId = unidadId; }

    // Getters/setters para los textos legibles
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
}
