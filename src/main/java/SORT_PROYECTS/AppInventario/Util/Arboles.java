package SORT_PROYECTS.AppInventario.Util;

import SORT_PROYECTS.AppInventario.Entidades.ItemInventario;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class Arboles {

    /**
     * Crea un TreeTableView con columnas dinámicas
     * Matriz: {título, propiedad}
     */
    public static <T> TreeTableView<T> crearTreeTabla(String[][] columnas) {
        TreeTableView<T> tree = new TreeTableView<>();
        tree.setShowRoot(false);

        for (String[] colDef : columnas) {
            String titulo = colDef[0];
            String propiedad = colDef[1];

            TreeTableColumn<T, Object> col = new TreeTableColumn<>(titulo);
            col.setId(propiedad);
            col.setCellValueFactory(new TreeItemPropertyValueFactory<>(propiedad));

            // Aplicar formato según tipo
            aplicarFormato(col, propiedad);

            // Alineación
            if (propiedad.contains("nombre") || propiedad.contains("descripcion")) {
                col.getStyleClass().add("col-left");
            } else {
                col.getStyleClass().add("col-center");
            }

            tree.getColumns().add(col);
        }

        return tree;
    }

    /**
     * Aplica formato automático según el nombre de la propiedad
     */
    private static <T> void aplicarFormato(TreeTableColumn<T, Object> col, String propiedad) {
        String propLower = propiedad.toLowerCase();

        // Formato de moneda
        if (propLower.contains("precio") || propLower.contains("total") ||
                propLower.contains("costo") || propLower.contains("subtotal")) {
            col.setCellFactory(tc -> new javafx.scene.control.TreeTableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
                        if (item instanceof BigDecimal) {
                            setText(formato.format((BigDecimal) item));
                        } else if (item instanceof Number) {
                            setText(formato.format(((Number) item).doubleValue()));
                        } else {
                            setText(item.toString());
                        }
                    }
                }
            });
        }
        // Formato de enteros
        else if (propLower.contains("stock") || propLower.equals("qty") || propLower.equals("cantidad")) {
            col.setCellFactory(tc -> new javafx.scene.control.TreeTableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else if (item instanceof Number) {
                        setText(String.format("%d", ((Number) item).intValue()));
                    } else {
                        setText(item.toString());
                    }
                }
            });
        }
    }

    // ============================================================================
