package SORT_PROYECTS.AppInventario.session;

import SORT_PROYECTS.AppInventario.DAO.AutenticacionDAO;
import SORT_PROYECTS.AppInventario.Licencia.Licencia;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitor de licencias en tiempo real
 * Verifica periódicamente el estado de la licencia en la base de datos
 * para detectar suspensiones mientras la aplicación está en ejecución
 */
public class LicenseMonitor {

    // ============================================================================
    // CONFIGURACIÓN (EDITABLE)
    // ============================================================================

    /**
     * Intervalo de verificación en MINUTOS
     * Cambia este valor para ajustar la frecuencia de verificación
     */
    private static final int INTERVALO_VERIFICACION_MINUTOS = 30;
    /**
     * Delay inicial antes de la primera verificación (en minutos)
     */
    private static final int DELAY_INICIAL_MINUTOS = 5;

    // ============================================================================
    // SINGLETON
    // ============================================================================

    private static LicenseMonitor instance;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private int verificacionesRealizadas = 0;  // Contador para debugging

    private LicenseMonitor() {
        // Constructor privado para singleton
    }

    public static synchronized LicenseMonitor getInstance() {
        if (instance == null) {
            instance = new LicenseMonitor();
        }
        return instance;
    }

    // ============================================================================
    // CONTROL DEL MONITOR
    // ============================================================================

    /**
     * Inicia el monitor de licencias
     * Ejecuta verificaciones periódicas en segundo plano
     */
    public synchronized void iniciar() {
        if (isRunning) {
            log("⚠️ Monitor ya está en ejecución");
            return;
        }

        if (!SessionManager.getInstance().isAutenticado()) {
            log("⚠️ No hay sesión activa - monitor no iniciado");
            return;
        }

        log("🚀 Iniciando monitor de licencias...");
        log("   Intervalo: " + INTERVALO_VERIFICACION_MINUTOS + " minutos");
        log("   Delay inicial: " + DELAY_INICIAL_MINUTOS + " minutos");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "LicenseMonitor-Thread");
            thread.setDaemon(true); // Thread daemon para que no impida el cierre de la app
            return thread;
        });

        // Programar verificaciones periódicas
        scheduler.scheduleAtFixedRate(
                this::verificarEstadoLicencia,
                DELAY_INICIAL_MINUTOS,           // Delay inicial
                INTERVALO_VERIFICACION_MINUTOS,  // Intervalo
                TimeUnit.MINUTES
        );

        isRunning = true;
        log("✅ Monitor iniciado exitosamente");
    }

    /**
     * Detiene el monitor de licencias
     */
    public synchronized void detener() {
        if (!isRunning) {
            return;
        }

        log("🛑 Deteniendo monitor de licencias...");

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        isRunning = false;
        log("✅ Monitor detenido");
    }

    // ============================================================================
    // VERIFICACIÓN DE ESTADO
    // ============================================================================

    /**
     * Verifica el estado actual de la licencia en la base de datos
     * Se ejecuta periódicamente en segundo plano
     */
    private void verificarEstadoLicencia() {
        verificacionesRealizadas++;
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log("🔍 INICIO VERIFICACIÓN #" + verificacionesRealizadas);
        log("   Hora: " + LocalDateTime.now());

        try {
            // Obtener email de la sesión actual
            SessionManager sessionManager = SessionManager.getInstance();

            if (!sessionManager.isAutenticado()) {
                log("⚠️ Sesión no autenticada - saltando verificación");
                log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return;
            }

            String email = sessionManager.getLicencia().getEmail();
            log("🔍 Verificando estado de licencia: " + email);

            // Consultar estado en DB (query ligera)
            AutenticacionDAO dao = new AutenticacionDAO();
            Licencia.EstadoLicencia estadoActual = dao.verificarEstado(email);

            if (estadoActual == null) {
                log("❌ No se pudo obtener el estado - usuario eliminado?");
                log("🚪 Cerrando aplicación - usuario no encontrado");
                log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                mostrarAlertaYCerrar(
                        "Cuenta no encontrada",
                        "Tu cuenta de usuario ya no existe en el sistema.\n\n" +
                        "La aplicación se cerrará."
                );
                return;
            }

            // Verificar si está suspendida
            if (estadoActual == Licencia.EstadoLicencia.SUSPENDIDO) {
                log("🚫 LICENCIA SUSPENDIDA DETECTADA - cerrando aplicación");
                log("🚪 Mostrando alerta y cerrando...");
                log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                mostrarAlertaYCerrar(
                        "Cuenta Suspendida",
                        "Tu cuenta ha sido suspendida.\n\n" +
                        "Por favor, contacta al administrador para más información.\n\n" +
                        "La aplicación se cerrará ahora."
                );
                return;
            }

            // Verificar si está expirada
            if (estadoActual == Licencia.EstadoLicencia.EXPIRADO) {
                log("⏰ LICENCIA EXPIRADA DETECTADA - cerrando aplicación");
                log("🚪 Mostrando alerta y cerrando...");
                log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                mostrarAlertaYCerrar(
                        "Licencia Expirada",
                        "Tu licencia ha expirado.\n\n" +
                        "Por favor, renueva tu suscripción para continuar usando la aplicación.\n\n" +
                        "La aplicación se cerrará ahora."
                );
                return;
            }

            log("✅ Estado verificado: " + estadoActual);
            log("✅ FIN VERIFICACIÓN #" + verificacionesRealizadas + " - TODO OK");
            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log("❌ Error en verificación periódica #" + verificacionesRealizadas + ": " + e.getMessage());
            e.printStackTrace();
            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    /**
     * Muestra una alerta modal y cierra la aplicación
     *
     * @param titulo Título del diálogo
     * @param mensaje Mensaje a mostrar
     */
    private void mostrarAlertaYCerrar(String titulo, String mensaje) {
        // Ejecutar en el thread de JavaFX
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.getButtonTypes().setAll(ButtonType.OK);

            // Mostrar y esperar a que el usuario cierre
            alert.showAndWait();

            // Cerrar la aplicación
            log("🚪 Cerrando aplicación por licencia inválida");

            // Cerrar sesión antes de salir
            SessionManager.getInstance().logout();
            SessionPersistence.borrarSesion();

            // Detener el monitor
            detener();

            // Salir de la aplicación
            Platform.exit();
            System.exit(0);
        });
    }

    // ============================================================================
    // UTILIDADES
    // ============================================================================

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Log con timestamp
     */
    private void log(String mensaje) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println("[LicenseMonitor " + timestamp + "] " + mensaje);
    }
}
