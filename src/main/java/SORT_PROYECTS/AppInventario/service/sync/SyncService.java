package SORT_PROYECTS.AppInventario.service.sync;

import SORT_PROYECTS.AppInventario.DAO.*;
import SORT_PROYECTS.AppInventario.DAO.sqlite.*;
import SORT_PROYECTS.AppInventario.Entidades.*;
import SORT_PROYECTS.AppInventario.session.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de sincronización entre Supabase (PostgreSQL) y SQLite local
 */
public class SyncService {

    /**
     * Sincroniza datos desde la nube (Supabase) hacia el backup local (SQLite)
     * Dirección: PostgreSQL → SQLite
     *
     * @return SyncResult con estadísticas de la sincronización
     */
    public SyncResult syncFromCloud() {
        SyncResult result = new SyncResult();
        result.setDirection(SyncResult.SyncDirection.CLOUD_TO_LOCAL);

        System.out.println("========================================");
        System.out.println("Iniciando sincronización: Supabase → SQLite");
        System.out.println("========================================");

        try {
            // 1. Sincronizar Unidades
            syncUnidadesFromCloud(result);

            // 2. Sincronizar Categorías
            syncCategoriasFromCloud(result);

            // 3. Sincronizar Productos
            syncProductosFromCloud(result);

            // 4. Sincronizar Variantes
            syncVariantesFromCloud(result);

            // 5. Sincronizar Ventas
            syncVentasFromCloud(result);

            // 6. Sincronizar Clientes (del negocio)
            syncClientesFromCloud(result);

            result.setSuccess(true);
            result.setMessage("Sincronización completada exitosamente");

            System.out.println("\n✓ Sincronización Cloud → Local completada");
            System.out.println("  Total operaciones: " + result.getStats().getTotalOperaciones());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error durante la sincronización: " + e.getMessage());
            result.addError(e.getMessage());
            e.printStackTrace();

            System.err.println("\n✗ Error en sincronización: " + e.getMessage());
        }

        return result;
    }

    /**
     * Sincroniza datos desde el backup local (SQLite) hacia la nube (Supabase)
     * Dirección: SQLite → PostgreSQL
     *
     * @return SyncResult con estadísticas de la sincronización
     */
    public SyncResult syncToCloud() {
        SyncResult result = new SyncResult();
        result.setDirection(SyncResult.SyncDirection.LOCAL_TO_CLOUD);

        System.out.println("========================================");
        System.out.println("Iniciando sincronización: SQLite → Supabase");
        System.out.println("========================================");

        try {
            // 1. Sincronizar Unidades
            syncUnidadesToCloud(result);

            // 2. Sincronizar Categorías
            syncCategoriasToCloud(result);

            // 3. Sincronizar Productos
            syncProductosToCloud(result);

            // 4. Sincronizar Variantes
            syncVariantesToCloud(result);

            // 5. Sincronizar Ventas
            syncVentasToCloud(result);

            // 6. Sincronizar Clientes
            syncClientesToCloud(result);

            result.setSuccess(true);
            result.setMessage("Sincronización completada exitosamente");

            System.out.println("\n✓ Sincronización Local → Cloud completada");
            System.out.println("  Total operaciones: " + result.getStats().getTotalOperaciones());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error durante la sincronización: " + e.getMessage());
            result.addError(e.getMessage());
            e.printStackTrace();

            System.err.println("\n✗ Error en sincronización: " + e.getMessage());
        }

        return result;
    }

    // ========================================
    // SINCRONIZACIÓN CLOUD → LOCAL
    // ========================================

