module com.arielcardales.arielcardales {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires net.synedra.validatorfx;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires jdk.compiler;


// Exportar para que el FXML pueda acceder
    opens com.arielcardales.arielcardales.controller to javafx.fxml;
    opens com.arielcardales.arielcardales.Entidades to javafx.base;

// Exportar el resto si hace falta
    exports com.arielcardales.arielcardales;

}
