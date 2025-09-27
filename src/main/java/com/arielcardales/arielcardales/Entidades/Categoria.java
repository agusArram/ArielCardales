package com.arielcardales.arielcardales.Entidades;

import java.time.LocalDateTime;

public class Categoria {
    private long id;
    private String nombre;
    private Long parentId; // puede ser null
    private LocalDateTime createdAt;

    // Getters y setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return nombre; // útil para ComboBox
    }
}
