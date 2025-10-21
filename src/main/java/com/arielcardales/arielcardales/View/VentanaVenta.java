package com.arielcardales.arielcardales.View;

import com.arielcardales.arielcardales.DAO.*;
import com.arielcardales.arielcardales.Entidades.*;
import com.arielcardales.arielcardales.Util.AutoCompleteCliente;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Helper para mostrar ventana modal de venta.
 * Extrae toda la lógica del ProductoTreeController para mantenerlo limpio.
 */
public class VentanaVenta {

    private static final ProductoDAO productoDAO = new ProductoDAO();
    private static final ProductoVarianteDAO varianteDAO = new ProductoVarianteDAO();

    /**
     * Muestra el diálogo de venta para un producto o variante seleccionado.
     *
     * @param owner Stage padre
     * @param item ItemInventario seleccionado
     * @param onSuccess Callback para recargar UI después de venta exitosa
     */
    public static void mostrar(Stage owner, ItemInventario item, Runnable onSuccess) {
        // 🔹 PASO 1: Cargar datos desde BD
        Producto producto;
        Long idVariante = null;

        if (item.isEsVariante()) {
            Optional<ProductoVariante> varOpt = varianteDAO.findById(item.getVarianteId());
            if (varOpt.isEmpty()) {
                error("No se encontró la variante en la base de datos.");
                return;
            }

            ProductoVariante variante = varOpt.get();
            idVariante = variante.getId();

            // Obtener info del producto base
            Optional<Producto> baseOpt = productoDAO.findById(variante.getProductoId());
            if (baseOpt.isEmpty()) {
                error("No se encontró el producto base.");
                return;
            }

            producto = baseOpt.get();
            producto.setPrecio(variante.getPrecio());

        } else {
            Optional<Producto> prodOpt = productoDAO.findById(item.getProductoId());
            if (prodOpt.isEmpty()) {
                error("Producto no encontrado.");
                return;
            }
            producto = prodOpt.get();
        }

        // 🔹 PASO 2: Pedir cantidad
        pedirCantidad(owner, producto, idVariante, onSuccess);
    }

    /**
     * Muestra diálogo para ingresar cantidad
     */
    private static void pedirCantidad(Stage owner, Producto producto, Long idVariante, Runnable onSuccess) {
        // 🔹 PASO 1: Obtener stock actual desde BD
        int stockDisponible = obtenerStockActual(producto.getId(), idVariante);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva venta");
        dialog.setHeaderText("Producto: " + producto.getNombre() + "\nStock disponible: " + stockDisponible);
        dialog.setContentText("Cantidad:");

        DialogPane pane1 = dialog.getDialogPane();
        pane1.getStylesheets().add(VentanaVenta.class.getResource("/Estilos/Estilos.css").toExternalForm());
        pane1.getStyleClass().add("dialog-cuero");

        dialog.showAndWait().ifPresent(valor -> {
            try {
                int cantidad = Integer.parseInt(valor);
                if (cantidad <= 0) throw new NumberFormatException();

                // ✅ VALIDAR STOCK ANTES DE CALCULAR TOTAL
                if (cantidad > stockDisponible) {
                    error(String.format("⚠ Stock insuficiente.\nDisponible: %d | Solicitado: %d",
                            stockDisponible, cantidad));
                    return;
                }

                BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
                NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                String totalFormateado = formato.format(total);

                // 🔹 PASO 3: Confirmar venta con datos opcionales
                confirmarVenta(owner, producto, cantidad, total, totalFormateado, idVariante, onSuccess);

            } catch (NumberFormatException e) {
                error("⚠ Ingresá un número válido.");
            }
        });
    }

