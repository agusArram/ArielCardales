package com.arielcardales.arielcardales;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/producto.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1250, 700); //tamanio de la ventana
        stage.setTitle("Inventario Ariel");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
