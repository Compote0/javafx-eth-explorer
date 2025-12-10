package org.example.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.javafx.controller.DashboardController;

import java.io.IOException;

public class BlockchainExplorerApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Enable better font rendering
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        
        FXMLLoader fxmlLoader = new FXMLLoader(BlockchainExplorerApp.class.getResource("dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        
        // Apply font smoothing to the scene
        scene.getRoot().setStyle("-fx-font-smoothing-type: gray;");
        
        // Store scene reference in controller for navigation
        DashboardController controller = fxmlLoader.getController();
        if (controller != null) {
            controller.setScene(scene);
        }
        
        stage.setTitle("Ethereum Explorer - Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

