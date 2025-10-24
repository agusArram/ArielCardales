package SORT_PROYECTS.AppInventario.Entidades;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una fila de inventario (producto base o variante) para TreeTableView.
 * Se usa junto con la vista SQL vInventario_variantes.
 */
public class ItemInventario {

    // Identificadores
    private final LongProperty productoId = new SimpleLongProperty();
    private final ObjectProperty<Long> varianteId = new SimpleObjectProperty<>(null);

    // Datos del producto
    private final StringProperty etiquetaProducto = new SimpleStringProperty();
    private final StringProperty nombreProducto = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty unidad = new SimpleStringProperty();

    // Datos específicos de la variante (si aplica)
    private final StringProperty color = new SimpleStringProperty("-");
    private final StringProperty talle = new SimpleStringProperty("-");

    // Datos cuantitativos
    private final ObjectProperty<BigDecimal> precio = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> costo  = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty stockOnHand = new SimpleIntegerProperty(0);

    // Estado
    private final BooleanProperty active = new SimpleBooleanProperty(true);
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();

    // Flag para saber si es variante o base
    private final BooleanProperty esVariante = new SimpleBooleanProperty(false);

    // ======================
    // Getters y Setters
    // ======================

    // --- productoId
    public long getProductoId() { return productoId.get(); }
    public void setProductoId(long id) { this.productoId.set(id); }
    public LongProperty productoIdProperty() { return productoId; }

    // --- varianteId
    public Long getVarianteId() { return varianteId.get(); }
    public void setVarianteId(Long id) { this.varianteId.set(id); }
    public ObjectProperty<Long> varianteIdProperty() { return varianteId; }

    // --- etiquetaProducto
    public String getEtiquetaProducto() { return etiquetaProducto.get(); }
    public void setEtiquetaProducto(String e) { this.etiquetaProducto.set(e); }
    public StringProperty etiquetaProductoProperty() { return etiquetaProducto; }

    // --- nombreProducto
    public String getNombreProducto() { return nombreProducto.get(); }
    public void setNombreProducto(String n) { this.nombreProducto.set(n); }
    public StringProperty nombreProductoProperty() { return nombreProducto; }

    // --- descripcion
    public String getDescripcion() { return descripcion.get(); }
    public void setDescripcion(String d) { this.descripcion.set(d); }
    public StringProperty descripcionProperty() { return descripcion; }

    // --- categoria
    public String getCategoria() { return categoria.get(); }
    public void setCategoria(String c) { this.categoria.set(c); }
    public StringProperty categoriaProperty() { return categoria; }

    // --- unidad
    public String getUnidad() { return unidad.get(); }
    public void setUnidad(String u) { this.unidad.set(u); }
    public StringProperty unidadProperty() { return unidad; }

    // --- color
    public String getColor() { return color.get(); }
    public void setColor(String c) { this.color.set(c); }
    public StringProperty colorProperty() { return color; }

    // --- talle
    public String getTalle() { return talle.get(); }
    public void setTalle(String t) { this.talle.set(t); }
    public StringProperty talleProperty() { return talle; }

    // --- precio
    public BigDecimal getPrecio() { return precio.get(); }
    public void setPrecio(BigDecimal p) { this.precio.set(p); }
    public ObjectProperty<BigDecimal> precioProperty() { return precio; }

    // --- costo
    public BigDecimal getCosto() { return costo.get(); }
    public void setCosto(BigDecimal c) { this.costo.set(c); }
    public ObjectProperty<BigDecimal> costoProperty() { return costo; }

    // --- stockOnHand
    public int getStockOnHand() { return stockOnHand.get(); }
    public void setStockOnHand(int s) { this.stockOnHand.set(s); }
    public IntegerProperty stockOnHandProperty() { return stockOnHand; }

    // --- active
    public boolean isActive() { return active.get(); }
    public void setActive(boolean a) { this.active.set(a); }
    public BooleanProperty activeProperty() { return active; }

    // --- updatedAt
    public LocalDateTime getUpdatedAt() { return updatedAt.get(); }
    public void setUpdatedAt(LocalDateTime u) { this.updatedAt.set(u); }
    public ObjectProperty<LocalDateTime> updatedAtProperty() { return updatedAt; }

    // --- esVariante
    public boolean isEsVariante() { return esVariante.get(); }
    public void setEsVariante(boolean v) { this.esVariante.set(v); }
    public BooleanProperty esVarianteProperty() { return esVariante; }

    // ======================
    // Utilidad
    // ======================

    @Override
    public String toString() {
        return (isEsVariante()
                ? "Variante de " + nombreProducto.get() + " (" + color.get() + " / " + talle.get() + ")"
                : "Producto base: " + nombreProducto.get());
    }
}
