package com.arielcardales.arielcardales.service.sync;

import java.time.LocalDateTime;

/**
 * Resuelve conflictos de sincronización basándose en updatedAt
 * Estrategia: gana la última modificación (Last Write Wins - LWW)
 */
public class ConflictResolver {

    /**
     * Resultado de la resolución de conflicto
     */
    public enum ConflictResolution {
        USE_CLOUD,   // Usar dato de Supabase
        USE_LOCAL,   // Usar dato de SQLite
        NO_CONFLICT  // No hay conflicto
    }

    /**
     * Resuelve un conflicto entre dos registros basándose en updatedAt
     *
     * @param cloudUpdatedAt fecha de última actualización en Supabase
     * @param localUpdatedAt fecha de última actualización en SQLite
     * @return ConflictResolution indicando qué versión usar
     */
    public static ConflictResolution resolve(LocalDateTime cloudUpdatedAt, LocalDateTime localUpdatedAt) {
        // Si alguno es null, no hay conflicto
        if (cloudUpdatedAt == null && localUpdatedAt == null) {
            return ConflictResolution.NO_CONFLICT;
        }

        if (cloudUpdatedAt == null) {
            return ConflictResolution.USE_LOCAL;
        }

        if (localUpdatedAt == null) {
            return ConflictResolution.USE_CLOUD;
        }

        // Comparar fechas: el más reciente gana
        int comparison = cloudUpdatedAt.compareTo(localUpdatedAt);

        if (comparison > 0) {
            // Cloud es más reciente
            return ConflictResolution.USE_CLOUD;
        } else if (comparison < 0) {
            // Local es más reciente
            return ConflictResolution.USE_LOCAL;
        } else {
            // Son iguales, no hay conflicto
            return ConflictResolution.NO_CONFLICT;
        }
    }

    /**
     * Verifica si existe un conflicto real (ambas versiones han sido modificadas)
     *
     * @param cloudUpdatedAt fecha de última actualización en Supabase
     * @param localUpdatedAt fecha de última actualización en SQLite
     * @return true si hay conflicto
     */
    public static boolean hasConflict(LocalDateTime cloudUpdatedAt, LocalDateTime localUpdatedAt) {
        if (cloudUpdatedAt == null || localUpdatedAt == null) {
            return false;
        }

        // Hay conflicto si las fechas son diferentes
        return !cloudUpdatedAt.equals(localUpdatedAt);
    }

    /**
     * Obtiene un mensaje descriptivo del conflicto
     *
     * @param cloudUpdatedAt fecha de última actualización en Supabase
     * @param localUpdatedAt fecha de última actualización en SQLite
     * @return mensaje descriptivo
     */
    public static String getConflictMessage(LocalDateTime cloudUpdatedAt, LocalDateTime localUpdatedAt) {
        ConflictResolution resolution = resolve(cloudUpdatedAt, localUpdatedAt);

        switch (resolution) {
            case USE_CLOUD:
                return "Usando versión de nube (más reciente: " + cloudUpdatedAt + ")";
            case USE_LOCAL:
                return "Usando versión local (más reciente: " + localUpdatedAt + ")";
            case NO_CONFLICT:
                return "Sin conflicto (misma fecha de actualización)";
            default:
                return "Resolución desconocida";
        }
    }
}
