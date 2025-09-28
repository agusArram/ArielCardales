module com.arielcardales.arielcardales {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Librerías externas que vi en tu proyecto/documentación
    requires org.controlsfx.controls;     // Para notificaciones, dialogs, etc.
    requires com.dlsc.formsfx;           // Para formularios (si no lo usás, se puede borrar).
    requires net.synedra.validatorfx;    // Validaciones (si no lo usás aún, se puede borrar).
    requires org.kordamp.ikonli.javafx;  // Íconos vectoriales (si no usás íconos, se puede borrar).
    requires org.kordamp.bootstrapfx.core; // Estilos tipo Bootstrap (si no lo aplicaste, se puede borrar).


    // JDBC
    requires java.sql;

    // Paquete principal
    opens com.arielcardales.arielcardales to javafx.fxml; // Necesario para App.java
    exports com.arielcardales.arielcardales; // Para exponer el paquete principal

    //No lo se
    requires com.zaxxer.hikari; // Solo si usás pool de conexiones HikariCP.

    requires org.slf4j; // Solo si usás logging con SLF4J.

    // requires jdk.compiler; // ⚠️ Esto es para generar código dinámicamente (annotation processors, etc.)



    // Exportar para que el FXML pueda acceder
    opens com.arielcardales.arielcardales.controller to javafx.fxml; // Necesario para FXML
    opens com.arielcardales.arielcardales.Entidades to javafx.base;  // Necesario para TableView (PropertyValueFactory)


}
