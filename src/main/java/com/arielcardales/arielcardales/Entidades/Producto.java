package com.arielcardales.arielcardales.Entidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Producto {
    private long id;
    private String etiqueta;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal costo;
    private int stockOnHand;
    private boolean active;
    private LocalDateTime updatedAt;

    // Claves foráneas
    private long categoriaId;
    private long unidadId;

    // Campos adicionales solo para mostrar en la tabla (JOINs)
    private String categoria;  // nombre legible de categoría
    private String unidad;     // abreviatura/nombre de unidad

    // getters y setters
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

    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }

    public int getStockOnHand() { return stockOnHand; }
    public void setStockOnHand(int stockOnHand) { this.stockOnHand = stockOnHand; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(long categoriaId) { this.categoriaId = categoriaId; }

    public long getUnidadId() { return unidadId; }
    public void setUnidadId(long unidadId) { this.unidadId = unidadId; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    // ===== Helpers =====
    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", etiqueta='" + etiqueta + '\'' +
                ", nombre='" + nombre + '\'' +
                ", categoria='" + categoria + '\'' +
                ", precio=" + precio +
                ", stockOnHand=" + stockOnHand +
                '}';
    }
}
