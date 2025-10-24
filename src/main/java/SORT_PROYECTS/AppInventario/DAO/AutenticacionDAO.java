package SORT_PROYECTS.AppInventario.DAO;

import SORT_PROYECTS.AppInventario.Licencia.Licencia;
import SORT_PROYECTS.AppInventario.Util.Mapper;
import SORT_PROYECTS.AppInventario.Util.PasswordUtil;

import java.sql.*;
import java.util.Optional;

/**
 * DAO para autenticación de usuarios
 * Maneja login, registro, y cambio de contraseña
 */
public class AutenticacionDAO {

    // ============================================================================
    // LOGIN
    // ============================================================================

    /**
     * Autentica un usuario por email y contraseña
     *
     * @param email Email del usuario
     * @param password Contraseña en texto plano
     * @return Optional con la Licencia si las credenciales son correctas
     */
    public Optional<Licencia> login(String email, String password) {
        // Validar parámetros
        if (email == null || email.trim().isEmpty()) {
            log("❌ Email vacío en intento de login");
            return Optional.empty();
        }

        if (password == null || password.trim().isEmpty()) {
            log("❌ Password vacío en intento de login");
            return Optional.empty();
        }

        String sql = """
            SELECT cliente_id, nombre, email, password_hash,
                   estado::text as estado, plan::text as plan,
                   fecha_expiracion, notas, createdAt, updatedAt
            FROM licencia
            WHERE LOWER(email) = LOWER(?)
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log("❌ Usuario no encontrado: " + email);
                    throw new InvalidCredentialsException("Email o contraseña incorrectos");
                }

                // Obtener hash de la DB
                String passwordHash = rs.getString("password_hash");

                if (passwordHash == null || passwordHash.trim().isEmpty()) {
                    log("❌ Usuario sin password configurado: " + email);
                    throw new InvalidCredentialsException("Usuario sin contraseña configurada");
                }

                // Verificar password con BCrypt
                if (!PasswordUtil.verifyPassword(password, passwordHash)) {
                    log("❌ Password incorrecto para: " + email);
                    throw new InvalidCredentialsException("Email o contraseña incorrectos");
                }

                // Password correcto, cargar licencia
                Licencia licencia = Mapper.getLicencia(rs);

                // Validar que la licencia esté activa y vigente
                if (licencia.getEstado() == Licencia.EstadoLicencia.SUSPENDIDO) {
                    log("❌ Cuenta suspendida: " + email);
                    throw new AccountSuspendedException(
                        "Tu cuenta ha sido suspendida.\n\n" +
                        "Por favor, contacta al administrador para más información."
                    );
                }

                if (licencia.getEstado() == Licencia.EstadoLicencia.EXPIRADO) {
                    log("❌ Cuenta expirada: " + email);
                    throw new AccountExpiredException(
                        "Tu licencia ha expirado.\n\n" +
                        "Por favor, renueva tu suscripción para continuar."
                    );
                }

                // Verificar fecha de expiración
                if (!licencia.isValida(java.time.LocalDate.now())) {
                    log("❌ Licencia vencida: " + email);
                    throw new AccountExpiredException(
                        "Tu licencia ha expirado el " + licencia.getFechaExpiracion() + ".\n\n" +
                        "Por favor, renueva tu suscripción para continuar."
                    );
                }

                log("✅ Login exitoso: " + email + " (" + licencia.getNombre() + ")");
                return Optional.of(licencia);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            log("❌ Error en login: " + e.getMessage());
            throw new DaoException("Error en autenticación: " + e.getMessage(), e);
        }
    }

    /**
     * Carga una licencia solo por email (sin verificar password)
     * SOLO para revalidación de sesión persistente
     *
     * @param email Email del usuario
     * @return Optional con la Licencia si existe y está activa
     */
    public Optional<Licencia> loginPorEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            log("❌ Email vacío en revalidación");
            return Optional.empty();
        }

        String sql = """
            SELECT cliente_id, nombre, email, password_hash,
                   estado::text as estado, plan::text as plan,
                   fecha_expiracion, notas, createdAt, updatedAt
            FROM licencia
            WHERE LOWER(email) = LOWER(?)
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log("❌ Usuario no encontrado en revalidación: " + email);
                    return Optional.empty();
                }

                // Cargar licencia
                Licencia licencia = Mapper.getLicencia(rs);

                // Validar que la licencia esté activa y vigente
                if (licencia.getEstado() == Licencia.EstadoLicencia.SUSPENDIDO) {
                    log("❌ Cuenta suspendida en revalidación: " + email);
                    return Optional.empty();
                }

                if (licencia.getEstado() == Licencia.EstadoLicencia.EXPIRADO) {
                    log("❌ Cuenta expirada en revalidación: " + email);
                    return Optional.empty();
                }

