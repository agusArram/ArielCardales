// === CAMBIO AQUÍ: Nombre del módulo ===
module SORT_PROYECTS.AppInventario {
// === FIN CAMBIO ===

    // JavaFX (Core)
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // Necesario para gráficos, layouts
    requires javafx.base;     // Necesario para Properties en Entidades

    // --- LIBRERÍAS EXTERNAS ---
    requires org.controlsfx.controls; // UI Auxiliares
    requires org.json;                // Para parsear JSON (GitHub API)
    requires com.zaxxer.hikari;       // Pool de conexiones HikariCP
    requires java.sql;                // JDBC (PostgreSQL, SQLite)
    requires bcrypt;                  // Hash de contraseñas
    requires org.apache.poi.poi;      // Exportar Excel (Core)
    requires org.apache.poi.ooxml;    // Exportar Excel (Formatos Office Open XML)
    requires com.github.librepdf.openpdf; // Exportar PDF

    // --- MÓDULOS JAVA ESTÁNDAR ---
    requires java.net.http; // Cliente HTTP para UpdateChecker, LicenciaManager
    requires java.desktop;  // Necesario para abrir archivos (PDF, Excel)
    requires java.prefs;    // Para SessionPersistence

    // --- APERTURA Y EXPORTACIÓN DE PAQUETES ---

    // === CAMBIOS AQUÍ: Usar el paquete correcto ===
    // Abrir paquetes a JavaFX para reflexión (FXML, TableView)
    opens SORT_PROYECTS.AppInventario to javafx.fxml;
    opens SORT_PROYECTS.AppInventario.controller to javafx.fxml;
    opens SORT_PROYECTS.AppInventario.Entidades to javafx.base; // Para PropertyValueFactory

    // Exportar paquetes principales para que otras partes (como el Launcher) puedan usarlos
    exports SORT_PROYECTS.AppInventario;
    exports SORT_PROYECTS.AppInventario.controller;
    exports SORT_PROYECTS.AppInventario.DAO;
    exports SORT_PROYECTS.AppInventario.Entidades;
    exports SORT_PROYECTS.AppInventario.Licencia;
    exports SORT_PROYECTS.AppInventario.Updates;
    exports SORT_PROYECTS.AppInventario.Util;
    exports SORT_PROYECTS.AppInventario.View;
    exports SORT_PROYECTS.AppInventario.service;
    exports SORT_PROYECTS.AppInventario.session;
    // === FIN CAMBIOS ===

}
