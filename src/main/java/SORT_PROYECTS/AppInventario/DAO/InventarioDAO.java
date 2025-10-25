package SORT_PROYECTS.AppInventario.DAO;

import SORT_PROYECTS.AppInventario.Entidades.ItemInventario;
import SORT_PROYECTS.AppInventario.Util.Mapper;
import SORT_PROYECTS.AppInventario.session.SessionManager;
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
        String clienteId = SessionManager.getInstance().getClienteId();
        String f = filtro == null ? "" : filtro.trim();
        boolean porEtiqueta = f.matches("p\\d+");  // heurística p###

        String likeEtiqueta = porEtiqueta ? f + "%" : "%" + f + "%";
        String like = "%" + f + "%";

        TreeItem<ItemInventario> root = new TreeItem<>();
        Map<Long, TreeItem<ItemInventario>> padres = new LinkedHashMap<>();

        try (Connection cn = Database.getWithFallback()) {
            boolean isSqlite = Database.isSqlite(cn);

            String sql;
            if (isSqlite) {
                // SQLite: construir vista manualmente con JOINs
                sql = """
                    SELECT
                        p.id as producto_id,
                        p.etiqueta as producto_etiqueta,
                        p.nombre as producto_nombre,
                        COALESCE(c.nombre, '') as categoria,
                        COALESCE(u.nombre, '') as unidad,
                        COALESCE(pv.color, NULL) as color,
                        COALESCE(pv.talle, NULL) as talle,
                        COALESCE(pv.precio, p.precio) as precio,
                        COALESCE(pv.costo, p.costo) as costo,
                        COALESCE(pv.stock, p.stockOnHand) as stockOnHand,
                        COALESCE(pv.active, p.active) as active,
                        COALESCE(pv.updatedAt, p.updatedAt) as updatedAt,
                        pv.id as variante_id
                    FROM producto p
                    LEFT JOIN categoria c ON c.id = p.categoriaId
                    LEFT JOIN unidad u ON u.id = p.unidadId
                    LEFT JOIN producto_variante pv ON pv.producto_id = p.id
                    WHERE COALESCE(pv.active, p.active) = 1
                      AND p.cliente_id = ?
                      AND (? = ''
                           OR lower(p.etiqueta) LIKE lower(?)
                           OR lower(p.nombre) LIKE lower(?)
                           OR lower(COALESCE(pv.color,'')) LIKE lower(?)
                           OR lower(COALESCE(pv.talle,'')) LIKE lower(?))
                    ORDER BY p.nombre, pv.color, pv.talle
                """;
            } else {
                // PostgreSQL: usar vista vInventario_variantes
                sql = """
                    SELECT * FROM vInventario_variantes
                    WHERE active = true
                      AND cliente_id = ?
                      AND (? = ''
                           OR lower(producto_etiqueta) LIKE lower(?)
                           OR lower(producto_nombre) LIKE lower(?)
                           OR lower(coalesce(color,'')) LIKE lower(?)
                           OR lower(coalesce(talle,'')) LIKE lower(?))
                    ORDER BY producto_nombre, color, talle
                """;
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, clienteId);
                ps.setString(2, f);
                ps.setString(3, likeEtiqueta);
                ps.setString(4, like);
                ps.setString(5, like);
                ps.setString(6, like);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long prodId = rs.getLong("producto_id");

                        // Fix para casting seguro de variante_id (SQLite devuelve Integer, no Long)
                        Object varIdObj = rs.getObject("variante_id");
                        Long varId = null;
                        if (varIdObj != null) {
                            if (varIdObj instanceof Long) {
                                varId = (Long) varIdObj;
                            } else if (varIdObj instanceof Integer) {
                                varId = ((Integer) varIdObj).longValue();
                            }
                        }

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







