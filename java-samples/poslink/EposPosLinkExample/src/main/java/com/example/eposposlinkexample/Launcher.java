package com.example.eposposlinkexample;

import javafx.application.Application;

import java.awt.Toolkit;

public class Launcher {
    public static void main(String[] args) {
        // The SDK draws its own windows with AWT, which has to claim the native UI
        // thread before JavaFX does, otherwise the two toolkits deadlock on macOS.
        Toolkit.getDefaultToolkit();
        Application.launch(EposPosLinkApp.class, args);
    }
}
