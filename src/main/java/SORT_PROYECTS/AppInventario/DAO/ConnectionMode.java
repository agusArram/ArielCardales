package SORT_PROYECTS.AppInventario.DAO;

import SORT_PROYECTS.AppInventario.DAO.sqlite.SqliteDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

/**
 * Gestor del modo de conexión (Online/Offline)
 * Maneja el fallback automático de Supabase a SQLite
 */
public class ConnectionMode {

    private static final AtomicBoolean isOnline = new AtomicBoolean(true);
    private static final AtomicBoolean manualOfflineMode = new AtomicBoolean(false);
    private static final AtomicReference<String> lastError = new AtomicReference<>("");

    private static final Preferences prefs = Preferences.userNodeForPackage(ConnectionMode.class);
    private static final String PREF_OFFLINE_MODE = "offline_mode";

    // Estadísticas
    private static int connectionAttempts = 0;
    private static int failedAttempts = 0;

    static {
        // Cargar preferencia de modo offline
        manualOfflineMode.set(prefs.getBoolean(PREF_OFFLINE_MODE, false));

        if (manualOfflineMode.get()) {
            isOnline.set(false);
            System.out.println("⚠ Iniciando en modo OFFLINE (configuración guardada)");
        }
    }

    /**
     * Obtiene una conexión, usando fallback automático si es necesario
     */
    public static Connection getConnection() throws SQLException {
        connectionAttempts++;

        // Si estamos en modo offline manual, usar SQLite directamente
        if (manualOfflineMode.get()) {
            isOnline.set(false);
            return SqliteDatabase.get();
        }

        // Intentar conectar a Supabase si estamos online
        if (isOnline.get()) {
            try {
                Connection conn = Database.get();

                // Verificar que la conexión es válida
                if (conn != null && conn.isValid(5)) {
                    return conn;
                }
            } catch (SQLException e) {
                // Conexión falló - activar modo offline
                handleConnectionFailure(e);
            }
        }

        // Fallback a SQLite
        failedAttempts++;
        isOnline.set(false);
        return SqliteDatabase.get();
    }

    /**
     * Maneja un fallo de conexión a Supabase
     */
    private static void handleConnectionFailure(SQLException e) {
        isOnline.set(false);
        lastError.set(e.getMessage());

        System.err.println("❌ Conexión a Supabase falló - Cambiando a modo OFFLINE");
        System.err.println("   Error: " + e.getMessage());
        System.err.println("   Usando SQLite local como respaldo");
    }

    /**
     * Intenta reconectar a Supabase
     */
    public static boolean tryReconnect() {
        if (manualOfflineMode.get()) {
            System.out.println("⚠ Modo offline manual activado - no se puede reconectar");
            return false;
        }

        try {
            Connection conn = Database.get();
            if (conn != null && conn.isValid(5)) {
                isOnline.set(true);
                lastError.set("");
                System.out.println("✓ Reconexión exitosa a Supabase - Modo ONLINE");
                conn.close();
                return true;
            }
        } catch (SQLException e) {
            lastError.set(e.getMessage());
        }

        return false;
    }

    /**
     * Activa el modo offline manual
     */
    public static void setManualOfflineMode(boolean offline) {
        manualOfflineMode.set(offline);
        isOnline.set(!offline);
        prefs.putBoolean(PREF_OFFLINE_MODE, offline);

        if (offline) {
            System.out.println("⚠ Modo OFFLINE activado manualmente");
        } else {
            System.out.println("✓ Modo OFFLINE desactivado - intentando reconectar...");
            tryReconnect();
        }
    }

    // ========================================
    // GETTERS
    // ========================================

    public static boolean isOnline() {
        return isOnline.get();
    }

    public static boolean isOffline() {
        return !isOnline.get();
    }

    public static boolean isManualOfflineMode() {
        return manualOfflineMode.get();
    }

    public static String getLastError() {
        return lastError.get();
    }

    public static int getConnectionAttempts() {
        return connectionAttempts;
    }

    public static int getFailedAttempts() {
        return failedAttempts;
    }

    public static String getStatusMessage() {
        if (isOnline.get()) {
            return "🟢 Online - Conectado a Supabase";
        } else if (manualOfflineMode.get()) {
            return "🔴 Offline (Manual) - Usando backup local";
        } else {
            return "🟡 Offline (Auto) - Usando backup local";
        }
    }

    /**
     * Resetea las estadísticas (útil para testing)
     */
    public static void resetStats() {
        connectionAttempts = 0;
        failedAttempts = 0;
    }
}
