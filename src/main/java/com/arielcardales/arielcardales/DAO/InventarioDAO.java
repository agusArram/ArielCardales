package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.ItemInventario;
import com.arielcardales.arielcardales.Util.Mapper;
import javafx.scene.control.TreeItem;

import java.sql.*;
import java.util.*;

public class InventarioDAO {

    private static final String sqlbase =
            "select * from vInventario_variantes " +
                    "where ( " +
                    "  ? = '' " +
                    "  or lower(producto_etiqueta) like lower(?) " +
                    "  or lower(producto_nombre)  like lower(?) " +
                    "  or lower(coalesce(color,'')) like lower(?) " +
                    "  or lower(coalesce(talle,'')) like lower(?) " +
                    ") order by producto_nombre, color, talle";

    public static TreeItem<ItemInventario> cargarArbol(String filtro) throws SQLException {
        String f = filtro == null ? "" : filtro.trim();
        boolean porEtiqueta = f.matches("p\\d+");  // heurística p###

        String likeEtiqueta = porEtiqueta ? f + "%" : "%" + f + "%";
        String like = "%" + f + "%";

        TreeItem<ItemInventario> root = new TreeItem<>();
        Map<Long, TreeItem<ItemInventario>> padres = new LinkedHashMap<>();

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT * FROM vInventario_variantes " +
                             "WHERE active = true " +  // Filtra productos/variantes inactivos
                             "AND (? = '' " +
                             "OR lower(producto_etiqueta) LIKE lower(?) " +
                             "OR lower(producto_nombre) LIKE lower(?) " +
                             "OR lower(coalesce(color,'')) LIKE lower(?) " +
                             "OR lower(coalesce(talle,'')) LIKE lower(?)) " +
                             "ORDER BY producto_nombre, color, talle")) {

            ps.setString(1, f);
            ps.setString(2, likeEtiqueta);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long prodId = rs.getLong("producto_id");
                    Long varId = (Long) rs.getObject("variante_id");

                    TreeItem<ItemInventario> padre = padres.get(prodId);
                    if (padre == null) {
                        ItemInventario base = Mapper.getItemInventarioBase(rs);
                        padre = new TreeItem<>(base);
                        padres.put(prodId, padre);
                        root.getChildren().add(padre);
                    }

                    if (varId != null) {
                        ItemInventario hijo = Mapper.getItemInventarioVariante(rs);
                        padre.getChildren().add(new TreeItem<>(hijo));
                    }
                }
            }
        }

        // 🔧 Mejora: marcar los que no tienen hijos
        for (TreeItem<ItemInventario> padre : padres.values()) {
            if (padre.getChildren().isEmpty()) {
                ItemInventario base = padre.getValue();
                base.setColor("-");
                base.setTalle("-");
            }
        }

        return root;
    }

    public static boolean updateVarianteCampo(Long idVariante, String campo, String valor) {
        String columna = campo;

        // 🔄 Mapear nombres del modelo Java a columnas reales de la BD
        if (campo.equalsIgnoreCase("stockOnHand")) columna = "stock";
        if (campo.equalsIgnoreCase("nombreProducto")) columna = "nombre";
        if (campo.equalsIgnoreCase("precio")) columna = "precio";
        if (campo.equalsIgnoreCase("costo")) columna = "costo";

        // ✅ VALIDACIÓN ESPECIAL: Si edita color o talle, verificar duplicados
        if (columna.equalsIgnoreCase("color") || columna.equalsIgnoreCase("talle")) {
            try (Connection conn = Database.get()) {
                // 1. Obtener datos actuales de la variante
                String sqlActual = "SELECT producto_id, color, talle FROM producto_variante WHERE id = ?";
                Long productoId;
                String colorActual, talleActual;

                try (PreparedStatement ps = conn.prepareStatement(sqlActual)) {
                    ps.setLong(1, idVariante);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return false;
                        productoId = rs.getLong("producto_id");
                        colorActual = rs.getString("color");
                        talleActual = rs.getString("talle");
                    }
                }

                // 2. Determinar valores nuevos
                String nuevoColor = columna.equalsIgnoreCase("color") ? valor : colorActual;
                String nuevoTalle = columna.equalsIgnoreCase("talle") ? valor : talleActual;

                // 3. Verificar si ya existe otra variante con esa combinación
                String sqlCheck = "SELECT id FROM producto_variante " +
                        "WHERE producto_id = ? AND color = ? AND talle = ? AND id != ?";

                try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                    ps.setLong(1, productoId);
                    ps.setString(2, nuevoColor);
                    ps.setString(3, nuevoTalle);
                    ps.setLong(4, idVariante);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // ❌ Ya existe una variante con esa combinación
                            System.err.println("⚠️ Ya existe una variante con color=" + nuevoColor + " y talle=" + nuevoTalle);
                            return false;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        // 4. Si pasó la validación, hacer el UPDATE
        String sql = "UPDATE producto_variante SET " + columna + " = ? WHERE id = ?";

        try (Connection conn = Database.get();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, valor);
            stmt.setLong(2, idVariante);
            int filas = stmt.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}






