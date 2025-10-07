package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.DAO.ProductoDAO;
import com.arielcardales.arielcardales.Entidades.Producto;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class VentaController {

    public static void iniciarVenta(Producto producto, Runnable onFinish) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva venta");
        dialog.setHeaderText("Producto: " + producto.getNombre());
        dialog.setContentText("Cantidad vendida:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(valor -> {
            try {
                int cantidad = Integer.parseInt(valor);
                if (cantidad <= 0) throw new NumberFormatException();

                BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
                NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                String totalFormateado = formato.format(total);

                Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                        "Total: " + totalFormateado + "\n\n¿Confirmar venta?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirmar.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.OK) {
                        boolean ok = new ProductoDAO().descontarStock(producto.getId(), cantidad);
                        if (ok) {
                            new Alert(Alert.AlertType.INFORMATION, "Venta confirmada. Total: " + totalFormateado).showAndWait();
                            onFinish.run();
                        } else {
                            new Alert(Alert.AlertType.WARNING, "Stock insuficiente.").showAndWait();
                        }
                    }
                });
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Cantidad inválida.").showAndWait();
            }
        });
    }
}