    /**
     * Sincroniza unidades de Supabase a SQLite
     */
    private void syncUnidadesFromCloud(SyncResult result) {
        try {
            System.out.println("\n[1/6] Sincronizando Unidades...");

            // Leer de Supabase
            List<Unidad> cloudUnidades = UnidadDAO.getAll();

            // Leer de SQLite para comparar
            List<Unidad> localUnidades = SqliteUnidadDAO.findAll();
            Map<Long, Unidad> localMap = new HashMap<>();
            for (Unidad u : localUnidades) {
                localMap.put(u.getId(), u);
            }

            // Sincronizar cada unidad
            for (Unidad cloudUnidad : cloudUnidades) {
                Unidad localUnidad = localMap.get(cloudUnidad.getId());

                if (localUnidad == null) {
                    // No existe en local: INSERT
                    SqliteUnidadDAO.insert(cloudUnidad);
                    result.getStats().incrementUnidadesInsertadas();
                } else {
                    // Existe: verificar si hay que actualizar
                    // En el caso de Unidad, no tiene updatedAt, así que actualizamos siempre
                    SqliteUnidadDAO.update(cloudUnidad);
                    result.getStats().incrementUnidadesActualizadas();
                }
            }

            System.out.println("  ✓ Unidades: " +
                    result.getStats().getUnidadesInsertadas() + " nuevas, " +
                    result.getStats().getUnidadesActualizadas() + " actualizadas");

        } catch (Exception e) {
            result.addError("Error sincronizando unidades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sincroniza categorías de Supabase a SQLite
     */
    private void syncCategoriasFromCloud(SyncResult result) {
        try {
            System.out.println("\n[2/6] Sincronizando Categorías...");

            CategoriaDAO categoriaDAO = new CategoriaDAO();
            List<Categoria> cloudCategorias = categoriaDAO.findAll();

            List<Categoria> localCategorias = SqliteCategoriaDAO.findAll();
            Map<Long, Categoria> localMap = new HashMap<>();
            for (Categoria c : localCategorias) {
                localMap.put(c.getId(), c);
            }

            for (Categoria cloudCategoria : cloudCategorias) {
                Categoria localCategoria = localMap.get(cloudCategoria.getId());

                if (localCategoria == null) {
                    SqliteCategoriaDAO.insert(cloudCategoria);
                    result.getStats().incrementCategoriasInsertadas();
                } else {
                    SqliteCategoriaDAO.update(cloudCategoria);
                    result.getStats().incrementCategoriasActualizadas();
                }
            }

            System.out.println("  ✓ Categorías: " +
                    result.getStats().getCategoriasInsertadas() + " nuevas, " +
                    result.getStats().getCategoriasActualizadas() + " actualizadas");

        } catch (Exception e) {
            result.addError("Error sincronizando categorías: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sincroniza productos de Supabase a SQLite
     */
    private void syncProductosFromCloud(SyncResult result) {
        try {
            System.out.println("\n[3/6] Sincronizando Productos...");

            ProductoDAO productoDAO = new ProductoDAO();
            List<Producto> cloudProductos = productoDAO.findAllForSync();

            List<Producto> localProductos = SqliteProductoDAO.findAll();
            Map<Long, Producto> localMap = new HashMap<>();
            for (Producto p : localProductos) {
                localMap.put(p.getId(), p);
            }

            for (Producto cloudProducto : cloudProductos) {
                Producto localProducto = localMap.get(cloudProducto.getId());

                if (localProducto == null) {
                    SqliteProductoDAO.insert(cloudProducto);
                    result.getStats().incrementProductosInsertados();
                } else {
                    // Verificar conflicto con updatedAt
                    ConflictResolver.ConflictResolution resolution =
                            ConflictResolver.resolve(cloudProducto.getUpdatedAt(), localProducto.getUpdatedAt());

                    if (resolution == ConflictResolver.ConflictResolution.USE_CLOUD) {
                        SqliteProductoDAO.update(cloudProducto);
                        result.getStats().incrementProductosActualizados();
                        if (ConflictResolver.hasConflict(cloudProducto.getUpdatedAt(), localProducto.getUpdatedAt())) {
                            result.getStats().incrementConflictosResueltos();
                        }
                    } else if (resolution == ConflictResolver.ConflictResolution.NO_CONFLICT) {
                        // No hacer nada, están sincronizados
                    }
                    // Si resolution es USE_LOCAL, no actualizamos desde cloud
                }
            }

            System.out.println("  ✓ Productos: " +
                    result.getStats().getProductosInsertados() + " nuevos, " +
                    result.getStats().getProductosActualizados() + " actualizados");

        } catch (Exception e) {
            result.addError("Error sincronizando productos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sincroniza variantes de Supabase a SQLite
     */
    private void syncVariantesFromCloud(SyncResult result) {
        try {
            System.out.println("\n[4/6] Sincronizando Variantes...");

            ProductoVarianteDAO varianteDAO = new ProductoVarianteDAO();
            List<ProductoVariante> cloudVariantes = varianteDAO.findAll();

            List<ProductoVariante> localVariantes = SqliteProductoVarianteDAO.findAll();
            Map<Long, ProductoVariante> localMap = new HashMap<>();
            for (ProductoVariante v : localVariantes) {
                localMap.put(v.getId(), v);
            }

            for (ProductoVariante cloudVariante : cloudVariantes) {
                ProductoVariante localVariante = localMap.get(cloudVariante.getId());

                if (localVariante == null) {
                    SqliteProductoVarianteDAO.insert(cloudVariante);
                    result.getStats().incrementVariantesInsertadas();
                } else {
                    ConflictResolver.ConflictResolution resolution =
                            ConflictResolver.resolve(cloudVariante.getUpdatedAt(), localVariante.getUpdatedAt());

                    if (resolution == ConflictResolver.ConflictResolution.USE_CLOUD) {
                        SqliteProductoVarianteDAO.update(cloudVariante);
                        result.getStats().incrementVariantesActualizadas();
                        if (ConflictResolver.hasConflict(cloudVariante.getUpdatedAt(), localVariante.getUpdatedAt())) {
                            result.getStats().incrementConflictosResueltos();
                        }
                    }
                }
            }

            System.out.println("  ✓ Variantes: " +
                    result.getStats().getVariantesInsertadas() + " nuevas, " +
                    result.getStats().getVariantesActualizadas() + " actualizadas");

        } catch (Exception e) {
            result.addError("Error sincronizando variantes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sincroniza ventas de Supabase a SQLite
     */
    private void syncVentasFromCloud(SyncResult result) {
        try {
            System.out.println("\n[5/6] Sincronizando Ventas...");

            List<Venta> cloudVentas = VentaDAO.obtenerTodasLasVentas();

            List<Venta> localVentas = SqliteVentaDAO.findAll();
            Map<Long, Venta> localMap = new HashMap<>();
            for (Venta v : localVentas) {
                localMap.put(v.getId(), v);
            }

            for (Venta cloudVenta : cloudVentas) {
                Venta localVenta = localMap.get(cloudVenta.getId());

                if (localVenta == null) {
                    // Insertar venta (mantiene ID original de Supabase)
                    SqliteVentaDAO.insert(cloudVenta);

                    // Insertar items de la venta
                    List<Venta.VentaItem> items = VentaDAO.obtenerItemsDeVenta(cloudVenta.getId());
                    for (Venta.VentaItem item : items) {
                        item.setVentaId(cloudVenta.getId());
                        SqliteVentaDAO.insertItem(item);
                    }

                    result.getStats().incrementVentasInsertadas();
                }
                // Para ventas, generalmente no se actualizan, solo se insertan
            }

            System.out.println("  ✓ Ventas: " +
                    result.getStats().getVentasInsertadas() + " nuevas");

        } catch (Exception e) {
            result.addError("Error sincronizando ventas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sincroniza clientes del negocio de Supabase a SQLite
     */
    private void syncClientesFromCloud(SyncResult result) {
        try {
            System.out.println("\n[6/6] Sincronizando Clientes...");

            ClienteDAO clienteDAO = new ClienteDAO();
            // Nota: ClienteDAO devuelve com.AppInventario.AppInventario.Entidades.Cliente
            // pero SqliteClienteDAO usa su propia clase interna Cliente
            // Por ahora, contamos pero no sincronizamos clientes para evitar conflictos de tipos
            // TODO: Implementar mapeo de Cliente si es necesario

            System.out.println("  ⚠ Sincronización de clientes pendiente de implementación");

        } catch (Exception e) {
            result.addError("Error sincronizando clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================
    // SINCRONIZACIÓN LOCAL → CLOUD
    // ========================================

    /**
     * Sincroniza unidades de SQLite a Supabase
     */
    private void syncUnidadesToCloud(SyncResult result) {
        try {
            System.out.println("\n[1/6] Sincronizando Unidades a Cloud...");

            // Leer de SQLite
            List<Unidad> localUnidades = SqliteUnidadDAO.findAll();

            // Leer de Supabase
            List<Unidad> cloudUnidades = UnidadDAO.getAll();
            Map<Long, Unidad> cloudMap = new HashMap<>();
            for (Unidad u : cloudUnidades) {
                cloudMap.put(u.getId(), u);
            }

            // Sincronizar cada unidad local a cloud
            for (Unidad localUnidad : localUnidades) {
                Unidad cloudUnidad = cloudMap.get(localUnidad.getId());

                if (cloudUnidad == null) {
                    // No existe en cloud: INSERT
                    upsertUnidadToCloud(localUnidad, true);
                    result.getStats().incrementUnidadesInsertadas();
                } else {
                    // Existe: siempre actualizamos (Unidad no tiene updatedAt)
                    upsertUnidadToCloud(localUnidad, false);
                    result.getStats().incrementUnidadesActualizadas();
                }
            }

            System.out.println("  ✓ Unidades a Cloud: " +
                    result.getStats().getUnidadesInsertadas() + " nuevas, " +
                    result.getStats().getUnidadesActualizadas() + " actualizadas");

        } catch (Exception e) {
            result.addError("Error sincronizando unidades a cloud: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncCategoriasToCloud(SyncResult result) {
        try {
            System.out.println("\n[2/6] Sincronizando Categorías a Cloud...");

            // Leer de SQLite
            List<Categoria> localCategorias = SqliteCategoriaDAO.findAll();

            // Leer de Supabase
            CategoriaDAO categoriaDAO = new CategoriaDAO();
            List<Categoria> cloudCategorias = categoriaDAO.findAll();
            Map<Long, Categoria> cloudMap = new HashMap<>();
            for (Categoria c : cloudCategorias) {
                cloudMap.put(c.getId(), c);
            }

            // Sincronizar cada categoría local a cloud
            for (Categoria localCategoria : localCategorias) {
                Categoria cloudCategoria = cloudMap.get(localCategoria.getId());

                if (cloudCategoria == null) {
                    // No existe en cloud: INSERT
                    upsertCategoriaToCloud(localCategoria);
                    result.getStats().incrementCategoriasInsertadas();
                } else {
                    // Existe: siempre actualizamos (Categoria no tiene updatedAt)
                    upsertCategoriaToCloud(localCategoria);
                    result.getStats().incrementCategoriasActualizadas();
                }
            }

            System.out.println("  ✓ Categorías a Cloud: " +
                    result.getStats().getCategoriasInsertadas() + " nuevas, " +
                    result.getStats().getCategoriasActualizadas() + " actualizadas");

        } catch (Exception e) {
            result.addError("Error sincronizando categorías a cloud: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncProductosToCloud(SyncResult result) {
        try {
            System.out.println("\n[3/6] Sincronizando Productos a Cloud...");

            // Leer de SQLite
            List<Producto> localProductos = SqliteProductoDAO.findAll();

            // Leer de Supabase
            ProductoDAO productoDAO = new ProductoDAO();
            List<Producto> cloudProductos = productoDAO.findAllForSync();
            Map<Long, Producto> cloudMap = new HashMap<>();
            for (Producto p : cloudProductos) {
                cloudMap.put(p.getId(), p);
            }

            // Sincronizar cada producto local a cloud
            for (Producto localProducto : localProductos) {
                Producto cloudProducto = cloudMap.get(localProducto.getId());

                if (cloudProducto == null) {
                    // No existe en cloud: INSERT
                    upsertProductoToCloud(localProducto);
                    result.getStats().incrementProductosInsertados();
                } else {
                    // Verificar conflicto con updatedAt
                    ConflictResolver.ConflictResolution resolution =
                            ConflictResolver.resolve(cloudProducto.getUpdatedAt(), localProducto.getUpdatedAt());

                    if (resolution == ConflictResolver.ConflictResolution.USE_LOCAL) {
                        upsertProductoToCloud(localProducto);
                        result.getStats().incrementProductosActualizados();
                        if (ConflictResolver.hasConflict(cloudProducto.getUpdatedAt(), localProducto.getUpdatedAt())) {
                            result.getStats().incrementConflictosResueltos();
                        }
                    } else if (resolution == ConflictResolver.ConflictResolution.NO_CONFLICT) {
                        // No hacer nada, están sincronizados
                    }
                    // Si resolution es USE_CLOUD, no actualizamos desde local
                }
            }

            System.out.println("  ✓ Productos a Cloud: " +
                    result.getStats().getProductosInsertados() + " nuevos, " +
                    result.getStats().getProductosActualizados() + " actualizados");

        } catch (Exception e) {
            result.addError("Error sincronizando productos a cloud: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncVariantesToCloud(SyncResult result) {
        try {
            System.out.println("\n[4/6] Sincronizando Variantes a Cloud...");

            // Leer de SQLite
            List<ProductoVariante> localVariantes = SqliteProductoVarianteDAO.findAll();

            // Leer de Supabase
            ProductoVarianteDAO varianteDAO = new ProductoVarianteDAO();
            List<ProductoVariante> cloudVariantes = varianteDAO.findAll();
            Map<Long, ProductoVariante> cloudMap = new HashMap<>();
            for (ProductoVariante v : cloudVariantes) {
                cloudMap.put(v.getId(), v);
            }

            // Sincronizar cada variante local a cloud
            for (ProductoVariante localVariante : localVariantes) {
                ProductoVariante cloudVariante = cloudMap.get(localVariante.getId());

                if (cloudVariante == null) {
                    // No existe en cloud: INSERT
                    upsertVarianteToCloud(localVariante);
                    result.getStats().incrementVariantesInsertadas();
                } else {
                    // Verificar conflicto con updatedAt
                    ConflictResolver.ConflictResolution resolution =
                            ConflictResolver.resolve(cloudVariante.getUpdatedAt(), localVariante.getUpdatedAt());

                    if (resolution == ConflictResolver.ConflictResolution.USE_LOCAL) {
                        upsertVarianteToCloud(localVariante);
                        result.getStats().incrementVariantesActualizadas();
                        if (ConflictResolver.hasConflict(cloudVariante.getUpdatedAt(), localVariante.getUpdatedAt())) {
                            result.getStats().incrementConflictosResueltos();
                        }
                    } else if (resolution == ConflictResolver.ConflictResolution.NO_CONFLICT) {
                        // No hacer nada, están sincronizados
                    }
                    // Si resolution es USE_CLOUD, no actualizamos desde local
                }
            }

            System.out.println("  ✓ Variantes a Cloud: " +
                    result.getStats().getVariantesInsertadas() + " nuevas, " +
                    result.getStats().getVariantesActualizadas() + " actualizadas");

        } catch (Exception e) {
            result.addError("Error sincronizando variantes a cloud: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncVentasToCloud(SyncResult result) {
        try {
            System.out.println("\n[5/6] Sincronizando Ventas a Cloud...");

            // Leer de SQLite
            List<Venta> localVentas = SqliteVentaDAO.findAll();

            // Leer de Supabase
            List<Venta> cloudVentas = VentaDAO.obtenerTodasLasVentas();
            Map<Long, Venta> cloudMap = new HashMap<>();
            for (Venta v : cloudVentas) {
                cloudMap.put(v.getId(), v);
            }

            // Sincronizar cada venta local a cloud
            for (Venta localVenta : localVentas) {
                Venta cloudVenta = cloudMap.get(localVenta.getId());

                if (cloudVenta == null) {
                    // No existe en cloud: INSERT venta y sus items
                    upsertVentaToCloud(localVenta);

                    // Insertar items de la venta
                    List<Venta.VentaItem> items = SqliteVentaDAO.findItemsByVentaId(localVenta.getId());
                    for (Venta.VentaItem item : items) {
                        upsertVentaItemToCloud(item);
                    }

                    result.getStats().incrementVentasInsertadas();
                }
                // Para ventas, generalmente no se actualizan, solo se insertan
            }

            System.out.println("  ✓ Ventas a Cloud: " +
                    result.getStats().getVentasInsertadas() + " nuevas");

        } catch (Exception e) {
            result.addError("Error sincronizando ventas a cloud: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncClientesToCloud(SyncResult result) {
        System.out.println("\n[6/6] Sincronizando Clientes a Cloud...");
        System.out.println("  ⚠ Pendiente de implementación");
    }

    // ========================================
    // MÉTODOS AUXILIARES DE UPSERT
    // ========================================

    /**
     * Inserta o actualiza una unidad en Supabase
     */
    private void upsertUnidadToCloud(Unidad unidad, boolean isInsert) throws SQLException {
        String clienteId = SessionManager.getInstance().getClienteId();

        String sql = """
            INSERT INTO unidad (id, nombre, abreviatura, cliente_id, createdAt)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                nombre = EXCLUDED.nombre,
                abreviatura = EXCLUDED.abreviatura
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, unidad.getId());
            ps.setString(2, unidad.getNombre());
            ps.setString(3, unidad.getAbreviatura());
            ps.setString(4, clienteId);
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(unidad.getCreatedAt()));

            ps.executeUpdate();
        }
    }

    /**
     * Inserta o actualiza una categoría en Supabase
     */
    private void upsertCategoriaToCloud(Categoria categoria) throws SQLException {
        String clienteId = SessionManager.getInstance().getClienteId();

        String sql = """
            INSERT INTO categoria (id, nombre, parentId, cliente_id, createdAt)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                nombre = EXCLUDED.nombre,
                parentId = EXCLUDED.parentId
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, categoria.getId());
            ps.setString(2, categoria.getNombre());

            if (categoria.getParentId() == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, categoria.getParentId());
            }

            ps.setString(4, clienteId);
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(categoria.getCreatedAt()));

            ps.executeUpdate();
        }
    }

    /**
     * Inserta o actualiza un producto en Supabase
     */
    private void upsertProductoToCloud(Producto producto) throws SQLException {
        String clienteId = SessionManager.getInstance().getClienteId();

        String sql = """
            INSERT INTO producto (id, etiqueta, nombre, descripcion, categoriaId, unidadId,
                                  precio, costo, stockOnHand, active, cliente_id, createdAt, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                etiqueta = EXCLUDED.etiqueta,
                nombre = EXCLUDED.nombre,
                descripcion = EXCLUDED.descripcion,
                categoriaId = EXCLUDED.categoriaId,
                unidadId = EXCLUDED.unidadId,
                precio = EXCLUDED.precio,
                costo = EXCLUDED.costo,
                stockOnHand = EXCLUDED.stockOnHand,
                active = EXCLUDED.active,
                updatedAt = EXCLUDED.updatedAt
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, producto.getId());
            ps.setString(2, producto.getEtiqueta());
            ps.setString(3, producto.getNombre());

            if (producto.getDescripcion() == null || producto.getDescripcion().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, producto.getDescripcion());
            }

            ps.setLong(5, producto.getCategoriaId());
            ps.setLong(6, producto.getUnidadId());
            ps.setDouble(7, producto.getPrecio().doubleValue());
            ps.setDouble(8, producto.getCosto().doubleValue());
            ps.setInt(9, producto.getStockOnHand());
            ps.setBoolean(10, producto.isActive());
            ps.setString(11, clienteId);
            ps.setTimestamp(12, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())); // createdAt
            ps.setTimestamp(13, java.sql.Timestamp.valueOf(producto.getUpdatedAt()));

            ps.executeUpdate();
        }
    }

    /**
     * Inserta o actualiza una variante en Supabase
     */
    private void upsertVarianteToCloud(ProductoVariante variante) throws SQLException {
        String clienteId = SessionManager.getInstance().getClienteId();

        String sql = """
            INSERT INTO producto_variante (id, producto_id, color, talle, precio, costo, stock,
                                           etiqueta, active, cliente_id, createdAt, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                producto_id = EXCLUDED.producto_id,
                color = EXCLUDED.color,
                talle = EXCLUDED.talle,
                precio = EXCLUDED.precio,
                costo = EXCLUDED.costo,
                stock = EXCLUDED.stock,
                etiqueta = EXCLUDED.etiqueta,
                active = EXCLUDED.active,
                updatedAt = EXCLUDED.updatedAt
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, variante.getId());
            ps.setLong(2, variante.getProductoId());

            if (variante.getColor() == null || variante.getColor().isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, variante.getColor());
            }

            if (variante.getTalle() == null || variante.getTalle().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, variante.getTalle());
            }

            ps.setDouble(5, variante.getPrecio().doubleValue());
            ps.setDouble(6, variante.getCosto().doubleValue());
            ps.setInt(7, variante.getStock());

            if (variante.getEtiqueta() == null || variante.getEtiqueta().isEmpty()) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, variante.getEtiqueta());
            }

            ps.setBoolean(9, variante.isActive());
            ps.setString(10, clienteId);
            ps.setTimestamp(11, java.sql.Timestamp.valueOf(variante.getCreatedAt()));
            ps.setTimestamp(12, java.sql.Timestamp.valueOf(variante.getUpdatedAt()));

            ps.executeUpdate();
        }
    }

    /**
     * Inserta o actualiza una venta en Supabase
     */
    private void upsertVentaToCloud(Venta venta) throws SQLException {
        String clienteId = SessionManager.getInstance().getClienteId();

        String sql = """
            INSERT INTO venta (id, clienteNombre, fecha, medioPago, total, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, venta.getId());

            if (venta.getClienteNombre() == null || venta.getClienteNombre().isEmpty()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, venta.getClienteNombre());
            }

            ps.setTimestamp(3, java.sql.Timestamp.valueOf(venta.getFecha()));

            if (venta.getMedioPago() == null || venta.getMedioPago().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, venta.getMedioPago());
            }

            ps.setDouble(5, venta.getTotal().doubleValue());
            ps.setString(6, clienteId);

            ps.executeUpdate();
        }
    }

    /**
     * Inserta o actualiza un item de venta en Supabase
     */
    private void upsertVentaItemToCloud(Venta.VentaItem item) throws SQLException {
        String clienteId = SessionManager.getInstance().getClienteId();

        String sql = """
            INSERT INTO ventaItem (id, ventaId, productoId, variante_id, qty, precioUnit, productoNombre, cliente_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, item.getId());
            ps.setLong(2, item.getVentaId());
            ps.setLong(3, item.getProductoId());

            if (item.getVarianteId() == null || item.getVarianteId() == 0) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, item.getVarianteId());
            }

            ps.setInt(5, item.getQty());
            ps.setDouble(6, item.getPrecioUnit().doubleValue());

            if (item.getProductoNombre() == null || item.getProductoNombre().isEmpty()) {
                ps.setNull(7, Types.VARCHAR);
            } else {
                ps.setString(7, item.getProductoNombre());
            }

            ps.setString(8, clienteId);

            ps.executeUpdate();
        }
    }
}
