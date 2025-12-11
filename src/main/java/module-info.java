module org.example.javafx {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    
    requires java.net.http;
    requires java.desktop;

    // transitive so clients can access Gson types
    requires transitive com.google.gson;

    // transitive so clients can access Stage, Scene, etc.
    requires transitive javafx.graphics;

    // static so lombok for reducing boilerplate
    requires static lombok;

    opens org.example.javafx to javafx.fxml;
    opens org.example.javafx.model to com.google.gson;
    opens org.example.javafx.controller to javafx.fxml, com.google.gson;
    opens org.example.javafx.repository to com.google.gson;
    opens org.example.javafx.service to com.google.gson;
    opens org.example.javafx.exception to com.google.gson;
    exports org.example.javafx;
    exports org.example.javafx.model;
    exports org.example.javafx.service;
    exports org.example.javafx.controller;
    exports org.example.javafx.repository;
    exports org.example.javafx.exception;
}