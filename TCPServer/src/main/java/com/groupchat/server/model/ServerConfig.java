package com.groupchat.server.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Network configuration for the server, loaded at runtime so the IP/port can be
 * changed without recompiling (Technical Specification 3.3).
 * <p>
 * Resolution order:
 * <ol>
 *     <li>an external {@code server.properties} sitting next to the JAR;</li>
 *     <li>the {@code server.properties} bundled on the classpath;</li>
 *     <li>hard-coded defaults ({@code 0.0.0.0:3000}).</li>
 * </ol>
 */
public class ServerConfig {

    private static final String FILE_NAME = "server.properties";
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 3000;

    private final String host;
    private final int port;

    public ServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static ServerConfig load() {
        Properties props = new Properties();
        try (InputStream in = openConfig()) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Could not read " + FILE_NAME + ": " + e.getMessage());
        }
        String host = props.getProperty("server.host", DEFAULT_HOST).trim();
        int port = parsePort(props.getProperty("server.port"));
        return new ServerConfig(host, port);
    }

    private static InputStream openConfig() throws IOException {
        File external = new File(FILE_NAME);
        if (external.isFile()) {
            return new FileInputStream(external);
        }
        return ServerConfig.class.getResourceAsStream("/" + FILE_NAME);
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid server.port '" + raw + "', falling back to " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
