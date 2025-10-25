package SORT_PROYECTS.AppInventario.Util;

/**
 * Clase final para centralizar todas las constantes de permisos y límites.
 * Esto evita errores de tipeo y facilita la gestión de planes.
 */
public final class Permisos {

    // --- Permisos (Funcionalidades On/Off) ---

    /** Controla el acceso al menú "Administración" y sus funciones (ej: registrar usuarios). */
    public static final String ADMIN_MENU = "admin_menu";

    /** Controla la capacidad de exportar a PDF en cualquier vista. */
    public static final String EXPORTAR_PDF = "exportar_pdf";

    /** Controla la capacidad de exportar a Excel en cualquier vista. */
    public static final String EXPORTAR_EXCEL = "exportar_excel";

    /** * Controla el acceso a funciones avanzadas de métricas:
     * - La pestaña/vista "Rentabilidad".
     * - Los gráficos en el Dashboard.
     * (El plan BASE SÍ ve el Dashboard básico con cards, pero NO esto).
     */
    public static final String METRICAS_AVANZADAS = "metricas_avanzadas";

    // --- Límites (Numéricos - Principalmente para DEMO) ---

    /** Límite de cuántos productos (base, sin contar variantes) se pueden crear. */
    public static final String MAX_PRODUCTOS = "limite_productos";

    /** Límite de cuántas ventas se pueden registrar. */
    public static final String MAX_VENTAS = "limite_ventas";

    /** Límite de cuántos clientes se pueden registrar. */
    public static final String MAX_CLIENTES = "limite_clientes"; // NUEVO

    // Constructor privado para que no se pueda instanciar
    private Permisos() {}
}

