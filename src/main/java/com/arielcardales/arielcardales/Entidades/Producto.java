package com.arielcardales.arielcardales.Entidades;

import java.math.BigDecimal;

public class Producto {
    private int idProducto;
    private String nombre;
    private BigDecimal precio;
    private int stock;

    public Producto(int idProducto, String nombre, BigDecimal precio, int stock) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public int getStock() { return stock; }

    public void setStock(int stock) { this.stock = stock; }
}

