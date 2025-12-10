package org.example.javafx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {


    @FXML
    private Label myLabel;

    @FXML
    protected void onBT1Click(ActionEvent actionEvent) {
        myLabel.setText("ClicBT1");
    }

    public void onBT2Click(ActionEvent actionEvent) {
        myLabel.setText("ClicBT2");
    }
}