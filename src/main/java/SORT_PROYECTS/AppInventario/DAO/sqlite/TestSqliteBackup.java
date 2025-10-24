package SORT_PROYECTS.AppInventario.DAO.sqlite;

import SORT_PROYECTS.AppInventario.Entidades.Categoria;
import SORT_PROYECTS.AppInventario.Entidades.Producto;
import SORT_PROYECTS.AppInventario.Entidades.Unidad;

import java.math.BigDecimal;
import java.util.List;

/**
 * Test manual para verificar funcionamiento de SQLite backup
 * Para ejecutar: java -cp ... TestSqliteBackup
 */
public class TestSqliteBackup {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TEST: SQLite Backup System");
        System.out.println("========================================");

        try {
            // 1. Test de conexión y creación de DB
            System.out.println("\n[1] Probando conexión y creación de base de datos...");
            var conn = SqliteDatabase.get();
            System.out.println("✓ Conexión establecida");
            System.out.println("  - Ruta: " + SqliteDatabase.getDbPath());
            System.out.println("  - Cliente ID: " + SqliteDatabase.getClienteId());
            System.out.println("  - Inicializada: " + SqliteDatabase.isInitialized());

            // 2. Test de Unidad
            System.out.println("\n[2] Probando CRUD de Unidad...");
            Unidad unidad = new Unidad();
            unidad.setNombre("Unidad Test");
            unidad.setAbreviatura("ut");

            long unidadId = SqliteUnidadDAO.insert(unidad);
            System.out.println("✓ Unidad insertada con ID: " + unidadId);

            List<Unidad> unidades = SqliteUnidadDAO.findAll();
            System.out.println("✓ Unidades encontradas: " + unidades.size());

            // 3. Test de Categoría
            System.out.println("\n[3] Probando CRUD de Categoría...");
            Categoria categoria = new Categoria();
            categoria.setNombre("Categoría Test");
            categoria.setParentId(null);

            long categoriaId = SqliteCategoriaDAO.insert(categoria);
            System.out.println("✓ Categoría insertada con ID: " + categoriaId);

            List<Categoria> categorias = SqliteCategoriaDAO.findAll();
            System.out.println("✓ Categorías encontradas: " + categorias.size());

            // 4. Test de Producto
            System.out.println("\n[4] Probando CRUD de Producto...");
            Producto producto = new Producto();
            producto.setEtiqueta("PROD-TEST-001");
            producto.setNombre("Producto Test");
            producto.setDescripcion("Descripción de prueba");
            producto.setCategoriaId(categoriaId);
            producto.setUnidadId(unidadId);
            producto.setPrecio(new BigDecimal("100.50"));
            producto.setCosto(new BigDecimal("50.25"));
            producto.setStockOnHand(10);
            producto.setActive(true);

            long productoId = SqliteProductoDAO.insert(producto);
            System.out.println("✓ Producto insertado con ID: " + productoId);

            List<Producto> productos = SqliteProductoDAO.findAll();
            System.out.println("✓ Productos encontrados: " + productos.size());

            // 5. Test de lectura por ID
            System.out.println("\n[5] Probando lectura por ID...");
            Producto productoLeido = SqliteProductoDAO.findById(productoId);
            System.out.println("✓ Producto leído: " + productoLeido.getNombre());
            System.out.println("  - Precio: " + productoLeido.getPrecio());
            System.out.println("  - Stock: " + productoLeido.getStockOnHand());

            // 6. Test de actualización
            System.out.println("\n[6] Probando actualización...");
            productoLeido.setPrecio(new BigDecimal("150.75"));
            productoLeido.setStockOnHand(20);
            SqliteProductoDAO.update(productoLeido);
            System.out.println("✓ Producto actualizado");

            Producto productoActualizado = SqliteProductoDAO.findById(productoId);
            System.out.println("  - Nuevo precio: " + productoActualizado.getPrecio());
            System.out.println("  - Nuevo stock: " + productoActualizado.getStockOnHand());

            // 7. Test de conteo
            System.out.println("\n[7] Probando contadores...");
            System.out.println("✓ Total unidades: " + SqliteUnidadDAO.count());
            System.out.println("✓ Total categorías: " + SqliteCategoriaDAO.count());
            System.out.println("✓ Total productos: " + SqliteProductoDAO.count());

            // 8. Limpieza (eliminar datos de prueba)
            System.out.println("\n[8] Limpiando datos de prueba...");
            SqliteProductoDAO.deleteById(productoId);
            SqliteCategoriaDAO.deleteById(categoriaId);
            SqliteUnidadDAO.deleteById(unidadId);
            System.out.println("✓ Datos de prueba eliminados");

            System.out.println("\n========================================");
            System.out.println("✓ TODOS LOS TESTS PASARON EXITOSAMENTE");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("✗ ERROR EN LOS TESTS");
            System.err.println("========================================");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
