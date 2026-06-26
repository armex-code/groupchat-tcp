package com.groupchat.server.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles a single client connection on its own thread: reads incoming frames,
 * interprets the {@link Protocol}, and writes outgoing frames back to that client.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;

    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean readOnly;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            handleLogin(in.readLine());

            String line;
            while ((line = in.readLine()) != null) {
                if (!handleFrame(line)) {
                    break;
                }
            }
        } catch (IOException e) {
            // client vanished; fall through to cleanup
        } finally {
            server.removeClient(this);
            close();
        }
    }

    /**
     * The first frame must be {@code LOGIN|username}. An empty username puts the
     * session into READ-ONLY mode (Functional Requirement 2.1).
     */
    private void handleLogin(String firstLine) {
        String name = "";
        String loginPrefix = Protocol.LOGIN + Protocol.SEP;
        if (firstLine != null && firstLine.startsWith(loginPrefix)) {
            name = firstLine.substring(loginPrefix.length()).trim();
        }

        if (name.isEmpty()) {
            readOnly = true;
            username = null;
            send(Protocol.WELCOME + Protocol.SEP + Protocol.READ_ONLY);
            server.onGuestConnected();
        } else {
            readOnly = false;
            username = name;
            send(Protocol.WELCOME + Protocol.SEP + username);
            server.onClientLoggedIn(this);
        }
    }

    /** @return {@code false} when the client asked to disconnect. */
    private boolean handleFrame(String line) {
        int idx = line.indexOf(Protocol.SEP);
        String type = idx >= 0 ? line.substring(0, idx) : line;
        String body = idx >= 0 ? line.substring(idx + 1) : "";

        switch (type) {
            case Protocol.MSG -> {
                if (readOnly) {
                    send(Protocol.INFO + Protocol.SEP
                            + "You are in READ-ONLY mode and cannot send messages.");
                } else {
                    server.broadcast(username, body);
                }
            }
            case Protocol.USERS -> {
                String list = String.join(", ", server.activeUsernames());
                send(Protocol.USERS + Protocol.SEP + list);
            }
            case Protocol.BYE -> {
                return false;
            }
            default -> {
                // unknown frame: ignore for forward-compatibility
            }
        }
        return true;
    }

    void sendChat(String formattedMessage) {
        send(Protocol.CHAT + Protocol.SEP + formattedMessage);
    }

    private void send(String rawFrame) {
        if (out != null) {
            out.println(rawFrame);
        }
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // already closed
        }
    }
}
