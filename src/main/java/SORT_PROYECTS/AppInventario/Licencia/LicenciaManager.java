package SORT_PROYECTS.AppInventario.Licencia;

import SORT_PROYECTS.AppInventario.DAO.LicenciaDAO;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Gestor de licencias simplificado con validación desde base de datos
 * Rápido y eficiente - una sola consulta SQL
 */
public class LicenciaManager {

    private static Licencia licenciaActual = null;
    private static final LicenciaDAO licenciaDAO = new LicenciaDAO();

    // ============================================================================
    // MÉTODOS PÚBLICOS PRINCIPALES
    // ============================================================================

    /**
     * Valida la licencia del cliente actual consultando la base de datos
     * Extremadamente rápido: una sola consulta SQL (<100ms)
     *
     * @return true si la licencia es válida
     */
    public static boolean validarLicencia() {
        try {
            // Obtener DNI del cliente desde configuración
            String dni = LicenciaConfig.CLIENTE_ID;

            if (dni == null || dni.trim().isEmpty()) {
                log("❌ No se configuró un DNI de cliente");
                return false;
            }

            // Buscar licencia en base de datos
            Optional<Licencia> licenciaOpt = licenciaDAO.findById(dni);

            if (licenciaOpt.isEmpty()) {
                log("❌ No existe licencia para DNI: " + dni);
                return false;
            }

            licenciaActual = licenciaOpt.get();

            // Validar estado y fecha de expiración
            LocalDate hoy = LocalDate.now();
            boolean valida = licenciaActual.isValida(hoy);

            if (valida) {
                log("✅ Licencia válida - " + licenciaActual.getPlan() + " - DNI: " + dni);
            } else {
                log("❌ Licencia inválida o expirada - DNI: " + dni);
            }

            return valida;

        } catch (Exception e) {
            log("❌ Error validando licencia: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene la licencia actual (después de validar)
     */
    public static Licencia getLicencia() {
        return licenciaActual;
    }

    /**
     * Obtiene días restantes antes de expiración
     */
    public static long getDiasRestantes() {
        if (licenciaActual == null) return 0;

        LocalDate hoy = LocalDate.now();
        LocalDate expiracion = licenciaActual.getFechaExpiracion();

        return java.time.temporal.ChronoUnit.DAYS.between(hoy, expiracion);
    }

    /**
     * Obtiene mensaje de estado de la licencia para mostrar al usuario
     */
    public static String getMensajeEstado() {
        if (licenciaActual == null) {
            return "❌ Licencia no válida";
        }

        long diasRestantes = getDiasRestantes();
        String planNombre = licenciaActual.getPlan().name();

        if (diasRestantes < 0) {
            return "❌ Licencia expirada";
        } else if (diasRestantes <= 7) {
            return "⚠ Licencia " + planNombre + " - " + diasRestantes + " días restantes";
        } else {
            return "✅ Licencia " + planNombre + " activa";
        }
    }

    /**
     * Fuerza una revalidación (recarga desde la base de datos)
     */
    public static boolean revalidar() {
        return validarLicencia();
    }

    // ============================================================================
    // LOGGING
    // ============================================================================

    /**
     * Registra eventos en consola (simplificado)
     */
    private static void log(String mensaje) {
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        System.out.println("[" + timestamp + "] " + mensaje);
    }
}
