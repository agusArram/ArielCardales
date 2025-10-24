package SORT_PROYECTS.AppInventario.service.sync;

import SORT_PROYECTS.AppInventario.DAO.*;
import SORT_PROYECTS.AppInventario.DAO.sqlite.*;
import SORT_PROYECTS.AppInventario.Entidades.*;
import SORT_PROYECTS.AppInventario.DAO.*;
import SORT_PROYECTS.AppInventario.DAO.sqlite.*;
import SORT_PROYECTS.AppInventario.Entidades.*;

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

            // Por ahora, solo reportamos, no subimos
            // La lógica sería similar a syncFromCloud pero invirtiendo origen/destino
            System.out.println("  ⚠ Sincronización de unidades a cloud pendiente de implementación completa");
            System.out.println("    (Solo lectura habilitada - no se suben cambios)");

        } catch (Exception e) {
            result.addError("Error sincronizando unidades a cloud: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncCategoriasToCloud(SyncResult result) {
        System.out.println("\n[2/6] Sincronizando Categorías a Cloud...");
        System.out.println("  ⚠ Pendiente de implementación");
    }

    private void syncProductosToCloud(SyncResult result) {
        System.out.println("\n[3/6] Sincronizando Productos a Cloud...");
        System.out.println("  ⚠ Pendiente de implementación");
    }

    private void syncVariantesToCloud(SyncResult result) {
        System.out.println("\n[4/6] Sincronizando Variantes a Cloud...");
        System.out.println("  ⚠ Pendiente de implementación");
    }

    private void syncVentasToCloud(SyncResult result) {
        System.out.println("\n[5/6] Sincronizando Ventas a Cloud...");
        System.out.println("  ⚠ Pendiente de implementación");
    }

    private void syncClientesToCloud(SyncResult result) {
        System.out.println("\n[6/6] Sincronizando Clientes a Cloud...");
        System.out.println("  ⚠ Pendiente de implementación");
    }
}
