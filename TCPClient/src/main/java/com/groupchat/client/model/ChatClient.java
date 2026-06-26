package com.groupchat.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Client-side networking logic: connects to the server, sends user input, and
 * streams incoming frames back to the view through a {@link ClientEventListener}.
 * Contains no JavaFX code.
 */
public class ChatClient {

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientEventListener listener;

    private volatile boolean connected;
    private boolean readOnly;
    private String username = "";

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setListener(ClientEventListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Opens the socket and sends the login frame. A blank username requests
     * READ-ONLY access. Incoming frames are then read on a background thread.
     */
    public void connect(String username) throws IOException {
        this.username = username == null ? "" : username.trim();
        this.readOnly = this.username.isEmpty();

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        out.println(Protocol.LOGIN + Protocol.SEP + this.username);
        connected = true;

        Thread reader = new Thread(this::readLoop, "client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                dispatch(line);
            }
        } catch (IOException ignored) {
            // socket closed
        } finally {
            connected = false;
            if (listener != null) {
                listener.onDisconnected();
            }
        }
    }

    private void dispatch(String line) {
        if (listener == null) {
            return;
        }
        int idx = line.indexOf(Protocol.SEP);
        String type = idx >= 0 ? line.substring(0, idx) : line;
        String body = idx >= 0 ? line.substring(idx + 1) : "";

        switch (type) {
            case Protocol.WELCOME -> {
                readOnly = Protocol.READ_ONLY.equals(body);
                listener.onConnected(readOnly ? "Guest" : username, readOnly);
            }
            case Protocol.CHAT -> listener.onMessage(body);
            case Protocol.USERS -> listener.onUsersList(body);
            case Protocol.INFO -> listener.onInfo(body);
            default -> {
                // unknown frame: ignore
            }
        }
    }

    /**
     * Interprets a line typed by the user: the {@code allUsers}, {@code end} and
     * {@code bye} commands are handled specially, everything else is a chat message.
     */
    public void handleInput(String text) {
        if (!connected || text == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        if (trimmed.equalsIgnoreCase(Protocol.CMD_END) || trimmed.equalsIgnoreCase(Protocol.CMD_BYE)) {
            disconnect();
        } else if (trimmed.equalsIgnoreCase(Protocol.CMD_ALL_USERS)) {
            out.println(Protocol.USERS);
        } else {
            out.println(Protocol.MSG + Protocol.SEP + text);
        }
    }

    /** Tells the server we are leaving and closes the socket. */
    public void disconnect() {
        try {
            if (out != null && connected) {
                out.println(Protocol.BYE);
            }
        } catch (Exception ignored) {
            // best effort
        }
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // already closed
        }
    }
}
