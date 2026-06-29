package com.groupchat.client.view;

import com.groupchat.client.model.ChatClient;
import com.groupchat.client.model.ClientConfig;
import com.groupchat.client.model.ClientEventListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * JavaFX presentation layer for the client.
 * <p>
 * Prompts for a username (empty = READ-ONLY mode), then drives a {@link ChatClient}
 * and renders the conversation. Shows an "Online"/"Offline" status label plus a
 * coloured status dot, and disables the input controls when read-only. All model
 * callbacks are routed onto the FX thread with {@link Platform#runLater}.
 */
public class ClientApp extends Application implements ClientEventListener {

    private static final Color DOT_ONLINE = Color.web("#2ecc71");
    private static final Color DOT_OFFLINE = Color.web("#e74c3c");
    private static final Color DOT_IDLE = Color.web("#bdc3c7");

    private final TextArea chatArea = new TextArea();
    private final TextField input = new TextField();
    private final Button sendButton = new Button("SEND");
    private final Label statusLabel = new Label("Offline");
    private final Circle statusDot = new Circle(7, DOT_IDLE);

    private ChatClient client;

    @Override
    public void start(Stage stage) {
        List<String> rawArgs = getParameters().getRaw();
        ClientConfig config = ClientConfig.load(rawArgs.toArray(new String[0]));

        String username = askUsername();

        buildUI(stage);

        client = new ChatClient(config.getHost(), config.getPort());
        client.setListener(this);
        try {
            client.connect(username);
        } catch (Exception e) {
            appendSystem("Could not connect to " + config.getHost() + ":" + config.getPort()
                    + " — " + e.getMessage());
        }
    }

    /** Username prompt. Cancelling or leaving it empty joins in READ-ONLY mode. */
    private String askUsername() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText("Enter your username");
        dialog.setContentText("Leave empty to join in READ-ONLY mode:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse("").trim();
    }

    private void buildUI(Stage stage) {
        // ---- Header bar ----
        Label title = new Label("Group Chat");
        title.getStyleClass().add("title");
        Label subtitle = new Label("TCP messaging");
        subtitle.getStyleClass().add("subtitle");
        VBox titleBox = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel.getStyleClass().add("status-label");
        HBox statusPill = new HBox(8, statusDot, statusLabel);
        statusPill.setAlignment(Pos.CENTER);
        statusPill.getStyleClass().add("status-pill");

        HBox header = new HBox(12, titleBox, spacer, statusPill);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // ---- Chat surface ----
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.getStyleClass().add("message-area");
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // ---- Composer ----
        input.setPromptText("Type a message, or 'allUsers', 'end', 'bye' ...");
        input.setOnAction(e -> onSend());
        input.getStyleClass().add("input-field");
        HBox.setHgrow(input, Priority.ALWAYS);

        sendButton.setDefaultButton(true);
        sendButton.setOnAction(e -> onSend());
        sendButton.getStyleClass().add("send-button");

        HBox composer = new HBox(10, input, sendButton);
        composer.setAlignment(Pos.CENTER);
        composer.getStyleClass().add("composer");

        VBox root = new VBox(14, header, chatArea, composer);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(18));

        Scene scene = new Scene(root, 600, 560);
        applyStylesheet(scene, "/client.css");

        stage.setTitle("TCPClient — Group Chat");
        stage.setScene(scene);
        stage.show();

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

    private void onSend() {
        String text = input.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        client.handleInput(text);
        input.clear();
    }

    private void setReadOnlyUI(boolean readOnly) {
        input.setDisable(readOnly);
        sendButton.setDisable(readOnly);
        if (readOnly) {
            input.setPromptText("READ-ONLY MODE — you cannot send messages");
        }
    }

    private void appendSystem(String text) {
        Platform.runLater(() -> chatArea.appendText("* " + text + "\n"));
    }

    private void shutdown() {
        if (client != null) {
            client.disconnect();
        }
        Platform.exit();
        System.exit(0);
    }

    // ---- ClientEventListener ----

    @Override
    public void onConnected(String displayName, boolean readOnly) {
        Platform.runLater(() -> {
            statusLabel.setText("Online");
            statusDot.setFill(DOT_ONLINE);
            setReadOnlyUI(readOnly);
            if (readOnly) {
                appendSystem("Connected as guest (READ-ONLY MODE).");
            } else {
                appendSystem("Connected as " + displayName + ".");
            }
        });
    }

    @Override
    public void onMessage(String text) {
        Platform.runLater(() -> chatArea.appendText(text + "\n"));
    }

    @Override
    public void onUsersList(String csv) {
        appendSystem("Active users: " + (csv == null || csv.isBlank() ? "(none)" : csv));
    }

    @Override
    public void onInfo(String text) {
        appendSystem(text);
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            statusLabel.setText("Offline");
            statusDot.setFill(DOT_OFFLINE);
            setReadOnlyUI(true);
            appendSystem("Disconnected from server.");
        });
    }
}
