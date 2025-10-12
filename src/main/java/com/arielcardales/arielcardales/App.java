package com.arielcardales.arielcardales;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // 🔹 Registrar las fuentes manualmente
        Font.loadFont(getClass().getResourceAsStream("/Fuentes/static/Lora-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/Fuentes/static/Lora-Bold.ttf"), 14);

        // 🔹 Cargar interfaz principal
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/principal.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 1450, 830); // tamaño ventana

        // 🔹 Aplicar tu CSS
        scene.getStylesheets().add(getClass().getResource("/Estilos/Estilos.css").toExternalForm());

        stage.setTitle("Inventario Ariel");
        stage.setScene(scene);
        stage.show();

        // 🪶 Verificar que la fuente se cargó correctamente
        System.out.println("Fuentes disponibles:");
        javafx.scene.text.Font.getFamilies().forEach(System.out::println);
    }

    public static void main(String[] args) {
        launch();
    }
}
