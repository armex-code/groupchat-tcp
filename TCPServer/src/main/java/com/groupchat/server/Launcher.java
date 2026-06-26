package com.groupchat.server;

import com.groupchat.server.view.ServerApp;
import javafx.application.Application;

/**
 * Plain (non-JavaFX) entry point.
 * <p>
 * Launching the {@link javafx.application.Application} from a separate class that does
 * <em>not</em> extend {@code Application} lets the program be started from a normal
 * executable JAR ({@code java -jar TCPServer.jar}) without the JavaFX runtime
 * complaining about missing components.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(ServerApp.class, args);
    }
}
