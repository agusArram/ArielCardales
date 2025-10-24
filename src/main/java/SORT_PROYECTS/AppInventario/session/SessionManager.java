package SORT_PROYECTS.AppInventario.session;

import SORT_PROYECTS.AppInventario.Licencia.Licencia;

/**
 * Gestor de sesión global para sistema multi-tenant
 *
 * Responsabilidades:
 * - Mantener la sesión del usuario autenticado
 * - Proveer el cliente_id para aislamiento de datos
 * - Gestionar login/logout
 *
 * IMPORTANTE: Este es un singleton thread-safe que mantiene el estado global de la sesión
 */
public class SessionManager {

    // Singleton instance
    private static SessionManager instance;

    // Estado de la sesión
    private String clienteId;
    private Licencia licenciaActual;
    private boolean autenticado;

    // Constructor privado (singleton)
    private SessionManager() {
        this.autenticado = false;
    }

    /**
     * Obtiene la instancia única del SessionManager
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ============================================================================
    // MÉTODOS DE AUTENTICACIÓN
    // ============================================================================

    /**
     * Inicia sesión con los datos del usuario autenticado
     *
     * @param licencia Licencia del usuario autenticado
     */
    public synchronized void login(Licencia licencia) {
        if (licencia == null) {
            throw new IllegalArgumentException("La licencia no puede ser null");
        }

        this.clienteId = licencia.getClienteId();
        this.licenciaActual = licencia;
        this.autenticado = true;

        log("✅ Usuario autenticado: " + licencia.getNombre() + " (" + licencia.getEmail() + ")");
        log("📋 Plan: " + licencia.getPlan() + " | Estado: " + licencia.getEstado());
    }

    /**
     * Cierra la sesión actual
     */
    public synchronized void logout() {
        if (autenticado) {
            log("👋 Cerrando sesión: " + licenciaActual.getNombre());
        }

        this.clienteId = null;
        this.licenciaActual = null;
        this.autenticado = false;
    }

    // ============================================================================
    // GETTERS
    // ============================================================================

    /**
     * Obtiene el cliente_id del usuario actual
     * CRÍTICO: Este método se usa en todos los DAOs para filtrar datos
     *
     * @return cliente_id del usuario autenticado
     * @throws IllegalStateException si no hay sesión activa
     */
    public String getClienteId() {
        if (!autenticado) {
            throw new IllegalStateException("No hay sesión activa. Usuario debe estar autenticado.");
        }
        return clienteId;
    }

    /**
     * Obtiene el cliente_id de forma segura (retorna null si no hay sesión)
     *
     * @return cliente_id o null si no hay sesión
     */
    public String getClienteIdSafe() {
        return autenticado ? clienteId : null;
    }

    /**
     * Obtiene la licencia completa del usuario actual
     *
     * @return Licencia del usuario autenticado
     * @throws IllegalStateException si no hay sesión activa
     */
    public Licencia getLicencia() {
        if (!autenticado) {
            throw new IllegalStateException("No hay sesión activa. Usuario debe estar autenticado.");
        }
        return licenciaActual;
    }

    /**
     * Obtiene la licencia de forma segura (retorna null si no hay sesión)
     *
     * @return Licencia o null si no hay sesión
     */
    public Licencia getLicenciaSafe() {
        return autenticado ? licenciaActual : null;
    }

    /**
     * Verifica si hay un usuario autenticado
     *
     * @return true si hay sesión activa
     */
    public boolean isAutenticado() {
        return autenticado;
    }

    /**
     * Obtiene el nombre del usuario actual
     *
     * @return Nombre del usuario o "Invitado" si no hay sesión
     */
    public String getNombreUsuario() {
        return autenticado ? licenciaActual.getNombre() : "Invitado";
    }

    /**
     * Obtiene el email del usuario actual
     *
     * @return Email del usuario o null si no hay sesión
     */
    public String getEmail() {
        return autenticado ? licenciaActual.getEmail() : null;
    }

    /**
     * Obtiene el plan del usuario actual
     *
     * @return Plan de licencia o null si no hay sesión
     */
    public Licencia.PlanLicencia getPlan() {
        return autenticado ? licenciaActual.getPlan() : null;
    }

    // ============================================================================
    // VALIDACIÓN DE PERMISOS
    // ============================================================================

    /**
     * Verifica si el usuario actual puede acceder a una funcionalidad
     *
     * @param funcionalidad Nombre de la funcionalidad a validar
     * @return true si tiene acceso
     */
    public boolean permiteAcceso(String funcionalidad) {
        if (!autenticado) {
            return false;
        }
        return licenciaActual.permiteAcceso(funcionalidad);
    }

    /**
     * Verifica si la licencia del usuario está vigente
     *
     * @return true si la licencia está vigente (no expirada)
     */
    public boolean licenciaVigente() {
        if (!autenticado) {
            return false;
        }
        return licenciaActual.isValida(java.time.LocalDate.now());
    }

    /**
     * Obtiene días restantes hasta expiración
     *
     * @return Días restantes o 0 si no hay sesión
     */
    public long getDiasRestantes() {
        if (!autenticado) {
            return 0;
        }

        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate expiracion = licenciaActual.getFechaExpiracion();

        return java.time.temporal.ChronoUnit.DAYS.between(hoy, expiracion);
    }

    // ============================================================================
    // UTILIDADES
    // ============================================================================

    /**
     * Registra eventos en consola
     */
    private void log(String mensaje) {
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        System.out.println("[SessionManager " + timestamp + "] " + mensaje);
    }

    /**
     * Obtiene información resumida de la sesión para debugging
     */
    public String getSessionInfo() {
        if (!autenticado) {
            return "Sesión: No autenticado";
        }

        return String.format(
            "Sesión: %s (%s) | Plan: %s | Días restantes: %d | cliente_id: %s",
            licenciaActual.getNombre(),
            licenciaActual.getEmail(),
            licenciaActual.getPlan(),
            getDiasRestantes(),
            clienteId
        );
    }

    /**
     * Resetea la sesión (útil para testing)
     * NO USAR EN PRODUCCIÓN
     */
    @Deprecated
    public synchronized void reset() {
        logout();
        instance = null;
    }
}
