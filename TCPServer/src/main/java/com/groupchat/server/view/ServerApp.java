package com.groupchat.server.view;

import com.groupchat.server.model.ChatServer;
import com.groupchat.server.model.ServerConfig;
import com.groupchat.server.model.ServerEventListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * JavaFX presentation layer for the server.
 * <p>
 * It owns no networking logic — it merely starts a {@link ChatServer}, registers
 * itself as the {@link ServerEventListener}, and renders whatever the model reports:
 * an activity log and a live, colour-coded list of connected users. All model
 * callbacks are marshalled onto the JavaFX Application Thread with
 * {@link Platform#runLater}.
 */
public class ServerApp extends Application implements ServerEventListener {

    private final TextArea logArea = new TextArea();
    private final ObservableList<String> users = FXCollections.observableArrayList();
    private final Map<String, String> userColors = new HashMap<>();
    private final Random random = new Random();

    private ChatServer server;

    @Override
    public void start(Stage stage) {
        ServerConfig config = ServerConfig.load();

        Scene scene = new Scene(buildLayout(), 760, 480);
        applyStylesheet(scene, "/server.css");

        stage.setTitle("TCPServer — Group Chat");
        stage.setScene(scene);
        stage.show();

        server = new ChatServer(config.getPort());
        server.setListener(this);
        try {
            server.start();
        } catch (Exception e) {
            onLog("Failed to start server: " + e.getMessage());
        }

        stage.setOnCloseRequest(e -> shutdown());
    }

    /** Loads a CSS stylesheet if present; styling is optional, so a missing file is not fatal. */
    private void applyStylesheet(Scene scene, String path) {
        var url = getClass().getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            System.err.println("Stylesheet " + path + " not found on classpath; using default styling.");
        }
    }

    private GridPane buildLayout() {
        Label title = new Label("Group Chat Server");
        title.getStyleClass().add("title");

        Label logLabel = new Label("Activity Log");
        logArea.setEditable(false);
        logArea.setWrapText(true);

        Label usersLabel = new Label("Connected Users");
        ListView<String> userList = new ListView<>(users);
        userList.setCellFactory(lv -> coloredCell());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("root-grid");
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));

        ColumnConstraints logCol = new ColumnConstraints();
        logCol.setPercentWidth(65);
        ColumnConstraints usersCol = new ColumnConstraints();
        usersCol.setPercentWidth(35);
        grid.getColumnConstraints().addAll(logCol, usersCol);

        grid.add(title, 0, 0, 2, 1);
        grid.add(logLabel, 0, 1);
        grid.add(usersLabel, 1, 1);
        grid.add(logArea, 0, 2);
        grid.add(userList, 1, 2);

        GridPane.setVgrow(logArea, Priority.ALWAYS);
        GridPane.setVgrow(userList, Priority.ALWAYS);
        GridPane.setHgrow(logArea, Priority.ALWAYS);
        return grid;
    }

    /** A list cell that paints each username with its assigned random colour. */
    private ListCell<String> coloredCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-background-color:" + userColors.getOrDefault(item, "#ffffff")
                            + "; -fx-padding:6; -fx-background-radius:4; -fx-font-weight:bold;");
                }
            }
        };
    }

    /** Assigns a readable, light-ish random colour so usernames stay legible. */
    private String randomColor() {
        int r = 150 + random.nextInt(106);
        int g = 150 + random.nextInt(106);
        int b = 150 + random.nextInt(106);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private void shutdown() {
        if (server != null) {
            server.stop();
        }
        Platform.exit();
        System.exit(0);
    }

    // ---- ServerEventListener (always called off the FX thread) ----

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    @Override
    public void onUserConnected(String username) {
        Platform.runLater(() -> {
            userColors.putIfAbsent(username, randomColor());
            if (!users.contains(username)) {
                users.add(username);
            }
        });
    }

    @Override
    public void onUserDisconnected(String username) {
        Platform.runLater(() -> users.remove(username));
    }
}