    /**
     * Obtiene el stock actual desde BD
     */
    private static int obtenerStockActual(Long productoId, Long idVariante) {
        try {
            if (idVariante != null) {
                // Stock de variante
                Optional<ProductoVariante> varOpt = varianteDAO.findById(idVariante);
                return varOpt.map(ProductoVariante::getStock).orElse(0);
            } else {
                // Stock de producto base
                Optional<Producto> prodOpt = productoDAO.findById(productoId);
                return prodOpt.map(Producto::getStockOnHand).orElse(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Diálogo de confirmación con nombre cliente y medio de pago
     */
    private static void confirmarVenta(Stage owner, Producto producto, int cantidad, BigDecimal total,
                                       String totalFormateado, Long idVariante, Runnable onSuccess) {

        Dialog<ButtonType> confirmDialog = new Dialog<>();
        confirmDialog.setTitle("Confirmar venta");
        confirmDialog.setHeaderText("Total: " + totalFormateado);

        // 📦 Crear contenido del diálogo
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);

        // Campo nombre cliente con autocompletado
        AutoCompleteCliente txtCliente = new AutoCompleteCliente();
        txtCliente.setPromptText("Nombre del cliente (opcional)");
        txtCliente.setPrefWidth(300);

        // Label de ayuda
        Label lblAyuda = new Label("💡 Empieza a escribir para ver sugerencias");
        lblAyuda.setStyle("-fx-text-fill: #888; -fx-font-size: 10px; -fx-font-style: italic;");

        // ComboBox medio de pago
        ComboBox<String> cmbMedioPago = new ComboBox<>();
        cmbMedioPago.getItems().addAll("Efectivo", "Débito", "Crédito", "Transferencia", "Mercado Pago");
        cmbMedioPago.setValue("Efectivo");
        cmbMedioPago.setPrefWidth(300);

        content.getChildren().addAll(
                new Label("Nombre del cliente:"),
                txtCliente,
                lblAyuda,
                new Label("Medio de pago:"),
                cmbMedioPago
        );

        confirmDialog.getDialogPane().setContent(content);
        confirmDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Aplicar estilos
        DialogPane pane2 = confirmDialog.getDialogPane();
        pane2.getStylesheets().add(VentanaVenta.class.getResource("/Estilos/Estilos.css").toExternalForm());
        pane2.getStyleClass().add("dialog-cuero");

        confirmDialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String clienteNombre = txtCliente.getText().trim();
                if (clienteNombre.isEmpty()) clienteNombre = null;

                // Obtener ID del cliente si fue seleccionado del autocompletado
                Long clienteId = txtCliente.getClienteId();

                String medioPago = cmbMedioPago.getValue();

                // 🔹 PASO 4: Procesar venta
                procesarVenta(producto, cantidad, total, medioPago, idVariante, clienteNombre, clienteId, onSuccess);
            }
        });
    }

    /**
     * Registra la venta en BD y actualiza stock
     */
    private static void procesarVenta(Producto producto, int cantidad, BigDecimal total,
                                      String medioPago, Long idVariante, String clienteNombre,
                                      Long clienteId, Runnable onSuccess) {
        try {
            // 🔹 PASO 1: Determinar ID del producto para ventaItem
            Long productoIdParaVenta;
            ProductoVariante variante = null;

            if (idVariante != null) {
                Optional<ProductoVariante> varOpt = varianteDAO.findById(idVariante);
                if (varOpt.isEmpty()) {
                    error("⚠ Variante no encontrada.");
                    return;
                }
                variante = varOpt.get();
                productoIdParaVenta = variante.getProductoId();
            } else {
                productoIdParaVenta = producto.getId();
            }

            // 🔹 PASO 2: Crear objeto venta
            Venta venta = new Venta();
            venta.setClienteNombre(clienteNombre);
            venta.setClienteId(clienteId); // Vincular con cliente si fue seleccionado
            venta.setMedioPago(medioPago);
            venta.setFecha(LocalDateTime.now());

            Venta.VentaItem item = new Venta.VentaItem();
            item.setProductoId(productoIdParaVenta);

            // 🔹 Construir nombre con color y talle si es variante
            String nombreCompleto = producto.getNombre();
            if (variante != null) {
                StringBuilder sb = new StringBuilder(nombreCompleto);
                if (variante.getColor() != null && !variante.getColor().isEmpty()) {
                    sb.append(" - ").append(variante.getColor());
                }
                if (variante.getTalle() != null && !variante.getTalle().isEmpty()) {
                    sb.append(" - Talle ").append(variante.getTalle());
                }
                nombreCompleto = sb.toString();
            }

            item.setProductoNombre(nombreCompleto);
            item.setProductoEtiqueta(producto.getEtiqueta());
            item.setQty(cantidad);
            item.setPrecioUnit(producto.getPrecio());

            if (idVariante != null) {
                item.setVarianteId(idVariante);
            }

            venta.addItem(item);
            venta.calcularTotal();

            // 🔹 PASO 3: Registrar venta (trigger descuenta stock automáticamente)
            Long ventaId;
            try {
                ventaId = VentaDAO.registrarVentaCompleta(venta);
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("Stock insuficiente")) {
                    error("⚠ Stock insuficiente. La venta no se completó.");
                    return;
                }
                throw e;
            }

            if (ventaId == null || ventaId <= 0) {
                error("❌ Error al registrar venta en base de datos.");
                return;
            }

            // ✅ PASO 4: Confirmación y recarga de UI
            NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
            ok("✅ Venta registrada correctamente. Total: " + formato.format(total));

            Platform.runLater(onSuccess);

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Stock insuficiente")) {
                error("⚠ Stock insuficiente para completar la venta.");
            } else {
                error("❌ Error al registrar venta: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            error("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers de notificaciones
    // ─────────────────────────────────────────────────────────────────

    private static void ok(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(3))
                .showConfirm();
    }

    private static void error(String msg) {
        Notifications.create()
                .text(msg)
                .position(javafx.geometry.Pos.BOTTOM_RIGHT)
                .hideAfter(javafx.util.Duration.seconds(5))
                .showError();
    }
}