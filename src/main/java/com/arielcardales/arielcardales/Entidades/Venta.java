package com.arielcardales.arielcardales.Entidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una venta completa con sus items
 */
public class Venta {
    private Long id;
    private String clienteNombre;
    private LocalDateTime fecha;
    private String medioPago;
    private BigDecimal total;
    private List<VentaItem> items;

    // Constructor vacío
    public Venta() {
        this.items = new ArrayList<>();
        this.total = BigDecimal.ZERO;
        this.fecha = LocalDateTime.now();
    }

    // Constructor completo
    public Venta(Long id, String clienteNombre, LocalDateTime fecha,
                 String medioPago, BigDecimal total) {
        this.id = id;
        this.clienteNombre = clienteNombre;
        this.fecha = fecha;
        this.medioPago = medioPago;
        this.total = total;
        this.items = new ArrayList<>();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    /**
     * Método para usar en TableView con Tablas.crearColumnas()
     */
    public String getFechaFormateada() {
        if (fecha == null) return "";
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Devuelve etiquetas con cantidades para columna separada
     */
    public String getProductosEtiquetas() {
        if (items == null || items.isEmpty()) return "-";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            VentaItem item = items.get(i);
            if (i > 0) sb.append(", ");
            sb.append(item.getProductoEtiqueta()).append(" x").append(item.getQty());
        }
        return sb.toString();
    }

    /**
     * Devuelve nombres de productos para columna separada
     */
    public String getProductosNombres() {
        if (items == null || items.isEmpty()) return "-";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            VentaItem item = items.get(i);
            if (i > 0) sb.append(", ");
            sb.append(item.getProductoNombre());
        }
        return sb.toString();
    }

    /**
     * Devuelve resumen de productos para mostrar en tabla
     * @deprecated Usar getProductosEtiquetas() y getProductosNombres() en columnas separadas
     */
    @Deprecated
    public String getProductosDetalle() {
        if (items == null || items.isEmpty()) return "Sin items";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            VentaItem item = items.get(i);
            if (i > 0) sb.append(", ");
            sb.append(item.getProductoEtiqueta()).append(" x").append(item.getQty());
        }
        return sb.toString();
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<VentaItem> getItems() {
        return items;
    }

    public void setItems(List<VentaItem> items) {
        this.items = items;
    }

    public void addItem(VentaItem item) {
        this.items.add(item);
    }

    /**
     * Calcula el total sumando todos los items
     */
    public void calcularTotal() {
        this.total = items.stream()
                .map(VentaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtiene la cantidad total de productos vendidos
     */
    public int getCantidadProductos() {
        return items.stream()
                .mapToInt(VentaItem::getQty)
                .sum();
    }

    @Override
    public String toString() {
        return String.format("Venta #%d - %s - $%,.2f",
                id, clienteNombre != null ? clienteNombre : "Sin cliente", total);
    }

    /**
     * Clase interna para items de venta
     */
    public static class VentaItem {
        private Long id;
        private Long ventaId;
        private Long productoId;
        private String productoNombre;
        private String productoEtiqueta;
        private int qty;
        private BigDecimal precioUnit;
        private BigDecimal subtotal;

        // Constructor vacío
        public VentaItem() {}

        // Constructor completo
        public VentaItem(Long id, Long ventaId, Long productoId,
                         String productoNombre, String productoEtiqueta,
                         int qty, BigDecimal precioUnit, BigDecimal subtotal) {
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
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getVentaId() {
            return ventaId;
        }

        public void setVentaId(Long ventaId) {
            this.ventaId = ventaId;
        }

        public Long getProductoId() {
            return productoId;
        }

        public void setProductoId(Long productoId) {
            this.productoId = productoId;
        }

        public String getProductoNombre() {
            return productoNombre;
        }

        public void setProductoNombre(String productoNombre) {
            this.productoNombre = productoNombre;
        }

        public String getProductoEtiqueta() {
            return productoEtiqueta;
        }

        public void setProductoEtiqueta(String productoEtiqueta) {
            this.productoEtiqueta = productoEtiqueta;
        }

        /**
         * Método para usar en TableView con Tablas.crearColumnas()
         */
        public String getProductoCompleto() {
            return productoEtiqueta + " - " + productoNombre;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
            calcularSubtotal();
        }

        public BigDecimal getPrecioUnit() {
            return precioUnit;
        }

        public void setPrecioUnit(BigDecimal precioUnit) {
            this.precioUnit = precioUnit;
            calcularSubtotal();
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }

        private void calcularSubtotal() {
            if (precioUnit != null && qty > 0) {
                this.subtotal = precioUnit.multiply(BigDecimal.valueOf(qty));
            }
        }

        @Override
        public String toString() {
            return String.format("%s x%d - $%,.2f", productoNombre, qty, subtotal);
        }
    }
}