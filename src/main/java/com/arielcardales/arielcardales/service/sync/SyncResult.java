package com.arielcardales.arielcardales.service.sync;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de una operación de sincronización
 */
public class SyncResult {

    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    private SyncDirection direction;
    private SyncStats stats;
    private List<String> errors;

    public SyncResult() {
        this.timestamp = LocalDateTime.now();
        this.stats = new SyncStats();
        this.errors = new ArrayList<>();
    }

    /**
     * Dirección de la sincronización
     */
    public enum SyncDirection {
        CLOUD_TO_LOCAL,  // Supabase → SQLite
        LOCAL_TO_CLOUD,  // SQLite → Supabase
        BIDIRECTIONAL    // Ambas direcciones
    }

    /**
     * Estadísticas de la sincronización
     */
    public static class SyncStats {
        private int productosInsertados;
        private int productosActualizados;
        private int productosEliminados;

        private int variantesInsertadas;
        private int variantesActualizadas;
        private int variantesEliminadas;

        private int categoriasInsertadas;
        private int categoriasActualizadas;
        private int categoriasEliminadas;

        private int unidadesInsertadas;
        private int unidadesActualizadas;
        private int unidadesEliminadas;

        private int ventasInsertadas;
        private int ventasActualizadas;
        private int ventasEliminadas;

        private int clientesInsertados;
        private int clientesActualizados;
        private int clientesEliminados;

        private int conflictosResueltos;

        // Getters y Setters
        public int getProductosInsertados() { return productosInsertados; }
        public void setProductosInsertados(int productosInsertados) { this.productosInsertados = productosInsertados; }
        public void incrementProductosInsertados() { this.productosInsertados++; }

        public int getProductosActualizados() { return productosActualizados; }
        public void setProductosActualizados(int productosActualizados) { this.productosActualizados = productosActualizados; }
        public void incrementProductosActualizados() { this.productosActualizados++; }

        public int getProductosEliminados() { return productosEliminados; }
        public void setProductosEliminados(int productosEliminados) { this.productosEliminados = productosEliminados; }
        public void incrementProductosEliminados() { this.productosEliminados++; }

        public int getVariantesInsertadas() { return variantesInsertadas; }
        public void setVariantesInsertadas(int variantesInsertadas) { this.variantesInsertadas = variantesInsertadas; }
        public void incrementVariantesInsertadas() { this.variantesInsertadas++; }

        public int getVariantesActualizadas() { return variantesActualizadas; }
        public void setVariantesActualizadas(int variantesActualizadas) { this.variantesActualizadas = variantesActualizadas; }
        public void incrementVariantesActualizadas() { this.variantesActualizadas++; }

        public int getVariantesEliminadas() { return variantesEliminadas; }
        public void setVariantesEliminadas(int variantesEliminadas) { this.variantesEliminadas = variantesEliminadas; }
        public void incrementVariantesEliminadas() { this.variantesEliminadas++; }

        public int getCategoriasInsertadas() { return categoriasInsertadas; }
        public void setCategoriasInsertadas(int categoriasInsertadas) { this.categoriasInsertadas = categoriasInsertadas; }
        public void incrementCategoriasInsertadas() { this.categoriasInsertadas++; }

        public int getCategoriasActualizadas() { return categoriasActualizadas; }
        public void setCategoriasActualizadas(int categoriasActualizadas) { this.categoriasActualizadas = categoriasActualizadas; }
        public void incrementCategoriasActualizadas() { this.categoriasActualizadas++; }

        public int getCategoriasEliminadas() { return categoriasEliminadas; }
        public void setCategoriasEliminadas(int categoriasEliminadas) { this.categoriasEliminadas = categoriasEliminadas; }
        public void incrementCategoriasEliminadas() { this.categoriasEliminadas++; }

        public int getUnidadesInsertadas() { return unidadesInsertadas; }
        public void setUnidadesInsertadas(int unidadesInsertadas) { this.unidadesInsertadas = unidadesInsertadas; }
        public void incrementUnidadesInsertadas() { this.unidadesInsertadas++; }

