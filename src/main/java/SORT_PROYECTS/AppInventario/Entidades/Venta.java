package SORT_PROYECTS.AppInventario.Entidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Venta {
    private Long id;
    private Long clienteId;  // ID del cliente vinculado (puede ser null)
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

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

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

    // ✅ Getters computados para JavaFX TableView
    public String getFechaFormateada() {
        if (fecha == null) return "";
        return fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getTotalFormateado() {
        if (total == null) return "$0,00";
        return String.format("$%,.2f", total);
    }

    public String getProductosEtiquetasosEtiquetas() {
        if (items == null || items.isEmpty()) return "-";
        return items.stream()
                .map(VentaItem::getProductoEtiqueta)
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
    }



    // ========================================
    // CLASE INTERNA: VentaItem
    // ========================================
    public static class VentaItem {
        private Long id;
        private Long ventaId;
        private Long productoId;
        private Long varianteId;
        private String productoNombre;
        private String productoEtiqueta;
        private String color;           // 🔹 Nuevo: color de la variante
        private String talle;           // 🔹 Nuevo: talle de la variante
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

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getTalle() { return talle; }
        public void setTalle(String talle) { this.talle = talle; }

        /**
         * Retorna el nombre del producto con color y talle (si es variante)
         * Ejemplo: "Pantalón Jean - Azul - Talle 42"
         */
        public String getNombreCompleto() {
            if (productoNombre == null) return "";

            StringBuilder sb = new StringBuilder(productoNombre);

            // Agregar color si existe
            if (color != null && !color.trim().isEmpty()) {
                sb.append(" - -  ").append(color);
            }
            // Agregar talle si existe
            if (talle != null && !talle.trim().isEmpty()) {
                sb.append("  ").append(talle);
            }

            return sb.toString();
        }

        private void calcularSubtotal() {
            if (precioUnit != null && qty > 0) {
                this.subtotal = precioUnit.multiply(BigDecimal.valueOf(qty));
            }
        }

        @Override
        public String toString() {
            return String.format("%s x%d @ %s = %s",
                    getNombreCompleto(), qty, precioUnit, subtotal);
        }
    }

    @Override
    public String toString() {
        return String.format("Venta #%d - %s - %s - Total: %s",
                id, clienteNombre, getFechaFormateada(), getTotalFormateado());
    }

    public String getProductosEtiquetas() {
        if (items == null || items.isEmpty()) return "-";

        try {
            String result = items.stream()
                    .map(VentaItem::getProductoEtiqueta)
                    .filter(etiq -> etiq != null && !etiq.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(", "));

            return result.isEmpty() ? "-" : result;
        } catch (Exception e) {
            return "-";
        }
    }

    public String getProductosNombres() {
        if (items == null || items.isEmpty()) return "-";

        try {
            String result = items.stream()
                    .map(VentaItem::getProductoNombre)
                    .filter(nombre -> nombre != null && !nombre.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(", "));

            return result.isEmpty() ? "-" : result;
        } catch (Exception e) {
            return "-";
        }
    }

    public int getCantidadTotal() {
        return getCantidadProductos();
    }

    public int getCantidadProductos() {
        if (items == null || items.isEmpty()) return 0;
        return items.stream()
                .mapToInt(VentaItem::getQty)
                .sum();
    }
}