package SORT_PROYECTS.AppInventario.Licencia;

import java.time.LocalDate;

/**
 * Modelo de licencia del sistema
 */
public class Licencia {
    private String clienteId;
    private String nombre;
    private String email;
    private EstadoLicencia estado;
    private PlanLicencia plan;
    private LocalDate fechaExpiracion;
    private String firma; // Hash de verificación

    // Constructor vacío
    public Licencia() {
    }

    // Constructor completo
    public Licencia(String clienteId, String nombre, String email,
                    EstadoLicencia estado, PlanLicencia plan,
                    LocalDate fechaExpiracion, String firma) {
        this.clienteId = clienteId;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
        this.plan = plan;
        this.fechaExpiracion = fechaExpiracion;
        this.firma = firma;
    }

    // Getters y Setters
    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public EstadoLicencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoLicencia estado) {
        this.estado = estado;
    }

    public PlanLicencia getPlan() {
        return plan;
    }

    public void setPlan(PlanLicencia plan) {
        this.plan = plan;
    }

    public LocalDate getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDate fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getFirma() {
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
    }

    /**
     * Verifica si la licencia está activa y vigente
     */
    public boolean isValida(LocalDate fechaActual) {
        return estado == EstadoLicencia.ACTIVO &&
               !fechaActual.isAfter(fechaExpiracion);
    }

    /**
     * Verifica si la licencia permite una funcionalidad específica
     */
    public boolean permiteAcceso(String funcionalidad) {
        if (!isValida(LocalDate.now())) {
            return false;
        }

        return plan.permiteAcceso(funcionalidad);
    }

    @Override
    public String toString() {
        return "Licencia{" +
                "clienteId='" + clienteId + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", estado=" + estado +
                ", plan=" + plan +
                ", fechaExpiracion=" + fechaExpiracion +
                '}';
    }

    /**
     * Estados posibles de una licencia
     */
    public enum EstadoLicencia {
        ACTIVO,
        SUSPENDIDO,
        EXPIRADO,
        DEMO
    }

    /**
     * Planes de licencia con permisos
     */
    public enum PlanLicencia {
        DEMO(15, 10, false, false),
        BASE(Integer.MAX_VALUE, Integer.MAX_VALUE, false, false),
        FULL(Integer.MAX_VALUE, Integer.MAX_VALUE, true, false),
        DEV(Integer.MAX_VALUE, Integer.MAX_VALUE, true, true); // Plan especial para desarrollador

        private final int maxProductos;
        private final int maxVentas;
        private final boolean metricasAvanzadas;
        private final boolean accesoAdministracion;

        PlanLicencia(int maxProductos, int maxVentas, boolean metricasAvanzadas, boolean accesoAdministracion) {
            this.maxProductos = maxProductos;
            this.maxVentas = maxVentas;
            this.metricasAvanzadas = metricasAvanzadas;
            this.accesoAdministracion = accesoAdministracion;
        }

        public int getMaxProductos() {
            return maxProductos;
        }

        public int getMaxVentas() {
            return maxVentas;
        }

        public boolean tieneMetricasAvanzadas() {
            return metricasAvanzadas;
        }

        public boolean tieneAccesoAdministracion() {
            return accesoAdministracion;
        }

        /**
         * Verifica si este plan permite acceso a una funcionalidad
         */
        public boolean permiteAcceso(String funcionalidad) {
            switch (funcionalidad) {
                case "metricas_avanzadas":
                    return metricasAvanzadas;
                case "exportar_pdf":
                    return this != DEMO;
                case "exportar_excel":
                    return this != DEMO;
                case "multi_usuario":
                    return this == FULL || this == DEV;
                case "backup_auto":
                    return this == FULL || this == DEV;
                case "administracion":
                    return accesoAdministracion;
                default:
                    return true; // Funcionalidad básica siempre disponible
            }
        }
    }
}