        public int getUnidadesActualizadas() { return unidadesActualizadas; }
        public void setUnidadesActualizadas(int unidadesActualizadas) { this.unidadesActualizadas = unidadesActualizadas; }
        public void incrementUnidadesActualizadas() { this.unidadesActualizadas++; }

        public int getUnidadesEliminadas() { return unidadesEliminadas; }
        public void setUnidadesEliminadas(int unidadesEliminadas) { this.unidadesEliminadas = unidadesEliminadas; }
        public void incrementUnidadesEliminadas() { this.unidadesEliminadas++; }

        public int getVentasInsertadas() { return ventasInsertadas; }
        public void setVentasInsertadas(int ventasInsertadas) { this.ventasInsertadas = ventasInsertadas; }
        public void incrementVentasInsertadas() { this.ventasInsertadas++; }

        public int getVentasActualizadas() { return ventasActualizadas; }
        public void setVentasActualizadas(int ventasActualizadas) { this.ventasActualizadas = ventasActualizadas; }
        public void incrementVentasActualizadas() { this.ventasActualizadas++; }

        public int getVentasEliminadas() { return ventasEliminadas; }
        public void setVentasEliminadas(int ventasEliminadas) { this.ventasEliminadas = ventasEliminadas; }
        public void incrementVentasEliminadas() { this.ventasEliminadas++; }

        public int getClientesInsertados() { return clientesInsertados; }
        public void setClientesInsertados(int clientesInsertados) { this.clientesInsertados = clientesInsertados; }
        public void incrementClientesInsertados() { this.clientesInsertados++; }

        public int getClientesActualizados() { return clientesActualizados; }
        public void setClientesActualizados(int clientesActualizados) { this.clientesActualizados = clientesActualizados; }
        public void incrementClientesActualizados() { this.clientesActualizados++; }

        public int getClientesEliminados() { return clientesEliminados; }
        public void setClientesEliminados(int clientesEliminados) { this.clientesEliminados = clientesEliminados; }
        public void incrementClientesEliminados() { this.clientesEliminados++; }

        public int getConflictosResueltos() { return conflictosResueltos; }
        public void setConflictosResueltos(int conflictosResueltos) { this.conflictosResueltos = conflictosResueltos; }
        public void incrementConflictosResueltos() { this.conflictosResueltos++; }

        /**
         * Retorna el total de operaciones realizadas
         */
        public int getTotalOperaciones() {
            return productosInsertados + productosActualizados + productosEliminados +
                   variantesInsertadas + variantesActualizadas + variantesEliminadas +
                   categoriasInsertadas + categoriasActualizadas + categoriasEliminadas +
                   unidadesInsertadas + unidadesActualizadas + unidadesEliminadas +
                   ventasInsertadas + ventasActualizadas + ventasEliminadas +
                   clientesInsertados + clientesActualizados + clientesEliminados;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Productos: ").append(productosInsertados).append(" nuevos, ")
              .append(productosActualizados).append(" actualizados\n");
            sb.append("Variantes: ").append(variantesInsertadas).append(" nuevas, ")
              .append(variantesActualizadas).append(" actualizadas\n");
            sb.append("Categorías: ").append(categoriasInsertadas).append(" nuevas, ")
              .append(categoriasActualizadas).append(" actualizadas\n");
            sb.append("Unidades: ").append(unidadesInsertadas).append(" nuevas, ")
              .append(unidadesActualizadas).append(" actualizadas\n");
            sb.append("Ventas: ").append(ventasInsertadas).append(" nuevas\n");
            sb.append("Clientes: ").append(clientesInsertados).append(" nuevos\n");
            if (conflictosResueltos > 0) {
                sb.append("Conflictos resueltos: ").append(conflictosResueltos);
            }
            return sb.toString();
        }
    }

    // Getters y Setters principales
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public SyncDirection getDirection() { return direction; }
    public void setDirection(SyncDirection direction) { this.direction = direction; }

    public SyncStats getStats() { return stats; }
    public void setStats(SyncStats stats) { this.stats = stats; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public void addError(String error) { this.errors.add(error); }

    @Override
    public String toString() {
        return "SyncResult{" +
               "success=" + success +
               ", direction=" + direction +
               ", totalOperaciones=" + stats.getTotalOperaciones() +
               ", errors=" + errors.size() +
               '}';
    }
}
