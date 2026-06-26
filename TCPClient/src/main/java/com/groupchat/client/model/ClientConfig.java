package com.groupchat.client.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves the server endpoint for the client.
 * <p>
 * Resolution order (highest priority first):
 * <ol>
 *     <li>command-line arguments {@code <serverIp> <port>} (Operational Requirement 4.1);</li>
 *     <li>an external {@code client.properties} next to the JAR;</li>
 *     <li>the bundled {@code client.properties};</li>
 *     <li>defaults ({@code localhost:3000}).</li>
 * </ol>
 */
public class ClientConfig {

    private static final String FILE_NAME = "client.properties";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3000;

    private final String host;
    private final int port;

    public ClientConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static ClientConfig load(String[] args) {
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

        // Command-line arguments take precedence: java -jar TCPClient.jar <ip> <port>
        if (args != null && args.length >= 1 && !args[0].isBlank()) {
            host = args[0].trim();
        }
        if (args != null && args.length >= 2 && !args[1].isBlank()) {
            port = parsePort(args[1]);
        }
        return new ClientConfig(host, port);
    }

    private static InputStream openConfig() throws IOException {
        File external = new File(FILE_NAME);
        if (external.isFile()) {
            return new FileInputStream(external);
        }
        return ClientConfig.class.getResourceAsStream("/" + FILE_NAME);
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid port '" + raw + "', falling back to " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
