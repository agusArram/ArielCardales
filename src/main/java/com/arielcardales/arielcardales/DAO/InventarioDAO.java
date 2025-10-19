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

    // ⚠️ DEPRECADO: updateVarianteCampo() movido a ProductoVarianteDAO.updateCampo()
    // Este método se eliminó para seguir el patrón DAO correcto
}







