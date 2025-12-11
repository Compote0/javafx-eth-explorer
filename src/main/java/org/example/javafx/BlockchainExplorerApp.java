package org.example.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.javafx.controller.DashboardController;

import javax.imageio.ImageIO;
import java.awt.Taskbar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        
        // set application icon (window + dock/taskbar where supported)
        try (InputStream iconStream = BlockchainExplorerApp.class.getResourceAsStream("etherscan.png")) {
            if (iconStream != null) {
                byte[] iconBytes = iconStream.readAllBytes();
                Image fxIcon = new Image(new ByteArrayInputStream(iconBytes));
                stage.getIcons().add(fxIcon);

                // macOS dock / Windows taskbar icon for the app
                try {
                    Taskbar taskbar = Taskbar.getTaskbar();
                    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                        java.awt.Image awtIcon = ImageIO.read(new ByteArrayInputStream(iconBytes));
                        if (awtIcon != null) {
                            taskbar.setIconImage(awtIcon);
                        }
                    }
                } catch (UnsupportedOperationException | SecurityException innerEx) {
                    System.err.println("[BlockchainExplorerApp] Unable to set dock/taskbar icon: " + innerEx.getMessage());
                }
            } else {
                System.err.println("[BlockchainExplorerApp] etherscan.png not found in resources.");
            }
        }

        stage.setTitle("Ethereum Explorer - Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