                // Verificar fecha de expiración
                if (!licencia.isValida(java.time.LocalDate.now())) {
                    log("❌ Licencia vencida en revalidación: " + email);
                    return Optional.empty();
                }

                log("✅ Revalidación exitosa: " + email);
                return Optional.of(licencia);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            log("❌ Error en revalidación: " + e.getMessage());
            throw new DaoException("Error en revalidación: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // REGISTRO
    // ============================================================================

    /**
     * Registra un nuevo usuario
     *
     * @param clienteId ID único del cliente (puede ser DNI, UUID, etc.)
     * @param nombre Nombre completo
     * @param email Email (único)
     * @param password Contraseña en texto plano (será hasheada)
     * @param plan Plan de licencia
     * @return true si se creó exitosamente
     */
    public boolean registrar(String clienteId, String nombre, String email,
                             String password, Licencia.PlanLicencia plan) {

        // Validaciones
        if (clienteId == null || clienteId.trim().isEmpty()) {
            throw new IllegalArgumentException("cliente_id no puede estar vacío");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email no puede estar vacío");
        }

        if (!PasswordUtil.validarPassword(password)) {
            String error = PasswordUtil.getPasswordError(password);
            throw new IllegalArgumentException("Password inválido: " + error);
        }

        // Hash del password
        String passwordHash = PasswordUtil.hashPassword(password);

        // Fecha de expiración por defecto según el plan
        java.time.LocalDate fechaExpiracion;
        switch (plan) {
            case DEMO -> fechaExpiracion = java.time.LocalDate.now().plusDays(15);
            case BASE -> fechaExpiracion = java.time.LocalDate.now().plusYears(1);
            case FULL -> fechaExpiracion = java.time.LocalDate.now().plusYears(10); // Prácticamente permanente
            default -> fechaExpiracion = java.time.LocalDate.now().plusDays(30);
        }

        String sql = """
            INSERT INTO licencia (cliente_id, nombre, email, password_hash,
                                  estado, plan, fecha_expiracion, notas)
            VALUES (?, ?, ?, ?, 'ACTIVO', ?::plan_licencia, ?, ?)
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, clienteId.trim());
            ps.setString(2, nombre.trim());
            ps.setString(3, email.trim().toLowerCase());
            ps.setString(4, passwordHash);
            ps.setString(5, plan.name());
            ps.setDate(6, Date.valueOf(fechaExpiracion));
            ps.setString(7, "Registro desde aplicación");

            int rows = ps.executeUpdate();

            if (rows == 1) {
                log("✅ Usuario registrado: " + email + " (" + nombre + ")");
                return true;
            }

            return false;

        } catch (SQLException e) {
            e.printStackTrace();

            if (e.getSQLState().equals("23505")) { // unique_violation
                log("❌ Email o cliente_id ya existe: " + email);
                throw new DaoException("El email o cliente_id ya está registrado", e);
            }

            log("❌ Error en registro: " + e.getMessage());
            throw new DaoException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Registra un nuevo usuario con todos los parámetros (versión completa para admin)
     *
     * @param clienteId ID único del cliente
     * @param nombre Nombre completo
     * @param email Email (único)
     * @param password Contraseña en texto plano (será hasheada)
     * @param estado Estado de la licencia (ACTIVA, SUSPENDIDA, VENCIDA)
     * @param plan Plan de licencia (DEMO, BASICO, PREMIUM, ENTERPRISE)
     * @param fechaExpiracion Fecha de expiración
     * @param notas Notas opcionales
     * @return true si se creó exitosamente
     */
    public boolean registrar(String clienteId, String nombre, String email, String password,
                             String estado, String plan, java.time.LocalDate fechaExpiracion,
                             String notas) {

        // Validaciones
        if (clienteId == null || clienteId.trim().isEmpty()) {
            throw new IllegalArgumentException("cliente_id no puede estar vacío");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email no puede estar vacío");
        }

        if (!PasswordUtil.validarPassword(password)) {
            String error = PasswordUtil.getPasswordError(password);
            throw new IllegalArgumentException("Password inválido: " + error);
        }

        // Hash del password
        String passwordHash = PasswordUtil.hashPassword(password);

        String sql = """
            INSERT INTO licencia (cliente_id, nombre, email, password_hash,
                                  estado, plan, fecha_expiracion, notas)
            VALUES (?, ?, ?, ?, ?::estado_licencia, ?::plan_licencia, ?, ?)
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, clienteId.trim());
            ps.setString(2, nombre.trim());
            ps.setString(3, email.trim().toLowerCase());
            ps.setString(4, passwordHash);
            ps.setString(5, estado.toUpperCase());
            ps.setString(6, plan.toUpperCase());
            ps.setDate(7, Date.valueOf(fechaExpiracion));
            ps.setString(8, notas != null && !notas.trim().isEmpty() ? notas.trim() : null);

            int rows = ps.executeUpdate();

            if (rows == 1) {
                log("✅ Usuario registrado (admin): " + email + " (" + nombre + ")");
                return true;
            }

            return false;

        } catch (SQLException e) {
            e.printStackTrace();

            if (e.getSQLState().equals("23505")) { // unique_violation
                log("❌ Email o cliente_id ya existe: " + email);
                return false; // Retornar false en lugar de lanzar excepción
            }

            log("❌ Error en registro: " + e.getMessage());
            throw new DaoException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // CAMBIO DE CONTRASEÑA
    // ============================================================================

    /**
     * Cambia la contraseña de un usuario
     *
     * @param email Email del usuario
     * @param passwordActual Contraseña actual (para verificar)
     * @param passwordNueva Nueva contraseña
     * @return true si se cambió exitosamente
     */
    public boolean cambiarPassword(String email, String passwordActual, String passwordNueva) {

        // Validar nueva contraseña
        if (!PasswordUtil.validarPassword(passwordNueva)) {
            String error = PasswordUtil.getPasswordError(passwordNueva);
            throw new IllegalArgumentException("Nueva password inválida: " + error);
        }

        // 1. Verificar password actual
        Optional<Licencia> licenciaOpt = login(email, passwordActual);
        if (licenciaOpt.isEmpty()) {
            log("❌ Password actual incorrecta para: " + email);
            return false;
        }

        // 2. Hashear nueva password
        String nuevoHash = PasswordUtil.hashPassword(passwordNueva);

        // 3. Actualizar en DB
        String sql = """
            UPDATE licencia
            SET password_hash = ?,
                updatedAt = now()
            WHERE email = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, nuevoHash);
            ps.setString(2, email.trim().toLowerCase());

            int rows = ps.executeUpdate();

            if (rows == 1) {
                log("✅ Password cambiada para: " + email);
                return true;
            }

            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            log("❌ Error cambiando password: " + e.getMessage());
            throw new DaoException("Error al cambiar password: " + e.getMessage(), e);
        }
    }

    /**
     * Resetea la contraseña de un usuario (sin verificar la actual)
     * SOLO para uso administrativo
     *
     * @param email Email del usuario
     * @param nuevaPassword Nueva contraseña
     * @return true si se reseteo exitosamente
     */
    public boolean resetearPassword(String email, String nuevaPassword) {

        if (!PasswordUtil.validarPassword(nuevaPassword)) {
            String error = PasswordUtil.getPasswordError(nuevaPassword);
            throw new IllegalArgumentException("Password inválida: " + error);
        }

        String nuevoHash = PasswordUtil.hashPassword(nuevaPassword);

        String sql = """
            UPDATE licencia
            SET password_hash = ?,
                updatedAt = now()
            WHERE email = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, nuevoHash);
            ps.setString(2, email.trim().toLowerCase());

            int rows = ps.executeUpdate();

            if (rows == 1) {
                log("✅ Password reseteada para: " + email);
                return true;
            }

            log("❌ Usuario no encontrado: " + email);
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            log("❌ Error reseteando password: " + e.getMessage());
            throw new DaoException("Error al resetear password: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // MONITOREO DE ESTADO
    // ============================================================================

    /**
     * Verifica únicamente el estado de una licencia (query ligera)
     * Usado por el monitor en background para detectar suspensiones
     *
     * @param email Email del usuario
     * @return Estado actual de la licencia, o null si no existe
     */
    public Licencia.EstadoLicencia verificarEstado(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT estado::text FROM licencia WHERE LOWER(email) = LOWER(?)";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log("⚠️ Usuario no encontrado en verificación de estado: " + email);
                    return null;
                }

                String estadoStr = rs.getString("estado");
                return Licencia.EstadoLicencia.valueOf(estadoStr);
            }

        } catch (SQLException e) {
            log("❌ Error verificando estado: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ============================================================================
    // UTILIDADES
    // ============================================================================

    /**
     * Verifica si un email ya está registrado
     *
     * @param email Email a verificar
     * @return true si ya existe
     */
    public boolean existeEmail(String email) {
        String sql = "SELECT 1 FROM licencia WHERE LOWER(email) = LOWER(?)";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DaoException("Error verificando email", e);
        }
    }

    /**
     * Verifica si un cliente_id ya está registrado
     *
     * @param clienteId Cliente ID a verificar
     * @return true si ya existe
     */
    public boolean existeClienteId(String clienteId) {
        String sql = "SELECT 1 FROM licencia WHERE cliente_id = ?";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, clienteId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DaoException("Error verificando cliente_id", e);
        }
    }

    /**
     * Log simple
     */
    private void log(String mensaje) {
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        System.out.println("[AutenticacionDAO " + timestamp + "] " + mensaje);
    }
}
