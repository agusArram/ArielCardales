package com.arielcardales.arielcardales.Licencia;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Gestor de licencias con validación remota segura
 */
public class LicenciaManager {

    private static Licencia licenciaActual = null;
    private static LocalDate ultimaValidacionOnline = null;
    private static boolean modoOffline = false;

    // ============================================================================
    // MÉTODOS PÚBLICOS PRINCIPALES
    // ============================================================================

    /**
     * Valida la licencia del cliente actual
     * Intenta validar online, si falla usa cache local
     *
     * @return true si la licencia es válida
     */
    public static boolean validarLicencia() {
        LicenciaConfig.inicializarDirectorios();

        try {
            // 1. Intentar validación online
            if (validarOnline()) {
                log("✅ Validación online exitosa");
                guardarCacheLocal();
                return licenciaActual.isValida(obtenerFechaReal());
            }
        } catch (Exception e) {
            log("⚠ Error en validación online: " + e.getMessage());
        }

        // 2. Si falla online, usar cache local (con límite de días)
        try {
            if (validarOffline()) {
                log("⚠ Usando validación offline (cache local)");
                return licenciaActual.isValida(LocalDate.now());
            }
        } catch (Exception e) {
            log("❌ Error en validación offline: " + e.getMessage());
        }

        // 3. Sin licencia válida
        log("❌ No se pudo validar licencia");
        return false;
    }

    /**
     * Obtiene la licencia actual (después de validar)
     */
    public static Licencia getLicencia() {
        return licenciaActual;
    }

    /**
     * Verifica si el sistema está en modo offline
     */
    public static boolean isModoOffline() {
        return modoOffline;
    }

    /**
     * Obtiene días restantes antes de expiración
     */
    public static long getDiasRestantes() {
        if (licenciaActual == null) return 0;

        LocalDate fechaActual = obtenerFechaReal();
        LocalDate expiracion = licenciaActual.getFechaExpiracion();

        return java.time.temporal.ChronoUnit.DAYS.between(fechaActual, expiracion);
    }

    // ============================================================================
    // VALIDACIÓN ONLINE
    // ============================================================================

    /**
     * Valida la licencia contra el JSON remoto en GitHub
     */
    private static boolean validarOnline() throws Exception {
        // 1. Descargar JSON de licencias
        String jsonContent = descargarJSON(LicenciaConfig.LICENCIAS_JSON_URL);
        if (jsonContent == null) {
            throw new Exception("No se pudo descargar el JSON de licencias");
        }

        // 2. Parsear JSON
        JSONObject jsonRoot = new JSONObject(jsonContent);

        // 3. Verificar firma del JSON (integridad)
        String firmaEsperada = jsonRoot.optString("firma", "");
        String firmaCalculada = calcularFirmaJSON(jsonContent);

        if (!firmaEsperada.equals(firmaCalculada)) {
            log("⚠ ADVERTENCIA: Firma del JSON no coincide (posible manipulación)");
            // No bloqueamos por esto, pero lo registramos
        }

        // 4. Buscar licencia del cliente
        JSONArray usuarios = jsonRoot.getJSONArray("usuarios");
        JSONObject datosCliente = null;

        for (int i = 0; i < usuarios.length(); i++) {
            JSONObject usuario = usuarios.getJSONObject(i);
            String id = usuario.optString("clienteId", "");

            if (id.equals(LicenciaConfig.CLIENTE_ID)) {
                datosCliente = usuario;
                break;
            }
        }

        if (datosCliente == null) {
            throw new Exception("Cliente no encontrado en el sistema de licencias");
        }

        // 5. Parsear datos de la licencia
        licenciaActual = parsearLicencia(datosCliente);

        // 6. Obtener fecha real desde API externa
        LocalDate fechaReal = obtenerFechaReal();

        // 7. Actualizar última validación
        ultimaValidacionOnline = fechaReal;
        modoOffline = false;

        return true;
    }

