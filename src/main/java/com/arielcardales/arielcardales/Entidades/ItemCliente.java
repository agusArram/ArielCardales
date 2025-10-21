package com.arielcardales.arielcardales.Entidades;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wrapper para TreeTableView de Clientes
 * Puede ser un Cliente (padre) o una Venta (hijo)
 */
public class ItemCliente {

    // Flag para distinguir entre cliente y venta
    private final BooleanProperty esVenta = new SimpleBooleanProperty(false);

    // ========================================
    // Datos de Cliente (cuando es padre)
    // ========================================
    private final LongProperty clienteId = new SimpleLongProperty();
    private final StringProperty nombre = new SimpleStringProperty();
    private final StringProperty dni = new SimpleStringProperty();
    private final StringProperty telefono = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty notas = new SimpleStringProperty();

    // ========================================
    // Datos de Venta (cuando es hijo)
    // ========================================
    private final LongProperty ventaId = new SimpleLongProperty();
    private final ObjectProperty<LocalDateTime> fecha = new SimpleObjectProperty<>();
    private final StringProperty medioPago = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> total = new SimpleObjectProperty<>();

    // ========================================
    // Constructor
    // ========================================
    public ItemCliente() {
        // Constructor vacío
    }

    // ========================================
    // Factory methods
    // ========================================

    /**
     * Crea un ItemCliente a partir de un Cliente
     */
    public static ItemCliente fromCliente(Cliente cliente) {
        ItemCliente item = new ItemCliente();
        item.setEsVenta(false);
        item.setClienteId(cliente.getId());
        item.setNombre(cliente.getNombre());
        item.setDni(cliente.getDni());
        item.setTelefono(cliente.getTelefono());
        item.setEmail(cliente.getEmail());
        item.setNotas(cliente.getNotas());
        return item;
    }

    /**
     * Crea un ItemCliente a partir de una Venta
     */
    public static ItemCliente fromVenta(Venta venta) {
        ItemCliente item = new ItemCliente();
        item.setEsVenta(true);
        item.setVentaId(venta.getId());
        item.setFecha(venta.getFecha());
        item.setMedioPago(venta.getMedioPago());
        item.setTotal(venta.getTotal());
        return item;
    }

    // ========================================
    // Getters, Setters y Properties
    // ========================================

    // --- esVenta
    public boolean isEsVenta() {
        return esVenta.get();
    }

    public void setEsVenta(boolean value) {
        esVenta.set(value);
    }

    public BooleanProperty esVentaProperty() {
        return esVenta;
    }

    // --- clienteId
    public Long getClienteId() {
        return clienteId.get();
    }

    public void setClienteId(Long value) {
        clienteId.set(value);
    }

    public LongProperty clienteIdProperty() {
        return clienteId;
    }

    // --- nombre
    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String value) {
        nombre.set(value);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    // --- dni
    public String getDni() {
        return dni.get();
    }

    public void setDni(String value) {
        dni.set(value);
    }

    public StringProperty dniProperty() {
        return dni;
    }

    // --- telefono
    public String getTelefono() {
        return telefono.get();
    }

    public void setTelefono(String value) {
        telefono.set(value);
    }

    public StringProperty telefonoProperty() {
        return telefono;
    }

    // --- email
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String value) {
        email.set(value);
    }

    public StringProperty emailProperty() {
        return email;
    }

    // --- notas
    public String getNotas() {
        return notas.get();
    }

    public void setNotas(String value) {
        notas.set(value);
    }

    public StringProperty notasProperty() {
        return notas;
    }

    // --- ventaId
    public Long getVentaId() {
        return ventaId.get();
    }

    public void setVentaId(Long value) {
        ventaId.set(value);
    }

    public LongProperty ventaIdProperty() {
        return ventaId;
    }

    // --- fecha
    public LocalDateTime getFecha() {
        return fecha.get();
    }

    public void setFecha(LocalDateTime value) {
        fecha.set(value);
    }

    public ObjectProperty<LocalDateTime> fechaProperty() {
        return fecha;
    }

    // --- medioPago
    public String getMedioPago() {
        return medioPago.get();
    }

    public void setMedioPago(String value) {
        medioPago.set(value);
    }

    public StringProperty medioPagoProperty() {
        return medioPago;
    }

    // --- total
    public BigDecimal getTotal() {
        return total.get();
    }

    public void setTotal(BigDecimal value) {
        total.set(value);
    }

    public ObjectProperty<BigDecimal> totalProperty() {
        return total;
    }
}
