package SORT_PROYECTS.AppInventario.DAO;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import SORT_PROYECTS.AppInventario.DAO.sqlite.SqliteDatabase;

import java.sql.Connection;
import java.sql.SQLException;

public final class Database {
    //tengo que terminar de entender esto, se como se conecta la DB normal pero no entiendo a detalle con este metodo
    private static volatile HikariDataSource DS;
    private static volatile boolean isOnline = true;
    private static volatile boolean useAutoFallback = true;
    private static volatile boolean poolInitialized = false;
    private static final Object LOCK = new Object();

    // NO inicializar el pool en static block - hacerlo lazy para manejar errores de conexión

    private static String getEnv(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }

    private Database() {}

    /**
     * Inicializa el pool de HikariCP de forma lazy (solo cuando se necesita)
     * Maneja errores de conexión de forma elegante
     */
    private static void initializePool() {
        if (poolInitialized) {
            return;
        }

        synchronized (LOCK) {
            if (poolInitialized) {
                return;
            }

            try {
                HikariConfig cfg = new HikariConfig();

                String url = getEnv("PG_URL",
                        "jdbc:postgresql://aws-1-us-east-2.pooler.supabase.com:5432/postgres" +
                                "?sslmode=require&preferQueryMode=simple&reWriteBatchedInserts=true");
                String user = getEnv("PG_USER", "postgres.gybuxvjuhqhjmjmwkwyb");
                String pass = getEnv("PG_PASSWORD", "r$t13XR$^*R!U!@w");

                cfg.setJdbcUrl(url);
                cfg.setUsername(user);
                cfg.setPassword(pass);

                // Pool & rendimiento
                int maxPool = Integer.parseInt(getEnv("PG_POOL_SIZE", "5"));
                cfg.setMaximumPoolSize(maxPool);
                cfg.setMinimumIdle(Math.min(1, maxPool));
                cfg.setAutoCommit(true);

                // Timeouts cortos para detección rápida de fallo
                cfg.setConnectionTimeout(5_000);
                cfg.setIdleTimeout(60_000);
                cfg.setMaxLifetime(30 * 60_000);

                // CRÍTICO: No verificar conexión al inicializar el pool
                cfg.setInitializationFailTimeout(-1);  // No fallar si no puede conectar

                DS = new HikariDataSource(cfg);
                poolInitialized = true;
                System.out.println("✓ Pool de conexiones Supabase inicializado");

            } catch (Exception e) {
                System.err.println("⚠ No se pudo inicializar pool de Supabase: " + e.getMessage());
                System.err.println("  Usando modo OFFLINE desde el inicio");
                isOnline = false;
                poolInitialized = true;  // Marcamos como inicializado para no reintentar
            }
        }
    }

    /**
     * Obtiene conexión a Supabase SIN fallback
     * Lanza SQLException si falla
     */
    public static Connection get() throws SQLException {
        initializePool();

        if (DS == null) {
            throw new SQLException("Pool de conexiones no disponible");
        }

        Connection conn = DS.getConnection();

        // Si logramos conectar, marcamos como online
        if (conn != null && conn.isValid(2)) {
            isOnline = true;
        }

        return conn;
    }

    /**
     * Obtiene conexión con fallback automático a SQLite si Supabase falla
     * Este es el método que deberían usar los DAOs para trabajo offline
     */
    public static Connection getWithFallback() throws SQLException {
        if (!useAutoFallback) {
            return get();  // Sin fallback, usar solo Supabase
        }

        // Inicializar pool si no está inicializado
        initializePool();

        // Intentar Supabase si estamos online y el pool está disponible
        if (isOnline && DS != null) {
            try {
                Connection conn = DS.getConnection();

                // Verificar que la conexión es válida
                if (conn != null && conn.isValid(2)) {
                    isOnline = true;
                    return conn;
                }
            } catch (SQLException e) {
                // Conexión falló - activar modo offline
                handleConnectionFailure(e);
            }
        }

        // Fallback a SQLite
        isOnline = false;
        System.out.println("🔄 Usando SQLite local (modo offline)");
        return SqliteDatabase.get();
    }

    /**
     * Maneja un fallo de conexión a Supabase
     */
    private static void handleConnectionFailure(SQLException e) {
        if (isOnline) {  // Solo loguear la primera vez que falla
            isOnline = false;
            System.err.println("❌ Conexión a Supabase falló - Cambiando a modo OFFLINE");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("   Usando SQLite local como respaldo");
        }
    }

    /**
     * Intenta reconectar a Supabase
     */
    public static boolean tryReconnect() {
        initializePool();

        if (DS == null) {
            System.err.println("⚠ Pool no disponible - no se puede reconectar");
            return false;
        }

        try {
            Connection conn = DS.getConnection();
            if (conn != null && conn.isValid(2)) {
                isOnline = true;
                System.out.println("✓ Reconexión exitosa a Supabase - Modo ONLINE");
                conn.close();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Reconexión fallida: " + e.getMessage());
            isOnline = false;
        }

        return false;
    }

    // ========================================
    // GETTERS Y SETTERS
    // ========================================

    public static boolean isOnline() {
        return isOnline;
    }

    public static boolean isOffline() {
        return !isOnline;
    }

    public static void setAutoFallback(boolean enabled) {
        useAutoFallback = enabled;
        System.out.println((enabled ? "✓" : "⚠") + " Fallback automático " +
                (enabled ? "ACTIVADO" : "DESACTIVADO"));
    }

    public static String getStatusMessage() {
        if (!poolInitialized) {
            return "⚪ Inicializando...";
        }

        if (isOnline) {
            return "🟢 Online - Conectado a Supabase";
        } else {
            return "🔴 Offline - Usando backup SQLite local";
        }
    }

    /**
     * Obtiene el estado sin forzar inicialización del pool
     * Útil para UI que solo muestra el estado
     */
    public static String getStatusMessageSafe() {
        if (!poolInitialized) {
            // No inicializar el pool solo para mostrar estado
            return "⚪ No inicializado";
        }
        return getStatusMessage();
    }

    /**
     * Detecta si una conexión es a SQLite o PostgreSQL
     * Útil para ejecutar SQL específico de cada dialecto
     * @param conn Conexión a verificar
     * @return true si es SQLite, false si es PostgreSQL
     */
    public static boolean isSqlite(Connection conn) {
        if (conn == null) return false;
        try {
            String url = conn.getMetaData().getURL();
            return url != null && url.startsWith("jdbc:sqlite:");
        } catch (SQLException e) {
            return false;
        }
    }
}
