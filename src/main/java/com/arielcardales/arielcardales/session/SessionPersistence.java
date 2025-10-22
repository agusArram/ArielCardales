package com.arielcardales.arielcardales.session;

import com.arielcardales.arielcardales.DAO.AutenticacionDAO;
import com.arielcardales.arielcardales.Licencia.Licencia;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

/**
 * Gestor de persistencia de sesión
 * Guarda la sesión del usuario en disco para no tener que iniciar sesión constantemente
 *
 * Seguridad:
 * - Guarda el email del usuario (no la contraseña)
 * - Guarda timestamp de último login
 * - Revalida con la BD cada vez que se carga
 * - Se invalida automáticamente después de 60 días sin uso
 */
public class SessionPersistence {

    private static final String SESSION_FILE = "session.dat";
    private static final String SESSION_DIR = ".appinventario";
    private static final int DIAS_VALIDEZ = 60; // 60 días sin usar = re-login

    // ============================================================================
    // GUARDAR SESIÓN
    // ============================================================================

    /**
     * Guarda la sesión actual en disco para persistencia
     *
     * @param licencia Licencia del usuario autenticado
     * @return true si se guardó exitosamente
     */
    public static boolean guardarSesion(Licencia licencia) {
        if (licencia == null) {
            log("⚠️ No se puede guardar sesión null");
            return false;
        }

        try {
            // Crear directorio si no existe
            Path dirPath = getSessionDirectory();
            Files.createDirectories(dirPath);

            // Crear properties con datos de sesión
            Properties props = new Properties();
            props.setProperty("email", licencia.getEmail());
            props.setProperty("cliente_id", licencia.getClienteId());
            props.setProperty("nombre", licencia.getNombre());
            props.setProperty("ultimo_acceso", LocalDateTime.now().toString());
            props.setProperty("plan", licencia.getPlan().name());

            // Guardar en archivo (ofuscado con Base64)
            Path sessionFile = dirPath.resolve(SESSION_FILE);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 OutputStream fos = Files.newOutputStream(sessionFile)) {

                props.store(baos, "Session Data");
                String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
                fos.write(encoded.getBytes());
            }

            log("✅ Sesión guardada: " + licencia.getEmail());
            return true;

        } catch (IOException e) {
            log("❌ Error guardando sesión: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ============================================================================
    // CARGAR SESIÓN
    // ============================================================================

    /**
     * Intenta cargar una sesión guardada previamente
     *
     * @return Optional con la Licencia si existe sesión válida
     */
    public static Optional<Licencia> cargarSesion() {
        try {
            Path sessionFile = getSessionDirectory().resolve(SESSION_FILE);

            // Verificar si existe el archivo
            if (!Files.exists(sessionFile)) {
                log("ℹ️ No hay sesión guardada");
                return Optional.empty();
            }

            // Leer y decodificar
            String encoded = Files.readString(sessionFile);
            byte[] decoded = Base64.getDecoder().decode(encoded);

            Properties props = new Properties();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(decoded)) {
                props.load(bais);
            }

            // Obtener datos de sesión
            String email = props.getProperty("email");
            String ultimoAccesoStr = props.getProperty("ultimo_acceso");

            if (email == null || ultimoAccesoStr == null) {
                log("⚠️ Sesión guardada incompleta");
                borrarSesion();
                return Optional.empty();
            }

            // Verificar que no haya expirado (60 días)
            LocalDateTime ultimoAcceso = LocalDateTime.parse(ultimoAccesoStr);
            long diasDesdeUltimoAcceso = ChronoUnit.DAYS.between(ultimoAcceso, LocalDateTime.now());

            if (diasDesdeUltimoAcceso > DIAS_VALIDEZ) {
                log("⚠️ Sesión expirada (último acceso hace " + diasDesdeUltimoAcceso + " días)");
                borrarSesion();
                return Optional.empty();
            }

            // Revalidar con la base de datos (puede que la licencia haya sido suspendida)
            log("🔄 Revalidando sesión: " + email);
            AutenticacionDAO dao = new AutenticacionDAO();
            Optional<Licencia> licenciaOpt = dao.loginPorEmail(email);

            if (licenciaOpt.isEmpty()) {
                log("❌ Sesión inválida (usuario no encontrado o suspendido)");
                borrarSesion();
                return Optional.empty();
            }

            Licencia licencia = licenciaOpt.get();

            // Verificar que la licencia esté activa y vigente
            if (!licencia.isValida(java.time.LocalDate.now())) {
                log("❌ Sesión inválida (licencia expirada o suspendida)");
                borrarSesion();
                return Optional.empty();
            }

            // Actualizar último acceso
            props.setProperty("ultimo_acceso", LocalDateTime.now().toString());
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 OutputStream fos = Files.newOutputStream(sessionFile)) {
                props.store(baos, "Session Data");
                String newEncoded = Base64.getEncoder().encodeToString(baos.toByteArray());
                fos.write(newEncoded.getBytes());
            }

            log("✅ Sesión cargada: " + licencia.getNombre() + " (" + licencia.getPlan() + ")");
            return Optional.of(licencia);

        } catch (Exception e) {
            log("❌ Error cargando sesión: " + e.getMessage());
            e.printStackTrace();
            borrarSesion(); // Limpiar sesión corrupta
            return Optional.empty();
        }
    }

    // ============================================================================
    // BORRAR SESIÓN
    // ============================================================================

    /**
     * Borra la sesión guardada en disco
     *
     * @return true si se borró exitosamente
     */
    public static boolean borrarSesion() {
        try {
            Path sessionFile = getSessionDirectory().resolve(SESSION_FILE);

            if (Files.exists(sessionFile)) {
                Files.delete(sessionFile);
                log("🗑️ Sesión borrada del disco");
                return true;
            }

            return true; // No hay nada que borrar

        } catch (IOException e) {
            log("❌ Error borrando sesión: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si existe una sesión guardada
     *
     * @return true si existe un archivo de sesión
     */
    public static boolean existeSesionGuardada() {
        Path sessionFile = getSessionDirectory().resolve(SESSION_FILE);
        return Files.exists(sessionFile);
    }

    // ============================================================================
    // UTILIDADES
    // ============================================================================

    /**
     * Obtiene el directorio de sesión (crea si no existe)
     */
    private static Path getSessionDirectory() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, SESSION_DIR);
    }

    /**
     * Log de eventos
     */
    private static void log(String mensaje) {
        String timestamp = LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        System.out.println("[SessionPersistence " + timestamp + "] " + mensaje);
    }
}
