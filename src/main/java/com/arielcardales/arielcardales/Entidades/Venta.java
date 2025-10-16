package com.arielcardales.arielcardales.Entidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venta {
    private Long id;
    private String clienteNombre;
    private LocalDateTime fecha;
    private String medioPago;
    private BigDecimal total;
    private List<VentaItem> items;

    public Venta() {
        this.items = new ArrayList<>();
        this.total = BigDecimal.ZERO;
    }

    public Venta(Long id, String clienteNombre, LocalDateTime fecha, String medioPago, BigDecimal total) {
        this.id = id;
        this.clienteNombre = clienteNombre;
        this.fecha = fecha;
        this.medioPago = medioPago;
        this.total = total;
        this.items = new ArrayList<>();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<VentaItem> getItems() { return items; }
    public void setItems(List<VentaItem> items) { this.items = items; }

    public void addItem(VentaItem item) {
        this.items.add(item);
    }

    public void calcularTotal() {
        this.total = items.stream()
                .map(VentaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ✅ NUEVO: Getters computados para JavaFX TableView
    public String getFechaFormateada() {
        if (fecha == null) return "";
        return fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public int getCantidadProductos() {
        return items.stream()
                .mapToInt(VentaItem::getQty)
                .sum();
    }

    // ========================================
    // CLASE INTERNA: VentaItem
    // ========================================
    public static class VentaItem {
        private Long id;
        private Long ventaId;
        private Long productoId;
        private Long varianteId;  // ✅ NUEVO - ID de la variante si aplica
        private String productoNombre;
        private String productoEtiqueta;
        private int qty;
        private BigDecimal precioUnit;
        private BigDecimal subtotal;

        public VentaItem() {}

        public VentaItem(Long id, Long ventaId, Long productoId, String productoNombre,
                         String productoEtiqueta, int qty, BigDecimal precioUnit, BigDecimal subtotal) {
            this.id = id;
            this.ventaId = ventaId;
            this.productoId = productoId;
            this.productoNombre = productoNombre;
            this.productoEtiqueta = productoEtiqueta;
            this.qty = qty;
            this.precioUnit = precioUnit;
            this.subtotal = subtotal;
        }

        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getVentaId() { return ventaId; }
        public void setVentaId(Long ventaId) { this.ventaId = ventaId; }

        public Long getProductoId() { return productoId; }
        public void setProductoId(Long productoId) { this.productoId = productoId; }

        // ✅ NUEVO: Getter y Setter para varianteId
        public Long getVarianteId() { return varianteId; }
        public void setVarianteId(Long varianteId) { this.varianteId = varianteId; }

        public String getProductoNombre() { return productoNombre; }
        public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

        public String getProductoEtiqueta() { return productoEtiqueta; }
        public void setProductoEtiqueta(String productoEtiqueta) { this.productoEtiqueta = productoEtiqueta; }

        public int getQty() { return qty; }
        public void setQty(int qty) {
            this.qty = qty;
            calcularSubtotal();
        }

        public BigDecimal getPrecioUnit() { return precioUnit; }
        public void setPrecioUnit(BigDecimal precioUnit) {
            this.precioUnit = precioUnit;
            calcularSubtotal();
        }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

        private void calcularSubtotal() {
            if (precioUnit != null && qty > 0) {
                this.subtotal = precioUnit.multiply(BigDecimal.valueOf(qty));
            }
        }

        @Override
        public String toString() {
            return String.format("%s x%d @ %s = %s",
                    productoNombre, qty, precioUnit, subtotal);
        }
    }

    public String getProductosEtiquetas() {
        if (items == null || items.isEmpty()) return "";
        return items.stream()
                .map(VentaItem::getProductoEtiqueta)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public String getProductosNombres() {
        if (items == null || items.isEmpty()) return "";
        return items.stream()
                .map(VentaItem::getProductoNombre)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

}