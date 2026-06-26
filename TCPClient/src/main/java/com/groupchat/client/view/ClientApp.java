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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
        Label title = new Label("Group Chat");
        title.getStyleClass().add("title");

        HBox status = new HBox(6, statusDot, statusLabel);
        status.setAlignment(Pos.CENTER_RIGHT);

        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        input.setPromptText("Type a message, or 'allUsers', 'end', 'bye' ...");
        input.setOnAction(e -> onSend());

        sendButton.setDefaultButton(true);
        sendButton.setMaxWidth(Double.MAX_VALUE);
        sendButton.setOnAction(e -> onSend());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("root-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints inputCol = new ColumnConstraints();
        inputCol.setPercentWidth(78);
        ColumnConstraints buttonCol = new ColumnConstraints();
        buttonCol.setPercentWidth(22);
        grid.getColumnConstraints().addAll(inputCol, buttonCol);

        grid.add(title, 0, 0);
        grid.add(status, 1, 0);
        grid.add(chatArea, 0, 1, 2, 1);
        grid.add(input, 0, 2);
        grid.add(sendButton, 1, 2);

        GridPane.setVgrow(chatArea, Priority.ALWAYS);
        GridPane.setHgrow(chatArea, Priority.ALWAYS);
        GridPane.setHgrow(input, Priority.ALWAYS);

        Scene scene = new Scene(grid, 560, 520);
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
