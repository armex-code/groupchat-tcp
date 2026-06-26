package com.groupchat.server.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central message distributor.
 * <p>
 * Uses a <strong>thread-per-connection</strong> model: a dedicated acceptor thread
 * waits on {@link ServerSocket#accept()} and hands every new socket to its own
 * {@link ClientHandler} thread. This is the simplest of the two approaches noted in
 * the requirements (3.2); the alternative — a single thread with an NIO
 * {@code Selector} — scales better for very large client counts but is heavier to
 * implement and is documented in the README as a possible evolution.
 * <p>
 * The class is pure logic: it knows nothing about JavaFX and reports everything it
 * does through a {@link ServerEventListener}.
 */
public class ChatServer {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private volatile boolean running;
    private ServerEventListener listener;

    public ChatServer(int port) {
        this.port = port;
    }

    public void setListener(ServerEventListener listener) {
        this.listener = listener;
    }

    /** Binds the server socket and starts accepting clients on a background thread. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        log("Server Started on port " + port);

        Thread acceptor = new Thread(this::acceptLoop, "accept-loop");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                log("Waiting for Client ...");
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);

                Thread worker = new Thread(handler, "client-" + socket.getPort());
                worker.setDaemon(true);
                worker.start();
            } catch (IOException e) {
                if (running) {
                    log("Accept error: " + e.getMessage());
                }
            }
        }
    }

    /** Stops accepting clients and closes every open connection. */
    public void stop() {
        running = false;
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // shutting down anyway
        }
        log("Server stopped");
    }

    // ---- callbacks invoked by ClientHandler ----

    void onClientLoggedIn(ClientHandler handler) {
        log("Welcome " + handler.getUsername());
        if (listener != null) {
            listener.onUserConnected(handler.getUsername());
        }
    }

    void onGuestConnected() {
        log("A guest connected in READ-ONLY mode");
    }

    void removeClient(ClientHandler handler) {
        boolean wasPresent = clients.remove(handler);
        if (wasPresent && handler.getUsername() != null && !handler.isReadOnly()) {
            log(handler.getUsername() + " disconnected");
            if (listener != null) {
                listener.onUserDisconnected(handler.getUsername());
            }
        }
    }

    /** Formats a message with sender + time and pushes it to every connected client. */
    void broadcast(String fromUser, String text) {
        String formatted = "[" + LocalTime.now().format(TIME) + "] " + fromUser + ": " + text;
        log(formatted);
        for (ClientHandler client : clients) {
            client.sendChat(formatted);
        }
    }

    /** Returns the usernames of all active (non read-only) clients. */
    List<String> activeUsernames() {
        return clients.stream()
                .filter(c -> c.getUsername() != null && !c.isReadOnly())
                .map(ClientHandler::getUsername)
                .toList();
    }

    void log(String message) {
        if (listener != null) {
            listener.onLog(message);
        } else {
            System.out.println(message);
        }
    }
}