    /**
     * Descarga el contenido del JSON desde GitHub
     */
    private static String descargarJSON(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(LicenciaConfig.TIMEOUT_MS))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(LicenciaConfig.TIMEOUT_MS))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            log("Error al descargar JSON: " + e.getMessage());
        }
        return null;
    }

    // ============================================================================
    // VALIDACIÓN OFFLINE
    // ============================================================================

    /**
     * Valida usando cache local (máximo MAX_DIAS_SIN_VALIDACION días)
     */
    private static boolean validarOffline() throws Exception {
        File cacheFile = new File(LicenciaConfig.LICENCIA_LOCAL_FILE);

        if (!cacheFile.exists()) {
            throw new Exception("No existe cache local de licencia");
        }

        // 1. Leer cache encriptado
        String datosEncriptados = Files.readString(Paths.get(LicenciaConfig.LICENCIA_LOCAL_FILE));
        String datosJson = desencriptar(datosEncriptados);

        JSONObject cache = new JSONObject(datosJson);

        // 2. Verificar fecha de última validación
        String fechaStr = cache.getString("ultimaValidacion");
        LocalDate ultimaValidacion = LocalDate.parse(fechaStr);
        LocalDate hoy = LocalDate.now(); // Usamos reloj local (puede manipularse, pero con límite)

        long diasSinValidar = java.time.temporal.ChronoUnit.DAYS.between(ultimaValidacion, hoy);

        if (diasSinValidar > LicenciaConfig.MAX_DIAS_SIN_VALIDACION) {
            throw new Exception("Hace más de " + LicenciaConfig.MAX_DIAS_SIN_VALIDACION +
                    " días sin validación online. Conecte a internet.");
        }

        // 3. Cargar licencia del cache
        JSONObject licenciaJson = cache.getJSONObject("licencia");
        licenciaActual = parsearLicencia(licenciaJson);

        ultimaValidacionOnline = ultimaValidacion;
        modoOffline = true;

        return true;
    }

    /**
     * Guarda la licencia en cache local encriptado
     */
    private static void guardarCacheLocal() {
        try {
            JSONObject cache = new JSONObject();
            cache.put("ultimaValidacion", ultimaValidacionOnline.toString());

            JSONObject licenciaJson = new JSONObject();
            licenciaJson.put("clienteId", licenciaActual.getClienteId());
            licenciaJson.put("nombre", licenciaActual.getNombre());
            licenciaJson.put("email", licenciaActual.getEmail());
            licenciaJson.put("estado", licenciaActual.getEstado().name());
            licenciaJson.put("plan", licenciaActual.getPlan().name());
            licenciaJson.put("expira", licenciaActual.getFechaExpiracion().toString());
            licenciaJson.put("firma", licenciaActual.getFirma());

            cache.put("licencia", licenciaJson);

            // Encriptar y guardar
            String datosEncriptados = encriptar(cache.toString());
            Files.writeString(Paths.get(LicenciaConfig.LICENCIA_LOCAL_FILE), datosEncriptados);

        } catch (Exception e) {
            log("Error al guardar cache local: " + e.getMessage());
        }
    }

    // ============================================================================
    // OBTENCIÓN DE FECHA REAL
    // ============================================================================

    /**
     * Obtiene la fecha actual desde una API externa (evita manipulación del reloj)
     * Si falla, usa la fecha del sistema con advertencia
     */
    private static LocalDate obtenerFechaReal() {
        // Intentar con API principal
        LocalDate fecha = obtenerFechaDesdeAPI(LicenciaConfig.TIME_API_URL);
        if (fecha != null) return fecha;

        // Intentar con backup 1
        fecha = obtenerFechaDesdeAPI(LicenciaConfig.TIME_API_BACKUP_1);
        if (fecha != null) return fecha;

        // Si todo falla, usar reloj del sistema (con log de advertencia)
        log("⚠ No se pudo obtener fecha real, usando reloj del sistema");
        return LocalDate.now();
    }

    /**
     * Obtiene fecha desde una API específica
     */
    private static LocalDate obtenerFechaDesdeAPI(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(LicenciaConfig.TIMEOUT_MS))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(LicenciaConfig.TIMEOUT_MS))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());

                // worldtimeapi.org formato
                if (json.has("datetime")) {
                    String datetime = json.getString("datetime");
                    return LocalDate.parse(datetime.substring(0, 10));
                }

                // timeapi.io formato
                if (json.has("date")) {
                    String date = json.getString("date");
                    return LocalDate.parse(date);
                }
            }
        } catch (Exception e) {
            // Silencioso, probar siguiente API
        }
        return null;
    }

    // ============================================================================
    // UTILIDADES DE SEGURIDAD
    // ============================================================================

    /**
     * Calcula firma SHA-256 del JSON (para verificar integridad)
     */
    private static String calcularFirmaJSON(String jsonContent) {
        try {
            // Remover el campo firma antes de calcular
            JSONObject json = new JSONObject(jsonContent);
            json.remove("firma");
            String contenidoSinFirma = json.toString();

            // Calcular SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String dataConClave = contenidoSinFirma + LicenciaConfig.SECRET_KEY;
            byte[] hash = digest.digest(dataConClave.getBytes(StandardCharsets.UTF_8));

            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            log("Error al calcular firma: " + e.getMessage());
            return "";
        }
    }

    /**
     * Encripta datos usando Base64 + XOR simple (ofuscación básica)
     * Para mayor seguridad, usar AES en producción
     */
    private static String encriptar(String datos) {
        try {
            byte[] bytes = datos.getBytes(StandardCharsets.UTF_8);
            byte[] clave = LicenciaConfig.SECRET_KEY.getBytes(StandardCharsets.UTF_8);

            // XOR simple para ofuscación
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ clave[i % clave.length]);
            }

            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log("Error al encriptar: " + e.getMessage());
            return datos;
        }
    }

    /**
     * Desencripta datos
     */
    private static String desencriptar(String datosEncriptados) {
        try {
            byte[] bytes = Base64.getDecoder().decode(datosEncriptados);
            byte[] clave = LicenciaConfig.SECRET_KEY.getBytes(StandardCharsets.UTF_8);

            // XOR inverso
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ clave[i % clave.length]);
            }

            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log("Error al desencriptar: " + e.getMessage());
            return datosEncriptados;
        }
    }

    /**
     * Parsea un objeto JSON a Licencia
     */
    private static Licencia parsearLicencia(JSONObject json) {
        Licencia lic = new Licencia();
        lic.setClienteId(json.optString("clienteId", ""));
        lic.setNombre(json.optString("nombre", ""));
        lic.setEmail(json.optString("email", ""));

        String estadoStr = json.optString("estado", "DEMO");
        lic.setEstado(Licencia.EstadoLicencia.valueOf(estadoStr.toUpperCase()));

        String planStr = json.optString("plan", "DEMO");
        lic.setPlan(Licencia.PlanLicencia.valueOf(planStr.toUpperCase()));

        String expiraStr = json.optString("expira", LocalDate.now().plusDays(15).toString());
        lic.setFechaExpiracion(LocalDate.parse(expiraStr));

        lic.setFirma(json.optString("firma", ""));

        return lic;
    }

    // ============================================================================
    // LOGGING
    // ============================================================================

    /**
     * Registra eventos en archivo de log
     */
    private static void log(String mensaje) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String linea = "[" + timestamp + "] " + mensaje + "\n";

            Files.writeString(
                    Paths.get(LicenciaConfig.VALIDACION_LOG_FILE),
                    linea,
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );

            // También imprimir en consola para debug
            System.out.println(linea);

        } catch (Exception e) {
            System.err.println("Error al escribir log: " + e.getMessage());
        }
    }

    // ============================================================================
    // MÉTODOS DE UTILIDAD PARA INTERFAZ
    // ============================================================================

    /**
     * Obtiene mensaje de estado de la licencia para mostrar al usuario
     */
    public static String getMensajeEstado() {
        if (licenciaActual == null) {
            return "❌ Licencia no válida";
        }

        long diasRestantes = getDiasRestantes();
        String planNombre = licenciaActual.getPlan().name();
        String modoStr = modoOffline ? " (offline)" : "";

        if (diasRestantes < 0) {
            return "❌ Licencia expirada";
        } else if (diasRestantes <= 7) {
            return "⚠ Licencia " + planNombre + " - " + diasRestantes + " días restantes" + modoStr;
        } else {
            return "✅ Licencia " + planNombre + " activa" + modoStr;
        }
    }

    /**
     * Fuerza una revalidación online (para botón "Verificar licencia")
     */
    public static boolean revalidarOnline() {
        try {
            return validarOnline();
        } catch (Exception e) {
            log("Error en revalidación: " + e.getMessage());
            return false;
        }
    }
}
