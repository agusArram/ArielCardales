package SORT_PROYECTS.AppInventario.service.sync;

import SORT_PROYECTS.AppInventario.DAO.LicenciaDAO;
import SORT_PROYECTS.AppInventario.DAO.sqlite.SqliteDatabase;
import SORT_PROYECTS.AppInventario.Licencia.Licencia;
import SORT_PROYECTS.AppInventario.Licencia.LicenciaConfig;
import SORT_PROYECTS.AppInventario.session.SessionManager;

/**
 * Test manual de sincronización
 * Ejecutar con: mvn exec:java -Dexec.mainClass="com.AppInventario.AppInventario.service.sync.TestSync"
 */
public class TestSync {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TEST: Sincronización Supabase ↔ SQLite");
        System.out.println("========================================");

        try {
            // 1. Verificar configuración
            System.out.println("\n[1] Verificando configuración...");
            System.out.println("  - Cliente ID: " + LicenciaConfig.CLIENTE_ID);
            System.out.println("  - Cliente Nombre: " + LicenciaConfig.CLIENTE_NOMBRE);

            // Inicializar SessionManager obteniendo la licencia del cliente
            try {
                LicenciaDAO licenciaDAO = new LicenciaDAO();
                Licencia licencia = licenciaDAO.findById(LicenciaConfig.CLIENTE_ID)
                        .orElseThrow(() -> new RuntimeException("No se encontró licencia para cliente_id: " + LicenciaConfig.CLIENTE_ID));

                SessionManager.getInstance().login(licencia);
                System.out.println("  ✓ SessionManager configurado");
            } catch (Exception e) {
                System.err.println("  ✗ Error configurando SessionManager: " + e.getMessage());
                System.err.println("  → Se requiere una licencia válida en Supabase para ejecutar el test");
                throw e;
            }

            // 2. Verificar SQLite
            System.out.println("\n[2] Verificando SQLite...");
            var conn = SqliteDatabase.get();
            System.out.println("  ✓ SQLite conectado: " + SqliteDatabase.getDbPath());

            // 3. Ejecutar sincronización Cloud → Local
            System.out.println("\n[3] Ejecutando sincronización Cloud → Local...");
            SyncService syncService = new SyncService();
            SyncResult result = syncService.syncFromCloud();

            // 4. Mostrar resultados
            System.out.println("\n========================================");
            System.out.println("RESULTADOS DE SINCRONIZACIÓN");
            System.out.println("========================================");
            System.out.println("Estado: " + (result.isSuccess() ? "✓ EXITOSO" : "✗ FALLIDO"));
            System.out.println("Dirección: " + result.getDirection());
            System.out.println("Mensaje: " + result.getMessage());
            System.out.println("\nEstadísticas:");
            System.out.println(result.getStats().toString());

            if (!result.getErrors().isEmpty()) {
                System.out.println("\nErrores (" + result.getErrors().size() + "):");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
            }

            System.out.println("\n========================================");
            if (result.isSuccess()) {
                System.out.println("✓ TEST COMPLETADO EXITOSAMENTE");
            } else {
                System.out.println("✗ TEST FALLIDO");
                System.exit(1);
            }
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("✗ ERROR EN EL TEST");
            System.err.println("========================================");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
