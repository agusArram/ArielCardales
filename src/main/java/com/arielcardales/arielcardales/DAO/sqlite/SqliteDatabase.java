package com.arielcardales.arielcardales.DAO.sqlite;

import com.arielcardales.arielcardales.Licencia.LicenciaConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestión de conexión a la base de datos SQLite local (backup)
 * Cada cliente tiene su propio archivo de base de datos aislado
 */
public final class SqliteDatabase {

    private static final String DB_DIR_NAME = ".appinventario";
    private static Path dbPath;
    private static Connection connection;
    private static boolean initialized = false;

    static {
        try {
            initDatabase();
        } catch (Exception e) {
            System.err.println("Error inicializando SQLite Database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private SqliteDatabase() {}

    /**
     * Inicializa la base de datos SQLite
     * Crea el directorio y archivo si no existen
     */
    private static void initDatabase() throws SQLException, IOException {
        // Obtener directorio home del usuario
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, DB_DIR_NAME);

        // Crear directorio si no existe
        if (!Files.exists(appDir)) {
            Files.createDirectories(appDir);
            System.out.println("✓ Directorio de backup creado: " + appDir);
        }

        // Construir nombre del archivo de DB basado en cliente_id
        String clienteId = LicenciaConfig.CLIENTE_ID;
        String dbFileName = "backup_" + clienteId + ".db";
        dbPath = appDir.resolve(dbFileName);

        boolean isNewDatabase = !Files.exists(dbPath);

        // Conectar a SQLite (crea el archivo si no existe)
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connection = DriverManager.getConnection(jdbcUrl);

        // Habilitar foreign keys
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        System.out.println("✓ Base de datos SQLite conectada: " + dbPath);

        // Si es nueva, inicializar el esquema
        if (isNewDatabase) {
            System.out.println("→ Base de datos nueva detectada, inicializando esquema...");
            initSchema();
            initialized = true;
        } else {
            initialized = true;
        }
    }

    /**
     * Obtiene una conexión a la base de datos SQLite
     * @return Connection a SQLite
     * @throws SQLException si hay error de conexión
     */
    public static Connection get() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                initDatabase();
            } catch (IOException e) {
                throw new SQLException("Error inicializando base de datos SQLite", e);
            }
        }
        return connection;
    }

    /**
     * Inicializa el esquema de la base de datos ejecutando el script SQL
     */
    public static void initSchema() throws SQLException {
        System.out.println("→ Inicializando esquema SQLite...");

        try (var inputStream = SqliteDatabase.class.getResourceAsStream("/sqlite_schema.sql")) {
            if (inputStream == null) {
                throw new SQLException("No se encontró el archivo sqlite_schema.sql en resources");
            }

            // Leer todo el contenido del archivo
            String schemaSQL = new String(inputStream.readAllBytes());

            Connection conn = get();
            try (Statement stmt = conn.createStatement()) {
                int executed = 0;

                // Procesar el script línea por línea construyendo statements completos
                StringBuilder currentStatement = new StringBuilder();
                String[] lines = schemaSQL.split("\n");

                for (String line : lines) {
                    String trimmedLine = line.trim();

                    // Ignorar líneas vacías y comentarios
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                        continue;
                    }

                    // Agregar línea al statement actual
                    currentStatement.append(line).append("\n");

                    // Si la línea termina en ;, ejecutar el statement
                    if (trimmedLine.endsWith(";")) {
                        String sqlToExecute = currentStatement.toString().trim();
                        // Remover el ; final
                        if (sqlToExecute.endsWith(";")) {
                            sqlToExecute = sqlToExecute.substring(0, sqlToExecute.length() - 1);
                        }

                        if (!sqlToExecute.isEmpty()) {
                            try {
                                stmt.execute(sqlToExecute);
                                executed++;
                            } catch (SQLException e) {
                                // Los errores de "table already exists" son esperados
                                if (!e.getMessage().contains("already exists")) {
                                    System.err.println("⚠ Error ejecutando statement SQL: " + e.getMessage());
                                    System.err.println("  SQL: " + sqlToExecute.substring(0, Math.min(100, sqlToExecute.length())) + "...");
                                }
                            }
                        }

                        // Limpiar para el siguiente statement
                        currentStatement.setLength(0);
                    }
                }

                System.out.println("✓ Esquema SQLite inicializado (" + executed + " statements ejecutados)");
            }

        } catch (IOException e) {
            throw new SQLException("Error leyendo archivo de esquema", e);
        }
    }

    /**
     * Cierra la conexión a la base de datos
     */
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Conexión SQLite cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error cerrando conexión SQLite: " + e.getMessage());
        }
    }

    /**
     * Obtiene la ruta del archivo de base de datos
     * @return Path del archivo .db
     */
    public static Path getDbPath() {
        return dbPath;
    }

    /**
     * Verifica si la base de datos está inicializada
     * @return true si está inicializada
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Obtiene el ID del cliente actual
     * @return cliente_id
     */
    public static String getClienteId() {
        return LicenciaConfig.CLIENTE_ID;
    }
}
