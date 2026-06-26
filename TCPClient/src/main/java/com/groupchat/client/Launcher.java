package com.groupchat.client;

import com.groupchat.client.view.ClientApp;
import javafx.application.Application;

/**
 * Plain (non-JavaFX) entry point so the client runs from a normal executable JAR:
 * {@code java -jar TCPClient.jar [serverIp] [port]}.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