// AGREGAR este método a la clase Arboles.java
// ============================================================================

    /**
     * Clona un TreeItem y filtra sus hijos según un criterio de búsqueda
     *
     * @param original TreeItem raíz a clonar
     * @param campo Campo por el cual filtrar ("nombre", "categoria", "etiqueta")
     * @param filtro Texto a buscar (case-insensitive)
     * @return TreeItem clonado con solo los nodos que coinciden con el filtro
     */
    public static <T> javafx.scene.control.TreeItem<T> clonarYFiltrar(
            javafx.scene.control.TreeItem<T> original,
            String campo,
            String filtro) {

        if (original == null) return null;

        // Crear copia del nodo actual
        javafx.scene.control.TreeItem<T> copia = new javafx.scene.control.TreeItem<>(original.getValue());
        copia.setExpanded(original.isExpanded());

        String filtroLower = filtro.toLowerCase().trim();

        // Procesar cada hijo recursivamente
        for (javafx.scene.control.TreeItem<T> hijo : original.getChildren()) {
            // Si el hijo o algún descendiente coincide, agregarlo
            javafx.scene.control.TreeItem<T> hijoFiltrado = clonarYFiltrar(hijo, campo, filtro);

            if (hijoFiltrado != null && !hijoFiltrado.getChildren().isEmpty()) {
                // Tiene hijos que coinciden
                copia.getChildren().add(hijoFiltrado);
            } else if (coincideConFiltro(hijo.getValue(), campo, filtroLower)) {
                // El nodo actual coincide
                copia.getChildren().add(hijoFiltrado != null ? hijoFiltrado :
                        new javafx.scene.control.TreeItem<>(hijo.getValue()));
            }
        }

        return copia;
    }

    /**
     * Verifica si un objeto coincide con el filtro en el campo especificado
     */
    private static <T> boolean coincideConFiltro(T value, String campo, String filtro) {
        if (value == null || filtro.isEmpty()) return true;

        // Usar reflexión para obtener el valor del campo
        try {
            String valorCampo = obtenerValorCampo(value, campo);
            return valorCampo != null && valorCampo.toLowerCase().contains(filtro);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene el valor de un campo de un objeto usando getters
     */
    private static <T> String obtenerValorCampo(T objeto, String campo) {
        if (objeto == null) return null;

        try {
            // Construir nombre del getter
            String getter = "get" + campo.substring(0, 1).toUpperCase() + campo.substring(1);

            // Intentar variantes comunes
            String[] posiblesGetters = {
                    getter,
                    "get" + campo.substring(0, 1).toUpperCase() + campo.substring(1) + "Producto",
                    "getNombre" + campo.substring(0, 1).toUpperCase() + campo.substring(1),
                    "getEtiqueta" + campo.substring(0, 1).toUpperCase() + campo.substring(1)
            };

            for (String metodo : posiblesGetters) {
                try {
                    java.lang.reflect.Method m = objeto.getClass().getMethod(metodo);
                    Object valor = m.invoke(objeto);
                    if (valor != null) return valor.toString();
                } catch (NoSuchMethodException ignored) {
                    // Probar siguiente variante
                }
            }

            // Casos especiales para ItemInventario
            if (campo.equals("nombre")) {
                return obtenerValorCampo(objeto, "nombreProducto");
            } else if (campo.equals("etiqueta")) {
                return obtenerValorCampo(objeto, "etiquetaProducto");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

// ============================================================================
// Alternativa más simple si la reflexión da problemas:
// ============================================================================

    /**
     * Versión alternativa específica para ItemInventario (sin reflexión)
     * Usar esta si la versión genérica da problemas
     */
    public static javafx.scene.control.TreeItem<ItemInventario>
    clonarYFiltrarItemInventario(
            javafx.scene.control.TreeItem<ItemInventario> original,
            String campo,
            String filtro) {

        if (original == null) return null;

        javafx.scene.control.TreeItem<ItemInventario> copia =
                new javafx.scene.control.TreeItem<>(original.getValue());
        copia.setExpanded(original.isExpanded());

        String filtroLower = filtro.toLowerCase().trim();

        for (javafx.scene.control.TreeItem<ItemInventario> hijo : original.getChildren()) {
            javafx.scene.control.TreeItem<ItemInventario> hijoFiltrado =
                    clonarYFiltrarItemInventario(hijo, campo, filtro);

            if (hijoFiltrado != null && !hijoFiltrado.getChildren().isEmpty()) {
                copia.getChildren().add(hijoFiltrado);
            } else if (hijo.getValue() != null) {
                ItemInventario item = hijo.getValue();
                boolean coincide = false;

                // Comparación directa sin reflexión
                switch (campo.toLowerCase()) {
                    case "nombre" -> coincide = item.getNombreProducto() != null &&
                            item.getNombreProducto().toLowerCase().contains(filtroLower);
                    case "categoria" -> coincide = item.getCategoria() != null &&
                            item.getCategoria().toLowerCase().contains(filtroLower);
                    case "etiqueta" -> coincide = item.getEtiquetaProducto() != null &&
                            item.getEtiquetaProducto().toLowerCase().contains(filtroLower);
                    default -> coincide = false;
                }

                if (coincide) {
                    copia.getChildren().add(hijoFiltrado != null ? hijoFiltrado :
                            new javafx.scene.control.TreeItem<>(hijo.getValue()));
                }
            }
        }

        return copia;
    }

    // ============================================================================
// AGREGAR a Arboles.java - Similar a Tablas.crearColumnas() pero para TreeTableView
// ============================================================================

    /**
     * Crea columnas para TreeTableView con pesos y anchos mínimos
     * Matriz: {Título, Propiedad, Peso, AnchoMin}
     *
     * @param columnas Definición de columnas [titulo, propiedad, peso, anchoMin]
     * @return Lista de TreeTableColumn configuradas
     */
    public static <T> java.util.List<javafx.scene.control.TreeTableColumn<T, ?>> crearColumnasTree(String[][] columnas) {
        java.util.List<javafx.scene.control.TreeTableColumn<T, ?>> lista = new java.util.ArrayList<>();

        for (String[] def : columnas) {
            String titulo = def[0];
            String propiedad = def[1];
            double peso = Double.parseDouble(def[2]);
            double anchoMin = Double.parseDouble(def[3]);

            // Crear columna genérica
            javafx.scene.control.TreeTableColumn<T, Object> col = new javafx.scene.control.TreeTableColumn<>(titulo);
            col.setId(propiedad);
            col.setCellValueFactory(new javafx.scene.control.cell.TreeItemPropertyValueFactory<>(propiedad));
            col.setMinWidth(anchoMin);
            col.setUserData(peso); // Guardar peso para ajustes dinámicos

            // Aplicar formato según tipo
            aplicarFormato(col, propiedad);

            // Alineación
            if (propiedad.contains("nombre") || propiedad.contains("descripcion")) {
                col.getStyleClass().add("col-left");
                col.setStyle("-fx-alignment: CENTER-LEFT;");
            } else {
                col.getStyleClass().add("col-center");
                col.setStyle("-fx-alignment: CENTER;");
            }

            lista.add(col);
        }

        return lista;
    }

    /**
     * Configura ajuste dinámico de columnas en un TreeTableView según pesos
     * Llamar después de agregar todas las columnas
     */
    public static <T> void configurarAjusteDinamicoTree(javafx.scene.control.TreeTableView<T> tree) {
        tree.setColumnResizePolicy(javafx.scene.control.TreeTableView.CONSTRAINED_RESIZE_POLICY);

        tree.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0) {
                ajustarAnchosPorPesoTree(tree, newW.doubleValue());
            }
        });

        // Aplicar tamaños iniciales
        if (tree.getWidth() > 0) {
            ajustarAnchosPorPesoTree(tree, tree.getWidth());
        }
    }

    /**
     * Ajusta anchos de columnas según peso definido en UserData
     */
    private static <T> void ajustarAnchosPorPesoTree(javafx.scene.control.TreeTableView<T> tree, double anchoTotal) {
        if (tree.getColumns().isEmpty()) return;

        double anchoDisponible = anchoTotal - 20; // margen para scrollbar

        for (javafx.scene.control.TreeTableColumn<T, ?> col : tree.getColumns()) {
            if (col.getUserData() instanceof Double peso) {
                double anchoCalculado = anchoDisponible * peso;
                double anchoMin = col.getMinWidth();
                col.setPrefWidth(Math.max(anchoCalculado, anchoMin));
            }
        }
    }

}