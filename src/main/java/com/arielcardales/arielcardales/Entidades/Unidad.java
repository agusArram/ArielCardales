package com.arielcardales.arielcardales.Entidades;

import java.time.LocalDateTime;

public class Unidad {
    private long id;
    private String nombre;
    private String abreviatura;
    private LocalDateTime createdAt;

    // Getters y setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return abreviatura; // útil para ComboBox
    }
}
