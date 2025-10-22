package com.arielcardales.arielcardales.Util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utilidad para hash y verificación de contraseñas con BCrypt
 *
 * BCrypt es un algoritmo de hash diseñado específicamente para passwords:
 * - Lento por diseño (protege contra ataques de fuerza bruta)
 * - Salt automático (cada hash es único)
 * - Resistente a rainbow tables
 * - Ajustable en costo computacional
 */
public class PasswordUtil {

    /**
     * Costo de BCrypt (número de rondas: 2^cost)
     * 10 = ~150ms por hash (balance entre seguridad y UX)
     * 12 = ~600ms por hash (más seguro, más lento)
     *
     * IMPORTANTE: Cada incremento duplica el tiempo de cálculo
     */
    private static final int BCRYPT_COST = 10;

    // ============================================================================
    // MÉTODOS PÚBLICOS
    // ============================================================================

    /**
     * Genera un hash BCrypt de una contraseña
     *
     * Ejemplo:
     * ```java
     * String hash = PasswordUtil.hashPassword("miPassword123");
     * // Retorna: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     * ```
     *
     * @param password Contraseña en texto plano
     * @return Hash BCrypt (60 caracteres)
     * @throws IllegalArgumentException si password es null o vacío
     */
    public static String hashPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    /**
     * Verifica si una contraseña coincide con un hash BCrypt
     *
     * Ejemplo:
     * ```java
     * String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
     * boolean valido = PasswordUtil.verifyPassword("miPassword123", hash);
     * // Retorna: true
     * ```
     *
     * @param password Contraseña en texto plano
     * @param hash Hash BCrypt a comparar
     * @return true si la contraseña coincide
     */
    public static boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }

        try {
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
            return result.verified;
        } catch (Exception e) {
            // Hash inválido o corrupto
            System.err.println("Error verificando password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Valida que una contraseña cumpla requisitos mínimos de seguridad
     *
     * Requisitos:
     * - Mínimo 6 caracteres
     * - Al menos una letra
     * - Al menos un número
     *
     * @param password Contraseña a validar
     * @return true si cumple los requisitos
     */
    public static boolean validarPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        boolean tieneLetra = password.matches(".*[a-zA-Z].*");
        boolean tieneNumero = password.matches(".*\\d.*");

        return tieneLetra && tieneNumero;
    }

    /**
     * Obtiene un mensaje de error describiendo por qué una contraseña no es válida
     *
     * @param password Contraseña a validar
     * @return Mensaje de error, o null si es válida
     */
    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) {
            return "La contraseña no puede estar vacía";
        }

        if (password.length() < 6) {
            return "La contraseña debe tener al menos 6 caracteres";
        }

        if (!password.matches(".*[a-zA-Z].*")) {
            return "La contraseña debe contener al menos una letra";
        }

        if (!password.matches(".*\\d.*")) {
            return "La contraseña debe contener al menos un número";
        }

        return null; // Válida
    }

    // ============================================================================
    // MÉTODOS DE UTILIDAD
    // ============================================================================

    /**
     * Genera un hash de ejemplo para testing
     * NO USAR EN PRODUCCIÓN
     *
     * @param password Password a hashear
     * @return Hash para copiar en SQL/testing
     */
    public static String generateHashForTesting(String password) {
        String hash = hashPassword(password);
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("\nSQL UPDATE:");
        System.out.println("UPDATE licencia SET password_hash = '" + hash + "' WHERE email = 'usuario@ejemplo.com';");
        return hash;
    }

    /**
     * Genera múltiples hashes para testing (passwords comunes)
     * Útil para poblar la DB de desarrollo
     */
    public static void generateCommonHashes() {
        System.out.println("=== HASHES PARA TESTING (NO USAR EN PRODUCCIÓN) ===\n");

        String[] passwords = {"demo123", "admin123", "test123", "dev123", "user123"};

        for (String pwd : passwords) {
            String hash = hashPassword(pwd);
            System.out.println("-- Password: " + pwd);
            System.out.println("UPDATE licencia SET password_hash = '" + hash + "' WHERE ...");
            System.out.println();
        }
    }

    // ============================================================================
    // MAIN PARA TESTING
    // ============================================================================

    /**
     * Main para generar hashes desde consola
     * Ejecutar: mvn exec:java -Dexec.mainClass="com.arielcardales.arielcardales.Util.PasswordUtil" -Dexec.args="miPassword"
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            // Generar hash de password específico
            String password = args[0];
            generateHashForTesting(password);
        } else {
            // Generar hashes comunes
            generateCommonHashes();

            // Ejemplo de uso
            System.out.println("\n=== EJEMPLO DE USO ===\n");

            String password = "test123";
            String hash = hashPassword(password);

            System.out.println("1. Hash generado:");
            System.out.println("   " + hash);

            System.out.println("\n2. Verificación correcta:");
            System.out.println("   " + verifyPassword("test123", hash)); // true

            System.out.println("\n3. Verificación incorrecta:");
            System.out.println("   " + verifyPassword("wrong", hash)); // false

            System.out.println("\n4. Validación de password:");
            System.out.println("   'abc' válido? " + validarPassword("abc")); // false (muy corto)
            System.out.println("   'abc123' válido? " + validarPassword("abc123")); // true
            System.out.println("   '123456' válido? " + validarPassword("123456")); // false (sin letras)
        }
    }
}
