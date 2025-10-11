package com.arielcardales.arielcardales.service;

import com.arielcardales.arielcardales.DAO.*;
import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Util.Mapper;
import com.arielcardales.arielcardales.controller.ProductoTreeController;
import javafx.scene.control.TreeItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.prefs.Preferences;

public class InventarioService {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final ProductoVarianteDAO varianteDAO = new ProductoVarianteDAO();

    // 🔹 Cargar todo el árbol del inventario
    public TreeItem<ItemInventario> cargarArbol(String filtro) throws SQLException {
        TreeItem<ItemInventario> root = inventarioDAO.cargarArbol(filtro);

        limpiarCamposPadres(root);

        // 📦 Leemos preferencia del usuario
        Preferences prefs = Preferences.userNodeForPackage(ProductoTreeController.class);
        boolean expandir = prefs.getBoolean("expandir_nodos_hijos", false);

        if (expandir) {
            expandirNodos(root);
        }

        return root;
    }



    private void limpiarCamposPadres(TreeItem<ItemInventario> nodo) {
        if (nodo == null) return;

        ItemInventario item = nodo.getValue();
        if (item != null && !item.isEsVariante()) {
            boolean tieneHijos = nodo.getChildren() != null && !nodo.getChildren().isEmpty();
            if (tieneHijos) {
                // 🔸 Limpiar todos los campos que no aplican a productos base
                item.setColor(null);
                item.setTalle(null);
                item.setCosto(null);
                item.setPrecio(null);
                item.setStockOnHand(0);
            }
        }

        // 🔁 Recurre en todos los hijos
        if (nodo.getChildren() != null) {
            for (TreeItem<ItemInventario> hijo : nodo.getChildren()) {
                limpiarCamposPadres(hijo);
            }
        }
    }

    // 🔽 Expande automáticamente los productos con hijos
    private void expandirNodos(TreeItem<ItemInventario> nodo) {
        if (nodo == null) return;

        boolean tieneHijos = nodo.getChildren() != null && !nodo.getChildren().isEmpty();
        if (tieneHijos) {
            nodo.setExpanded(true); // 🔓 abre el nodo
            for (TreeItem<ItemInventario> hijo : nodo.getChildren()) {
                expandirNodos(hijo); // 🔁 recursivo
            }
        }
    }


    // 🔹 Actualizar un campo en un producto base
    public boolean actualizarCampo(long productoId, String campo, String valor) {
        try {
            return productoDAO.updateCampo(productoId, campo, valor);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Actualizar un campo en una variante
    public boolean actualizarVariante(long varianteId, String campo, String valor) {
        try {
            return InventarioDAO.updateVarianteCampo(varianteId, campo, valor);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Eliminar un producto base
    public boolean eliminarProducto(long productoId) {
        try {
            productoDAO.deleteById(productoId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Eliminar una variante
    public boolean eliminarVariante(long varianteId) {
        try {
            varianteDAO.deleteById(varianteId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Descontar stock después de una venta
    public boolean descontarStock(long productoId, int cantidad) {
        try {
            return productoDAO.descontarStock(productoId, cantidad);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
