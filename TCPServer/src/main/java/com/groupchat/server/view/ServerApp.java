package com.groupchat.server.view;

import com.groupchat.server.model.ChatServer;
import com.groupchat.server.model.ServerConfig;
import com.groupchat.server.model.ServerEventListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

    private VBox buildLayout() {
        // ---- Header ----
        Label title = new Label("Group Chat Server");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Central message distributor");
        subtitle.getStyleClass().add("subtitle");
        VBox titleBox = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label runningBadge = new Label("● RUNNING");
        runningBadge.getStyleClass().add("running-badge");

        HBox header = new HBox(12, titleBox, spacer, runningBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // ---- Log panel ----
        Label logLabel = new Label("Activity Log");
        logLabel.getStyleClass().add("section-label");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("log-area");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox logPanel = new VBox(8, logLabel, logArea);
        logPanel.getStyleClass().add("panel");

        // ---- Users panel ----
        Label usersLabel = new Label("Connected Users");
        usersLabel.getStyleClass().add("section-label");
        ListView<String> userList = new ListView<>(users);
        userList.setCellFactory(lv -> coloredCell());
        userList.setPlaceholder(new Label("No users connected"));
        VBox.setVgrow(userList, Priority.ALWAYS);
        VBox usersPanel = new VBox(8, usersLabel, userList);
        usersPanel.getStyleClass().add("panel");

        // ---- Body grid ----
        GridPane body = new GridPane();
        body.setHgap(14);
        ColumnConstraints logCol = new ColumnConstraints();
        logCol.setPercentWidth(65);
        ColumnConstraints usersCol = new ColumnConstraints();
        usersCol.setPercentWidth(35);
        body.getColumnConstraints().addAll(logCol, usersCol);
        body.add(logPanel, 0, 0);
        body.add(usersPanel, 1, 0);
        GridPane.setVgrow(logPanel, Priority.ALWAYS);
        GridPane.setVgrow(usersPanel, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox root = new VBox(14, header, body);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(18));
        return root;
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
