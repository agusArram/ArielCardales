package SORT_PROYECTS.AppInventario.Entidades;

import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * Entidad Cliente con JavaFX Properties para binding en TableView
 */
public class Cliente {
    private final LongProperty id;
    private final StringProperty nombre;
    private final StringProperty dni;
    private final StringProperty telefono;
    private final StringProperty email;
    private final StringProperty notas;
    private final ObjectProperty<LocalDateTime> createdAt;

    // Constructor vacío
    public Cliente() {
        this.id = new SimpleLongProperty();
        this.nombre = new SimpleStringProperty();
        this.dni = new SimpleStringProperty();
        this.telefono = new SimpleStringProperty();
        this.email = new SimpleStringProperty();
        this.notas = new SimpleStringProperty();
        this.createdAt = new SimpleObjectProperty<>();
    }

    // Constructor completo
    public Cliente(Long id, String nombre, String dni, String telefono,
                   String email, String notas, LocalDateTime createdAt) {
        this.id = new SimpleLongProperty(id != null ? id : 0L);
        this.nombre = new SimpleStringProperty(nombre);
        this.dni = new SimpleStringProperty(dni);
        this.telefono = new SimpleStringProperty(telefono);
        this.email = new SimpleStringProperty(email);
        this.notas = new SimpleStringProperty(notas);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    // Getters y Setters para las properties
    public Long getId() { return id.get(); }
    public void setId(Long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    public String getNombre() { return nombre.get(); }
    public void setNombre(String value) { nombre.set(value); }
    public StringProperty nombreProperty() { return nombre; }

    public String getDni() { return dni.get(); }
    public void setDni(String value) { dni.set(value); }
    public StringProperty dniProperty() { return dni; }

    public String getTelefono() { return telefono.get(); }
    public void setTelefono(String value) { telefono.set(value); }
    public StringProperty telefonoProperty() { return telefono; }

    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    public String getNotas() { return notas.get(); }
    public void setNotas(String value) { notas.set(value); }
    public StringProperty notasProperty() { return notas; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(LocalDateTime value) { createdAt.set(value); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }

    @Override
    public String toString() {
        return String.format("Cliente{id=%d, nombre='%s', dni='%s', telefono='%s', email='%s'}",
                getId(), getNombre(), getDni(), getTelefono(), getEmail());
    }
}
