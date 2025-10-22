package com.arielcardales.arielcardales.Licencia;

/**
 * Configuración del sistema de licencias
 */
public class LicenciaConfig {

    // ============================================================================
    // URL DEL ARCHIVO JSON DE LICENCIAS (GitHub RAW)
    // ============================================================================
    // IMPORTANTE: Cambiar esta URL por tu repositorio real
    public static final String LICENCIAS_JSON_URL =
            "https://raw.githubusercontent.com/agusArram/ArielCardales/refs/heads/Dev/licencias.json";

    // ============================================================================
    // API DE FECHA EXTERNA (para evitar manipulación de reloj del sistema)
    // ============================================================================
    // worldtimeapi.org es gratuito y confiable
    public static final String TIME_API_URL =
            "http://worldtimeapi.org/api/timezone/America/Argentina/Buenos_Aires";

    // Alternativas en caso de fallo
    public static final String TIME_API_BACKUP_1 =
            "https://timeapi.io/api/Time/current/zone?timeZone=America/Argentina/Buenos_Aires";

    public static final String TIME_API_BACKUP_2 =
            "http://worldclockapi.com/api/json/est/now";

    // ============================================================================
    // CONFIGURACIÓN DE SEGURIDAD
    // ============================================================================

    /**
     * Clave secreta para generar hash de verificación (CAMBIAR en producción)
     * Esta clave debe ser la misma que uses para firmar el JSON
     */
    public static final String SECRET_KEY = "ArielCardales2025_SecretKey_ChangeThis!";

    /**
     * Máximo de días que puede funcionar sin validación online
     * Después de este período, OBLIGA a conectarse para verificar
     */
    public static final int MAX_DIAS_SIN_VALIDACION = 7;

    /**
     * Timeout de conexión HTTP (milisegundos)
     */
    public static final int TIMEOUT_MS = 5000;

    // ============================================================================
    // ARCHIVOS LOCALES
    // ============================================================================

    /**
     * Directorio de configuración en home del usuario
     */
    public static final String CONFIG_DIR = System.getProperty("user.home") + "/.arielcardales";

    /**
     * Archivo local de licencia (cache encriptado)
     */
    public static final String LICENCIA_LOCAL_FILE = CONFIG_DIR + "/licencia.dat";

    /**
     * Archivo de log de validaciones
     */
    public static final String VALIDACION_LOG_FILE = CONFIG_DIR + "/validaciones.log";

    // ============================================================================
    // CONFIGURACIÓN DE CLIENTE
    // ============================================================================

    /**
     * ID único del cliente (se carga desde cliente.properties)
     * Puede ser: DNI, CUIT, email, o un UUID generado
     */
    public static String CLIENTE_ID = "DEMO_CLIENT";

    /**
     * Nombre del cliente (se carga desde cliente.properties)
     */
    public static String CLIENTE_NOMBRE = "Cliente Demo";

    /**
     * Email del cliente (se carga desde cliente.properties)
     */
    public static String CLIENTE_EMAIL = "demo@ejemplo.com";

    // Bloque estático para cargar configuración al iniciar
    static {
        cargarConfiguracionCliente();
    }

    // ============================================================================
    // MÉTODOS DE UTILIDAD
    // ============================================================================

    /**
     * Carga la configuración del cliente desde cliente.properties
     */
    private static void cargarConfiguracionCliente() {
        try {
            java.io.InputStream input = LicenciaConfig.class
                    .getResourceAsStream("/cliente.properties");

            if (input != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(input);

                String id = props.getProperty("cliente.id");
                String nombre = props.getProperty("cliente.nombre");
                String email = props.getProperty("cliente.email");

                if (id != null && !id.trim().isEmpty()) {
                    CLIENTE_ID = id.trim();
                }
                if (nombre != null && !nombre.trim().isEmpty()) {
                    CLIENTE_NOMBRE = nombre.trim();
                }
                if (email != null && !email.trim().isEmpty()) {
                    CLIENTE_EMAIL = email.trim();
                }

                input.close();

                System.out.println("✓ Configuración de cliente cargada:");
                System.out.println("  - ID: " + CLIENTE_ID);
                System.out.println("  - Nombre: " + CLIENTE_NOMBRE);
                System.out.println("  - Email: " + CLIENTE_EMAIL);

            } else {
                System.err.println("⚠ No se encontró cliente.properties, usando valores por defecto");
            }

        } catch (Exception e) {
            System.err.println("⚠ Error al cargar cliente.properties: " + e.getMessage());
            System.err.println("  Usando configuración por defecto (DEMO_CLIENT)");
        }
    }

    /**
     * Configura el ID del cliente manualmente (opcional, sobrescribe properties)
     */
    public static void setClienteId(String clienteId) {
        CLIENTE_ID = clienteId;
    }

    /**
     * Configura el nombre del cliente manualmente (opcional, sobrescribe properties)
     */
    public static void setClienteNombre(String nombre) {
        CLIENTE_NOMBRE = nombre;
    }

    /**
     * Verifica si el directorio de configuración existe, si no lo crea
     */
    public static void inicializarDirectorios() {
        java.io.File dir = new java.io.File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
