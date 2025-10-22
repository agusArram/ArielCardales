package com.arielcardales.arielcardales.Licencia;

/**
 * Configuración del sistema de licencias simplificado
 * Basado en base de datos - sin APIs externas ni cache
 */
public class LicenciaConfig {

    // ============================================================================
    // CONFIGURACIÓN DE CLIENTE
    // ============================================================================

    /**
     * ID único del cliente (DNI)
     * Se carga desde cliente.properties
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
                System.out.println("  - DNI: " + CLIENTE_ID);
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
}
